package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V17__CreateInvitations extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("invitations"))
                    .column(field("invitation_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("email", SQLDataType.VARCHAR(100)))
                    .column(field("code", SQLDataType.VARCHAR(8)))
                    .column(field("invited_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("invitation_id")),
                            constraint().unique(field("code"))
                    ).getSQL();
            stmt.execute(ddl);

            ddl = create.createTable(table("group_invitations"))
                    .column(field("group_invitation_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("invitation_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("group_id", SQLDataType.BIGINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("group_invitation_id")),
                            constraint().foreignKey(field("invitation_id")).references(table("invitations"), field("invitation_id")).onDeleteCascade(),
                            constraint().foreignKey(field("group_id")).references(table("groups"), field("group_id")).onDeleteCascade()
                    ).getSQL();
            stmt.execute(ddl);

            ddl = create.createTable(table("oidc_invitations"))
                    .column(field("oidc_invitation_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("invitation_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("oidc_provider_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("oidc_sub", SQLDataType.VARCHAR(2048).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("oidc_invitation_id")),
                            constraint().foreignKey(field("invitation_id")).references(table("invitations"), field("invitation_id")).onDeleteCascade(),
                            constraint().foreignKey(field("oidc_provider_id")).references(table("oidc_providers"), field("oidc_provider_id")).onDeleteCascade()
                    ).getSQL();
            stmt.execute(ddl);

        }
    }
}
