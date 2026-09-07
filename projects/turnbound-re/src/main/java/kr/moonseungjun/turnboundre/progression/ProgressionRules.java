package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.ProgressionDefinition;

/** Pure formulas shared by progression, debug inspection and save validation. */
public final class ProgressionRules {
    public record Cost(long coin, long essence, int shards) {
        public Cost {
            if (coin < 0 || essence < 0 || shards < 0) throw new IllegalArgumentException("costs must be >= 0");
        }
    }

    private ProgressionRules() {}

    public static int levelCap(int star) {
        return switch (star) {
            case 1 -> 20;
            case 2 -> 30;
            case 3 -> 40;
            case 4 -> 50;
            case 5 -> 60;
            case 6 -> 70;
            default -> throw new IllegalArgumentException("star must be 1..6");
        };
    }

    public static int unlockShardCost(ProgressionDefinition tuning, int originStar) {
        Integer value = tuning.unlockShardCostByOriginStar().get(Integer.toString(originStar));
        if (value == null) throw new IllegalArgumentException("missing unlock shard cost for originStar " + originStar);
        return value;
    }

    /** Cost to move from current level to current level + 1. */
    public static Cost levelUpCost(ProgressionDefinition tuning, int currentStar, int currentLevel) {
        if (currentLevel < 1 || currentLevel >= levelCap(currentStar)) {
            throw new IllegalArgumentException("currentLevel must be below current star cap");
        }
        ProgressionDefinition.LevelCost row = tuning.levelCostsByStar().get(Integer.toString(currentStar));
        if (row == null) throw new IllegalArgumentException("missing level cost for star " + currentStar);
        long coin = Math.addExact(row.coinBase(), Math.multiplyExact((long) row.coinPerLevel(), currentLevel));
        long essence = Math.addExact(row.essenceBase(), Math.multiplyExact((long) row.essencePerLevel(), currentLevel));
        return new Cost(coin, essence, 0);
    }

    /** Cost to ascend from targetStar - 1 to targetStar. */
    public static Cost ascensionCost(ProgressionDefinition tuning, int targetStar) {
        ProgressionDefinition.AscensionCost row = tuning.ascensionCostsByTargetStar().get(Integer.toString(targetStar));
        if (row == null) throw new IllegalArgumentException("missing ascension cost for targetStar " + targetStar);
        return new Cost(row.coin(), row.essence(), row.shards());
    }

    /** Canonical stat(level,currentStar) = floor(base + growth*(level-1) + ascensionFlat[currentStar]). */
    public static CharacterDefinition.Stats stats(CharacterDefinition definition, CharacterProgress progress) {
        requireMatches(definition, progress);
        CharacterDefinition.Stats base = definition.baseStats();
        CharacterDefinition.Growth growth = definition.growth();
        CharacterDefinition.Stats asc = definition.ascensionFlat().getOrDefault(
                Integer.toString(progress.currentStar()), new CharacterDefinition.Stats(0, 0, 0, 0, 0));
        int levels = progress.level() - 1;
        return new CharacterDefinition.Stats(
                scaled(base.hp(), growth.hp(), levels, asc.hp()),
                scaled(base.atk(), growth.atk(), levels, asc.atk()),
                scaled(base.def(), growth.def(), levels, asc.def()),
                scaled(base.spd(), growth.spd(), levels, asc.spd()),
                scaled(base.poise(), growth.poise(), levels, asc.poise()));
    }

    public static void requireMatches(CharacterDefinition definition, CharacterProgress progress) {
        if (definition == null || progress == null) throw new IllegalArgumentException("definition/progress required");
        if (!definition.id().equals(progress.characterId())) throw new IllegalArgumentException("character id mismatch");
        if (definition.originStar() != progress.originStar()) throw new IllegalArgumentException("originStar is immutable and must match definition");
        if (progress.currentStar() < definition.originStar()) throw new IllegalArgumentException("currentStar below originStar");
        if (progress.level() > levelCap(progress.currentStar())) throw new IllegalArgumentException("level exceeds star cap");
    }

    private static int scaled(int base, double growth, int levels, int ascension) {
        double value = base + growth * levels + ascension;
        if (!Double.isFinite(value) || value < 0 || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid derived stat " + value);
        }
        return (int) Math.floor(value);
    }
}
