package com.health.system.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("health_migration_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void migrateShouldSucceedWhenFeedbackReplyColumnsAlreadyExist() throws SQLException {
        String schema = MYSQL.getDatabaseName();

        withSchemaConnection(schema, conn -> {
            resetFlywayTargetTables(conn);
            execute(conn, """
                CREATE TABLE feedback_message (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    sender_user_id BIGINT NOT NULL,
                    sender_username VARCHAR(64) NOT NULL,
                    sender_role_type VARCHAR(20) NOT NULL,
                    content VARCHAR(500) NOT NULL,
                    status TINYINT NOT NULL DEFAULT 0,
                    reply_content VARCHAR(500) NULL,
                    replied_time DATETIME NULL,
                    reply_read TINYINT NOT NULL DEFAULT 0,
                    reply_read_time DATETIME NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
        });

        Flyway flyway = newFlyway(schema);
        flyway.migrate();

        withSchemaConnection(schema, conn -> {
            assertTrue(hasColumn(conn, "feedback_message", "reply_content"));
            assertTrue(hasColumn(conn, "feedback_message", "replied_time"));
            assertTrue(hasColumn(conn, "feedback_message", "reply_read"));
            assertTrue(hasColumn(conn, "feedback_message", "reply_read_time"));
            assertEquals(1, versionCount(conn, "11"));
            assertEquals(1, versionCount(conn, "12"));
        });
    }

    @Test
    void migrateShouldAddReplyColumnsWhenFeedbackTableExistsWithoutThem() throws SQLException {
        String schema = MYSQL.getDatabaseName();

        withSchemaConnection(schema, conn -> {
            resetFlywayTargetTables(conn);
            execute(conn, """
                CREATE TABLE feedback_message (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    sender_user_id BIGINT NOT NULL,
                    sender_username VARCHAR(64) NOT NULL,
                    sender_role_type VARCHAR(20) NOT NULL,
                    content VARCHAR(500) NOT NULL,
                    status TINYINT NOT NULL DEFAULT 0,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
        });

        Flyway flyway = newFlyway(schema);
        flyway.migrate();

        withSchemaConnection(schema, conn -> {
            assertTrue(hasColumn(conn, "feedback_message", "reply_content"));
            assertTrue(hasColumn(conn, "feedback_message", "replied_time"));
            assertTrue(hasColumn(conn, "feedback_message", "reply_read"));
            assertTrue(hasColumn(conn, "feedback_message", "reply_read_time"));
            assertEquals(1, versionCount(conn, "11"));
            assertEquals(1, versionCount(conn, "12"));
        });
    }

    private Flyway newFlyway(String schema) {
        return Flyway.configure()
                .dataSource(schemaJdbcUrl(schema), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineVersion(MigrationVersion.fromVersion("10"))
                .baselineOnMigrate(true)
                .load();
    }

    private void withSchemaConnection(String schema, SqlConsumer consumer) throws SQLException {
        String schemaUrl = schemaJdbcUrl(schema);
        try (Connection conn = DriverManager.getConnection(schemaUrl, MYSQL.getUsername(), MYSQL.getPassword())) {
            consumer.accept(conn);
        }
    }

    private String schemaJdbcUrl(String schema) {
        String url = MYSQL.getJdbcUrl();
        int slash = url.indexOf('/', "jdbc:mysql://".length());
        int query = url.indexOf('?');
        if (slash < 0 || query < 0 || query <= slash) {
            return url;
        }
        return url.substring(0, slash + 1) + schema + url.substring(query);
    }

    private void execute(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void resetFlywayTargetTables(Connection conn) throws SQLException {
        execute(conn, "DROP TABLE IF EXISTS flyway_schema_history");
        execute(conn, "DROP TABLE IF EXISTS feedback_message");
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
              AND COLUMN_NAME = ?
            """;
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private int versionCount(Connection conn, String version) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT COUNT(*) FROM flyway_schema_history WHERE version = ?")) {
            ps.setString(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
