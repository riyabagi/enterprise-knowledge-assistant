package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.entity.KnowledgeChunk;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public class KnowledgeChunkSearchRepositoryImpl implements KnowledgeChunkSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<KnowledgeChunk> fulltextSearch(String query, Collection<String> accessLevels, Pageable pageable) {
        return search(query, accessLevels, null, pageable);
    }

    @Override
    public long countFulltextSearch(String query, Collection<String> accessLevels) {
        return count(query, accessLevels, null);
    }

    @Override
    public List<KnowledgeChunk> fulltextSearchWithCategory(
            String query, Collection<String> accessLevels, String category, Pageable pageable) {
        return search(query, accessLevels, category, pageable);
    }

    @Override
    public long countFulltextSearchWithCategory(
            String query, Collection<String> accessLevels, String category) {
        return count(query, accessLevels, category);
    }

    @Override
    public List<KnowledgeChunk> fulltextDocumentSearch(
            String query, Collection<String> accessLevels, String category, Pageable pageable) {
        return uniqueDocumentSearch(query, accessLevels, category, pageable, false, true);
    }

    @Override
    public long countFulltextDocumentSearch(
            String query, Collection<String> accessLevels, String category) {
        return uniqueDocumentCount(query, accessLevels, category, false, true);
    }

    @Override
    public List<KnowledgeChunk> fallbackDocumentSearch(
            String query, Collection<String> accessLevels, String category, Pageable pageable) {
        return uniqueDocumentSearch(query, accessLevels, category, pageable, true, true);
    }

    @Override
    public long countFallbackDocumentSearch(
            String query, Collection<String> accessLevels, String category) {
        return uniqueDocumentCount(query, accessLevels, category, true, true);
    }

    @Override
    public List<KnowledgeChunk> fallbackContentSearch(
            String query, Collection<String> accessLevels, Pageable pageable) {
        return uniqueDocumentSearch(query, accessLevels, null, pageable, true, false);
    }

    private List<KnowledgeChunk> search(
            String queryText, Collection<String> accessLevels, String category, Pageable pageable) {
        if (accessLevels.isEmpty() || queryText.isBlank()) {
            return List.of();
        }

        SearchSql search = buildSql(queryText, accessLevels, category, false);
        Query query = entityManager.createNativeQuery(search.sql(), KnowledgeChunk.class);
        bind(query, queryText, accessLevels, category, search.terms());
        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());
        @SuppressWarnings("unchecked")
        List<KnowledgeChunk> results = query.getResultList();
        return results;
    }

    private long count(String queryText, Collection<String> accessLevels, String category) {
        if (accessLevels.isEmpty() || queryText.isBlank()) {
            return 0L;
        }

        SearchSql search = buildSql(queryText, accessLevels, category, true);
        Query query = entityManager.createNativeQuery(search.sql());
        bind(query, queryText, accessLevels, category, search.terms());
        return ((Number) query.getSingleResult()).longValue();
    }

    private SearchSql buildSql(
            String queryText, Collection<String> accessLevels, String category, boolean count) {
        boolean mysql = isMySql();
        List<String> terms = terms(queryText);
        String select = count ? "SELECT COUNT(*)" : "SELECT kc.*";
        StringBuilder sql = new StringBuilder(select)
                .append(" FROM knowledge_chunk kc")
                .append(" JOIN document_version dv ON dv.id = kc.document_version_id")
                .append(" JOIN knowledge_document kd ON kd.id = dv.document_id")
                .append(" WHERE dv.status = 'PUBLISHED'")
                .append(" AND kd.status = 'INDEXED'")
                .append(" AND kd.current_version_id = dv.id")
                .append(" AND kd.access_level IN (:accessLevels)");

        if (category != null) {
            sql.append(" AND kd.category = :category");
        }

        if (mysql) {
            sql.append(" AND MATCH(kc.content, kc.searchable_text) AGAINST (:query IN BOOLEAN MODE)");
            if (!count) {
                sql.append(" ORDER BY MATCH(kc.content, kc.searchable_text) AGAINST (:query IN BOOLEAN MODE) DESC");
            }
        } else {
            String searchableText = "LOWER(CONCAT(CAST(kc.content AS VARCHAR), ' ', "
                    + "CAST(kc.searchable_text AS VARCHAR)))";
            sql.append(" AND (");
            for (int i = 0; i < terms.size(); i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append(searchableText).append(" LIKE :term").append(i);
            }
            sql.append(')');
            if (!count) {
                sql.append(" ORDER BY (");
                for (int i = 0; i < terms.size(); i++) {
                    if (i > 0) {
                        sql.append(" + ");
                    }
                    sql.append("(CASE WHEN ").append(searchableText)
                            .append(" LIKE :term").append(i)
                            .append(" THEN 1 ELSE 0 END)");
                }
                sql.append(") DESC, kc.id ASC");
            }
        }
        return new SearchSql(sql.toString(), terms);
    }

    private List<KnowledgeChunk> uniqueDocumentSearch(
            String queryText,
            Collection<String> accessLevels,
            String category,
            Pageable pageable,
            boolean fallback,
            boolean includeMetadata) {
        if (accessLevels.isEmpty() || terms(queryText).isEmpty()) {
            return List.of();
        }

        UniqueSearchSql search = uniqueSearchSql(
                queryText, accessLevels, category, fallback, includeMetadata);
        Query query = entityManager.createNativeQuery(search.sql(), KnowledgeChunk.class);
        bindUnique(query, accessLevels, category, search);
        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());
        @SuppressWarnings("unchecked")
        List<KnowledgeChunk> results = query.getResultList();
        return results;
    }

    private long uniqueDocumentCount(
            String queryText,
            Collection<String> accessLevels,
            String category,
            boolean fallback,
            boolean includeMetadata) {
        if (accessLevels.isEmpty() || terms(queryText).isEmpty()) {
            return 0L;
        }

        UniqueSearchSql search = uniqueSearchSql(
                queryText, accessLevels, category, fallback, includeMetadata);
        Query query = entityManager.createNativeQuery(search.countSql());
        bindUnique(query, accessLevels, category, search);
        return ((Number) query.getSingleResult()).longValue();
    }

    private UniqueSearchSql uniqueSearchSql(
            String queryText,
            Collection<String> accessLevels,
            String category,
            boolean fallback,
            boolean includeMetadata) {
        boolean mysql = isMySql();
        List<String> terms = terms(queryText);
        StringBuilder filters = baseFilters(accessLevels, category);
        List<String> scoreParts = new ArrayList<>();

        if (!fallback && mysql) {
            String match = "MATCH(kc.content, kc.searchable_text) AGAINST (:query IN BOOLEAN MODE)";
            filters.append(" AND ").append(match);
            scoreParts.add(match);
        } else if (!fallback) {
            String content = contentExpression(mysql);
            filters.append(" AND (");
            for (int i = 0; i < terms.size(); i++) {
                appendOr(filters, i);
                filters.append(content).append(" LIKE :term").append(i);
            }
            filters.append(')');
            for (int i = 0; i < terms.size(); i++) {
                scoreParts.add("CASE WHEN " + content + " LIKE :term" + i + " THEN 1 ELSE 0 END");
            }
        } else {
            filters.append(" AND (");
            for (int i = 0; i < terms.size(); i++) {
                appendOr(filters, i);
                appendFallbackTerm(filters, i, includeMetadata);
                scoreParts.add(fallbackScore(i, includeMetadata));
            }
            filters.append(')');
        }

        String score = String.join(" + ", scoreParts);
        String fromAndFilters = " FROM knowledge_chunk kc"
                + " JOIN document_version dv ON dv.id = kc.document_version_id"
                + " JOIN knowledge_document kd ON kd.id = dv.document_id"
                + filters;
        String ranked = "WITH ranked_chunks AS (SELECT kc.id AS chunk_id, "
                + score + " AS relevance, ROW_NUMBER() OVER (PARTITION BY kd.id ORDER BY "
                + score + " DESC, kc.id ASC) AS document_rank"
                + fromAndFilters + ")";
        String sql = ranked + " SELECT kc.* FROM knowledge_chunk kc"
                + " JOIN ranked_chunks ranked ON ranked.chunk_id = kc.id"
                + " WHERE ranked.document_rank = 1"
                + " ORDER BY ranked.relevance DESC, ranked.chunk_id ASC";
        String countSql = ranked
                + " SELECT COUNT(*) FROM ranked_chunks WHERE document_rank = 1";

        return new UniqueSearchSql(sql, countSql, queryText, accessLevels, category, terms, mysql, fallback);
    }

    private StringBuilder baseFilters(Collection<String> accessLevels, String category) {
        StringBuilder filters = new StringBuilder()
                .append(" WHERE dv.status = 'PUBLISHED'")
                .append(" AND kd.status = 'INDEXED'")
                .append(" AND kd.current_version_id = dv.id")
                .append(" AND kd.access_level IN (:accessLevels)");
        if (category != null) {
            filters.append(" AND kd.category = :category");
        }
        return filters;
    }

    private void appendFallbackTerm(StringBuilder sql, int index, boolean includeMetadata) {
        String content = contentExpression(isMySql());
        sql.append('(').append(content).append(" LIKE :term").append(index);
        if (includeMetadata) {
            sql.append(" OR LOWER(COALESCE(kd.title, '')) LIKE :term").append(index)
                    .append(" OR LOWER(COALESCE(kd.description, '')) LIKE :term").append(index);
        }
        sql.append(')');
    }

    private String fallbackScore(int index, boolean includeMetadata) {
        String content = contentExpression(isMySql());
        String score = "CASE WHEN " + content + " LIKE :term" + index + " THEN 2 ELSE 0 END";
        if (includeMetadata) {
            score += " + CASE WHEN LOWER(COALESCE(kd.title, '')) LIKE :term" + index
                    + " OR LOWER(COALESCE(kd.description, '')) LIKE :term" + index
                    + " THEN 1 ELSE 0 END";
        }
        return score;
    }

    private void appendOr(StringBuilder sql, int index) {
        if (index > 0) {
            sql.append(" OR ");
        }
    }

    private String contentExpression(boolean mysql) {
        String castType = mysql ? "CHAR" : "VARCHAR";
        return "LOWER(CONCAT(COALESCE(CAST(kc.content AS " + castType + "), ''), ' ', "
                + "COALESCE(CAST(kc.searchable_text AS " + castType + "), '')))";
    }

    private void bindUnique(Query query, Collection<String> accessLevels, String category, UniqueSearchSql search) {
        query.setParameter("accessLevels", accessLevels);
        if (category != null) {
            query.setParameter("category", category);
        }
        if (search.mysql() && !search.fallback()) {
            query.setParameter("query", search.queryText());
        } else {
            for (int i = 0; i < search.terms().size(); i++) {
                query.setParameter("term" + i, "%" + search.terms().get(i) + "%");
            }
        }
    }

    private boolean isMySql() {
        return entityManager.unwrap(Session.class).doReturningWork(connection -> {
            try {
                return "MySQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
            } catch (SQLException exception) {
                throw new IllegalStateException(
                        "Unable to determine database product for knowledge search.", exception);
            }
        });
    }

    private List<String> terms(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return List.of();
        }
        return List.of(queryText.strip().split("\\s+"));
    }

    private record UniqueSearchSql(
            String sql,
            String countSql,
            String queryText,
            Collection<String> accessLevels,
            String category,
            List<String> terms,
            boolean mysql,
            boolean fallback) {
    }

    private void bind(
            Query query, String queryText, Collection<String> accessLevels,
            String category, List<String> terms) {
        query.setParameter("accessLevels", accessLevels);
        if (category != null) {
            query.setParameter("category", category);
        }
        if ("MySQL".equalsIgnoreCase(databaseProductName())) {
            query.setParameter("query", queryText);
        } else {
            for (int i = 0; i < terms.size(); i++) {
                query.setParameter("term" + i, "%" + terms.get(i).toLowerCase() + "%");
            }
        }
    }

    private String databaseProductName() {
        return entityManager.unwrap(Session.class).doReturningWork(connection -> {
            try {
                return connection.getMetaData().getDatabaseProductName();
            } catch (SQLException exception) {
                throw new IllegalStateException("Unable to determine database product for knowledge search.", exception);
            }
        });
    }

    private record SearchSql(String sql, List<String> terms) {
    }
}
