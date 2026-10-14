package com.enterprise.knowledge.migration;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeedPasswordMigrationTest {

    private static final Map<String, String> ORIGINAL_HASHES = Map.of(
            "admin", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lihO",
            "alice", "$2a$10$TLbNhBeMayBiMDXPdnBfT.kA4tgzSBJXKYa0ohtGFq4sEXDEJFNZK",
            "hrmanager", "$2a$10$DO0Kz9UKvj5qDHKsJoQRde7qSg5yX8YoHh6IfZ30cJB4xzIGELj2q");
    private static final Map<String, String> REPAIRED_HASHES = Map.of(
            "admin", "$2a$10$gaQRR.3Bbg.bs0REIyUzueDZkqZlfmUVYM0tZjnAw0modUgVO9kvq",
            "alice", "$2a$10$cY5psd0Op5xB9Yk1z3xUaOmkHIRPEQTd1xTlyRT5C9oYGkoIsE3AK",
            "hrmanager", "$2a$10$sBLKjIWUmuR44RicG9xvJu5beR0r2E9.8DByv.we7pplWyL9FYMvW");

    @Test
    void migrationRepairsOnlyUnchangedV2SeedHashes() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:seed-migration-" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection connection = dataSource.getConnection();
                PreparedStatement create = connection.prepareStatement(
                        "CREATE TABLE app_user (username VARCHAR(100) PRIMARY KEY, password_hash VARCHAR(255) NOT NULL)")) {
            create.execute();
            insertUser(connection, "admin", ORIGINAL_HASHES.get("admin"));
            insertUser(connection, "alice", "a-password-hash-changed-after-v2");
            insertUser(connection, "hrmanager", ORIGINAL_HASHES.get("hrmanager"));
        }

        Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:src/main/resources/db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("2")
                .load()
                .migrate();

        assertEquals(REPAIRED_HASHES.get("admin"), passwordHash(dataSource, "admin"));
        assertEquals("a-password-hash-changed-after-v2", passwordHash(dataSource, "alice"));
        assertEquals(REPAIRED_HASHES.get("hrmanager"), passwordHash(dataSource, "hrmanager"));
    }

    private void insertUser(Connection connection, String username, String passwordHash) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO app_user (username, password_hash) VALUES (?, ?)")) {
            insert.setString(1, username);
            insert.setString(2, passwordHash);
            insert.executeUpdate();
        }
    }

    private String passwordHash(JdbcDataSource dataSource, String username) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement query = connection.prepareStatement(
                        "SELECT password_hash FROM app_user WHERE username = ?")) {
            query.setString(1, username);
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("Missing migration fixture user: " + username);
                }
                return result.getString(1);
            }
        }
    }
}
