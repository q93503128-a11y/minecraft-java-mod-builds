package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.GrowthRulesV1;

/**
 * Base XP level plus duplicate-only bonus levels.
 *
 * <p>Normal battle XP raises only {@code level} from 1..60. Duplicate acquisition raises
 * {@code bonusLevel} from +0..+10 independently, so the effective combat level can reach 70.</p>
 */
public final class CharacterProgression {
    public record State(int level, int xp, int bonusLevel) {
        public State(int level, int xp) {
            this(level, xp, 0);
        }

        public State {
            if (level < 1 || level > GrowthRulesV1.maxLevel() || xp < 0
                    || bonusLevel < 0 || bonusLevel > GrowthRulesV1.duplicateBonusMax()) {
                throw new IllegalArgumentException("Invalid character progression");
            }
            if (level >= GrowthRulesV1.maxLevel()) xp = 0;
        }

        public int effectiveLevel() {
            return Math.min(GrowthRulesV1.effectiveMaxLevel(), level + bonusLevel);
        }

        public State withBonusLevel(int value) {
            return new State(level, xp, Math.max(0, Math.min(GrowthRulesV1.duplicateBonusMax(), value)));
        }

        public State grantDuplicateBonus() {
            return bonusLevel >= GrowthRulesV1.duplicateBonusMax() ? this : withBonusLevel(bonusLevel + 1);
        }
    }

    public record Gain(State before, State after, int gainedXp, int levelsGained, int xpToNextAfter) {}

    private CharacterProgression() {}

    public static int xpToNext(int level) {
        if (level >= GrowthRulesV1.maxLevel()) return 0;
        return (int) Math.round(120.0 * Math.pow(level, 1.55));
    }

    public static Gain gain(State state, int gainedXp, int levelCap) {
        if (gainedXp < 0) throw new IllegalArgumentException("Negative XP gain");
        int cap = Math.max(1, Math.min(GrowthRulesV1.maxLevel(), levelCap));
        int level = Math.min(state.level(), cap);
        int xp = level >= cap ? 0 : state.xp();
        int remaining = gainedXp;
        int gainedLevels = 0;
        while (remaining > 0 && level < cap) {
            int need = xpToNext(level);
            int missing = Math.max(0, need - xp);
            if (remaining < missing) {
                xp += remaining;
                remaining = 0;
            } else {
                remaining -= missing;
                level++;
                gainedLevels++;
                xp = 0;
            }
        }
        if (level >= cap) xp = 0;
        State after = new State(level, xp, state.bonusLevel());
        return new Gain(state, after, gainedXp, gainedLevels, level >= cap ? 0 : xpToNext(level));
    }
}
