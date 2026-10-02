package dev.moonseungjun.openworldrpg.progression.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.List;
import org.junit.jupiter.api.Test;

class R01NatureSpiritRewardRulesTest {
    @Test
    void canonicalNatureSpiritPoolAndDropRatesAreLocked() {
        assertEquals(7, R01NatureSpiritRewardRules.CONTENT_LEVEL);
        assertEquals(30, R01NatureSpiritRewardRules.EQUIPMENT_DROP_PERCENT);
        assertEquals(60, R01NatureSpiritRewardRules.HEALING_HERB_DROP_PERCENT);
        assertEquals(35, R01NatureSpiritRewardRules.VERDANT_CRYSTAL_DROP_PERCENT);
        assertEquals(
                List.of(
                        "openworld_rpg:initiate_staff",
                        "openworld_rpg:initiate_wand",
                        "openworld_rpg:apprentice_focus",
                        "openworld_rpg:river_scholar_garb",
                        "openworld_rpg:greenwater_pendant"
                ),
                R01NatureSpiritRewardRules.EQUIPMENT_POOL.stream()
                        .map(R01NatureSpiritRewardRules.NormalBase::baseId)
                        .toList()
        );
    }

    @Test
    void eliteGradeAndItemLevelBoundariesMatchGlobalLootCanon() {
        assertEquals(ProjectItemGrade.STANDARD, R01NatureSpiritRewardRules.grade(0));
        assertEquals(ProjectItemGrade.STANDARD, R01NatureSpiritRewardRules.grade(49));
        assertEquals(ProjectItemGrade.REFINED, R01NatureSpiritRewardRules.grade(50));
        assertEquals(ProjectItemGrade.REFINED, R01NatureSpiritRewardRules.grade(84));
        assertEquals(ProjectItemGrade.SUPERIOR, R01NatureSpiritRewardRules.grade(85));
        assertEquals(ProjectItemGrade.SUPERIOR, R01NatureSpiritRewardRules.grade(97));
        assertEquals(ProjectItemGrade.EXALTED, R01NatureSpiritRewardRules.grade(98));
        assertEquals(ProjectItemGrade.EXALTED, R01NatureSpiritRewardRules.grade(99));

        assertEquals(5, R01NatureSpiritRewardRules.itemLevel(0));
        assertEquals(6, R01NatureSpiritRewardRules.itemLevel(10));
        assertEquals(7, R01NatureSpiritRewardRules.itemLevel(30));
        assertEquals(8, R01NatureSpiritRewardRules.itemLevel(70));
        assertEquals(9, R01NatureSpiritRewardRules.itemLevel(90));
    }

    @Test
    void materialRangeIsUniformOneOrTwoAndArmorFamilyResolvesOneSlot() {
        var oneHerb = R01NatureSpiritRewardRules.createPlan(
                99, 0, 0, 0, 0, 1L,
                0, 0, 99
        );
        assertEquals(1, oneHerb.healingHerbQuantity());
        assertEquals(0, oneHerb.verdantCrystalQuantity());
        assertTrue(oneHerb.equipment().isEmpty());

        var twoHerbsAndCrystal = R01NatureSpiritRewardRules.createPlan(
                0,
                R01NatureSpiritRewardRules.EQUIPMENT_POOL.indexOf(
                        R01NatureSpiritRewardRules.NormalBase.RIVER_SCHOLAR_GARB
                ),
                98,
                90,
                R01NatureSpiritRewardRules.ARMOR_SLOTS.indexOf(
                        ProjectEquipmentSlot.GLOVES
                ),
                123L,
                59,
                1,
                34
        );
        assertEquals(2, twoHerbsAndCrystal.healingHerbQuantity());
        assertEquals(1, twoHerbsAndCrystal.verdantCrystalQuantity());
        var gear = twoHerbsAndCrystal.equipment().orElseThrow();
        assertEquals(
                R01NatureSpiritRewardRules.NormalBase.RIVER_SCHOLAR_GARB,
                gear.base()
        );
        assertEquals(ProjectItemGrade.EXALTED, gear.grade());
        assertEquals(9, gear.itemLevel());
        assertEquals(ProjectEquipmentSlot.GLOVES, gear.armorSlot().orElseThrow());

        var noDropAtBoundary = R01NatureSpiritRewardRules.createPlan(
                30, 0, 0, 0, 0, 1L,
                60, 0, 35
        );
        assertTrue(noDropAtBoundary.equipment().isEmpty());
        assertEquals(0, noDropAtBoundary.healingHerbQuantity());
        assertEquals(0, noDropAtBoundary.verdantCrystalQuantity());
    }
}
