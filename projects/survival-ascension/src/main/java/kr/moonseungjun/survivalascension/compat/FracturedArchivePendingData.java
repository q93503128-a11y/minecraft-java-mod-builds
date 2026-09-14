package kr.moonseungjun.survivalascension.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Preserves already-earned Fractured Archive material rewards across disconnects. */
public final class FracturedArchivePendingData extends SavedData {
    public static final SavedDataType<FracturedArchivePendingData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "fractured_archive_pending_v1"),
            FracturedArchivePendingData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .optionalFieldOf("stones", Map.of())
                            .forGetter(FracturedArchivePendingData::stoneView),
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .optionalFieldOf("scraps", Map.of())
                            .forGetter(FracturedArchivePendingData::scrapView)
            ).apply(instance, FracturedArchivePendingData::new))
    );

    private final Map<String, Integer> stones = new HashMap<>();
    private final Map<String, Integer> scraps = new HashMap<>();

    public FracturedArchivePendingData() {}

    private FracturedArchivePendingData(Map<String, Integer> loadedStones, Map<String, Integer> loadedScraps) {
        copyPositive(loadedStones, stones);
        copyPositive(loadedScraps, scraps);
    }

    public static FracturedArchivePendingData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public void add(UUID playerId, int stoneCount, int scrapCount) {
        if (playerId == null || (stoneCount <= 0 && scrapCount <= 0)) return;
        String key = playerId.toString();
        addBounded(stones, key, stoneCount);
        addBounded(scraps, key, scrapCount);
        setDirty();
    }

    public Reward take(UUID playerId) {
        if (playerId == null) return Reward.EMPTY;
        String key = playerId.toString();
        int stoneCount = Math.max(0, stones.getOrDefault(key, 0));
        int scrapCount = Math.max(0, scraps.getOrDefault(key, 0));
        boolean changed = stones.remove(key) != null;
        changed |= scraps.remove(key) != null;
        if (changed) setDirty();
        return stoneCount <= 0 && scrapCount <= 0 ? Reward.EMPTY : new Reward(stoneCount, scrapCount);
    }

    private Map<String, Integer> stoneView() { return Map.copyOf(stones); }
    private Map<String, Integer> scrapView() { return Map.copyOf(scraps); }

    private static void copyPositive(Map<String, Integer> source, Map<String, Integer> target) {
        if (source == null) return;
        source.forEach((key, value) -> {
            if (key != null && value != null && value > 0) target.put(key, value);
        });
    }

    private static void addBounded(Map<String, Integer> map, String key, int amount) {
        if (amount <= 0) return;
        long next = (long) map.getOrDefault(key, 0) + amount;
        map.put(key, (int) Math.min(Integer.MAX_VALUE, next));
    }

    public record Reward(int stones, int scraps) {
        public static final Reward EMPTY = new Reward(0, 0);
    }
}
