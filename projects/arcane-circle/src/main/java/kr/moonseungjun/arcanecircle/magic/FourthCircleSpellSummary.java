package kr.moonseungjun.arcanecircle.magic;

/** Alpha.71 authoritative fourth-circle mechanics shown in the effect compendium. */
public final class FourthCircleSpellSummary {
    private FourthCircleSpellSummary() {}

    public static String summary(String id) {
        return switch (id) {
            case "wall_of_fire" -> "12초 실제 화염 장벽 · 벽을 스치거나 통과하는 적에게 0.5초마다 피해와 연소를 반복합니다.";
            case "ice_storm" -> "6초 고정 우박 제압구역 · 0.5초마다 냉기 충격을 가하고 동결·둔화와 반복 강제 하강으로 공중 이동을 억제합니다.";
            case "greater_invisibility" -> "39초 전투 투명화 · 공격해도 유지되고 적대 추적을 계속 끊으며 적대 직접 공격은 45% 확률로 빗나갑니다.";
            case "resilient_sphere" -> "20초 완전 격리막 · 안팎의 피해를 모두 차단하고 내부 Arcane 시전도 봉쇄합니다.";
            case "dimension_door" -> "최대 약 36m 안전 공간 이동 · 3m 안의 웅크린 플레이어 1명을 함께 이동시킬 수 있습니다.";
            case "stoneskin" -> "38초 · 적이 가하는 비마법 물리 공격만 50% 경감하며 화염·Arcane·환경 피해는 그대로 받습니다.";
            case "confusion" -> "12초 범위 정신 교란 · 매초 정지·배회·오인공격·비틀림 중 하나로 행동을 바꾸고 Arcane 시전을 간헐적으로 끊습니다.";
            case "blight" -> "단일 생명 쇠퇴 · 즉시 피해 뒤 8초간 추가 쇠퇴 피해를 주고 받는 치유량을 80% 감소시킵니다.";
            case "freedom_of_movement" -> "26초 · 둔화·속박·동결·강제부양을 계속 정화하고 하위 이동 제어의 Arcane 시전 봉쇄를 무시합니다.";
            case "phantasmal_killer" -> "14초 단일 공포 결속 · 대상은 시전자에게서 강제로 도주하고 시전자에게 직접 피해를 줄 수 없으며 2초마다 정신 피해를 받습니다.";
            default -> "";
        };
    }
}
