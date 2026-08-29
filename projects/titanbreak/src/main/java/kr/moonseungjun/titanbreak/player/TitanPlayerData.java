package kr.moonseungjun.titanbreak.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TitanPlayerData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    private record PlayerEntry(String uuid, double sanity, double heat, int researchData,
                               List<String> normalFirstKills, int schemaVersion) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.DOUBLE.optionalFieldOf("sanity", 100.0).forGetter(PlayerEntry::sanity),
                Codec.DOUBLE.optionalFieldOf("heat", 0.0).forGetter(PlayerEntry::heat),
                Codec.INT.optionalFieldOf("research_data", 0).forGetter(PlayerEntry::researchData),
                Codec.STRING.listOf().optionalFieldOf("normal_first_kills", List.of()).forGetter(PlayerEntry::normalFirstKills),
                Codec.INT.optionalFieldOf("schema_version", 1).forGetter(PlayerEntry::schemaVersion)
        ).apply(instance, PlayerEntry::new));
    }

    // Keep the storage id stable so alpha.7 and older profiles migrate instead of starting over.
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
            if (entry.schemaVersion() > CURRENT_SCHEMA_VERSION) {
                Titanbreak.LOGGER.warn("TITANBREAK player profile {} uses newer schema {} (supported: {}). Preserving its schema marker without downgrading it.",
                        entry.uuid(), entry.schemaVersion(), CURRENT_SCHEMA_VERSION);
            }
            State state = new State(entry.sanity(), entry.heat(), entry.researchData(),
                    entry.normalFirstKills(), entry.schemaVersion());
            state.migrateKnownSchemas();
            players.put(entry.uuid(), state);
        }
    }

    private List<PlayerEntry> entries() {
        return players.entrySet().stream()
                .map(entry -> {
                    State state = entry.getValue();
                    List<String> kills = new ArrayList<>(state.normalFirstKills);
                    kills.sort(String::compareTo);
                    return new PlayerEntry(entry.getKey(), state.sanity, state.heat,
                            state.researchData, kills, state.schemaVersion);
                })
                .toList();
    }

    public static TitanPlayerData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean ensureProfile(ServerPlayer player) {
        String key = player.getUUID().toString();
        if (players.containsKey(key)) return false;
        players.put(key, new State(100.0, 0.0, 0, List.of(), CURRENT_SCHEMA_VERSION));
        setDirty();
        return true;
    }

    public State state(ServerPlayer player) {
        ensureProfile(player);
        return players.get(player.getUUID().toString());
    }

    public void setHeat(ServerPlayer player, double value) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion()) return;
        double clamped = clamp(value, 0.0, 100.0);
        if (Math.abs(state.heat - clamped) < 0.001) return;
        state.heat = clamped;
        setDirty();
    }

    public void setSanity(ServerPlayer player, double value) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion()) return;
        double clamped = clamp(value, 0.0, 100.0);
        if (Math.abs(state.sanity - clamped) < 0.001) return;
        state.sanity = clamped;
        setDirty();
    }

    public boolean recordNormalFirstKill(ServerPlayer player, String speciesKey, int researchReward) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion() || !state.normalFirstKills.add(speciesKey)) return false;
        state.researchData = Math.max(0, state.researchData + Math.max(0, researchReward));
        setDirty();
        return true;
    }

    public void addResearchData(ServerPlayer player, int amount) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion() || amount == 0) return;
        state.researchData = Math.max(0, state.researchData + amount);
        setDirty();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class State {
        private double sanity;
        private double heat;
        private int researchData;
        private final Set<String> normalFirstKills;
        private int schemaVersion;

        private State(double sanity, double heat, int researchData,
                      List<String> normalFirstKills, int schemaVersion) {
            this.sanity = clamp(sanity, 0.0, 100.0);
            this.heat = clamp(heat, 0.0, 100.0);
            this.researchData = Math.max(0, researchData);
            this.normalFirstKills = new LinkedHashSet<>(normalFirstKills);
            this.schemaVersion = schemaVersion;
        }

        private void migrateKnownSchemas() {
            if (schemaVersion < 1) schemaVersion = 1;
            if (schemaVersion == 1) schemaVersion = 2;
        }

        public boolean isWritableByCurrentVersion() {
            return schemaVersion <= CURRENT_SCHEMA_VERSION;
        }

        public double sanity() { return sanity; }
        public double heat() { return heat; }
        public int researchData() { return researchData; }
        public boolean hasNormalFirstKill(String speciesKey) { return normalFirstKills.contains(speciesKey); }
        public int schemaVersion() { return schemaVersion; }
    }
}
