package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

/**
 * The entity of role assignments.
 *
 * @author kawasima
 */
public class V9__CreateAssinments implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("assignments"))
                    .column(field("group_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("role_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("realm_id", SQLDataType.BIGINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("group_id"), field("role_id"), field("realm_id")),
                            constraint().foreignKey(field("group_id")).references(table("groups"), field("group_id")),
                            constraint().foreignKey(field("role_id")).references(table("roles"), field("role_id")),
                            constraint().foreignKey(field("realm_id")).references(table("realms"), field("realm_id"))
                    ).getSQL();
            stmt.execute(ddl);
        }
    }
}
