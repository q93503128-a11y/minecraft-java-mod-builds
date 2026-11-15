package kr.moonseungjun.anomalousclasses.state;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Initial server-owned host adapters. These values are the existing v7 design
 * candidates, NOT final approved balance. Unimplemented boss/mod controls stay
 * explicitly unsupported rather than pretending to possess every LivingEntity.
 */
public final class ParasiteRules {
    public record HostSpec(long requiredBd, int intrusionTicks, double speedFactor) {
        public HostSpec {
            if (requiredBd < 0 || intrusionTicks < 10 || speedFactor <= 0) throw new IllegalArgumentException();
        }
    }
    private static final Map<String, HostSpec> SUPPORTED = Map.of(
        "minecraft:pig", new HostSpec(0, 30, 1.00),
        "minecraft:chicken", new HostSpec(0, 30, 1.10),
        "minecraft:sheep", new HostSpec(0, 30, 0.95),
        "minecraft:cow", new HostSpec(20, 50, 0.90),
        "minecraft:zombie", new HostSpec(100, 80, 0.90),
        "minecraft:spider", new HostSpec(160, 100, 1.10),
        "minecraft:creeper", new HostSpec(240, 120, 0.95)
    );
    private static final double[] FOCUS = {0.05, 0.10, 0.15, 0.20, 0.40};
    private ParasiteRules() {}

    public static HostSpec supported(ResourceLocation id) {
        return id == null ? null : SUPPORTED.get(id.toString());
    }
    public static long plannedRequirement(ResourceLocation id) {
        if (id == null) return -1;
        return switch (id.toString()) {
            case "minecraft:guardian" -> 600;
            case "minecraft:warden" -> 5000;
            default -> supported(id) == null ? -1 : supported(id).requiredBd();
        };
    }
    public static int rank(PlayerClassProfile profile, String id) {
        if (profile == null || id == null) return 0;
        var tree = GraphSkillTreeCatalog.tree(ClassId.PARASITE_MORPHER);
        var skill = tree.skills().get(id);
        if (skill == null) return 0;
        var purchased = profile.progress(ClassId.PARASITE_MORPHER).purchasedNodes();
        int ranks = 0;
        for (var segment : skill.segments()) {
            if (purchased.contains(segment.majorNodeId())) ranks += segment.oldRankTo() - segment.oldRankFrom() + 1;
        }
        return ranks;
    }
    public static double focus(PlayerClassProfile profile) {
        if (profile == null) return 0;
        var item = profile.equippedFocus().orElse(null);
        return item != null && item.classId() == ClassId.PARASITE_MORPHER
                ? FOCUS[Math.max(0, Math.min(4, item.tier().index()))] : 0;
    }
    public static double healthMultiplier(long bd, int mastery, PlayerClassProfile profile) {
        return bounded(1 + 0.05 * Math.sqrt(Math.max(0, bd) / 100.0))
                * (1 + Math.min(100, Math.max(0, mastery)) * 0.0005)
                * (1 + rank(profile, "tissue_reinforcement") * 0.0035)
                * (1 + focus(profile));
    }
    public static double damageMultiplier(long bd, int mastery, PlayerClassProfile profile) {
        return bounded(1 + 0.06 * Math.sqrt(Math.max(0, bd) / 100.0))
                * (1 + Math.min(100, Math.max(0, mastery)) * 0.0005)
                * (1 + rank(profile, "attack_adaptation") * 0.0035)
                * (1 + focus(profile));
    }
    public static double movementMultiplier(PlayerClassProfile profile) {
        return 1 + rank(profile, "body_control") * 0.003;
    }
    public static int infiltrationTicks(HostSpec spec, long bd, PlayerClassProfile profile) {
        double ratio = spec.requiredBd() == 0 ? 1.0
                : Math.max(1.0, Math.min(4.0, (double) bd / spec.requiredBd()));
        double bdFactor = 1.0 - 0.10 * (ratio - 1.0);
        double passive = 1.0 - 0.005 * rank(profile, "infiltration_precision");
        return (int) Math.max(Math.ceil(spec.intrusionTicks() * .55),
                Math.ceil(spec.intrusionTicks() * bdFactor * passive));
    }
    public static int lockoutTicks(PlayerClassProfile profile) {
        return Math.max(900, 1200 - 10 * rank(profile, "survival_transfer"));
    }
    public static int maxEmergencyCharges(PlayerClassProfile profile) {
        return Math.min(3, rank(profile, "survival_transfer") / 10);
    }
    public static float humanMaximum(PlayerClassProfile profile) {
        return 8.0F + 0.08F * rank(profile, "human_mimicry");
    }
    private static double bounded(double value) {
        return Math.min(100.0, Math.max(1.0, Double.isFinite(value) ? value : 1.0));
    }
}
