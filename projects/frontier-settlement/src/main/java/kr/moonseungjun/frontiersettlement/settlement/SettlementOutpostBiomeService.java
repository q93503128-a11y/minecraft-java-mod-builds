package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.Tags;

/**
 * Soft biome-tag context for existing outpost specialization. No companion class or biome id is required.
 * The physical local survey remains primary; biome tags only provide bounded evidence bias.
 */
public final class SettlementOutpostBiomeService {
    public static final int FOREST_LOG_BONUS = 8;
    public static final int OPEN_FIELD_BONUS = 24;
    public static final int MOUNTAIN_STONE_BONUS = 8;
    public static final int MOUNTAIN_ORE_BONUS = 1;
    public static final int DRY_STONE_BONUS = 6;

    public record Bias(int ore, int logs, int field, int stone, String label) {
        static final Bias NONE = new Bias(0, 0, 0, 0, "중립");
    }

    private SettlementOutpostBiomeService() {}

    public static Bias bias(ServerLevel level, BlockPos center) {
        if (!level.hasChunkAt(center)) return Bias.NONE;
        var biome = level.getBiome(center);
        int ore = 0;
        int logs = 0;
        int field = 0;
        int stone = 0;
        String label = "중립";

        if (biome.is(Tags.Biomes.IS_FOREST) || biome.is(Tags.Biomes.IS_DENSE_VEGETATION)) {
            logs += FOREST_LOG_BONUS;
            label = "삼림";
        }
        if (biome.is(Tags.Biomes.IS_PLAINS) || biome.is(Tags.Biomes.IS_SAVANNA)) {
            field += OPEN_FIELD_BONUS;
            label = "개활지";
        }
        if (biome.is(Tags.Biomes.IS_MOUNTAIN) || biome.is(Tags.Biomes.IS_HILL)) {
            stone += MOUNTAIN_STONE_BONUS;
            ore += MOUNTAIN_ORE_BONUS;
            label = "산악";
        } else if (biome.is(Tags.Biomes.IS_BADLANDS) || biome.is(Tags.Biomes.IS_SANDY)) {
            stone += DRY_STONE_BONUS;
            label = "건조 암지";
        }
        return new Bias(ore, logs, field, stone, label);
    }
}
