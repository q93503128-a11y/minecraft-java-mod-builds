package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalQuestMenuContentServiceTest {
    @Test
    void emitsOnePlayerFacingFirstRouteQuestWithoutLegacyQuestIds() {
        assertEquals(
                "Q|뉴 드라비엘로 가는 길|메인 · Capital Valley|1|0|길을 따라 뉴 드라비엘을 찾으십시오.\n",
                DrehmalQuestMenuContentService.encodeObjective("길을 따라 뉴 드라비엘을 찾으십시오."));
    }

    @Test
    void sanitizesWireDelimitersAndNewlines() {
        assertEquals(
                "Q|뉴 드라비엘로 가는 길|메인 · Capital Valley|1|0|야영지 / 길 확인\n",
                DrehmalQuestMenuContentService.encodeObjective("야영지 | 길 확인\n"));
    }

    @Test
    void surveyGatedSideQuestsStayHiddenUntilTheirEncounterIsProductionReady() {
        String encoded = DrehmalQuestMenuContentService.encodeState(
                Set.of(DrehmalFirstRouteProgress.TOWER_REACHED, DrehmalFirstRouteProgress.CAMP_REACHED),
                Set.of(),
                Set.of(),
                "뉴 드라비엘로 향하십시오.");
        assertTrue(encoded.contains("뉴 드라비엘로 가는 길"));
        assertFalse(encoded.contains("경고 동굴의 강적"));
        assertFalse(encoded.contains("진입로 안전 확보"));
    }

    @Test
    void productionBoundSideQuestsExposePlaceSpecificObjectivesAndCompletion() {
        String encoded = DrehmalQuestMenuContentService.encodeState(
                Set.of(DrehmalFirstRouteProgress.TOWER_REACHED, DrehmalFirstRouteProgress.CAMP_REACHED),
                Set.of("CV_WARNING_CAVE_ELITE"),
                Set.of("CV_WARNING_CAVE_ELITE", "CV_DRABYEL_ROAD"),
                "뉴 드라비엘로 향하십시오.");
        assertTrue(encoded.contains("Q|경고 동굴의 강적|서브 목표 · Capital Valley|1|1|경고 동굴 안의 강적을 조사하고 쓰러뜨리십시오."));
        assertTrue(encoded.contains("Q|진입로 안전 확보|서브 목표 · New Drabyel|1|0|뉴 드라비엘 진입로의 순찰대를 정리하십시오."));
    }
}
