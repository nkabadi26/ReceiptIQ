package com.nkabadi.receiptIQ.repository;

import com.nkabadi.receiptIQ.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    @Query(value = """
            select count(*), coalesce(sum(r.total), 0)
            from receipts r
            where lower(r.merchant_name) like lower(concat('%', :merchant, '%'))
              and (cast(:fromDate as date) is null or r.purchase_date >= cast(:fromDate as date))
              and (cast(:toDate as date) is null or r.purchase_date <= cast(:toDate as date))
            """, nativeQuery = true)
    List<Object[]> findReceiptStats(@Param("merchant") String merchant,
                                    @Param("fromDate") LocalDate fromDate,
                                    @Param("toDate") LocalDate toDate);
}
