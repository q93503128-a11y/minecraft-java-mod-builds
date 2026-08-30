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
