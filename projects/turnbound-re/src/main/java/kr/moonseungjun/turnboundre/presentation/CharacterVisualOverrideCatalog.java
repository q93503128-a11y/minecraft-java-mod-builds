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
    public static final String BLAZE_CHARACTER_ID = "turnbound_re:blaze";
    public static final String BLAZE_SOURCE_ENTITY = "minecraft:blaze";
    public static final String BLAZE_VISUAL_ENTITY = "turnbound_re:blaze_visual";
    public static final String WITCH_CHARACTER_ID = "turnbound_re:witch";
    public static final String WITCH_SOURCE_ENTITY = "minecraft:witch";
    public static final String WITCH_VISUAL_ENTITY = "turnbound_re:witch_visual";
    public static final String IRON_GOLEM_CHARACTER_ID = "turnbound_re:iron_golem";
    public static final String IRON_GOLEM_SOURCE_ENTITY = "minecraft:iron_golem";
    public static final String IRON_GOLEM_VISUAL_ENTITY = "turnbound_re:iron_golem_visual";

    private CharacterVisualOverrideCatalog() {}

    public static String visualEntityId(String characterId, String sourceEntityId) {
        if (STARTER_ZOMBIE_CHARACTER_ID.equals(characterId)
                && STARTER_ZOMBIE_SOURCE_ENTITY.equals(sourceEntityId)) {
            return STARTER_ZOMBIE_VISUAL_ENTITY;
        }
        if (BLAZE_CHARACTER_ID.equals(characterId)
                && BLAZE_SOURCE_ENTITY.equals(sourceEntityId)) {
            return BLAZE_VISUAL_ENTITY;
        }
        if (WITCH_CHARACTER_ID.equals(characterId)
                && WITCH_SOURCE_ENTITY.equals(sourceEntityId)) {
            return WITCH_VISUAL_ENTITY;
        }
        if (IRON_GOLEM_CHARACTER_ID.equals(characterId)
                && IRON_GOLEM_SOURCE_ENTITY.equals(sourceEntityId)) {
            return IRON_GOLEM_VISUAL_ENTITY;
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
