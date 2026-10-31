package com.nkabadi.receiptIQ.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Objects;

import com.nkabadi.receiptIQ.dto.LineItemDto;
import com.nkabadi.receiptIQ.dto.ReceiptDto;
import com.nkabadi.receiptIQ.entity.CategoryKind;
import com.nkabadi.receiptIQ.entity.LineItem;
import com.nkabadi.receiptIQ.entity.LineItemKind;
import com.nkabadi.receiptIQ.entity.Receipt;
import com.nkabadi.receiptIQ.repository.ReceiptRepository;
import com.nkabadi.receiptIQ.util.TextUtils;
import org.springframework.stereotype.Service;

@Service
public class ReceiptService {
	private static final DateTimeFormatter PURCHASE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

	private final ReceiptRepository receiptRepository;

	public ReceiptService(ReceiptRepository receiptRepository) {
		this.receiptRepository = receiptRepository;
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

		boolean needsReview = receiptDto.getTotal() == null
		        || receiptDto.getPurchaseDate() == null
				|| invalidPurchaseTime
				|| hasMissingAmount
				|| lineItemsTotal.compareTo(receiptDto.getTotal()) != 0;
		receipt.setNeedsReview(needsReview);

		return receiptRepository.save(receipt);
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
