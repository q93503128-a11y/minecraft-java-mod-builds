package dev.moonseungjun.openworldrpg.multiplayer;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectActiveEncounterRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectDodgeRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerActionRuntime;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongEncounterService;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-owned Downed / revive backend for exact encounter-scoped co-op.
 *
 * <p>Final lethal interception, the non-encounter nearby-play-context branch and physical revive
 * interaction transport remain separate gates. Those callers must prove their own authority before
 * invoking these transitions.</p>
 */
public final class ProjectDownedRuntime {
    public static final String REVIVE_ACTION_ID = "openworld_rpg:action/revive";
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private ProjectDownedRuntime() {}

    public static boolean isDowned(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return isDowned(player.getUUID(), player.level().getGameTime());
    }

    public static boolean isDowned(UUID playerId, long nowTick) {
        Objects.requireNonNull(playerId, "playerId");
        Session session = SESSIONS.get(playerId);
        return session != null && session.state().snapshot(nowTick).downed();
    }

    public static EnterResult tryEnterEncounterDowned(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!player.isAlive() || player.isSpectator()) {
            return EnterResult.rejected(EnterStatus.INVALID_PLAYER);
        }
        var encounter = ProjectActiveEncounterRuntime.activeEncounterFor(player).orElse(null);
        if (encounter == null) return EnterResult.rejected(EnterStatus.NO_ACTIVE_ENCOUNTER);
        long nowTick = player.level().getGameTime();
        if (!hasEligibleEncounterRescuer(player, encounter, nowTick)) {
            return EnterResult.rejected(EnterStatus.NO_LIVING_RESCUER);
        }

        Session existing = SESSIONS.get(player.getUUID());
        if (existing != null
                && existing.state().downed()
                && !ProjectActiveEncounterRuntime.sameEncounter(existing.encounter(), encounter)) {
            return EnterResult.rejected(EnterStatus.OTHER_ENCOUNTER_STATE);
        }
        Session session = existing != null
                ? existing
                : new Session(new ProjectDownedRuntimeState(), encounter);
        var status = session.state().tryEnter(nowTick);
        if (status != ProjectDownedRuntimeState.EnterStatus.ENTERED) {
            return EnterResult.rejected(
                    status == ProjectDownedRuntimeState.EnterStatus.RESCUE_FATIGUE
                            ? EnterStatus.RESCUE_FATIGUE
                            : EnterStatus.ALREADY_DOWNED
            );
        }

        SESSIONS.put(player.getUUID(), new Session(session.state(), encounter));
        /*
         * A player who becomes Downed while reviving someone else can no longer own that revive
         * channel. Clear the target session before resetting the shared action state so a stale
         * reviver id cannot complete after the player has become incapacitated.
         */
        interruptRevive(player);
        ProjectDodgeRuntime.reset(player);
        ProjectPlayerActionRuntime.reset(player);
        CombatStateServices.defenseStates().getOrCreate(player.getUUID()).releaseGuard();
        return new EnterResult(
                EnterStatus.ENTERED,
                Optional.of(encounter),
                session.state().snapshot(nowTick).rescueTicksRemaining()
        );
    }

    public static BeginReviveResult beginEncounterReviveAfterValidatedInteraction(
            ServerPlayer reviver,
            ServerPlayer downedPlayer
    ) {
        Objects.requireNonNull(reviver, "reviver");
        Objects.requireNonNull(downedPlayer, "downedPlayer");
        long nowTick = reviver.level().getGameTime();
        if (reviver == downedPlayer
                || !eligibleLivingRescuerState(
                        reviver.isAlive(),
                        reviver.isSpectator(),
                        isDowned(reviver.getUUID(), nowTick)
                )
                || reviver.level() != downedPlayer.level()) {
            return BeginReviveResult.rejected(BeginReviveStatus.INVALID_REVIVER);
        }
        Session session = SESSIONS.get(downedPlayer.getUUID());
        if (session == null || !session.state().downed()) {
            return BeginReviveResult.rejected(BeginReviveStatus.NOT_DOWNED);
        }
        if (!ProjectActiveEncounterRuntime.isParticipantOf(reviver, session.encounter())) {
            return BeginReviveResult.rejected(BeginReviveStatus.WRONG_ENCOUNTER);
        }
        if (!ProjectPlayerActionRuntime.canStartAction(reviver)) {
            return BeginReviveResult.rejected(BeginReviveStatus.REVIVER_BUSY);
        }

        var start = session.state().tryBeginRevive(reviver.getUUID(), nowTick);
        if (start != ProjectDownedRuntimeState.BeginReviveStatus.STARTED) {
            return BeginReviveResult.rejected(switch (start) {
                case NOT_DOWNED -> BeginReviveStatus.NOT_DOWNED;
                case RESCUE_EXPIRED -> BeginReviveStatus.RESCUE_EXPIRED;
                case ALREADY_CHANNELING -> BeginReviveStatus.ALREADY_CHANNELING;
                case OTHER_REVIVER_CHANNELING -> BeginReviveStatus.OTHER_REVIVER_CHANNELING;
                case INSUFFICIENT_RESCUE_TIME -> BeginReviveStatus.INSUFFICIENT_RESCUE_TIME;
                case STARTED -> throw new IllegalStateException("Started revive cannot reject.");
            });
        }

        var action = ProjectPlayerActionRuntime.beginAction(
                reviver,
                new ProjectPlayerActionRuntime.ActionSpec(
                        REVIVE_ACTION_ID,
                        ProjectDownedRuntimeState.BASE_REVIVE_CHANNEL_TICKS,
                        ProjectDownedRuntimeState.BASE_REVIVE_CHANNEL_TICKS,
                        1.0
                )
        );
        if (!action.accepted()) {
            session.state().interruptRevive(reviver.getUUID());
            return BeginReviveResult.rejected(BeginReviveStatus.REVIVER_BUSY);
        }
        return new BeginReviveResult(
                BeginReviveStatus.STARTED,
                session.state().snapshot(nowTick).reviveChannelTicksRemaining()
        );
    }

    public static CompleteReviveResult completeEncounterReviveAfterValidatedInteraction(
            ServerPlayer reviver,
            ServerPlayer downedPlayer
    ) {
        Objects.requireNonNull(reviver, "reviver");
        Objects.requireNonNull(downedPlayer, "downedPlayer");
        Session session = SESSIONS.get(downedPlayer.getUUID());
        if (session == null) return CompleteReviveResult.rejected(CompleteReviveStatus.NOT_DOWNED);
        long nowTick = reviver.level().getGameTime();
        if (reviver.level() != downedPlayer.level()
                || !eligibleLivingRescuerState(
                        reviver.isAlive(),
                        reviver.isSpectator(),
                        isDowned(reviver.getUUID(), nowTick)
                )
                || !ProjectActiveEncounterRuntime.isParticipantOf(reviver, session.encounter())) {
            interruptRevive(reviver);
            return CompleteReviveResult.rejected(CompleteReviveStatus.INVALID_CONTEXT);
        }

        var status = session.state().tryCompleteRevive(reviver.getUUID(), nowTick);
        if (status != ProjectDownedRuntimeState.CompleteReviveStatus.REVIVED) {
            return CompleteReviveResult.rejected(switch (status) {
                case NOT_DOWNED -> CompleteReviveStatus.NOT_DOWNED;
                case RESCUE_EXPIRED -> CompleteReviveStatus.RESCUE_EXPIRED;
                case NOT_CHANNEL_OWNER -> CompleteReviveStatus.NOT_CHANNEL_OWNER;
                case TOO_EARLY -> CompleteReviveStatus.TOO_EARLY;
                case REVIVED -> throw new IllegalStateException("Revived state cannot reject.");
            });
        }

        downedPlayer.setHealth((float) (downedPlayer.getMaxHealth()
                * ProjectDownedRuntimeState.REVIVE_HP_FRACTION));
        var combat = CombatStateServices.states().getOrCreate(downedPlayer.getUUID(), nowTick);
        combat.setReviveResourceFractions(
                ProjectDownedRuntimeState.REVIVE_MANA_FRACTION,
                ProjectDownedRuntimeState.REVIVE_STAMINA_FRACTION,
                nowTick
        );
        combat.markCombatActivity(nowTick);
        CombatStateServices.markCombatActivity(reviver.getUUID(), nowTick);
        ProjectPlayerActionRuntime.cancelAction(reviver, REVIVE_ACTION_ID);
        recordEncounterReviveParticipation(reviver, downedPlayer, session.encounter());

        return new CompleteReviveResult(
                CompleteReviveStatus.REVIVED,
                downedPlayer.getHealth(),
                combat.stamina(nowTick),
                combat.mana(nowTick),
                session.state().snapshot(nowTick).rescueFatigueTicksRemaining()
        );
    }

    private static boolean hasEligibleEncounterRescuer(
            ServerPlayer downedCandidate,
            ProjectActiveEncounterRuntime.ActiveEncounterRef encounter,
            long nowTick
    ) {
        var server = downedCandidate.level().getServer();
        if (server == null
                || !ProjectActiveEncounterRuntime.isParticipantOf(
                        downedCandidate,
                        encounter
                )) {
            return false;
        }
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (candidate == downedCandidate
                    || candidate.level() != downedCandidate.level()
                    || !eligibleLivingRescuerState(
                            candidate.isAlive(),
                            candidate.isSpectator(),
                            isDowned(candidate.getUUID(), nowTick)
                    )) {
                continue;
            }
            if (ProjectActiveEncounterRuntime.isParticipantOf(candidate, encounter)) {
                return true;
            }
        }
        return false;
    }

    static boolean eligibleLivingRescuerState(
            boolean alive,
            boolean spectator,
            boolean downed
    ) {
        return alive && !spectator && !downed;
    }

    public static boolean interruptRevive(ServerPlayer reviver) {
        Objects.requireNonNull(reviver, "reviver");
        for (Session session : SESSIONS.values()) {
            if (session.state().interruptRevive(reviver.getUUID())) {
                ProjectPlayerActionRuntime.cancelAction(reviver, REVIVE_ACTION_ID);
                return true;
            }
        }
        return false;
    }

    public static Optional<ProjectDownedRuntimeState.Snapshot> snapshot(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        Session session = SESSIONS.get(player.getUUID());
        return session == null ? Optional.empty()
                : Optional.of(session.state().snapshot(player.level().getGameTime()));
    }

    public static void clearAfterDefeat(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        Session session = SESSIONS.remove(playerId);
        if (session != null) session.state().clearAfterDefeat();
    }

    private static void recordEncounterReviveParticipation(
            ServerPlayer reviver,
            ServerPlayer revived,
            ProjectActiveEncounterRuntime.ActiveEncounterRef encounter
    ) {
        if (!"openworld_rpg:r01/earthloong".equals(encounter.kind())) return;
        if (!(revived.level() instanceof ServerLevel level)) return;
        Entity actor = level.getEntity(encounter.actorId());
        if (actor instanceof LivingEntity living) {
            R01EarthloongEncounterService.recordValidatedSupportContribution(living, reviver);
        }
    }

    private record Session(
            ProjectDownedRuntimeState state,
            ProjectActiveEncounterRuntime.ActiveEncounterRef encounter
    ) {
        private Session {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(encounter, "encounter");
        }
    }

    public enum EnterStatus {
        ENTERED, INVALID_PLAYER, NO_ACTIVE_ENCOUNTER, NO_LIVING_RESCUER,
        RESCUE_FATIGUE, ALREADY_DOWNED, OTHER_ENCOUNTER_STATE
    }

    public record EnterResult(
            EnterStatus status,
            Optional<ProjectActiveEncounterRuntime.ActiveEncounterRef> encounter,
            long rescueTicksRemaining
    ) {
        public EnterResult {
            Objects.requireNonNull(status, "status");
            encounter = Objects.requireNonNull(encounter, "encounter");
            if (rescueTicksRemaining < 0L
                    || (status == EnterStatus.ENTERED) != encounter.isPresent()
                    || (status != EnterStatus.ENTERED && rescueTicksRemaining != 0L)) {
                throw new IllegalArgumentException("Invalid Downed enter result.");
            }
        }
        public static EnterResult rejected(EnterStatus status) {
            return new EnterResult(status, Optional.empty(), 0L);
        }
    }

    public enum BeginReviveStatus {
        STARTED, INVALID_REVIVER, NOT_DOWNED, WRONG_ENCOUNTER, REVIVER_BUSY,
        RESCUE_EXPIRED, ALREADY_CHANNELING, OTHER_REVIVER_CHANNELING,
        INSUFFICIENT_RESCUE_TIME
    }

    public record BeginReviveResult(BeginReviveStatus status, long channelTicksRemaining) {
        public BeginReviveResult {
            Objects.requireNonNull(status, "status");
            if (channelTicksRemaining < 0L
                    || (status == BeginReviveStatus.STARTED && channelTicksRemaining <= 0L)
                    || (status != BeginReviveStatus.STARTED && channelTicksRemaining != 0L)) {
                throw new IllegalArgumentException("Invalid revive start result.");
            }
        }
        public static BeginReviveResult rejected(BeginReviveStatus status) {
            return new BeginReviveResult(status, 0L);
        }
    }

    public enum CompleteReviveStatus {
        REVIVED, NOT_DOWNED, INVALID_CONTEXT, RESCUE_EXPIRED, NOT_CHANNEL_OWNER, TOO_EARLY
    }

    public record CompleteReviveResult(
            CompleteReviveStatus status,
            double hpAfter,
            double staminaAfter,
            double manaAfter,
            long rescueFatigueTicks
    ) {
        public CompleteReviveResult {
            Objects.requireNonNull(status, "status");
            if (!Double.isFinite(hpAfter) || hpAfter < 0.0
                    || !Double.isFinite(staminaAfter) || staminaAfter < 0.0
                    || !Double.isFinite(manaAfter) || manaAfter < 0.0
                    || rescueFatigueTicks < 0L
                    || (status != CompleteReviveStatus.REVIVED
                        && (hpAfter != 0.0 || staminaAfter != 0.0
                            || manaAfter != 0.0 || rescueFatigueTicks != 0L))) {
                throw new IllegalArgumentException("Invalid revive completion result.");
            }
        }
        public static CompleteReviveResult rejected(CompleteReviveStatus status) {
            return new CompleteReviveResult(status, 0.0, 0.0, 0.0, 0L);
        }
    }
}
