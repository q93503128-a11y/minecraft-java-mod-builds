package kr.moonseungjun.arcanecircle.magic;

/** Alpha.72 exact third-circle gameplay contract used by the grimoire. */
public final class ThirdCircleSpellSummary {
    private ThirdCircleSpellSummary() {}

    public static String summary(String id) {
        if (id == null) return "";
        return switch (id) {
            case "fireball" -> "고정 착탄점 화염 폭발 · 중심 강피해/거리 감쇠 + 화상 + 플레이어 시전 시 주변 취약 지형 파괴";
            case "lightning_bolt" -> "시전점~고정 목표를 잇는 관통 번개선 · 경로의 복수 대상을 같은 번개로 타격 + 약한 지형 파손";
            case "fly" -> "30초 실제 자유 비행 권한 · 종료/해제 시 기존 비행 권한 복원 + 안전 낙하";
            case "haste" -> "30초 Arcane 템포 가속 · 플레이어와 NPC 모두 시전시간 28% 단축 + 재사용 대기 15% 단축 + 이동 가속";
            case "dispel_magic" -> "대상에게 유지 중인 1~3써클 강화·제어 마법만 확정 해제 · 4써클 이상 권능은 보존 · 대상이 없으면 자신의 해로운 상태 정화";
            case "vampiric_touch" -> "10m 이내 단일 생명력 흡수 · 실제로 감소시킨 체력+흡수량의 60%만큼 시전자 회복";
            case "slow" -> "9초 반경 약 5~9m 시간왜곡 구역 · 0.2초 간격으로 강한 둔화·약화·채굴 피로를 재적용";
            case "protection_from_energy" -> "30초 5중 공명막 · Arcane/화염/투사체성 충격만 45% 경감 · 소모막은 3.5초마다 재충전";
            case "sleet_storm" -> "9초 반경 약 6.5~10.5m 진눈깨비 구역 · 0.5초마다 냉기/동결/암흑/미끄럼 압박 + 내부 적대 Arcane 시전 봉쇄";
            case "blink" -> "최대 약 20m 1인 위상 통과 · 출발~종착 사이 고체 지형은 무시하고 종착점만 안전 공간을 요구 · 착지 후 2초 위상 잔류막";
            default -> "";
        };
    }
}
