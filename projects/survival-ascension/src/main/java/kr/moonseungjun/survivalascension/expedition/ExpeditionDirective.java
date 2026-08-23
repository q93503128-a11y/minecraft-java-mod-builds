package kr.moonseungjun.survivalascension.expedition;

import java.util.List;

public enum ExpeditionDirective {
    WOODLAND_STANDARD(ExpeditionRegion.WOODLAND, "거목 정리", task(ExpeditionAction.LOGS_FELLED, 96)),
    WOODLAND_PATROL(ExpeditionRegion.WOODLAND, "수림 개척", task(ExpeditionAction.LOGS_FELLED, 64), task(ExpeditionAction.TRAVEL_DISTANCE, 240)),

    ARID_STANDARD(ExpeditionRegion.ARID, "전초 건설", task(ExpeditionAction.BLOCKS_BUILT, 128)),
    ARID_ROUTE(ExpeditionRegion.ARID, "사막 보급로", task(ExpeditionAction.BLOCKS_BUILT, 96), task(ExpeditionAction.TRAVEL_DISTANCE, 240)),

    WETLAND_STANDARD(ExpeditionRegion.WETLAND, "습지 수확", task(ExpeditionAction.CROPS_HARVESTED, 96)),
    WETLAND_CLEARANCE(ExpeditionRegion.WETLAND, "습지 정비", task(ExpeditionAction.CROPS_HARVESTED, 64), task(ExpeditionAction.HOSTILES_KILLED, 8)),

    HIGHLANDS_STANDARD(ExpeditionRegion.HIGHLANDS, "능선 횡단", task(ExpeditionAction.TRAVEL_DISTANCE, 600)),
    HIGHLANDS_DASH(ExpeditionRegion.HIGHLANDS, "능선 돌파", task(ExpeditionAction.TRAVEL_DISTANCE, 360), task(ExpeditionAction.DASHES_USED, 12)),

    OCEAN_STANDARD(ExpeditionRegion.OCEAN, "해양 항로", task(ExpeditionAction.OCEAN_VOYAGE, 800)),
    OCEAN_PATROL(ExpeditionRegion.OCEAN, "심해 순찰", task(ExpeditionAction.OCEAN_VOYAGE, 500), task(ExpeditionAction.HOSTILES_KILLED, 8)),

    DEEP_STANDARD(ExpeditionRegion.DEEP, "심층 채굴", task(ExpeditionAction.BLOCKS_MINED, 192)),
    DEEP_CLEARANCE(ExpeditionRegion.DEEP, "심층 개척", task(ExpeditionAction.BLOCKS_MINED, 128), task(ExpeditionAction.HOSTILES_KILLED, 10)),

    FROZEN_STANDARD(ExpeditionRegion.FROZEN, "설원 횡단", task(ExpeditionAction.TRAVEL_DISTANCE, 600)),
    FROZEN_DASH(ExpeditionRegion.FROZEN, "빙설 돌파", task(ExpeditionAction.TRAVEL_DISTANCE, 360), task(ExpeditionAction.DASHES_USED, 12)),

    NETHER_STANDARD(ExpeditionRegion.NETHER, "네더 토벌", task(ExpeditionAction.HOSTILES_KILLED, 24)),
    NETHER_SUPPLY(ExpeditionRegion.NETHER, "네더 보급전", task(ExpeditionAction.HOSTILES_KILLED, 16), task(ExpeditionAction.BLOCKS_MINED, 96)),

    END_STANDARD(ExpeditionRegion.END, "엔드 토벌", task(ExpeditionAction.HOSTILES_KILLED, 32)),
    END_TRAVERSE(ExpeditionRegion.END, "공허 전진", task(ExpeditionAction.HOSTILES_KILLED, 20), task(ExpeditionAction.TRAVEL_DISTANCE, 360));

    public record Task(ExpeditionAction action, int target) {}

    private final ExpeditionRegion region;
    private final String koreanName;
    private final List<Task> tasks;

    ExpeditionDirective(ExpeditionRegion region, String koreanName, Task... tasks) {
        this.region = region;
        this.koreanName = koreanName;
        this.tasks = List.of(tasks);
    }

    public ExpeditionRegion region() { return region; }
    public String koreanName() { return koreanName; }
    public List<Task> tasks() { return tasks; }

    public static List<ExpeditionDirective> forRegion(ExpeditionRegion region) {
        return switch (region) {
            case WOODLAND -> List.of(WOODLAND_STANDARD, WOODLAND_PATROL);
            case ARID -> List.of(ARID_STANDARD, ARID_ROUTE);
            case WETLAND -> List.of(WETLAND_STANDARD, WETLAND_CLEARANCE);
            case HIGHLANDS -> List.of(HIGHLANDS_STANDARD, HIGHLANDS_DASH);
            case OCEAN -> List.of(OCEAN_STANDARD, OCEAN_PATROL);
            case DEEP -> List.of(DEEP_STANDARD, DEEP_CLEARANCE);
            case FROZEN -> List.of(FROZEN_STANDARD, FROZEN_DASH);
            case NETHER -> List.of(NETHER_STANDARD, NETHER_SUPPLY);
            case END -> List.of(END_STANDARD, END_TRAVERSE);
        };
    }

    public static ExpeditionDirective select(ExpeditionRegion region, int index) {
        List<ExpeditionDirective> options = forRegion(region);
        int safe = Math.floorMod(index, options.size());
        return options.get(safe);
    }

    public static int optionCount(ExpeditionRegion region) { return forRegion(region).size(); }

    private static Task task(ExpeditionAction action, int target) { return new Task(action, target); }
}
