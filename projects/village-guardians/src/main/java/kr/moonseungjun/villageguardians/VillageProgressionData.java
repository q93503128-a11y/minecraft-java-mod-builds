package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class VillageProgressionData extends SavedData {
    private static final Codec<VillageProgressionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("supplies", 180).forGetter(data -> data.supplies),
            Codec.INT.optionalFieldOf("wall_level", 0).forGetter(data -> data.wallLevel),
            Codec.INT.optionalFieldOf("armory_level", 0).forGetter(data -> data.armoryLevel),
            Codec.INT.optionalFieldOf("infirmary_level", 0).forGetter(data -> data.infirmaryLevel),
            Codec.INT.optionalFieldOf("storehouse_level", 0).forGetter(data -> data.storehouseLevel),
            Codec.INT.optionalFieldOf("barracks_level", 0).forGetter(data -> data.barracksLevel),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("last_supply_claim_day", Map.of())
                    .forGetter(data -> data.lastSupplyClaimDay)
    ).apply(instance, VillageProgressionData::new));

    public static final SavedDataType<VillageProgressionData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_progression"),
            level -> new VillageProgressionData(),
            level -> CODEC);

    private int supplies;
    private int wallLevel;
    private int armoryLevel;
    private int infirmaryLevel;
    private int storehouseLevel;
    private int barracksLevel;
    private Map<String, Integer> lastSupplyClaimDay;

    public VillageProgressionData() {
        this(180, 0, 0, 0, 0, 0, Map.of());
    }

    private VillageProgressionData(
            int supplies,
            int wallLevel,
            int armoryLevel,
            int infirmaryLevel,
            int storehouseLevel,
            int barracksLevel,
            Map<String, Integer> lastSupplyClaimDay) {
        this.supplies = Math.max(0, supplies);
        this.wallLevel = clampLevel(wallLevel);
        this.armoryLevel = clampLevel(armoryLevel);
        this.infirmaryLevel = clampLevel(infirmaryLevel);
        this.storehouseLevel = clampLevel(storehouseLevel);
        this.barracksLevel = clampLevel(barracksLevel);
        this.lastSupplyClaimDay = new LinkedHashMap<>(lastSupplyClaimDay);
    }

    public int supplies() {
        return supplies;
    }

    public int wallLevel() {
        return wallLevel;
    }

    public int armoryLevel() {
        return armoryLevel;
    }

    public int infirmaryLevel() {
        return infirmaryLevel;
    }

    public int storehouseLevel() {
        return storehouseLevel;
    }

    public int barracksLevel() {
        return barracksLevel;
    }

    public int lastSupplyClaimDay(UUID playerId) {
        return lastSupplyClaimDay.getOrDefault(playerId.toString(), 0);
    }

    public void replaceState(
            int supplies,
            int wallLevel,
            int armoryLevel,
            int infirmaryLevel,
            int storehouseLevel,
            int barracksLevel,
            Map<UUID, Integer> claimDays) {
        this.supplies = Math.max(0, supplies);
        this.wallLevel = clampLevel(wallLevel);
        this.armoryLevel = clampLevel(armoryLevel);
        this.infirmaryLevel = clampLevel(infirmaryLevel);
        this.storehouseLevel = clampLevel(storehouseLevel);
        this.barracksLevel = clampLevel(barracksLevel);
        Map<String, Integer> encoded = new LinkedHashMap<>();
        claimDays.forEach((uuid, day) -> encoded.put(uuid.toString(), Math.max(0, day)));
        this.lastSupplyClaimDay = encoded;
        setDirty();
    }

    private static int clampLevel(int value) {
        return Math.max(0, Math.min(VillageProgressionSystem.MAX_BUILDING_LEVEL, value));
    }
}
