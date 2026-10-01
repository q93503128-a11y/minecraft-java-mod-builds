package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildPublisher;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class MageArcaneWeaveRuntime {
    public static final double ARCANE_MEMORY_MOVEMENT_SPEED_BONUS = 0.05;
    public static final long ARCANE_MEMORY_DAMAGE_SHORTEN_TICKS = 40L;

    private static final Map<UUID, MageArcaneWeaveRuntimeState> STATES =
            new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ARCANE_MEMORY_ACTIVE =
            new ConcurrentHashMap<>();

    private MageArcaneWeaveRuntime() {
    }

    public static CastApplication onAcceptedCast(
            ServerPlayer player,
            String spellId,
            long nowTick
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spellId, "spellId");
        if (player.level().isClientSide()
                || PlayerProgressionService.state(player).activeClass()
                        .filter(RootClass.MAGE::equals).isEmpty()
                || ProjectSpellSpec.requiredRootClass(spellId)
                        .filter(RootClass.MAGE::equals).isEmpty()) {
            return CastApplication.rejected();
        }

        MageArcaneWeaveRuntimeState state = STATES.computeIfAbsent(
                player.getUUID(),
                ignored -> new MageArcaneWeaveRuntimeState()
        );
        var result = state.recordAcceptedActive(
                spellId,
                isWeaveConsumer(spellId),
                nowTick,
                MageRootPassiveEffects.weaveSequenceExpiryBonusTicks(player),
                MageRootPassiveEffects.triuneStudyMagnitudeMultiplier(player)
        );

        if (result.weaveCompleted()) {
            ProjectUltimateChargeRuntime.recordMageWeaveCompletion(player);
        }

        double restoredMana = 0.0;
        if (result.weaveCompleted()
                && MageRootPassiveEffects.resonantMindEnabled(player)
                && state.tryClaimResonantMind(nowTick)) {
            var combat = CombatStateServices.states()
                    .getOrCreate(player.getUUID(), nowTick);
            double before = combat.mana(nowTick);
            combat.restoreMana(MageRootPassiveEffects.RESONANT_MIND_MANA_RESTORE, nowTick);
            restoredMana = combat.mana(nowTick) - before;
        }

        synchronizeArcaneMemoryMovement(player, nowTick);
        return new CastApplication(
                true,
                result.distinctSigilAdded(),
                result.weaveCompleted(),
                result.weaveConsumed(),
                result.sigilCountAfter(),
                result.weaveReadyAfter(),
                result.weaveEffectMagnitudeMultiplier(),
                restoredMana
        );
    }

    public static double arcaneMemoryMovementSpeedBonus(
            ServerPlayer player,
            long nowTick
    ) {
        Objects.requireNonNull(player, "player");
        if (!isMage(player)) {
            return 0.0;
        }
        MageArcaneWeaveRuntimeState state = STATES.get(
                player.getUUID()
        );
        return state != null && state.atTwoSigils(nowTick)
                ? ARCANE_MEMORY_MOVEMENT_SPEED_BONUS
                : 0.0;
    }

    public static MageArcaneWeaveRuntimeState.DirectHpDamageResult
            onDirectHpDamage(
                    ServerPlayer player,
                    long nowTick
            ) {
        Objects.requireNonNull(player, "player");
        MageArcaneWeaveRuntimeState state = STATES.get(
                player.getUUID()
        );
        if (!isMage(player) || state == null) {
            return new MageArcaneWeaveRuntimeState.DirectHpDamageResult(
                    false,
                    0L,
                    0L,
                    0
            );
        }
        var result = state.recordDirectHpDamage(
                nowTick,
                ARCANE_MEMORY_DAMAGE_SHORTEN_TICKS
        );
        synchronizeArcaneMemoryMovement(player, nowTick);
        return result;
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        for (var entry : STATES.entrySet()) {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(entry.getKey());
            if (player == null || !isMage(player)) {
                continue;
            }
            synchronizeArcaneMemoryMovement(
                    player,
                    player.level().getGameTime()
            );
        }
    }

    public static OptionalDouble consumeEmpoweredCast(
            UUID playerId,
            String spellId,
            long nowTick
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(spellId, "spellId");
        MageArcaneWeaveRuntimeState state = STATES.get(playerId);
        return state == null
                ? OptionalDouble.empty()
                : state.consumeEmpoweredCast(spellId, nowTick);
    }

    public static OptionalDouble consumeWeaveReadyForUltimate(
            ServerPlayer player,
            long nowTick
    ) {
        Objects.requireNonNull(player, "player");
        MageArcaneWeaveRuntimeState state = STATES.get(player.getUUID());
        if (!isMage(player) || state == null) {
            return OptionalDouble.empty();
        }
        return state.consumeWeaveReadyForUltimate(
                nowTick,
                MageRootPassiveEffects.triuneStudyMagnitudeMultiplier(player)
        );
    }

    public static void reset(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        STATES.remove(playerId);
        ARCANE_MEMORY_ACTIVE.remove(playerId);
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    private static void synchronizeArcaneMemoryMovement(
            ServerPlayer player,
            long nowTick
    ) {
        MageArcaneWeaveRuntimeState state = STATES.get(
                player.getUUID()
        );
        boolean active = state != null
                && state.atTwoSigils(nowTick);
        Boolean previous = ARCANE_MEMORY_ACTIVE.put(
                player.getUUID(),
                active
        );
        if ((previous == null && active)
                || (previous != null
                && previous.booleanValue() != active)) {
            PlayerCombatBuildPublisher.refresh(player);
        }
    }

    private static boolean isMage(ServerPlayer player) {
        return player != null
                && !player.level().isClientSide()
                && PlayerProgressionService.state(player)
                        .activeClass()
                        .filter(RootClass.MAGE::equals)
                        .isPresent();
    }

    static boolean isWeaveConsumer(String spellId) {
        return ProjectSpellSpec.ARC_BOLT_ID.equals(spellId)
                || ProjectSpellSpec.PHASE_STEP_ID.equals(spellId)
                || ProjectSpellSpec.FROST_RING_ID.equals(spellId)
                || ProjectSpellSpec.FLAME_BURST_ID.equals(spellId);
    }

    public record CastApplication(
            boolean accepted,
            boolean distinctSigilAdded,
            boolean weaveCompleted,
            boolean weaveConsumed,
            int sigilCountAfter,
            boolean weaveReadyAfter,
            double weaveEffectMagnitudeMultiplier,
            double restoredMana
    ) {
        public CastApplication {
            if (sigilCountAfter < 0 || sigilCountAfter > 2
                    || !Double.isFinite(weaveEffectMagnitudeMultiplier)
                    || weaveEffectMagnitudeMultiplier < 0.0
                    || !Double.isFinite(restoredMana)
                    || restoredMana < 0.0) {
                throw new IllegalArgumentException("Invalid Mage cast application.");
            }
            if (!accepted
                    && (distinctSigilAdded || weaveCompleted || weaveConsumed
                    || sigilCountAfter != 0 || weaveReadyAfter
                    || weaveEffectMagnitudeMultiplier != 0.0
                    || restoredMana != 0.0)) {
                throw new IllegalArgumentException(
                        "Rejected Mage cast cannot carry applied state."
                );
            }
        }

        public static CastApplication rejected() {
            return new CastApplication(false, false, false, false, 0, false, 0.0, 0.0);
        }
    }
}
