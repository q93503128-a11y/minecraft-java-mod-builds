package io.github.q93503128.turnbound.content;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical v0.4 Signature Trial objectives separated from unresolved encounter authoring.
 *
 * <p>The character wiki defines each hero's Trial objective, but several authoring inputs remain absent.
 * This catalog records the exact missing canon category so runtime/UI code can distinguish a roster-only
 * gap from an unresolved required actor or a real prerequisite contradiction without fabricating content.</p>
 */
public final class SignatureTrialCatalog {
    public enum CanonState { RULES_READY_ROSTER_GAP, CANON_CONTRADICTION }

    public enum GapKind {
        SPECIAL_ELITE_IDENTITY("SPECIAL_ELITE_IDENTITY", false, "특수 엘리트 identity/편성 미정"),
        TRIAL_BOSS_IDENTITY("TRIAL_BOSS_IDENTITY", false, "Trial Boss identity/편성 미정"),
        PROTECTED_NPC_IDENTITY("PROTECTED_NPC_IDENTITY", false, "보호 대상 NPC identity/편성 미정"),
        ENCOUNTER_ROSTER("ENCOUNTER_ROSTER", true, "전투 편성만 미정"),
        PREREQUISITE_CONTRADICTION("PREREQUISITE_CONTRADICTION", false, "정본 선행조건 순환 모순");

        private final String code;
        private final boolean objectiveEvaluatorReady;
        private final String summary;

        GapKind(String code, boolean objectiveEvaluatorReady, String summary) {
            this.code = code;
            this.objectiveEvaluatorReady = objectiveEvaluatorReady;
            this.summary = summary;
        }

        public String code() { return code; }
        public boolean objectiveEvaluatorReady() { return objectiveEvaluatorReady; }
        public String summary() { return summary; }
    }

    public record Spec(
            String characterId,
            String title,
            String objective,
            CanonState canonState,
            GapKind gapKind,
            String unresolvedReason
    ) {
        public boolean objectiveEvaluatorReady() { return gapKind.objectiveEvaluatorReady(); }

        /** Player-facing authoring diagnosis; never implies that a missing roster was silently authored. */
        public String authoringBlockReason() {
            String readiness = objectiveEvaluatorReady() ? "목표 판정 로직 준비 완료" : "목표 판정 완성 대기";
            return gapKind.code() + " · " + gapKind.summary() + " · " + readiness + " · " + unresolvedReason;
        }
    }

    private static final List<Spec> ALL = List.of(
            gap("P01", "결투의 잔향",
                    "P01 포함 2인 이하 파티로 특수 엘리트 1체 격파",
                    GapKind.SPECIAL_ELITE_IDENTITY,
                    "특수 엘리트의 canonical ID/전투 편성이 v0.4에 지정되지 않음"),
            gap("P02", "정지하지 않는 초침의 시련",
                    "Final SPD 80 이하 아군 2명 이상 포함 · 22행동 안에 Trial Boss 격파",
                    GapKind.TRIAL_BOSS_IDENTITY,
                    "Trial Boss의 canonical ID/전투 편성이 v0.4에 지정되지 않음"),
            gap("P03", "성문 수호 시련",
                    "지정 NPC를 10회 적 행동 동안 생존시킴",
                    GapKind.PROTECTED_NPC_IDENTITY,
                    "보호 대상 NPC의 canonical ID와 Trial 적 편성이 v0.4에 지정되지 않음"),
            gap("P04", "되돌아오는 불씨의 시련",
                    "전투 중 아군 사망 1회 이상 · 최종 승리 시 전원 생존",
                    GapKind.ENCOUNTER_ROSTER,
                    "Trial 적 편성이 v0.4에 지정되지 않음"),
            gap("P05", "한 발 늦지 않는 시련",
                    "Follow-up 10회 이상 · 25행동 안에 승리",
                    GapKind.ENCOUNTER_ROSTER,
                    "Trial 적 편성이 v0.4에 지정되지 않음"),
            gap("P06", "이름 없는 기록의 시련",
                    "P06 자가부활 1회 · 기억 5 이상으로 승리",
                    GapKind.ENCOUNTER_ROSTER,
                    "Trial 적 편성이 v0.4에 지정되지 않음"),
            gap("P07", "두 번째 계약의 시련",
                    "Toto 1회 사망 후 재소환 · Marion 생존 상태로 승리",
                    GapKind.ENCOUNTER_ROSTER,
                    "Trial 적 편성이 v0.4에 지정되지 않음"),
            new Spec("P08", "핏빛 손잡이의 시련",
                    "전투 중 HP 1 생존 패시브 발동 · 종료 HP 30% 이하 · 승리",
                    CanonState.CANON_CONTRADICTION,
                    GapKind.PREREQUISITE_CONTRADICTION,
                    "v0.4에서 HP 1 치명 생존은 P08 Awakening 효과이지만 Signature Trial 클리어가 Awakening의 선행조건임")
    );
    private static final Map<String, Spec> BY_CHARACTER = index();

    private SignatureTrialCatalog() { }

    public static List<Spec> all() { return ALL; }

    public static Spec forCharacter(String characterId) {
        Spec spec = BY_CHARACTER.get(characterId);
        if (spec == null) throw new IllegalArgumentException("No Signature Trial for " + characterId);
        return spec;
    }

    public static boolean contains(String characterId) { return BY_CHARACTER.containsKey(characterId); }

    private static Spec gap(String id, String title, String objective, GapKind gapKind, String reason) {
        return new Spec(id, title, objective, CanonState.RULES_READY_ROSTER_GAP, gapKind, reason);
    }

    private static Map<String, Spec> index() {
        Map<String, Spec> out = new LinkedHashMap<>();
        for (Spec spec : ALL) out.put(spec.characterId(), spec);
        return Map.copyOf(out);
    }
}
