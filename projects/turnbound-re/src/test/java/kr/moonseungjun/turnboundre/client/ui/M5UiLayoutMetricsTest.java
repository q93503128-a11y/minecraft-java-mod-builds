package kr.moonseungjun.turnboundre.client.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class M5UiLayoutMetricsTest {
    @Test
    void battleHudRegionsStayInsideAndProtectTheWorldViewportAtTargetSizes() {
        for (int[] size : List.of(new int[]{1280, 720}, new int[]{1920, 1080}, new int[]{640, 360})) {
            assertTrue(UiLayoutMetrics.supportsBattleHud(size[0], size[1]));
            UiLayoutMetrics.BattleHudLayout layout = UiLayoutMetrics.battleHud(size[0], size[1]);
            List<UiLayoutMetrics.Rect> regions = List.of(
                    layout.turnRail(), layout.enemySummary(), layout.partyStatus(),
                    layout.commandStrip(), layout.reservedWorldViewport());
            assertTrue(regions.stream().allMatch(rect -> rect.inside(size[0], size[1])),
                    () -> "out of bounds at " + size[0] + "x" + size[1]);

            assertFalse(layout.partyStatus().intersects(layout.commandStrip()));
            assertFalse(layout.reservedWorldViewport().intersects(layout.turnRail()));
            assertFalse(layout.reservedWorldViewport().intersects(layout.enemySummary()));
            assertFalse(layout.reservedWorldViewport().intersects(layout.partyStatus()));
            assertFalse(layout.reservedWorldViewport().intersects(layout.commandStrip()));
            assertTrue(layout.reservedWorldViewport().width() >= 300);
            assertTrue(layout.reservedWorldViewport().height() >= 120);
        }
    }

    @Test
    void minimumCanvasUsesReadableTwoByTwoPartyGridWithoutTakingTheWorld() {
        UiLayoutMetrics.BattleHudLayout layout = UiLayoutMetrics.battleHud(480, 270);
        UiLayoutMetrics.PartyGridLayout party = UiLayoutMetrics.partyGrid(layout.partyStatus(), 4);

        assertTrue(party.compact());
        assertEquals(2, party.columns());
        assertEquals(2, party.rows());
        assertTrue(party.cellWidth() >= 120, "compact party cells must still fit identity and resources");
        assertTrue(party.cellHeight() >= 40, "compact party rows must fit two bars and one status line");
        assertTrue(layout.reservedWorldViewport().width() >= 300);
        assertTrue(layout.reservedWorldViewport().height() >= 96);
        assertFalse(layout.reservedWorldViewport().intersects(layout.partyStatus()));
        assertFalse(layout.reservedWorldViewport().intersects(layout.commandStrip()));
    }

    @Test
    void commonScaleFourDesktopCanvasUsesCompactProductionLayoutInsteadOfBlankingUi() {
        int width = 420;
        int height = 236;

        assertTrue(UiLayoutMetrics.supportsBattleHud(width, height));
        UiLayoutMetrics.BattleHudLayout battle = UiLayoutMetrics.battleHud(width, height);
        List<UiLayoutMetrics.Rect> battleRegions = List.of(
                battle.turnRail(), battle.enemySummary(), battle.partyStatus(),
                battle.commandStrip(), battle.reservedWorldViewport());
        assertTrue(battleRegions.stream().allMatch(rect -> rect.inside(width, height)));
        assertFalse(battle.partyStatus().intersects(battle.commandStrip()));
        assertFalse(battle.reservedWorldViewport().intersects(battle.turnRail()));
        assertFalse(battle.reservedWorldViewport().intersects(battle.enemySummary()));
        assertFalse(battle.reservedWorldViewport().intersects(battle.partyStatus()));
        assertFalse(battle.reservedWorldViewport().intersects(battle.commandStrip()));
        assertTrue(battle.reservedWorldViewport().width() >= 300);
        assertTrue(battle.reservedWorldViewport().height() >= 60);

        UiLayoutMetrics.PartyGridLayout partyGrid = UiLayoutMetrics.partyGrid(battle.partyStatus(), 4);
        assertTrue(partyGrid.compact());
        assertEquals(2, partyGrid.columns());
        assertEquals(2, partyGrid.rows());
        assertTrue(partyGrid.cellWidth() >= 100);
        assertTrue(partyGrid.cellHeight() >= 34);

        assertTrue(UiLayoutMetrics.supportsPartyScreen(width, height));
        UiLayoutMetrics.PartyFormationLayout party = UiLayoutMetrics.partyFormation(width, height);
        List<UiLayoutMetrics.Rect> partyRegions = List.of(
                party.root(), party.header(), party.tabs(), party.roster(),
                party.activeParty(), party.selectedDetail(), party.footer());
        assertTrue(partyRegions.stream().allMatch(rect -> rect.inside(width, height)));
        assertFalse(party.roster().intersects(party.activeParty()));
        assertFalse(party.activeParty().intersects(party.selectedDetail()));
        assertFalse(party.roster().intersects(party.selectedDetail()));
        assertTrue(party.roster().width() >= 128);
        assertTrue(party.activeParty().width() >= 92);
        assertTrue(party.selectedDetail().width() >= 138);
        assertTrue(party.roster().height() >= 120);
    }

    @Test
    void normalCanvasKeepsFourMemberPartyOnOneRow() {
        UiLayoutMetrics.BattleHudLayout layout = UiLayoutMetrics.battleHud(640, 360);
        UiLayoutMetrics.PartyGridLayout party = UiLayoutMetrics.partyGrid(layout.partyStatus(), 4);
        assertFalse(party.compact());
        assertEquals(4, party.columns());
        assertEquals(1, party.rows());
        assertTrue(party.cellWidth() >= 90);
    }

    @Test
    void targetChooserReusesCommandStripInsteadOfCreatingCenterModal() {
        for (int[] size : List.of(
                new int[]{420, 236},
                new int[]{480, 270},
                new int[]{640, 360},
                new int[]{1280, 720},
                new int[]{1920, 1080})) {
            UiLayoutMetrics.BattleHudLayout hud = UiLayoutMetrics.battleHud(size[0], size[1]);
            UiLayoutMetrics.TargetChooserLayout chooser = UiLayoutMetrics.targetChooser(size[0], size[1]);

            assertEquals(hud.commandStrip(), chooser.region());
            assertTrue(chooser.header().inside(size[0], size[1]));
            assertTrue(chooser.grid().inside(size[0], size[1]));
            assertFalse(chooser.region().intersects(hud.reservedWorldViewport()),
                    () -> "target chooser invaded world viewport at " + size[0] + "x" + size[1]);
            assertEquals(chooser.columns() * chooser.rows(), chooser.pageSize());
            assertTrue(chooser.pageSize() >= 2);
        }

        assertEquals(6, UiLayoutMetrics.targetChooser(1280, 720).pageSize(),
                "normal desktop layout should expose six targets without a popup");
    }

    @Test
    void partyFormationKeepsRosterActivePartyAndDetailVisibleTogether() {
        for (int[] size : List.of(
                new int[]{480, 270},
                new int[]{640, 360},
                new int[]{1280, 720},
                new int[]{1920, 1080})) {
            assertTrue(UiLayoutMetrics.supportsPartyScreen(size[0], size[1]));
            UiLayoutMetrics.PartyFormationLayout layout = UiLayoutMetrics.partyFormation(size[0], size[1]);
            List<UiLayoutMetrics.Rect> regions = List.of(
                    layout.root(), layout.header(), layout.tabs(), layout.roster(),
                    layout.activeParty(), layout.selectedDetail(), layout.footer());
            assertTrue(regions.stream().allMatch(rect -> rect.inside(size[0], size[1])),
                    () -> "party screen out of bounds at " + size[0] + "x" + size[1]);
            assertFalse(layout.roster().intersects(layout.activeParty()));
            assertFalse(layout.activeParty().intersects(layout.selectedDetail()));
            assertFalse(layout.roster().intersects(layout.selectedDetail()));
            assertTrue(layout.roster().width() >= 160);
            assertTrue(layout.activeParty().width() >= 110);
            assertTrue(layout.selectedDetail().width() >= 150);
            assertTrue(layout.roster().height() >= 150);
        }
    }

    @Test
    void partyFormationCapsWideLayoutInsteadOfStretchingScanDistancesForever() {
        UiLayoutMetrics.PartyFormationLayout layout = UiLayoutMetrics.partyFormation(1920, 1080);
        assertEquals(960, layout.root().width());
        assertEquals((1920 - 960) / 2, layout.root().x());
    }

    @Test
    void unsupportedTinyLogicalCanvasIsDetectedBeforeRenderAndStillFailsExplicitLayout() {
        assertFalse(UiLayoutMetrics.supportsBattleHud(320, 180));
        assertFalse(UiLayoutMetrics.supportsPartyScreen(320, 180));
        IllegalArgumentException battleError = assertThrows(
                IllegalArgumentException.class,
                () -> UiLayoutMetrics.battleHud(320, 180));
        IllegalArgumentException partyError = assertThrows(
                IllegalArgumentException.class,
                () -> UiLayoutMetrics.partyFormation(320, 180));
        assertTrue(battleError.getMessage().contains("requires at least"));
        assertTrue(partyError.getMessage().contains("requires at least"));
    }
}
