package io.github.q93503128.turnbound.presentation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeroPortraitPlanTest {
    @Test
    void allEightCoreHeroesHaveStablePortraitCameras() {
        for (String id : List.of("P01","P02","P03","P04","P05","P06","P07","P08")) {
            assertTrue(HeroPortraitPlan.coreHero(id), id);
            var camera = HeroPortraitPlan.camera(id);
            assertTrue(camera.scale() >= 0.60F && camera.scale() <= 1.25F, id);
        }
        assertFalse(HeroPortraitPlan.coreHero("CV_A"));
    }

    @Test
    void signaturePortraitUsesTheSameAuthoredVisualFamilyAsBattleActors() {
        assertEquals("P01_SIG", HeroPortraitPlan.signatureVisualId("P01", "sig_p01_unending_vow"));
        assertEquals("P03_SIG", HeroPortraitPlan.signatureVisualId("P03", "sig_p03_gate_shield"));
        assertEquals("P08_SIG", HeroPortraitPlan.signatureVisualId("P08", "sig_p08_blood_grip"));
        assertEquals("P07", HeroPortraitPlan.signatureVisualId("P07", "sig_p07_second_contract"),
                "Marion stays Marion; the P07 signature attachment belongs to Toto");
        assertEquals("P05", HeroPortraitPlan.signatureVisualId("P05", "wrong_signature"));
    }
}
