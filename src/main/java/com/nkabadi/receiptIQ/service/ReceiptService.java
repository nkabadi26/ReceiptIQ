package com.nkabadi.receiptIQ.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.nkabadi.receiptIQ.dto.LineItemDto;
import com.nkabadi.receiptIQ.dto.ReceiptDto;
import com.nkabadi.receiptIQ.entity.CategoryKind;
import com.nkabadi.receiptIQ.entity.LineItem;
import com.nkabadi.receiptIQ.entity.LineItemKind;
import com.nkabadi.receiptIQ.entity.Receipt;
import com.nkabadi.receiptIQ.repository.LineItemRepository;
import com.nkabadi.receiptIQ.repository.ReceiptRepository;
import com.nkabadi.receiptIQ.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReceiptService {
	private static final Logger logger = LoggerFactory.getLogger(ReceiptService.class);
	private static final DateTimeFormatter PURCHASE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

	private final ReceiptRepository receiptRepository;
	private final LineItemRepository lineItemRepository;
	private final EmbeddingModel embeddingModel;
	private final String embeddingModelName;

	public ReceiptService(ReceiptRepository receiptRepository, EmbeddingModel embeddingModel, String embeddingModelName) {
		this(receiptRepository, null, embeddingModel, embeddingModelName);
	}

	@Autowired
	public ReceiptService(ReceiptRepository receiptRepository, LineItemRepository lineItemRepository,
			EmbeddingModel embeddingModel,
			@Value("${spring.ai.google.genai.embedding.text.model}") String embeddingModelName) {
		this.receiptRepository = receiptRepository;
		this.lineItemRepository = lineItemRepository;
		this.embeddingModel = embeddingModel;
		this.embeddingModelName = embeddingModelName;
	}

	public Receipt saveReceipt(ReceiptDto receiptDto, String fileName, String rawExtraction) {
		Objects.requireNonNull(receiptDto, "receiptDto must not be null");
		Objects.requireNonNull(fileName, "fileName must not be null");
		Objects.requireNonNull(rawExtraction, "rawExtraction must not be null");

		String merchantName = TextUtils.normalize(receiptDto.getMerchantName());
		String purchaseTimeText = TextUtils.normalize(receiptDto.getPurchaseTime());

		Receipt receipt = new Receipt();
		receipt.setMerchantName(merchantName);
		receipt.setRawExtraction(rawExtraction);
		receipt.setPurchaseDate(receiptDto.getPurchaseDate());
		receipt.setTotal(receiptDto.getTotal());
		receipt.setImagePath(fileName);

		boolean invalidPurchaseTime = false;
		if (purchaseTimeText != null) {
			try {
				receipt.setPurchaseTime(LocalTime.parse(purchaseTimeText, PURCHASE_TIME_FORMATTER));
			} catch (DateTimeParseException exception) {
				invalidPurchaseTime = true;
			}
		}

		List<LineItemDto> lineItemDtos = receiptDto.getItems() == null
				? List.of()
				: receiptDto.getItems();
		BigDecimal lineItemsTotal = BigDecimal.ZERO;
		boolean hasMissingAmount = false;

		for (LineItemDto lineItemDto : lineItemDtos) {
			String description = TextUtils.normalize(lineItemDto.getDescription());
			String productCode = TextUtils.normalize(lineItemDto.getProductCode());
			String plainEngDesc = TextUtils.normalize(lineItemDto.getPlainEngDesc());

			CategoryKind category = lineItemDto.getCategory();
			if (category == null && lineItemDto.getKind() != null) {
				category = lineItemDto.getKind().getDefaultCategory();
			}

			LineItem lineItem = new LineItem();
			lineItem.setDescription(description);
			lineItem.setPlainEngDesc(plainEngDesc);
			lineItem.setQuantity(lineItemDto.getQuantity());
			lineItem.setUnitPrice(lineItemDto.getUnitPrice());
			lineItem.setAmount(lineItemDto.getAmount());
			lineItem.setProductCode(productCode);
			lineItem.setCategory(category);
			lineItem.setKind(lineItemDto.getKind());
			if (lineItemDto.getKind() == LineItemKind.ITEM) {
				lineItem.setEmbeddingText(buildEmbeddingText(lineItem, merchantName));
			}
			receipt.addItem(lineItem);

			if (lineItemDto.getAmount() == null) {
				hasMissingAmount = true;
			} else {
				lineItemsTotal = lineItemsTotal.add(lineItemDto.getAmount());
			}
		}

		List<String> embeddingTexts = new ArrayList<>();
		List<LineItem> itemsToEmbed = new ArrayList<>();
		for (LineItem item : receipt.getItems()) {
			if (item.getEmbeddingText() != null && !item.getEmbeddingText().isBlank()) {
				embeddingTexts.add(item.getEmbeddingText());
				itemsToEmbed.add(item);
			}
		}
		if (!embeddingTexts.isEmpty()) {
			try {
				List<float[]> embeddings = embeddingModel.embed(embeddingTexts);
				for (int index = 0; index < Math.min(embeddings.size(), itemsToEmbed.size()); index++) {
					float[] embedding = embeddings.get(index);
					if (embedding != null) {
						LineItem item = itemsToEmbed.get(index);
						item.setEmbedding(embedding);
						item.setEmbeddingModel(embeddingModelName);
					}
				}
			} catch (Exception exception) {
				logger.warn("Failed to generate line item embeddings; saving receipt without vectors.", exception);
			}
		}

		boolean needsReview = receiptDto.getTotal() == null
		        || receiptDto.getPurchaseDate() == null
				|| invalidPurchaseTime
				|| hasMissingAmount
				|| lineItemsTotal.compareTo(receiptDto.getTotal()) != 0;
		receipt.setNeedsReview(needsReview);

		return receiptRepository.save(receipt);
	}

	public int backfillEmbeddings() {
		List<LineItem> rows = lineItemRepository.findForEmbeddingBackfill(embeddingModelName);
		int updated = 0;
		int failedBatches = 0;
		int batchSize = 20;

		logger.info("Embedding backfill found {} rows.", rows.size());
		for (int start = 0; start < rows.size(); start += batchSize) {
			int end = Math.min(start + batchSize, rows.size());
			List<LineItem> batch = rows.subList(start, end);
			List<float[]> previousEmbeddings = batch.stream().map(LineItem::getEmbedding).toList();
			List<String> previousModels = batch.stream().map(LineItem::getEmbeddingModel).toList();
			try {
				List<String> embeddingTexts = batch.stream()
						.map(LineItem::getEmbeddingText)
						.toList();
				List<float[]> embeddings = embeddingModel.embed(embeddingTexts);
				int batchUpdated = 0;

				for (int index = 0; index < Math.min(embeddings.size(), batch.size()); index++) {
					float[] embedding = embeddings.get(index);
					if (embedding != null) {
						LineItem item = batch.get(index);
						item.setEmbedding(embedding);
						item.setEmbeddingModel(embeddingModelName);
						batchUpdated++;
					}
				}

				lineItemRepository.saveAll(batch);
				updated += batchUpdated;
			} catch (Exception exception) {
				failedBatches++;
				for (int index = 0; index < batch.size(); index++) {
					batch.get(index).setEmbedding(previousEmbeddings.get(index));
					batch.get(index).setEmbeddingModel(previousModels.get(index));
				}
				logger.warn("Embedding backfill batch failed and was skipped; {} rows skipped.", batch.size(), exception);
			}
		}

		logger.info("Embedding backfill updated {} rows; {} batches failed.", updated, failedBatches);
		return updated;
	}

	private String buildEmbeddingText(LineItem lineItem, String merchentName) {
		String category = lineItem.getCategory() == null ? null : lineItem.getCategory().name();
		String description = TextUtils.normalize(lineItem.getDescription());
		String plainEngDesc = TextUtils.normalize(lineItem.getPlainEngDesc());

		return String.join(" ", java.util.stream.Stream.of(merchentName, category, description, plainEngDesc)
				.filter(Objects::nonNull)
				.filter(value -> !value.isBlank())
				.toList());
	}
}
