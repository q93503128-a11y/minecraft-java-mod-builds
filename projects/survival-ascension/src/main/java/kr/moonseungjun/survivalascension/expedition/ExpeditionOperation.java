package kr.moonseungjun.survivalascension.expedition;

import java.util.List;

public enum ExpeditionOperation {
    WOODLAND(ExpeditionRegion.WOODLAND, "심림 순환 벌채", 96, 24000,
            List.of(new Task(ExpeditionAction.LOGS_FELLED, 128), new Task(ExpeditionAction.TRAVEL_DISTANCE, 240))),
    ARID(ExpeditionRegion.ARID, "사막 보급로 개척", 96, 24000,
            List.of(new Task(ExpeditionAction.BLOCKS_BUILT, 96), new Task(ExpeditionAction.TRAVEL_DISTANCE, 240))),
    WETLAND(ExpeditionRegion.WETLAND, "습지 채집·소탕", 96, 24000,
            List.of(new Task(ExpeditionAction.CROPS_HARVESTED, 80), new Task(ExpeditionAction.HOSTILES_KILLED, 8))),
    HIGHLANDS(ExpeditionRegion.HIGHLANDS, "능선 장거리 순찰", 128, 24000,
            List.of(new Task(ExpeditionAction.TRAVEL_DISTANCE, 600), new Task(ExpeditionAction.DASHES_USED, 12))),
    OCEAN(ExpeditionRegion.OCEAN, "외해 순항", 128, 24000,
            List.of(new Task(ExpeditionAction.OCEAN_VOYAGE, 900), new Task(ExpeditionAction.HOSTILES_KILLED, 8))),
    DEEP(ExpeditionRegion.DEEP, "심층 채굴 회수", 128, 30000,
            List.of(new Task(ExpeditionAction.BLOCKS_MINED, 192), new Task(ExpeditionAction.HOSTILES_KILLED, 10))),
    FROZEN(ExpeditionRegion.FROZEN, "백설 장거리 순찰", 128, 30000,
            List.of(new Task(ExpeditionAction.TRAVEL_DISTANCE, 600), new Task(ExpeditionAction.HOSTILES_KILLED, 10))),
    NETHER(ExpeditionRegion.NETHER, "네더 전진 작전", 160, 30000,
            List.of(new Task(ExpeditionAction.HOSTILES_KILLED, 24), new Task(ExpeditionAction.BLOCKS_MINED, 96))),
    END(ExpeditionRegion.END, "공허 외곽 소탕", 160, 36000,
            List.of(new Task(ExpeditionAction.HOSTILES_KILLED, 28), new Task(ExpeditionAction.TRAVEL_DISTANCE, 360)));

    private final ExpeditionRegion region;
    private final String koreanName;
    private final int rangeTarget;
    private final int durationTicks;
    private final List<Task> tasks;

    ExpeditionOperation(ExpeditionRegion region, String koreanName, int rangeTarget, int durationTicks, List<Task> tasks) {
        this.region = region;
        this.koreanName = koreanName;
        this.rangeTarget = rangeTarget;
        this.durationTicks = durationTicks;
        this.tasks = tasks;
    }

    public ExpeditionRegion region() { return region; }
    public String koreanName() { return koreanName; }
    public int rangeTarget() { return rangeTarget; }
    public int durationTicks() { return durationTicks; }
    public List<Task> tasks() { return tasks; }
    public int skillXpReward() { return region.requiredWorldStage() == 0 ? 250 : region.requiredWorldStage() == 1 ? 400 : 600; }
    public int experienceReward() { return region.requiredWorldStage() == 0 ? 75 : region.requiredWorldStage() == 1 ? 125 : 200; }

    public static ExpeditionOperation forRegion(ExpeditionRegion region) {
        for (ExpeditionOperation operation : values()) if (operation.region == region) return operation;
        throw new IllegalArgumentException("No operation for region " + region);
    }

    public String taskSummary() {
        return tasks.get(0).action().koreanName() + " " + tasks.get(0).target()
                + " · " + tasks.get(1).action().koreanName() + " " + tasks.get(1).target();
    }

    public record Task(ExpeditionAction action, int target) {}
}
