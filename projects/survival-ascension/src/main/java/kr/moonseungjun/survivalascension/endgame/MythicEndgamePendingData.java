package kr.moonseungjun.survivalascension.endgame;

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

/** Preserves qualified Mythic III kill credit while a contributor is offline. */
public final class MythicEndgamePendingData extends SavedData {
    public static final SavedDataType<MythicEndgamePendingData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mythic_endgame_pending_v1"),
            MythicEndgamePendingData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .optionalFieldOf("pending", Map.of())
                            .forGetter(MythicEndgamePendingData::pendingView)
            ).apply(instance, MythicEndgamePendingData::new))
    );

    private final Map<String, Integer> pending = new HashMap<>();

    public MythicEndgamePendingData() {}

    private MythicEndgamePendingData(Map<String, Integer> loaded) {
        if (loaded == null) return;
        loaded.forEach((key, count) -> {
            if (key != null && count != null && count > 0) pending.put(key, count);
        });
    }

    public static MythicEndgamePendingData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private Map<String, Integer> pendingView() {
        return Map.copyOf(pending);
    }

    public void add(UUID playerId, int count) {
        if (playerId == null || count <= 0) return;
        String key = playerId.toString();
        long next = (long) pending.getOrDefault(key, 0) + count;
        pending.put(key, (int) Math.min(Integer.MAX_VALUE, next));
        setDirty();
    }

    public int take(UUID playerId) {
        if (playerId == null) return 0;
        Integer count = pending.remove(playerId.toString());
        if (count != null) setDirty();
        return count == null ? 0 : Math.max(0, count);
    }
}
