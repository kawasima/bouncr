package net.unit8.bouncr.api.resource;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
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

    private static final JavaMigration[] MIGRATIONS = {
            new db.migration.B28__BouncrV0_3_0(),
            new db.migration.V29__AddOidcLogoutUris(),
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
            for (JavaMigration migration : MIGRATIONS) {
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
