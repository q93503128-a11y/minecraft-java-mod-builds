package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.progression.r01.R01MainStage;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerState;
import org.junit.jupiter.api.Test;

class R01PlayerStateTest {
    @Test
    void initialStateStartsAtArrivalWithSchemaOneAndNoFabricatedProgress() {
        var state = R01PlayerState.initial();

        assertEquals(1, state.schemaVersion());
        assertEquals(R01MainStage.ARRIVAL_ROAD, state.opening().mainStage());
        assertEquals(0, state.opening().quarryRoadActionBits());
        assertFalse(state.quarry().firstClear());
        assertFalse(state.quarry().rewardChoicePending());
        assertTrue(state.economy().pendingRewardClaimIds().isEmpty());
    }

    @Test
    void openingLoadoutClaimIsIndependentAndIdempotent() {
        var initial = R01PlayerState.initial();
        var claimed = initial.markOpeningLoadoutClaimed(5);
        var retried = claimed.markOpeningLoadoutClaimed(6);

        assertFalse(initial.openingLoadoutClaimed());
        assertTrue(claimed.openingLoadoutClaimed());
        assertEquals(claimed, retried);
        assertTrue(claimed.ledger().rewardClaimIds().contains(
                R01PlayerState.OPENING_LOADOUT_CLAIM_ID
        ));
        assertEquals(R01MainStage.ARRIVAL_ROAD, claimed.opening().mainStage());
    }

    @Test
    void classAndStarterPackageTogetherOpenQuarryRoadWithoutDependingOnOrder() {
        var firstClassThenStarter = R01PlayerState.initial()
                .markFirstShrineActivated(10)
                .markFirstRootClassSelected(20)
                .markStarterPackageClaimed(21);
        var starterThenClass = R01PlayerState.initial()
                .markFirstShrineActivated(10)
                .markStarterPackageClaimed(20)
                .markFirstRootClassSelected(21);

        assertEquals(R01MainStage.QUARRY_ROAD_ACTIVE, firstClassThenStarter.opening().mainStage());
        assertEquals(R01MainStage.QUARRY_ROAD_ACTIVE, starterThenClass.opening().mainStage());
        assertEquals(
                "active",
                firstClassThenStarter.ledger().questStepIds().get(
                        R01PlayerState.DUST_ON_QUARRY_ROAD_QUEST_ID
                )
        );
        assertEquals(
                "active",
                starterThenClass.ledger().questStepIds().get(
                        R01PlayerState.DUST_ON_QUARRY_ROAD_QUEST_ID
                )
        );
    }

    @Test
    void quarryRoadCreditFailsClosedBeforeMainObjectiveActivation() {
        assertThrows(
                IllegalStateException.class,
                () -> R01PlayerState.initial().recordQuarryRoadAction(
                        R01PlayerState.QuarryRoadAction.LOST_CARGO,
                        1
                )
        );
    }

    @Test
    void quarryRoadRequiresThreeDistinctUsefulActionCategories() {
        var state = R01PlayerState.initial()
                .markFirstRootClassSelected(1)
                .markStarterPackageClaimed(2)
                .recordQuarryRoadAction(R01PlayerState.QuarryRoadAction.LOST_CARGO, 3)
                .recordQuarryRoadAction(R01PlayerState.QuarryRoadAction.LOST_CARGO, 4)
                .recordQuarryRoadAction(R01PlayerState.QuarryRoadAction.BROKEN_ROAD_MARKER, 5);

        assertEquals(2, Integer.bitCount(state.opening().quarryRoadActionBits()));
        assertEquals(R01MainStage.QUARRY_ROAD_ACTIVE, state.opening().mainStage());

        state = state.recordQuarryRoadAction(
                R01PlayerState.QuarryRoadAction.R01_GATHERING_NODE,
                6
        );

        assertEquals(3, Integer.bitCount(state.opening().quarryRoadActionBits()));
        assertEquals(R01MainStage.QUARRY_ROAD_COMPLETE, state.opening().mainStage());
        assertTrue(state.ledger().completedStepIds().contains(
                "openworld_rpg:r01/dust_on_quarry_road/complete"
        ));
        assertEquals(
                "completed",
                state.ledger().questStepIds().get(
                        R01PlayerState.DUST_ON_QUARRY_ROAD_QUEST_ID
                )
        );
        assertEquals(
                "available",
                state.ledger().questStepIds().get(
                        R01PlayerState.ROOTS_BELOW_STONE_QUEST_ID
                )
        );
    }

    @Test
    void earlyQuarryDiscoveryPersistsAndReconcilesWhenRoadCompletes() {
        var state = R01PlayerState.initial()
                .markFirstRootClassSelected(1)
                .markStarterPackageClaimed(2)
                .markQuarryDiscovered(3)
                .recordQuarryRoadAction(R01PlayerState.QuarryRoadAction.LOST_CARGO, 4)
                .recordQuarryRoadAction(R01PlayerState.QuarryRoadAction.MEADOW_VIPER, 5)
                .recordQuarryRoadAction(R01PlayerState.QuarryRoadAction.BROKEN_ROAD_MARKER, 6);

        assertTrue(state.quarry().discovered());
        assertEquals(R01MainStage.QUARRY_ENTRANCE_DISCOVERED, state.opening().mainStage());
    }

    @Test
    void regalhartDiscoveryWorksFromCluesOrDirectEncounter() {
        var clueState = R01PlayerState.initial()
                .markRegalhartClueSeen(R01PlayerState.RegalhartClue.ANTLER_SCORING, 1)
                .markRegalhartClueSeen(R01PlayerState.RegalhartClue.HOOF_FURROWS, 2);
        var directState = R01PlayerState.initial().markRegalhartDiscovered(1);

        assertTrue(clueState.opening().regalhartDiscovered());
        assertTrue(directState.opening().regalhartDiscovered());
    }

    @Test
    void firstClearCreatesOnePersistentPendingRewardChoice() {
        var state = R01PlayerState.initial()
                .markEarthloongFirstClear(100);
        var repeated = state.markEarthloongFirstClear(101);

        assertEquals(state, repeated);
        assertEquals(R01MainStage.EARTHLOONG_CLEARED, state.opening().mainStage());
        assertTrue(state.quarry().firstClear());
        assertTrue(state.quarry().rewardChoicePending());
        assertTrue(state.economy().pendingRewardClaimIds().contains(
                "openworld_rpg:r01/earthloong_first_clear_choice"
        ));
        assertTrue(state.ledger().firstClearIds().contains("openworld_rpg:r01_quarry"));
    }

    @Test
    void quarryWaystoneActivationFailsClosedUntilDiscovery() {
        assertThrows(
                IllegalStateException.class,
                () -> R01PlayerState.initial().markQuarryWaystoneActivated(1)
        );

        var state = R01PlayerState.initial()
                .markQuarryWaystoneDiscovered(1)
                .markQuarryWaystoneActivated(2);

        assertTrue(state.quarry().waystoneActivated());
    }

    @Test
    void schemaOneStateSurvivesCodecRoundTrip() {
        var original = R01PlayerState.initial()
                .markFirstShrineActivated(10)
                .markFirstRootClassSelected(20)
                .markStarterPackageClaimed(21)
                .recordQuarryRoadAction(R01PlayerState.QuarryRoadAction.LOST_CARGO, 30)
                .markQuarryDiscovered(31)
                .markQuarryWaystoneDiscovered(32);

        var encoded = R01PlayerState.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01PlayerState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
