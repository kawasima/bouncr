package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V21__CreateUserProfiles implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        createProfileFields(connection);
        createProfileValues(connection);
        createProfileVerification(connection);
    }

    private void createProfileFields(Connection connection) throws SQLException {
        DSLContext create = DSL.using(connection);
        try (Statement stmt = connection.createStatement()) {
            String ddl = create.createTable(table("user_profile_fields"))
                    .column(field("user_profile_field_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("json_name", SQLDataType.VARCHAR(100).nullable(true)))
                    .column(field("is_required", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("is_identity", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("regular_expression", SQLDataType.VARCHAR(255).nullable(true)))
                    .column(field("min_length", SQLDataType.SMALLINT.nullable(true)))
                    .column(field("max_length", SQLDataType.SMALLINT.nullable(true)))
                    .column(field("needs_verification", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("position", SQLDataType.TINYINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_profile_field_id")),
                            constraint().unique(field("name")),
                            constraint().unique(field("json_name"))
                    ).getSQL();
            stmt.execute(ddl);
        }
    }

    private void createProfileValues(Connection connection) throws SQLException {
        DSLContext create = DSL.using(connection);

        try (Statement stmt = connection.createStatement()) {
            String ddl = create.createTable(table("user_profile_values"))
                    .column(field("user_profile_field_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("value", SQLDataType.VARCHAR(255).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_profile_field_id"), field("user_id")),
                            constraint().foreignKey(field("user_profile_field_id"))
                                    .references(table("user_profile_fields"), field("user_profile_field_id")),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id"))
                    )
                    .getSQL();
            stmt.execute(ddl);
        }
    }

    private void createProfileVerification(Connection connection) throws SQLException {
        DSLContext create = DSL.using(connection);

        try (Statement stmt = connection.createStatement()) {
            String ddl = create.createTable(table("user_profile_verifications"))
                    .column(field("user_profile_field_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("code", SQLDataType.VARCHAR(255).nullable(false)))
                    .column(field("expires_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_profile_field_id"), field("user_id")),
                            constraint().foreignKey(field("user_profile_field_id"))
                                    .references(table("user_profile_fields"), field("user_profile_field_id")),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id"))
                    )
                    .getSQL();
            stmt.execute(ddl);
        }
    }
}
