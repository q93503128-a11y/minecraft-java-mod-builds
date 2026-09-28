package dev.moonseungjun.openworldrpg.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01FishingAuthorityTest {
    private static final String PLAYER =
            UUID.fromString("11111111-2222-3333-4444-555555555555").toString();

    @Test
    void spatialRosterIsExactlyFivePlusOnePlusOneAndStillCandidateOnly() {
        var spots = R01FishingSpatialRegistry.allAuthoredSpots();

        assertEquals(7, spots.size());
        assertEquals(
                5,
                spots.stream()
                        .filter(value -> value.tier() == R01FishingRules.SpotTier.ORDINARY)
                        .count()
        );
        assertEquals(
                1,
                spots.stream()
                        .filter(value -> value.tier() == R01FishingRules.SpotTier.UNCOMMON)
                        .count()
        );
        assertEquals(
                1,
                spots.stream()
                        .filter(value -> value.tier() == R01FishingRules.SpotTier.RARE)
                        .count()
        );
        assertFalse(R01FishingSpatialRegistry.productionReady());
        for (var spot : spots) {
            assertEquals("candidate", spot.anchor().status());
            assertTrue(
                    R01FishingSpatialRegistry
                            .productionSpot(spot.spotId())
                            .isEmpty()
            );
        }
    }

    @Test
    void speciesPoolBoundariesMatchCanonExactly() {
        assertEquals(
                R01FishingRules.COMMON_A,
                R01FishingRules.speciesForRoll(
                        R01FishingRules.SpotTier.ORDINARY, 54
                )
        );
        assertEquals(
                R01FishingRules.COMMON_B,
                R01FishingRules.speciesForRoll(
                        R01FishingRules.SpotTier.ORDINARY, 55
                )
        );
        assertEquals(
                R01FishingRules.POOL_UNCOMMON,
                R01FishingRules.speciesForRoll(
                        R01FishingRules.SpotTier.ORDINARY, 90
                )
        );

        assertEquals(
                R01FishingRules.FORD_RARE,
                R01FishingRules.speciesForRoll(
                        R01FishingRules.SpotTier.UNCOMMON, 95
                )
        );
        assertEquals(
                R01FishingRules.FORD_RARE,
                R01FishingRules.speciesForRoll(
                        R01FishingRules.SpotTier.RARE, 65
                )
        );
    }

    @Test
    void deterministicCandidateCannotBeRerolledByRepeatPreparationOrMiss() {
        String spot = R01FishingSpatialRegistry.ORDINARY_SPOTS.get(0);
        R01FishingState initial = R01FishingState.initial();

        var first = initial.prepareCandidate(
                99887766L,
                PLAYER,
                spot,
                R01FishingRules.SpotTier.ORDINARY,
                100L,
                1
        );
        var repeated = first.state().prepareCandidate(
                99887766L,
                PLAYER,
                spot,
                R01FishingRules.SpotTier.ORDINARY,
                120L,
                1
        );

        assertEquals(first.pending(), repeated.pending());
        assertEquals(first.state(), first.state().missHook(spot));
        assertEquals(
                first.pending(),
                first.state().missHook(spot).pendingCandidate(spot).orElseThrow()
        );
    }

    @Test
    void hookConsumesChargeAndOrdinarySpotRespawnsAfterSixActiveMinutes() {
        String spot = R01FishingSpatialRegistry.ORDINARY_SPOTS.get(1);
        R01FishingState state = R01FishingState.initial();

        state = state.prepareCandidate(
                123L, PLAYER, spot,
                R01FishingRules.SpotTier.ORDINARY,
                100L, 1
        ).state();
        state = state.commitHook(
                spot,
                R01FishingRules.SpotTier.ORDINARY,
                100L
        );
        assertEquals(1, state.spotCycles().get(spot).consumedCharges());
        state = state.resolveHookFailure(spot);

        state = state.prepareCandidate(
                123L, PLAYER, spot,
                R01FishingRules.SpotTier.ORDINARY,
                200L, 1
        ).state();
        state = state.commitHook(
                spot,
                R01FishingRules.SpotTier.ORDINARY,
                200L
        );
        state = state.resolveHookFailure(spot);

        assertEquals(2, state.spotCycles().get(spot).consumedCharges());
        assertEquals(
                200L + 6L * 60L * 20L,
                state.spotCycles().get(spot).availableAfterActiveTick()
        );
        assertFalse(
                state.isSpotAvailable(
                        spot,
                        R01FishingRules.SpotTier.ORDINARY,
                        200L + 6L * 60L * 20L - 1L
                )
        );
        assertTrue(
                state.isSpotAvailable(
                        spot,
                        R01FishingRules.SpotTier.ORDINARY,
                        200L + 6L * 60L * 20L
                )
        );

        var nextCycle = state.prepareCandidate(
                123L, PLAYER, spot,
                R01FishingRules.SpotTier.ORDINARY,
                200L + 6L * 60L * 20L,
                1
        );
        assertEquals(1L, nextCycle.pending().cycleIndex());
        assertEquals(0, nextCycle.pending().catchOrdinal());
        assertEquals(0, nextCycle.state().spotCycles().get(spot).consumedCharges());
    }

    @Test
    void interruptedHookLosesCatchButNeverRefundsConsumedCharge() {
        String spot = R01FishingSpatialRegistry.UNCOMMON_SPOT;
        R01FishingState state = R01FishingState.initial()
                .prepareCandidate(
                        55L, PLAYER, spot,
                        R01FishingRules.SpotTier.UNCOMMON,
                        500L, 2
                ).state()
                .commitHook(
                        spot,
                        R01FishingRules.SpotTier.UNCOMMON,
                        500L
                );

        assertTrue(state.pendingCandidate(spot).orElseThrow().hooked());
        assertEquals(1, state.spotCycles().get(spot).consumedCharges());

        state = state.reconcileInterruptedHooks();

        assertTrue(state.pendingCandidate(spot).isEmpty());
        assertEquals(1, state.spotCycles().get(spot).consumedCharges());
        assertEquals(1, state.spotCycles().get(spot).catchOrdinal());
    }

    @Test
    void successfulCatchBecomesReconnectSafeSemanticRewardPlan() {
        String spot = R01FishingSpatialRegistry.RARE_SPOT;
        R01FishingState state = R01FishingState.initial()
                .prepareCandidate(
                        999L, PLAYER, spot,
                        R01FishingRules.SpotTier.RARE,
                        800L, 5
                ).state()
                .commitHook(
                        spot,
                        R01FishingRules.SpotTier.RARE,
                        800L
                );

        var resolution = state.resolveCatchSuccess(spot);
        var reward = resolution.reward().orElseThrow();

        assertTrue(resolution.state().pendingCandidate(spot).isEmpty());
        assertEquals(1, resolution.state().spotCycles().get(spot).consumedCharges());
        assertEquals(1, resolution.state().spotCycles().get(spot).catchOrdinal());
        assertEquals(
                reward,
                resolution.state()
                        .pendingReward(reward.transactionId())
                        .orElseThrow()
        );
    }

    @Test
    void sizeBiteHookAndTensionRulesMatchClosedR01Numbers() {
        var common = R01FishingRules.fish(R01FishingRules.COMMON_A);
        assertEquals(16.0, R01FishingRules.sizeFromPercentile(common, 0.0), 0.0001);
        assertEquals(24.0, R01FishingRules.sizeFromPercentile(common, 0.10), 0.0001);
        assertEquals(34.0, R01FishingRules.sizeFromPercentile(common, 0.90), 0.0001);
        assertTrue(
                R01FishingRules.trophyThresholdCm(common) > 40.0
        );

        assertEquals(5.00, R01FishingRules.biteDelayMaximumSeconds(1), 0.0001);
        assertEquals(4.60, R01FishingRules.biteDelayMaximumSeconds(2), 0.0001);
        assertEquals(4.25, R01FishingRules.biteDelayMaximumSeconds(4), 0.0001);
        assertEquals(
                0.90,
                R01FishingRules.hookWindowSeconds(
                        R01FishingRules.Rarity.COMMON,
                        false,
                        1
                ),
                0.0001
        );
        assertEquals(
                0.63,
                R01FishingRules.hookWindowSeconds(
                        R01FishingRules.Rarity.COMMON,
                        true,
                        3
                ),
                0.0001
        );

        var rankOne = R01FishingRules.generateCandidate(
                222L, PLAYER, R01FishingSpatialRegistry.RARE_SPOT,
                R01FishingRules.SpotTier.RARE, 0L, 0, 1
        );
        var rankFive = R01FishingRules.generateCandidate(
                222L, PLAYER, R01FishingSpatialRegistry.RARE_SPOT,
                R01FishingRules.SpotTier.RARE, 0L, 0, 5
        );
        assertEquals(rankOne.fishSlotId(), rankFive.fishSlotId());
        assertNotEquals(-1.0, rankFive.secondarySizePercentile());
        assertTrue(rankFive.sizeCm() >= rankOne.sizeCm() - 0.0001);

        var rare = R01FishingRules.fish(R01FishingRules.FORD_RARE);
        double rareSize = 50.0;
        var tensionCandidate = new R01FishingRules.CatchCandidate(
                R01FishingRules.FORD_RARE,
                R01FishingRules.Rarity.RARE,
                0.5,
                -1.0,
                rareSize,
                false,
                R01FishingRules.saleValue(rare, rareSize),
                2.0,
                0.65
        );
        var highFailure = R01FishingRules.advanceTension(
                new R01FishingRules.TensionState(0.96, 0.0, 0.35, 0.0),
                tensionCandidate,
                true,
                1,
                0.10
        );
        assertEquals(
                R01FishingRules.TensionOutcome.FAILED,
                highFailure.outcome()
        );
    }
}
