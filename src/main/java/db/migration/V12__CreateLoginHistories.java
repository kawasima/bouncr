package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class V12__CreateLoginHistories implements JdbcMigration {
    private static final String SQL = "CREATE TABLE login_histories("
            + "login_id IDENTITY,"
            + "logined_at TIMESTAMP NOT NULL,"
            + "PRIMARY KEY (login_id)"
            + ")";
    @Override
    public void migrate(Connection connection) throws Exception {
        try(PreparedStatement stmt = connection.prepareStatement(SQL)) {

        }
    }
}
