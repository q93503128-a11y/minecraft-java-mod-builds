package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01FieldGuideTest {
    @Test
    void keepsFirstSliceGuideSmallStableAndLocalizationDriven() {
        assertEquals("Riftfrontier Field Guide", Region01FieldGuideSpec.TITLE);
        assertEquals("Riftfrontier", Region01FieldGuideSpec.AUTHOR);
        assertEquals(5, Region01FieldGuideSpec.PAGE_KEYS.size());
        assertEquals(5, Region01FieldGuideSpec.PAGE_KEYS.stream().distinct().count());
        assertTrue(Region01FieldGuideSpec.PAGE_KEYS.stream().allMatch(key -> key.startsWith("riftfrontier.guide.region_01.page.")));
    }
}
