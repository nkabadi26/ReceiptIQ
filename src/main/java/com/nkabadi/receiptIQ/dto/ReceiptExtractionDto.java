package com.nkabadi.receiptIQ.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.nkabadi.receiptIQ.entity.LineItemKind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReceiptExtractionDto {
	@JsonPropertyDescription("Merchant or store name printed on the receipt; use null if not visible.")
	private String merchantName;

	@JsonPropertyDescription("Purchase date in ISO format yyyy-MM-dd. Convert printed US-style dates in MM/DD/YY format; use null if not visible.")
	private LocalDate purchaseDate;

	@JsonPropertyDescription("Purchase time in HH:mm:ss format; use null if not visible.")
	private LocalTime purchaseTime;

	@JsonPropertyDescription("Receipt total exactly as printed.")
	private BigDecimal total;

	@JsonPropertyDescription("Set true if any extracted value is unclear or uncertain and needs human review; otherwise false.")
	private boolean needsReview;

	@JsonPropertyDescription("All receipt line items, including items, taxes, discounts, fees, tips, deposits, rounding adjustments, and other charges.")
	private List<LineItemDto> items = new ArrayList<>();

	public String getMerchantName() {
		return merchantName;
	}

	public void setMerchantName(String merchantName) {
		this.merchantName = merchantName;
	}

	public LocalDate getPurchaseDate() {
		return purchaseDate;
	}

	public void setPurchaseDate(LocalDate purchaseDate) {
		this.purchaseDate = purchaseDate;
	}

	public LocalTime getPurchaseTime() {
		return purchaseTime;
	}

	public void setPurchaseTime(LocalTime purchaseTime) {
		this.purchaseTime = purchaseTime;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public boolean isNeedsReview() {
		return needsReview;
	}

	public void setNeedsReview(boolean needsReview) {
		this.needsReview = needsReview;
	}

	public List<LineItemDto> getItems() {
		return items;
	}

	public void setItems(List<LineItemDto> items) {
		this.items = items;
	}

	public static class LineItemDto {
		@JsonPropertyDescription("Item or charge description as printed on the receipt.")
		private String description;

		@JsonPropertyDescription("Quantity shown for this line; use 1 if no quantity is printed.")
		private BigDecimal quantity;

		@JsonPropertyDescription("Unit price as printed; use null if not shown.")
		private BigDecimal unitPrice;

		@JsonPropertyDescription("Line amount exactly as printed; use a negative value for discounts.")
		private BigDecimal amount;

		@JsonPropertyDescription("Classify this line using ITEM, TAX, DISCOUNT, FEE, TIP, DEPOSIT, ROUNDING, or OTHER.")
		private LineItemKind kind;

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public BigDecimal getQuantity() {
			return quantity;
		}

		public void setQuantity(BigDecimal quantity) {
			this.quantity = quantity;
		}

		public BigDecimal getUnitPrice() {
			return unitPrice;
		}

		public void setUnitPrice(BigDecimal unitPrice) {
			this.unitPrice = unitPrice;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public LineItemKind getKind() {
			return kind;
		}

		public void setKind(LineItemKind kind) {
			this.kind = kind;
		}
	}
}
