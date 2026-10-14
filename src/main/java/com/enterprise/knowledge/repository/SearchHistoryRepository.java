package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    @Query("SELECT COUNT(s) FROM SearchHistory s")
    long countAll();
}
