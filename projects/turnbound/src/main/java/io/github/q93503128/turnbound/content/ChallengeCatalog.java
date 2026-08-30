package io.github.q93503128.turnbound.content;

import java.util.List;

/** v0.4 Challenge list. Auto-evaluable is false where the source does not define enough semantics. */
public final class ChallengeCatalog {
    public record Challenge(String id, int ordinal, String label, int crystal, int gold,
                            String cosmeticRule, boolean autoEvaluable, String unresolvedReason) {}

    private static final List<Challenge> ALL = List.of(
            pending(1, "CH01_NO_DEATH_1", "No Death 1", "No Death 1/2 have no distinct encounter/scope criteria in v0.4"),
            pending(2, "CH02_NO_DEATH_2", "No Death 2", "No Death 1/2 have no distinct encounter/scope criteria in v0.4"),
            exact(3, "CH03_UNDER_12_ALLY_ACTIONS", "Win under 12 ally actions"),
            exact(4, "CH04_UNDER_20_ALLY_ACTIONS", "Win under 20 ally actions"),
            exact(5, "CH05_REVIVE_AND_WIN", "Revive once and win"),
            pending(6, "CH06_COUNTER_5", "Counter 5 times", "v0.4 does not state whether the count is per-battle or cumulative"),
            pending(7, "CH07_FOLLOW_UP_6", "Follow-up 6 times", "v0.4 does not state whether the count is per-battle or cumulative"),
            pending(8, "CH08_GAUGE_DELAY_800", "Gauge delay total 800", "v0.4 does not state whether the total is per-battle or cumulative"),
            pending(9, "CH09_BARRIER_ABSORB_1500", "Barrier absorb 1500", "v0.4 does not state whether the total is per-battle or cumulative"),
            exact(10, "CH10_HEAL_2000_ONE_BATTLE", "Heal 2000 in one battle"),
            exact(11, "CH11_FINISH_LOW_HP", "Finish with ally HP under 10%"),
            exact(12, "CH12_KILL_E003_BEFORE_EXPLOSION", "Kill E003 before explosion"),
            exact(13, "CH13_SURVIVE_E003_EXPLOSION", "Survive E003 explosion"),
            exact(14, "CH14_ELITE_NO_REVIVE", "Defeat Elite without revive"),
            exact(15, "CH15_HARD_B01", "Defeat B01 Hard"),
            exact(16, "CH16_HARD_B02", "Defeat B02 Hard"),
            exact(17, "CH17_HARD_B03", "Defeat B03 Hard"),
            exact(18, "CH18_HARD_B04", "Defeat B04 Hard"),
            exact(19, "CH19_HARD_B05", "Defeat B05 Hard"),
            exact(20, "CH20_RIFT_F30", "Rift Gate Floor 30")
    );

    private ChallengeCatalog() {}
    public static List<Challenge> all() { return ALL; }
    public static Challenge get(String id) {
        return ALL.stream().filter(c -> c.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Challenge " + id));
    }

    private static Challenge exact(int ordinal, String id, String label) {
        return new Challenge(id, ordinal, label, 150, 1_500, "COSMETIC_TITLE_OR_CODEX_BADGE_UNASSIGNED", true, "");
    }
    private static Challenge pending(int ordinal, String id, String label, String reason) {
        return new Challenge(id, ordinal, label, 150, 1_500, "COSMETIC_TITLE_OR_CODEX_BADGE_UNASSIGNED", false, reason);
    }
}
