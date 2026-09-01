package io.github.q93503128.turnbound.content;

/** Presentation-only names for challenge objectives. Canonical ids, rules and rewards remain untouched. */
public final class ChallengePresentationText {
    private ChallengePresentationText() {}

    public static String label(int ordinal, String fallback) {
        return switch (ordinal) {
            case 1 -> "전투불능 없이 승리 1";
            case 2 -> "전투불능 없이 승리 2";
            case 3 -> "아군 행동 12회 미만으로 승리";
            case 4 -> "아군 행동 20회 미만으로 승리";
            case 5 -> "1회 부활 후 승리";
            case 6 -> "반격 5회";
            case 7 -> "추격 6회";
            case 8 -> "행동 게이지 지연 합계 800";
            case 9 -> "보호막으로 피해 1500 흡수";
            case 10 -> "한 전투에서 2000 회복";
            case 11 -> "아군 HP 10% 미만 상태로 승리";
            case 12 -> canonicalName("E003", "특수 적") + " 폭발 전에 처치";
            case 13 -> canonicalName("E003", "특수 적") + " 폭발에서 생존";
            case 14 -> "엘리트 적을 부활 없이 격파";
            case 15 -> canonicalName("B01", "보스 1") + " 하드 격파";
            case 16 -> canonicalName("B02", "보스 2") + " 하드 격파";
            case 17 -> canonicalName("B03", "보스 3") + " 하드 격파";
            case 18 -> canonicalName("B04", "보스 4") + " 하드 격파";
            case 19 -> canonicalName("B05", "보스 5") + " 하드 격파";
            case 20 -> "균열 관문 30층 클리어";
            default -> fallback == null || fallback.isBlank() ? "도전 " + ordinal : fallback;
        };
    }

    private static String canonicalName(String id, String fallback) {
        return CanonicalData.contains(id) ? CanonicalData.definition(id).name() : fallback;
    }
}
