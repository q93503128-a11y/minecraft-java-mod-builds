package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/** Resource ids for the deterministic 2D summon asset pipeline. */
final class SummonAssets {
    static final Identifier BG_ARCHIVE = texture("bg_archive.png");
    static final Identifier CRYSTAL = texture("crystal.png");
    static final Identifier RUNE_CIRCLE = texture("rune_circle.png");
    static final Identifier ORBIT_A = texture("orbit_a.png");
    static final Identifier ORBIT_B = texture("orbit_b.png");
    static final Identifier RADIAL_GLOW = texture("radial_glow.png");
    static final Identifier OMEN_VIOLET = texture("omen_violet.png");
    static final Identifier BREAK_GOLD = texture("break_gold.png");
    static final Identifier LIGHT_SWEEP = texture("light_sweep.png");
    static final Identifier VIGNETTE = texture("vignette.png");
    static final Identifier PAGE = texture("page.png");
    static final Identifier CARD_COMMON = texture("card_common.png");
    static final Identifier CARD_RARE = texture("card_rare.png");
    static final Identifier CARD_LEGENDARY = texture("card_legendary.png");
    static final Identifier NEW_TAG = texture("new_tag.png");

    private SummonAssets() {}

    static Identifier portrait(String characterId) {
        return texture("portraits/" + safeId(characterId) + ".png");
    }

    static Identifier silhouette(String characterId) {
        return texture("portraits/" + safeId(characterId) + "_silhouette.png");
    }

    static Identifier card(int stars) {
        if (stars >= 5) return CARD_LEGENDARY;
        if (stars >= 4) return CARD_RARE;
        return CARD_COMMON;
    }

    private static Identifier texture(String file) {
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "textures/gui/summon/" + file);
    }

    private static String safeId(String id) {
        if (id == null || id.isBlank()) return "unknown";
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }
}
