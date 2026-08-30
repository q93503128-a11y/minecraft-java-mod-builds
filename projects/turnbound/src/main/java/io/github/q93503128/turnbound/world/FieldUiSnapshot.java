package io.github.q93503128.turnbound.world;

import java.util.List;

/** Shared server/client view model for field UI and world bootstrap state. */
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
        List<Travel> travels,
        String loadingStage,
        int loadingPercent
) {
    public enum Mode { NONE, LOADING, QUEST, RESULT, TRAVEL }

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
        loadingStage = loadingStage == null ? "" : loadingStage;
        loadingPercent = Math.max(0, Math.min(100, loadingPercent));
    }

    /** Compatibility constructor for normal field snapshots. */
    public FieldUiSnapshot(
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
        this(active, mode, patrolsCleared, patrolGoal, bossUnlocked, chapterCleared, earnedXp, earnedGold,
                objective, dialogue, reward, encounters, travels, "", 0);
    }

    public static FieldUiSnapshot loading(String stage, int percent) {
        return new FieldUiSnapshot(true, Mode.LOADING, 0, 0, false, false, 0, 0,
                "", "", Reward.none(), List.of(), List.of(), stage, percent);
    }

    public static FieldUiSnapshot inactive() {
        return new FieldUiSnapshot(false, Mode.NONE, 0, 0, false, false, 0, 0,
                "", "", Reward.none(), List.of(), List.of(), "", 0);
    }
}
