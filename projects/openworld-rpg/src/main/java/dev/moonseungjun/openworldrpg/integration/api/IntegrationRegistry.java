package dev.moonseungjun.openworldrpg.integration.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class IntegrationRegistry {
    private final Map<String, IntegrationModule> modules = new LinkedHashMap<>();

    public void register(IntegrationModule module) {
        Objects.requireNonNull(module, "module");
        String id = Objects.requireNonNull(module.id(), "module id").trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Integration module id must not be blank.");
        }
        if (modules.putIfAbsent(id, module) != null) {
            throw new IllegalStateException("Duplicate integration module id: " + id);
        }
    }

    public Collection<IntegrationModule> modules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    public void initializeAll() {
        modules.values().forEach(IntegrationModule::initialize);
    }
}
