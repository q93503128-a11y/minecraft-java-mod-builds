package dev.moonseungjun.openworldrpg.progression.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.List;
import org.junit.jupiter.api.Test;

class R01RegalhartRewardRulesTest {
    @Test
    void canonicalPoolAndRewardConstantsAreLocked() {
        assertEquals(8, R01RegalhartRewardRules.CONTENT_LEVEL);
        assertEquals(0.20, R01RegalhartRewardRules.FIRST_COMBAT_XP_FRACTION);
        assertEquals(0.15, R01RegalhartRewardRules.FIRST_CLASS_XP_FRACTION);
        assertEquals(70L, R01RegalhartRewardRules.FIRST_GOLD);
        assertEquals(2, R01RegalhartRewardRules.FIRST_ANTLER_QUANTITY);
        assertEquals(0.07, R01RegalhartRewardRules.REPEAT_COMBAT_XP_FRACTION);
        assertEquals(0.06, R01RegalhartRewardRules.REPEAT_CLASS_XP_FRACTION);
        assertEquals(35L, R01RegalhartRewardRules.REPEAT_GOLD);
        assertEquals(1, R01RegalhartRewardRules.REPEAT_ANTLER_QUANTITY);
        assertEquals(25, R01RegalhartRewardRules.REPEAT_SECOND_EQUIPMENT_PERCENT);
        assertEquals(15, R01RegalhartRewardRules.SIGNATURE_DROP_PERCENT);
        assertEquals(
                List.of(
                        "openworld_rpg:river_pike",
                        "openworld_rpg:riverwood_bow",
                        "openworld_rpg:wayfarer_daggers",
                        "openworld_rpg:wayfarer_leathers",
                        "openworld_rpg:greenwater_pendant",
                        "openworld_rpg:wayfarers_token"
                ),
                R01RegalhartRewardRules.NORMAL_BASE_POOL.stream()
                        .map(R01RegalhartRewardRules.NormalBase::baseId)
                        .toList()
        );
    }

    @Test
    void firstEligibleDefeatUsesExactFieldBossSuperiorPlusWeights() {
        var superior = R01RegalhartRewardRules.createPlan(
                true, 0, 39, 30, 0, 1L,
                0, 1, 0, 30, 0, 2L, 99
        );
        var exalted = R01RegalhartRewardRules.createPlan(
                true, 0, 40, 30, 0, 1L,
                0, 1, 0, 30, 0, 2L, 99
        );

        assertEquals(ProjectItemGrade.SUPERIOR, superior.guaranteedEquipment().grade());
        assertEquals(ProjectItemGrade.EXALTED, exalted.guaranteedEquipment().grade());
        assertTrue(superior.secondEquipment().isEmpty());
        assertEquals(2, superior.antlerQuantity());
        assertEquals(0.20, superior.combatXpFraction());
        assertEquals(0.15, superior.classXpFraction());
        assertEquals(70L, superior.gold());
    }

    @Test
    void repeatUsesFieldBossGradeTableAndExactSecondRollBoundary() {
        assertEquals(ProjectItemGrade.STANDARD, R01RegalhartRewardRules.fieldBossGrade(0));
        assertEquals(ProjectItemGrade.STANDARD, R01RegalhartRewardRules.fieldBossGrade(9));
        assertEquals(ProjectItemGrade.REFINED, R01RegalhartRewardRules.fieldBossGrade(10));
        assertEquals(ProjectItemGrade.REFINED, R01RegalhartRewardRules.fieldBossGrade(44));
        assertEquals(ProjectItemGrade.SUPERIOR, R01RegalhartRewardRules.fieldBossGrade(45));
        assertEquals(ProjectItemGrade.SUPERIOR, R01RegalhartRewardRules.fieldBossGrade(84));
        assertEquals(ProjectItemGrade.EXALTED, R01RegalhartRewardRules.fieldBossGrade(85));
        assertEquals(ProjectItemGrade.EXALTED, R01RegalhartRewardRules.fieldBossGrade(99));

        var secondHit = R01RegalhartRewardRules.createPlan(
                false, 0, 0, 30, 0, 1L,
                24, 1, 99, 90, 0, 2L, 99
        );
        var secondMiss = R01RegalhartRewardRules.createPlan(
                false, 0, 0, 30, 0, 1L,
                25, 1, 99, 90, 0, 2L, 99
        );

        assertTrue(secondHit.secondEquipment().isPresent());
        assertEquals(
                ProjectItemGrade.EXALTED,
                secondHit.secondEquipment().orElseThrow().grade()
        );
        assertTrue(secondMiss.secondEquipment().isEmpty());
        assertEquals(1, secondHit.antlerQuantity());
        assertEquals(0.07, secondHit.combatXpFraction());
        assertEquals(0.06, secondHit.classXpFraction());
        assertEquals(35L, secondHit.gold());
    }

    @Test
    void signatureAndItemLevelBoundariesAreExact() {
        var hit = R01RegalhartRewardRules.createPlan(
                false, 0, 0, 0, 0, 1L,
                99, 1, 0, 0, 0, 2L, 14
        );
        var miss = R01RegalhartRewardRules.createPlan(
                false, 0, 0, 99, 0, 1L,
                99, 1, 0, 99, 0, 2L, 15
        );
        assertTrue(hit.hartcrownSpearDrop());
        assertFalse(miss.hartcrownSpearDrop());

        assertEquals(6, R01RegalhartRewardRules.itemLevel(0));
        assertEquals(7, R01RegalhartRewardRules.itemLevel(10));
        assertEquals(8, R01RegalhartRewardRules.itemLevel(30));
        assertEquals(9, R01RegalhartRewardRules.itemLevel(70));
        assertEquals(10, R01RegalhartRewardRules.itemLevel(90));
    }

    @Test
    void wayfarerLeathersResolvesExactlyOneArmorSlot() {
        int baseIndex = R01RegalhartRewardRules.NORMAL_BASE_POOL.indexOf(
                R01RegalhartRewardRules.NormalBase.WAYFARER_LEATHERS
        );
        int slotIndex = R01RegalhartRewardRules.ARMOR_SLOTS.indexOf(
                ProjectEquipmentSlot.GLOVES
        );
        var plan = R01RegalhartRewardRules.createPlan(
                false, baseIndex, 45, 30, slotIndex, 123L,
                99, 0, 0, 30, 0, 456L, 99
        );

        assertEquals(
                ProjectEquipmentSlot.GLOVES,
                plan.guaranteedEquipment().armorSlot().orElseThrow()
        );
    }
}
