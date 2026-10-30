package com.nkabadi.receiptIQ.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

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
    private static final int MODEL_MAX_ATTEMPTS = 3;
    private static final long[] MODEL_RETRY_WAIT_MILLIS = { 1_000L, 3_000L };
    private static final Set<Integer> MODEL_RETRYABLE_STATUS_CODES = Set.of(429, 503);

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

    @GetMapping("/getAllReceiptsFromFolder")
    public ResponseEntity<?> getAllReceiptsFromFolder(@RequestParam String folderPath) {
        Path folder;
        try {
            folder = Path.of(folderPath).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            return ResponseEntity.badRequest().body("Invalid folder path.");
        }

        if (!Files.isDirectory(folder) || !folder.startsWith(this.receiptDirectory)) {
            return ResponseEntity.badRequest().body("Folder must be a directory inside the receipt directory.");
        }

        List<Object> processedReceipts = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(Files::isRegularFile)
                    .filter(this::isSupportedReceiptImage)
                    .sorted()
                    .forEach(imagePath -> {
                        String fileName = this.receiptDirectory.relativize(imagePath).toString();
                        try {
                            ResponseEntity<?> response = getLLMReceiptJson(fileName);
                            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                                processedReceipts.add(response.getBody());
                            } else {
                                failures.add(fileName + ": " + response.getBody());
                            }
                        } catch (Exception exception) {
                            failures.add(fileName + ": " + exception.getMessage());
                        }
                    });
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to read receipt folder: " + exception.getMessage());
        }

        return ResponseEntity.ok(new FolderProcessingResult(processedReceipts, failures));
    }

    private boolean isSupportedReceiptImage(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png");
    }

    private record FolderProcessingResult(List<Object> processedReceipts, List<String> failures) {
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

        String response = callModelWithRetry(receiptPath, mimeType);

        ReceiptDto receiptDto = RECEIPT_OUTPUT_CONVERTER.convert(response);
        Receipt savedReceipt = receiptService.saveReceipt(receiptDto, fileName, response);
        return ResponseEntity.ok(savedReceipt);
    }

    private String callModelWithRetry(Path receiptPath, MimeType mimeType) {
        for (int attempt = 1; attempt <= MODEL_MAX_ATTEMPTS; attempt++) {
            try {
                return chatClient.prompt()
                        .options(GoogleGenAiChatOptions.builder()
                                .responseMimeType(MediaType.APPLICATION_JSON_VALUE)
                                .responseSchema(RECEIPT_OUTPUT_CONVERTER.getJsonSchema()))
                        .user(user -> user
                                .text("Extract the receipt data from the image. Omit optional values that are not readable. Return an empty items list if no line items are readable.")
                                .media(mimeType, new FileSystemResource(receiptPath)))
                        .call()
                        .content();
            } catch (RuntimeException exception) {
                if (!isRetryableModelException(exception) || attempt == MODEL_MAX_ATTEMPTS) {
                    throw exception;
                }
                waitBeforeRetry(MODEL_RETRY_WAIT_MILLIS[attempt - 1]);
            }
        }
        throw new IllegalStateException("Model call retry loop completed unexpectedly.");
    }

    private boolean isRetryableModelException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                for (int statusCode : MODEL_RETRYABLE_STATUS_CODES) {
                    if (message.contains(String.valueOf(statusCode))) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void waitBeforeRetry(long waitMillis) {
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry model call.", exception);
        }
    }
}
