package com.nkabadi.receiptIQ.repository;

import java.util.List;

import com.nkabadi.receiptIQ.entity.LineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LineItemRepository extends JpaRepository<LineItem, Long> {
	@Query(value = """
		select li.id,
		       r.merchant_name,
		       r.purchase_date,
		       li.category,
		       li.description,
		       li.plain_eng_desc,
		       li.amount,
		       li.embedding <=> cast(:q as vector) as distance,
		       r.id as receipt_id,
		       li.product_code
		from line_items li
		join receipts r on r.id = li.receipt_id
		where li.embedding is not null
		  and li.kind = 'ITEM'
		order by distance
		limit :limit
		""", nativeQuery = true)
	List<Object[]> findNearest(@Param("q") String queryVector, @Param("limit") int limit);

	@Query(value = """
		select li.receipt_id, li.product_code, li.amount
		from line_items li
		where li.receipt_id in (:receiptIds)
		  and li.kind = 'DISCOUNT'
		""", nativeQuery = true)
	List<Object[]> findDiscounts(@Param("receiptIds") List<Long> receiptIds);
}
