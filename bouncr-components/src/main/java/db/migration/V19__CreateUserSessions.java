package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V19__CreateUserSessions implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("user_sessions"))
                    .column(field("user_session_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("token", SQLDataType.VARCHAR(36).nullable(false)))
                    .column(field("remote_address", SQLDataType.VARCHAR(255)))
                    .column(field("user_agent", SQLDataType.VARCHAR(255)))
                    .column(field("created_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_session_id")),
                            constraint().unique(field("token")),
                            constraint().foreignKey(field("user_id")).references(table("users"), field("user_id"))
                    ).getSQL();
            stmt.execute(ddl);
        }
    }
}
