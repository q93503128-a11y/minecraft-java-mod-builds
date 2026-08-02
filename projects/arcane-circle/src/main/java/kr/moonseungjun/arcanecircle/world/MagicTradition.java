package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;

/**
 * Save-compatible affiliation keys. Factions remain social organizations, but each now teaches
 * a distinct combat doctrine with an explicit strength and drawback.
 */
public enum MagicTradition {
    UNBOUND(
            "무소속",
            "어느 조직에도 속하지 않은 떠돌이·은둔·생활 마법사입니다.",
            "제약 없는 중립 관계", "전용 교리와 보상 보정 없음",
            1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00),
    ARCANE(
            "왕국 마도연맹",
            "면허·정밀 조준·공공 전투 교리를 중시하는 왕국 공인 마도 조직입니다.",
            "사거리 +18% · 쿨타임 -14% · 비전술 +15%",
            "마력 소모 +8% · 일반 위력 -4%",
            1.08, 0.96, 1.18, 0.86, 1.00, 1.10, 1.08, 0.90, 1.04),
    DIVINE(
            "백은 성약",
            "보호·치유·재난 대응과 안정적인 장기전을 우선하는 독립 성약입니다.",
            "마력 회복 +28% · 생명/수호술 +25% · 의뢰 보상 +18%",
            "일반 공격 위력 -8% · 쿨타임 +5%",
            0.94, 0.92, 1.02, 1.05, 1.28, 1.02, 1.18, 1.02, 1.02),
    OCCULT(
            "녹월 결사",
            "민간 전승과 금단 직전의 복합 회로를 연구하는 느슨한 비밀 결사입니다.",
            "마력 소모 -22% · 공격 위력 +12% · 융합 위력 +18%",
            "사거리 -8% · 쿨타임 +12%",
            0.78, 1.12, 0.92, 1.12, 1.05, 1.08, 1.05, 1.06, 1.18),
    PRIMAL(
            "재의 밀약",
            "지배와 파괴를 추구하며 다른 학회와 적대하는 고위험 전투 조직입니다.",
            "위력 +24% · 전투 아르카나 +30% · 원소술 +15%",
            "마력 소모 +16% · 사거리 -12% · 쿨타임 +22%",
            1.16, 1.24, 0.88, 1.22, 0.92, 1.30, 1.12, 1.12, 1.08);

    private final String displayName;
    private final String description;
    private final String strength;
    private final String weakness;
    private final double manaMultiplier;
    private final double powerMultiplier;
    private final double rangeMultiplier;
    private final double cooldownMultiplier;
    private final double regenMultiplier;
    private final double combatRewardMultiplier;
    private final double questRewardMultiplier;
    private final double castTimeMultiplier;
    private final double fusionMultiplier;

    MagicTradition(String displayName, String description, String strength, String weakness,
                   double manaMultiplier, double powerMultiplier, double rangeMultiplier,
                   double cooldownMultiplier, double regenMultiplier, double combatRewardMultiplier,
                   double questRewardMultiplier, double castTimeMultiplier, double fusionMultiplier) {
        this.displayName = displayName;
        this.description = description;
        this.strength = strength;
        this.weakness = weakness;
        this.manaMultiplier = manaMultiplier;
        this.powerMultiplier = powerMultiplier;
        this.rangeMultiplier = rangeMultiplier;
        this.cooldownMultiplier = cooldownMultiplier;
        this.regenMultiplier = regenMultiplier;
        this.combatRewardMultiplier = combatRewardMultiplier;
        this.questRewardMultiplier = questRewardMultiplier;
        this.castTimeMultiplier = castTimeMultiplier;
        this.fusionMultiplier = fusionMultiplier;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
    public String strength() { return strength; }
    public String weakness() { return weakness; }
    public double manaMultiplier() { return manaMultiplier; }
    public double powerMultiplier() { return powerMultiplier; }
    public double rangeMultiplier() { return rangeMultiplier; }
    public double cooldownMultiplier() { return cooldownMultiplier; }
    public double regenMultiplier() { return regenMultiplier; }
    public double combatRewardMultiplier() { return combatRewardMultiplier; }
    public double questRewardMultiplier() { return questRewardMultiplier; }
    public double castTimeMultiplier() { return castTimeMultiplier; }
    public double fusionMultiplier() { return fusionMultiplier; }

    public double powerFor(SpellDefinition.School school) {
        double schoolDoctrine = switch (this) {
            case ARCANE -> school == SpellDefinition.School.ARCANE ? 1.15 : 1.0;
            case DIVINE -> school == SpellDefinition.School.LIFE || school == SpellDefinition.School.WARD ? 1.25 : 1.0;
            case OCCULT -> school == SpellDefinition.School.ARCANE || school == SpellDefinition.School.LIFE ? 1.12 : 1.0;
            case PRIMAL -> school == SpellDefinition.School.FIRE
                    || school == SpellDefinition.School.FROST
                    || school == SpellDefinition.School.WIND ? 1.15 : 1.0;
            default -> 1.0;
        };
        return powerMultiplier * schoolDoctrine;
    }

    public static MagicTradition parse(String value) {
        if (value == null || value.isBlank()) return UNBOUND;
        try { return valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException ignored) { return UNBOUND; }
    }
}
