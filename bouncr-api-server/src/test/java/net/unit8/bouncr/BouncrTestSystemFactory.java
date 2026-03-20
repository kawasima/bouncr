package net.unit8.bouncr;

import enkan.config.EnkanSystemFactory;
import enkan.system.EnkanSystem;
import net.unit8.bouncr.api.resource.MockFactory;
import org.jooq.DSLContext;

/**
 * Test system factory that provides a DSLContext backed by H2 in-memory database.
 * The old EclipseLink-based system has been replaced with jOOQ.
 */
public class BouncrTestSystemFactory implements EnkanSystemFactory {

    /**
     * Creates a DSLContext for testing (not an EnkanSystem).
     * Most tests should use {@link MockFactory#createTestDSLContext()} directly.
     */
    public static DSLContext createTestDSLContext() {
        return MockFactory.sharedDSLContext();
    }

    @Override
    public EnkanSystem create() {
        // This method is kept for backward compatibility but is no longer
        // the recommended way to set up tests. Use createTestDSLContext() instead.
        throw new UnsupportedOperationException(
                "EnkanSystem-based tests are no longer supported. Use BouncrTestSystemFactory.createTestDSLContext() instead.");
    }
}
