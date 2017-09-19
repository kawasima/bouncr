package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V14__CreateCertificateCredentials implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("certificate_credentials"))
                    .column(field("user_id", SQLDataType.BIGINT))
                    .column(field("client_dn", SQLDataType.VARCHAR(150).nullable(false)))
                    .column(field("certificate", SQLDataType.BLOB.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id")),
                            constraint().foreignKey(field("user_id")).references(table("users"), field("user_id"))
                    ).getSQL();
            stmt.execute(ddl);
        }
    }
}
