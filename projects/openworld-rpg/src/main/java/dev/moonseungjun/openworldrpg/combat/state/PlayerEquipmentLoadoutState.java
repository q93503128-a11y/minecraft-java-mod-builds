package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Persistent canonical equipped-item state for the 12-slot RPG loadout.
 *
 * <p>This is deliberately the equipped loadout only, not the backpack/storage/loot-generation
 * system. It is sufficient to publish authoritative combat stats without inventing those larger
 * systems during M0.</p>
 */
public record PlayerEquipmentLoadoutState(List<EquippedCombatItem> equipped) {
    private static final double WEAPON_FAMILY_POWER_GEAR_CAP = 0.60;

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
                && main.get().weaponFamily().orElseThrow() == ProjectWeaponFamily.STAFF
                && offhand.map(EquippedCombatItem::magicalFocus).orElse(false)) {
            throw new IllegalArgumentException("A two-handed staff cannot simultaneously use a magical focus.");
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
                    default -> {
                        // Non-primary combat affixes are aggregated by aggregateCombatState().
                    }
                }
            }
        }
        return new EffectiveAttributes(vit, end, str, dex, intel, wil);
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
                    case VIT -> vit += affix.value();
                    case END -> end += affix.value();
                    case STR -> str += affix.value();
                    case DEX -> dex += affix.value();
                    case INT -> intel += affix.value();
                    case WIL -> wil += affix.value();
                    case PHYSICAL_POWER -> physicalPower += affix.value();
                    case MAGIC_POWER -> magicPower += affix.value();
                    case WEAPON_FAMILY_POWER -> {
                        if (affix.weaponFamily().orElseThrow() == family) {
                            familyPower += affix.value();
                        }
                    }
                    case POISE_OUTPUT -> poiseOutput += affix.value();
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
