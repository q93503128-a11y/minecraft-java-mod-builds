package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.PlayerPassiveProgressService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

public final class WarriorRootPassiveEffects {
    public static final String STEEL_NERVE =
            "openworld_rpg:passive/warrior/root/steel_nerve";
    public static final String TIRELESS_COMBATANT =
            "openworld_rpg:passive/warrior/root/tireless_combatant";
    public static final String WEAPON_RHYTHM =
            "openworld_rpg:passive/warrior/root/weapon_rhythm";
    public static final String CRUSHING_INTENT =
            "openworld_rpg:passive/warrior/root/crushing_intent";
    public static final String HELD_MOMENTUM =
            "openworld_rpg:passive/warrior/root/held_momentum";
    public static final String COUNTERFORCE =
            "openworld_rpg:passive/warrior/root/counterforce";
    public static final String BATTLE_TEMPER =
            "openworld_rpg:passive/warrior/root/battle_temper";

    public static final long BATTLE_TEMPER_ICD_TICKS = 80L;
    public static final double BATTLE_TEMPER_STAMINA_RESTORE = 10.0;
    public static final double BATTLE_TEMPER_MANA_RESTORE = 8.0;

    private WarriorRootPassiveEffects() {
    }

    public static int rank(ServerPlayer player, String nodeId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(nodeId, "nodeId");
        if (PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.WARRIOR::equals)
                .isEmpty()) {
            return 0;
        }
        return PlayerPassiveProgressService.state(player)
                .allocationRank(nodeId);
    }

    public static double maxHealthPercentBonus(ServerPlayer player) {
        return maxHealthPercentBonusForRank(rank(player, STEEL_NERVE));
    }

    public static double maxHealthPercentBonusForRank(int rank) {
        requireRank(rank, 3, "Steel Nerve");
        return 0.03 * rank;
    }

    public static int maxStaminaFlatBonus(ServerPlayer player) {
        return maxStaminaFlatBonusForRank(rank(player, TIRELESS_COMBATANT));
    }

    public static int maxStaminaFlatBonusForRank(int rank) {
        requireRank(rank, 3, "Tireless Combatant");
        return 4 * rank;
    }

    public static double staminaRecoveryBonus(ServerPlayer player) {
        return staminaRecoveryBonusForRank(rank(player, TIRELESS_COMBATANT));
    }

    public static double staminaRecoveryBonusForRank(int rank) {
        requireRank(rank, 3, "Tireless Combatant");
        return 0.01 * rank;
    }

    public static double weaponRhythmAttackSpeedBonus(
            ServerPlayer player,
            int momentumPips
    ) {
        return momentumPips > 0
                ? weaponRhythmAttackSpeedBonusForRank(
                        rank(player, WEAPON_RHYTHM)
                )
                : 0.0;
    }

    public static double weaponRhythmAttackSpeedBonusForRank(int rank) {
        requireRank(rank, 3, "Weapon Rhythm");
        return 0.02 * rank;
    }

    public static double poiseOutputMultiplier(ServerPlayer player) {
        return poiseOutputMultiplierForRank(rank(player, CRUSHING_INTENT));
    }

    public static double poiseOutputMultiplierForRank(int rank) {
        requireRank(rank, 3, "Crushing Intent");
        return 1.0 + 0.04 * rank;
    }

    public static long momentumExpiryBonusTicks(ServerPlayer player) {
        return momentumExpiryBonusTicksForRank(rank(player, HELD_MOMENTUM));
    }

    public static long momentumExpiryBonusTicksForRank(int rank) {
        requireRank(rank, 2, "Held Momentum");
        return 20L * rank;
    }

    public static double counterforceOutputMultiplier(ServerPlayer player) {
        return counterforceOutputMultiplierForRank(rank(player, COUNTERFORCE));
    }

    public static double counterforceOutputMultiplierForRank(int rank) {
        requireRank(rank, 2, "Counterforce");
        return 1.0 + 0.08 * rank;
    }

    public static boolean battleTemperEnabled(ServerPlayer player) {
        return rank(player, BATTLE_TEMPER) > 0;
    }

    private static void requireRank(int rank, int max, String name) {
        if (rank < 0 || rank > max) {
            throw new IllegalArgumentException(
                    name + " rank must be inside [0, " + max + "]."
            );
        }
    }
}
