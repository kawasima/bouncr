package net.unit8.bouncr.hook.license.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V1__CreateUserLicense extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            createUserLicense(create, stmt);
            createLastActivity(create, stmt);
        }
    }

    private void createUserLicense(DSLContext create, Statement stmt) throws SQLException {
        String ddl = create.createTable(table("user_licenses"))
                .column(field("user_license_id", SQLDataType.BIGINT.identity(true)))
                .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                .column(field("license_key", SQLDataType.VARBINARY(16).nullable(false)))
                .constraints(
                        constraint().primaryKey(field("user_license_id")),
                        constraint().foreignKey(field("user_id")).references(table("users"), field("user_id")),
                        constraint().unique(field("license_key"))
                ).getSQL();
        stmt.execute(ddl);
    }

    private void createLastActivity(DSLContext create, Statement stmt) throws SQLException {
        String ddl = create.createTable(table("license_last_activities"))
                .column(field("license_last_activity_id", SQLDataType.BIGINT.identity(true)))
                .column(field("user_license_id", SQLDataType.BIGINT.nullable(false)))
                .column(field("last_used_at", SQLDataType.TIMESTAMP.nullable(false)))
                .constraints(
                        constraint().primaryKey(field("license_last_activity_id")),
                        constraint().unique(field("user_license_id")),
                        constraint().foreignKey(field("user_license_id")).references(table("user_licenses"), field("user_license_id"))
                ).getSQL();
        stmt.execute(ddl);
    }

}
