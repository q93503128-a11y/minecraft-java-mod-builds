package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.runtime.HunterQuickstepVolleyRules;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import java.util.Optional;

public record ProjectSpellSpec(
        String id,
        double manaCost,
        double staminaCost,
        int cooldownTicks,
        double actionCoefficient,
        double poiseCoefficient,
        int reentryWindowTicks
) {
    public static final String WARRIOR_DRIVING_SLASH_ID = "openworld_rpg:warrior_driving_slash";
    public static final String WARRIOR_IRON_COUNTER_ID = "openworld_rpg:warrior_iron_counter";
    public static final String WARRIOR_CYCLONE_CUT_ID = "openworld_rpg:warrior_cyclone_cut";
    public static final String WARRIOR_BREAKER_SLAM_ID = "openworld_rpg:warrior_breaker_slam";
    public static final String WARRIOR_EARTHSHATTER_ID = "openworld_rpg:warrior_earthshatter";
    public static final String HUNTER_QUICKSTEP_VOLLEY_ID = "openworld_rpg:hunter_quickstep_volley";
    public static final String ARC_BOLT_ID = "openworld_rpg:arc_bolt";
    public static final String RADIANT_LANCE_ID = "openworld_rpg:radiant_lance";
    public static final String MEND_ID = "openworld_rpg:mend";
    public static final String CONSECRATED_GROUND_ID = "openworld_rpg:consecrated_ground";
    public static final String REBUKE_ID = "openworld_rpg:rebuke";
    public static final String SANCTUARY_ID = "openworld_rpg:sanctuary";
    public static final double RADIANT_LANCE_ACTION_COEFFICIENT = 1.35;
    public static final double RADIANT_LANCE_POISE_COEFFICIENT = 0.60;
    public static final double RADIANT_LANCE_CHAIN_ACTION_COEFFICIENT = 0.55;
    public static final double RADIANT_LANCE_FALLBACK_HEAL_COEFFICIENT = 0.08;
    public static final double MEND_HEAL_COEFFICIENT = 0.30;
    public static final double MEND_EMPOWERED_HEAL_COEFFICIENT = 0.40;
    public static final double REBUKE_ACTION_COEFFICIENT = 1.80;
    public static final double REBUKE_POISE_COEFFICIENT = 1.60;
    public static final double REBUKE_EMPOWERED_POISE_COEFFICIENT = 2.20;

    public ProjectSpellSpec {
        Objects.requireNonNull(id, "id");
        if (!id.startsWith("openworld_rpg:")) {
            throw new IllegalArgumentException("Project spell must use openworld_rpg namespace: " + id);
        }
        if (!Double.isFinite(manaCost) || manaCost < 0) {
            throw new IllegalArgumentException("Mana cost must be finite and non-negative.");
        }
        if (!Double.isFinite(staminaCost) || staminaCost < 0) {
            throw new IllegalArgumentException("Stamina cost must be finite and non-negative.");
        }
        if (cooldownTicks < 0 || !Double.isFinite(actionCoefficient) || actionCoefficient < 0
                || !Double.isFinite(poiseCoefficient) || poiseCoefficient < 0 || reentryWindowTicks < 0) {
            throw new IllegalArgumentException("Invalid project spell numeric contract: " + id);
        }
    }

    public ProjectSpellSpec(
            String id,
            double manaCost,
            int cooldownTicks,
            double actionCoefficient,
            double poiseCoefficient,
            int reentryWindowTicks
    ) {
        this(
                id,
                manaCost,
                0.0,
                cooldownTicks,
                actionCoefficient,
                poiseCoefficient,
                reentryWindowTicks
        );
    }

    public static ProjectSpellSpec warriorDrivingSlash() {
        return new ProjectSpellSpec(
                WARRIOR_DRIVING_SLASH_ID,
                20.0,
                120,
                0.0,
                0.0,
                1
        );
    }

    public static ProjectSpellSpec warriorIronCounter() {
        return new ProjectSpellSpec(
                WARRIOR_IRON_COUNTER_ID,
                0.0,
                18.0,
                200,
                0.0,
                0.0,
                1
        );
    }

    public static ProjectSpellSpec warriorCycloneCut() {
        return new ProjectSpellSpec(
                WARRIOR_CYCLONE_CUT_ID,
                28.0,
                220,
                0.0,
                0.0,
                1
        );
    }

    public static ProjectSpellSpec warriorBreakerSlam() {
        return new ProjectSpellSpec(
                WARRIOR_BREAKER_SLAM_ID,
                36.0,
                300,
                0.0,
                0.0,
                1
        );
    }

    public static ProjectSpellSpec warriorEarthshatter() {
        return new ProjectSpellSpec(
                WARRIOR_EARTHSHATTER_ID,
                0.0,
                0,
                0.0,
                0.0,
                1
        );
    }

    public static ProjectSpellSpec hunterQuickstepVolley() {
        return new ProjectSpellSpec(
                HUNTER_QUICKSTEP_VOLLEY_ID,
                18.0,
                140,
                HunterQuickstepVolleyRules.SAME_TARGET_ACTION_COEFFICIENT_CAP,
                HunterQuickstepVolleyRules.WHOLE_ACTION_POISE_COEFFICIENT,
                1
        );
    }

    public static ProjectSpellSpec arcBolt() {
        return new ProjectSpellSpec(
                ARC_BOLT_ID,
                12.0,
                60,
                1.20,
                0.50,
                1
        );
    }

    public static ProjectSpellSpec radiantLance() {
        return new ProjectSpellSpec(
                RADIANT_LANCE_ID,
                14.0,
                80,
                RADIANT_LANCE_ACTION_COEFFICIENT,
                RADIANT_LANCE_POISE_COEFFICIENT,
                1
        );
    }

    public static ProjectSpellSpec consecratedGround() {
        return new ProjectSpellSpec(
                CONSECRATED_GROUND_ID,
                32.0,
                280,
                0.0,
                0.0,
                1
        );
    }

    public static ProjectSpellSpec rebuke() {
        return new ProjectSpellSpec(
                REBUKE_ID,
                24.0,
                200,
                REBUKE_ACTION_COEFFICIENT,
                REBUKE_POISE_COEFFICIENT,
                1
        );
    }

    public static ProjectSpellSpec mend() {
        return new ProjectSpellSpec(
                MEND_ID,
                22.0,
                160,
                0.0,
                0.0,
                1
        );
    }

    public static ProjectSpellSpec sanctuary() {
        return new ProjectSpellSpec(
                SANCTUARY_ID,
                0.0,
                0,
                0.0,
                0.0,
                1
        );
    }

    public static Optional<RootClass> requiredRootClass(String spellId) {
        Objects.requireNonNull(spellId, "spellId");
        return switch (spellId) {
            case WARRIOR_DRIVING_SLASH_ID, WARRIOR_IRON_COUNTER_ID, WARRIOR_CYCLONE_CUT_ID, WARRIOR_BREAKER_SLAM_ID, WARRIOR_EARTHSHATTER_ID -> Optional.of(RootClass.WARRIOR);
            case HUNTER_QUICKSTEP_VOLLEY_ID -> Optional.of(RootClass.HUNTER);
            case ARC_BOLT_ID -> Optional.of(RootClass.MAGE);
            case RADIANT_LANCE_ID, MEND_ID, CONSECRATED_GROUND_ID, REBUKE_ID, SANCTUARY_ID ->
                    Optional.of(RootClass.CLERIC);
            default -> Optional.empty();
        };
    }
}
