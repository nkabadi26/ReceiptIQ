package com.nkabadi.receiptIQ.controller;

import com.nkabadi.receiptIQ.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagController {
    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping(value = "/api/ask", produces = MediaType.TEXT_PLAIN_VALUE)
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
}
