package kr.moonseungjun.arcanecircle.magic;

/** Mechanical grimoire summaries for the sixth-circle Grand Archmage value pass. */
public final class SixthCircleSpellSummary {
    private SixthCircleSpellSummary() {}

    public static String summary(String spellId) {
        if (spellId == null) return "";
        return switch (spellId) {
            case "disintegrate" -> "고정된 가는 직선 분해광선 · 관통 생명체에 고화력 피해/쇠약 + 같은 광선 경로의 실제 물질 파괴";
            case "globe_of_invulnerability" -> "26초 이동 구체 · 외부에서 안으로 들어오는 적대 1~5써클 Arcane 주문을 경계에서 소거 · 6써클 이상/물리 공격 통과";
            case "mass_suggestion" -> "20초 범위 정신 명령 · 다수 대상이 공격과 Arcane 시전을 끊고 실제로 전장을 이탈하는 집단 퇴각 권능";
            case "move_earth" -> "약 20~36m 방향성 지형공학 · 중앙 참호를 파고 기존 표면 블록을 양측 토루로 실제 이동 · NPC 사용 시에는 지형 훼손 없이 같은 전장 분할 압박";
            case "sunbeam" -> "6초간 월드 좌표에 고정되는 1.55블록 반폭 관통 태양 회랑 · 0.5초마다 반복 피해/화상/실명/발광";
            case "true_seeing" -> "60초 진실의 시야 · 주변 은신을 주기적으로 벗기고 생명 반응을 지속 추적";
            case "freezing_sphere" -> "10초간 유지되는 반경 약 10.5~15.5m 극저온 봉쇄영역 · 0.5초마다 화염 소거/재동결/강한 둔화/채굴 피로";
            case "eyebite" -> "단일 정신 피해 + 18초 공포·쇠약·암흑 · 비플레이어 대상은 효과가 끝날 때까지 강제 도주하며 Arcane 시전도 중단";
            case "flesh_to_stone" -> "단일 피해 + 약 18초 완전 석화 · 이동·공격·Arcane 시전 봉쇄 + 석질 육체의 피해 저항";
            case "circle_of_death" -> "대형 생명 침식 영역 · 다수의 현재 생명력과 전투력을 동시에 깎는 6써클 군세 제압기 · 처형 판정은 전혀 없고 즉사 권능도 없습니다.";
            default -> "";
        };
    }
}
