package com.nkabadi.receiptIQ.repository;

import com.nkabadi.receiptIQ.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
}
