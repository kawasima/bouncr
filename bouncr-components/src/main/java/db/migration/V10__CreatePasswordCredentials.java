package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V10__CreatePasswordCredentials extends BaseJavaMigration {
    private void createPasswordCredentials(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("password_credentials"))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("password", SQLDataType.VARBINARY(256).nullable(false)))
                    .column(field("salt", SQLDataType.VARCHAR(16).nullable(false)))
                    .column(field("initial", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("created_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id")),
                            constraint().foreignKey(field("user_id")).references(table("users"), field("user_id")).onDeleteCascade()
                    ).getSQL();
            stmt.execute(ddl);

        }
    }

    private void createOtpKeys(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("otp_keys"))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))//
                    .column(field("otp_key", SQLDataType.BINARY(20).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id")),
                            constraint().foreignKey(field("user_id")).references(table("users"), field("user_id")).onDeleteCascade()
                    ).getSQL();
            stmt.execute(ddl);
        }
    }

    private void createPasswordResetChallenges(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("password_reset_challenges"))
                    .column(field("id", SQLDataType.BIGINT.identity(true)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("code", SQLDataType.VARCHAR(64).nullable(false)))
                    .column(field("expires_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("id"))
                    )
                    .getSQL();

            stmt.execute(ddl);

            stmt.execute(
                    create.createIndex(name("idx_pass_reset_challenges_01"))
                            .on(table("password_reset_challenges"), field("code"))
                            .getSQL());

        }
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        createPasswordCredentials(connection);
        createOtpKeys(connection);
        createPasswordResetChallenges(connection);
    }
}
