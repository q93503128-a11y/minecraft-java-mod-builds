package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;

public record EquipmentCombatAffix(
        EquipmentCombatAffixKind kind,
        double value,
        Optional<ProjectWeaponFamily> weaponFamily
) {
    public static final Codec<EquipmentCombatAffix> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EquipmentCombatAffixKind.CODEC.fieldOf("kind").forGetter(EquipmentCombatAffix::kind),
                    Codec.DOUBLE.fieldOf("value").forGetter(EquipmentCombatAffix::value),
                    ProjectWeaponFamily.CODEC.optionalFieldOf("weapon_family")
                            .forGetter(EquipmentCombatAffix::weaponFamily)
            ).apply(instance, EquipmentCombatAffix::new)
    );

    public EquipmentCombatAffix {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(weaponFamily, "weaponFamily");
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("Equipment combat-affix value must be finite and non-negative.");
        }

        boolean familyPower = kind == EquipmentCombatAffixKind.WEAPON_FAMILY_POWER;
        if (familyPower != weaponFamily.isPresent()) {
            throw new IllegalArgumentException(
                    "WEAPON_FAMILY_POWER requires exactly one weapon family and other affixes require none."
            );
        }

        if (isPrimary(kind) && Math.rint(value) != value) {
            throw new IllegalArgumentException("Primary-stat equipment affixes must use whole-number values.");
        }
    }

    public static EquipmentCombatAffix flat(
            EquipmentCombatAffixKind kind,
            double value
    ) {
        return new EquipmentCombatAffix(kind, value, Optional.empty());
    }

    public static EquipmentCombatAffix familyPower(
            ProjectWeaponFamily family,
            double value
    ) {
        return new EquipmentCombatAffix(
                EquipmentCombatAffixKind.WEAPON_FAMILY_POWER,
                value,
                Optional.of(family)
        );
    }

    private static boolean isPrimary(EquipmentCombatAffixKind kind) {
        return switch (kind) {
            case VIT, END, STR, DEX, INT, WIL -> true;
            default -> false;
        };
    }
}
