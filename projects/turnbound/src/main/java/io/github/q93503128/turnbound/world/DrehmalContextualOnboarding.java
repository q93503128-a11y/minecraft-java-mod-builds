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
        if (needs("GREETER", flags, roles)) {
            return new Guidance(
                    "마을 입구 안내와 대화해 뉴 드라비엘의 시설과 다음 길을 확인하십시오.",
                    "입구 안내 앞에서 오른쪽 버튼을 눌러 대화하십시오.");
        }
        if (!flags.contains(HUB_MENU_VIEWED)) {
            return new Guidance(
                    "E 메뉴를 열어 현재 파티와 장비 상태를 확인하십시오.",
                    "필요한 정비를 끝낸 뒤 마을 시설을 둘러보면 됩니다.");
        }
        if (needs("BLACKSMITH", flags, roles)) {
            return new Guidance(
                    "대장장이를 찾아 장비를 확인하거나 강화하십시오.",
                    "대장간은 마을 안쪽의 대장장이 앞에서 이용할 수 있습니다.");
        }
        if (needs("MARKET", flags, roles)) {
            return new Guidance(
                    "장비 상인을 찾아 다음 여정에 필요한 장비를 확인하십시오.",
                    "구매가 필요 없다면 확인만 하고 지나가도 됩니다.");
        }
        if (needs("TRAVEL", flags, roles)) {
            return new Guidance(
                    "마구간을 찾아 이동 거점과 길의 방향을 확인하십시오.",
                    "마구간에서 지도를 열어 발견한 이동 거점을 확인할 수 있습니다.");
        }
        if (DrehmalContentUnlocks.summonUnlocked(clears) && needs("SUMMON", flags, roles)) {
            return new Guidance(
                    "정령의 흔적을 찾아 새 동료를 부를 수 있는지 확인하십시오.",
                    "캐피털 밸리의 강적을 넘겼다면 소환이 열려 있습니다.");
        }
        if (!flags.contains(HUB_ROUTE_REVIEWED)) {
            return new Guidance(
                    "M 지도를 열어 뉴 드라비엘 서쪽의 다음 길을 확인하십시오.",
                    "지도에는 현재 위치, 목적지, 발견한 이동 거점이 표시됩니다.");
        }
        return new Guidance(
                "준비가 끝났다면 뉴 드라비엘 서쪽 출구에서 아브살 방향의 길을 따라가십시오.",
                "마을을 나서기 전 장비와 파티를 다시 확인해도 됩니다.");
    }

    private static boolean needs(String role, Set<String> flags, Set<String> roles) {
        return roles.contains(role) && !flags.contains(serviceFlag(role));
    }
}
