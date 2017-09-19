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
public class V3__CreateApplications implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("applications"))
                    .column(field("application_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("virtual_path", SQLDataType.VARCHAR(50).nullable(false)))
                    .column(field("pass_to", SQLDataType.VARCHAR(255).nullable(false)))
                    .column(field("top_page", SQLDataType.VARCHAR(255).nullable(false)))
                    .column(field("description", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("write_protected", SQLDataType.BOOLEAN.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("application_id")),
                            constraint().unique(field("virtual_path"))
                    ).getSQL();

            stmt.execute(ddl);
        }

    }
}
