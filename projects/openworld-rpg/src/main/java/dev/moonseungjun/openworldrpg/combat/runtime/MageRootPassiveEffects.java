package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.PlayerPassiveProgressService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

public final class MageRootPassiveEffects {
    public static final String DEEP_WELL = "openworld_rpg:passive/mage/root/deep_well";
    public static final String ARCANE_EFFICIENCY = "openworld_rpg:passive/mage/root/arcane_efficiency";
    public static final String SPELL_EDGE = "openworld_rpg:passive/mage/root/spell_edge";
    public static final String QUICK_SIGILS = "openworld_rpg:passive/mage/root/quick_sigils";
    public static final String WEAVE_MEMORY = "openworld_rpg:passive/mage/root/weave_memory";
    public static final String TRIUNE_STUDY = "openworld_rpg:passive/mage/root/triune_study";
    public static final String RESONANT_MIND = "openworld_rpg:passive/mage/root/resonant_mind";

    public static final double RESONANT_MIND_MANA_RESTORE = 8.0;
    public static final long RESONANT_MIND_ICD_TICKS = 100L;

    private MageRootPassiveEffects() {
    }

    public static int rank(ServerPlayer player, String nodeId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(nodeId, "nodeId");
        if (PlayerProgressionService.state(player).activeClass()
                .filter(RootClass.MAGE::equals).isEmpty()) {
            return 0;
        }
        return PlayerPassiveProgressService.state(player).allocationRank(nodeId);
    }

    public static int maxManaFlatBonus(ServerPlayer player) {
        return maxManaFlatBonusForRank(rank(player, DEEP_WELL));
    }

    public static int maxManaFlatBonusForRank(int rank) {
        requireRank(rank, 3, "Deep Well");
        return 6 * rank;
    }

    public static double skillManaCostMultiplier(ServerPlayer player) {
        return skillManaCostMultiplierForRank(rank(player, ARCANE_EFFICIENCY));
    }

    public static double skillManaCostMultiplierForRank(int rank) {
        requireRank(rank, 3, "Arcane Efficiency");
        return 1.0 - 0.03 * rank;
    }

    public static double magicPowerBonus(ServerPlayer player) {
        return magicPowerBonusForRank(rank(player, SPELL_EDGE));
    }

    public static double magicPowerBonusForRank(int rank) {
        requireRank(rank, 3, "Spell Edge");
        return 0.02 * rank;
    }

    public static double castSpeedBonus(ServerPlayer player) {
        return castSpeedBonusForRank(rank(player, QUICK_SIGILS));
    }

    public static double castSpeedBonusForRank(int rank) {
        requireRank(rank, 3, "Quick Sigils");
        return 0.02 * rank;
    }

    public static long weaveSequenceExpiryBonusTicks(ServerPlayer player) {
        return weaveSequenceExpiryBonusTicksForRank(rank(player, WEAVE_MEMORY));
    }

    public static long weaveSequenceExpiryBonusTicksForRank(int rank) {
        requireRank(rank, 2, "Weave Memory");
        return 30L * rank;
    }

    public static double triuneStudyMagnitudeMultiplier(ServerPlayer player) {
        return triuneStudyMagnitudeMultiplierForRank(rank(player, TRIUNE_STUDY));
    }

    public static double triuneStudyMagnitudeMultiplierForRank(int rank) {
        requireRank(rank, 2, "Triune Study");
        return 1.0 + 0.05 * rank;
    }

    public static boolean resonantMindEnabled(ServerPlayer player) {
        return rank(player, RESONANT_MIND) > 0;
    }

    public static ProjectImpactTransaction.DamageSourceSnapshot applyMagicPowerBonus(
            ServerPlayer player,
            ProjectImpactTransaction.DamageSourceSnapshot source
    ) {
        Objects.requireNonNull(player, "player");
        return applyMagicPowerBonus(source, magicPowerBonus(player));
    }

    public static ProjectImpactTransaction.DamageSourceSnapshot applyMagicPowerBonus(
            ProjectImpactTransaction.DamageSourceSnapshot source,
            double bonus
    ) {
        Objects.requireNonNull(source, "source");
        if (!Double.isFinite(bonus) || bonus < 0.0 || bonus > 1.0) {
            throw new IllegalArgumentException("Mage Magic Power bonus must be inside [0, 1].");
        }
        if (bonus == 0.0) {
            return source;
        }
        return new ProjectImpactTransaction.DamageSourceSnapshot(
                source.contentLevel(),
                source.weaponPower(),
                source.weightedOffensiveStat(),
                source.additivePowerBonus() + bonus,
                source.poiseOutputMultiplier()
        );
    }

    private static void requireRank(int rank, int max, String name) {
        if (rank < 0 || rank > max) {
            throw new IllegalArgumentException(
                    name + " rank must be inside [0, " + max + "]."
            );
        }
    }
}
