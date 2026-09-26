package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatAffix;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatAffixKind;
import dev.moonseungjun.openworldrpg.combat.state.EquippedCombatItem;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.List;
import java.util.Optional;

/** Exact server payloads for the R01 Earthloong deterministic first-clear choice. */
public enum R01EarthloongRewardChoice {
    IRONROOT_LONGSWORD(
            "openworld_rpg:r01/earthloong_choice/ironroot_longsword",
            EquippedCombatItem.weapon(
                    "openworld_rpg:ironroot_longsword",
                    8,
                    ProjectWeaponFamily.SWORD,
                    List.of(
                            EquipmentCombatAffix.flat(EquipmentCombatAffixKind.STR, 3.0),
                            EquipmentCombatAffix.flat(
                                    EquipmentCombatAffixKind.PHYSICAL_POWER,
                                    0.07
                            ),
                            EquipmentCombatAffix.flat(
                                    EquipmentCombatAffixKind.GUARD_STRENGTH,
                                    0.135
                            )
                    )
            )
    ),
    RIVERTHORN_BOW(
            "openworld_rpg:r01/earthloong_choice/riverthorn_bow",
            EquippedCombatItem.weapon(
                    "openworld_rpg:riverthorn_bow",
                    8,
                    ProjectWeaponFamily.BOW,
                    List.of(
                            EquipmentCombatAffix.flat(EquipmentCombatAffixKind.DEX, 3.0),
                            EquipmentCombatAffix.flat(
                                    EquipmentCombatAffixKind.CRITICAL_CHANCE,
                                    0.031
                            ),
                            EquipmentCombatAffix.flat(
                                    EquipmentCombatAffixKind.ATTACK_SPEED,
                                    0.055
                            )
                    )
            )
    ),
    LUMENWOOD_STAFF(
            "openworld_rpg:r01/earthloong_choice/lumenwood_staff",
            EquippedCombatItem.weapon(
                    "openworld_rpg:lumenwood_staff",
                    8,
                    ProjectWeaponFamily.STAFF,
                    List.of(
                            EquipmentCombatAffix.flat(EquipmentCombatAffixKind.INT, 3.0),
                            EquipmentCombatAffix.flat(
                                    EquipmentCombatAffixKind.MAX_MANA,
                                    0.08
                            ),
                            EquipmentCombatAffix.flat(
                                    EquipmentCombatAffixKind.MAGIC_POWER,
                                    0.07
                            )
                    )
            )
    );

    public static final int ITEM_LEVEL = 8;
    public static final long R01_SUPERIOR_WEAPON_BASE_PRICE = 500L;
    public static final long SELL_VALUE =
            R01_SUPERIOR_WEAPON_BASE_PRICE * 25L / 100L;

    private final String choiceFlag;
    private final EquippedCombatItem equipment;

    R01EarthloongRewardChoice(
            String choiceFlag,
            EquippedCombatItem equipment
    ) {
        this.choiceFlag = choiceFlag;
        this.equipment = equipment;
    }

    public String choiceFlag() {
        return choiceFlag;
    }

    public EquippedCombatItem equipment() {
        return equipment;
    }

    public ProjectInventoryItem inventoryItem() {
        return ProjectInventoryItem.equipment(
                equipment,
                ProjectItemGrade.SUPERIOR,
                SELL_VALUE
        );
    }

    public static Optional<R01EarthloongRewardChoice> fromChoiceFlag(String flag) {
        for (R01EarthloongRewardChoice value : values()) {
            if (value.choiceFlag.equals(flag)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
