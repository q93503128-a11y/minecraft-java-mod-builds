package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Persistent canonical equipped-item state for the 12-slot RPG loadout.
 *
 * <p>This is deliberately the equipped loadout only, not the backpack/storage/loot-generation
 * system. It publishes both offensive and defensive authority without inheriting donor item stats.</p>
 */
public record PlayerEquipmentLoadoutState(List<EquippedCombatItem> equipped) {
    private static final double WEAPON_FAMILY_POWER_GEAR_CAP = 0.60;
    private static final double GUARD_STRENGTH_GEAR_CAP = 0.50;
    private static final double POISE_STAGGER_RESISTANCE_GEAR_CAP = 0.50;

    public static final Codec<PlayerEquipmentLoadoutState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EquippedCombatItem.CODEC.listOf()
                            .fieldOf("equipped")
                            .forGetter(PlayerEquipmentLoadoutState::equipped)
            ).apply(instance, PlayerEquipmentLoadoutState::new)
    );

    public PlayerEquipmentLoadoutState {
        equipped = List.copyOf(equipped);
        if (equipped.size() > ProjectEquipmentSlot.values().length) {
            throw new IllegalArgumentException("Equipped loadout exceeds the canonical 12 slots.");
        }

        EnumSet<ProjectEquipmentSlot> seen = EnumSet.noneOf(ProjectEquipmentSlot.class);
        for (EquippedCombatItem item : equipped) {
            if (!seen.add(item.slot())) {
                throw new IllegalArgumentException("Duplicate equipped slot: " + item.slot());
            }
        }

        Optional<EquippedCombatItem> main = item(ProjectEquipmentSlot.MAIN_WEAPON, equipped);
        Optional<EquippedCombatItem> offhand = item(ProjectEquipmentSlot.OFF_HAND, equipped);
        if (main.isPresent()
                && main.get().weaponFamily().orElseThrow().usesBothHands()
                && offhand.isPresent()) {
            throw new IllegalArgumentException(
                    "A two-handed weapon cannot simultaneously occupy Off-hand."
            );
        }
    }

    public static PlayerEquipmentLoadoutState empty() {
        return new PlayerEquipmentLoadoutState(List.of());
    }

    public Optional<EquippedCombatItem> item(ProjectEquipmentSlot slot) {
        return item(slot, equipped);
    }

    public PlayerEquipmentLoadoutState withEquipped(EquippedCombatItem item) {
        ArrayList<EquippedCombatItem> next = new ArrayList<>(equipped.size() + 1);
        for (EquippedCombatItem current : equipped) {
            if (current.slot() != item.slot()) {
                next.add(current);
            }
        }
        next.add(item);
        return new PlayerEquipmentLoadoutState(next);
    }

    public PlayerEquipmentLoadoutState without(ProjectEquipmentSlot slot) {
        return new PlayerEquipmentLoadoutState(
                equipped.stream().filter(item -> item.slot() != slot).toList()
        );
    }

    /**
     * Aggregates the six primary-stat affixes across all 12 equipment slots independently of
     * whether a main weapon is currently equipped. Vitals/resources must not disappear when the
     * player unequips a weapon.
     */
    public EffectiveAttributes aggregateFlatAttributeBonuses() {
        double vit = 0.0;
        double end = 0.0;
        double str = 0.0;
        double dex = 0.0;
        double intel = 0.0;
        double wil = 0.0;

        for (EquippedCombatItem item : equipped) {
            for (EquipmentCombatAffix affix : item.affixes()) {
                switch (affix.kind()) {
                    case VIT -> vit += affix.value();
                    case END -> end += affix.value();
                    case STR -> str += affix.value();
                    case DEX -> dex += affix.value();
                    case INT -> intel += affix.value();
                    case WIL -> wil += affix.value();
                    default -> {
                        // Non-primary combat affixes are aggregated by their owned publisher.
                    }
                }
            }
        }
        return new EffectiveAttributes(vit, end, str, dex, intel, wil);
    }

    public double aggregateArmorPoise() {
        double result = 0.0;
        for (EquippedCombatItem item : equipped) {
            if (item.armorArchetype().isEmpty()) {
                continue;
            }
            double fullSet = switch (item.armorArchetype().orElseThrow()) {
                case LIGHT -> 0.0;
                case MEDIUM -> 15.0;
                case HEAVY -> 35.0;
            };
            double share = switch (item.slot()) {
                case HEAD -> 0.15;
                case CHEST -> 0.33;
                case LEGS -> 0.25;
                case GLOVES -> 0.12;
                case BOOTS -> 0.15;
                default -> throw new IllegalStateException(
                        "Armor archetype published from non-armor slot: " + item.slot()
                );
            };
            result += fullSet * share;
        }
        return result;
    }

    public double aggregatePoiseStaggerResistanceBonus() {
        double result = 0.0;
        for (EquippedCombatItem item : equipped) {
            for (EquipmentCombatAffix affix : item.affixes()) {
                if (affix.kind() == EquipmentCombatAffixKind.POISE_STAGGER_RESISTANCE) {
                    result += affix.value();
                }
            }
        }
        return Math.min(result, POISE_STAGGER_RESISTANCE_GEAR_CAP);
    }

    /**
     * Publishes canonical equipped Defense/MR and shield guard authority even with no main weapon.
     *
     * <p>Armor slots are individually Item-Lv scaled and rounded before summation. Defense/MR
     * affixes then modify those effective totals. Guard Strength affects only an actually equipped
     * shield and respects the +50% aggregate gear-contribution cap.</p>
     */
    public PlayerDefenseAuthority.DefenseSnapshot aggregateDefenseSnapshot() {
        double baseDefense = 0.0;
        double baseMagicResistance = 0.0;
        double defenseBonus = 0.0;
        double magicResistanceBonus = 0.0;
        double guardStrengthBonus = 0.0;

        for (EquippedCombatItem item : equipped) {
            if (item.armorArchetype().isPresent()) {
                ProjectArmorArchetype archetype = item.armorArchetype().orElseThrow();
                baseDefense += archetype.scaledDefense(item.slot(), item.itemLevel());
                baseMagicResistance += archetype.scaledMagicResistance(
                        item.slot(),
                        item.itemLevel()
                );
            }

            for (EquipmentCombatAffix affix : item.affixes()) {
                switch (affix.kind()) {
                    case DEFENSE -> defenseBonus += affix.value();
                    case MAGIC_RESISTANCE -> magicResistanceBonus += affix.value();
                    case GUARD_STRENGTH -> guardStrengthBonus += affix.value();
                    case POISE_STAGGER_RESISTANCE -> {
                        // Published independently by aggregatePoiseStaggerResistanceBonus().
                    }
                    default -> {
                        // Owned by primary/offense publishers or a later dedicated runtime.
                    }
                }
            }
        }

        double effectiveDefense = baseDefense * (1.0 + defenseBonus);
        double effectiveMagicResistance =
                baseMagicResistance * (1.0 + magicResistanceBonus);

        EquippedCombatItem offhand = item(ProjectEquipmentSlot.OFF_HAND).orElse(null);
        if (offhand == null || offhand.shieldFamily().isEmpty()) {
            return PlayerDefenseAuthority.DefenseSnapshot.unguarded(
                    effectiveDefense,
                    effectiveMagicResistance
            );
        }

        PlayerDefenseAuthority.GuardType guardType =
                offhand.shieldFamily().orElseThrow().guardType();
        double baseGuardRating = PlayerDefenseAuthority.shieldGuardRating(
                offhand.itemLevel(),
                guardType
        );
        double effectiveGuardRating = baseGuardRating
                * (1.0 + Math.min(guardStrengthBonus, GUARD_STRENGTH_GEAR_CAP));

        return PlayerDefenseAuthority.DefenseSnapshot.guarded(
                effectiveDefense,
                effectiveMagicResistance,
                effectiveGuardRating,
                guardType
        );
    }

    public Optional<EquipmentCombatState> aggregateCombatState() {
        EquippedCombatItem main = item(ProjectEquipmentSlot.MAIN_WEAPON).orElse(null);
        if (main == null) {
            return Optional.empty();
        }

        ProjectWeaponFamily family = main.weaponFamily().orElseThrow();
        EffectiveAttributes flatAttributes = aggregateFlatAttributeBonuses();
        double physicalPower = 0.0;
        double magicPower = 0.0;
        double familyPower = 0.0;
        double poiseOutput = 0.0;

        for (EquippedCombatItem item : equipped) {
            for (EquipmentCombatAffix affix : item.affixes()) {
                switch (affix.kind()) {
                    case VIT, END, STR, DEX, INT, WIL -> {
                        // Already included in aggregateFlatAttributeBonuses().
                    }
                    case PHYSICAL_POWER -> physicalPower += affix.value();
                    case MAGIC_POWER -> magicPower += affix.value();
                    case WEAPON_FAMILY_POWER -> {
                        if (affix.weaponFamily().orElseThrow() == family) {
                            familyPower += affix.value();
                        }
                    }
                    case POISE_OUTPUT -> poiseOutput += affix.value();
                    case CRITICAL_CHANCE, ATTACK_SPEED, MAX_MANA -> {
                        // Preserved in the item payload; dedicated crit/cadence/resource publishers own runtime use.
                    }
                    case DEFENSE, MAGIC_RESISTANCE, GUARD_STRENGTH,
                            POISE_STAGGER_RESISTANCE -> {
                        // Published independently by defensive/poise publishers.
                    }
                }
            }
        }

        familyPower = Math.min(familyPower, WEAPON_FAMILY_POWER_GEAR_CAP);

        double supplementalMagicWeaponPower = 0.0;
        EquippedCombatItem offhand = item(ProjectEquipmentSlot.OFF_HAND).orElse(null);
        if (family == ProjectWeaponFamily.WAND
                && offhand != null
                && offhand.magicalFocus()) {
            supplementalMagicWeaponPower =
                    22.0 * ProjectCombatRules.gearScale(offhand.itemLevel()) * 0.18;
        }

        return Optional.of(new EquipmentCombatState(
                family,
                main.itemLevel(),
                flatAttributes,
                physicalPower,
                magicPower,
                familyPower,
                poiseOutput,
                supplementalMagicWeaponPower
        ));
    }

    private static Optional<EquippedCombatItem> item(
            ProjectEquipmentSlot slot,
            List<EquippedCombatItem> items
    ) {
        return items.stream().filter(item -> item.slot() == slot).findFirst();
    }
}
