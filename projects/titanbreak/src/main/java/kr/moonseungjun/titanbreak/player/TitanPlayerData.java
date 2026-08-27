package kr.moonseungjun.titanbreak.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TitanPlayerData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private record PlayerEntry(String uuid, double sanity, double heat, int schemaVersion) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.DOUBLE.optionalFieldOf("sanity", 100.0).forGetter(PlayerEntry::sanity),
                Codec.DOUBLE.optionalFieldOf("heat", 0.0).forGetter(PlayerEntry::heat),
                Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(PlayerEntry::schemaVersion)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<TitanPlayerData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "player_profiles_v1"),
            TitanPlayerData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of())
                            .forGetter(TitanPlayerData::entries)
            ).apply(instance, TitanPlayerData::new))
    );

    private final Map<String, State> players = new HashMap<>();

    public TitanPlayerData() {}

    private TitanPlayerData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            State state = new State(entry.sanity(), entry.heat(), entry.schemaVersion());
            state.migrate();
            players.put(entry.uuid(), state);
        }
    }

    private List<PlayerEntry> entries() {
        return players.entrySet().stream()
                .map(entry -> new PlayerEntry(entry.getKey(), entry.getValue().sanity,
                        entry.getValue().heat, entry.getValue().schemaVersion))
                .toList();
    }

    public static TitanPlayerData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean ensureProfile(ServerPlayer player) {
        String key = player.getUUID().toString();
        if (players.containsKey(key)) return false;
        players.put(key, new State(100.0, 0.0, CURRENT_SCHEMA_VERSION));
        setDirty();
        return true;
    }

    public State state(ServerPlayer player) {
        ensureProfile(player);
        return players.get(player.getUUID().toString());
    }

    public void setHeat(ServerPlayer player, double value) {
        State state = state(player);
        double clamped = clamp(value, 0.0, 100.0);
        if (Math.abs(state.heat - clamped) < 0.001) return;
        state.heat = clamped;
        setDirty();
    }

    public void setSanity(ServerPlayer player, double value) {
        State state = state(player);
        double clamped = clamp(value, 0.0, 100.0);
        if (Math.abs(state.sanity - clamped) < 0.001) return;
        state.sanity = clamped;
        setDirty();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class State {
        private double sanity;
        private double heat;
        private int schemaVersion;

        private State(double sanity, double heat, int schemaVersion) {
            this.sanity = clamp(sanity, 0.0, 100.0);
            this.heat = clamp(heat, 0.0, 100.0);
            this.schemaVersion = schemaVersion;
        }

        private void migrate() {
            if (schemaVersion < 1) schemaVersion = 1;
            if (schemaVersion > CURRENT_SCHEMA_VERSION) schemaVersion = CURRENT_SCHEMA_VERSION;
        }

        public double sanity() { return sanity; }
        public double heat() { return heat; }
        public int schemaVersion() { return schemaVersion; }
    }
}
