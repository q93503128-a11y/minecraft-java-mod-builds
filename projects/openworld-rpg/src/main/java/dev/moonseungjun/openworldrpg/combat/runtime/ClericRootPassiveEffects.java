package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.PlayerPassiveProgressService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

public final class ClericRootPassiveEffects {
    public static final String WELLSPRING =
            "openworld_rpg:passive/cleric/root/wellspring";
    public static final String MERCY =
            "openworld_rpg:passive/cleric/root/mercy";
    public static final String SACRED_GUARD =
            "openworld_rpg:passive/cleric/root/sacred_guard";
    public static final String RESOLUTE_FAITH =
            "openworld_rpg:passive/cleric/root/resolute_faith";
    public static final String LINGERING_GRACE =
            "openworld_rpg:passive/cleric/root/lingering_grace";
    public static final String BALANCED_SERVICE =
            "openworld_rpg:passive/cleric/root/balanced_service";
    public static final String LIVING_DOCTRINE =
            "openworld_rpg:passive/cleric/root/living_doctrine";

    public static final double LIVING_DOCTRINE_MANA_RESTORE = 8.0;

    private ClericRootPassiveEffects() {
    }

    public static int rank(ServerPlayer player, String nodeId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(nodeId, "nodeId");
        if (PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.CLERIC::equals)
                .isEmpty()) {
            return 0;
        }
        return PlayerPassiveProgressService.state(player)
                .allocationRank(nodeId);
    }

    public static int maxManaFlatBonus(ServerPlayer player) {
        return maxManaFlatBonusForRank(rank(player, WELLSPRING));
    }

    public static int maxManaFlatBonusForRank(int rank) {
        requireRank(rank, 3, "Wellspring");
        return 5 * rank;
    }

    public static double healingOutputMultiplier(ServerPlayer player) {
        return healingOutputMultiplierForRank(rank(player, MERCY));
    }

    public static double healingOutputMultiplierForRank(int rank) {
        requireRank(rank, 3, "Mercy");
        return 1.0 + 0.03 * rank;
    }

    public static double barrierOutputBonus(ServerPlayer player) {
        return barrierOutputBonusForRank(rank(player, SACRED_GUARD));
    }

    public static double barrierOutputBonusForRank(int rank) {
        requireRank(rank, 3, "Sacred Guard");
        return 0.03 * rank;
    }

    public static double equipmentMagicResistanceMultiplier(
            ServerPlayer player
    ) {
        return equipmentMagicResistanceMultiplierForRank(
                rank(player, RESOLUTE_FAITH)
        );
    }

    public static double equipmentMagicResistanceMultiplierForRank(
            int rank
    ) {
        requireRank(rank, 3, "Resolute Faith");
        return 1.0 + 0.04 * rank;
    }

    public static long graceExpiryBonusTicks(ServerPlayer player) {
        return graceExpiryBonusTicksForRank(
                rank(player, LINGERING_GRACE)
        );
    }

    public static long graceExpiryBonusTicksForRank(int rank) {
        requireRank(rank, 2, "Lingering Grace");
        return 30L * rank;
    }

    public static int balancedServiceRank(ServerPlayer player) {
        return rank(player, BALANCED_SERVICE);
    }

    public static double balancedServiceManaCostMultiplierForRank(
            int rank
    ) {
        requireRank(rank, 2, "Balanced Service");
        return 1.0 - 0.05 * rank;
    }

    public static boolean livingDoctrineEnabled(ServerPlayer player) {
        return rank(player, LIVING_DOCTRINE) > 0;
    }

    private static void requireRank(int rank, int max, String name) {
        if (rank < 0 || rank > max) {
            throw new IllegalArgumentException(
                    name + " rank must be inside [0, " + max + "]."
            );
        }
    }
}
