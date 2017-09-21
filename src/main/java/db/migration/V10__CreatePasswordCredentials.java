package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V10__CreatePasswordCredentials implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("password_credentials"))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("password", SQLDataType.VARBINARY(256).nullable(false)))
                    .column(field("salt", SQLDataType.VARCHAR(16).nullable(false)))
                    .column(field("created_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id")),
                            constraint().foreignKey(field("user_id")).references(table("users"), field("user_id"))
                    ).getSQL();
            stmt.execute(ddl);

            ddl = create.createTable(table("otp_keys"))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))//
                    .column(field("otp_key", SQLDataType.BINARY(20).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id")),
                            constraint().foreignKey(field("user_id")).references(table("users"), field("user_id"))
                    ).getSQL();
            stmt.execute(ddl);
        }

    }
}
