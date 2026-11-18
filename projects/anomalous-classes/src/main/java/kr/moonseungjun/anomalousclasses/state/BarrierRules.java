package kr.moonseungjun.anomalousclasses.state;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Combat numbers from BARRIER_COMBAT_BALANCE.md; still awaiting gameplay calibration. */
public final class BarrierRules {
    public enum Kind {
        DEFENSE(14 * 20, 18 * 20), COMPRESSION(12 * 20, 90), ISOLATION(16 * 20, 14 * 20);
        public final int cooldown, duration;
        Kind(int cooldown, int duration) { this.cooldown = cooldown; this.duration = duration; }
        public static Kind of(int i) { return i < 0 || i >= values().length ? null : values()[i]; }
    }
    public enum Size {
        SMALL(3, 1.15), MEDIUM(5, 1.0), LARGE(7, .8);
        public final double radius, strength;
        Size(double radius, double strength) { this.radius = radius; this.strength = strength; }
        public static Size of(int i) { return i < 0 || i >= values().length ? null : values()[i]; }
    }

    public static final int BOLT_COOLDOWN = 30;
    public static final int FLASH_COOLDOWN = 360;
    public static final int AMPLIFY_COOLDOWN = 480;
    public static final int AMPLIFY_TICKS = 100;
    public static final int[] COMPRESSION_TICKS = {24, 44, 66, 90};
    public static final float[] COMPRESSION_DAMAGE = {5, 6, 7, 8};
    private static final double[] FOCUS = {1.05, 1.10, 1.15, 1.20, 1.40};
    private BarrierRules() {}

    /** Major-node stages encode the equivalent legacy rank span once bought. */
    public static int rank(PlayerClassProfile profile, String id) {
        if (profile == null) return 0;
        var skill = GraphSkillTreeCatalog.tree(ClassId.BARRIER_CASTER).skills().get(id);
        if (skill == null) return 0;
        var owned = profile.progress(ClassId.BARRIER_CASTER).purchasedNodes();
        int total = 0;
        for (var stage : skill.segments()) {
            if (owned.contains(stage.majorNodeId()))
                total += stage.oldRankTo() - stage.oldRankFrom() + 1;
        }
        return total;
    }
    public static double power(int level) {
        return 1.0 + .025 * (Math.sqrt(Math.max(1, level)) - 1.0);
    }
    public static double focus(PlayerClassProfile profile) {
        if (profile == null) return 1.0;
        ClassFocus focus = profile.equippedFocus().orElse(null);
        return focus == null || focus.classId() != ClassId.BARRIER_CASTER
                ? 1.0 : FOCUS[focus.tier().index()];
    }
    public static int capacity(PlayerClassProfile profile) {
        int rank = rank(profile, "multiple_deployment");
        return 3 + (rank >= 15 ? 1 : 0) + (rank >= 35 ? 1 : 0);
    }
    public static double reach(PlayerClassProfile profile) {
        return 12.0 + .1 * rank(profile, "spatial_aiming");
    }
    public static int lifetime(Kind kind, PlayerClassProfile profile) {
        return kind.duration + (kind != Kind.COMPRESSION ? 3 * rank(profile, "lasting_inscription") : 0);
    }
    public static double integrity(Kind kind, Size size, int level, PlayerClassProfile profile) {
        double base = kind == Kind.DEFENSE ? 64.0 : kind == Kind.ISOLATION ? 52.0 : 0.0;
        String skill = kind == Kind.DEFENSE ? "defense_density" : "sealing_strength";
        return base * size.strength * power(level) * (1 + .0055 * rank(profile, skill)) * focus(profile);
    }
    public static double compression(int stage, Size size, int level, PlayerClassProfile profile,
                                     boolean isolated, boolean amplified) {
        if (stage < 0 || stage >= COMPRESSION_DAMAGE.length) return 0.0;
        return COMPRESSION_DAMAGE[stage] * size.strength * power(level)
                * (1 + .006 * rank(profile, "compression_density"))
                * (size == Size.SMALL ? 1 + .0045 * rank(profile, "focused_pressure") : 1)
                * (isolated ? 1 + .004 * rank(profile, "barrier_link") : 1)
                * focus(profile) * (amplified ? 1.35 : 1);
    }
    public static double flashIntegrity(int level, PlayerClassProfile profile) {
        return 20.0 * power(level) * (1 + .005 * rank(profile, "quick_barrier")) * focus(profile);
    }
    public static boolean inside(Vec3 position, Vec3 base, double radius) {
        double dx = position.x - base.x, dz = position.z - base.z;
        return dx * dx + dz * dz <= radius * radius
                && position.y >= base.y && position.y <= base.y + 4.0;
    }
    public static boolean overlaps(AABB box, Vec3 base, double radius) {
        double dx = Math.max(box.minX - base.x, Math.max(0.0, base.x - box.maxX));
        double dz = Math.max(box.minZ - base.z, Math.max(0.0, base.z - box.maxZ));
        return box.maxY >= base.y && box.minY <= base.y + 4.0
                && dx * dx + dz * dz <= radius * radius;
    }
    public static boolean crossing(Vec3 from, Vec3 to, Vec3 base, double radius) {
        return inside(from, base, radius) != inside(to, base, radius);
    }
}
