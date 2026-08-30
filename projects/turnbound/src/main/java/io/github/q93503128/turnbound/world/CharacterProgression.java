package io.github.q93503128.turnbound.world;

/** Pure v0.4 level-XP rules used by the alpha.16 campaign result flow. */
public final class CharacterProgression {
    public record State(int level, int xp) {
        public State {
            if (level < 1 || level > 60 || xp < 0) throw new IllegalArgumentException("Invalid character progression");
        }
    }

    public record Gain(State before, State after, int gainedXp, int levelsGained, int xpToNextAfter) {}

    private CharacterProgression() {}

    public static int xpToNext(int level) {
        if (level >= 60) return 0;
        return (int) Math.round(120.0 * Math.pow(level, 1.55));
    }

    public static Gain gain(State state, int gainedXp, int levelCap) {
        if (gainedXp < 0) throw new IllegalArgumentException("Negative XP gain");
        int cap = Math.max(1, Math.min(60, levelCap));
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
        State after = new State(level, xp);
        return new Gain(state, after, gainedXp, gainedLevels, level >= cap ? 0 : xpToNext(level));
    }
}
