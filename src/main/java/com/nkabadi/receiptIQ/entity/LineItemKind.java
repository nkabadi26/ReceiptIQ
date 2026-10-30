package com.nkabadi.receiptIQ.entity;

public enum LineItemKind {
	ITEM(null),
	TAX(CategoryKind.TAXES_FEES),
	DISCOUNT(null),
	FEE(null),
	TIP(CategoryKind.TIP),
	DEPOSIT(null),
	ROUNDING(null),
	OTHER(null);

	private final CategoryKind defaultCategory;

	LineItemKind(CategoryKind defaultCategory) {
		this.defaultCategory = defaultCategory;
	}

	public CategoryKind getDefaultCategory() {
		return defaultCategory;
	}
}
