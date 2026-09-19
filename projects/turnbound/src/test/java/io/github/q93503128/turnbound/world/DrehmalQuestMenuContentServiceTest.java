package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DrehmalQuestMenuContentServiceTest {
    @Test
    void emitsOnePlayerFacingFirstRouteQuestWithoutLegacyQuestIds() {
        assertEquals(
                "Q|New Drabyel로 가는 길|메인 · Capital Valley|1|0|길을 따라 New Drabyel을 찾으십시오.\n",
                DrehmalQuestMenuContentService.encodeObjective("길을 따라 New Drabyel을 찾으십시오."));
    }

    @Test
    void sanitizesWireDelimitersAndNewlines() {
        assertEquals(
                "Q|New Drabyel로 가는 길|메인 · Capital Valley|1|0|야영지 / 길 확인\n",
                DrehmalQuestMenuContentService.encodeObjective("야영지 | 길 확인\n"));
    }
}
