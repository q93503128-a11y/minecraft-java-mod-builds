package kr.moonseungjun.survivalascension.apex;

import kr.moonseungjun.survivalascension.expedition.ExpeditionRegion;

import java.util.List;

public enum ApexArchetype {
    WOODLAND_BREAKER(
            ExpeditionRegion.WOODLAND, "수림 파쇄자", "minecraft:ravager",
            List.of("minecraft:vindicator", "minecraft:zombie"), 4,
            Pattern.CHARGE, 60.0D, 4.0D, 0.12D),
    ARID_COMMANDER(
            ExpeditionRegion.ARID, "황야 지휘관", "minecraft:husk",
            List.of("minecraft:skeleton", "minecraft:pillager"), 5,
            Pattern.REINFORCE, 70.0D, 6.0D, 0.22D),
    WETLAND_PLAGUEHEART(
            ExpeditionRegion.WETLAND, "늪지 역병핵", "minecraft:zombie",
            List.of("minecraft:cave_spider", "minecraft:witch"), 5,
            Pattern.PLAGUE, 80.0D, 5.0D, 0.20D),
    HIGHLAND_HUNTER(
            ExpeditionRegion.HIGHLANDS, "능선 사냥꾼", "minecraft:stray",
            List.of("minecraft:stray", "minecraft:skeleton"), 4,
            Pattern.SKIRMISH, 65.0D, 4.0D, 0.16D),
    OCEAN_TYRANT(
            ExpeditionRegion.OCEAN, "심해 압제자", "minecraft:elder_guardian",
            List.of("minecraft:guardian"), 4,
            Pattern.PULL, 80.0D, 6.0D, 0.16D),
    DEEP_STALKER(
            ExpeditionRegion.DEEP, "심층 추적자", "minecraft:spider",
            List.of("minecraft:cave_spider", "minecraft:skeleton"), 6,
            Pattern.LEAP, 70.0D, 4.0D, 0.18D),
    FROZEN_WARDEN(
            ExpeditionRegion.FROZEN, "빙설 감시자", "minecraft:stray",
            List.of("minecraft:stray", "minecraft:zombie"), 5,
            Pattern.FROST, 75.0D, 6.0D, 0.18D),
    NETHER_REAVER(
            ExpeditionRegion.NETHER, "네더 약탈자", "minecraft:wither_skeleton",
            List.of("minecraft:blaze", "minecraft:wither_skeleton"), 6,
            Pattern.WITHER, 90.0D, 8.0D, 0.24D),
    END_HARBINGER(
            ExpeditionRegion.END, "공허 전조자", "minecraft:enderman",
            List.of("minecraft:shulker", "minecraft:enderman"), 6,
            Pattern.VOID, 100.0D, 8.0D, 0.24D);

    public enum Pattern {
        CHARGE,
        REINFORCE,
        PLAGUE,
        SKIRMISH,
        PULL,
        LEAP,
        FROST,
        WITHER,
        VOID
    }

    private final ExpeditionRegion region;
    private final String koreanName;
    private final String bossTypeId;
    private final List<String> escortTypeIds;
    private final int escortCount;
    private final Pattern pattern;
    private final double healthBonus;
    private final double armorBonus;
    private final double attackBonus;

    ApexArchetype(ExpeditionRegion region, String koreanName, String bossTypeId,
                  List<String> escortTypeIds, int escortCount, Pattern pattern,
                  double healthBonus, double armorBonus, double attackBonus) {
        this.region = region;
        this.koreanName = koreanName;
        this.bossTypeId = bossTypeId;
        this.escortTypeIds = escortTypeIds;
        this.escortCount = escortCount;
        this.pattern = pattern;
        this.healthBonus = healthBonus;
        this.armorBonus = armorBonus;
        this.attackBonus = attackBonus;
    }

    public ExpeditionRegion region() { return region; }
    public String koreanName() { return koreanName; }
    public String bossTypeId() { return bossTypeId; }
    public List<String> escortTypeIds() { return escortTypeIds; }
    public int escortCount() { return escortCount; }
    public Pattern pattern() { return pattern; }
    public double healthBonus() { return healthBonus; }
    public double armorBonus() { return armorBonus; }
    public double attackBonus() { return attackBonus; }
    public boolean aquatic() { return region == ExpeditionRegion.OCEAN; }

    public static ApexArchetype forRegion(ExpeditionRegion region) {
        for (ApexArchetype archetype : values()) if (archetype.region == region) return archetype;
        return null;
    }
}
