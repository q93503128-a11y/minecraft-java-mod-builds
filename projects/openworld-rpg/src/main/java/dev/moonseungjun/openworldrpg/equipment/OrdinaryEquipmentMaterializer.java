package dev.moonseungjun.openworldrpg.equipment;

import dev.moonseungjun.openworldrpg.combat.state.EquippedCombatItem;
import dev.moonseungjun.openworldrpg.combat.state.ProjectArmorArchetype;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.combat.state.ProjectShieldFamily;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared ordinary-equipment materializer.
 *
 * <p>It refuses to silently remove data-owned affixes whose runtime effect is not implemented.</p>
 */
public final class OrdinaryEquipmentMaterializer {
    private OrdinaryEquipmentMaterializer() {
    }

    public static MaterializedEquipment materialize(
            MaterializationRequest request,
            OrdinaryEquipmentAffixCatalogData catalog
    ) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(catalog, "catalog");

        for (var definition : request.eligibleAffixes()) {
            if (!catalog.runtimeImplemented(definition.id())) {
                throw new IllegalStateException(
                        "Equipment pool contains affix without live runtime adapter: "
                                + definition.id()
                );
            }
        }

        List<OrdinaryEquipmentAffixRoller.GeneratedAffix> generated =
                OrdinaryEquipmentAffixRoller.roll(
                        new OrdinaryEquipmentAffixRoller.RollRequest(
                                request.grade(),
                                request.itemLevel(),
                                request.base().affixFamily(),
                                request.eligibleAffixes(),
                                request.seed()
                        )
                );

        var runtimeAffixes = generated.stream()
                .map(OrdinaryEquipmentRuntimeAffixAdapter::adapt)
                .toList();

        EquippedCombatItem combatItem =
                request.base().combatItem(
                        request.itemLevel(),
                        runtimeAffixes
                );
        ProjectInventoryItem inventoryItem = ProjectInventoryItem.equipment(
                combatItem,
                request.grade(),
                request.unitSellValue()
        );

        return new MaterializedEquipment(
                inventoryItem,
                generated
        );
    }

    public record BaseProfile(
            String itemId,
            ProjectEquipmentSlot slot,
            OrdinaryEquipmentAffixRoller.ItemFamily affixFamily,
            Optional<ProjectWeaponFamily> weaponFamily,
            boolean magicalFocus,
            Optional<ProjectArmorArchetype> armorArchetype,
            Optional<ProjectShieldFamily> shieldFamily
    ) {
        public BaseProfile {
            requireStableId(itemId);
            Objects.requireNonNull(slot, "slot");
            Objects.requireNonNull(affixFamily, "affixFamily");
            weaponFamily = Objects.requireNonNull(
                    weaponFamily,
                    "weaponFamily"
            );
            armorArchetype = Objects.requireNonNull(
                    armorArchetype,
                    "armorArchetype"
            );
            shieldFamily = Objects.requireNonNull(
                    shieldFamily,
                    "shieldFamily"
            );
            validateShape();
        }

        public static BaseProfile weapon(
                String itemId,
                ProjectWeaponFamily family
        ) {
            return new BaseProfile(
                    itemId,
                    ProjectEquipmentSlot.MAIN_WEAPON,
                    OrdinaryEquipmentAffixRoller.ItemFamily.NORMAL_WEAPON,
                    Optional.of(Objects.requireNonNull(family, "family")),
                    false,
                    Optional.empty(),
                    Optional.empty()
            );
        }

        public static BaseProfile focus(String itemId) {
            return new BaseProfile(
                    itemId,
                    ProjectEquipmentSlot.OFF_HAND,
                    OrdinaryEquipmentAffixRoller.ItemFamily.MAGICAL_FOCUS,
                    Optional.empty(),
                    true,
                    Optional.empty(),
                    Optional.empty()
            );
        }

        public static BaseProfile shield(
                String itemId,
                ProjectShieldFamily family
        ) {
            return new BaseProfile(
                    itemId,
                    ProjectEquipmentSlot.OFF_HAND,
                    OrdinaryEquipmentAffixRoller.ItemFamily
                            .SHIELD_DEFENSIVE_OFFHAND,
                    Optional.empty(),
                    false,
                    Optional.empty(),
                    Optional.of(Objects.requireNonNull(family, "family"))
            );
        }

        public static BaseProfile armor(
                String itemId,
                ProjectEquipmentSlot slot,
                ProjectArmorArchetype archetype
        ) {
            OrdinaryEquipmentAffixRoller.ItemFamily family = switch (
                    Objects.requireNonNull(archetype, "archetype")
            ) {
                case LIGHT ->
                        OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_LIGHT;
                case MEDIUM ->
                        OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_MEDIUM;
                case HEAVY ->
                        OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_HEAVY;
            };
            return new BaseProfile(
                    itemId,
                    slot,
                    family,
                    Optional.empty(),
                    false,
                    Optional.of(archetype),
                    Optional.empty()
            );
        }

        public static BaseProfile accessory(
                String itemId,
                ProjectEquipmentSlot slot
        ) {
            return new BaseProfile(
                    itemId,
                    slot,
                    OrdinaryEquipmentAffixRoller.ItemFamily.ACCESSORY,
                    Optional.empty(),
                    false,
                    Optional.empty(),
                    Optional.empty()
            );
        }

        EquippedCombatItem combatItem(
                int itemLevel,
                List<dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatAffix>
                        affixes
        ) {
            if (slot == ProjectEquipmentSlot.MAIN_WEAPON) {
                return EquippedCombatItem.weapon(
                        itemId,
                        itemLevel,
                        weaponFamily.orElseThrow(),
                        affixes
                );
            }
            if (magicalFocus) {
                return EquippedCombatItem.focus(
                        itemId,
                        itemLevel,
                        affixes
                );
            }
            if (shieldFamily.isPresent()) {
                return EquippedCombatItem.shield(
                        itemId,
                        itemLevel,
                        shieldFamily.orElseThrow(),
                        affixes
                );
            }
            if (armorArchetype.isPresent()) {
                return EquippedCombatItem.armor(
                        itemId,
                        slot,
                        itemLevel,
                        armorArchetype.orElseThrow(),
                        affixes
                );
            }
            return EquippedCombatItem.gear(
                    itemId,
                    slot,
                    itemLevel,
                    affixes
            );
        }

        private void validateShape() {
            if (slot == ProjectEquipmentSlot.MAIN_WEAPON) {
                if (weaponFamily.isEmpty()
                        || magicalFocus
                        || armorArchetype.isPresent()
                        || shieldFamily.isPresent()
                        || affixFamily
                        != OrdinaryEquipmentAffixRoller.ItemFamily.NORMAL_WEAPON) {
                    throw new IllegalArgumentException(
                            "Weapon base profile shape mismatch."
                    );
                }
                return;
            }
            if (magicalFocus) {
                if (slot != ProjectEquipmentSlot.OFF_HAND
                        || weaponFamily.isPresent()
                        || armorArchetype.isPresent()
                        || shieldFamily.isPresent()
                        || affixFamily
                        != OrdinaryEquipmentAffixRoller.ItemFamily.MAGICAL_FOCUS) {
                    throw new IllegalArgumentException(
                            "Focus base profile shape mismatch."
                    );
                }
                return;
            }
            if (shieldFamily.isPresent()) {
                if (slot != ProjectEquipmentSlot.OFF_HAND
                        || weaponFamily.isPresent()
                        || armorArchetype.isPresent()
                        || affixFamily
                        != OrdinaryEquipmentAffixRoller.ItemFamily
                                .SHIELD_DEFENSIVE_OFFHAND) {
                    throw new IllegalArgumentException(
                            "Shield base profile shape mismatch."
                    );
                }
                return;
            }
            if (armorArchetype.isPresent()) {
                if (!ProjectArmorArchetype.isArmorSlot(slot)
                        || weaponFamily.isPresent()) {
                    throw new IllegalArgumentException(
                            "Armor base profile shape mismatch."
                    );
                }
                OrdinaryEquipmentAffixRoller.ItemFamily expected = switch (
                        armorArchetype.orElseThrow()
                ) {
                    case LIGHT ->
                            OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_LIGHT;
                    case MEDIUM ->
                            OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_MEDIUM;
                    case HEAVY ->
                            OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_HEAVY;
                };
                if (affixFamily != expected) {
                    throw new IllegalArgumentException(
                            "Armor affix-family/archetype mismatch."
                    );
                }
                return;
            }

            boolean accessorySlot = switch (slot) {
                case NECKLACE, RING_1, RING_2, CHARM, RELIC -> true;
                default -> false;
            };
            if (!accessorySlot
                    || affixFamily
                    != OrdinaryEquipmentAffixRoller.ItemFamily.ACCESSORY
                    || weaponFamily.isPresent()) {
                throw new IllegalArgumentException(
                        "Accessory base profile shape mismatch."
                );
            }
        }
    }

    public record MaterializationRequest(
            BaseProfile base,
            ProjectItemGrade grade,
            int itemLevel,
            List<OrdinaryEquipmentAffixRoller.AffixDefinition>
                    eligibleAffixes,
            long seed,
            long unitSellValue
    ) {
        public MaterializationRequest {
            Objects.requireNonNull(base, "base");
            Objects.requireNonNull(grade, "grade");
            eligibleAffixes = List.copyOf(
                    Objects.requireNonNull(
                            eligibleAffixes,
                            "eligibleAffixes"
                    )
            );
            if (itemLevel < 1 || itemLevel > 80) {
                throw new IllegalArgumentException(
                        "Item Lv must be inside 1..80."
                );
            }
            if (unitSellValue < 0L) {
                throw new IllegalArgumentException(
                        "unitSellValue must be non-negative."
                );
            }
        }
    }

    public record MaterializedEquipment(
            ProjectInventoryItem inventoryItem,
            List<OrdinaryEquipmentAffixRoller.GeneratedAffix> rolledAffixes
    ) {
        public MaterializedEquipment {
            Objects.requireNonNull(inventoryItem, "inventoryItem");
            rolledAffixes = List.copyOf(
                    Objects.requireNonNull(
                            rolledAffixes,
                            "rolledAffixes"
                    )
            );
        }
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected stable namespaced equipment id."
            );
        }
    }
}
