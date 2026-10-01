package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DrehmalQuestMenuContentServiceTest {
    @Test
    void emitsOnePlayerFacingFirstRouteQuestWithoutLegacyQuestIds() {
        assertEquals(
                "Q|뉴 드라비엘로 가는 길|메인 · Capital Valley|1|0|길을 따라 뉴 드라비엘을 찾으십시오. · 보상 600 Crystal / 5,000 Gold / 파티 XP 1200\n",
                DrehmalQuestMenuContentService.encodeObjective("길을 따라 뉴 드라비엘을 찾으십시오."));
    }

    @Test
    void sanitizesWireDelimitersNewlinesAndRepeatedWhitespace() {
        assertEquals(
                "Q|뉴 드라비엘로 가는 길|메인 · Capital Valley|1|0|야영지 / 길 확인 · 보상 600 Crystal / 5,000 Gold / 파티 XP 1200\n",
                DrehmalQuestMenuContentService.encodeObjective("야영지 | 길 확인\n"));
    }

    @Test
    void productionBoundSideQuestsStayHiddenUntilTheirEncounterIsProductionReady() {
        String encoded = DrehmalQuestMenuContentService.encodeState(
                Set.of(DrehmalFirstRouteProgress.TOWER_REACHED, DrehmalFirstRouteProgress.CAMP_REACHED),
                Set.of(),
                Set.of(),
                "뉴 드라비엘로 향하십시오.");
        assertTrue(encoded.contains("뉴 드라비엘로 가는 길"));
        assertFalse(encoded.contains("경고 동굴의 강적"));
        assertFalse(encoded.contains("북문 순찰"));
    }

    @Test
    void combatSideQuestAndNpcIssuedMainPatrolRespectTheirOwnActivationRules() {
        String warning = DrehmalQuestMenuContentService.encodeState(
                Set.of(DrehmalFirstRouteProgress.TOWER_REACHED, DrehmalFirstRouteProgress.CAMP_REACHED),
                Set.of("CV_WARNING_CAVE_ELITE"),
                Set.of("CV_WARNING_CAVE_ELITE"),
                "뉴 드라비엘로 향하십시오.");
        assertTrue(warning.contains(
                "Q|경고 동굴의 강적|서브 목표 · Capital Valley|1|1|경고 동굴 안의 강적을 조사하고 쓰러뜨리십시오. · 보상 400 Crystal / 4,000 Gold / 파티 XP 900"));

        String patrol = DrehmalQuestMenuContentService.encodeState(
                Set.of(DrabyelOpeningTutorial.GREETER_FLAG),
                Set.of("CV_DRABYEL_ROAD"),
                Set.of("CV_DRABYEL_ROAD"),
                "다음 목표");
        assertTrue(patrol.contains(
                "Q|북문 순찰|메인 · New Drabyel|1|1|입구 안내원 아렌의 부탁대로 뉴 드라비엘 북쪽 가까운 길목의 순찰대를 정리하십시오. · 보상 300 Crystal / 3,000 Gold / 파티 XP 700"));
    }
}
