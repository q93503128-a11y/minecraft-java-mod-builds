package kr.moonseungjun.survivalascension.expedition;

import kr.moonseungjun.survivalascension.progress.SkillType;

public enum ExpeditionAction {
    LOGS_FELLED("자연 로그 일괄 벌목"),
    BLOCKS_BUILT("대량 건축"),
    CROPS_HARVESTED("성숙 작물 수확"),
    TRAVEL_DISTANCE("도보·돌진 이동"),
    OCEAN_VOYAGE("수영·항해"),
    BLOCKS_MINED("곡괭이 채굴"),
    HOSTILES_KILLED("적대적 몹 처치"),
    DASHES_USED("돌진 사용");

    private final String koreanName;

    ExpeditionAction(String koreanName) {
        this.koreanName = koreanName;
    }

    public String koreanName() { return koreanName; }

    public static ExpeditionAction fromSkill(SkillType skill) {
        return switch (skill) {
            case WOODCUTTING -> LOGS_FELLED;
            case CONSTRUCTION -> BLOCKS_BUILT;
            case HARVESTING -> CROPS_HARVESTED;
            case MOBILITY -> TRAVEL_DISTANCE;
            case MINING -> BLOCKS_MINED;
            case COMBAT -> HOSTILES_KILLED;
        };
    }
}
