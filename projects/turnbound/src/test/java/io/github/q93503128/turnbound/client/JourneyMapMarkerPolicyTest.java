package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JourneyMapMarkerPolicyTest {
    @Test
    void onlyCurrentNavigationOverlayIsForcedOntoTheMinimap() {
        var current = new FieldUiSnapshot.MapPoint(
                "navigation:turnbound:site/north", "현재 목표 · 북쪽 길 순찰대", "QUEST",
                612.5D, 1440.5D, true);
        var otherQuest = new FieldUiSnapshot.MapPoint(
                "quest:side", "옛 길의 흔적", "QUEST", 700.5D, 1500.5D, true);
        var service = new FieldUiSnapshot.MapPoint(
                "service:smith", "대장장이", "SERVICE", 510.5D, 1805.5D, false);

        assertTrue(JourneyMapMarkerPolicy.isCurrentNavigation(current));
        assertTrue(JourneyMapMarkerPolicy.showOnMinimap(current));
        assertFalse(JourneyMapMarkerPolicy.showOnMinimap(otherQuest));
        assertFalse(JourneyMapMarkerPolicy.showOnMinimap(service));
    }
}
