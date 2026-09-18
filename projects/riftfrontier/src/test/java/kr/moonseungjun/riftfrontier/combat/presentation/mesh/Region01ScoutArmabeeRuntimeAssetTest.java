package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class Region01ScoutArmabeeRuntimeAssetTest {
    @Test
    void inspectionReceiptAndReviewVocabularyStayLocked() {
        assertEquals("10ca05955ab7f7f6e2f9bd8fd85f28ed1351c945e390b1fd0fa84428b43a5916", Region01ScoutArmabeeRuntimeAsset.SOURCE_SHA256);
        assertEquals(1260, Region01ScoutArmabeeRuntimeAsset.VERTEX_COUNT);
        assertEquals(2280, Region01ScoutArmabeeRuntimeAsset.TRIANGLE_COUNT);
        assertEquals(List.of("Death", "Fast_Flying", "Flying_Idle", "Headbutt", "HitReact", "No", "Punch", "Yes"), Region01ScoutArmabeeRuntimeAsset.EXPECTED_CLIPS);
        assertEquals(List.of("Flying_Idle", "Fast_Flying", "Punch", "Headbutt", "HitReact", "Death"), Region01ScoutArmabeeRuntimeAsset.FIELD_REVIEW_SEQUENCE);
    }

    @Test
    void sourceDoesNotAuthorizeGroundedLocomotionOrFlightGameplay() {
        assertFalse(Region01ScoutArmabeeRuntimeAsset.hasGroundedLocomotionSource());
    }
}
