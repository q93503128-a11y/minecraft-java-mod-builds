package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.ClericGraceRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

public final class ClericRootPassiveRuntime {
    private static final ConcurrentHashMap<UUID, ClericRootPassiveRuntimeState>
            STATES = new ConcurrentHashMap<>();

    private ClericRootPassiveRuntime() {
    }

    public static void synchronize(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        state(player.getUUID()).synchronizeBalancedServiceRank(
                ClericRootPassiveEffects.balancedServiceRank(player)
        );
        CombatStateServices.clericGraceStates()
                .getOrCreate(player.getUUID())
                .synchronizeSupportPipsPerQualifiedEvent(
                        ClericSaintEffects.supportGracePips(player)
                );
    }

    public static double previewManaCostMultiplier(
            UUID playerId,
            String spellId,
            long nowTick
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(spellId, "spellId");
        ClericRootPassiveRuntimeState state = STATES.get(playerId);
        if (state == null) {
            return 1.0;
        }
        return state.previewManaCostMultiplier(
                isDamagingManaSkill(spellId),
                isHealingOrBarrierManaSkill(spellId),
                nowTick
        );
    }

    public static void consumeManaDiscount(
            UUID playerId,
            String spellId,
            long nowTick
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(spellId, "spellId");
        ClericRootPassiveRuntimeState state = STATES.get(playerId);
        if (state == null) {
            return;
        }
        state.consumeManaDiscount(
                isDamagingManaSkill(spellId),
                isHealingOrBarrierManaSkill(spellId),
                nowTick
        );
    }

    public static void recordDamagingEligibleHit(
            ServerPlayer player,
            long nowTick
    ) {
        if (!isActiveCleric(player)) {
            return;
        }
        synchronize(player);
        state(player.getUUID()).recordDamagingEligibleHit(nowTick);
    }

    public static void recordEffectiveHealing(
            ServerPlayer player,
            double effectiveHealing,
            long nowTick
    ) {
        Objects.requireNonNull(player, "player");
        if (!Double.isFinite(effectiveHealing)
                || effectiveHealing < 0.0) {
            throw new IllegalArgumentException(
                    "effectiveHealing must be finite and non-negative."
            );
        }
        if (effectiveHealing <= 0.0 || !isActiveCleric(player)) {
            return;
        }
        synchronize(player);
        state(player.getUUID()).recordEffectiveHealing(nowTick);
    }

    public static void onGraceGain(
            ServerPlayer player,
            ClericGraceRuntimeState.GainResult gain,
            long nowTick
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(gain, "gain");
        if (!gain.pipAdded()
                || gain.currentPips() != ClericGraceRuntimeState.MAX_PIPS
                || !ClericRootPassiveEffects.livingDoctrineEnabled(player)) {
            return;
        }
        ClericRootPassiveRuntimeState state = state(player.getUUID());
        if (!state.tryClaimLivingDoctrine(nowTick)) {
            return;
        }
        CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick)
                .restoreMana(
                        ClericRootPassiveEffects
                                .LIVING_DOCTRINE_MANA_RESTORE,
                        nowTick
                );
    }

    public static void reset(UUID playerId) {
        if (playerId != null) {
            STATES.remove(playerId);
        }
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    static int stateCount() {
        return STATES.size();
    }

    private static ClericRootPassiveRuntimeState state(UUID playerId) {
        return STATES.computeIfAbsent(
                playerId,
                ignored -> new ClericRootPassiveRuntimeState()
        );
    }

    private static boolean isActiveCleric(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.CLERIC::equals)
                .isPresent();
    }

    private static boolean isDamagingManaSkill(String spellId) {
        return ProjectSpellSpec.RADIANT_LANCE_ID.equals(spellId)
                || ProjectSpellSpec.REBUKE_ID.equals(spellId);
    }

    private static boolean isHealingOrBarrierManaSkill(String spellId) {
        return ProjectSpellSpec.MEND_ID.equals(spellId)
                || ProjectSpellSpec.CONSECRATED_GROUND_ID.equals(spellId);
    }
}
