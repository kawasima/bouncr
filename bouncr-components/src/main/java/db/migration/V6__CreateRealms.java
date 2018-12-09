package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

/**
 * @author kawasima
 */
public class V6__CreateRealms implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("realms"))
                    .column(field("realm_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("url", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("application_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("description", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("write_protected", SQLDataType.BOOLEAN.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("realm_id")),
                            constraint().unique(field("name")),
                            constraint().foreignKey(field("application_id")).references(table("applications"), field("application_id"))
                    ).getSQL();
            stmt.execute(ddl);
        }
    }
}
