package kr.moonseungjun.anomalousclasses.state;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-owned starting values. The numbers below are balance candidates, not playtested constants. */
public final class MentalDominatorConfig {
    public static final ModConfigSpec SPEC;
    public static final MentalDominatorConfig VALUES;

    static {
        var pair = new ModConfigSpec.Builder().configure(MentalDominatorConfig::new);
        VALUES = pair.getLeft();
        SPEC = pair.getRight();
    }

    public final ModConfigSpec.DoubleValue basePsyche;
    public final ModConfigSpec.DoubleValue psychePerLevel;
    public final ModConfigSpec.DoubleValue baseRegeneration;
    public final ModConfigSpec.DoubleValue regenerationPerLevel;
    public final ModConfigSpec.DoubleValue baseControl;
    public final ModConfigSpec.DoubleValue controlPerLevel;
    public final ModConfigSpec.DoubleValue maxCaptureRange;
    public final ModConfigSpec.DoubleValue intrusionStartCost;
    public final ModConfigSpec.DoubleValue intrusionCostPerSecond;
    public final ModConfigSpec.DoubleValue intrusionProgressPerSecond;
    public final ModConfigSpec.DoubleValue interruptionDecayPerSecond;
    public final ModConfigSpec.IntValue intrusionCooldownTicks;
    public final ModConfigSpec.IntValue baseControlledSlots;
    public final ModConfigSpec.IntValue levelsPerControlledSlot;
    public final ModConfigSpec.DoubleValue threatHealthWeight;
    public final ModConfigSpec.DoubleValue threatAttackWeight;
    public final ModConfigSpec.DoubleValue threatArmorWeight;
    public final ModConfigSpec.DoubleValue threatBase;
    public final ModConfigSpec.DoubleValue maintenanceThreatFactor;
    public final ModConfigSpec.DoubleValue maintenanceThreatExponent;
    public final ModConfigSpec.DoubleValue maintenanceLevelDiscount;
    public final ModConfigSpec.DoubleValue groupMaintenanceFactor;
    public final ModConfigSpec.DoubleValue followSpeed;

    private MentalDominatorConfig(ModConfigSpec.Builder builder) {
        builder.comment("Adjustable initial values for the Mental Dominator. All gameplay values are decided by the server.",
                "Capture and maintenance values are candidates and have not been playtested.").push("mental_dominator");
        basePsyche = decimal(builder, "base_psyche", 100.0, 1.0, 1_000_000.0,
                "Base maximum psyche before level and focus bonuses.");
        psychePerLevel = decimal(builder, "psyche_per_level", 5.0, 0.0, 100_000.0,
                "Maximum psyche added per class level after level 1.");
        baseRegeneration = decimal(builder, "base_regeneration_per_second", 2.5, 0.0, 100_000.0,
                "Natural psyche regeneration per second at level 1.");
        regenerationPerLevel = decimal(builder, "regeneration_per_level", 0.06, 0.0, 10_000.0,
                "Natural psyche regeneration added per level.");
        baseControl = decimal(builder, "base_control", 1.0, 0.01, 1_000_000.0,
                "Base intrusion control multiplier.");
        controlPerLevel = decimal(builder, "control_per_level", 0.035, 0.0, 100_000.0,
                "Intrusion control multiplier added per level after level 1.");
        maxCaptureRange = decimal(builder, "max_capture_range", 16.0, 2.0, 64.0,
                "Maximum unobstructed distance for starting mental intrusion.");
        intrusionStartCost = decimal(builder, "intrusion_start_cost", 12.0, 0.0, 1_000_000.0,
                "One-time psyche cost when intrusion starts.");
        intrusionCostPerSecond = decimal(builder, "intrusion_cost_per_second", 5.0, 0.0, 1_000_000.0,
                "Psyche drain while channeling intrusion.");
        intrusionProgressPerSecond = decimal(builder, "intrusion_progress_per_second", 40.0, 0.01, 1_000_000.0,
                "Base control progress per second before target resistance.");
        interruptionDecayPerSecond = decimal(builder, "interruption_decay_per_second", 20.0, 0.0, 1_000_000.0,
                "Control progress lost per second after the channel is interrupted.");
        intrusionCooldownTicks = builder.comment("Candidate cooldown after a completed or interrupted channel.")
                .defineInRange("intrusion_cooldown_ticks", 80, 0, 20 * 60 * 60);
        baseControlledSlots = builder.comment("Starting controlled-entity slots at class level 1.")
                .defineInRange("base_controlled_slots", 3, 1, 1024);
        levelsPerControlledSlot = builder.comment("Level divisor used by the square-root slot growth candidate.")
                .defineInRange("levels_per_controlled_slot", 8, 1, 1_000_000);
        threatBase = decimal(builder, "threat_base", 4.0, 0.0, 1_000_000.0,
                "Base threat score used by the candidate resistance model.");
        threatHealthWeight = decimal(builder, "threat_health_weight", 4.0, 0.0, 1_000_000.0,
                "Weight applied to the square root of target max health in the resistance model.");
        threatAttackWeight = decimal(builder, "threat_attack_weight", 1.4, 0.0, 1_000_000.0,
                "Weight applied to target attack damage in the resistance model.");
        threatArmorWeight = decimal(builder, "threat_armor_weight", 0.3, 0.0, 1_000_000.0,
                "Weight applied to target armor in the resistance model.");
        maintenanceThreatFactor = decimal(builder, "maintenance_threat_factor", 0.018, 0.0, 1_000_000.0,
                "Threat factor in the candidate maintenance model.");
        maintenanceThreatExponent = decimal(builder, "maintenance_threat_exponent", 1.4, 0.01, 10.0,
                "Threat exponent in the candidate maintenance model.");
        maintenanceLevelDiscount = decimal(builder, "maintenance_level_discount", 0.006, 0.0, 1_000_000.0,
                "Flat per-level reduction in the candidate maintenance model.");
        groupMaintenanceFactor = decimal(builder, "group_maintenance_factor", 0.05, 0.0, 1_000_000.0,
                "Additional maintenance factor for controlling multiple loaded entities.");
        followSpeed = decimal(builder, "follow_speed", 1.0, 0.1, 4.0,
                "Path navigation speed for controlled PathfinderMob entities.");
        builder.pop();
    }

    private static ModConfigSpec.DoubleValue decimal(ModConfigSpec.Builder builder, String key, double value,
                                                       double min, double max, String comment) {
        return builder.comment(comment).defineInRange(key, value, min, max);
    }

    static double value(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    static int value(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private MentalDominatorConfig() {
        throw new AssertionError("Use the config spec");
    }
}
