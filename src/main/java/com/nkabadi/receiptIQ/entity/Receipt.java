package com.nkabadi.receiptIQ.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receipts")
public class Receipt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String merchantName;

	private LocalDate purchaseDate;

	private LocalTime purchaseTime;

	@Column(precision = 10, scale = 2)
	private BigDecimal total;

	@Column(nullable = false)
	private boolean needsReview = false;

	@CreationTimestamp
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	private String imagePath;

	@Column(columnDefinition = "text")
	private String rawExtraction;

	@OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<LineItem> items = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public String getRawExtraction() {
		return rawExtraction;
	}

	public void setRawExtraction(String rawExtraction) {
		this.rawExtraction = rawExtraction;
	}

	public List<LineItem> getItems() {
		return items;
	}

	public void setItems(List<LineItem> items) {
		this.items = items;
	}

	public void addItem(LineItem item) {
		items.add(item);
		item.setReceipt(this);
	}

	@Override
	public String toString() {
		return "Receipt{" +
				"id=" + id +
				", merchantName='" + merchantName + '\'' +
				", purchaseDate=" + purchaseDate +
				", total=" + total +
				", imagePath='" + imagePath + '\'' +
				", rawExtraction='" + rawExtraction + '\'' +
				'}';
	}
}
