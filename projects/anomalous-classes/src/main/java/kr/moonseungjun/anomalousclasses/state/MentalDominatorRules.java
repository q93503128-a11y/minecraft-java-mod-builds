package kr.moonseungjun.anomalousclasses.state;

import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Candidate balance math kept separate from entity and packet handling. */
public final class MentalDominatorRules {
    private static final double[] FOCUS_MAX_PSYCHIC = {0.15, 0.25, 0.40, 0.60, 1.00};
    private static final double[] FOCUS_UPKEEP_REDUCTION = {0.04, 0.10, 0.17, 0.25, 0.40};
    private static final double[] FOCUS_INTRUSION_BONUS = {0.00, 0.04, 0.08, 0.13, 0.28};

    private MentalDominatorRules() {}

    public record Threat(double score, double maxHealth, double attackDamage, double armor) {}

    public static int passiveRanks(PlayerClassProfile profile, String skillId) {
        if (profile == null || skillId == null) return 0;
        ClassProgress progress = profile.progress(ClassId.MENTAL_DOMINATOR);
        Set<String> owned = progress.purchasedNodes();
        GraphSkillTreeCatalog.Skill skill = GraphSkillTreeCatalog.tree(ClassId.MENTAL_DOMINATOR).skills().get(skillId);
        if (skill == null) return 0;
        int ranks = 0;
        for (GraphSkillTreeCatalog.Segment segment : skill.segments()) {
            if (owned.contains(segment.majorNodeId())) {
                ranks = Math.addExact(ranks, segment.oldRankTo() - segment.oldRankFrom() + 1);
            }
        }
        return ranks;
    }

    public static double focusMaxBonus(PlayerClassProfile profile) {
        return focusValue(profile, FOCUS_MAX_PSYCHIC);
    }

    public static double focusUpkeepReduction(PlayerClassProfile profile) {
        return focusValue(profile, FOCUS_UPKEEP_REDUCTION);
    }

    public static double focusIntrusionBonus(PlayerClassProfile profile) {
        return focusValue(profile, FOCUS_INTRUSION_BONUS);
    }

    private static double focusValue(PlayerClassProfile profile, double[] values) {
        if (profile == null) return 0.0;
        ClassFocus focus = profile.equippedFocus().orElse(null);
        if (focus == null || focus.classId() != ClassId.MENTAL_DOMINATOR) return 0.0;
        return values[Math.max(0, Math.min(values.length - 1, focus.tier().index()))];
    }

    public static double maxPsyche(int level, PlayerClassProfile profile) {
        MentalDominatorConfig cfg = MentalDominatorConfig.VALUES;
        double base = MentalDominatorConfig.value(cfg.basePsyche)
                + (Math.max(1, level) - 1.0) * MentalDominatorConfig.value(cfg.psychePerLevel);
        double value = base * (1.0 + focusMaxBonus(profile));
        return finiteClamp(value, 1.0, Double.MAX_VALUE / 4.0);
    }

    public static double regenerationPerSecond(int level) {
        MentalDominatorConfig cfg = MentalDominatorConfig.VALUES;
        return finiteClamp(MentalDominatorConfig.value(cfg.baseRegeneration)
                        + (Math.max(1, level) - 1.0) * MentalDominatorConfig.value(cfg.regenerationPerLevel),
                0.0, Double.MAX_VALUE / 4.0);
    }

    public static int maxControlled(int level, PlayerClassProfile profile) {
        int multipleBondRank = passiveRanks(profile, "multiple_bonds");
        MentalDominatorConfig cfg = MentalDominatorConfig.VALUES;
        double growth = Math.sqrt((Math.max(1, level) - 1.0) / MentalDominatorConfig.value(cfg.levelsPerControlledSlot));
        long slots = (long) MentalDominatorConfig.value(cfg.baseControlledSlots)
                + (long) Math.floor(growth) + multipleBondRank / 5L;
        return (int) Math.min(Integer.MAX_VALUE, slots);
    }

    public static Threat threat(LivingEntity target) {
        MentalDominatorConfig cfg = MentalDominatorConfig.VALUES;
        double health = attribute(target, Attributes.MAX_HEALTH, target.getMaxHealth());
        double attack = attribute(target, Attributes.ATTACK_DAMAGE, 0.0);
        double armor = attribute(target, Attributes.ARMOR, 0.0);
        double score = MentalDominatorConfig.value(cfg.threatBase)
                + MentalDominatorConfig.value(cfg.threatHealthWeight) * Math.sqrt(Math.max(0.0, health) / 20.0)
                + MentalDominatorConfig.value(cfg.threatAttackWeight) * Math.max(0.0, attack)
                + MentalDominatorConfig.value(cfg.threatArmorWeight) * Math.max(0.0, armor);
        return new Threat(finiteClamp(score, 1.0, Double.MAX_VALUE / 4.0),
                finiteClamp(health, 1.0, Double.MAX_VALUE / 4.0),
                finiteClamp(attack, 0.0, Double.MAX_VALUE / 4.0),
                finiteClamp(armor, 0.0, Double.MAX_VALUE / 4.0));
    }

    public static double effectiveThreat(double threat, PlayerClassProfile profile) {
        int piercing = passiveRanks(profile, "resistance_piercing");
        return Math.max(1.0, threat * (1.0 - 0.005 * Math.min(30, piercing)));
    }

    public static double intrusionProgressPerSecond(int level, double targetHealthRatio, double threat,
                                                     PlayerClassProfile profile) {
        MentalDominatorConfig cfg = MentalDominatorConfig.VALUES;
        int infiltration = passiveRanks(profile, "deep_infiltration");
        double control = MentalDominatorConfig.value(cfg.baseControl)
                + (Math.max(1, level) - 1.0) * MentalDominatorConfig.value(cfg.controlPerLevel);
        double rate = MentalDominatorConfig.value(cfg.intrusionProgressPerSecond)
                * control
                * (1.0 + 0.01 * Math.min(30, infiltration))
                * (1.0 + focusIntrusionBonus(profile))
                * (1.0 + 0.75 * (1.0 - finiteClamp(targetHealthRatio, 0.0, 1.0)))
                / (1.0 + Math.pow(Math.max(1.0, effectiveThreat(threat, profile)) / 20.0, 1.35));
        return finiteClamp(rate, 0.01, 1_000_000.0);
    }

    public static double maintenancePerSecond(double threat, int level, PlayerClassProfile profile) {
        MentalDominatorConfig cfg = MentalDominatorConfig.VALUES;
        int bond = passiveRanks(profile, "mental_bond");
        double base = Math.max(0.0, MentalDominatorConfig.value(cfg.maintenanceThreatFactor)
                * Math.pow(Math.max(0.0, threat), MentalDominatorConfig.value(cfg.maintenanceThreatExponent))
                - MentalDominatorConfig.value(cfg.maintenanceLevelDiscount) * (Math.max(1, level) - 1.0));
        return finiteClamp(base * (1.0 - focusUpkeepReduction(profile))
                * (1.0 - 0.0065 * Math.min(40, bond)), 0.0, Double.MAX_VALUE / 4.0);
    }

    public static double groupMaintenancePerSecond(int count, PlayerClassProfile profile) {
        MentalDominatorConfig cfg = MentalDominatorConfig.VALUES;
        int attunement = passiveRanks(profile, "distributed_attunement");
        double extra = Math.max(0, count - 1);
        return finiteClamp(MentalDominatorConfig.value(cfg.groupMaintenanceFactor) * extra * extra
                * (1.0 - 0.01 * Math.min(30, attunement)), 0.0, Double.MAX_VALUE / 4.0);
    }

    public static double interruptionDecayPerSecond(PlayerClassProfile profile) {
        int stability = passiveRanks(profile, "domination_stability");
        return MentalDominatorConfig.value(MentalDominatorConfig.VALUES.interruptionDecayPerSecond)
                * (1.0 - 0.02 * Math.min(20, stability));
    }

    public static boolean shouldTakeFollowMovement(MentalDominatorSavedData.CommandMode command,
                                                    boolean sameDimension, double distanceSquared) {
        if (command == MentalDominatorSavedData.CommandMode.STAY) return true;
        return command == MentalDominatorSavedData.CommandMode.FOLLOW && sameDimension
                && Double.isFinite(distanceSquared) && distanceSquared > 36.0;
    }

    private static double attribute(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> key,
                                    double fallback) {
        AttributeInstance instance = entity.getAttribute(key);
        if (instance == null) return fallback;
        double value = instance.getValue();
        return Double.isFinite(value) ? value : fallback;
    }

    private static double finiteClamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return value > 0.0 ? max : min;
        return Math.max(min, Math.min(max, value));
    }
}
