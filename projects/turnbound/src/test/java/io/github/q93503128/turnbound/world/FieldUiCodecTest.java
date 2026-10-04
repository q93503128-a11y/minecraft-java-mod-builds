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
    void roundTripsServerAuthoredMapPoints() {
        FieldUiSnapshot source = new FieldUiSnapshot(
                true, FieldUiSnapshot.Mode.NONE, 0, 0, false, false, 0, 0,
                "여러 목표를 확인하십시오.", "", FieldUiSnapshot.Reward.none(), List.of(), List.of(),
                "", 0, "", "", "", "", "", FieldUiSnapshot.Navigation.none(),
                List.of(
                        new FieldUiSnapshot.MapPoint("npc:sera", "길잡이 세라", "NPC", 600.5D, 900.5D, false),
                        new FieldUiSnapshot.MapPoint("quest:tower", "돌에 새겨진 길", "QUEST", 557.5D, 1176.5D, true)));
        FieldUiSnapshot decoded = FieldUiCodec.decode(FieldUiCodec.encode(source));
        assertEquals(source.mapPoints(), decoded.mapPoints());
        assertEquals(2, decoded.mapPoints().size());
    }

    @Test
    void roundTripsAuxiliaryQuestTrackers() {
        FieldUiSnapshot source = new FieldUiSnapshot(
                true, FieldUiSnapshot.Mode.NONE, 0, 0, false, false, 0, 0,
                "아브살로 향하십시오.", "", FieldUiSnapshot.Reward.none(), List.of(), List.of(),
                "", 0, "", "", "", "", "", FieldUiSnapshot.Navigation.none(), List.of(),
                List.of(
                        new FieldUiSnapshot.QuestTracker("SQ_AV05", "갈림길을 막은 자들", "서브 목표",
                                "서쪽 갈림길의 약탈대를 정리하십시오.", false),
                        new FieldUiSnapshot.QuestTracker("regional:avsal", "긴 가도 전면 순찰", "지역 의뢰",
                                "가도 전투 2/6", true)));
        FieldUiSnapshot decoded = FieldUiCodec.decode(FieldUiCodec.encode(source));
        assertEquals(source.questTrackers(), decoded.questTrackers());
        assertEquals(2, decoded.questTrackers().size());
    }

    @Test
    void navigationTargetIsAlsoProjectedAsAnObjectiveMapPoint() {
        FieldUiSnapshot.Navigation navigation = new FieldUiSnapshot.Navigation(
                "regional:CV_DRABYEL_NORTH", "북부 도로 순찰대", 612.5D, 1440.5D);
        List<FieldUiSnapshot.MapPoint> projected = DrehmalFirstRouteRuntime.withNavigationMarker(
                List.of(new FieldUiSnapshot.MapPoint("service:smith", "대장장이", "SERVICE", 510.5D, 1805.5D, false)),
                navigation);

        FieldUiSnapshot.MapPoint target = projected.stream()
                .filter(FieldUiSnapshot.MapPoint::objective)
                .findFirst().orElseThrow();
        assertEquals(navigation.x(), target.x());
        assertEquals(navigation.z(), target.z());
        assertTrue(target.label().contains(navigation.label()));

        List<FieldUiSnapshot.MapPoint> deduped = DrehmalFirstRouteRuntime.withNavigationMarker(projected, navigation);
        assertEquals(projected.size(), deduped.size());
    }

    @Test
    void roundTripsBattleTransitionOwnershipState() {
        FieldUiSnapshot decoded = FieldUiCodec.decode(FieldUiCodec.encode(FieldUiSnapshot.battleTransition()));
        assertTrue(decoded.active());
        assertEquals(FieldUiSnapshot.Mode.BATTLE_TRANSITION, decoded.mode());
        assertTrue(decoded.objective().isBlank());
        assertFalse(decoded.navigation().active());
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
