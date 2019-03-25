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
 * The entity of role assignments.
 *
 * @author kawasima
 */
public class V9__CreateAssinments extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("assignments"))
                    .column(field("group_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("role_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("realm_id", SQLDataType.BIGINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("group_id"), field("role_id"), field("realm_id")),
                            constraint().foreignKey(field("group_id")).references(table("groups"), field("group_id")).onDeleteCascade(),
                            constraint().foreignKey(field("role_id")).references(table("roles"), field("role_id")).onDeleteCascade(),
                            constraint().foreignKey(field("realm_id")).references(table("realms"), field("realm_id")).onDeleteCascade()
                    ).getSQL();
            stmt.execute(ddl);
        }
    }
}
