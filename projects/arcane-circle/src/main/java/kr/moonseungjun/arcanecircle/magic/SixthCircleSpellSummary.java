package kr.moonseungjun.arcanecircle.magic;

/** Mechanical grimoire summaries for the alpha.58 sixth-circle deep pass. */
public final class SixthCircleSpellSummary {
    private SixthCircleSpellSummary() {}

    public static String summary(String spellId) {
        if (spellId == null) return "";
        return switch (spellId) {
            case "disintegrate" -> "고정된 가는 직선 분해광선 · 관통 생명체에 고화력 피해/쇠약 + 같은 광선 경로의 실제 물질 파괴";
            case "globe_of_invulnerability" -> "26초 이동 구체 · 외부에서 안으로 들어오는 적대 1~5써클 Arcane 주문을 경계에서 소거 · 6써클 이상/물리 공격 통과";
            case "mass_suggestion" -> "8초 범위 정신 명령 · 대상들이 공격을 끊고 실제로 전장에서 후퇴하며 Arcane 시전도 억제";
            case "move_earth" -> "고정 지면 대형 대지 융기 · 거리 감쇠 피해 + 적을 바깥/위로 강제 이동 + 동일 중심 실제 지형 변형";
            case "sunbeam" -> "넓은 직선 관통 태양광 · 복수 대상 피해 + 화상·장기 실명·발광";
            case "true_seeing" -> "60초 진실의 시야 · 주변 은신을 주기적으로 벗기고 생명 반응을 지속 추적";
            case "freezing_sphere" -> "고정 착탄점 대형 극저온 폭발 · 거리 감쇠 냉기 피해 + 화염 소거·초강력 동결·둔화";
            case "eyebite" -> "단일 정신 피해 + 18초 공포·쇠약·암흑 · 비플레이어 대상은 즉시 전투 거리를 벌리려 함";
            case "flesh_to_stone" -> "단일 피해 + 약 18초 완전 석화 · 이동·공격·Arcane 시전 봉쇄 + 석질 육체의 피해 저항";
            case "circle_of_death" -> "대형 생명 파동 · 일반 대상 범위 피해 + 낮은 체력의 보통 체급 적에게 강한 처형 압박 · 대형/보스급은 처형 제외";
            default -> "";
        };
    }
}
