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

public final class OutpostData extends SavedData {
    public static final int MAX_OUTPOSTS_PER_PLAYER = FieldDepotData.MAX_DEPOTS_PER_PLAYER;

    public record OutpostEntry(String dimension, int x, int y, int z) {
        private static final Codec<OutpostEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dimension").forGetter(OutpostEntry::dimension),
                Codec.INT.fieldOf("x").forGetter(OutpostEntry::x),
                Codec.INT.fieldOf("y").forGetter(OutpostEntry::y),
                Codec.INT.fieldOf("z").forGetter(OutpostEntry::z)
        ).apply(instance, OutpostEntry::new));

        public BlockPos pos() { return new BlockPos(x, y, z); }
        private String key() { return dimension + ":" + x + ":" + y + ":" + z; }
    }

    private record PlayerEntry(String uuid, List<OutpostEntry> outposts) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                OutpostEntry.CODEC.listOf().optionalFieldOf("outposts", List.of()).forGetter(PlayerEntry::outposts)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<OutpostData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "outpost_v1"),
            OutpostData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(OutpostData::entries)
            ).apply(instance, OutpostData::new))
    );

    private final Map<String, List<OutpostEntry>> players = new HashMap<>();

    public OutpostData() {}

    private OutpostData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            List<OutpostEntry> sanitized = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (OutpostEntry outpost : entry.outposts()) {
                if (sanitized.size() >= MAX_OUTPOSTS_PER_PLAYER) break;
                if (seen.add(outpost.key())) sanitized.add(outpost);
            }
            if (!sanitized.isEmpty()) players.put(entry.uuid(), sanitized);
        }
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> out = new ArrayList<>(players.size());
        players.forEach((uuid, outposts) -> out.add(new PlayerEntry(uuid, List.copyOf(outposts))));
        return out;
    }

    public static OutpostData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public static OutpostData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    private List<OutpostEntry> state(ServerPlayer player) {
        return players.computeIfAbsent(player.getUUID().toString(), ignored -> new ArrayList<>());
    }

    public List<OutpostEntry> outposts(ServerPlayer player) { return List.copyOf(state(player)); }
    public int count(ServerPlayer player) { return state(player).size(); }

    public boolean isOutpost(ServerPlayer player, String dimension, BlockPos pos) {
        String key = new OutpostEntry(dimension, pos.getX(), pos.getY(), pos.getZ()).key();
        for (OutpostEntry entry : state(player)) if (entry.key().equals(key)) return true;
        return false;
    }

    public boolean upgrade(ServerPlayer player, String dimension, BlockPos pos) {
        if (isOutpost(player, dimension, pos) || state(player).size() >= MAX_OUTPOSTS_PER_PLAYER) return false;
        state(player).add(new OutpostEntry(dimension, pos.getX(), pos.getY(), pos.getZ()));
        setDirty();
        return true;
    }

    public boolean remove(ServerPlayer player, String dimension, BlockPos pos) {
        List<OutpostEntry> own = state(player);
        String key = new OutpostEntry(dimension, pos.getX(), pos.getY(), pos.getZ()).key();
        boolean removed = own.removeIf(entry -> entry.key().equals(key));
        if (removed) {
            if (own.isEmpty()) players.remove(player.getUUID().toString());
            setDirty();
        }
        return removed;
    }
}
