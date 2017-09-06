package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V15__CreateOAuth2Users implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE oauth2_users("
                    + "oauth2_provider_id BIGINT NOT NULL,"
                    + "user_id BIGINT NOT NULL,"
                    + "oauth2_account VARCHAR(100) NOT NULL,"
                    + "PRIMARY KEY(oauth2_provider_id, user_id)"
                    + ")");
        }
    }
}
