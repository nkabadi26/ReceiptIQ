package com.nkabadi.receiptIQ.util;

public final class TextUtils {
	private TextUtils() {
	}

	public static String normalize(String value) {
		if (value == null) {
			return null;
		}

		String trimmedValue = value.trim();
		if (trimmedValue.isEmpty() || trimmedValue.equalsIgnoreCase("null")) {
			return null;
		}
		return trimmedValue;
	}
}
