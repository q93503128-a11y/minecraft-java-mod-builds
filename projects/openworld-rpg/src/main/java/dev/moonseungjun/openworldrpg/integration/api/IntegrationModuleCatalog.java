package dev.moonseungjun.openworldrpg.integration.api;

import java.util.Set;

/**
 * Closed vocabulary for dependency-manifest integration module identifiers.
 */
public final class IntegrationModuleCatalog {
    private static final Set<String> KNOWN = Set.of(
            "player_animation",
            "geckolib",
            "armor_model",
            "ranged_weapon",
            "trinkets",
            "bettercombat",
            "spellengine",
            "common",
            "ecology/mobfilter",
            "ecology/alexs_mobs",
            "ecology/threateningly"
    );

    private IntegrationModuleCatalog() {
    }

    public static boolean isKnown(String id) {
        return id != null && KNOWN.contains(id.trim());
    }
}
