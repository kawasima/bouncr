package db.migration;

import net.unit8.bouncr.web.entity.ActionType;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V12__CreateUserActions implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        DSLContext create = DSL.using(connection);
        try(Statement stmt = connection.createStatement()) {
            String ddl = create.createTable(table("actions"))
                    .column(field("action_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("action_id")),
                            constraint().unique(field("name"))
                    ).getSQL();
            stmt.execute(ddl);

            ddl = create.createTable(table("user_actions"))
                    .column(field("user_action_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("action_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("actor", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("actor_ip", SQLDataType.VARCHAR(50).nullable(false)))
                    .column(field("options", SQLDataType.CLOB))
                    .column(field("created_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_action_id"))
                    ).getSQL();
            stmt.execute(ddl);
        }

        try (PreparedStatement stmt = connection.prepareStatement(create
                .insertInto(table("actions"))
                .columns(
                        field("action_id"),
                        field("name")
                )
                .values("?", "?")
                .getSQL())) {
            for (ActionType actionType : ActionType.values()) {
                stmt.setLong(1, actionType.getId());
                stmt.setString(2, actionType.getName());
                stmt.executeUpdate();
            }
            connection.commit();
        }
    }
}
