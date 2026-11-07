package com.nkabadi.receiptIQ.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.nkabadi.receiptIQ.dto.RouteDecision;
import com.nkabadi.receiptIQ.dto.SmartAskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class QuestionRouterService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionRouterService.class);

    private final ChatClient chatClient;
    private final ReceiptStatsService receiptStatsService;
    private final RagService ragService;

    public QuestionRouterService(ChatClient.Builder chatClientBuilder,
                                 ReceiptStatsService receiptStatsService,
                                 RagService ragService) {
        this.chatClient = chatClientBuilder.build();
        this.receiptStatsService = receiptStatsService;
        this.ragService = ragService;
    }

    public SmartAskResponse answer(String question) {
        RouteDecision decision;
        try {
            String prompt = "Today is " + LocalDate.now() + ". Decide how to answer the user's question. "
                    + "Return only structured output matching RouteDecision. "
                    + "Route is SQL only when the question asks for a total amount spent at a named merchant, "
                    + "optionally within a date range. A question about a specific item or product is RAG, even if a merchant is named, for example how much was the milk at Aldi. "
                    + "Everything else, including questions about items, categories or products, routes to RAG. "
                    + "Set merchant, from and to to null when not stated. Resolve relative date phrases using today and return ISO dates. "
                    + "Do not calculate any amount. "
                    + "Question: " + question;
            decision = chatClient.prompt(prompt).call().entity(RouteDecision.class);
            if (decision == null) {
                throw new IllegalStateException("Router returned no decision");
            }
        } catch (Exception exception) {
            logger.warn("Question routing failed; falling back to RAG.", exception);
            return new SmartAskResponse("RAG", null, null, null, null, null,
                    ragService.answer(question), null);
        }

        if (!"SQL".equalsIgnoreCase(decision.route()) || decision.merchant() == null
                || decision.merchant().isBlank()) {
            return new SmartAskResponse("RAG", null, null, null, null, null,
                    ragService.answer(question), null);
        }

        LocalDate from;
        LocalDate to;
        try {
            from = parseDate(decision.from());
            to = parseDate(decision.to());
        } catch (DateTimeParseException exception) {
            logger.warn("Question routing returned invalid dates; falling back to RAG.", exception);
            return new SmartAskResponse("RAG", null, null, null, null, null,
                    ragService.answer(question), null);
        }

        var stats = receiptStatsService.getTotal(decision.merchant(), from, to);
        if (stats.receiptCount() == 0) {
            return new SmartAskResponse("SQL", stats.merchant(), stats.from(), stats.to(),
                    stats.receiptCount(), stats.totalPaid(), null,
                    "no receipts found for that merchant");
        }
        return new SmartAskResponse("SQL", stats.merchant(), stats.from(), stats.to(),
                stats.receiptCount(), stats.totalPaid(), null, null);
    }

    private static LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }
}
