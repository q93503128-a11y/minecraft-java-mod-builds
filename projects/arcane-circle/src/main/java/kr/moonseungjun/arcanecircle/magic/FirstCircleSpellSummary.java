package kr.moonseungjun.arcanecircle.magic;

/** Alpha.74 exact first-circle gameplay contract used by the grimoire. */
public final class FirstCircleSpellSummary {
    private FirstCircleSpellSummary() {}

    public static String summary(String spellId) {
        return switch (spellId) {
            case "magic_missile" -> "조준한 생명체에 3발이 수렴하는 추적 비전 탄환 · 한 번의 합산 피해로 판정되어 저써클 정밀 필살 역할 유지";
            case "fire_bolt" -> "비유도 화염탄 · 보이는 착탄점의 생명체에 단일 피해 + 화상 · 추적탄과 달리 실제 착탄 위치가 중요";
            case "ray_of_frost" -> "단발 냉기 광선 피해 · 동결 + 강한 둔화 · 채널 다단히트 없이 즉시 냉각 제어";
            case "shield" -> "약 8.5초 내 다음 충격 2회를 반응 방벽이 고정량 흡수 · 2장 소진 즉시 종료 · 플레이어/NPC 동일 규칙";
            case "feather_fall" -> "6초 안정 낙하 · 즉시 누적 추락거리 초기화 · Dispel/사망/차원이동 시 해당 낙하 권능만 정리";
            case "light" -> "90초 야간 시야 + 시전자 주변을 따라 이동하는 실제 임시 광원 5점 · 플레이어/NPC 모두 실제 월드 광원";
            case "grease" -> "8초 단일 유지 미끄럼 영역 · 보이는 원형 반경 안에서만 약한 둔화 + 횡미끄러짐 · 재시전 시 이전 장판 교체";
            case "sleep" -> "일반 체급 적만 최대 7초 원형 광역 수면 · 플레이어/NPC 동일 반경 · AI/이동/시전 정지 · 실제 피해가 남으면 즉시 각성";
            case "thunderwave" -> "전방 부채꼴 충격파 피해 + 넉백 · 플레이어만 보이는 경로의 취약 블록을 조건부 파손하고 NPC는 지형 보존";
            case "mage_armor" -> "36초 · 4장 재생형 아케인 플레이트가 피해를 분산하고 4.5초마다 재충전 · 플레이어/NPC 동일 규칙";
            default -> "";
        };
    }
}
