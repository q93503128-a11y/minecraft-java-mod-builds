package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.integration.api.IntegrationModule;
import dev.moonseungjun.openworldrpg.integration.api.IntegrationRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class IntegrationRegistryTest {
    @Test
    void rejectsDuplicateModuleIdsAndInitializesRegisteredModules() {
        IntegrationRegistry registry = new IntegrationRegistry();
        AtomicInteger initialized = new AtomicInteger();

        registry.register(module("alpha", initialized));
        registry.register(module("beta", initialized));

        assertThrows(IllegalStateException.class, () -> registry.register(module("alpha", initialized)));

        registry.initializeAll();
        assertEquals(2, initialized.get());
        assertEquals(2, registry.modules().size());
    }

    private static IntegrationModule module(String id, AtomicInteger initialized) {
        return new IntegrationModule() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public void initialize() {
                initialized.incrementAndGet();
            }
        };
    }
}
