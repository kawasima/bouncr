package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V20__CreateCerts implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE certs("
                    + "cert_id IDENTITY,"
                    + "user_id BIGINT NOT NULL,"
                    + "serial BIGINT NOT NULL,"
                    + "private_key BLOB NOT NULL,"
                    + "client_cert BLOB NOT NULL,"
                    + "expires DATE NOT NULL,"
                    + "PRIMARY KEY (cert_id),"
                    + "FOREIGN KEY (user_id) REFERENCES users(user_id)"
                    + ")");
        }
    }
}
