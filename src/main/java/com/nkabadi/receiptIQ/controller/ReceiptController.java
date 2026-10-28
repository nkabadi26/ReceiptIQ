package com.nkabadi.receiptIQ.controller;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import com.nkabadi.receiptIQ.dto.ReceiptDto;
import com.nkabadi.receiptIQ.entity.Receipt;
import com.nkabadi.receiptIQ.service.ReceiptService;

@RestController
@RequestMapping ("/receipt")
public class ReceiptController {
    private static final Path RECEIPT_DIRECTORY =
            Path.of("D:\\Development\\ReceiptIQ\\documents\\TestReceipts").toAbsolutePath().normalize();
    private static final BeanOutputConverter<ReceiptDto> RECEIPT_OUTPUT_CONVERTER =
            new BeanOutputConverter<>(ReceiptDto.class);

    private final ChatClient chatClient;
    private final Path receiptDirectory;
    private final ReceiptService receiptService;

    public ReceiptController(ChatClient.Builder chatClientBuilder,
                    @Value("${RECEIPT_DIRECTORY}") String receiptDirectoryPath,
                    ReceiptService receiptService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.receiptService = receiptService;
        try{
        this.receiptDirectory = Path.of(receiptDirectoryPath).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Invalid receipt directory path: " + receiptDirectoryPath, exception);
        }
    }

    @GetMapping("/getLLMReceiptJson")
    public ResponseEntity<?> getLLMReceiptJson(@RequestParam String fileName) {
        if (!this.receiptDirectory.startsWith(RECEIPT_DIRECTORY)) {
            return ResponseEntity.badRequest().body("File must be inside the receipt directory.");
        }

        Path receiptPath = this.receiptDirectory.resolve(fileName).normalize();
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

        String response = chatClient.prompt()
                .options(GoogleGenAiChatOptions.builder()
                        .responseMimeType(MediaType.APPLICATION_JSON_VALUE)
                        .responseSchema(RECEIPT_OUTPUT_CONVERTER.getJsonSchema()))
                .user(user -> user
                        .text("Extract the receipt data from the image. Omit optional values that are not readable. Return an empty items list if no line items are readable.")
                        .media(mimeType, new FileSystemResource(receiptPath)))
                .call()
                .content();

        ReceiptDto receiptDto = RECEIPT_OUTPUT_CONVERTER.convert(response);
        Receipt savedReceipt = receiptService.saveReceipt(receiptDto, fileName);
        return ResponseEntity.ok(savedReceipt);
    }
}
