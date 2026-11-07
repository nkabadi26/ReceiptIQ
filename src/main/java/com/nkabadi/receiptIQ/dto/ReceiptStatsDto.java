package com.nkabadi.receiptIQ.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiptStatsDto(String merchant, LocalDate from, LocalDate to, long receiptCount,
                              BigDecimal totalPaid) {
}
