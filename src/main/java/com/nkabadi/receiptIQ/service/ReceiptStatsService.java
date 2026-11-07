package com.nkabadi.receiptIQ.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.nkabadi.receiptIQ.dto.ReceiptStatsDto;
import com.nkabadi.receiptIQ.repository.ReceiptRepository;
import org.springframework.stereotype.Service;

@Service
public class ReceiptStatsService {
    private final ReceiptRepository receiptRepository;

    public ReceiptStatsService(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    public ReceiptStatsDto getTotal(String merchant, LocalDate from, LocalDate to) {
        List<Object[]> rows = receiptRepository.findReceiptStats(merchant.trim(), from, to);
        Object[] row = rows.getFirst();
        long receiptCount = ((Number) row[0]).longValue();
        BigDecimal totalPaid = row[1] == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString());
        return new ReceiptStatsDto(merchant.trim(), from, to, receiptCount, totalPaid);
    }
}
