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

        if ("HUB_SAFE".equals(kind)) return hubGuidance(clears, flags, roles);

        if (clears.contains(DrehmalContentUnlocks.DRABYEL_ROAD)) {
            return new Guidance(
                    "New Drabyel로 들어가 여정을 정비하십시오.",
                    "마을 안에서는 서두르지 않아도 됩니다.");
        }

        if ("ELITE_ZONE".equals(kind)) {
            return new Guidance(
                    "경고 동굴의 강적을 살피거나 New Drabyel로 향하십시오.",
                    "이 강적을 피해 길을 계속 가도 됩니다.");
        }

        if ("REST_ZONE".equals(kind)) {
            return new Guidance(
                    "야영지에서 숨을 돌린 뒤 New Drabyel로 향하십시오.",
                    "필요하면 장비를 확인하고, 준비가 되면 길을 이어가십시오.");
        }

        if ("BREATHING_ZONE".equals(kind)) {
            return new Guidance(
                    "Capital Valley Tower를 지나 New Drabyel로 향하십시오.",
                    "경고 동굴은 선택입니다. 그대로 마을로 향해도 됩니다.");
        }

        if (!clears.contains(FIRST_COMMON)) {
            if ("ENCOUNTER_ZONE".equals(kind)) {
                return new Guidance(
                        "길가의 이끼등 멧돼지를 넘고 New Drabyel로 향하십시오.",
                        "적이 경계하기 시작하면 곧 전투가 이어집니다.");
            }
            return new Guidance("길을 따라 New Drabyel로 향하십시오.", "");
        }

        return new Guidance(
                "Capital Valley Tower를 지나 New Drabyel로 향하십시오.",
                DrehmalContentUnlocks.summonUnlocked(clears)
                        ? "강적을 넘긴 뒤 정령의 흔적이 반응하기 시작했습니다."
                        : "");
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
        if (needs("BLACKSMITH", flags, roles)) {
            return new Guidance(
                    "New Drabyel의 대장간에서 장비를 점검하십시오.",
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
                    "Capital Valley의 강적을 넘긴 뒤 흔적이 깨어났습니다.");
        }
        return new Guidance(
                "New Drabyel에서 준비를 마치고 다음 길을 확인하십시오.",
                "필요한 일만 마친 뒤 다시 길 위로 나가면 됩니다.");
    }

    private static boolean needs(String role, Set<String> flags, Set<String> roles) {
        return roles.contains(role) && !flags.contains(serviceFlag(role));
    }
}
