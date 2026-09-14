package kr.moonseungjun.turnboundre.presentation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure presentation-only mapping layered on top of the server-authored character source-entity catalog.
 * Gameplay identity never changes: the override only selects a client render entity for known characters.
 */
public final class CharacterVisualOverrideCatalog {
    public static final String STARTER_ZOMBIE_CHARACTER_ID = "turnbound_re:zombie";
    public static final String STARTER_ZOMBIE_SOURCE_ENTITY = "minecraft:zombie";
    public static final String STARTER_ZOMBIE_VISUAL_ENTITY = "turnbound_re:starter_zombie_visual";

    private CharacterVisualOverrideCatalog() {}

    public static String visualEntityId(String characterId, String sourceEntityId) {
        if (STARTER_ZOMBIE_CHARACTER_ID.equals(characterId)
                && STARTER_ZOMBIE_SOURCE_ENTITY.equals(sourceEntityId)) {
            return STARTER_ZOMBIE_VISUAL_ENTITY;
        }
        return sourceEntityId == null ? "" : sourceEntityId;
    }

    public static Map<String, String> apply(Map<String, String> serverCatalog) {
        if (serverCatalog == null || serverCatalog.isEmpty()) return Map.of();
        Map<String, String> presentation = new LinkedHashMap<>();
        serverCatalog.forEach((characterId, sourceEntityId) -> {
            if (characterId == null || characterId.isBlank()) return;
            String visual = visualEntityId(characterId, sourceEntityId);
            if (!visual.isBlank()) presentation.put(characterId, visual);
        });
        return Map.copyOf(presentation);
    }
}
