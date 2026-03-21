package net.unit8.bouncr.api.resource;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.h2.jdbcx.JdbcDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Factory for creating test DSLContext backed by H2 in-memory database with migrations.
 *
 * <p>Usage: call {@link #sharedDSLContext()} once, then use {@link #beginTransaction()} /
 * {@link #rollback()} around each test to isolate data changes without re-running migrations.</p>
 */
public class MockFactory {

    private static volatile DataSource sharedDataSource;
    private static volatile DSLContext sharedDSLContext;
    private static final ThreadLocal<Connection> txConnection = new ThreadLocal<>();

    private static final BaseJavaMigration[] MIGRATIONS = {
            new db.migration.V1__CreateUsers(),
            new db.migration.V2__CreateGroups(),
            new db.migration.V3__CreateApplications(),
            new db.migration.V4__CreateRoles(),
            new db.migration.V5__CreatePermissions(),
            new db.migration.V6__CreateRealms(),
            new db.migration.V7__CreateMemberships(),
            new db.migration.V8__CreateRolePermissions(),
            new db.migration.V9__CreateAssinments(),
            new db.migration.V10__CreatePasswordCredentials(),
            new db.migration.V12__CreateUserActions(),
            new db.migration.V13__CreateOidcProviders(),
            new db.migration.V14__CreateCertificateCredentials(),
            new db.migration.V15__CreateOidcUsers(),
            new db.migration.V16__CreateOidcApplications(),
            new db.migration.V17__CreateInvitations(),
            new db.migration.V18__CreateUserLocks(),
            new db.migration.V19__CreateUserSessions(),
            new db.migration.V20__CreateCerts(),
            new db.migration.V21__CreateUserProfiles(),
            new db.migration.V22__AlterInvitations(),
            new db.migration.V23__InsertAdminUser(),
            new db.migration.V24__AddAccountLowerToUser(),
            new db.migration.V25__AddRedirectUriToOidcProvider(),
            new db.migration.V26__AddJwksUriAndIssuerToOidcProvider(),
            new db.migration.V27__AddPkceEnabledToOidcProvider(),
            new db.migration.V28__AlterOidcApplicationSecretLength(),
    };

    /**
     * Returns a shared DSLContext reused across all tests in a JVM.
     * Migrations are run once on first call.
     */
    public static DSLContext sharedDSLContext() {
        if (sharedDSLContext == null) {
            synchronized (MockFactory.class) {
                if (sharedDSLContext == null) {
                    sharedDataSource = createDataSource();
                    sharedDSLContext = DSL.using(sharedDataSource, SQLDialect.H2);
                }
            }
        }
        return sharedDSLContext;
    }

    /**
     * Begins a transaction on the shared connection. All changes within
     * the test will be rolled back by {@link #rollback()}.
     */
    public static DSLContext beginTransaction() {
        try {
            sharedDSLContext(); // ensure initialization
            Connection conn = sharedDataSource.getConnection();
            conn.setAutoCommit(false);
            txConnection.set(conn);
            return DSL.using(conn, SQLDialect.H2);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Rolls back the current transaction, undoing all changes made during the test.
     */
    public static void rollback() {
        Connection conn = txConnection.get();
        if (conn != null) {
            try {
                conn.rollback();
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                txConnection.remove();
            }
        }
    }

    private static DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(true);
            FlywayContext ctx = new FlywayContext(conn);
            for (BaseJavaMigration migration : MIGRATIONS) {
                migration.migrate(ctx);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to run migrations", e);
        }
        return ds;
    }

    private static class FlywayContext implements Context {
        private final Connection connection;

        FlywayContext(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public Configuration getConfiguration() {
            return null;
        }
    }
}
