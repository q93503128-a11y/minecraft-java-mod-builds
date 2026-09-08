package kr.moonseungjun.earthtostars.ship.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ModuleCatalog {
    private final Map<String, ModuleDefinition> definitions = new LinkedHashMap<>();

    public void register(ModuleDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (definitions.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("duplicate module definition: " + definition.id());
        }
    }

    public ModuleDefinition require(String id) {
        ModuleDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("unknown module definition: " + id);
        }
        return definition;
    }

    public Map<String, ModuleDefinition> definitions() {
        return Collections.unmodifiableMap(definitions);
    }
}
