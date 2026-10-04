package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.ClassSpecialization;
import dev.moonseungjun.openworldrpg.progression.PlayerClassAdvancementService;
import dev.moonseungjun.openworldrpg.progression.PlayerPassiveProgressService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-owned modifiers for the Cleric Saint specialization.
 *
 * <p>Only effects with an existing real Cleric runtime consumer live here. Branch actives and
 * Miracle remain separate until their own skill/presentation bindings are admitted.</p>
 */
public final class ClericSaintEffects {
    public static final String GENTLE_HANDS =
            "openworld_rpg:passive/cleric/saint/gentle_hands";
    public static final String OVERFLOWING_LIGHT =
            "openworld_rpg:passive/cleric/saint/overflowing_light";
    public static final String AEGIS =
            "openworld_rpg:passive/cleric/saint/aegis";

    public static final int BENEDICTION_SUPPORT_PIPS = 2;
    public static final double BASE_OVERHEAL_TO_BARRIER_FRACTION = 0.20;
    public static final double OVERFLOWING_LIGHT_PER_RANK = 0.03;
    public static final double OVERHEAL_BARRIER_MAX_HP_CAP = 0.12;

    private ClericSaintEffects() {
    }

    public static boolean active(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.CLERIC::equals)
                .isEmpty()) {
            return false;
        }
        return PlayerClassAdvancementService.state(player)
                .rootState(RootClass.CLERIC)
                .active()
                .filter(ClassSpecialization.CLERIC_SAINT::equals)
                .isPresent();
    }

    public static int supportGracePips(ServerPlayer player) {
        return active(player) ? BENEDICTION_SUPPORT_PIPS : 1;
    }

    public static double healingOutputMultiplier(ServerPlayer player) {
        return active(player)
                ? healingOutputMultiplierForRank(rank(player, GENTLE_HANDS, 3))
                : 1.0;
    }

    public static double healingOutputMultiplierForRank(int rank) {
        requireRank(rank, 3, "Gentle Hands");
        return 1.0 + 0.03 * rank;
    }

    public static double barrierOutputBonus(ServerPlayer player) {
        return active(player)
                ? barrierOutputBonusForRank(rank(player, AEGIS, 3))
                : 0.0;
    }

    public static double barrierOutputBonusForRank(int rank) {
        requireRank(rank, 3, "Aegis");
        return 0.03 * rank;
    }

    public static double overhealBarrierAmount(
            ServerPlayer caster,
            double otherwiseWastedOverheal,
            double targetMaxHp
    ) {
        Objects.requireNonNull(caster, "caster");
        if (!active(caster)) {
            return 0.0;
        }
        return overhealBarrierAmountForRanks(
                otherwiseWastedOverheal,
                targetMaxHp,
                rank(caster, OVERFLOWING_LIGHT, 3),
                rank(caster, AEGIS, 3)
        );
    }

    public static double overhealBarrierAmountForRanks(
            double otherwiseWastedOverheal,
            double targetMaxHp,
            int overflowingLightRank,
            int aegisRank
    ) {
        if (!Double.isFinite(otherwiseWastedOverheal)
                || otherwiseWastedOverheal < 0.0
                || !Double.isFinite(targetMaxHp)
                || targetMaxHp <= 0.0) {
            throw new IllegalArgumentException(
                    "Saint overheal conversion inputs must be finite and non-negative."
            );
        }
        requireRank(overflowingLightRank, 3, "Overflowing Light");
        requireRank(aegisRank, 3, "Aegis");

        double conversion = BASE_OVERHEAL_TO_BARRIER_FRACTION
                + OVERFLOWING_LIGHT_PER_RANK * overflowingLightRank;
        double withAegis = otherwiseWastedOverheal
                * conversion
                * (1.0 + barrierOutputBonusForRank(aegisRank));
        return Math.min(
                targetMaxHp * OVERHEAL_BARRIER_MAX_HP_CAP,
                withAegis
        );
    }

    private static int rank(
            ServerPlayer player,
            String nodeId,
            int maxRank
    ) {
        int rank = PlayerPassiveProgressService.state(player)
                .allocationRank(nodeId);
        requireRank(rank, maxRank, nodeId);
        return rank;
    }

    private static void requireRank(int rank, int max, String name) {
        if (rank < 0 || rank > max) {
            throw new IllegalArgumentException(
                    name + " rank must be inside [0, " + max + "]."
            );
        }
    }
}
