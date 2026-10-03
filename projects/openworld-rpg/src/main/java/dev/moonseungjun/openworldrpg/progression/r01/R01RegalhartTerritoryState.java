package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Persistent non-spatial Regalhart territory timing state.
 *
 * <p>This state intentionally does not contain coordinates, anchor identities, spawn decisions,
 * camera visibility or physical boss transforms. Those remain owned by the later accepted spatial
 * binding. The state only persists canon-closed active-world timing needed across reconnects.</p>
 */
public record R01RegalhartTerritoryState(
        int schemaVersion,
        long lastDefeatActiveTicks,
        long arenaEmptySinceActiveTicks,
        long engagementEmptySinceActiveTicks
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01RegalhartTerritoryState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("regalhart_territory_schema_version")
                            .forGetter(R01RegalhartTerritoryState::schemaVersion),
                    Codec.LONG.fieldOf("last_defeat_active_ticks")
                            .forGetter(R01RegalhartTerritoryState::lastDefeatActiveTicks),
                    Codec.LONG.fieldOf("arena_empty_since_active_ticks")
                            .forGetter(R01RegalhartTerritoryState::arenaEmptySinceActiveTicks),
                    Codec.LONG.fieldOf("engagement_empty_since_active_ticks")
                            .forGetter(R01RegalhartTerritoryState::engagementEmptySinceActiveTicks)
            ).apply(instance, R01RegalhartTerritoryState::new));

    public R01RegalhartTerritoryState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported Regalhart territory schema version: "
                            + schemaVersion
            );
        }
        if (lastDefeatActiveTicks < -1L
                || arenaEmptySinceActiveTicks < -1L
                || engagementEmptySinceActiveTicks < -1L) {
            throw new IllegalArgumentException(
                    "Regalhart territory time sentinels must be >= -1."
            );
        }
        if (lastDefeatActiveTicks < 0L
                && arenaEmptySinceActiveTicks >= 0L) {
            throw new IllegalArgumentException(
                    "Regalhart arena-empty time requires a previous valid defeat."
            );
        }
        if (lastDefeatActiveTicks >= 0L
                && arenaEmptySinceActiveTicks >= 0L
                && arenaEmptySinceActiveTicks
                        < R01RegalhartTerritoryRules
                                .repeatEligibilityActiveTick(
                                        lastDefeatActiveTicks
                                )) {
            throw new IllegalArgumentException(
                    "Regalhart arena-empty timing cannot begin before repeat eligibility."
            );
        }
    }

    public static R01RegalhartTerritoryState initial() {
        return new R01RegalhartTerritoryState(
                CURRENT_SCHEMA_VERSION,
                -1L,
                -1L,
                -1L
        );
    }

    public R01RegalhartTerritoryState recordValidDefeat(
            long activeWorldTicks
    ) {
        requireTick(activeWorldTicks);
        return new R01RegalhartTerritoryState(
                schemaVersion,
                activeWorldTicks,
                -1L,
                -1L
        );
    }

    /**
     * Called by the later authored core-arena volume adapter.
     *
     * <p>Empty time before the 20-minute repeat gate never counts toward the required post-gate
     * 60-second empty window.</p>
     */
    public R01RegalhartTerritoryState updatePostEligibilityArenaPresence(
            long activeWorldTicks,
            boolean anyPlayerInsideCoreArena
    ) {
        requireTick(activeWorldTicks);
        if (lastDefeatActiveTicks < 0L) {
            return arenaEmptySinceActiveTicks == -1L
                    ? this
                    : copy(-1L, engagementEmptySinceActiveTicks);
        }

        long eligibilityTick =
                R01RegalhartTerritoryRules
                        .repeatEligibilityActiveTick(
                                lastDefeatActiveTicks
                        );
        if (activeWorldTicks < eligibilityTick
                || anyPlayerInsideCoreArena) {
            return arenaEmptySinceActiveTicks == -1L
                    ? this
                    : copy(-1L, engagementEmptySinceActiveTicks);
        }

        if (arenaEmptySinceActiveTicks >= 0L) {
            return this;
        }
        return copy(
                activeWorldTicks,
                engagementEmptySinceActiveTicks
        );
    }

    public boolean repeatEligible(
            long activeWorldTicks,
            boolean activeBossInstance
    ) {
        requireTick(activeWorldTicks);
        if (activeBossInstance
                || lastDefeatActiveTicks < 0L
                || arenaEmptySinceActiveTicks < 0L
                || !R01RegalhartTerritoryRules.repeatDelayElapsed(
                        lastDefeatActiveTicks,
                        activeWorldTicks
                )) {
            return false;
        }

        return R01RegalhartTerritoryRules.emptyWindowElapsed(
                arenaEmptySinceActiveTicks,
                R01RegalhartTerritoryRules
                        .POST_ELIGIBILITY_ARENA_EMPTY_ACTIVE_TICKS,
                activeWorldTicks
        );
    }

    /**
     * Called by the later Regalhart territory adapter while a boss instance exists.
     */
    public R01RegalhartTerritoryState updateEngagementPresence(
            long activeWorldTicks,
            boolean activeBossInstance,
            boolean anyEligibleEngagedPlayerInTerritory
    ) {
        requireTick(activeWorldTicks);
        if (!activeBossInstance
                || anyEligibleEngagedPlayerInTerritory) {
            return engagementEmptySinceActiveTicks == -1L
                    ? this
                    : copy(
                            arenaEmptySinceActiveTicks,
                            -1L
                    );
        }

        if (engagementEmptySinceActiveTicks >= 0L) {
            return this;
        }
        return copy(
                arenaEmptySinceActiveTicks,
                activeWorldTicks
        );
    }

    public boolean shouldDisengage(
            long activeWorldTicks,
            boolean activeBossInstance
    ) {
        requireTick(activeWorldTicks);
        return activeBossInstance
                && engagementEmptySinceActiveTicks >= 0L
                && R01RegalhartTerritoryRules.emptyWindowElapsed(
                        engagementEmptySinceActiveTicks,
                        R01RegalhartTerritoryRules
                                .DISENGAGE_EMPTY_ACTIVE_TICKS,
                        activeWorldTicks
                );
    }

    public R01RegalhartTerritoryState acknowledgeDisengage() {
        return engagementEmptySinceActiveTicks == -1L
                ? this
                : copy(
                        arenaEmptySinceActiveTicks,
                        -1L
                );
    }

    private R01RegalhartTerritoryState copy(
            long nextArenaEmptySince,
            long nextEngagementEmptySince
    ) {
        return new R01RegalhartTerritoryState(
                schemaVersion,
                lastDefeatActiveTicks,
                nextArenaEmptySince,
                nextEngagementEmptySince
        );
    }

    private static void requireTick(long activeWorldTicks) {
        if (activeWorldTicks < 0L) {
            throw new IllegalArgumentException(
                    "Regalhart active-world time must be non-negative."
            );
        }
    }
}
