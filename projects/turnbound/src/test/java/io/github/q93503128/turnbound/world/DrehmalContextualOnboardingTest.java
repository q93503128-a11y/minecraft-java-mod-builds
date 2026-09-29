package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalContextualOnboardingTest {
    @Test
    void hubObjectivesNameTheNextConcreteAction() {
        Set<String> roles = Set.of("GREETER", "BLACKSMITH", "MARKET", "TRAVEL", "SUMMON");
        var greeter = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE", Set.of(DrehmalContentUnlocks.DRABYEL_ROAD), Set.of(), roles);
        assertTrue(greeter.objective().contains("입구 안내"));
        var menu = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE", Set.of(DrehmalContentUnlocks.DRABYEL_ROAD),
                Set.of(DrehmalContextualOnboarding.serviceFlag("GREETER")), roles);
        assertTrue(menu.objective().contains("E 메뉴"));
        var forge = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE", Set.of(DrehmalContentUnlocks.DRABYEL_ROAD),
                Set.of(DrehmalContextualOnboarding.serviceFlag("GREETER"), DrehmalContextualOnboarding.HUB_MENU_VIEWED), roles);
        assertTrue(forge.objective().contains("대장장이"));
        var map = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE", Set.of(DrehmalContentUnlocks.DRABYEL_ROAD),
                Set.of(DrehmalContextualOnboarding.serviceFlag("GREETER"), DrehmalContextualOnboarding.HUB_MENU_VIEWED,
                        DrehmalContextualOnboarding.serviceFlag("BLACKSMITH"), DrehmalContextualOnboarding.serviceFlag("MARKET"),
                        DrehmalContextualOnboarding.serviceFlag("TRAVEL"), DrehmalContextualOnboarding.serviceFlag("SUMMON")), roles);
        assertTrue(map.objective().contains("M 지도"));
    }

    @Test
    void hubReachedFlagUsesHubGuidanceEvenWhenRouteSiteIsNotPromoted() {
        var guidance = DrehmalContextualOnboarding.resolve(
                "",
                Set.of(),
                Set.of(DrehmalFirstRouteProgress.HUB_REACHED),
                Set.of());
        assertTrue(guidance.objective().contains("E 메뉴"));
        assertFalse(guidance.objective().contains("길을 따라"));
    }

    @Test
    void hubKeepsAUsefulObjectiveWhenNoSurveyedHubServicesAreAvailable() {
        var completed = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE",
                Set.of(),
                Set.of(DrehmalContextualOnboarding.HUB_MENU_VIEWED),
                Set.of());
        assertTrue(completed.objective().contains("북쪽 길"));
        assertTrue(completed.hint().contains("입구"));
    }

    @Test
    void townOpeningSendsThePlayerOutOnlyAfterGreeterAndPartyCheck() {
        Set<String> roles = Set.of("GREETER", "BLACKSMITH", "MARKET");
        var beforeMenu = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE",
                Set.of(),
                Set.of(DrehmalContextualOnboarding.serviceFlag("GREETER")),
                roles);
        assertTrue(beforeMenu.objective().contains("E 메뉴"));

        var patrol = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE",
                Set.of(),
                Set.of(DrehmalContextualOnboarding.serviceFlag("GREETER"),
                        DrehmalContextualOnboarding.HUB_MENU_VIEWED),
                roles);
        assertTrue(patrol.objective().contains("북쪽 길"));

        var returning = DrehmalContextualOnboarding.resolve(
                "PATROL_ZONE",
                Set.of(DrehmalContentUnlocks.DRABYEL_ROAD),
                Set.of(DrehmalFirstRouteProgress.HUB_REACHED,
                        DrehmalContextualOnboarding.serviceFlag("GREETER"),
                        DrehmalContextualOnboarding.HUB_MENU_VIEWED),
                roles);
        assertTrue(returning.objective().contains("돌아가"));
    }

    @Test
    void lockedOrUnavailableServicesNeverBecomeMandatoryObjectives() {
        var lockedSummon = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE",
                Set.of("CV_FIRST_COMMON"),
                Set.of(DrehmalContextualOnboarding.HUB_MENU_VIEWED),
                Set.of("SUMMON"));
        assertFalse(lockedSummon.objective().contains("정령의 흔적"));

        var unavailableForge = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE",
                Set.of(DrehmalContentUnlocks.DRABYEL_ROAD),
                Set.of(DrehmalContextualOnboarding.HUB_MENU_VIEWED),
                Set.of("MARKET"));
        assertTrue(unavailableForge.objective().contains("장비 상인"));
        assertTrue(unavailableForge.hint().contains("구매"));
        assertFalse(unavailableForge.hint().contains("대장간"));
    }

    @Test
    void firstRouteGuidanceMovesFromTowerToCampToDrabyelApproach() {
        var tower = DrehmalContextualOnboarding.resolve(
                "",
                Set.of("CV_FIRST_COMMON"),
                Set.of(),
                Set.of());
        assertTrue(tower.objective().contains("캐피털 밸리 탑"));

        var camp = DrehmalContextualOnboarding.resolve(
                "",
                Set.of("CV_FIRST_COMMON"),
                Set.of(DrehmalFirstRouteProgress.TOWER_REACHED),
                Set.of());
        assertTrue(camp.objective().contains("야영지"));
        assertTrue(camp.hint().contains("선택"));

        var road = DrehmalContextualOnboarding.resolve(
                "",
                Set.of("CV_FIRST_COMMON"),
                Set.of(
                        DrehmalFirstRouteProgress.TOWER_REACHED,
                        DrehmalFirstRouteProgress.CAMP_REACHED),
                Set.of());
        assertTrue(road.objective().contains("진입로"));

        var hub = DrehmalContextualOnboarding.resolve(
                "",
                Set.of("CV_FIRST_COMMON"),
                Set.of(
                        DrehmalFirstRouteProgress.TOWER_REACHED,
                        DrehmalFirstRouteProgress.CAMP_REACHED,
                        DrehmalFirstRouteProgress.APPROACH_REACHED),
                Set.of());
        assertTrue(hub.objective().contains("뉴 드라비엘"));
        assertFalse(hub.objective().contains("캐피털 밸리 탑"));
    }

    @Test
    void worldBossGuidanceStaysOptionalEvenAfterHubProgress() {
        var active = DrehmalContextualOnboarding.resolve(
                DrehmalWorldBossPlacementRules.SITE_KIND,
                Set.of("CV_FIRST_COMMON"),
                Set.of(DrehmalFirstRouteProgress.HUB_REACHED),
                Set.of());
        assertTrue(active.objective().contains("그라울"));
        assertTrue(active.objective().contains("거나"));
        assertTrue(active.hint().contains("선택"));

        var cleared = DrehmalContextualOnboarding.resolve(
                DrehmalWorldBossPlacementRules.SITE_KIND,
                Set.of(DrehmalWorldBossPlacementRules.ENCOUNTER_ID),
                Set.of(DrehmalFirstRouteProgress.HUB_REACHED),
                Set.of());
        assertTrue(cleared.objective().contains("위협은 사라"));
        assertTrue(cleared.hint().contains("다시 나타나지"));
    }

    @Test
    void warningCaveStaysOptionalAndRouteProgressDoesNotRewind() {
        var cave = DrehmalContextualOnboarding.resolve(
                "ELITE_ZONE",
                Set.of("CV_FIRST_COMMON"),
                Set.of(),
                Set.of());
        assertTrue(cave.hint().contains("피해"));
        assertTrue(cave.objective().contains("뉴 드라비엘"));

        var afterRoad = DrehmalContextualOnboarding.resolve(
                "BREATHING_ZONE",
                Set.of(DrehmalContentUnlocks.DRABYEL_ROAD),
                Set.of(),
                Set.of());
        assertTrue(afterRoad.objective().contains("뉴 드라비엘로 들어가"));
        assertFalse(afterRoad.objective().contains("캐피털 밸리 탑"));
    }
}
