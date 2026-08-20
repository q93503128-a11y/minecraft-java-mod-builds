package kr.moonseungjun.arcanecircle.magic;

/** Mechanical text for the first-circle deep-audit runtime. */
public final class FirstCircleSpellSummary {
    private FirstCircleSpellSummary() {}

    public static String summary(String spellId) {
        return switch (spellId) {
            case "magic_missile" -> "조준한 생명체에 3발이 수렴하는 추적 비전 탄환 · 한 번의 합산 피해로 판정";
            case "fire_bolt" -> "비유도 화염탄 · 보이는 착탄점의 생명체에 단일 피해 + 화상";
            case "ray_of_frost" -> "단발 냉기 광선 피해 · 동결 + 둔화 · 채널 다단히트 없음";
            case "shield" -> "약 8.5초 · 반응 방벽 2장이 다음 충격의 고정 피해를 직접 흡수";
            case "feather_fall" -> "6초 안정 낙하 · 즉시 누적 추락거리 초기화 · 사망/차원이동 시 상태 정리";
            case "light" -> "90초 야간 시야 · 플레이어 주변을 따라 이동하는 실제 임시 광원 5점";
            case "grease" -> "8초 지속 미끄럼 영역 · 반복적인 약한 둔화 + 횡방향 미끄러짐";
            case "sleep" -> "일반 체급 적만 최대 7초 수면 · AI/이동/시전 정지 · 피해를 받으면 즉시 각성";
            case "thunderwave" -> "전방 부채꼴 충격파 피해 + 넉백 · 보이는 경로의 취약 블록 조건부 파손";
            case "mage_armor" -> "36초 · 4장 재생형 아케인 플레이트가 소모·재충전되며 피해를 분산";
            default -> "";
        };
    }
}
