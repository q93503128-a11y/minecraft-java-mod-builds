package kr.moonseungjun.livingkingdoms.worldgen;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.world.RealmSiteLayoutSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/** Fails construction when the active Erden capital and generated ecological geography disagree. */
public final class AuthoredBiomeVerifier {
    private AuthoredBiomeVerifier() {
    }

    public static void verifyCapital(ServerLevel level, String homelandId,
                                     RealmSiteLayoutSavedData.RealmSite site) {
        if (!"erden_kingdom".equals(homelandId)) {
            throw new IllegalArgumentException("Inactive homeland biome verification: " + homelandId);
        }
        String expected = "minecraft:meadow";
        BlockPos sample = new BlockPos(site.centerX(), site.baseY() + 2, site.centerZ());
        Holder<Biome> biome = level.getBiome(sample);
        String actual = biome.unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("<direct-biome>");
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    "Erden capital ecological region mismatch: expected=" + expected
                            + " actual=" + actual + " at " + sample.toShortString()
            );
        }
        LivingKingdoms.LOGGER.info(
                "Verified Erden capital ecological region biome={} at {},{},{}",
                actual, sample.getX(), sample.getY(), sample.getZ()
        );
    }
}
