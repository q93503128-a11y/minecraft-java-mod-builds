package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-authoritative Hunter root mechanic bridge.
 *
 * <p>The current production hook binds ranged basics to Quarry/Focus. Skill-spender and
 * anatomical weak-point callers can reuse this state only after their own canonical runtime and
 * presentation bindings exist.</p>
 */
public final class HunterSkillRuntime {
    private static final ConcurrentHashMap<UUID, HunterQuarryFocusRuntimeState>
            STATES = new ConcurrentHashMap<>();

    private HunterSkillRuntime() {
    }

    public static RangedHitResult onRangedBasicHit(
            ServerPlayer hunter,
            LivingEntity target,
            double shotDistanceBlocks,
            boolean weakPointHit,
            boolean poiseBreakTriggered,
            long nowTick
    ) {
        Objects.requireNonNull(hunter, "hunter");
        Objects.requireNonNull(target, "target");
        if (!isHunter(hunter)
                || hunter.level().isClientSide()
                || hunter.level() != target.level()
                || ExternalActorBindingRuntime.combatProfile(target).isEmpty()) {
            return RangedHitResult.rejected();
        }

        var combatState = CombatStateServices.states()
                .getOrCreate(hunter.getUUID(), nowTick);
        HunterQuarryFocusRuntimeState.HitApplication stateResult =
                state(hunter).recordRangedHit(
                        target.getUUID(),
                        shotDistanceBlocks,
                        weakPointHit,
                        nowTick,
                        combatState.lastCombatActivityTick()
                );

        ProjectUltimateChargeRuntime.recordHunterRangedQuarryHit(
                hunter,
                target
        );
        if (stateResult.longRangeEligible()) {
            ProjectUltimateChargeRuntime.recordHunterLongRangeBonus(
                    hunter,
                    target
            );
        }
        if (stateResult.weakPointUltimatePublicationClaimed()) {
            ProjectUltimateChargeRuntime.recordHunterWeakPointHit(
                    hunter,
                    target
            );
        }
        if (poiseBreakTriggered) {
            ProjectUltimateChargeRuntime.recordHunterRangedPoiseBreak(
                    hunter,
                    target
            );
        }

        return new RangedHitResult(
                true,
                stateResult.newlyMarked(),
                stateResult.replacedPreviousQuarry(),
                stateResult.focusBefore(),
                stateResult.focusAfter(),
                stateResult.longRangeFocusGranted(),
                stateResult.weakPointFocusGranted(),
                poiseBreakTriggered
        );
    }

    public static HunterQuarryFocusRuntimeState.FocusLoss
    onDirectHpDamage(
            ServerPlayer hunter,
            long nowTick,
            long combatActivityBeforeHit
    ) {
        Objects.requireNonNull(hunter, "hunter");
        if (!isHunter(hunter) || hunter.level().isClientSide()) {
            return new HunterQuarryFocusRuntimeState.FocusLoss(0, 0);
        }
        return state(hunter).onDirectHpDamage(
                nowTick,
                combatActivityBeforeHit
        );
    }

    public static boolean consumeFocusSpenderIfFull(
            ServerPlayer hunter
    ) {
        Objects.requireNonNull(hunter, "hunter");
        if (!isHunter(hunter) || hunter.level().isClientSide()) {
            return false;
        }
        long nowTick = hunter.level().getGameTime();
        var combatState = CombatStateServices.states()
                .getOrCreate(hunter.getUUID(), nowTick);
        return state(hunter).consumeFocusSpenderIfFull(
                nowTick,
                combatState.lastCombatActivityTick()
        );
    }

    public static int focusPips(ServerPlayer hunter) {
        Objects.requireNonNull(hunter, "hunter");
        if (!isHunter(hunter) || hunter.level().isClientSide()) {
            return 0;
        }
        long nowTick = hunter.level().getGameTime();
        var combatState = CombatStateServices.states()
                .getOrCreate(hunter.getUUID(), nowTick);
        return state(hunter).snapshot(
                nowTick,
                combatState.lastCombatActivityTick()
        ).focus();
    }

    public static Optional<UUID> quarryId(ServerPlayer hunter) {
        Objects.requireNonNull(hunter, "hunter");
        if (!isHunter(hunter) || hunter.level().isClientSide()) {
            return Optional.empty();
        }
        long nowTick = hunter.level().getGameTime();
        var combatState = CombatStateServices.states()
                .getOrCreate(hunter.getUUID(), nowTick);
        return state(hunter).snapshot(
                nowTick,
                combatState.lastCombatActivityTick()
        ).quarryId();
    }

    public static boolean isCurrentQuarry(
            ServerPlayer hunter,
            LivingEntity target
    ) {
        Objects.requireNonNull(hunter, "hunter");
        Objects.requireNonNull(target, "target");
        if (!isHunter(hunter)
                || hunter.level().isClientSide()
                || hunter.level() != target.level()) {
            return false;
        }
        long nowTick = hunter.level().getGameTime();
        var combatState = CombatStateServices.states()
                .getOrCreate(hunter.getUUID(), nowTick);
        return state(hunter).isCurrentQuarry(
                target.getUUID(),
                nowTick,
                combatState.lastCombatActivityTick()
        );
    }

    public static void reset(UUID playerId) {
        if (playerId == null) {
            return;
        }
        HunterQuarryFocusRuntimeState state = STATES.remove(playerId);
        if (state != null) {
            state.reset();
        }
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    private static HunterQuarryFocusRuntimeState state(
            ServerPlayer hunter
    ) {
        return STATES.computeIfAbsent(
                hunter.getUUID(),
                ignored -> new HunterQuarryFocusRuntimeState()
        );
    }

    private static boolean isHunter(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.HUNTER::equals)
                .isPresent();
    }

    public record RangedHitResult(
            boolean accepted,
            boolean newlyMarked,
            boolean replacedPreviousQuarry,
            int focusBefore,
            int focusAfter,
            boolean longRangeFocusGranted,
            boolean weakPointFocusGranted,
            boolean poiseBreakTriggered
    ) {
        public RangedHitResult {
            if (!accepted
                    && (newlyMarked
                            || replacedPreviousQuarry
                            || focusBefore != 0
                            || focusAfter != 0
                            || longRangeFocusGranted
                            || weakPointFocusGranted
                            || poiseBreakTriggered)) {
                throw new IllegalArgumentException(
                        "Rejected Hunter ranged hits cannot mutate state."
                );
            }
        }

        public static RangedHitResult rejected() {
            return new RangedHitResult(
                    false,
                    false,
                    false,
                    0,
                    0,
                    false,
                    false,
                    false
            );
        }
    }
}
