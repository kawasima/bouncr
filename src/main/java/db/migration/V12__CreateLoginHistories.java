package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V12__CreateLoginHistories implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE login_histories("
                    + "login_id IDENTITY,"
                    + "logined_at TIMESTAMP NOT NULL,"
                    + "account VARCHAR(100),"
                    + "successful BOOLEAN,"
                    + "PRIMARY KEY (login_id)"
                    + ")");
        }
    }
}
