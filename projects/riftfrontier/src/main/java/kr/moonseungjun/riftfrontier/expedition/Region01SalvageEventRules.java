package kr.moonseungjun.riftfrontier.expedition;

/** Pure Region 01 event rules kept free of Minecraft runtime classes for JVM contract tests. */
public final class Region01SalvageEventRules {
    public static final int TRIGGER_SALVAGE = 2;

    private Region01SalvageEventRules() {}

    public static boolean isBlackoutDue(int recoveredSalvage) {
        return recoveredSalvage == TRIGGER_SALVAGE;
    }
}
