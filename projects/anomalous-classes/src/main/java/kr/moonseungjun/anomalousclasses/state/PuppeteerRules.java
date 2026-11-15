package kr.moonseungjun.anomalousclasses.state;

import java.util.Set;

/**
 * Authoritative puppet balancing math from the puppeteer workshop, combat
 * matrix, scale and control-bar design documents. Numbers remain tunable
 * candidates; high-grade focus bonuses (+40%, +5 slots) are approved.
 */
public final class PuppeteerRules {
    private PuppeteerRules() {}

    public record Stats(double maxHealth, double damage, int attackIntervalTicks,
                        double moveSpeedFactor, double armor, double reach,
                        double height, double incomingDamageFactor) {}

    private static final double[] FRAME_HP = {24, 32, 46};
    private static final double[] FRAME_ARMOR = {0, 2, 6};
    private static final double[] FRAME_MOVE = {1.20, 1.00, 0.78};
    private static final double[] FRAME_DAMAGE = {0.95, 1.00, 1.08};
    private static final double[] FRAME_INTERVAL = {0.88, 1.00, 1.14};
    private static final double[] DEVICE_DAMAGE = {5.0, 4.0, 2.5};
    private static final double[] DEVICE_INTERVAL = {25, 33, 38};
    private static final double[] DEVICE_REACH = {2.3, 10.0, 2.5};
    private static final double[] SIZE_HP = {0.80, 1.0, 1.20, 1.45, 1.70};
    private static final double[] SIZE_DAMAGE = {0.90, 1.0, 1.08, 1.18, 1.28};
    private static final double[] SIZE_INTERVAL = {0.92, 1.0, 1.05, 1.12, 1.22};
    private static final double[] SIZE_MOVE = {1.10, 1.0, 0.94, 0.88, 0.80};
    private static final double[] SIZE_REACH = {0.0, 0.0, 0.25, 0.50, 0.75};
    private static final double[] FOCUS_STAT = {1.05, 1.10, 1.15, 1.20, 1.40};

    public static int rank(PlayerClassProfile profile, String legacyId) {
        if (profile == null || legacyId == null) return 0;
        Set<String> purchased = profile.progress(ClassId.PUPPETEER).purchasedNodes();
        GraphSkillTreeCatalog.Skill skill = GraphSkillTreeCatalog.tree(ClassId.PUPPETEER)
                .skills().get(legacyId);
        if (skill == null) return 0;
        int ranks = 0;
        for (GraphSkillTreeCatalog.Segment segment : skill.segments()) {
            if (purchased.contains(segment.majorNodeId()))
                ranks += segment.oldRankTo() - segment.oldRankFrom() + 1;
        }
        return ranks;
    }

    public static int gearSlots(PlayerClassProfile profile) {
        ClassFocus focus = profile == null ? null : profile.equippedFocus().orElse(null);
        return focus != null && focus.classId() == ClassId.PUPPETEER
                ? focus.tier().index() + 1 : 0;
    }

    public static double gearStats(PlayerClassProfile profile) {
        int grade = gearSlots(profile);
        return grade == 0 ? 1.0 : FOCUS_STAT[grade - 1];
    }

    public static int maxActive(int level, PlayerClassProfile profile) {
        long base = 1L + (long) Math.floor(Math.sqrt((Math.max(1, level) - 1.0) / 24.0));
        long slots = base + rank(profile, "spare_threads") / 10L + gearSlots(profile);
        return (int) Math.min(Integer.MAX_VALUE, slots);
    }

    public static Stats stats(PuppetBlueprint blueprint, int level, PlayerClassProfile profile) {
        int f = blueprint.frame().ordinal();
        int d = blueprint.device().ordinal();
        int s = blueprint.size().ordinal();
        double lvl = Math.sqrt(Math.max(1, level)) - 1.0;
        double hpGrowth = .025 * lvl + .0045 * Math.min(30, rank(profile, "frame_refinement"));
        double dmgGrowth = .030 * lvl;
        double gear = gearStats(profile);
        double hp = FRAME_HP[f] * SIZE_HP[s] * (blueprint.core() == PuppetBlueprint.Core.SWIFT ? .90 : 1.0)
                * (1.0 + hpGrowth) * gear;
        double damage = DEVICE_DAMAGE[d] * FRAME_DAMAGE[f] * SIZE_DAMAGE[s]
                * (blueprint.core() == PuppetBlueprint.Core.OUTPUT ? 1.28 : 1.0)
                * (1.0 + dmgGrowth)
                * (1.0 + .004 * Math.min(30, rank(profile, "weapon_tuning"))) * gear;
        double coreInterval = switch(blueprint.core()) {
            case BASIC -> 1.0;
            case SWIFT -> .82;
            case STABLE -> 1.06;
            case OUTPUT -> 1.12;
        };
        double interval = DEVICE_INTERVAL[d] * FRAME_INTERVAL[f] * SIZE_INTERVAL[s] * coreInterval;
        int ticks = (int) Math.max(4, Math.min(12000, Math.ceil(interval)));
        double speed = FRAME_MOVE[f] * SIZE_MOVE[s]
                * (1.0 + .004 * Math.min(25, rank(profile, "mobility_axis")));
        double mitigation = switch (blueprint.core()) {
            case BASIC, SWIFT -> 1.0;
            case STABLE -> .86;
            case OUTPUT -> 1.10;
        };
        double reach = DEVICE_REACH[d] + (blueprint.device() == PuppetBlueprint.Device.LAUNCHER
                ? 0.0 : SIZE_REACH[s]);
        return new Stats(finite(hp, 1, 1_000_000), finite(damage, .1, 1_000_000), ticks,
                finite(speed, .1, 16), FRAME_ARMOR[f], reach, blueprint.size().height(),
                mitigation);
    }

    public static double incomingDamageFactor(PlayerClassProfile profile, PuppetBlueprint blueprint) {
        return stats(blueprint, 1, profile).incomingDamageFactor()
                * (1.0 - .004 * Math.min(30, rank(profile, "guarded_joints")));
    }

    private static double finite(double value, double min, double max) {
        if (Double.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
}
