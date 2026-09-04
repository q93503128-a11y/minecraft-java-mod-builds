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
     * Field encounter preview projected from the same canonical combat data used by BattleEngine.
     * v0.4 does not author a separate recommended-CP table, so recommendedCp is the summed enemy CP reference
     * under the existing canonical CP formula rather than a guaranteed win/loss threshold.
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
            this(id, label, cleared, unlocked, boss, preview(id));
        }

        private Encounter(String id, String label, boolean cleared, boolean unlocked, boolean boss, Preview preview) {
            this(id, label, cleared, unlocked, boss,
                    preview.level(), preview.category(), preview.partySize(), preview.composition(),
                    preview.recommendedCp(), preview.rewardXp(), preview.rewardGold());
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
        objective = playerFacingText(objective);
        dialogue = playerFacingText(dialogue);
        reward = reward == null ? Reward.none() : reward;
        encounters = List.copyOf(encounters == null ? List.of() : encounters);
        travels = List.copyOf(travels == null ? List.of() : travels);
        loadingStage = loadingStage == null ? "" : loadingStage;
        loadingPercent = Math.max(0, Math.min(100, loadingPercent));
    }

    /**
     * Internal quest/enemy IDs are useful in code and save data, but they should never leak into the authored RPG
     * objective copy. Keep the translation at the final field-UI boundary so progression code can stay canonical.
     */
    private static String playerFacingText(String value) {
        if (value == null || value.isBlank()) return "";
        String text = value;
        for (String token : List.of(
                "MQ_P00_01 ", "MQ_P00_02 ", "MQ_P00_03 ",
                "MQ_C01_01 ", "MQ_C01_02 ", "MQ_C01_03 ",
                "MQ_C02_01 ", "MQ_C02_02 ", "MQ_C02_03 ",
                "MQ_C03_01 ", "MQ_C03_02 ", "MQ_C03_03 ",
                "MQ_C04_01 ", "MQ_C04_02 ", "MQ_C04_03 ",
                "MQ_C05_01 ", "MQ_C05_02 ", "MQ_C05_03 ")) {
            text = text.replace(token, "");
        }
        return text
                .replace("P01/P03/P04/F03", "카이렌/브람/엘리시아/변경 사냥꾼")
                .replace("P01", "카이렌")
                .replace("P02", "루메아")
                .replace("P03", "브람")
                .replace("P04", "엘리시아")
                .replace("P05", "리네트")
                .replace("P06", "모르웬")
                .replace("P07", "마리온")
                .replace("P08", "라제")
                .replace("F03", "변경 사냥꾼")
                .replace("B01", "그라울")
                .replace("B02", "베르나")
                .replace("B03", "ORO-7")
                .replace("B04", "콜바크")
                .replace("B05", "세라크")
                .replace("EL03", "녹슨 백부장")
                .replace("E008", "뿌리수호병")
                .replace("E012/E013", "잿빛 사냥개/잉걸술사")
                .replace("E014", "용암굴착수")
                .replace("CORE_FRAGMENT", "Relay 핵 파편")
                .replace("Relay fragment", "Relay 조각")
                .replace("Relay console", "Relay 제어 콘솔")
                .replace("Rift Gate / Hard Boss / Signature Trial", "균열문 / 고난도 재도전 / 전용 장비 시험")
                .replace("Signature Trial", "전용 장비 시험")
                .replace("Hard Boss", "고난도 재도전");
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
