package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.dto.SearchCategoryCount;
import com.enterprise.knowledge.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    @Query("SELECT COUNT(s) FROM SearchHistory s")
    long countAll();

    @Query("""
            SELECT new com.enterprise.knowledge.dto.SearchCategoryCount(
                s.categoryFilter, COUNT(s))
            FROM SearchHistory s
            WHERE s.categoryFilter IS NOT NULL
            GROUP BY s.categoryFilter
            ORDER BY COUNT(s) DESC, s.categoryFilter ASC
            """)
    List<SearchCategoryCount> countSearchesBySelectedCategory();
}
