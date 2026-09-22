package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/**
 * Canonical weapon-family combat factors from COMBAT_BALANCE.md and EQUIPMENT_BALANCE.md.
 */
public enum ProjectWeaponFamily {
    DAGGER(0.88, 1.85, 0.55, 0.15, 0.85, 0.0, 0.0),
    DUAL_BLADES(1.03, 1.35, 0.65, 0.25, 0.75, 0.0, 0.0),
    SWORD(0.95, 1.25, 1.00, 0.50, 0.50, 0.0, 0.0),
    GREATSWORD(1.10, 0.78, 1.60, 0.80, 0.20, 0.0, 0.0),
    SPEAR(1.03, 1.05, 1.10, 0.40, 0.60, 0.0, 0.0),
    AXE(0.97, 0.90, 1.35, 0.75, 0.25, 0.0, 0.0),
    HAMMER_MACE(1.10, 0.70, 1.80, 0.90, 0.10, 0.0, 0.0),
    BOW(1.02, 1.00, 0.80, 0.10, 0.90, 0.0, 0.0),
    CROSSBOW(1.07, 0.72, 1.10, 0.25, 0.75, 0.0, 0.0),
    BLACK_POWDER_PISTOL(0.95, 0.85, 0.90, 0.20, 0.80, 0.0, 0.0),
    MUSKET_HAND_CANNON(1.10, 0.55, 1.25, 0.35, 0.65, 0.0, 0.0),
    STAFF(1.00, 1.15, 0.85, 0.0, 0.0, 0.85, 0.15),
    WAND(0.82, 1.45, 0.50, 0.0, 0.0, 0.75, 0.25);

    public static final Codec<ProjectWeaponFamily> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(
                            ProjectWeaponFamily.valueOf(value.trim().toUpperCase(Locale.ROOT))
                    );
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown Openworld RPG weapon family: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );

    private final double powerFactor;
    private final double basicCadence;
    private final double poiseMultiplier;
    private final double strWeight;
    private final double dexWeight;
    private final double intWeight;
    private final double wilWeight;

    ProjectWeaponFamily(
            double powerFactor,
            double basicCadence,
            double poiseMultiplier,
            double strWeight,
            double dexWeight,
            double intWeight,
            double wilWeight
    ) {
        this.powerFactor = powerFactor;
        this.basicCadence = basicCadence;
        this.poiseMultiplier = poiseMultiplier;
        this.strWeight = strWeight;
        this.dexWeight = dexWeight;
        this.intWeight = intWeight;
        this.wilWeight = wilWeight;
    }

    public double powerFactor() {
        return powerFactor;
    }

    public double basicCadence() {
        return basicCadence;
    }

    public double poiseMultiplier() {
        return poiseMultiplier;
    }

    public double weightedOffensiveStat(EffectiveAttributes attributes) {
        return attributes.str() * strWeight
                + attributes.dex() * dexWeight
                + attributes.intel() * intWeight
                + attributes.wil() * wilWeight;
    }
}
