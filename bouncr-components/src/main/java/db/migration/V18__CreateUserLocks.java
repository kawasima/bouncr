package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V18__CreateUserLocks extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("user_locks"))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("locked_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id")),
                            constraint().foreignKey(field("user_id")).references(table("users"), field("user_id"))
                    ).getSQL();
            stmt.execute(ddl);
        }
    }
}
