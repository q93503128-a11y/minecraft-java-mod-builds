package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
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
    void signatureThresholdIsExactFifteenPercentAndPoolIsEqualTwoItemSet() {
        var hit = R01EarthloongBossLootRules.createPlan(0, 14, 1);
        var miss = R01EarthloongBossLootRules.createPlan(0, 15, 0);

        assertEquals(ProjectItemGrade.SUPERIOR, hit.minimumNormalGrade());
        assertEquals(
                R01EarthloongBossLootRules.MythicBase.EARTHSCALE_WARD,
                hit.mythicDrop().orElseThrow()
        );
        assertTrue(miss.mythicDrop().isEmpty());
        assertEquals(2, R01EarthloongBossLootRules.MYTHIC_POOL.size());
    }

    @Test
    void persistedPlanCannotBeRerolled() {
        var first = R01EarthloongBossLootRules.createPlan(2, 99, 0);
        var other = R01EarthloongBossLootRules.createPlan(4, 4, 1);
        var state = R01EarthloongBossLootPlanState.initial()
                .withFirstClearPlan(first);

        assertEquals(first, state.withFirstClearPlan(first)
                .firstClearPlan().orElseThrow());
        assertThrows(
                IllegalStateException.class,
                () -> state.withFirstClearPlan(other)
        );
    }

    @Test
    void planSurvivesCodecRoundTrip() {
        var original = R01EarthloongBossLootPlanState.initial()
                .withFirstClearPlan(
                        R01EarthloongBossLootRules.createPlan(5, 3, 0)
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
