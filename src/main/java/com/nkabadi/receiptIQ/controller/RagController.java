package com.nkabadi.receiptIQ.controller;

import com.nkabadi.receiptIQ.service.RagService;
import com.nkabadi.receiptIQ.service.ReceiptService;
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

    public RagController(RagService ragService, ReceiptService receiptService) {
        this.ragService = ragService;
        this.receiptService = receiptService;
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

    @PostMapping(value = "/admin/backfill-embeddings", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> backfillEmbeddings() {
        return ResponseEntity.ok(String.valueOf(receiptService.backfillEmbeddings()));
    }
}
