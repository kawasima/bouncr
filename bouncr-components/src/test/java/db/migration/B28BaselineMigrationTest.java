package db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class B28BaselineMigrationTest {
    @Test
    void bPrefixJavaMigrationIsDiscoveredAndApplied() throws Exception {
        String dbName = "b28_" + UUID.randomUUID().toString().replace("-", "");
        String jdbcUrl = "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(
                    "select version, description, success from flyway_schema_history where version = '28'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("version")).isEqualTo("28");
                assertThat(rs.getString("description")).isEqualTo("BouncrV0_3_0");
                assertThat(rs.getBoolean("success")).isTrue();
            }

            try (ResultSet rs = statement.executeQuery("select count(*) from users")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(1);
            }
        }
    }
}
