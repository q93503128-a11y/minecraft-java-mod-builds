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
                    EquipmentCombatAffix.CODEC.listOf()
                            .optionalFieldOf("combat_affixes", List.of())
                            .forGetter(EquippedCombatItem::affixes)
            ).apply(instance, EquippedCombatItem::new)
    );

    public EquippedCombatItem {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(weaponFamily, "weaponFamily");
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
            if (weaponFamily.isEmpty() || magicalFocus) {
                throw new IllegalArgumentException(
                        "Main Weapon requires a weapon family and cannot be a magical focus."
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
                affixes
        );
    }
}
