package com.nkabadi.receiptIQ.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

@Entity
@Table(name = "line_items")
public class LineItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "receipt_id")
	private Receipt receipt;

	private String description;

	@Column(precision = 10, scale = 3)
	private BigDecimal quantity = BigDecimal.ONE;

	@Column(precision = 10, scale = 3)
	private BigDecimal unitPrice;

	@Column(precision = 10, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	private LineItemKind kind;

	@Column(nullable = true)
	private String productCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private CategoryKind category;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@JsonIgnore
	public Receipt getReceipt() {
		return receipt;
	}

	public void setReceipt(Receipt receipt) {
		this.receipt = receipt;
	}

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

	@Override
	public String toString() {
		return "LineItem{" +
				"id=" + id +
				", description='" + description + '\'' +
				", quantity=" + quantity +
				", unitPrice=" + unitPrice +
				", amount=" + amount +
				", kind=" + kind +
				'}';
	}
}
