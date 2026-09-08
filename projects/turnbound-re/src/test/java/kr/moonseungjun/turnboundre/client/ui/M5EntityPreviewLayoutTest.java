package kr.moonseungjun.turnboundre.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class M5EntityPreviewLayoutTest {
    @Test
    void compactDetailSkipsPreviewInsteadOfCrushingCharacterFacts() {
        EntityPreviewLayout.PreviewSpec spec = EntityPreviewLayout.fit(180, 150, 0.6F, 1.95F);
        assertFalse(spec.visible());
        assertEquals(180, spec.textWidth());
    }

    @Test
    void normalDetailReservesReadableTextAndBoundedPreview() {
        EntityPreviewLayout.PreviewSpec spec = EntityPreviewLayout.fit(240, 166, 0.6F, 1.95F);
        assertTrue(spec.visible());
        assertTrue(spec.textWidth() >= 118);
        assertTrue(spec.renderScale() > 0 && spec.renderScale() <= 48);
        assertTrue(spec.fitsInside(240, 166));
        assertTrue(spec.xOffset() >= spec.textWidth());
    }

    @Test
    void tallWideAndLowBodiesAllFitInsideTheSamePreviewContract() {
        assertFits(240, 166, 0.6F, 2.9F);  // Enderman-like tall body
        assertFits(240, 166, 1.4F, 2.7F);  // Iron Golem-like large body
        assertFits(240, 166, 1.4F, 0.9F);  // Spider-like wide/low body
        assertFits(240, 166, 0.6F, 1.95F); // Zombie-like humanoid
    }

    @Test
    void invalidEntityDimensionsFailClosed() {
        assertFalse(EntityPreviewLayout.fit(240, 166, 0.0F, 1.0F).visible());
        assertFalse(EntityPreviewLayout.fit(240, 166, Float.NaN, 1.0F).visible());
        assertFalse(EntityPreviewLayout.fit(240, 166, 1.0F, Float.POSITIVE_INFINITY).visible());
    }

    private static void assertFits(int regionWidth, int regionHeight, float entityWidth, float entityHeight) {
        EntityPreviewLayout.PreviewSpec spec = EntityPreviewLayout.fit(regionWidth, regionHeight, entityWidth, entityHeight);
        assertTrue(spec.visible());
        assertTrue(spec.fitsInside(regionWidth, regionHeight));
        assertTrue(entityWidth * spec.renderScale() <= spec.width() - 12 + 0.001F);
        assertTrue(entityHeight * spec.renderScale() <= spec.height() - 8 + 0.001F);
    }
}
