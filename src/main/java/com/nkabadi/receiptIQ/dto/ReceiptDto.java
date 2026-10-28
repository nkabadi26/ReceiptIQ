package com.nkabadi.receiptIQ.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReceiptDto {
	@JsonPropertyDescription("Merchant or store name printed on the receipt; omit if not visible.")
	@Nullable
	private String merchantName;

	@JsonPropertyDescription("Purchase date in ISO format yyyy-MM-dd. Convert printed US-style dates in MM/DD/YY format; omit if not visible.")
	@Nullable
	private LocalDate purchaseDate;

	@JsonPropertyDescription("Purchase time as HH:mm:ss only, exactly as printed on the receipt. No timezone, no offset, no fractional seconds. Omit if not visible.")
	@Nullable
	private String purchaseTime;

	@JsonPropertyDescription("Receipt total exactly as printed; omit if unreadable.")
	@Nullable
	private BigDecimal total;

	@JsonIgnore
	private boolean needsReview;

	@JsonPropertyDescription("All receipt line items, including items, taxes, discounts, fees, tips, deposits, rounding adjustments, and other charges. Return an empty list if none are present or readable.")
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

	public String getPurchaseTime() {
		return purchaseTime;
	}

	public void setPurchaseTime(String purchaseTime) {
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
}
