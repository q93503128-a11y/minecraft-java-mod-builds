package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FieldUiCodecTest {
    @Test
    void roundTripsKoreanQuestRewardAndTravelState() {
        FieldUiSnapshot source = new FieldUiSnapshot(
                true,
                FieldUiSnapshot.Mode.RESULT,
                5,
                5,
                true,
                true,
                410,
                710,
                "남부 도로 거점으로 진출해 계전석을 활성화하십시오.",
                "봉쇄선은 무너졌다. 남쪽 길이 열렸어.",
                new FieldUiSnapshot.Reward("B01 그라울", 180, 300, true, true),
                List.of(
                        new FieldUiSnapshot.Encounter("southgate_enc_m01", "무너진 순찰대", true, true, false),
                        new FieldUiSnapshot.Encounter("southgate_b01_graul", "B01 그라울", true, true, true)),
                List.of(
                        new FieldUiSnapshot.Travel(FieldTravelCatalog.RELAY_A01, "남문 초원 계전석", true, false),
                        new FieldUiSnapshot.Travel(FieldTravelCatalog.RELAY_A02, "남부 도로 거점 계전석", false, true)));

        FieldUiSnapshot decoded = FieldUiCodec.decode(FieldUiCodec.encode(source));
        assertEquals(source, decoded);
    }

    @Test
    void roundTripsSurveyedLocationPresentation() {
        FieldUiSnapshot source = new FieldUiSnapshot(
                true,
                FieldUiSnapshot.Mode.NONE,
                0,
                0,
                false,
                false,
                0,
                0,
                "길을 따라 New Drabyel을 찾으십시오.",
                "",
                FieldUiSnapshot.Reward.none(),
                List.of(),
                List.of(),
                "",
                0,
                "turnbound:site/capital_valley/tower",
                "Capital Valley Tower");

        FieldUiSnapshot decoded = FieldUiCodec.decode(FieldUiCodec.encode(source));
        assertEquals("turnbound:site/capital_valley/tower", decoded.locationId());
        assertEquals("Capital Valley Tower", decoded.locationTitle());
    }

    @Test
    void roundTripsCloseRangeServicePrompt() {
        FieldUiSnapshot source = new FieldUiSnapshot(
                true,
                FieldUiSnapshot.Mode.NONE,
                0,
                0,
                false,
                false,
                0,
                0,
                "New Drabyel에서 다음 여정을 준비하십시오.",
                "",
                FieldUiSnapshot.Reward.none(),
                List.of(),
                List.of(),
                "",
                0,
                "turnbound:site/capital_valley/new_drabyel",
                "New Drabyel",
                "turnbound:service/new_drabyel/market",
                "장비 상인",
                "상점 열기");

        FieldUiSnapshot decoded = FieldUiCodec.decode(FieldUiCodec.encode(source));
        assertEquals("turnbound:service/new_drabyel/market", decoded.interactionId());
        assertEquals("장비 상인", decoded.interactionLabel());
        assertEquals("상점 열기", decoded.interactionAction());
    }

    @Test
    void roundTripsServerAuthoredDrehmalNavigation() {
        FieldUiSnapshot.Navigation navigation = new FieldUiSnapshot.Navigation(
                "turnbound:site/capital_valley/tower",
                "Capital Valley Tower",
                557.5D,
                1176.5D);
        FieldUiSnapshot source = new FieldUiSnapshot(
                true,
                FieldUiSnapshot.Mode.NONE,
                0,
                0,
                false,
                false,
                0,
                0,
                "길을 따라 New Drabyel을 찾으십시오.",
                "",
                FieldUiSnapshot.Reward.none(),
                List.of(),
                List.of(),
                "",
                0,
                "",
                "",
                "",
                "",
                "",
                navigation);

        FieldUiSnapshot decoded = FieldUiCodec.decode(FieldUiCodec.encode(source));
        assertTrue(decoded.navigation().active());
        assertEquals(navigation, decoded.navigation());
    }

    @Test
    void malformedOptionalLineDoesNotDestroyHeaderState() {
        String encoded = "H|1|QUEST|2|5|0|0|75|130\nE|broken\n";
        FieldUiSnapshot decoded = FieldUiCodec.decode(encoded);
        assertTrue(decoded.active());
        assertEquals(FieldUiSnapshot.Mode.QUEST, decoded.mode());
        assertEquals(2, decoded.patrolsCleared());
        assertEquals(75, decoded.earnedXp());
        assertTrue(decoded.encounters().isEmpty());
    }
}
