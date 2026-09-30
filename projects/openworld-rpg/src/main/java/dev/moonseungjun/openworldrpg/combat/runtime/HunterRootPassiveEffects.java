package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.PlayerPassiveProgressService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

public final class HunterRootPassiveEffects {
    public static final String KEEN_EYE =
            "openworld_rpg:passive/hunter/root/keen_eye";
    public static final String LIGHT_STEP =
            "openworld_rpg:passive/hunter/root/light_step";
    public static final String EFFICIENT_DRAW =
            "openworld_rpg:passive/hunter/root/efficient_draw";
    public static final String QUARRY_PRESSURE =
            "openworld_rpg:passive/hunter/root/quarry_pressure";
    public static final String FOCUS_RETENTION =
            "openworld_rpg:passive/hunter/root/focus_retention";
    public static final String WEAKPOINT_STUDY =
            "openworld_rpg:passive/hunter/root/weakpoint_study";
    public static final String TRAIL_SENSE =
            "openworld_rpg:passive/hunter/root/trail_sense";
    public static final double DEFAULT_AUTHORED_WEAK_POINT_MULTIPLIER = 1.25;

    private HunterRootPassiveEffects() {
    }

    public static int rank(ServerPlayer player, String nodeId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(nodeId, "nodeId");
        if (!PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.HUNTER::equals)
                .isPresent()) {
            return 0;
        }
        return PlayerPassiveProgressService.state(player)
                .allocationRank(nodeId);
    }

    public static double criticalChanceBonus(ServerPlayer player) {
        return criticalChanceBonusForRank(rank(player, KEEN_EYE));
    }

    public static double criticalChanceBonusForRank(int rank) {
        requireRank(rank, 3, "Keen Eye");
        return 0.02 * rank;
    }

    public static double movementSpeedBonus(
            ServerPlayer player,
            ProjectWeaponFamily family
    ) {
        if (family == null || !isRangedCapable(family)) {
            return 0.0;
        }
        return movementSpeedBonusForRank(rank(player, LIGHT_STEP));
    }

    public static double movementSpeedBonusForRank(int rank) {
        requireRank(rank, 3, "Light Step");
        return 0.02 * rank;
    }

    public static boolean isRangedCapable(ProjectWeaponFamily family) {
        Objects.requireNonNull(family, "family");
        return switch (family) {
            case BOW, CROSSBOW, BLACK_POWDER_PISTOL,
                    MUSKET_HAND_CANNON -> true;
            default -> false;
        };
    }

    public static double skillManaCostMultiplier(ServerPlayer player) {
        return skillManaCostMultiplierForRank(
                rank(player, EFFICIENT_DRAW)
        );
    }

    public static double skillManaCostMultiplierForRank(int rank) {
        requireRank(rank, 3, "Efficient Draw");
        return 1.0 - 0.03 * rank;
    }

    public static double quarryDirectDamageMultiplier(
            ServerPlayer player,
            boolean currentQuarry
    ) {
        return currentQuarry
                ? quarryDirectDamageMultiplierForRank(
                        rank(player, QUARRY_PRESSURE)
                )
                : 1.0;
    }

    public static double quarryDirectDamageMultiplierForRank(int rank) {
        requireRank(rank, 3, "Quarry Pressure");
        return 1.0 + 0.02 * rank;
    }

    public static long focusExpiryBonusTicks(ServerPlayer player) {
        return focusExpiryBonusTicksForRank(
                rank(player, FOCUS_RETENTION)
        );
    }

    public static long focusExpiryBonusTicksForRank(int rank) {
        requireRank(rank, 2, "Focus Retention");
        return 30L * rank;
    }

    public static double weakPointMultiplier(
            ServerPlayer player,
            boolean authoredWeakPointHit
    ) {
        if (!authoredWeakPointHit) {
            return 1.0;
        }
        return DEFAULT_AUTHORED_WEAK_POINT_MULTIPLIER
                + weakPointBonus(player);
    }

    public static double augmentWeakPointMultiplier(
            ServerPlayer player,
            double authoredMultiplier,
            boolean authoredWeakPointHit
    ) {
        if (!Double.isFinite(authoredMultiplier)
                || authoredMultiplier < 1.0) {
            throw new IllegalArgumentException(
                    "Authored weak-point multiplier must be finite and >= 1."
            );
        }
        return authoredWeakPointHit
                ? authoredMultiplier + weakPointBonus(player)
                : authoredMultiplier;
    }

    public static double weakPointBonus(ServerPlayer player) {
        return weakPointBonusForRank(
                rank(player, WEAKPOINT_STUDY)
        );
    }

    public static double weakPointBonusForRank(int rank) {
        requireRank(rank, 2, "Weakpoint Study");
        return 0.04 * rank;
    }

    public static boolean trailSenseEnabled(ServerPlayer player) {
        return rank(player, TRAIL_SENSE) > 0;
    }

    private static void requireRank(int rank, int max, String name) {
        if (rank < 0 || rank > max) {
            throw new IllegalArgumentException(
                    name + " rank must be inside [0, " + max + "]."
            );
        }
    }
}
