package com.nkabadi.receiptIQ.controller;

import java.time.LocalDate;

import com.nkabadi.receiptIQ.dto.ReceiptStatsDto;
import com.nkabadi.receiptIQ.dto.SmartAskResponse;
import com.nkabadi.receiptIQ.service.QuestionRouterService;
import com.nkabadi.receiptIQ.service.RagService;
import com.nkabadi.receiptIQ.service.ReceiptService;
import com.nkabadi.receiptIQ.service.ReceiptStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping ("/api")
public class RagController {
    private final RagService ragService;
    private final ReceiptService receiptService;
    private final ReceiptStatsService receiptStatsService;
    private final QuestionRouterService questionRouterService;

    public RagController(RagService ragService, ReceiptService receiptService,
                         ReceiptStatsService receiptStatsService,
                         QuestionRouterService questionRouterService) {
        this.ragService = ragService;
        this.receiptService = receiptService;
        this.receiptStatsService = receiptStatsService;
        this.questionRouterService = questionRouterService;
    }

    @GetMapping(value = "/ask", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> ask(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().body("Question must not be empty.");
        }

        try {
            return ResponseEntity.ok(ragService.answer(q.trim()));
        } catch (Exception exception) {
            return ResponseEntity.internalServerError()
                    .body("Unable to answer the question because embedding, receipt search, or the chat model failed: "
                            + exception.getMessage());
        }
    }

    @GetMapping(value = "smart-ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public SmartAskResponse smartAsk(@RequestParam String q) {
        return questionRouterService.answer(q.trim());
    }

    @PostMapping(value = "/admin/backfill-embeddings", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> backfillEmbeddings() {
        return ResponseEntity.ok(String.valueOf(receiptService.backfillEmbeddings()));
    }

    @GetMapping(value = "total", produces = MediaType.APPLICATION_JSON_VALUE)
    public ReceiptStatsDto total(@RequestParam String merchant,
                        @RequestParam(required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return receiptStatsService.getTotal(merchant, from, to);
    }
}
