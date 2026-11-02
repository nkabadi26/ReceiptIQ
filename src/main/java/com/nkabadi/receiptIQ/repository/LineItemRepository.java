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
		       li.embedding <=> cast(:q as vector) as distance
		from line_items li
		join receipts r on r.id = li.receipt_id
		where li.embedding is not null
		order by distance
		limit :limit
		""", nativeQuery = true)
	List<Object[]> findNearest(@Param("q") String queryVector, @Param("limit") int limit);
}
