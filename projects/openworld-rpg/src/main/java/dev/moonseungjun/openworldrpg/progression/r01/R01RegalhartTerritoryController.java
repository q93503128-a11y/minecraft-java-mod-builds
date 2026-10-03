package dev.moonseungjun.openworldrpg.progression.r01;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Server-owned non-spatial Regalhart territory timing controller.
 *
 * <p>The accepted spatial adapter must supply real core-arena/territory presence and the live boss
 * instance fact. This class does not search for coordinates and cannot spawn Regalhart.</p>
 */
public final class R01RegalhartTerritoryController {
    private R01RegalhartTerritoryController() {
    }

    public static R01RegalhartTerritoryState state(
            MinecraftServer server
    ) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getAttachedOrSet(
                R01RegalhartTerritoryAttachments.TERRITORY,
                R01RegalhartTerritoryState.initial()
        );
    }

    public static R01RegalhartTerritoryState recordValidDefeat(
            MinecraftServer server
    ) {
        long activeWorldTicks = activeWorldTicks(server);
        return replace(
                server,
                state(server).recordValidDefeat(activeWorldTicks)
        );
    }

    /**
     * Presence comes only from the accepted authored core-arena volume.
     */
    public static R01RegalhartTerritoryState updateCoreArenaPresence(
            MinecraftServer server,
            boolean anyPlayerInsideCoreArena
    ) {
        long activeWorldTicks = activeWorldTicks(server);
        return replace(
                server,
                state(server).updatePostEligibilityArenaPresence(
                        activeWorldTicks,
                        anyPlayerInsideCoreArena
                )
        );
    }

    public static boolean repeatEligible(
            MinecraftServer server,
            boolean activeBossInstance
    ) {
        long activeWorldTicks = activeWorldTicks(server);
        return state(server).repeatEligible(
                activeWorldTicks,
                activeBossInstance
        );
    }

    /**
     * Returns true only when the exact 25-second no-eligible-engaged-player window has elapsed.
     * Physical HP/poise/status reset and return-to-start movement remain the later encounter
     * executor's responsibility.
     */
    public static boolean updateEngagementPresence(
            MinecraftServer server,
            boolean activeBossInstance,
            boolean anyEligibleEngagedPlayerInTerritory
    ) {
        long activeWorldTicks = activeWorldTicks(server);
        R01RegalhartTerritoryState next =
                state(server).updateEngagementPresence(
                        activeWorldTicks,
                        activeBossInstance,
                        anyEligibleEngagedPlayerInTerritory
                );
        replace(server, next);
        return next.shouldDisengage(
                activeWorldTicks,
                activeBossInstance
        );
    }

    public static R01RegalhartTerritoryState acknowledgeDisengage(
            MinecraftServer server
    ) {
        Objects.requireNonNull(server, "server");
        return replace(
                server,
                state(server).acknowledgeDisengage()
        );
    }

    private static long activeWorldTicks(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return R01RoadsideTroubleController
                .state(server)
                .activeWorldTicks();
    }

    private static R01RegalhartTerritoryState replace(
            MinecraftServer server,
            R01RegalhartTerritoryState next
    ) {
        ServerLevel overworld = server.overworld();
        R01RegalhartTerritoryState current = state(server);
        if (current.equals(next)) {
            return current;
        }
        overworld.setAttached(
                R01RegalhartTerritoryAttachments.TERRITORY,
                next
        );
        return next;
    }
}
