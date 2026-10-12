package com.nkabadi.receiptIQ.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

	private BigDecimal quantity = BigDecimal.ONE;

	private BigDecimal unitPrice;

	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	private LineItemKind kind;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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
