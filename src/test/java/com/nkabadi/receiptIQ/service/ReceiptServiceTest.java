package com.nkabadi.receiptIQ.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.nkabadi.receiptIQ.dto.LineItemDto;
import com.nkabadi.receiptIQ.dto.ReceiptDto;
import com.nkabadi.receiptIQ.entity.LineItemKind;
import com.nkabadi.receiptIQ.entity.Receipt;
import com.nkabadi.receiptIQ.repository.ReceiptRepository;
import org.junit.jupiter.api.Test;

class ReceiptServiceTest {
	@Test
	void mapsReceiptAndIncludesTaxWhenReconcilingLineAmounts() {
		ReceiptRepository repository = mock(ReceiptRepository.class);
		when(repository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ReceiptService service = new ReceiptService(repository);

		ReceiptDto dto = new ReceiptDto();
		dto.setMerchantName("Market");
		dto.setPurchaseDate(LocalDate.of(2026, 10, 24));
		dto.setPurchaseTime("22:11:15");
		dto.setTotal(new BigDecimal("12.50"));

		LineItemDto item = new LineItemDto();
		item.setDescription("Groceries");
		item.setQuantity(BigDecimal.ONE);
		item.setAmount(new BigDecimal("10.00"));
		item.setKind(LineItemKind.ITEM);

		LineItemDto tax = new LineItemDto();
		tax.setDescription("Tax");
		tax.setAmount(new BigDecimal("2.50"));
		tax.setKind(LineItemKind.TAX);
		dto.setItems(List.of(item, tax));

		Receipt savedReceipt = service.saveReceipt(dto, "receipt.jpg");

		assertEquals("Market", savedReceipt.getMerchantName());
		assertEquals("receipt.jpg", savedReceipt.getImagePath());
		assertEquals(new BigDecimal("12.50"), savedReceipt.getTotal());
		assertEquals(LocalTime.of(22, 11, 15), savedReceipt.getPurchaseTime());
		assertFalse(savedReceipt.isNeedsReview());
		assertEquals(2, savedReceipt.getItems().size());
		assertSame(savedReceipt, savedReceipt.getItems().get(0).getReceipt());
		assertEquals(LineItemKind.TAX, savedReceipt.getItems().get(1).getKind());
		assertNull(savedReceipt.getItems().get(1).getUnitPrice());
	}

	@Test
	void leavesInvalidPurchaseTimeNullAndSetsNeedsReview() {
		ReceiptRepository repository = mock(ReceiptRepository.class);
		when(repository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ReceiptService service = new ReceiptService(repository);

		ReceiptDto dto = new ReceiptDto();
		dto.setPurchaseDate(LocalDate.of(2026, 10, 24));
		dto.setPurchaseTime("22:11:15+00:00");
		dto.setTotal(BigDecimal.ZERO);
		dto.setItems(List.of());

		Receipt savedReceipt = service.saveReceipt(dto, "receipt.jpg");

		assertNull(savedReceipt.getPurchaseTime());
		assertTrue(savedReceipt.isNeedsReview());
	}

	@Test
	void setsNeedsReviewWhenTotalOrPurchaseDateIsMissingOrLineAmountsDoNotReconcile() {
		ReceiptRepository repository = mock(ReceiptRepository.class);
		when(repository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ReceiptService service = new ReceiptService(repository);

		ReceiptDto dto = new ReceiptDto();
		dto.setTotal(new BigDecimal("5.00"));

		LineItemDto lineItem = new LineItemDto();
		lineItem.setDescription("Unclear item");
		lineItem.setKind(LineItemKind.ITEM);
		dto.setItems(List.of(lineItem));

		Receipt savedReceipt = service.saveReceipt(dto, "receipt.jpg");

		assertTrue(savedReceipt.isNeedsReview());
		assertNull(savedReceipt.getPurchaseDate());
		assertNull(savedReceipt.getItems().get(0).getAmount());
	}
}
