package dev.moonseungjun.openworldrpg.progression.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.camp.R01CampRules;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import org.junit.jupiter.api.Test;

class R01OrdinaryEnemyRewardRulesTest {
    private static R01OrdinaryEnemyRewardRules.Rolls rolls(
            int gold, int primary, int secondary, int quantity, int equipment,
            int base, int grade, int itemLevel, int armor
    ) {
        return new R01OrdinaryEnemyRewardRules.Rolls(
                gold, primary, secondary, quantity, equipment,
                base, grade, itemLevel, armor, 1234L
        );
    }

    @Test
    void globalCommonAndEliteProgressionTargetsAreBoundPerSourceLevel() {
        assertEquals(0.01, R01OrdinaryEnemyRewardRules.EnemySource.BISON.combatXpFraction());
        assertEquals(0.008, R01OrdinaryEnemyRewardRules.EnemySource.GRIZZLY.classXpFraction());
        assertEquals(0.06, R01OrdinaryEnemyRewardRules.EnemySource.STEELBOAR.combatXpFraction());
        assertEquals(0.05, R01OrdinaryEnemyRewardRules.EnemySource.STEELBOAR.classXpFraction());
        assertEquals(4, R01OrdinaryEnemyRewardRules.EnemySource.CAVE_CENTIPEDE.contentLevel());
    }

    @Test
    void creatureMaterialsAndGoldMatchR01CanonAtBoundaries() {
        assertTrue(
                R01OrdinaryEnemyRewardRules.sourceForEntityId(
                        "threateningly_mobs:louxia"
                ).isEmpty()
        );
        var centipede = R01OrdinaryEnemyRewardRules.createPlan(
                R01OrdinaryEnemyRewardRules.EnemySource.CAVE_CENTIPEDE,
                rolls(69, 99, 99, 0, 99, 0, 0, 0, 0)
        );
        assertEquals(3L, centipede.gold());
        assertTrue(centipede.materials().isEmpty());

        var bison = R01OrdinaryEnemyRewardRules.createPlan(
                R01OrdinaryEnemyRewardRules.EnemySource.BISON,
                rolls(99, 69, 99, 1, 99, 0, 0, 0, 0)
        );
        assertEquals(2, bison.materials().get(R01CampRules.TOUGH_HIDE));

        var grizzly = R01OrdinaryEnemyRewardRules.createPlan(
                R01OrdinaryEnemyRewardRules.EnemySource.GRIZZLY,
                rolls(99, 99, 99, 1, 99, 0, 0, 0, 0)
        );
        assertEquals(3, grizzly.materials().get(R01CampRules.TOUGH_HIDE));

        var steelboar = R01OrdinaryEnemyRewardRules.createPlan(
                R01OrdinaryEnemyRewardRules.EnemySource.STEELBOAR,
                rolls(99, 59, 34, 2, 99, 0, 0, 0, 0)
        );
        assertEquals(18L, steelboar.gold());
        assertEquals(3, steelboar.materials().get(R01GatheringRules.IRON_ORE));
        assertEquals(1, steelboar.materials().get(R01CampRules.TOUGH_HIDE));
    }

    @Test
    void steelboarEliteEquipmentRollCommitsFullFutureMaterializationPlan() {
        int armorBase = R01OrdinaryEnemyRewardRules.STEELBOAR_EQUIPMENT_POOL.indexOf(
                "openworld_rpg:ironbound_guard"
        );
        var plan = R01OrdinaryEnemyRewardRules.createPlan(
                R01OrdinaryEnemyRewardRules.EnemySource.STEELBOAR,
                rolls(99, 99, 99, 0, 29, armorBase, 98, 90, 3)
        );
        var equipment = plan.equipment().orElseThrow();
        assertEquals(ProjectItemGrade.EXALTED, equipment.grade());
        assertEquals(8, equipment.itemLevel());
        assertEquals(ProjectEquipmentSlot.GLOVES, equipment.armorSlot().orElseThrow());

        assertEquals(ProjectItemGrade.STANDARD, R01OrdinaryEnemyRewardRules.eliteGrade(49));
        assertEquals(ProjectItemGrade.REFINED, R01OrdinaryEnemyRewardRules.eliteGrade(50));
        assertEquals(ProjectItemGrade.SUPERIOR, R01OrdinaryEnemyRewardRules.eliteGrade(85));
        assertEquals(ProjectItemGrade.EXALTED, R01OrdinaryEnemyRewardRules.eliteGrade(98));

        assertEquals(4, R01OrdinaryEnemyRewardRules.itemLevel(6, 0));
        assertEquals(5, R01OrdinaryEnemyRewardRules.itemLevel(6, 10));
        assertEquals(6, R01OrdinaryEnemyRewardRules.itemLevel(6, 30));
        assertEquals(7, R01OrdinaryEnemyRewardRules.itemLevel(6, 70));
        assertEquals(8, R01OrdinaryEnemyRewardRules.itemLevel(6, 90));
    }
}