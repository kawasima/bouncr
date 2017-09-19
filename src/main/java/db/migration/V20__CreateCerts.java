package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V20__CreateCerts implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("certs"))
                    .column(field("cert_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("serial",  SQLDataType.BIGINT.nullable(false)))
                    .column(field("expires", SQLDataType.DATE.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("cert_id")),
                            constraint().foreignKey(field("user_id")).references(table("users"), field("user_id"))
                    ).getSQL();
            stmt.execute(ddl);

            ddl = create.createTable(table("cert_devices"))
                    .column(field("cert_device_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("cert_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("device_token", SQLDataType.VARCHAR(36)))
                    .constraints(
                            constraint().primaryKey(field("cert_device_id")),
                            constraint().foreignKey(field("cert_id")).references(table("certs"), field("cert_id"))
                    )
                    .getSQL();
            stmt.execute(ddl);
        }
    }
}
