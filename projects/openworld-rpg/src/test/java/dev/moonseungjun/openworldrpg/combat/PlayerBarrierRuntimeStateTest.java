package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.PlayerBarrierRuntimeState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerBarrierRuntimeStateTest {
    private static final double EPSILON = 0.0001;

    @Test
    void sameSourceReplacesWhileDifferentSourcesShareFortyPercentCap() {
        var state = new PlayerBarrierRuntimeState();
        UUID source = UUID.randomUUID();

        var first = state.grant(
                "openworld_rpg:first",
                source,
                30.0,
                100.0,
                120,
                false,
                10L
        );
        assertEquals(30.0, first.appliedAmount(), EPSILON);
        assertEquals(30.0, first.effectiveGranted(), EPSILON);

        var replacement = state.grant(
                "openworld_rpg:first",
                source,
                25.0,
                100.0,
                120,
                false,
                20L
        );
        assertEquals(25.0, replacement.appliedAmount(), EPSILON);
        assertEquals(0.0, replacement.effectiveGranted(), EPSILON);
        assertEquals(25.0, replacement.totalBarrierAfter(), EPSILON);

        var second = state.grant(
                "openworld_rpg:second",
                source,
                30.0,
                100.0,
                120,
                false,
                21L
        );
        assertEquals(15.0, second.appliedAmount(), EPSILON);
        assertEquals(40.0, second.totalBarrierAfter(), EPSILON);
    }

    @Test
    void layersExpireAtExactBoundaryAndSoonestExpiryAbsorbsFirst() {
        var state = new PlayerBarrierRuntimeState();
        UUID source = UUID.randomUUID();

        state.grant(
                "openworld_rpg:long",
                source,
                20.0,
                100.0,
                200,
                false,
                0L
        );
        state.grant(
                "openworld_rpg:short",
                source,
                10.0,
                100.0,
                40,
                false,
                0L
        );

        var absorbed = state.absorbHostileDamage(
                15.0,
                100.0,
                10L
        );
        assertEquals(0.0, absorbed.remainingDamage(), EPSILON);
        assertEquals(15.0, absorbed.absorbedDamage(), EPSILON);
        assertEquals(
                0.0,
                state.layerAmount(
                        "openworld_rpg:short",
                        10L
                ),
                EPSILON
        );
        assertEquals(
                15.0,
                state.layerAmount(
                        "openworld_rpg:long",
                        10L
                ),
                EPSILON
        );

        assertEquals(15.0, state.totalBarrier(100.0, 199L), EPSILON);
        assertEquals(0.0, state.totalBarrier(100.0, 200L), EPSILON);
    }

    @Test
    void clericBarrierSignalsGraceOnlyAfterSixPercentHasActuallyBeenConsumed() {
        var state = new PlayerBarrierRuntimeState();
        UUID cleric = UUID.randomUUID();

        state.grant(
                "openworld_rpg:cleric_barrier",
                cleric,
                20.0,
                100.0,
                120,
                true,
                0L
        );

        var first = state.absorbHostileDamage(
                5.0,
                100.0,
                1L
        );
        assertFalse(
                first.sourceConsumptions()
                        .getFirst()
                        .clericGraceThresholdReached()
        );
        assertEquals(
                1,
                first.sourceConsumptions()
                        .getFirst()
                        .clericUltimateChargeStepsReached()
        );

        var second = state.absorbHostileDamage(
                1.0,
                100.0,
                2L
        );
        assertTrue(
                second.sourceConsumptions()
                        .getFirst()
                        .clericGraceThresholdReached()
        );
        assertEquals(
                0,
                second.sourceConsumptions()
                        .getFirst()
                        .clericUltimateChargeStepsReached()
        );

        var third = state.absorbHostileDamage(
                4.0,
                100.0,
                3L
        );
        assertFalse(
                third.sourceConsumptions()
                        .getFirst()
                        .clericGraceThresholdReached()
        );
        assertEquals(
                1,
                third.sourceConsumptions()
                        .getFirst()
                        .clericUltimateChargeStepsReached()
        );

        var fourth = state.absorbHostileDamage(
                5.0,
                100.0,
                4L
        );
        assertEquals(
                1,
                fourth.sourceConsumptions()
                        .getFirst()
                        .clericUltimateChargeStepsReached()
        );

        var capped = state.absorbHostileDamage(
                2.0,
                100.0,
                5L
        );
        assertEquals(
                0,
                capped.sourceConsumptions()
                        .getFirst()
                        .clericUltimateChargeStepsReached()
        );
    }

    @Test
    void guardianResolveOwnershipIsSnapshottedAtBarrierGrant() {
        var state = new PlayerBarrierRuntimeState();
        UUID guardian = UUID.randomUUID();

        state.grant(
                "openworld_rpg:guardian_barrier",
                guardian,
                20.0,
                100.0,
                120,
                false,
                true,
                0L
        );

        var absorbed = state.absorbHostileDamage(
                8.0,
                100.0,
                1L
        );
        assertTrue(
                absorbed.sourceConsumptions()
                        .getFirst()
                        .guardianResolveSource()
        );

        state.grant(
                "openworld_rpg:ordinary_barrier",
                guardian,
                10.0,
                100.0,
                120,
                false,
                2L
        );
        var ordinary = state.absorbHostileDamage(
                25.0,
                100.0,
                3L
        );
        assertFalse(
                ordinary.sourceConsumptions()
                        .stream()
                        .filter(consumption -> consumption.sourceId()
                                .equals("openworld_rpg:ordinary_barrier"))
                        .findFirst()
                        .orElseThrow()
                        .guardianResolveSource()
        );
    }

    @Test
    void maxHpDecreaseTrimsPoolBackToCurrentFortyPercentCap() {
        var state = new PlayerBarrierRuntimeState();
        UUID source = UUID.randomUUID();

        state.grant(
                "openworld_rpg:a",
                source,
                20.0,
                100.0,
                120,
                false,
                0L
        );
        state.grant(
                "openworld_rpg:b",
                source,
                20.0,
                100.0,
                120,
                false,
                0L
        );
        assertEquals(40.0, state.totalBarrier(100.0, 1L), EPSILON);
        assertEquals(20.0, state.totalBarrier(50.0, 2L), EPSILON);
    }
}
