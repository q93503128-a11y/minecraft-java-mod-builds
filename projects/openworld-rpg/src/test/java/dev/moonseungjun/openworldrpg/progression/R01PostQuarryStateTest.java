package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.progression.r01.R01MainStage;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerState;
import org.junit.jupiter.api.Test;

class R01PostQuarryStateTest {
    @Test
    void immediateAftermathAdvancesToReturnObjectiveWithoutOpeningActLeadsYet() {
        R01PlayerState state = firstClearState();

        state = state.commitPostQuarryAftermath(10L);

        assertEquals(
                R01MainStage.POST_QUARRY_BRIEFING_PENDING,
                state.opening().mainStage()
        );
        assertFalse(state.opening().postQuarryBriefingSeen());
        assertFalse(state.opening().act1WesternRelayLeadKnown());
        assertFalse(state.opening().act1WhitecrestStationLeadKnown());
        assertEquals(
                "return_to_alderford",
                state.ledger().questStepIds().get(
                        "openworld_rpg:r01/lines_beneath_the_land"
                )
        );
        assertTrue(state.ledger().completedStepIds().contains(
                "openworld_rpg:r01/post_quarry_aftermath"
        ));
    }

    @Test
    void completingOrSkippingBriefingOpensBothPeerLeadsExactlyOnce() {
        R01PlayerState state = firstClearState()
                .commitPostQuarryAftermath(10L)
                .completePostQuarryBriefing(11L);

        assertEquals(
                R01MainStage.ACT1_LEADS_OPEN,
                state.opening().mainStage()
        );
        assertTrue(state.opening().postQuarryBriefingSeen());
        assertTrue(state.opening().act1WesternRelayLeadKnown());
        assertTrue(state.opening().act1WhitecrestStationLeadKnown());
        assertEquals(
                "act1_leads_open",
                state.ledger().questStepIds().get(
                        "openworld_rpg:r01/lines_beneath_the_land"
                )
        );
        assertTrue(state.ledger().discoveryFlags().contains(
                "openworld_rpg:r01/lead/western_relay"
        ));
        assertTrue(state.ledger().discoveryFlags().contains(
                "openworld_rpg:r01/lead/whitecrest_station"
        ));

        assertEquals(state, state.completePostQuarryBriefing(12L));
    }

    private static R01PlayerState firstClearState() {
        R01PlayerState state = R01PlayerState.initial()
                .markFirstShrineActivated(1L)
                .markFirstRootClassSelected(2L)
                .markStarterPackageClaimed(3L)
                .recordQuarryRoadAction(
                        R01PlayerState.QuarryRoadAction.LOST_CARGO,
                        4L
                )
                .recordQuarryRoadAction(
                        R01PlayerState.QuarryRoadAction.MEADOW_VIPER,
                        5L
                )
                .recordQuarryRoadAction(
                        R01PlayerState.QuarryRoadAction.BROKEN_ROAD_MARKER,
                        6L
                )
                .markQuarryDiscovered(7L)
                .beginQuarryRun(1L, 8L)
                .markEarthloongFirstClear(9L);

        assertEquals(
                R01MainStage.EARTHLOONG_CLEARED,
                state.opening().mainStage()
        );
        return state;
    }
}
