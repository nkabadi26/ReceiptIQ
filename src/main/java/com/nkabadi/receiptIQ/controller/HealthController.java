package com.nkabadi.receiptIQ.controller;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nkabadi.receiptIQ.dto.ReceiptDto;

@RestController
public class HealthController {
    private static final BeanOutputConverter<ReceiptDto> RECEIPT_OUTPUT_CONVERTER =
            new BeanOutputConverter<>(ReceiptDto.class);

    private final ChatClient chatClient;
    private final Path receiptDirectory;

    public HealthController(ChatClient.Builder chatClientBuilder,
                            @Value("${receipts.directory}") String receiptDirectoryPath) {
        this.chatClient = chatClientBuilder.build();
        this.receiptDirectory = Path.of(receiptDirectoryPath).toAbsolutePath().normalize();
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping(value = "/json-schema", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receiptJsonSchema() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(RECEIPT_OUTPUT_CONVERTER.getJsonSchema());
    }

    @GetMapping("/test-receipt")
    public ResponseEntity<String> transcribeReceipt(@RequestParam String fileName) {
        Path receiptPath;
        try {
            receiptPath = receiptDirectory.resolve(fileName).normalize();
        } catch (InvalidPathException exception) {
            return ResponseEntity.badRequest().body("Invalid receipt file name.");
        }

        if (!receiptPath.startsWith(receiptDirectory)) {
            return ResponseEntity.badRequest().body("File must be inside the receipt directory.");
        }

        String normalizedFileName = receiptPath.getFileName().toString().toLowerCase(Locale.ROOT);
        MimeType mimeType;
        if (normalizedFileName.endsWith(".jpg") || normalizedFileName.endsWith(".jpeg")) {
            mimeType = MimeTypeUtils.IMAGE_JPEG;
        } else if (normalizedFileName.endsWith(".png")) {
            mimeType = MimeTypeUtils.IMAGE_PNG;
        } else {
            return ResponseEntity.badRequest().body("Use a .jpg, .jpeg, or .png receipt file.");
        }

        if (!Files.isRegularFile(receiptPath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Receipt file not found.");
        }

        String transcription = chatClient.prompt()
                .user(user -> user
                        .text("Transcribe all text on this receipt exactly as printed.")
                        .media(mimeType, new FileSystemResource(receiptPath)))
                .call()
                .content();

        return ResponseEntity.ok(transcription);
    }

}
