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
                    + "name VARCHAR(50) NOT NULL,"
                    + "api_key VARCHAR(100) NOT NULL,"
                    + "api_secret VARCHAR(100),"
                    + "scope VARCHAR(100),"
                    + "state VARCHAR(100),"
                    + "response_type VARCHAR(100),"
                    + "access_token_endpoint VARCHAR(100),"
                    + "authorization_base_url VARCHAR(100),"
                    + "user_info_endpoint VARCHAR(100),"
                    + "user_id_path VARCHAR(100),"
                    + "user_agent VARCHAR(100),"
                    + "PRIMARY KEY(oauth2_provider_id)"
                    + ")"
            );
        }
    }
}
