package kr.moonseungjun.survivalascension.expedition;

import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Region incident catalog. Modifier-style event composition is independently implemented
 * after studying Enhanced Celestials Tweaks (MIT). Rare forced encounter pacing is only a
 * high-level design reference from Majrusz's Progressive Difficulty; no source/assets are copied.
 */
public enum ExpeditionIncident {
    WOODLAND_AMBUSH(ExpeditionRegion.WOODLAND, "수림 습격", 1200, 6, "minecraft:zombie", "minecraft:spider"),
    WOODLAND_RUSH(ExpeditionRegion.WOODLAND, "벌목 비상", 900, ExpeditionAction.LOGS_FELLED, 24),
    WOODLAND_FRONTIER_AMBUSH(ExpeditionRegion.WOODLAND, "밀림 포위", 1200, 6,
            "minecraft:witch", "minecraft:spider", "minecraft:zombie"),

    ARID_AMBUSH(ExpeditionRegion.ARID, "약탈대 급습", 1200, 6, "minecraft:husk", "minecraft:pillager"),
    ARID_RUSH(ExpeditionRegion.ARID, "긴급 보급선", 900, ExpeditionAction.BLOCKS_BUILT, 24),
    ARID_FRONTIER_RUSH(ExpeditionRegion.ARID, "황무지 방벽 구축", 900, ExpeditionAction.BLOCKS_BUILT, 30),

    WETLAND_AMBUSH(ExpeditionRegion.WETLAND, "늪지 습격", 1200, 6, "minecraft:zombie", "minecraft:spider"),
    WETLAND_RUSH(ExpeditionRegion.WETLAND, "긴급 수확", 900, ExpeditionAction.CROPS_HARVESTED, 20),
    WETLAND_FRONTIER_AMBUSH(ExpeditionRegion.WETLAND, "습원 마녀습격", 1200, 6,
            "minecraft:witch", "minecraft:slime", "minecraft:spider"),

    HIGHLANDS_AMBUSH(ExpeditionRegion.HIGHLANDS, "능선 매복", 1200, 6, "minecraft:pillager", "minecraft:skeleton"),
    HIGHLANDS_RUSH(ExpeditionRegion.HIGHLANDS, "능선 돌파", 900, ExpeditionAction.DASHES_USED, 4),
    HIGHLANDS_FRONTIER_RUSH(ExpeditionRegion.HIGHLANDS, "절벽 강행 돌파", 900, ExpeditionAction.DASHES_USED, 5),

    OCEAN_AMBUSH(ExpeditionRegion.OCEAN, "익사자 습격", 1200, 6, "minecraft:drowned"),
    OCEAN_RUSH(ExpeditionRegion.OCEAN, "폭풍 항해", 900, ExpeditionAction.OCEAN_VOYAGE, 180),

    DEEP_AMBUSH(ExpeditionRegion.DEEP, "심층 군집", 1200, 7, "minecraft:zombie", "minecraft:skeleton", "minecraft:spider"),
    DEEP_RUSH(ExpeditionRegion.DEEP, "붕괴 전 채굴", 900, ExpeditionAction.BLOCKS_MINED, 48),
    DEEP_FRONTIER_AMBUSH(ExpeditionRegion.DEEP, "동굴 군집 붕괴", 1200, 7,
            "minecraft:cave_spider", "minecraft:silverfish", "minecraft:zombie"),

    FROZEN_AMBUSH(ExpeditionRegion.FROZEN, "설원 습격", 1200, 6, "minecraft:stray", "minecraft:skeleton"),
    FROZEN_RUSH(ExpeditionRegion.FROZEN, "빙설 강행군", 900, ExpeditionAction.TRAVEL_DISTANCE, 180),
    FROZEN_FRONTIER_RUSH(ExpeditionRegion.FROZEN, "눈보라 강행군", 900, ExpeditionAction.TRAVEL_DISTANCE, 220),

    NETHER_AMBUSH(ExpeditionRegion.NETHER, "네더 급습", 1200, 8, "minecraft:wither_skeleton", "minecraft:blaze"),
    NETHER_RUSH(ExpeditionRegion.NETHER, "열기 속 채굴", 900, ExpeditionAction.BLOCKS_MINED, 48),
    NETHER_FRONTIER_AMBUSH(ExpeditionRegion.NETHER, "용암 협곡 습격", 1200, 8,
            "minecraft:magma_cube", "minecraft:blaze", "minecraft:wither_skeleton"),

    END_AMBUSH(ExpeditionRegion.END, "공허 습격", 1200, 7, "minecraft:endermite", "minecraft:shulker"),
    END_RUSH(ExpeditionRegion.END, "공허 추적", 900, ExpeditionAction.TRAVEL_DISTANCE, 180),
    END_FRONTIER_RUSH(ExpeditionRegion.END, "공허 균열 사냥", 900, ExpeditionAction.HOSTILES_KILLED, 10);

    public enum Kind { AMBUSH, ACTION_RUSH }

    private final ExpeditionRegion region;
    private final String koreanName;
    private final Kind kind;
    private final int durationTicks;
    private final ExpeditionAction action;
    private final int actionTarget;
    private final int spawnCount;
    private final List<String> mobTypeIds;

    ExpeditionIncident(ExpeditionRegion region, String koreanName, int durationTicks,
                       ExpeditionAction action, int actionTarget) {
        this.region = region;
        this.koreanName = koreanName;
        this.kind = Kind.ACTION_RUSH;
        this.durationTicks = durationTicks;
        this.action = action;
        this.actionTarget = actionTarget;
        this.spawnCount = 0;
        this.mobTypeIds = List.of();
    }

    ExpeditionIncident(ExpeditionRegion region, String koreanName, int durationTicks,
                       int spawnCount, String... mobTypeIds) {
        this.region = region;
        this.koreanName = koreanName;
        this.kind = Kind.AMBUSH;
        this.durationTicks = durationTicks;
        this.action = null;
        this.actionTarget = 0;
        this.spawnCount = spawnCount;
        this.mobTypeIds = List.of(mobTypeIds);
    }

    public ExpeditionRegion region() { return region; }
    public String koreanName() { return koreanName; }
    public Kind kind() { return kind; }
    public int durationTicks() { return durationTicks; }
    public ExpeditionAction action() { return action; }
    public int actionTarget() { return actionTarget; }
    public int spawnCount() { return spawnCount; }
    public List<String> mobTypeIds() { return mobTypeIds; }

    public static List<ExpeditionIncident> forRegion(ExpeditionRegion region) {
        return forRegion(region, false);
    }

    /**
     * Data-integrated terrain receives one additional regional incident instead of merely sharing
     * the vanilla fallback pair. External biome IDs remain in data tags rather than Java.
     */
    public static List<ExpeditionIncident> forRegion(ExpeditionRegion region, boolean integrationTagged) {
        if (!integrationTagged) {
            return switch (region) {
                case WOODLAND -> List.of(WOODLAND_AMBUSH, WOODLAND_RUSH);
                case ARID -> List.of(ARID_AMBUSH, ARID_RUSH);
                case WETLAND -> List.of(WETLAND_AMBUSH, WETLAND_RUSH);
                case HIGHLANDS -> List.of(HIGHLANDS_AMBUSH, HIGHLANDS_RUSH);
                case OCEAN -> List.of(OCEAN_AMBUSH, OCEAN_RUSH);
                case DEEP -> List.of(DEEP_AMBUSH, DEEP_RUSH);
                case FROZEN -> List.of(FROZEN_AMBUSH, FROZEN_RUSH);
                case NETHER -> List.of(NETHER_AMBUSH, NETHER_RUSH);
                case END -> List.of(END_AMBUSH, END_RUSH);
            };
        }
        return switch (region) {
            case WOODLAND -> List.of(WOODLAND_AMBUSH, WOODLAND_RUSH, WOODLAND_FRONTIER_AMBUSH);
            case ARID -> List.of(ARID_AMBUSH, ARID_RUSH, ARID_FRONTIER_RUSH);
            case WETLAND -> List.of(WETLAND_AMBUSH, WETLAND_RUSH, WETLAND_FRONTIER_AMBUSH);
            case HIGHLANDS -> List.of(HIGHLANDS_AMBUSH, HIGHLANDS_RUSH, HIGHLANDS_FRONTIER_RUSH);
            case OCEAN -> List.of(OCEAN_AMBUSH, OCEAN_RUSH);
            case DEEP -> List.of(DEEP_AMBUSH, DEEP_RUSH, DEEP_FRONTIER_AMBUSH);
            case FROZEN -> List.of(FROZEN_AMBUSH, FROZEN_RUSH, FROZEN_FRONTIER_RUSH);
            case NETHER -> List.of(NETHER_AMBUSH, NETHER_RUSH, NETHER_FRONTIER_AMBUSH);
            case END -> List.of(END_AMBUSH, END_RUSH, END_FRONTIER_RUSH);
        };
    }

    public static ExpeditionIncident random(ExpeditionRegion region, RandomSource random) {
        return random(region, random, false);
    }

    public static ExpeditionIncident random(ExpeditionRegion region, RandomSource random, boolean integrationTagged) {
        List<ExpeditionIncident> options = forRegion(region, integrationTagged);
        return options.get(random.nextInt(options.size()));
    }
}
