package com.nkabadi.receiptIQ.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
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

        // 3. Build the receipt context for the prompt, one item per line.
        StringBuilder receiptContext = new StringBuilder();
        for (Object[] item : items) {
            String merchant = text(item[1]);
            String date = dateText(item[2]);
            String category = text(item[3]);
            String rawDescription = text(item[4]);
            String plainDescription = optionalText(item[5]);
            String amount = amountText(item[6]);
            String distance = item.length > 7 && item[7] != null
                    ? String.valueOf(item[7])
                    : "not returned";

            StringJoiner details = new StringJoiner("; ");
            details.add("merchant=" + merchant);
            details.add("date=" + date);
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
                + "For each matching item mention the merchant, the date and the amount. "
                + "Do not repeat field names like merchant= or raw_description= and do not copy the item lines. "
                + "Use only the receipt items below. "
                + "If no item matches the question, say you could not find it in the receipts. Do not guess.\n\n"
                + "Question: " + question + "\n\n"
                + "Receipt items:\n" + receiptContext;

        logger.info("RAG question before chat model call: {}", question);

        // 4. Ask the existing Gemini chat model to answer from the retrieved receipt items.
        return chatClient.prompt(prompt).call().content();
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
        return text(value);
    }

    private static String amountText(Object value) {
        if (value instanceof BigDecimal amount) {
            return amount.toPlainString();
        }
        return text(value);
    }
}
