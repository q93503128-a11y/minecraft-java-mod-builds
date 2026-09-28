package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongBossLootPlanState;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongBossLootRules;
import org.junit.jupiter.api.Test;

class R01EarthloongBossLootPlanStateTest {
    @Test
    void normalPoolMatchesCanonicalEqualWeightSourceSet() {
        assertEquals(6, R01EarthloongBossLootRules.NORMAL_BASE_POOL.size());
        assertTrue(R01EarthloongBossLootRules.NORMAL_BASE_POOL.contains(
                R01EarthloongBossLootRules.NormalBase.QUARRY_MAUL
        ));
        assertTrue(R01EarthloongBossLootRules.NORMAL_BASE_POOL.contains(
                R01EarthloongBossLootRules.NormalBase.WATCH_BUCKLER
        ));
        assertTrue(R01EarthloongBossLootRules.NORMAL_BASE_POOL.contains(
                R01EarthloongBossLootRules.NormalBase.IRONBOUND_GUARD
        ));
        assertTrue(R01EarthloongBossLootRules.NORMAL_BASE_POOL.contains(
                R01EarthloongBossLootRules.NormalBase.INITIATE_STAFF
        ));
        assertTrue(R01EarthloongBossLootRules.NORMAL_BASE_POOL.contains(
                R01EarthloongBossLootRules.NormalBase.APPRENTICE_FOCUS
        ));
        assertTrue(R01EarthloongBossLootRules.NORMAL_BASE_POOL.contains(
                R01EarthloongBossLootRules.NormalBase.QUARRY_SEAL
        ));
    }

    @Test
    void guaranteedSuperiorPlusUsesNormalizedDungeonBossWeights() {
        var superior = R01EarthloongBossLootRules.createPlan(
                0, 44, 0, 99, 0
        );
        var exalted = R01EarthloongBossLootRules.createPlan(
                0, 45, 0, 99, 0
        );

        assertEquals(ProjectItemGrade.SUPERIOR, superior.minimumNormalGrade());
        assertEquals(ProjectItemGrade.SUPERIOR, superior.resolvedNormalGrade());
        assertEquals(ProjectItemGrade.EXALTED, exalted.resolvedNormalGrade());
        assertEquals(
                65,
                R01EarthloongBossLootRules.SUPERIOR_PLUS_WEIGHT_TOTAL
        );
    }

    @Test
    void ironboundGuardResolvesOneOfFiveArmorSlots() {
        var head = R01EarthloongBossLootRules.createPlan(
                2, 0, 0, 99, 0
        );
        var boots = R01EarthloongBossLootRules.createPlan(
                2, 0, 4, 99, 0
        );
        var weapon = R01EarthloongBossLootRules.createPlan(
                0, 0, 3, 99, 0
        );

        assertEquals(
                ProjectEquipmentSlot.HEAD,
                head.normalArmorSlot().orElseThrow()
        );
        assertEquals(
                ProjectEquipmentSlot.BOOTS,
                boots.normalArmorSlot().orElseThrow()
        );
        assertTrue(weapon.normalArmorSlot().isEmpty());
    }

    @Test
    void signatureThresholdIsExactFifteenPercentAndPoolIsEqualTwoItemSet() {
        var hit = R01EarthloongBossLootRules.createPlan(
                0, 0, 0, 14, 1
        );
        var miss = R01EarthloongBossLootRules.createPlan(
                0, 0, 0, 15, 0
        );

        assertEquals(
                R01EarthloongBossLootRules.MythicBase.EARTHSCALE_WARD,
                hit.mythicDrop().orElseThrow()
        );
        assertTrue(miss.mythicDrop().isEmpty());
        assertEquals(2, R01EarthloongBossLootRules.MYTHIC_POOL.size());
    }

    @Test
    void persistedPlanCannotBeRerolled() {
        var first = R01EarthloongBossLootRules.createPlan(
                2, 10, 1, 99, 0
        );
        var other = R01EarthloongBossLootRules.createPlan(
                4, 55, 3, 4, 1
        );
        var state = R01EarthloongBossLootPlanState.initial()
                .withFirstClearPlan(first);

        assertEquals(
                first,
                state.withFirstClearPlan(first)
                        .firstClearPlan().orElseThrow()
        );
        assertThrows(
                IllegalStateException.class,
                () -> state.withFirstClearPlan(other)
        );
    }

    @Test
    void legacyPlanDecodesWithoutRerollingExistingOutcome() {
        var legacyJson = JsonParser.parseString(
                """
                {
                  "earthloong_loot_plan_schema_version": 1,
                  "first_clear_plan": {
                    "normal_base": "ironbound_guard",
                    "minimum_normal_grade": "superior",
                    "signature_roll_percent": 99
                  }
                }
                """
        );

        var decoded = R01EarthloongBossLootPlanState.CODEC
                .parse(JsonOps.INSTANCE, legacyJson)
                .getOrThrow();
        var plan = decoded.firstClearPlan().orElseThrow();

        assertEquals(ProjectItemGrade.SUPERIOR, plan.minimumNormalGrade());
        assertEquals(ProjectItemGrade.SUPERIOR, plan.resolvedNormalGrade());
        assertTrue(plan.requiresArmorSlotResolution());
        assertTrue(plan.normalArmorSlot().isEmpty());

        var completed = plan.withNormalArmorSlot(ProjectEquipmentSlot.CHEST);
        assertFalse(completed.requiresArmorSlotResolution());
        assertEquals(
                ProjectEquipmentSlot.CHEST,
                completed.normalArmorSlot().orElseThrow()
        );
    }

    @Test
    void planSurvivesCodecRoundTrip() {
        var original = R01EarthloongBossLootPlanState.initial()
                .withFirstClearPlan(
                        R01EarthloongBossLootRules.createPlan(
                                5, 64, 2, 3, 0
                        )
                );

        var encoded = R01EarthloongBossLootPlanState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01EarthloongBossLootPlanState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
