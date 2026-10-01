package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.PlayerPassiveProgressService;
import net.minecraft.server.level.ServerPlayer;

public final class GuardianRootPassiveEffects {
    public static final String BULWARK =
            "openworld_rpg:passive/guardian/root/bulwark";
    public static final String ENDURING_GUARD =
            "openworld_rpg:passive/guardian/root/enduring_guard";
    public static final String SHIELDCRAFT =
            "openworld_rpg:passive/guardian/root/shieldcraft";
    public static final String PROTECTIVE_FORCE =
            "openworld_rpg:passive/guardian/root/protective_force";
    public static final String RESOLVE_KEEPER =
            "openworld_rpg:passive/guardian/root/resolve_keeper";
    public static final String DEFIANT_RETORT =
            "openworld_rpg:passive/guardian/root/defiant_retort";
    public static final String STAND_TOGETHER =
            "openworld_rpg:passive/guardian/root/stand_together";

    public static final double STAND_TOGETHER_ALLY_BARRIER_MAX_HP_FRACTION = 0.05;
    public static final double STAND_TOGETHER_SOLO_BARRIER_MAX_HP_FRACTION = 0.03;
    public static final double STAND_TOGETHER_RANGE = 6.0;
    public static final long STAND_TOGETHER_ICD_TICKS = 200L;

    private GuardianRootPassiveEffects() {
    }

    public static int rank(ServerPlayer player, String nodeId) {
        if (PlayerProgressionService.state(player).activeClass()
                .filter(RootClass.GUARDIAN::equals).isEmpty()) {
            return 0;
        }
        return PlayerPassiveProgressService.state(player)
                .allocationRank(nodeId);
    }

    public static double maxHealthPercentBonus(ServerPlayer player) {
        return maxHealthPercentBonusForRank(rank(player, BULWARK));
    }

    public static double maxHealthPercentBonusForRank(int rank) {
        requireRank(rank, 3, "Bulwark");
        return 0.03 * rank;
    }

    public static int maxStaminaFlatBonus(ServerPlayer player) {
        return maxStaminaFlatBonusForRank(rank(player, ENDURING_GUARD));
    }

    public static int maxStaminaFlatBonusForRank(int rank) {
        requireRank(rank, 3, "Enduring Guard");
        return 4 * rank;
    }

    public static double staminaRecoveryBonus(ServerPlayer player) {
        return staminaRecoveryBonusForRank(rank(player, ENDURING_GUARD));
    }

    public static double staminaRecoveryBonusForRank(int rank) {
        requireRank(rank, 3, "Enduring Guard");
        return 0.01 * rank;
    }

    public static double guardImpactStaminaCostMultiplier(ServerPlayer player) {
        return guardImpactStaminaCostMultiplierForRank(
                rank(player, SHIELDCRAFT)
        );
    }

    public static double guardImpactStaminaCostMultiplierForRank(int rank) {
        requireRank(rank, 3, "Shieldcraft");
        return 1.0 - 0.04 * rank;
    }

    public static double barrierOutputBonus(ServerPlayer player) {
        return barrierOutputBonusForRank(rank(player, PROTECTIVE_FORCE));
    }

    public static double barrierOutputBonusForRank(int rank) {
        requireRank(rank, 3, "Protective Force");
        return 0.03 * rank;
    }

    public static long resolveExpiryBonusTicks(ServerPlayer player) {
        return resolveExpiryBonusTicksForRank(rank(player, RESOLVE_KEEPER));
    }

    public static long resolveExpiryBonusTicksForRank(int rank) {
        requireRank(rank, 2, "Resolve Keeper");
        return 30L * rank;
    }

    public static double perfectGuardPoiseOutputMultiplier(
            ServerPlayer player
    ) {
        return perfectGuardPoiseOutputMultiplierForRank(
                rank(player, DEFIANT_RETORT)
        );
    }

    public static double perfectGuardPoiseOutputMultiplierForRank(int rank) {
        requireRank(rank, 2, "Defiant Retort");
        return 1.0 + 0.08 * rank;
    }

    public static boolean standTogetherEnabled(ServerPlayer player) {
        return rank(player, STAND_TOGETHER) > 0;
    }

    private static void requireRank(int rank, int max, String name) {
        if (rank < 0 || rank > max) {
            throw new IllegalArgumentException(
                    name + " rank must be inside [0, " + max + "]."
            );
        }
    }
}
