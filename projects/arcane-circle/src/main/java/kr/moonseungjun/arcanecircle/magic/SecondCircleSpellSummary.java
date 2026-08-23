package kr.moonseungjun.arcanecircle.magic;

/** Alpha.73 exact second-circle gameplay contract used by the grimoire. */
public final class SecondCircleSpellSummary {
    private SecondCircleSpellSummary() {}

    public static String summary(String id) {
        if (id == null) return "";
        return switch (id) {
            case "scorching_ray" -> "단일 대상을 추적하는 3연속 화염 광선 · 첫 타격 후 0.5초 간격으로 2회 추가 타격 + 각 타격 화상";
            case "misty_step" -> "최대 약 12m 단거리 안전 이동 · 출발~도착 사이 열린 통로가 필요하며 고체 벽은 관통할 수 없음";
            case "web" -> "11초 반경 약 4.2~7.5m 고정 포박장 · 0.2초마다 적의 수평 속도를 강제 감쇠하고 강한 둔화·약화를 재적용";
            case "mirror_image" -> "13초 동안 적대 직접 공격 3회를 환영 3체가 확정 대리 · 환경/비공격성 피해에는 소모되지 않음";
            case "invisibility" -> "21초 은신 · 주변 적대 추적 즉시 해제 + 첫 적대 직접 공격 궤적 1회를 흘린 뒤 은신 종료";
            case "gust_of_wind" -> "전방 약 8~18m 직선 강풍으로 적을 강제 밀침 · 플레이어 시전은 거미줄/불/횃불 같은 취약 오브젝트도 제거";
            case "hold_person" -> "일반 인간형 체급 대상을 9초 완전 속박 · 이동/공격/Arcane 시전 봉쇄 · 대형/보스급 면역";
            case "shatter" -> "조준 지점 반경 약 4~6.5m 진동 폭발 · 광역 피해 + 플레이어 시전은 유리/얼음/자수정 같은 취성 재료 파괴";
            case "blur" -> "18초 동안 적대 직접 공격이 매번 35% 확률로 빗나감 · Mirror Image와 달리 충전 수 제한 없이 확률 판정";
            case "levitate" -> "자신·아군 또는 일반 체급 대상 하나를 3초 상승시킨 뒤 4초 정점에 붙잡아 두고 종료 후 4초 안전 하강";
            default -> "";
        };
    }
}
