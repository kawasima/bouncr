package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V14__CreateCertificateCredentials implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE certificate_credentials(" +
                    "user_id BIGINT NOT NULL," +
                    "client_dn VARCHAR(150) NOT NULL," +
                    "certificate BLOB NOT NULL," +
                    "PRIMARY KEY(user_id)" +
                    ")");
        }
    }
}
