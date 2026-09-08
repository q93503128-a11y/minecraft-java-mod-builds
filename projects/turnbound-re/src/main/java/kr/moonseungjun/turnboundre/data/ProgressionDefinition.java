package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/** Data-driven progression/economy tuning. Costs and the new-save starter party can be rebalanced without code changes. */
public record ProgressionDefinition(
        String id,
        int partyCapacity,
        List<String> starterParty,
        Map<String, Integer> unlockShardCostByOriginStar,
        Map<String, LevelCost> levelCostsByStar,
        Map<String, AscensionCost> ascensionCostsByTargetStar
) {
    public record LevelCost(int coinBase, int coinPerLevel, int essenceBase, int essencePerLevel) {
        public static final Codec<LevelCost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("coinBase").forGetter(LevelCost::coinBase),
                Codec.INT.fieldOf("coinPerLevel").forGetter(LevelCost::coinPerLevel),
                Codec.INT.fieldOf("essenceBase").forGetter(LevelCost::essenceBase),
                Codec.INT.fieldOf("essencePerLevel").forGetter(LevelCost::essencePerLevel)
        ).apply(instance, LevelCost::new));
    }

    public record AscensionCost(int coin, int essence, int shards) {
        public static final Codec<AscensionCost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("coin").forGetter(AscensionCost::coin),
                Codec.INT.fieldOf("essence").forGetter(AscensionCost::essence),
                Codec.INT.fieldOf("shards").forGetter(AscensionCost::shards)
        ).apply(instance, AscensionCost::new));
    }

    public static final Codec<ProgressionDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(ProgressionDefinition::id),
            Codec.INT.fieldOf("partyCapacity").forGetter(ProgressionDefinition::partyCapacity),
            Codec.STRING.listOf().optionalFieldOf("starterParty", List.of()).forGetter(ProgressionDefinition::starterParty),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("unlockShardCostByOriginStar")
                    .forGetter(ProgressionDefinition::unlockShardCostByOriginStar),
            Codec.unboundedMap(Codec.STRING, LevelCost.CODEC).fieldOf("levelCostsByStar")
                    .forGetter(ProgressionDefinition::levelCostsByStar),
            Codec.unboundedMap(Codec.STRING, AscensionCost.CODEC).fieldOf("ascensionCostsByTargetStar")
                    .forGetter(ProgressionDefinition::ascensionCostsByTargetStar)
    ).apply(instance, ProgressionDefinition::new));

    /** Source-compatible constructor for tests/older callers that intentionally have no starter party. */
    public ProgressionDefinition(
            String id,
            int partyCapacity,
            Map<String, Integer> unlockShardCostByOriginStar,
            Map<String, LevelCost> levelCostsByStar,
            Map<String, AscensionCost> ascensionCostsByTargetStar
    ) {
        this(id, partyCapacity, List.of(), unlockShardCostByOriginStar, levelCostsByStar, ascensionCostsByTargetStar);
    }

    public ProgressionDefinition {
        starterParty = starterParty == null ? List.of() : List.copyOf(starterParty);
        unlockShardCostByOriginStar = unlockShardCostByOriginStar == null ? Map.of() : Map.copyOf(unlockShardCostByOriginStar);
        levelCostsByStar = levelCostsByStar == null ? Map.of() : Map.copyOf(levelCostsByStar);
        ascensionCostsByTargetStar = ascensionCostsByTargetStar == null ? Map.of() : Map.copyOf(ascensionCostsByTargetStar);
    }
}
