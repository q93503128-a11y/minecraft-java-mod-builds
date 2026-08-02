package kr.moonseungjun.livingkingdoms.worldgen;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.world.RealmSiteLayoutSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/** Fails world construction when political geography and generated biome geography disagree. */
public final class AuthoredBiomeVerifier {
    private AuthoredBiomeVerifier() {
    }

    public static void verifyCapital(ServerLevel level, String homelandId,
                                     RealmSiteLayoutSavedData.RealmSite site) {
        String expected = switch (homelandId) {
            case "erden_kingdom" -> "minecraft:plains";
            case "silvana_forest" -> "minecraft:dark_forest";
            case "kardum_league" -> "minecraft:stony_peaks";
            default -> null;
        };
        if (expected == null) return;

        BlockPos sample = new BlockPos(site.centerX(), site.baseY() + 2, site.centerZ());
        Holder<Biome> biome = level.getBiome(sample);
        String actual = biome.unwrapKey()
                .map(key -> key.location().toString())
                .orElse("<direct-biome>");
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    "Authored capital biome mismatch for " + homelandId
                            + ": expected=" + expected + " actual=" + actual
                            + " at " + sample.toShortString()
            );
        }
        LivingKingdoms.LOGGER.info(
                "Verified authored capital biome {} biome={} at {},{},{}",
                homelandId, actual, sample.getX(), sample.getY(), sample.getZ()
        );
    }
}
