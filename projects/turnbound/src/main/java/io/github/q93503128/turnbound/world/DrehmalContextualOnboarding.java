package io.github.q93503128.turnbound.world;

import java.util.Locale;
import java.util.Set;

/**
 * Pure first-route guidance rules.
 *
 * <p>Progress beats physical proximity: walking backward must not rewind the player's objective. Hub onboarding
 * exposes one useful action at a time and only points at services that are actually materialized in the surveyed
 * production world.</p>
 */
final class DrehmalContextualOnboarding {
    static final String HUB_MENU_VIEWED = "HUB_MENU_VIEWED";
    static final String HUB_ROUTE_REVIEWED = "HUB_ROUTE_REVIEWED";
    private static final String FIRST_COMMON = "CV_FIRST_COMMON";

    record Guidance(String objective, String hint) {
        Guidance {
            objective = objective == null ? "" : objective.trim();
            hint = hint == null ? "" : hint.trim();
        }
    }

    private DrehmalContextualOnboarding() {}

    static Guidance resolve(
            String locationKind,
            Set<String> clearedEncounters,
            Set<String> onboardingFlags,
            Set<String> availableHubRoles
    ) {
        String kind = locationKind == null ? "" : locationKind.trim();
        Set<String> clears = clearedEncounters == null ? Set.of() : clearedEncounters;
        Set<String> flags = onboardingFlags == null ? Set.of() : onboardingFlags;
        Set<String> roles = availableHubRoles == null ? Set.of() : availableHubRoles;

        if (DrehmalWorldBossPlacementRules.SITE_KIND.equals(kind)) {
            if (clears.contains(DrehmalWorldBossPlacementRules.ENCOUNTER_ID)) {
                return new Guidance(
                        "들판의 위협은 사라졌습니다. 뉴 드라비엘로 향하거나 다음 길을 살피십시오.",
                        "그라울은 쓰러진 뒤 필드에서 다시 나타나지 않습니다.");
            }
            return new Guidance(
                    "들이받는 왕 그라울과 맞서거나 길을 따라 뉴 드라비엘로 향하십시오.",
                    "선택 전투입니다. 몸을 낮추는 예고 동작 뒤의 돌진 방향을 보고 피하십시오.");
        }

        if ("HUB_SAFE".equals(kind) || DrehmalFirstRouteProgress.reached(flags, DrehmalFirstRouteProgress.HUB_REACHED)) {
            return hubGuidance(clears, flags, roles);
        }

        if (clears.contains(DrehmalContentUnlocks.DRABYEL_ROAD)) {
            return new Guidance(
                    "뉴 드라비엘로 들어가 여정을 정비하십시오.",
                    "마을 안에서는 서두르지 않아도 됩니다.");
        }

        if ("ELITE_ZONE".equals(kind)) {
            return new Guidance(
                    "경고 동굴의 강적을 살피거나 뉴 드라비엘로 향하십시오.",
                    "이 강적을 피해 길을 계속 가도 됩니다.");
        }

        if ("REST_ZONE".equals(kind)) {
            return new Guidance(
                    "야영지에서 장비를 한 번 비교한 뒤 뉴 드라비엘로 향하십시오.",
                    "여기서는 잠시 쉬어도 됩니다. 준비가 되면 진입로를 따라가십시오.");
        }

        if (!clears.contains(FIRST_COMMON)) {
            if ("ENCOUNTER_ZONE".equals(kind)) {
                return new Guidance(
                        "길가의 이끼등 멧돼지를 넘고 뉴 드라비엘로 향하십시오.",
                        "적이 경계하기 시작하면 곧 전투가 이어집니다.");
            }
            return new Guidance("길을 따라 뉴 드라비엘로 향하십시오.", "");
        }

        if (!DrehmalFirstRouteProgress.reached(flags, DrehmalFirstRouteProgress.TOWER_REACHED)) {
            return new Guidance(
                    "캐피털 밸리 탑를 따라 길의 중간 지점을 확인하십시오.",
                    "첫 전투가 끝났습니다. 이제 길의 큰 표식을 기준으로 이동하십시오.");
        }

        if ("BREATHING_ZONE".equals(kind)
                || !DrehmalFirstRouteProgress.reached(flags, DrehmalFirstRouteProgress.CAMP_REACHED)) {
            return new Guidance(
                    "탐험가 안내서 야영지 쪽으로 길을 이어가십시오.",
                    DrehmalContentUnlocks.summonUnlocked(clears)
                            ? "경고 동굴의 강적은 선택입니다. 넘겼다면 정령의 흔적이 반응하기 시작합니다."
                            : "경고 동굴의 강적은 보상이 크지만 선택입니다. 그대로 길을 이어가도 됩니다.");
        }

        if (!DrehmalFirstRouteProgress.reached(flags, DrehmalFirstRouteProgress.APPROACH_REACHED)) {
            return new Guidance(
                    "뉴 드라비엘 진입로를 따라 마을로 향하십시오.",
                    "진입로의 순찰대를 상대하거나 안전하게 지나갈 길을 살피십시오.");
        }

        return new Guidance(
                "뉴 드라비엘로 들어가 여정을 정비하십시오.",
                DrehmalContentUnlocks.summonUnlocked(clears)
                        ? "마을 안에서 정령의 흔적이 어디에 반응하는지 살펴보십시오."
                        : "마을 안에서는 서두르지 않아도 됩니다.");
    }

    static String serviceFlag(String role) {
        String clean = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        return clean.isBlank() ? "" : "HUB_SERVICE_" + clean;
    }

    private static Guidance hubGuidance(Set<String> clears, Set<String> flags, Set<String> roles) {
        String objective = "뉴 드라비엘에서 준비를 마치고 다음 길을 확인하십시오.";
        if (!flags.contains(HUB_MENU_VIEWED)) {
            return new Guidance(objective, "E 메뉴에서 파티와 장비를 확인할 수 있습니다.");
        }
        if (roles.isEmpty()) {
            return new Guidance(
                    objective,
                    flags.contains(HUB_ROUTE_REVIEWED)
                            ? ""
                            : "M 지도를 열어 발견한 길과 다음 이동 방향을 확인하십시오.");
        }
        if (needs("BLACKSMITH", flags, roles)) {
            return new Guidance(objective, "장비를 손볼 필요가 있다면 대장간을 이용할 수 있습니다.");
        }
        if (needs("MARKET", flags, roles)) {
            return new Guidance(objective, "시장에서는 다음 여정에 필요한 장비를 확인할 수 있습니다.");
        }
        if (needs("TRAVEL", flags, roles)) {
            return new Guidance(objective, "마구간과 지도에서 발견한 이동 거점을 확인할 수 있습니다.");
        }
        if (DrehmalContentUnlocks.summonUnlocked(clears) && needs("SUMMON", flags, roles)) {
            return new Guidance(objective, "캐피털 밸리의 강적을 넘겼다면 정령의 흔적이 반응합니다.");
        }
        return new Guidance(objective, "");
    }

    private static boolean needs(String role, Set<String> flags, Set<String> roles) {
        return roles.contains(role) && !flags.contains(serviceFlag(role));
    }
}
