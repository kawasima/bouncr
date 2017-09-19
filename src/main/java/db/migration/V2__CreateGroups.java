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
public class V2__CreateGroups implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("groups"))
                    .column(field("group_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("description", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("write_protected", SQLDataType.BOOLEAN.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("group_id")),
                            constraint().unique(field("name"))
                    ).getSQL();
            stmt.execute(ddl);
        }
    }
}
