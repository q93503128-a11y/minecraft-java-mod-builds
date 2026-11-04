package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/** Resource ids for authored summon artwork and model-referenced portrait illustrations. */
final class SummonAssets {
    static final Identifier BG_ARCHIVE = texture("bg_archive.png");
    static final Identifier WASH_VIOLET = texture("wash_violet.png");
    static final Identifier WASH_GOLD = texture("wash_gold.png");
    static final Identifier CARD_COMMON = texture("card_common.png");
    static final Identifier CARD_RARE = texture("card_rare.png");
    static final Identifier CARD_LEGENDARY = texture("card_legendary.png");

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

    static Identifier rarityWash(int stars) {
        if (stars >= 5) return WASH_GOLD;
        if (stars >= 4) return WASH_VIOLET;
        return null;
    }

    private static Identifier texture(String file) {
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "textures/gui/summon/" + file);
    }

    private static String safeId(String id) {
        if (id == null || id.isBlank()) return "unknown";
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }
}
