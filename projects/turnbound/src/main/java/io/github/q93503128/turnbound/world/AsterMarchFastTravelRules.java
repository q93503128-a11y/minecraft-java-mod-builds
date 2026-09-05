package io.github.q93503128.turnbound.world;

import java.util.Set;

/** Pure v0.4 fast-travel activation rules. */
public final class AsterMarchFastTravelRules {
    public record Progress(Set<String> completedQuests, Set<String> unlockFlags, Set<String> clearedEncounters) {
        public Progress {
            completedQuests = Set.copyOf(completedQuests == null ? Set.of() : completedQuests);
            unlockFlags = Set.copyOf(unlockFlags == null ? Set.of() : unlockFlags);
            clearedEncounters = Set.copyOf(clearedEncounters == null ? Set.of() : clearedEncounters);
        }
    }

    private AsterMarchFastTravelRules() {}

    public static boolean canonicalDestination(String id) {
        return switch (id == null ? "" : id) {
            case AsterMarchRegionCatalog.FT_RADIA,
                 AsterMarchRegionCatalog.FT_MEADOW,
                 AsterMarchRegionCatalog.FT_GLOAM,
                 AsterMarchRegionCatalog.FT_AQUEDUCT,
                 AsterMarchRegionCatalog.FT_QUARRY,
                 AsterMarchRegionCatalog.FT_RELAY -> true;
            default -> false;
        };
    }

    public static boolean unlocked(String id, Progress progress) {
        if (progress == null) throw new IllegalArgumentException("Missing fast-travel progress");
        return switch (id == null ? "" : id) {
            case AsterMarchRegionCatalog.FT_RADIA -> true;
            case AsterMarchRegionCatalog.FT_MEADOW ->
                    completed(progress, "MQ_C01_01_patrol")
                            || flag(progress, "FT_MEADOW")
                            || (cleared(progress, "ENC_M01") && cleared(progress, "ENC_M02"));
            case AsterMarchRegionCatalog.FT_GLOAM ->
                    completed(progress, "MQ_C02_01_spores") || flag(progress, "GLOAM_DEEP_PATH");
            case AsterMarchRegionCatalog.FT_AQUEDUCT ->
                    completed(progress, "MQ_C03_01_dry_channel") || flag(progress, "AQUEDUCT_LOWER");
            case AsterMarchRegionCatalog.FT_QUARRY ->
                    completed(progress, "MQ_C04_01_ash_route") || flag(progress, "FT_QUARRY");
            case AsterMarchRegionCatalog.FT_RELAY ->
                    completed(progress, "MQ_C05_01_relay_key") || flag(progress, "OLD_RELAY_ENTRANCE");
            default -> false;
        };
    }

    public static String lockedReason(String id) {
        return switch (id == null ? "" : id) {
            case AsterMarchRegionCatalog.FT_MEADOW -> "초원 순찰 임무를 완료하면 이 계전소를 사용할 수 있습니다.";
            case AsterMarchRegionCatalog.FT_GLOAM -> "그늘숲의 포자등불 조사를 완료하면 이 계전소를 사용할 수 있습니다.";
            case AsterMarchRegionCatalog.FT_AQUEDUCT -> "붕괴 수로의 수문 조사를 완료하면 이 계전소를 사용할 수 있습니다.";
            case AsterMarchRegionCatalog.FT_QUARRY -> "재의 길을 확보하면 이 계전소를 사용할 수 있습니다.";
            case AsterMarchRegionCatalog.FT_RELAY -> "세 지역 Relay 조각을 제출해 구 중계소 접근로를 복원해야 합니다.";
            default -> "해당 계전소는 아직 활성화되지 않았습니다.";
        };
    }

    private static boolean completed(Progress progress, String questId) {
        return progress.completedQuests().contains(questId);
    }

    private static boolean flag(Progress progress, String flag) {
        return progress.unlockFlags().contains(flag);
    }

    private static boolean cleared(Progress progress, String encounterId) {
        return progress.clearedEncounters().contains(encounterId);
    }
}
