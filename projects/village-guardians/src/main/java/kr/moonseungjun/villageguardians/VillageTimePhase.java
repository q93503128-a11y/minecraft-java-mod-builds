package kr.moonseungjun.villageguardians;

public enum VillageTimePhase {
    MORNING("아침", 1000L),
    DAY("낮", 6000L),
    EVENING("저녁", 12000L),
    NIGHT("밤", 18000L);

    private final String koreanName;
    private final long minecraftTime;

    VillageTimePhase(String koreanName, long minecraftTime) {
        this.koreanName = koreanName;
        this.minecraftTime = minecraftTime;
    }

    public String koreanName() {
        return koreanName;
    }

    public long minecraftTime() {
        return minecraftTime;
    }

    public VillageTimePhase next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
