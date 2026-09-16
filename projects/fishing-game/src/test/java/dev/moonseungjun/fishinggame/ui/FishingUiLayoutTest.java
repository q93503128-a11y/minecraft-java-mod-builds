package dev.moonseungjun.fishinggame.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FishingUiLayoutTest {
    @Test
    void keepsPreferredSizesAtObservedGuiScale() {
        assertEquals(new FishingUiLayout.Size(174, 62), FishingUiLayout.hud(428, 259));
        assertEquals(new FishingUiLayout.Size(340, 220), FishingUiLayout.catchBag(428, 259));
        assertEquals(new FishingUiLayout.Size(348, 220), FishingUiLayout.bestiary(428, 259));
        assertEquals(new FishingUiLayout.Size(320, 206), FishingUiLayout.travel(428, 259));
    }

    @Test
    void clampsScreensInsideSmallLogicalGui() {
        FishingUiLayout.Size bag = FishingUiLayout.catchBag(300, 180);
        FishingUiLayout.Size bestiary = FishingUiLayout.bestiary(300, 180);
        FishingUiLayout.Size travel = FishingUiLayout.travel(300, 180);

        assertTrue(bag.width() <= 276 && bag.height() <= 160);
        assertTrue(bestiary.width() <= 276 && bestiary.height() <= 160);
        assertTrue(travel.width() <= 276 && travel.height() <= 160);
        assertTrue(bag.width() > 0 && bag.height() > 0);
        assertTrue(bestiary.width() > 0 && bestiary.height() > 0);
        assertTrue(travel.width() > 0 && travel.height() > 0);
    }
}
