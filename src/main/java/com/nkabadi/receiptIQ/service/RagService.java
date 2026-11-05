package com.nkabadi.receiptIQ.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.nkabadi.receiptIQ.repository.LineItemRepository;
import com.nkabadi.receiptIQ.util.VectorTextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RagService {
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    private final EmbeddingModel embeddingModel;
    private final LineItemRepository lineItemRepository;
    private final ChatClient chatClient;
    private final int topN;

    public RagService(EmbeddingModel embeddingModel,
                      LineItemRepository lineItemRepository,
                      ChatClient.Builder chatClientBuilder,
                      @Value("${rag.top-n:12}") int topN) {
        this.embeddingModel = embeddingModel;
        this.lineItemRepository = lineItemRepository;
        this.chatClient = chatClientBuilder.build();
        this.topN = topN;
    }

    public String answer(String question) {
        // 1. Embed the user's question with the same embedding model used by ReceiptService.
        float[] questionEmbedding = embeddingModel.embed(question);
        String vectorText = VectorTextUtils.toVectorText(questionEmbedding);

        // 2. Retrieve the nearest receipt items using the existing native pgvector query.
        List<Object[]> items = lineItemRepository.findNearest(vectorText, topN);
        items = netVoidedItems(items);

        // 3. Build the receipt context for the prompt, one item per line.
        StringBuilder receiptContext = new StringBuilder();
        for (Object[] item : items) {
            String merchant = text(item[1]);
            String date = optionalText(dateText(item[2]));
            String category = text(item[3]);
            String rawDescription = text(item[4]);
            String plainDescription = optionalText(item[5]);
            String amount = amountText(item[6]);
            String distance = item.length > 7 && item[7] != null
                    ? String.valueOf(item[7])
                    : "not returned";

            StringJoiner details = new StringJoiner("; ");
            details.add("merchant=" + merchant);
            if (!date.isBlank()) {
                details.add("date=" + date);
            }
            details.add("category=" + category);
            details.add("raw_description=" + rawDescription);
            if (!plainDescription.isBlank()) {
                details.add("plain_eng_desc=" + plainDescription);
            }
            details.add("amount=" + amount);
            receiptContext.append(details).append('\n');

            logger.info("RAG retrieved item: merchant={}, date={}, raw_description={}, amount={}, distance={}",
                    merchant, date, rawDescription, amount, distance);
        }

        String prompt = "Write a short, natural answer in plain English. "
                + "For each matching item mention the merchant, the date and the amount, with one line per item. "
                + "Do not repeat field names like merchant= or raw_description= and do not copy the item lines. "
                + "Use only the receipt items below. Do not invent anything. "
                + "If the question is about a broad category, such as snacks, groceries or tools, include every item that plausibly fits, not just the obvious ones. "
                + "For items where the fit is uncertain, especially cryptic descriptions, still list them but say they might be a match. "
                + "If nothing plausibly fits, say you could not find it in the receipts. Do not guess.\n\n"
                + "Question: " + question + "\n\n"
                + "Receipt items:\n" + receiptContext;

        logger.info("RAG question before chat model call: {}", question);

        // 4. Ask the existing Gemini chat model to answer from the retrieved receipt items.
        return chatClient.prompt(prompt).call().content();
    }

    private List<Object[]> netVoidedItems(List<Object[]> items) {
        if (items.isEmpty()) {
            return items;
        }

        Set<Long> receiptIds = new HashSet<>();
        for (Object[] item : items) {
            if (item.length > 8 && item[8] != null) {
                receiptIds.add(((Number) item[8]).longValue());
            }
        }
        if (receiptIds.isEmpty()) {
            return items;
        }

        List<Object[]> discounts = lineItemRepository.findDiscounts(new ArrayList<>(receiptIds));
        Set<Integer> cancelledIndexes = new HashSet<>();
        for (Object[] discount : discounts) {
            String discountCode = optionalText(discount[1]);
            if (discountCode.isBlank() || discount.length < 3 || !(discount[2] instanceof BigDecimal discountAmount)) {
                continue;
            }
            Long discountReceiptId = ((Number) discount[0]).longValue();

            for (int index = 0; index < items.size(); index++) {
                if (cancelledIndexes.contains(index) || items.get(index).length <= 9 || items.get(index)[8] == null) {
                    continue;
                }
                Object[] item = items.get(index);
                if (((Number) item[8]).longValue() != discountReceiptId) {
                    continue;
                }

                String itemCode = optionalText(item[9]);
                if (!itemCode.equals(discountCode) || !(item[6] instanceof BigDecimal itemAmount)) {
                    continue;
                }
                if (discountAmount.compareTo(itemAmount.negate()) != 0) {
                    continue;
                }

                cancelledIndexes.add(index);
                logger.info("RAG cancelled item: merchant={}, date={}, raw_description={}, amount={}, distance={}",
                        text(item[1]), optionalText(dateText(item[2])), text(item[4]), amountText(item[6]),
                        item.length > 7 && item[7] != null ? String.valueOf(item[7]) : "not returned");
                break;
            }
        }

        List<Object[]> retainedItems = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            if (!cancelledIndexes.contains(index)) {
                retainedItems.add(items.get(index));
            }
        }
        return retainedItems;
    }

    private static String text(Object value) {
        return value == null ? "unknown" : String.valueOf(value);
    }

    private static String optionalText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String dateText(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate.toString();
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        return optionalText(value);
    }

    private static String amountText(Object value) {
        if (value instanceof BigDecimal amount) {
            return amount.toPlainString();
        }
        return text(value);
    }
}
