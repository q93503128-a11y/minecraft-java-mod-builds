package kr.moonseungjun.survivalascension.production;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FieldDepotData extends SavedData {
    public static final int MAX_DEPOTS_PER_PLAYER = 3;

    public record DepotEntry(String dimension, int x, int y, int z) {
        private static final Codec<DepotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dimension").forGetter(DepotEntry::dimension),
                Codec.INT.fieldOf("x").forGetter(DepotEntry::x),
                Codec.INT.fieldOf("y").forGetter(DepotEntry::y),
                Codec.INT.fieldOf("z").forGetter(DepotEntry::z)
        ).apply(instance, DepotEntry::new));

        public BlockPos pos() { return new BlockPos(x, y, z); }
        private String key() { return dimension + ":" + x + ":" + y + ":" + z; }
    }

    private record PlayerEntry(String uuid, List<DepotEntry> depots) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                DepotEntry.CODEC.listOf().optionalFieldOf("depots", List.of()).forGetter(PlayerEntry::depots)
        ).apply(instance, PlayerEntry::new));
    }

    public enum AddResult { ADDED, ALREADY_OWNED, CLAIMED_BY_OTHER, LIMIT_REACHED }

    public static final SavedDataType<FieldDepotData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "field_depots_v1"),
            FieldDepotData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(FieldDepotData::entries)
            ).apply(instance, FieldDepotData::new))
    );

    private final Map<String, List<DepotEntry>> players = new HashMap<>();

    public FieldDepotData() {}

    private FieldDepotData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            List<DepotEntry> sanitized = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (DepotEntry depot : entry.depots()) {
                if (sanitized.size() >= MAX_DEPOTS_PER_PLAYER) break;
                if (seen.add(depot.key())) sanitized.add(depot);
            }
            if (!sanitized.isEmpty()) players.put(entry.uuid(), sanitized);
        }
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> out = new ArrayList<>(players.size());
        players.forEach((uuid, depots) -> out.add(new PlayerEntry(uuid, List.copyOf(depots))));
        return out;
    }

    public static FieldDepotData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public static FieldDepotData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    private List<DepotEntry> state(ServerPlayer player) {
        return players.computeIfAbsent(player.getUUID().toString(), ignored -> new ArrayList<>());
    }

    public List<DepotEntry> depots(ServerPlayer player) { return List.copyOf(state(player)); }
    public int count(ServerPlayer player) { return state(player).size(); }

    public boolean owns(ServerPlayer player, String dimension, BlockPos pos) {
        String key = new DepotEntry(dimension, pos.getX(), pos.getY(), pos.getZ()).key();
        for (DepotEntry depot : state(player)) if (depot.key().equals(key)) return true;
        return false;
    }

    public AddResult add(ServerPlayer player, String dimension, BlockPos pos) {
        DepotEntry candidate = new DepotEntry(dimension, pos.getX(), pos.getY(), pos.getZ());
        String playerId = player.getUUID().toString();
        List<DepotEntry> own = state(player);
        for (DepotEntry depot : own) if (depot.key().equals(candidate.key())) return AddResult.ALREADY_OWNED;
        for (Map.Entry<String, List<DepotEntry>> entry : players.entrySet()) {
            if (entry.getKey().equals(playerId)) continue;
            for (DepotEntry depot : entry.getValue()) {
                if (depot.key().equals(candidate.key())) return AddResult.CLAIMED_BY_OTHER;
            }
        }
        if (own.size() >= MAX_DEPOTS_PER_PLAYER) return AddResult.LIMIT_REACHED;
        own.add(candidate);
        setDirty();
        return AddResult.ADDED;
    }

    public boolean remove(ServerPlayer player, DepotEntry depot) {
        List<DepotEntry> own = state(player);
        boolean removed = own.removeIf(value -> value.key().equals(depot.key()));
        if (removed) {
            if (own.isEmpty()) players.remove(player.getUUID().toString());
            setDirty();
        }
        return removed;
    }

    public boolean remove(ServerPlayer player, String dimension, BlockPos pos) {
        return remove(player, new DepotEntry(dimension, pos.getX(), pos.getY(), pos.getZ()));
    }
}
