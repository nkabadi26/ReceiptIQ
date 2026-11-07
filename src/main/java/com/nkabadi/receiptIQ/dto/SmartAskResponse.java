package com.nkabadi.receiptIQ.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SmartAskResponse(String route, String merchant, LocalDate from, LocalDate to,
                               Long receiptCount, BigDecimal totalPaid, String answer, String message) {
}
