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
import java.util.List;
import java.util.Map;

public final class FieldRecoveryData extends SavedData {
    public record RecoveryPoint(String dimension, int x, int y, int z) {
        private static final Codec<RecoveryPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dimension").forGetter(RecoveryPoint::dimension),
                Codec.INT.fieldOf("x").forGetter(RecoveryPoint::x),
                Codec.INT.fieldOf("y").forGetter(RecoveryPoint::y),
                Codec.INT.fieldOf("z").forGetter(RecoveryPoint::z)
        ).apply(instance, RecoveryPoint::new));

        public BlockPos pos() { return new BlockPos(x, y, z); }
        public String key() { return dimension + ":" + x + ":" + y + ":" + z; }
    }

    private record PlayerEntry(String uuid, List<RecoveryPoint> armed, List<RecoveryPoint> pending, int recoveries) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                RecoveryPoint.CODEC.listOf().optionalFieldOf("armed", List.of()).forGetter(PlayerEntry::armed),
                RecoveryPoint.CODEC.listOf().optionalFieldOf("pending", List.of()).forGetter(PlayerEntry::pending),
                Codec.INT.optionalFieldOf("recoveries", 0).forGetter(PlayerEntry::recoveries)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<FieldRecoveryData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "field_recovery_v1"),
            FieldRecoveryData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(FieldRecoveryData::entries)
            ).apply(instance, FieldRecoveryData::new))
    );

    private static final class State {
        RecoveryPoint armed;
        RecoveryPoint pending;
        int recoveries;

        State(RecoveryPoint armed, RecoveryPoint pending, int recoveries) {
            this.armed = pending == null ? armed : null;
            this.pending = pending;
            this.recoveries = Math.max(0, recoveries);
        }
    }

    private final Map<String, State> players = new HashMap<>();

    public FieldRecoveryData() {}

    private FieldRecoveryData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            RecoveryPoint armed = entry.armed().isEmpty() ? null : entry.armed().getFirst();
            RecoveryPoint pending = entry.pending().isEmpty() ? null : entry.pending().getFirst();
            players.put(entry.uuid(), new State(armed, pending, entry.recoveries()));
        }
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> out = new ArrayList<>(players.size());
        players.forEach((uuid, state) -> out.add(new PlayerEntry(
                uuid,
                state.armed == null ? List.of() : List.of(state.armed),
                state.pending == null ? List.of() : List.of(state.pending),
                state.recoveries
        )));
        return out;
    }

    public static FieldRecoveryData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public static FieldRecoveryData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    private State state(ServerPlayer player) {
        return players.computeIfAbsent(player.getUUID().toString(), ignored -> new State(null, null, 0));
    }

    public RecoveryPoint armed(ServerPlayer player) { return state(player).armed; }
    public RecoveryPoint pending(ServerPlayer player) { return state(player).pending; }
    public int recoveries(ServerPlayer player) { return state(player).recoveries; }

    public void arm(ServerPlayer player, String dimension, BlockPos pos) {
        State state = state(player);
        state.pending = null;
        state.armed = new RecoveryPoint(dimension, pos.getX(), pos.getY(), pos.getZ());
        setDirty();
    }

    public boolean queuePending(ServerPlayer player) {
        State state = state(player);
        if (state.armed == null || state.pending != null) return false;
        state.pending = state.armed;
        state.armed = null;
        setDirty();
        return true;
    }

    public void rearmPending(ServerPlayer player, String dimension, BlockPos pos) {
        State state = state(player);
        state.pending = null;
        state.armed = new RecoveryPoint(dimension, pos.getX(), pos.getY(), pos.getZ());
        setDirty();
    }

    public void completePending(ServerPlayer player) {
        State state = state(player);
        if (state.pending == null) return;
        state.pending = null;
        state.recoveries++;
        setDirty();
    }
}
