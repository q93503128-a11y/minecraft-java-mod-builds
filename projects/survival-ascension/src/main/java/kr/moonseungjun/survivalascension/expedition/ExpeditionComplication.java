package kr.moonseungjun.survivalascension.expedition;

public enum ExpeditionComplication {
    NONE("기본 작전", "0.32 호환 작전: 추가 작전 변수 없음"),
    DEEP_FRONT("전선 고착", "현장 행동은 최초 전진선 바깥에서만 기록"),
    FORWARD_SHIFT("전선 재전개", "첫 현장 목표 뒤 전진선을 48블록 더 밀어야 남은 목표 재개"),
    HOT_EXTRACTION("긴급 철수", "현장 목표 완료 뒤 단계별 귀환 제한시간 시작"),
    PURSUIT("지속 추격", "전진선 돌파 뒤 첫 목표 절반부터 2차 추격대가 조기 투입"),
    ANOMALY_SURGE("이변 증폭", "각 저지대에서 외부 이변 개체가 최대 2개 슬롯을 대체"),
    HIDDEN_AMBUSH("잠복 매복", "전진선 직후는 조용하지만 첫 목표 진행 중 강화 매복이 발생");

    private final String koreanName;
    private final String description;

    ExpeditionComplication(String koreanName, String description) {
        this.koreanName = koreanName;
        this.description = description;
    }

    public String koreanName() { return koreanName; }
    public String description() { return description; }

    public int extractionWindowTicks(ExpeditionOperation operation) {
        if (this != HOT_EXTRACTION) return 0;
        return switch (operation.region().requiredWorldStage()) {
            case 0 -> 4800;
            case 1 -> 3600;
            default -> 3000;
        };
    }
}
