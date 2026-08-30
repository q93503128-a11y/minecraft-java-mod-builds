package io.github.q93503128.turnbound.world;

import java.util.List;

/** Shared server/client view model for Southgate field UI. */
public record FieldUiSnapshot(
        boolean active,
        Mode mode,
        int patrolsCleared,
        int patrolGoal,
        boolean bossUnlocked,
        boolean chapterCleared,
        int earnedXp,
        int earnedGold,
        String objective,
        String dialogue,
        Reward reward,
        List<Encounter> encounters,
        List<Travel> travels
) {
    public enum Mode { NONE, QUEST, RESULT, TRAVEL }

    public record Reward(String encounterLabel, int xp, int gold, boolean firstClear, boolean chapterCleared) {
        public static Reward none() { return new Reward("", 0, 0, false, false); }
    }

    public record Encounter(String id, String label, boolean cleared, boolean unlocked, boolean boss) {}
    public record Travel(String id, String label, boolean unlocked, boolean current) {}

    public FieldUiSnapshot {
        mode = mode == null ? Mode.NONE : mode;
        objective = objective == null ? "" : objective;
        dialogue = dialogue == null ? "" : dialogue;
        reward = reward == null ? Reward.none() : reward;
        encounters = List.copyOf(encounters == null ? List.of() : encounters);
        travels = List.copyOf(travels == null ? List.of() : travels);
    }

    public static FieldUiSnapshot inactive() {
        return new FieldUiSnapshot(false, Mode.NONE, 0, 0, false, false, 0, 0,
                "", "", Reward.none(), List.of(), List.of());
    }
}
