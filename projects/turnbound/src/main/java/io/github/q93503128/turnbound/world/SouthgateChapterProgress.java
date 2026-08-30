package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;

import java.util.LinkedHashSet;
import java.util.Set;

/** Session-scoped P2 quest/reward/travel state. P3 will persist the same concepts in saveSchemaVersion 4. */
public final class SouthgateChapterProgress {
    public record RewardReceipt(String encounterId, int xp, int gold, boolean firstClear, boolean bossUnlocked, boolean chapterCleared) {}

    private final Set<String> cleared = new LinkedHashSet<>();
    private final Set<String> activatedRelays = new LinkedHashSet<>();
    private int earnedXp;
    private int earnedGold;

    public RewardReceipt recordVictory(String encounterId) {
        var spec = SouthgateEncounterCatalog.spec(encounterId);
        if (spec.boss() && !bossUnlocked()) {
            throw new IllegalStateException("B01 is locked until ENC_M01~M05 are cleared");
        }
        boolean first = cleared.add(encounterId);
        if (first) {
            earnedXp += spec.rewardXp();
            earnedGold += spec.rewardGold();
        }
        return new RewardReceipt(encounterId, first ? spec.rewardXp() : 0, first ? spec.rewardGold() : 0,
                first, bossUnlocked(), chapterCleared());
    }

    public boolean cleared(String encounterId) { return cleared.contains(encounterId); }

    public int patrolsCleared() {
        int count = 0;
        for (String id : SouthgateEncounterCatalog.normalEncounterIds()) if (cleared.contains(id)) count++;
        return count;
    }

    public int patrolGoal() { return SouthgateEncounterCatalog.normalEncounterIds().size(); }
    public boolean bossUnlocked() { return patrolsCleared() == patrolGoal(); }
    public boolean chapterCleared() { return cleared.contains(SouthgateEncounterCatalog.B01_GRAUL); }
    public int earnedXp() { return earnedXp; }
    public int earnedGold() { return earnedGold; }
    public Set<String> clearedView() { return Set.copyOf(cleared); }

    public boolean activateRelay(String relayId) {
        if (!FieldTravelCatalog.destinations().stream().anyMatch(destination -> destination.id().equals(relayId))) {
            throw new IllegalArgumentException("Unknown relay " + relayId);
        }
        if (FieldTravelCatalog.RELAY_A02.equals(relayId) && !chapterCleared()) {
            return false;
        }
        return activatedRelays.add(relayId);
    }

    public boolean relayActivated(String relayId) { return activatedRelays.contains(relayId); }
    public Set<String> activatedRelaysView() { return Set.copyOf(activatedRelays); }
}
