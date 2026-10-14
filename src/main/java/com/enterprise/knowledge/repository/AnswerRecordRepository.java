package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.entity.AnswerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, Long> {
    @Query("SELECT COUNT(a) FROM AnswerRecord a WHERE a.status = 'ANSWERED'")
    long countAnswered();
}
