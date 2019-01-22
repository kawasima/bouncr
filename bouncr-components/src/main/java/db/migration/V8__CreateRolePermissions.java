package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

/**
 * @author kawasima
 */
public class V8__CreateRolePermissions extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("role_permissions"))
                    .column(field("role_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("permission_id", SQLDataType.BIGINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("role_id"), field("permission_id")),
                            constraint().foreignKey(field("role_id")).references(table("roles"), field("role_id")),
                            constraint().foreignKey(field("permission_id")).references(table("permissions"), field("permission_id"))
                    ).getSQL();
            stmt.execute(ddl);
        }
    }
}
