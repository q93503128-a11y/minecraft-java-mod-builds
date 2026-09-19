package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrabyelInteractionPromptRulesTest {
    @Test
    void nearestSupportedServiceWinsOnlyInsideInteractionRange() {
        var market = service("market", "MARKET", "장비 상인", 0, 0, 5);
        var smith = service("smith", "BLACKSMITH", "대장장이", 12, 0, 5);

        var nearMarket = DrabyelInteractionPromptRules.nearest(
                List.of(market, smith), 2.0, 70.0, 0.5, asset -> true);
        var between = DrabyelInteractionPromptRules.nearest(
                List.of(market, smith), 6.5, 70.0, 0.5, asset -> true);

        assertTrue(nearMarket.active());
        assertEquals(market.locator(), nearMarket.id());
        assertEquals("상점 열기", nearMarket.action());
        assertFalse(between.active());
    }

    @Test
    void unsupportedVisualNeverProducesGhostPrompt() {
        var story = service("story", "STORY", "마을 중심", 0, 0, 5);

        var prompt = DrabyelInteractionPromptRules.nearest(
                List.of(story), 0.5, 70.0, 0.5, asset -> false);

        assertFalse(prompt.active());
    }

    @Test
    void roleCopyStaysPlayerFacingAndCompact() {
        assertEquals("대장간 이용", DrabyelInteractionPromptRules.action("BLACKSMITH"));
        assertEquals("이동 지도 보기", DrabyelInteractionPromptRules.action("TRAVEL"));
        assertEquals("대화하기", DrabyelInteractionPromptRules.action("GREETER"));
    }

    private static DrabyelHubServiceCatalog.Service service(
            String id, String role, String label, int x, int z, int radius
    ) {
        return new DrabyelHubServiceCatalog.Service(
                "turnbound:test/" + id,
                role,
                label,
                role.equals("BLACKSMITH") ? "FORGE" : role.equals("MARKET") ? "MARKET" : "QUESTS",
                "TEST",
                "TEST_VISUAL",
                new DrabyelHubServiceCatalog.Position(x, 70, z),
                0.0F,
                radius,
                true,
                true);
    }
}
