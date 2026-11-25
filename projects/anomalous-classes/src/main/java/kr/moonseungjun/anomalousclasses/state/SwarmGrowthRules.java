package kr.moonseungjun.anomalousclasses.state;

import java.util.Set;
import kr.moonseungjun.anomalousclasses.state.SwarmSavedData.SwarmNodeRole;

/**
 * One source for nine purchased swarm graph skills and the equipped focus tier.
 * Minor graph nodes still grant their existing generic player stats; only
 * purchased major milestones advance the former class-specific skill ranks.
 */
public record SwarmGrowthRules(int sharpJaws, int assaultTactics, int sacrificeInstinct,
        int hardShell, int ballisticSense, int escortFormation, int naturalFission,
        int regrouping, int packTracking, double focusBonus) {
    public static SwarmGrowthRules from(PlayerClassProfile profile) {
        ClassProgress progress = profile.progress(ClassId.SWARM_CASTER);
        Set<String> nodes = progress.purchasedNodes();
        ClassFocus focus = profile.equippedFocus().orElse(null);
        double bonus = focus == null || focus.classId() != ClassId.SWARM_CASTER
                ? 0.0 : switch (focus.tier()) {
                    case STANDARD -> .05;
                    case ALTERED -> .10;
                    case ANOMALOUS -> .15;
                    case ELDRITCH -> .20;
                    case INCOMPREHENSIBLE -> .40;
                };
        return new SwarmGrowthRules(
                rank(nodes, "sharp_jaws"), rank(nodes, "assault_tactics"),
                rank(nodes, "sacrifice_instinct"), rank(nodes, "hard_shell"),
                rank(nodes, "ballistic_sense"), rank(nodes, "escort_formation"),
                rank(nodes, "natural_fission"), rank(nodes, "regrouping"),
                rank(nodes, "pack_tracking"), bonus);
    }

    private static int rank(Set<String> nodes, String skill) {
        GraphSkillTreeCatalog.Skill definition = GraphSkillTreeCatalog
                .tree(ClassId.SWARM_CASTER).skills().get(skill);
        if (definition == null) throw new IllegalStateException("Unknown swarm growth node: " + skill);
        int obtained = 0;
        for (GraphSkillTreeCatalog.Segment segment : definition.segments())
            if (nodes.contains(segment.majorNodeId()))
                obtained = Math.max(obtained, segment.oldRankTo());
        return Math.min(30, obtained);
    }

    /** Each relevant source applies once; no extra bonus on the player's weapon. */
    public double normalAttackFactor() {
        return 1.0 + focusBonus + .006 * sharpJaws;
    }
    public double rushAttackFactor() {
        return normalAttackFactor() + .006 * assaultTactics;
    }
    public double sacrificeAttackFactor() {
        return normalAttackFactor() + .006 * sacrificeInstinct;
    }
    public double durabilityFactor(SwarmNodeRole role) {
        return 1.0 + focusBonus + .006 * hardShell
                + (role == SwarmNodeRole.GUARD ? .005 * escortFormation : 0.0);
    }
    public int regenerationTicks() {
        return Math.max(20, (int) Math.ceil(40.0 / (1.0 + .012 * naturalFission)));
    }
    public int transferBudget() {
        return 2 + regrouping / 10 + (escortFormation >= 10 ? 1 : 0);
    }
    public double chaseSpeedFactor() {
        return 1.0 + .008 * packTracking;
    }
    public double guardMoveFactor() {
        return 1.0 + .008 * ballisticSense;
    }
    public double regroupMoveFactor() {
        return 1.0 + .007 * regrouping;
    }
    public float flightSpeed(float baseline) {
        return (float) (baseline * (1.0 + .003 * packTracking));
    }
}
