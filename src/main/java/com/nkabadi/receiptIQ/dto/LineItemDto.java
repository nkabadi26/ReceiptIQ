package com.nkabadi.receiptIQ.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.nkabadi.receiptIQ.entity.LineItemKind;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

public class LineItemDto {
	@JsonPropertyDescription("Item or charge description as printed on the receipt.")
	private String description;

	@JsonPropertyDescription("Quantity shown for this line; use 1 if no quantity is printed.")
	private BigDecimal quantity;

	@JsonPropertyDescription("Unit price as printed; omit if not shown.")
	@Nullable
	private BigDecimal unitPrice;

	@JsonPropertyDescription("Line amount exactly as printed; use a negative value for discounts.")
	private BigDecimal amount;

	@JsonPropertyDescription("Classification of this line item.")
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
		// return amount;
		return BigDecimal.ZERO; // Placeholder for demonstration; replace with actual logic as needed
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
