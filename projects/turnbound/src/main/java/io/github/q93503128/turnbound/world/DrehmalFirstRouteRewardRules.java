package io.github.q93503128.turnbound.world;

import java.util.Set;

/**
 * First-route one-time reward policy for Drehmal.
 *
 * <p>The balance canon expects the first ten-pull within roughly the first 30-45 minutes. The first meaningful
 * Capital Valley combat milestone therefore tops the player up to a 3,000-Crystal summon launch reward without
 * paying that launch reward twice when both eligible milestones are later cleared.</p>
 */
public final class DrehmalFirstRouteRewardRules {
    private static final int WARNING_CAVE_BASE_CRYSTAL = 300;
    private static final int FIRST_SUMMON_BUDGET = 3_000;

    private DrehmalFirstRouteRewardRules() {}

    public static int supplementalCrystal(String encounterId, boolean firstClear, Set<String> alreadyCleared) {
        if (!firstClear || encounterId == null) return 0;
        Set<String> clears = alreadyCleared == null ? Set.of() : alreadyCleared;

        return switch (encounterId) {
            case DrehmalContentUnlocks.WARNING_CAVE_ELITE ->
                    WARNING_CAVE_BASE_CRYSTAL
                            + (clears.contains(DrehmalContentUnlocks.DRABYEL_ROAD)
                            ? 0 : FIRST_SUMMON_BUDGET - WARNING_CAVE_BASE_CRYSTAL);
            case DrehmalContentUnlocks.DRABYEL_ROAD ->
                    clears.contains(DrehmalContentUnlocks.WARNING_CAVE_ELITE) ? 0 : FIRST_SUMMON_BUDGET;
            default -> 0;
        };
    }

    public static boolean unlocksSummonNow(String encounterId, boolean firstClear, Set<String> alreadyCleared) {
        if (!firstClear || encounterId == null) return false;
        Set<String> clears = alreadyCleared == null ? Set.of() : alreadyCleared;
        return switch (encounterId) {
            case DrehmalContentUnlocks.WARNING_CAVE_ELITE ->
                    !clears.contains(DrehmalContentUnlocks.DRABYEL_ROAD);
            case DrehmalContentUnlocks.DRABYEL_ROAD ->
                    !clears.contains(DrehmalContentUnlocks.WARNING_CAVE_ELITE);
            default -> false;
        };
    }
}
