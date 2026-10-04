package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class QuestGuideDirectionTest {
    @Test
    void minecraftYawProjectsCardinalTargetsToExpectedScreenDirections() {
        assertEquals("↑", QuestGuideDirection.arrow(
                QuestGuideDirection.targetDeltaDegrees(0, 0, 0.0F, 0, 10)));
        assertEquals("↓", QuestGuideDirection.arrow(
                QuestGuideDirection.targetDeltaDegrees(0, 0, 0.0F, 0, -10)));
        assertEquals("←", QuestGuideDirection.arrow(
                QuestGuideDirection.targetDeltaDegrees(0, 0, 0.0F, 10, 0)));
        assertEquals("→", QuestGuideDirection.arrow(
                QuestGuideDirection.targetDeltaDegrees(0, 0, 0.0F, -10, 0)));
        assertEquals("→", QuestGuideDirection.arrow(
                QuestGuideDirection.targetDeltaDegrees(0, 0, -90.0F, 0, 10)));
    }
}
