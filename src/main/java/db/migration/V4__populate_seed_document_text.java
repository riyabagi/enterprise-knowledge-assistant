package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class V4__populate_seed_document_text extends BaseJavaMigration {

    private static final String DEMONSTRATION_NOTICE =
            "Demonstration text assembled from the seeded searchable passages. "
                    + "This is not the original source file.\n\n";

    private static final Map<Long, SeedIdentity> SEED_VERSIONS = Map.of(
            1L, new SeedIdentity("remote-work-policy.txt", "abc001"),
            2L, new SeedIdentity("it-security-guidelines.txt", "abc002"),
            3L, new SeedIdentity("onboarding-handbook.txt", "abc003"),
            4L, new SeedIdentity("salary-bands-confidential.txt", "abc004"),
            5L, new SeedIdentity("q3-it-infra-report.txt", "abc005"));

    @Override
    public void migrate(Context context) throws Exception {
        List<SeedVersion> versions = findMissingSeedVersions(context);
        for (SeedVersion version : versions) {
            String text = buildDemonstrationText(context, version.id());
            if (text != null) {
                storeExtractedText(context, version.id(), text);
            }
        }
    }

    private List<SeedVersion> findMissingSeedVersions(Context context) throws Exception {
        String sql = """
                SELECT id, document_id, version_number, original_filename, checksum
                FROM document_version
                WHERE id BETWEEN 1 AND 5
                  AND (extracted_text IS NULL OR TRIM(extracted_text) = '')
                """;
        List<SeedVersion> versions = new ArrayList<>();
        try (PreparedStatement statement = context.getConnection().prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                long id = rows.getLong("id");
                SeedIdentity identity = SEED_VERSIONS.get(id);
                if (identity != null
                        && rows.getLong("document_id") == id
                        && rows.getInt("version_number") == 1
                        && identity.filename().equals(rows.getString("original_filename"))
                        && identity.checksum().equals(rows.getString("checksum"))) {
                    versions.add(new SeedVersion(id));
                }
            }
        }
        return versions;
    }

    private String buildDemonstrationText(Context context, long versionId) throws Exception {
        String sql = """
                SELECT section_reference, content
                FROM knowledge_chunk
                WHERE document_version_id = ?
                ORDER BY chunk_index
                """;
        StringBuilder text = new StringBuilder(DEMONSTRATION_NOTICE);
        boolean hasPassages = false;
        try (PreparedStatement statement = context.getConnection().prepareStatement(sql)) {
            statement.setLong(1, versionId);
            try (ResultSet chunks = statement.executeQuery()) {
                while (chunks.next()) {
                    if (hasPassages) {
                        text.append("\n\n");
                    }
                    String section = chunks.getString("section_reference");
                    if (section != null && !section.isBlank()) {
                        text.append(section).append('\n');
                    }
                    text.append(chunks.getString("content"));
                    hasPassages = true;
                }
            }
        }
        return hasPassages ? text.toString() : null;
    }

    private void storeExtractedText(Context context, long versionId, String text) throws Exception {
        String sql = """
                UPDATE document_version
                SET extracted_text = ?
                WHERE id = ?
                  AND (extracted_text IS NULL OR TRIM(extracted_text) = '')
                """;
        try (PreparedStatement statement = context.getConnection().prepareStatement(sql)) {
            statement.setString(1, text);
            statement.setLong(2, versionId);
            statement.executeUpdate();
        }
    }

    private record SeedIdentity(String filename, String checksum) {
    }

    private record SeedVersion(long id) {
    }
}
