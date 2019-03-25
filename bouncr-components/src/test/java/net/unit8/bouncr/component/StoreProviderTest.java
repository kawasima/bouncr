package net.unit8.bouncr.component;

import enkan.middleware.session.KeyValueStore;
import enkan.system.EnkanSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static enkan.component.ComponentRelationship.component;

public class StoreProviderTest {
    private EnkanSystem system;
    @BeforeEach
    void setup() {
        system = EnkanSystem.of(
                "storeProvider", new StoreProvider(),
                "config", new BouncrConfiguration()
        ).relationships(
                component("storeProvider").using("config")
        );
        system.start();
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.stop();
        }
    }

    @Test
    void test() {
        StoreProvider provider = system.getComponent("storeProvider", StoreProvider.class);
        KeyValueStore store = provider.getStore(StoreProvider.StoreType.BOUNCR_TOKEN);
        store.write("A", "B");
    }
}
