package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V12__CreateSignInHistories implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE sign_in_histories("
                    + "sign_in_id IDENTITY,"
                    + "signed_in_at TIMESTAMP NOT NULL,"
                    + "account VARCHAR(100),"
                    + "successful BOOLEAN,"
                    + "PRIMARY KEY (sign_in_id)"
                    + ")");
        }
    }
}
