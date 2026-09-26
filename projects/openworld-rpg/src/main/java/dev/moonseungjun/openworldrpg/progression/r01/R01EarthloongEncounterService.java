package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-authoritative Earthloong contribution and defeat bridge.
 *
 * <p>Damage callers record only after project damage has actually applied. Support callers must
 * invoke the support entry point only after their authored support/control effect has been
 * validated as real contribution. Proximity and party membership never call this service.</p>
 */
public final class R01EarthloongEncounterService {
    private static final String EARTHLOONG_ID =
            "threateningly_mobs:the_earthloong";

    private R01EarthloongEncounterService() {
    }

    public static R01EarthloongEncounterState state(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getAttachedOrSet(
                R01EarthloongEncounterAttachments.EARTHLOONG_ENCOUNTERS,
                R01EarthloongEncounterState.initial()
        );
    }

    public static boolean recordDamageContribution(
            LivingEntity earthloong,
            ServerPlayer player
    ) {
        return recordValidatedContribution(earthloong, player);
    }

    /**
     * Future healing/barrier/control/revive adapters call this only after a non-no-op contribution
     * has already been accepted by their own server authority.
     */
    public static boolean recordValidatedSupportContribution(
            LivingEntity earthloong,
            ServerPlayer player
    ) {
        return recordValidatedContribution(earthloong, player);
    }

    public static boolean resolveDefeat(LivingEntity earthloong) {
        Objects.requireNonNull(earthloong, "earthloong");
        if (!isEarthloong(earthloong)
                || !(earthloong.level() instanceof ServerLevel level)) {
            return false;
        }
        MinecraftServer server = level.getServer();
        String encounterId = encounterId(earthloong);
        R01EarthloongEncounterState before = state(server);
        if (!before.activeEncounters().containsKey(encounterId)) {
            return false;
        }

        replace(server, before.completeEncounter(encounterId));
        reconcileOnlineFinalizations(server);
        return true;
    }

    /**
     * Resolves one persisted personal boss participation on join or immediately after boss death.
     *
     * <p>The normal boss contribution is always personal for an eligible contributor. Main-story
     * first-clear state is committed only when that player personally has an active quarry run.
     * The separate dungeon-completion reward uses bounded whole-run class attribution rather than
     * whichever class happens to be active when the boss dies.</p>
     */
    public static FinalizationResult reconcilePendingFinalization(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return FinalizationResult.none();
        }

        String playerUuid = player.getUUID().toString();
        Optional<R01EarthloongEncounterState.PendingFinalization> pendingOptional =
                state(server).pendingFinalization(playerUuid);
        if (pendingOptional.isEmpty()) {
            return FinalizationResult.none();
        }
        R01EarthloongEncounterState.PendingFinalization pending =
                pendingOptional.orElseThrow();

        R01RewardService.grantEarthloongFirstBossLayer(
                player,
                pending.rewardClass()
        );

        boolean firstClearCommitted = false;
        R01PlayerState personal = R01PlayerStateService.state(player);
        if (!personal.quarry().firstClear()
                && personal.opening().mainStage() == R01MainStage.QUARRY_DUNGEON_ACTIVE
                && personal.quarry().runId() > 0L
                && personal.quarry().runState().filter("active"::equals).isPresent()) {
            R01PlayerStateService.markEarthloongFirstClear(player);
            firstClearCommitted = true;
        }
        R01QuarryRunAttributionService.reconcileFirstClearCompletion(player);
        R01EarthloongFirstClearRewardService.reconcilePending(player);

        replace(
                server,
                state(server).clearPendingFinalization(
                        playerUuid,
                        pending.encounterId()
                )
        );
        return new FinalizationResult(
                true,
                pending.rewardClass(),
                firstClearCommitted
        );
    }

    private static boolean recordValidatedContribution(
            LivingEntity earthloong,
            ServerPlayer player
    ) {
        Objects.requireNonNull(earthloong, "earthloong");
        Objects.requireNonNull(player, "player");
        if (!isEarthloong(earthloong)
                || !(earthloong.level() instanceof ServerLevel level)
                || player.level() != level) {
            return false;
        }

        Optional<RootClass> activeClass =
                PlayerProgressionService.state(player).activeClass();
        if (activeClass.isEmpty()) {
            return false;
        }

        MinecraftServer server = level.getServer();
        String encounterId = encounterId(earthloong);
        String playerUuid = player.getUUID().toString();
        R01EarthloongEncounterState current = state(server);
        boolean wasParticipant = current.hasParticipant(encounterId, playerUuid);
        RootClass contributionClass = activeClass.orElseThrow();
        replace(
                server,
                current.recordParticipation(
                        encounterId,
                        playerUuid,
                        contributionClass
                )
        );
        R01QuarryRunAttributionService.recordContribution(
                player,
                R01QuarryRunContribution.EARTHLOONG_COMBAT,
                contributionClass
        );
        return !wasParticipant;
    }

    private static void reconcileOnlineFinalizations(MinecraftServer server) {
        for (String playerUuid
                : new ArrayList<>(state(server).pendingFinalizations().keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(
                    UUID.fromString(playerUuid)
            );
            if (player != null) {
                reconcilePendingFinalization(player);
            }
        }
    }

    private static boolean isEarthloong(LivingEntity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && EARTHLOONG_ID.equals(id.toString());
    }

    private static String encounterId(LivingEntity earthloong) {
        return "openworld_rpg:r01/earthloong/" + earthloong.getUUID();
    }

    private static R01EarthloongEncounterState replace(
            MinecraftServer server,
            R01EarthloongEncounterState next
    ) {
        ServerLevel overworld = server.overworld();
        R01EarthloongEncounterState current = state(server);
        if (current.equals(next)) {
            return current;
        }
        overworld.setAttached(
                R01EarthloongEncounterAttachments.EARTHLOONG_ENCOUNTERS,
                next
        );
        return next;
    }

    public record FinalizationResult(
            boolean finalized,
            RootClass rewardClass,
            boolean firstClearCommitted
    ) {
        public static FinalizationResult none() {
            return new FinalizationResult(false, null, false);
        }

        public FinalizationResult {
            if (finalized && rewardClass == null) {
                throw new IllegalArgumentException(
                        "Finalized Earthloong reward requires its qualifying class."
                );
            }
            if (!finalized && (rewardClass != null || firstClearCommitted)) {
                throw new IllegalArgumentException(
                        "Non-finalized Earthloong result cannot carry reward state."
                );
            }
        }
    }
}
