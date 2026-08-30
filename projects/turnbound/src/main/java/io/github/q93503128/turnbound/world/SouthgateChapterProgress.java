package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;

import java.util.LinkedHashSet;
import java.util.Set;

/** Session-scoped P2 quest/reward state aligned to v0.4 MQ_C01. P3 will persist it. */
public final class SouthgateChapterProgress {
    public record RewardReceipt(String encounterId, int xp, int gold, boolean firstClear,
                                boolean bossUnlocked, boolean chapterCleared,
                                int crystal, int starEssence, boolean t2ChoiceBox, boolean p08Unlocked) {}

    private final Set<String> cleared = new LinkedHashSet<>();
    private final Set<String> activatedRelays = new LinkedHashSet<>();
    private int earnedXp;
    private int earnedGold;
    private int summonCrystal;
    private int starEssence;
    private int t2ChoiceBoxes;
    private boolean archiveUnlocked;
    private boolean autoUnlocked;
    private boolean speedUnlocked;
    private boolean p08Unlocked;

    public RewardReceipt recordVictory(String encounterId) {
        var spec = SouthgateEncounterCatalog.spec(encounterId);
        if (spec.boss() && !bossUnlocked()) throw new IllegalStateException("B01 is locked until MQ_C01_01 and MQ_C01_02 are complete");
        boolean first = cleared.add(encounterId);
        int crystal = 0;
        int essence = 0;
        boolean box = false;
        boolean p08 = false;
        if (first) {
            earnedXp += spec.rewardXp();
            earnedGold += spec.rewardGold();
            if (spec.boss()) {
                crystal = 1200;
                essence = 60;
                summonCrystal += crystal;
                starEssence += essence;
                t2ChoiceBoxes++;
                box = true;
                archiveUnlocked = true;
                autoUnlocked = true;
                speedUnlocked = true;
                p08Unlocked = true;
                p08 = true;
            }
        }
        return new RewardReceipt(encounterId, first ? spec.rewardXp() : 0, first ? spec.rewardGold() : 0,
                first, bossUnlocked(), chapterCleared(), crystal, essence, box, p08);
    }

    public boolean cleared(String encounterId) { return cleared.contains(encounterId); }
    public int normalClears() {
        int count = 0;
        for (String id : SouthgateEncounterCatalog.normalEncounterIds()) if (cleared.contains(id)) count++;
        return count;
    }
    /** MQ_C01_01 counter: only M01/M02 are mandatory patrol clears. */
    public int patrolsCleared() {
        int count = 0;
        if (cleared(SouthgateEncounterCatalog.ENC_M01)) count++;
        if (cleared(SouthgateEncounterCatalog.ENC_M02)) count++;
        return count;
    }
    public int patrolGoal() { return 2; }
    public boolean meadowRouteUnlocked() { return patrolsCleared() == patrolGoal(); }
    public boolean unstableStageComplete() { return cleared(SouthgateEncounterCatalog.ENC_M04); }
    public boolean bossUnlocked() { return meadowRouteUnlocked() && unstableStageComplete(); }
    public boolean chapterCleared() { return cleared(SouthgateEncounterCatalog.B01_GRAUL); }
    public int earnedXp() { return earnedXp; }
    public int earnedGold() { return earnedGold; }
    public int summonCrystal() { return summonCrystal; }
    public int starEssence() { return starEssence; }
    public int t2ChoiceBoxes() { return t2ChoiceBoxes; }
    public boolean archiveUnlocked() { return archiveUnlocked; }
    public boolean autoUnlocked() { return autoUnlocked; }
    public boolean speedUnlocked() { return speedUnlocked; }
    public boolean p08Unlocked() { return p08Unlocked; }
    public Set<String> clearedView() { return Set.copyOf(cleared); }

    public boolean activateRelay(String relayId) {
        if (!FieldTravelCatalog.destinations().stream().anyMatch(destination -> destination.id().equals(relayId))) {
            throw new IllegalArgumentException("Unknown relay " + relayId);
        }
        if (FieldTravelCatalog.RELAY_A02.equals(relayId) && !meadowRouteUnlocked()) return false;
        return activatedRelays.add(relayId);
    }

    public boolean relayActivated(String relayId) { return activatedRelays.contains(relayId); }
    public Set<String> activatedRelaysView() { return Set.copyOf(activatedRelays); }
}
