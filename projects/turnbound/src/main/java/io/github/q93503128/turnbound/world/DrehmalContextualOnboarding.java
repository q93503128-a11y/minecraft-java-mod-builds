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
        if (!flags.contains(HUB_MENU_VIEWED)) {
            return new Guidance(
                    "파티와 장비를 한 번 확인하십시오.",
                    "E 메뉴에서 현재 파티를 바로 확인할 수 있습니다.");
        }
        if (roles.isEmpty()) {
            return new Guidance(
                    "뉴 드라비엘에서 파티와 장비를 정리하고 다음 길을 확인하십시오.",
                    "E 메뉴에서 캐릭터와 장비를 정리한 뒤 M 지도에서 다음 경로를 확인하십시오.");
        }
        if (needs("BLACKSMITH", flags, roles)) {
            return new Guidance(
                    "뉴 드라비엘의 대장간에서 장비를 점검하십시오.",
                    "새 장비를 얻었다면 여기서 바로 손볼 수 있습니다.");
        }
        if (needs("MARKET", flags, roles)) {
            return new Guidance(
                    "시장 쪽 장비 상인에게 들러 보십시오.",
                    "먼 길을 나서기 전에 필요한 장비를 확인해 두십시오.");
        }
        if (needs("TRAVEL", flags, roles)) {
            return new Guidance(
                    "마구간에서 다음 길과 지도를 확인하십시오.",
                    "확인된 길과 표식만 지도에 이어집니다.");
        }
        if (DrehmalContentUnlocks.summonUnlocked(clears) && needs("SUMMON", flags, roles)) {
            return new Guidance(
                    "마을 안에서 반응하는 정령의 흔적을 찾아보십시오.",
                    "캐피털 밸리의 강적을 넘긴 뒤 흔적이 깨어났습니다.");
        }
        return new Guidance(
                "뉴 드라비엘에서 준비를 마치고 다음 길을 확인하십시오.",
                "필요한 일만 마친 뒤 다시 길 위로 나가면 됩니다.");
    }

    private static boolean needs(String role, Set<String> flags, Set<String> roles) {
        return roles.contains(role) && !flags.contains(serviceFlag(role));
    }
}
