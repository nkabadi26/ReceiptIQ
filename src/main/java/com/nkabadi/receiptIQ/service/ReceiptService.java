package com.nkabadi.receiptIQ.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Objects;

import com.nkabadi.receiptIQ.dto.LineItemDto;
import com.nkabadi.receiptIQ.dto.ReceiptDto;
import com.nkabadi.receiptIQ.entity.LineItem;
import com.nkabadi.receiptIQ.entity.Receipt;
import com.nkabadi.receiptIQ.repository.ReceiptRepository;
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

		Receipt receipt = new Receipt();
		receipt.setMerchantName(receiptDto.getMerchantName());
		receipt.setRawExtraction(rawExtraction);
		receipt.setPurchaseDate(receiptDto.getPurchaseDate());
		receipt.setTotal(receiptDto.getTotal());
		receipt.setImagePath(fileName);

		boolean invalidPurchaseTime = false;
		String purchaseTimeText = receiptDto.getPurchaseTime();
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
			LineItem lineItem = new LineItem();
			lineItem.setDescription(lineItemDto.getDescription());
			lineItem.setQuantity(lineItemDto.getQuantity());
			lineItem.setUnitPrice(lineItemDto.getUnitPrice());
			lineItem.setAmount(lineItemDto.getAmount());
			lineItem.setKind(lineItemDto.getKind());
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
}
