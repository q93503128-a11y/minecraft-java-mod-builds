package kr.moonseungjun.turnboundre.presentation;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CharacterVisualOverrideCatalogTest {
    @Test
    void productionOverridesKeepGameplaySourcesUntouched() {
        Map<String, String> serverCatalog = new LinkedHashMap<>();
        serverCatalog.put("turnbound_re:zombie", "minecraft:zombie");
        serverCatalog.put("turnbound_re:skeleton", "minecraft:skeleton");
        serverCatalog.put("turnbound_re:blaze", "minecraft:blaze");

        Map<String, String> presentation = CharacterVisualOverrideCatalog.apply(serverCatalog);

        assertEquals("turnbound_re:starter_zombie_visual", presentation.get("turnbound_re:zombie"));
        assertEquals("minecraft:skeleton", presentation.get("turnbound_re:skeleton"));
        assertEquals("turnbound_re:blaze_visual", presentation.get("turnbound_re:blaze"));
        assertEquals("minecraft:zombie", serverCatalog.get("turnbound_re:zombie"));
        assertEquals("minecraft:blaze", serverCatalog.get("turnbound_re:blaze"));
    }

    @Test
    void sourceMismatchNeverForcesAProductionOverride() {
        assertEquals("minecraft:husk", CharacterVisualOverrideCatalog.visualEntityId(
                "turnbound_re:zombie", "minecraft:husk"));
        assertEquals("minecraft:zombie", CharacterVisualOverrideCatalog.visualEntityId(
                "turnbound_re:other", "minecraft:zombie"));
        assertEquals("minecraft:breeze", CharacterVisualOverrideCatalog.visualEntityId(
                "turnbound_re:blaze", "minecraft:breeze"));
        assertEquals("minecraft:blaze", CharacterVisualOverrideCatalog.visualEntityId(
                "turnbound_re:other", "minecraft:blaze"));
    }
}
