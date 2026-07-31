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
            Codec.INT.optionalFieldOf("armory_level", 0).forGetter(data -> data.smithyLevel),
            Codec.INT.optionalFieldOf("infirmary_level", 0).forGetter(data -> data.infirmaryLevel),
            Codec.INT.optionalFieldOf("storehouse_level", 0).forGetter(data -> data.storehouseLevel),
            Codec.INT.optionalFieldOf("barracks_level", 0).forGetter(data -> data.barracksLevel),
            Codec.INT.optionalFieldOf("skill_hall_level", 0).forGetter(data -> data.skillHallLevel),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("last_supply_claim_day", Map.of())
                    .forGetter(data -> data.lastSupplyClaimDay),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("guardian_coins", Map.of())
                    .forGetter(data -> data.guardianCoins),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("forge_ranks", Map.of())
                    .forGetter(data -> data.forgeRanks),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("skill_ranks", Map.of())
                    .forGetter(data -> data.skillRanks),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("building_durability", Map.of())
                    .forGetter(data -> data.buildingDurability),
            Codec.BOOL.optionalFieldOf("game_over", false).forGetter(data -> data.gameOver)
    ).apply(instance, VillageProgressionData::new));

    public static final SavedDataType<VillageProgressionData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_progression"),
            level -> new VillageProgressionData(),
            level -> CODEC);

    private int supplies;
    private int wallLevel;
    private int smithyLevel;
    private int infirmaryLevel;
    private int storehouseLevel;
    private int barracksLevel;
    private int skillHallLevel;
    private Map<String, Integer> lastSupplyClaimDay;
    private Map<String, Integer> guardianCoins;
    private Map<String, Integer> forgeRanks;
    private Map<String, Integer> skillRanks;
    private Map<String, Integer> buildingDurability;
    private boolean gameOver;

    public VillageProgressionData() {
        this(180, 0, 0, 0, 0, 0, 0,
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), false);
    }

    private VillageProgressionData(
            int supplies,
            int wallLevel,
            int smithyLevel,
            int infirmaryLevel,
            int storehouseLevel,
            int barracksLevel,
            int skillHallLevel,
            Map<String, Integer> lastSupplyClaimDay,
            Map<String, Integer> guardianCoins,
            Map<String, Integer> forgeRanks,
            Map<String, Integer> skillRanks,
            Map<String, Integer> buildingDurability,
            boolean gameOver) {
        this.supplies = Math.max(0, supplies);
        this.wallLevel = clampLevel(wallLevel);
        this.smithyLevel = clampLevel(smithyLevel);
        this.infirmaryLevel = clampLevel(infirmaryLevel);
        this.storehouseLevel = clampLevel(storehouseLevel);
        this.barracksLevel = clampLevel(barracksLevel);
        this.skillHallLevel = clampLevel(skillHallLevel);
        this.lastSupplyClaimDay = new LinkedHashMap<>(lastSupplyClaimDay);
        this.guardianCoins = new LinkedHashMap<>(guardianCoins);
        this.forgeRanks = new LinkedHashMap<>(forgeRanks);
        this.skillRanks = new LinkedHashMap<>(skillRanks);
        this.buildingDurability = new LinkedHashMap<>(buildingDurability);
        this.gameOver = gameOver;
    }

    public int supplies() {
        return supplies;
    }

    public int wallLevel() {
        return wallLevel;
    }

    public int smithyLevel() {
        return smithyLevel;
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

    public int skillHallLevel() {
        return skillHallLevel;
    }

    public Map<UUID, Integer> claimDays() {
        return parseUuidMap(lastSupplyClaimDay, Integer.MAX_VALUE);
    }

    public Map<UUID, Integer> coins() {
        return parseUuidMap(guardianCoins, Integer.MAX_VALUE);
    }

    public Map<UUID, Integer> forgeRanks() {
        return parseUuidMap(forgeRanks, VillageProgressionSystem.MAX_PERSONAL_RANK);
    }

    public Map<UUID, Integer> skillRanks() {
        return parseUuidMap(skillRanks, VillageProgressionSystem.MAX_PERSONAL_RANK);
    }

    public Map<String, Integer> buildingDurability() {
        return new LinkedHashMap<>(buildingDurability);
    }

    public boolean gameOver() {
        return gameOver;
    }

    public void replaceState(
            int supplies,
            int wallLevel,
            int smithyLevel,
            int infirmaryLevel,
            int storehouseLevel,
            int barracksLevel,
            int skillHallLevel,
            Map<UUID, Integer> claimDays,
            Map<UUID, Integer> coins,
            Map<UUID, Integer> forgeRanks,
            Map<UUID, Integer> skillRanks,
            Map<String, Integer> buildingDurability,
            boolean gameOver) {
        this.supplies = Math.max(0, supplies);
        this.wallLevel = clampLevel(wallLevel);
        this.smithyLevel = clampLevel(smithyLevel);
        this.infirmaryLevel = clampLevel(infirmaryLevel);
        this.storehouseLevel = clampLevel(storehouseLevel);
        this.barracksLevel = clampLevel(barracksLevel);
        this.skillHallLevel = clampLevel(skillHallLevel);
        this.lastSupplyClaimDay = encodeUuidMap(claimDays);
        this.guardianCoins = encodeUuidMap(coins);
        this.forgeRanks = encodeUuidMap(forgeRanks);
        this.skillRanks = encodeUuidMap(skillRanks);
        this.buildingDurability = new LinkedHashMap<>();
        buildingDurability.forEach((id, hp) -> this.buildingDurability.put(id, Math.max(0, hp)));
        this.gameOver = gameOver;
        setDirty();
    }

    private static Map<UUID, Integer> parseUuidMap(Map<String, Integer> encoded, int maxValue) {
        Map<UUID, Integer> parsed = new LinkedHashMap<>();
        encoded.forEach((uuidText, value) -> {
            try {
                parsed.put(UUID.fromString(uuidText), Math.max(0, Math.min(maxValue, value)));
            } catch (IllegalArgumentException ignored) {
                // Ignore only the damaged player entry.
            }
        });
        return parsed;
    }

    private static Map<String, Integer> encodeUuidMap(Map<UUID, Integer> source) {
        Map<String, Integer> encoded = new LinkedHashMap<>();
        source.forEach((uuid, value) -> encoded.put(uuid.toString(), Math.max(0, value)));
        return encoded;
    }

    private static int clampLevel(int value) {
        return Math.max(0, Math.min(VillageProgressionSystem.MAX_BUILDING_LEVEL, value));
    }
}
