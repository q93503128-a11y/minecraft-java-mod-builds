package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.world.spatial.R01RegalhartSpatialAuthority;
import dev.moonseungjun.openworldrpg.world.spatial.R01RegalhartSpatialBindingData;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-owned Regalhart repeat/disengage controller.
 *
 * <p>Persistent timing is kept in the world attachment while accepted Azari Rootshade bounds and
 * the three start anchors come from the dedicated Regalhart spatial binding.</p>
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
     * Uses the accepted Rootshade binding instead of caller-supplied booleans for physical presence.
     *
     * <p>The caller still owns the combat eligibility predicate because "eligible engaged" is an
     * encounter-state fact, not a coordinate fact.</p>
     */
    public static boolean updateSpatialPresence(
            MinecraftServer server,
            boolean activeBossInstance,
            Predicate<ServerPlayer> eligibleEngagedPlayer
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(
                eligibleEngagedPlayer,
                "eligibleEngagedPlayer"
        );
        ServerLevel overworld = server.overworld();

        boolean anyPlayerInsideCoreArena =
                server.getPlayerList().getPlayers().stream()
                        .filter(player -> player.level() == overworld)
                        .filter(player -> !player.isSpectator())
                        .anyMatch(player ->
                                R01RegalhartSpatialAuthority.insideCoreArena(
                                        player.getX(),
                                        player.getZ()
                                )
                        );
        updateCoreArenaPresence(server, anyPlayerInsideCoreArena);

        boolean anyEligibleEngagedPlayerInTerritory =
                server.getPlayerList().getPlayers().stream()
                        .filter(player -> player.level() == overworld)
                        .filter(player -> !player.isSpectator())
                        .filter(eligibleEngagedPlayer)
                        .anyMatch(player ->
                                R01RegalhartSpatialAuthority.insideTerritory(
                                        player.getX(),
                                        player.getZ()
                                )
                        );
        return updateEngagementPresence(
                server,
                activeBossInstance,
                anyEligibleEngagedPlayerInTerritory
        );
    }

    /**
     * The same world seed + persisted Regalhart cycle always resolves the same authored start anchor.
     */
    public static Optional<R01RegalhartSpatialBindingData.StartAnchor>
            selectedRepeatStartAnchor(
                    MinecraftServer server,
                    boolean activeBossInstance
            ) {
        Objects.requireNonNull(server, "server");
        if (!repeatEligible(server, activeBossInstance)) {
            return Optional.empty();
        }
        R01RegalhartTerritoryState current = state(server);
        return Optional.of(
                R01RegalhartSpatialAuthority.selectStartAnchor(
                        server.overworld().getSeed(),
                        current.cycleIndex()
                )
        );
    }

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
