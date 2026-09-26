package dev.moonseungjun.openworldrpg.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.EquippedCombatItem;
import dev.moonseungjun.openworldrpg.recovery.RecoveryConsumable;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-owned ordinary-inventory item payload.
 *
 * <p>Visual model/icon data is intentionally absent. Equipment combat projections are preserved
 * when an equipped item moves into the Backpack/Storage, while final appearance remains owned by
 * the external-first asset binding.</p>
 */
public record ProjectInventoryItem(
        String itemId,
        int quantity,
        int stackCap,
        boolean starterBound,
        long unitSellValue,
        boolean favorite,
        boolean locked,
        Optional<ProjectItemGrade> equipmentGrade,
        Optional<EquippedCombatItem> equipmentProjection,
        Optional<RecoveryConsumable> recoveryConsumable
) {
    public static final Codec<ProjectInventoryItem> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("item_id").forGetter(ProjectInventoryItem::itemId),
                    Codec.intRange(1, 9999).fieldOf("quantity").forGetter(ProjectInventoryItem::quantity),
                    Codec.intRange(1, 9999).fieldOf("stack_cap").forGetter(ProjectInventoryItem::stackCap),
                    Codec.BOOL.optionalFieldOf("starter_bound", false).forGetter(ProjectInventoryItem::starterBound),
                    Codec.LONG.optionalFieldOf("unit_sell_value", 0L).forGetter(ProjectInventoryItem::unitSellValue),
                    Codec.BOOL.optionalFieldOf("favorite", false).forGetter(ProjectInventoryItem::favorite),
                    Codec.BOOL.optionalFieldOf("locked", false).forGetter(ProjectInventoryItem::locked),
                    ProjectItemGrade.CODEC.optionalFieldOf("equipment_grade")
                            .forGetter(ProjectInventoryItem::equipmentGrade),
                    EquippedCombatItem.CODEC.optionalFieldOf("equipment_projection")
                            .forGetter(ProjectInventoryItem::equipmentProjection),
                    RecoveryConsumable.CODEC.optionalFieldOf("recovery_consumable")
                            .forGetter(ProjectInventoryItem::recoveryConsumable)
            ).apply(instance, ProjectInventoryItem::new)
    );

    public ProjectInventoryItem {
        itemId = requireItemId(itemId);
        if (quantity < 1 || quantity > stackCap) {
            throw new IllegalArgumentException("quantity must be inside [1, stackCap].");
        }
        if (unitSellValue < 0L) {
            throw new IllegalArgumentException("unitSellValue must be non-negative.");
        }
        equipmentGrade = Objects.requireNonNull(equipmentGrade, "equipmentGrade");
        equipmentProjection = Objects.requireNonNull(equipmentProjection, "equipmentProjection");
        recoveryConsumable = Objects.requireNonNull(recoveryConsumable, "recoveryConsumable");

        if (starterBound && unitSellValue != 0L) {
            throw new IllegalArgumentException("starter-bound item must have zero sell value.");
        }
        if (equipmentProjection.isPresent()) {
            EquippedCombatItem equipment = equipmentProjection.orElseThrow();
            if (!equipment.itemId().equals(itemId) || quantity != 1 || stackCap != 1) {
                throw new IllegalArgumentException(
                        "Equipment inventory payload must be one non-stackable matching item."
                );
            }
            if (recoveryConsumable.isPresent()) {
                throw new IllegalArgumentException(
                        "Equipment inventory payload cannot also be a recovery consumable."
                );
            }
        } else if (equipmentGrade.isPresent()) {
            throw new IllegalArgumentException(
                    "Only equipment inventory payloads may carry an equipment grade."
            );
        }
        if (recoveryConsumable.isPresent() && stackCap != 20) {
            throw new IllegalArgumentException("R01 recovery consumables use backpack stack cap 20.");
        }
    }

    public static ProjectInventoryItem starterEquipment(EquippedCombatItem item) {
        Objects.requireNonNull(item, "item");
        return new ProjectInventoryItem(
                item.itemId(),
                1,
                1,
                true,
                0L,
                false,
                false,
                Optional.of(ProjectItemGrade.STANDARD),
                Optional.of(item),
                Optional.empty()
        );
    }

    public static ProjectInventoryItem equipment(
            EquippedCombatItem item,
            ProjectItemGrade grade,
            long unitSellValue
    ) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(grade, "grade");
        if (unitSellValue < 0L) {
            throw new IllegalArgumentException("unitSellValue must be non-negative.");
        }
        return new ProjectInventoryItem(
                item.itemId(),
                1,
                1,
                false,
                unitSellValue,
                false,
                false,
                Optional.of(grade),
                Optional.of(item),
                Optional.empty()
        );
    }

    public static ProjectInventoryItem recovery(
            String itemId,
            RecoveryConsumable consumable,
            int quantity,
            long unitSellValue
    ) {
        return new ProjectInventoryItem(
                itemId,
                quantity,
                20,
                false,
                unitSellValue,
                false,
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.of(Objects.requireNonNull(consumable, "consumable"))
        );
    }

    public static ProjectInventoryItem ordinary(
            String itemId,
            int quantity,
            int stackCap,
            long unitSellValue
    ) {
        return new ProjectInventoryItem(
                itemId,
                quantity,
                stackCap,
                false,
                unitSellValue,
                false,
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    public Optional<ProjectItemGrade> resolvedEquipmentGrade() {
        if (equipmentProjection.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(equipmentGrade.orElse(ProjectItemGrade.STANDARD));
    }

    public ProjectInventoryItem withQuantity(int nextQuantity) {
        if (nextQuantity == quantity) {
            return this;
        }
        return new ProjectInventoryItem(
                itemId,
                nextQuantity,
                stackCap,
                starterBound,
                unitSellValue,
                favorite,
                locked,
                equipmentGrade,
                equipmentProjection,
                recoveryConsumable
        );
    }

    public boolean canMerge(ProjectInventoryItem other) {
        Objects.requireNonNull(other, "other");
        return equipmentProjection.isEmpty()
                && other.equipmentProjection.isEmpty()
                && itemId.equals(other.itemId)
                && stackCap == other.stackCap
                && starterBound == other.starterBound
                && unitSellValue == other.unitSellValue
                && favorite == other.favorite
                && locked == other.locked
                && resolvedEquipmentGrade().equals(other.resolvedEquipmentGrade())
                && recoveryConsumable.equals(other.recoveryConsumable);
    }

    private static String requireItemId(String value) {
        if (value == null) {
            throw new IllegalArgumentException("itemId must not be null.");
        }
        String normalized = value.trim();
        if (normalized.isEmpty()
                || normalized.indexOf(':') <= 0
                || normalized.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("itemId must be a namespaced id.");
        }
        return normalized;
    }
}
