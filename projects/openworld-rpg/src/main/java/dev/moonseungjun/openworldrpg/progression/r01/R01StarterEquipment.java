package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.EquippedCombatItem;
import dev.moonseungjun.openworldrpg.combat.state.ProjectShieldFamily;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Canonical combat-state projection of the R01 opening and first-class starter equipment. */
public final class R01StarterEquipment {
    public static final int STARTER_ITEM_LEVEL = 1;

    private R01StarterEquipment() {
    }

    public static EquippedCombatItem openingSword() {
        return EquippedCombatItem.weapon(
                "openworld_rpg:heartland_arming_sword",
                STARTER_ITEM_LEVEL,
                ProjectWeaponFamily.SWORD,
                List.of()
        );
    }

    public static EquippedCombatItem openingBuckler() {
        return EquippedCombatItem.shield(
                "openworld_rpg:watch_buckler",
                STARTER_ITEM_LEVEL,
                ProjectShieldFamily.BUCKLER,
                List.of()
        );
    }

    public static Optional<EquippedCombatItem> firstClassWeapon(RootClass rootClass) {
        Objects.requireNonNull(rootClass, "rootClass");
        return switch (rootClass) {
            case WARRIOR, GUARDIAN -> Optional.empty();
            case HUNTER -> Optional.of(EquippedCombatItem.weapon(
                    "openworld_rpg:riverwood_bow",
                    STARTER_ITEM_LEVEL,
                    ProjectWeaponFamily.BOW,
                    List.of()
            ));
            case CLERIC -> Optional.of(EquippedCombatItem.weapon(
                    "openworld_rpg:initiate_staff",
                    STARTER_ITEM_LEVEL,
                    ProjectWeaponFamily.STAFF,
                    List.of()
            ));
            case MAGE -> Optional.of(EquippedCombatItem.weapon(
                    "openworld_rpg:initiate_wand",
                    STARTER_ITEM_LEVEL,
                    ProjectWeaponFamily.WAND,
                    List.of()
            ));
        };
    }
}
