package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent combat-relevant representation of one equipped project item.
 *
 * <p>The item id is the project data identity, not a donor/vanilla item-stat authority. Full item
 * generation, inventory UI and appearance data remain owned by their later systems.</p>
 */
public record EquippedCombatItem(
        String itemId,
        ProjectEquipmentSlot slot,
        int itemLevel,
        Optional<ProjectWeaponFamily> weaponFamily,
        boolean magicalFocus,
        Optional<ProjectArmorArchetype> armorArchetype,
        Optional<ProjectShieldFamily> shieldFamily,
        List<EquipmentCombatAffix> affixes
) {
    public static final Codec<EquippedCombatItem> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("item_id").forGetter(EquippedCombatItem::itemId),
                    ProjectEquipmentSlot.CODEC.fieldOf("slot").forGetter(EquippedCombatItem::slot),
                    Codec.intRange(
                            ProjectCombatRules.MIN_CONTENT_LEVEL,
                            ProjectCombatRules.MAX_CONTENT_LEVEL
                    ).fieldOf("item_level").forGetter(EquippedCombatItem::itemLevel),
                    ProjectWeaponFamily.CODEC.optionalFieldOf("weapon_family")
                            .forGetter(EquippedCombatItem::weaponFamily),
                    Codec.BOOL.optionalFieldOf("magical_focus", false)
                            .forGetter(EquippedCombatItem::magicalFocus),
                    ProjectArmorArchetype.CODEC.optionalFieldOf("armor_archetype")
                            .forGetter(EquippedCombatItem::armorArchetype),
                    ProjectShieldFamily.CODEC.optionalFieldOf("shield_family")
                            .forGetter(EquippedCombatItem::shieldFamily),
                    EquipmentCombatAffix.CODEC.listOf()
                            .optionalFieldOf("combat_affixes", List.of())
                            .forGetter(EquippedCombatItem::affixes)
            ).apply(instance, EquippedCombatItem::new)
    );

    /**
     * Backward-compatible source constructor for pre-defense M0 fixtures and persisted shape.
     */
    public EquippedCombatItem(
            String itemId,
            ProjectEquipmentSlot slot,
            int itemLevel,
            Optional<ProjectWeaponFamily> weaponFamily,
            boolean magicalFocus,
            List<EquipmentCombatAffix> affixes
    ) {
        this(
                itemId,
                slot,
                itemLevel,
                weaponFamily,
                magicalFocus,
                Optional.empty(),
                Optional.empty(),
                affixes
        );
    }

    public EquippedCombatItem {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(weaponFamily, "weaponFamily");
        Objects.requireNonNull(armorArchetype, "armorArchetype");
        Objects.requireNonNull(shieldFamily, "shieldFamily");
        Objects.requireNonNull(affixes, "affixes");

        String normalizedId = itemId.trim();
        if (normalizedId.isEmpty()
                || normalizedId.indexOf(':') <= 0
                || normalizedId.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Equipped project item id must be a namespaced id.");
        }
        itemId = normalizedId;
        affixes = List.copyOf(affixes);

        if (slot == ProjectEquipmentSlot.MAIN_WEAPON) {
            if (weaponFamily.isEmpty()
                    || magicalFocus
                    || armorArchetype.isPresent()
                    || shieldFamily.isPresent()) {
                throw new IllegalArgumentException(
                        "Main Weapon requires only a weapon family."
                );
            }
        } else if (weaponFamily.isPresent()) {
            throw new IllegalArgumentException(
                    "Only the Main Weapon slot owns the active weapon family."
            );
        }

        if (magicalFocus && slot != ProjectEquipmentSlot.OFF_HAND) {
            throw new IllegalArgumentException("A magical focus may only occupy Off-hand.");
        }
        if (magicalFocus && shieldFamily.isPresent()) {
            throw new IllegalArgumentException("Off-hand cannot be both a magical focus and a shield.");
        }
        if (shieldFamily.isPresent() && slot != ProjectEquipmentSlot.OFF_HAND) {
            throw new IllegalArgumentException("A shield family may only occupy Off-hand.");
        }
        if (armorArchetype.isPresent() && !ProjectArmorArchetype.isArmorSlot(slot)) {
            throw new IllegalArgumentException(
                    "Armor archetype requires Head/Chest/Legs/Gloves/Boots."
            );
        }
    }

    public static EquippedCombatItem weapon(
            String itemId,
            int itemLevel,
            ProjectWeaponFamily family,
            List<EquipmentCombatAffix> affixes
    ) {
        return new EquippedCombatItem(
                itemId,
                ProjectEquipmentSlot.MAIN_WEAPON,
                itemLevel,
                Optional.of(family),
                false,
                Optional.empty(),
                Optional.empty(),
                affixes
        );
    }

    public static EquippedCombatItem focus(
            String itemId,
            int itemLevel,
            List<EquipmentCombatAffix> affixes
    ) {
        return new EquippedCombatItem(
                itemId,
                ProjectEquipmentSlot.OFF_HAND,
                itemLevel,
                Optional.empty(),
                true,
                Optional.empty(),
                Optional.empty(),
                affixes
        );
    }

    public static EquippedCombatItem shield(
            String itemId,
            int itemLevel,
            ProjectShieldFamily family,
            List<EquipmentCombatAffix> affixes
    ) {
        return new EquippedCombatItem(
                itemId,
                ProjectEquipmentSlot.OFF_HAND,
                itemLevel,
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.of(Objects.requireNonNull(family, "family")),
                affixes
        );
    }

    public static EquippedCombatItem armor(
            String itemId,
            ProjectEquipmentSlot slot,
            int itemLevel,
            ProjectArmorArchetype archetype,
            List<EquipmentCombatAffix> affixes
    ) {
        if (!ProjectArmorArchetype.isArmorSlot(slot)) {
            throw new IllegalArgumentException("Armor item requires an armor equipment slot.");
        }
        return new EquippedCombatItem(
                itemId,
                slot,
                itemLevel,
                Optional.empty(),
                false,
                Optional.of(Objects.requireNonNull(archetype, "archetype")),
                Optional.empty(),
                affixes
        );
    }

    public static EquippedCombatItem gear(
            String itemId,
            ProjectEquipmentSlot slot,
            int itemLevel,
            List<EquipmentCombatAffix> affixes
    ) {
        if (slot == ProjectEquipmentSlot.MAIN_WEAPON) {
            throw new IllegalArgumentException("Use weapon(...) for Main Weapon.");
        }
        return new EquippedCombatItem(
                itemId,
                slot,
                itemLevel,
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                affixes
        );
    }
}
