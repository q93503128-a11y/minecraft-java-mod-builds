package kr.moonseungjun.villageguardians;

public enum VillageTimePhase {
    DAY("낮", 5000L),
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
        return this == DAY ? NIGHT : DAY;
    }
}
