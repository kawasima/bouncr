package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V13__CreateOAuth2Providers implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE oauth2_providers("
                    + "oauth2_provider_id IDENTITY,"
                    + "api_key VARCHAR(100) NOT NULL,"
                    + "api_secret VARCHAR(100),"
                    + "scope VARCHAR(100),"
                    + "state VARCHAR(100),"
                    + "response_type VARCHAR(100),"
                    + "user_agent VARCHAR(100),"
                    + "PRIMARY KEY(oauth2_provider_id)"
                    + ")"
            );
        }
    }
}
