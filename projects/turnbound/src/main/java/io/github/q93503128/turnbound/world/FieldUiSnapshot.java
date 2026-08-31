package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.EquipmentRules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Canonical field encounter preview. The legacy five-argument constructor remains the call-site API;
     * all preview details are projected from the same encounter/definition data used by BattleEngine.
     */
    public record Encounter(
            String id,
            String label,
            boolean cleared,
            boolean unlocked,
            boolean boss,
            int level,
            String category,
            int partySize,
            String composition,
            int recommendedCp,
            int rewardXp,
            int rewardGold
    ) {
        public Encounter(String id, String label, boolean cleared, boolean unlocked, boolean boss) {
            this(id, label, cleared, unlocked, boss,
                    preview(id).level(), preview(id).category(), preview(id).partySize(), preview(id).composition(),
                    preview(id).recommendedCp(), preview(id).rewardXp(), preview(id).rewardGold());
        }

        private static Preview preview(String encounterId) {
            try {
                V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(encounterId);
                Map<String, Integer> counts = new LinkedHashMap<>();
                int cp = 0;
                boolean elite = false;
                for (String enemyId : spec.enemies()) {
                    var definition = CanonicalData.definition(enemyId, spec.level(), 0, false);
                    counts.merge(definition.name(), 1, Integer::sum);
                    cp += EquipmentRules.combatPower(definition.stats());
                    elite |= enemyId.startsWith("EL");
                }
                String composition = counts.entrySet().stream()
                        .map(entry -> entry.getValue() > 1 ? entry.getKey() + "×" + entry.getValue() : entry.getKey())
                        .reduce((left, right) -> left + " · " + right).orElse("-");
                String category = spec.boss() ? "BOSS" : elite ? "ELITE" : "NORMAL";
                return new Preview(spec.level(), category, spec.enemies().size(), composition, cp,
                        V04Catalogs.battleXp(spec), V04Catalogs.battleGold(spec));
            } catch (RuntimeException ignored) {
                return new Preview(0, bossFallback(encounterId) ? "BOSS" : "NORMAL", 0, "-", 0, 0, 0);
            }
        }

        private static boolean bossFallback(String id) {
            return id != null && (id.startsWith("BATTLE_B") || id.startsWith("HARD_B"));
        }

        private record Preview(int level, String category, int partySize, String composition,
                               int recommendedCp, int rewardXp, int rewardGold) {}
    }

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
