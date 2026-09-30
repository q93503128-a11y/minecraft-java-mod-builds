package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattleResultLayoutTest {
    @Test
    void compactResultUsesTwoColumnPartyGridBeforeFooterOverlap() {
        int panelHeight = 235;
        int partyRowTopOffset = 125;
        assertTrue(BattleResultLayout.useCompactPartyGrid(panelHeight, partyRowTopOffset, 4));
        int compactHeight = BattleResultLayout.partyBlockHeight(4, true);
        assertEquals(50, compactHeight);
        assertTrue(partyRowTopOffset + compactHeight <= panelHeight - BattleResultLayout.FOOTER_RESERVE);
    }

    @Test
    void largeResultKeepsDetailedSingleColumnRows() {
        int panelHeight = 420;
        int partyRowTopOffset = 145;
        assertFalse(BattleResultLayout.useCompactPartyGrid(panelHeight, partyRowTopOffset, 4));
        assertEquals(112, BattleResultLayout.partyBlockHeight(4, false));
    }

    @Test
    void smallViewportEstimatesCompactPartyHeight() {
        assertEquals(50, BattleResultLayout.estimatedPartyHeight(259, 4));
        assertEquals(112, BattleResultLayout.estimatedPartyHeight(480, 4));
    }
}
