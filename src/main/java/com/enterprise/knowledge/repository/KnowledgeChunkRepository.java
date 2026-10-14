package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long>, KnowledgeChunkSearchRepository {
    List<KnowledgeChunk> findByDocumentVersionId(Long documentVersionId);

    long countByDocumentVersionIdIn(Collection<Long> documentVersionIds);

    List<KnowledgeChunk> findByIdIn(Collection<Long> ids);

    @Modifying
    @Query("DELETE FROM KnowledgeChunk c WHERE c.documentVersion.id IN :versionIds")
    int deleteByDocumentVersionIds(@Param("versionIds") Collection<Long> versionIds);
}
