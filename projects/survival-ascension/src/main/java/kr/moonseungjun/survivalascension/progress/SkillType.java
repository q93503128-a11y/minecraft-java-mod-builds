package kr.moonseungjun.survivalascension.progress;

import java.util.Arrays;

public enum SkillType {
    MINING("mining", "채굴", 0x55D6FF),
    WOODCUTTING("woodcutting", "벌목", 0xA9D45B),
    HARVESTING("harvesting", "농사", 0xF4D35E),
    COMBAT("combat", "전투", 0xFF6B6B),
    CONSTRUCTION("construction", "건축", 0xD6B27C),
    MOBILITY("mobility", "기동", 0xB89CFF);

    private final String id;
    private final String koreanName;
    private final int color;

    SkillType(String id, String koreanName, int color) {
        this.id = id;
        this.koreanName = koreanName;
        this.color = color;
    }

    public String id() { return id; }
    public String koreanName() { return koreanName; }
    public int color() { return color; }

    public static SkillType fromId(String id) {
        return Arrays.stream(values()).filter(skill -> skill.id.equals(id)).findFirst().orElse(null);
    }
}
