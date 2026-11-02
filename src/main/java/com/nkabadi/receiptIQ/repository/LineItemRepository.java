package com.nkabadi.receiptIQ.repository;

import java.util.List;

import com.nkabadi.receiptIQ.entity.LineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LineItemRepository extends JpaRepository<LineItem, Long> {
	@Query(value = "select id, embedding_text, embedding <=> cast(:q as vector) as distance from line_items where embedding is not null order by distance limit 5", nativeQuery = true)
	List<Object[]> findNearest(@Param("q") String queryVector);
}
