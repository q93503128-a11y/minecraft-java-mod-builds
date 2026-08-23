package kr.moonseungjun.arcanecircle.magic;

/** Mechanical grimoire summaries for the fifth-circle battlefield-command audit. */
public final class FifthCircleSpellSummary {
    private FifthCircleSpellSummary() {}

    public static String summary(String spellId) {
        if (spellId == null) return "";
        return switch (spellId) {
            case "cone_of_cold" -> "즉발 방향성 냉기 원뿔 · 멀어질수록 폭이 넓어지며 범위 내 적을 동결·둔화하고 화염을 끕니다.";
            case "wall_of_force" -> "12초 물리 역장벽 · 적대 생명체의 통과와 벽을 가로지르는 적대 Arcane 주문 궤적을 실제로 차단합니다.";
            case "cloudkill" -> "11초 이동 독성 전선 · 시전 방향으로 천천히 전진하며 반복 피해·독·쇠약을 주고 약해진 적에게 추가 압박을 가합니다.";
            case "telekinesis" -> "5초 유지 염동력 · 대상을 시선 앞에 실제로 붙잡아 이동시키고 종료 순간 현재 시선 방향으로 강하게 던집니다.";
            case "flame_strike" -> "4초 천공 화염기둥 · 최초 강한 수직 타격과 지형 파괴 후 같은 지점에 고정되어 0.5초마다 내부 적을 다시 태웁니다.";
            case "hold_monster" -> "일반 대상 약 15초 완전 속박 · 초대형/보스급도 약 7초는 이동·공격·Arcane 시전이 봉쇄됩니다.";
            case "mass_cure_wounds" -> "주변 아군·자신·소유 길들인 생명체를 동시에 즉시 회복하고 짧은 재생 효과를 부여하는 전장 광역 회복입니다.";
            case "passwall" -> "실제 벽 블록을 임시 제거해 통과 가능한 터널을 만들고 약 12초 뒤 내부가 비면 원래 블록 상태로 복구합니다.";
            case "dominate_person" -> "인간형 체급 비플레이어 적 하나의 전투 진영을 30초 탈취 · 주변 위협과 싸우고 비전투 시 시전자를 추종합니다.";
            case "insect_plague" -> "11초 고정 곤충떼 영역 · 반복 피해·둔화·쇠약과 함께 내부 Arcane 집중을 주기적으로 끊고 짧게 재시전을 방해합니다.";
            default -> "";
        };
    }
}
