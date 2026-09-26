package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalContextualOnboardingTest {
    @Test
    void hubTeachesOneUsefulSurfaceAtATime() {
        Set<String> roles = Set.of("BLACKSMITH", "MARKET", "TRAVEL", "SUMMON");

        var menu = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE",
                Set.of(DrehmalContentUnlocks.DRABYEL_ROAD),
                Set.of(),
                roles);
        assertTrue(menu.objective().contains("파티"));

        var forge = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE",
                Set.of(DrehmalContentUnlocks.DRABYEL_ROAD),
                Set.of(DrehmalContextualOnboarding.HUB_MENU_VIEWED),
                roles);
        assertTrue(forge.objective().contains("대장간"));
        assertFalse(forge.objective().contains("상인"));

        var market = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE",
                Set.of(DrehmalContentUnlocks.DRABYEL_ROAD),
                Set.of(
                        DrehmalContextualOnboarding.HUB_MENU_VIEWED,
                        DrehmalContextualOnboarding.serviceFlag("BLACKSMITH")),
                roles);
        assertTrue(market.objective().contains("상인"));

        var travel = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE",
                Set.of(DrehmalContentUnlocks.DRABYEL_ROAD),
                Set.of(
                        DrehmalContextualOnboarding.HUB_MENU_VIEWED,
                        DrehmalContextualOnboarding.serviceFlag("BLACKSMITH"),
                        DrehmalContextualOnboarding.serviceFlag("MARKET")),
                roles);
        assertTrue(travel.objective().contains("마구간"));

        var summon = DrehmalContextualOnboarding.resolve(
                "HUB_SAFE",
                Set.of(DrehmalContentUnlocks.DRABYEL_ROAD),
                Set.of(
                        DrehmalContextualOnboarding.HUB_MENU_VIEWED,
                        DrehmalContextualOnboarding.serviceFlag("BLACKSMITH"),
                        DrehmalContextualOnboarding.serviceFlag("MARKET"),
                        DrehmalContextualOnboarding.serviceFlag("TRAVEL")),
                roles);
        assertTrue(summon.objective().contains("정령의 흔적"));
    }

    @Test
    void hubReachedFlagUsesHubGuidanceEvenWhenRouteSiteIsNotPromoted() {
        var guidance = DrehmalContextualOnboarding.resolve(
                "",
                Set.of(),
                Set.of(DrehmalFirstRouteProgress.HUB_REACHED),
                Set.of());
        assertTrue(guidance.objective().contains("파티"));
        assertFalse(guidance.objective().contains("길을 따라"));
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
        assertTrue(unavailableForge.objective().contains("상인"));
        assertFalse(unavailableForge.objective().contains("대장간"));
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
