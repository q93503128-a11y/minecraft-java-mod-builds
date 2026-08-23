package kr.moonseungjun.survivalascension.expedition;

import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public enum ExpeditionRegion {
    WOODLAND("삼림권", 0, SkillType.WOODCUTTING, 300, "자연 나무 일괄 벌목", 96),
    ARID("건조권", 0, SkillType.CONSTRUCTION, 300, "대량 건축 배치", 128),
    WETLAND("습지권", 0, SkillType.HARVESTING, 300, "성숙 작물 수확", 96),
    HIGHLANDS("고산권", 0, SkillType.MOBILITY, 350, "도보·돌진 횡단", 600),
    OCEAN("대양권", 0, SkillType.MOBILITY, 350, "수영·항해 탐사", 800),
    DEEP("심층권", 1, SkillType.MINING, 500, "곡괭이 채굴", 192),
    FROZEN("빙설권", 1, SkillType.MOBILITY, 450, "도보·돌진 횡단", 600),
    NETHER("네더권", 1, SkillType.COMBAT, 600, "적대적 몹 처치", 24),
    END("엔드권", 2, SkillType.COMBAT, 800, "적대적 몹 처치", 32);

    private final String koreanName;
    private final int requiredWorldStage;
    private final SkillType rewardSkill;
    private final int skillXp;
    private final String objectiveName;
    private final int objectiveTarget;

    ExpeditionRegion(String koreanName, int requiredWorldStage, SkillType rewardSkill, int skillXp,
                     String objectiveName, int objectiveTarget) {
        this.koreanName = koreanName;
        this.requiredWorldStage = requiredWorldStage;
        this.rewardSkill = rewardSkill;
        this.skillXp = skillXp;
        this.objectiveName = objectiveName;
        this.objectiveTarget = objectiveTarget;
    }

    public String koreanName() { return koreanName; }
    public int requiredWorldStage() { return requiredWorldStage; }
    public SkillType rewardSkill() { return rewardSkill; }
    public int skillXp() { return skillXp; }
    public String objectiveName() { return objectiveName; }
    public int objectiveTarget() { return objectiveTarget; }
    public int bit() { return 1 << ordinal(); }

    public boolean matches(Holder<Biome> biome) {
        return switch (this) {
            case WOODLAND -> isAny(biome,
                    Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
                    Biomes.DARK_FOREST, Biomes.PALE_GARDEN, Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA,
                    Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE);
            case ARID -> isAny(biome,
                    Biomes.DESERT, Biomes.BADLANDS, Biomes.WOODED_BADLANDS, Biomes.ERODED_BADLANDS,
                    Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA);
            case WETLAND -> isAny(biome, Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
            case HIGHLANDS -> isAny(biome,
                    Biomes.MEADOW, Biomes.CHERRY_GROVE, Biomes.STONY_PEAKS, Biomes.JAGGED_PEAKS,
                    Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST);
            case OCEAN -> isAny(biome,
                    Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN,
                    Biomes.DEEP_LUKEWARM_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN,
                    Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN);
            case DEEP -> isAny(biome, Biomes.DEEP_DARK, Biomes.DRIPSTONE_CAVES, Biomes.LUSH_CAVES);
            case FROZEN -> isAny(biome,
                    Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_TAIGA, Biomes.SNOWY_SLOPES,
                    Biomes.FROZEN_PEAKS, Biomes.GROVE, Biomes.SNOWY_BEACH);
            case NETHER -> isAny(biome,
                    Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY, Biomes.CRIMSON_FOREST,
                    Biomes.WARPED_FOREST, Biomes.BASALT_DELTAS);
            case END -> isAny(biome,
                    Biomes.THE_END, Biomes.SMALL_END_ISLANDS, Biomes.END_MIDLANDS,
                    Biomes.END_HIGHLANDS, Biomes.END_BARRENS);
        };
    }

    @SafeVarargs
    private static boolean isAny(Holder<Biome> biome, ResourceKey<Biome>... keys) {
        for (ResourceKey<Biome> key : keys) if (biome.is(key)) return true;
        return false;
    }
}
