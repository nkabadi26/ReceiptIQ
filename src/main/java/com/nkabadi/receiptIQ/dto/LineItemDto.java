package com.nkabadi.receiptIQ.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.nkabadi.receiptIQ.entity.CategoryKind;
import com.nkabadi.receiptIQ.entity.LineItemKind;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

public class LineItemDto {
	@JsonPropertyDescription("Item or charge description as printed on the receipt, without product codes, UPC or SKU numbers.")
	private String description;

	@JsonPropertyDescription("A plain-English name for this item, expanded from the abbreviated text printed on the receipt, using the merchant name and that retailer's usual abbreviation style as context (for example, 'GV ORG 2LB' at Walmart is a Great Value organic product). Only fill this when you are reasonably confident of what the item is. Do not guess. If the printed text is already readable, repeat it. If you cannot tell, omit this field.")
	@Nullable
	private String plainEngDesc;

	@JsonPropertyDescription("Quantity shown for this line; use 1 if no quantity is printed.")
	private BigDecimal quantity;

	@JsonPropertyDescription("Unit price as printed; omit if not shown.")
	@Nullable
	private BigDecimal unitPrice;

	@JsonPropertyDescription("Line amount exactly as printed; use a negative value for discounts.")
	private BigDecimal amount;

	@JsonPropertyDescription("Classification of this line item. Use DISCOUNT only for a separate deduction that changes the amount paid, such as a coupon or a voided item with a negative amount. Never use DISCOUNT for a 'You saved' or 'Total savings' note when the item price is already the final price. All line amounts must add up to the receipt total.")
	private LineItemKind kind;

	@JsonPropertyDescription("Product code printed on that line, such as a UPC, SKU, or retailer item code. Never use a transaction, receipt, invoice, order, or tracking number. If the line has no product code, omit this field.")
	@Nullable
	private String productCode;

	@JsonPropertyDescription("Best matching category for the purchased item from the CategoryKind enum. Use OTHER when no category fits confidently; return null for non-item line types such as tax, discount, fee, tip, deposit, or rounding.")
	@Nullable
	private CategoryKind category;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPlainEngDesc() {
		return plainEngDesc;
	}

	public void setPlainEngDesc(String plainEngDesc) {
		this.plainEngDesc = plainEngDesc;
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

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public CategoryKind getCategory() {
		return category;
	}

	public void setCategory(CategoryKind category) {
		this.category = category;
	}
}
