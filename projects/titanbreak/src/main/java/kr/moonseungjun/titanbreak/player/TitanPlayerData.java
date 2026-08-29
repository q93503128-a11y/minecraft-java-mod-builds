package kr.moonseungjun.titanbreak.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TitanPlayerData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 3;
    public static final int MAX_ADAPTATION_LEVEL = 50;

    private record PlayerEntry(String uuid, double sanity, double heat, int researchData,
                               List<String> normalFirstKills, List<String> eliteFirstKills,
                               List<String> bossFirstKills, List<String> installedAugments,
                               int adaptationLevel, int adaptationXp, int adaptationPoints,
                               int schemaVersion) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.DOUBLE.optionalFieldOf("sanity", 100.0D).forGetter(PlayerEntry::sanity),
                Codec.DOUBLE.optionalFieldOf("heat", 0.0D).forGetter(PlayerEntry::heat),
                Codec.INT.optionalFieldOf("research_data", 0).forGetter(PlayerEntry::researchData),
                Codec.STRING.listOf().optionalFieldOf("normal_first_kills", List.of()).forGetter(PlayerEntry::normalFirstKills),
                Codec.STRING.listOf().optionalFieldOf("elite_first_kills", List.of()).forGetter(PlayerEntry::eliteFirstKills),
                Codec.STRING.listOf().optionalFieldOf("boss_first_kills", List.of()).forGetter(PlayerEntry::bossFirstKills),
                Codec.STRING.listOf().optionalFieldOf("installed_augments", List.of()).forGetter(PlayerEntry::installedAugments),
                Codec.INT.optionalFieldOf("adaptation_level", 1).forGetter(PlayerEntry::adaptationLevel),
                Codec.INT.optionalFieldOf("adaptation_xp", 0).forGetter(PlayerEntry::adaptationXp),
                Codec.INT.optionalFieldOf("adaptation_points", 0).forGetter(PlayerEntry::adaptationPoints),
                Codec.INT.optionalFieldOf("schema_version", 1).forGetter(PlayerEntry::schemaVersion)
        ).apply(instance, PlayerEntry::new));
    }

    // Keep this storage id stable so prior worlds migrate in-place.
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
                Titanbreak.LOGGER.warn("TITANBREAK player profile {} uses newer schema {} (supported: {}). Preserving it read-only.",
                        entry.uuid(), entry.schemaVersion(), CURRENT_SCHEMA_VERSION);
            }
            State state = new State(entry.sanity(), entry.heat(), entry.researchData(),
                    entry.normalFirstKills(), entry.eliteFirstKills(), entry.bossFirstKills(),
                    entry.installedAugments(), entry.adaptationLevel(), entry.adaptationXp(),
                    entry.adaptationPoints(), entry.schemaVersion());
            state.migrateKnownSchemas();
            players.put(entry.uuid(), state);
        }
    }

    private List<PlayerEntry> entries() {
        return players.entrySet().stream().map(entry -> {
            State state = entry.getValue();
            List<String> normal = sorted(state.normalFirstKills);
            List<String> elite = sorted(state.eliteFirstKills);
            List<String> boss = sorted(state.bossFirstKills);
            List<String> installed = new ArrayList<>();
            for (Map.Entry<AugmentationCatalog.Slot, String> augment : state.installed.entrySet()) {
                installed.add(augment.getKey().name() + "=" + augment.getValue());
            }
            installed.sort(String::compareTo);
            return new PlayerEntry(entry.getKey(), state.sanity, state.heat, state.researchData,
                    normal, elite, boss, installed, state.adaptationLevel, state.adaptationXp,
                    state.adaptationPoints, state.schemaVersion);
        }).toList();
    }

    private static List<String> sorted(Set<String> values) {
        List<String> result = new ArrayList<>(values);
        result.sort(String::compareTo);
        return result;
    }

    public static TitanPlayerData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean ensureProfile(ServerPlayer player) {
        String key = player.getUUID().toString();
        if (players.containsKey(key)) return false;
        players.put(key, new State(100.0D, 0.0D, 0, List.of(), List.of(), List.of(),
                List.of(), 1, 0, 0, CURRENT_SCHEMA_VERSION));
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
        double clamped = clamp(value, 0.0D, 100.0D);
        if (Math.abs(state.heat - clamped) < 0.001D) return;
        state.heat = clamped;
        setDirty();
    }

    public void setSanity(ServerPlayer player, double value) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion()) return;
        double clamped = clamp(value, 0.0D, 100.0D);
        if (Math.abs(state.sanity - clamped) < 0.001D) return;
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

    public boolean recordEliteFirstKill(ServerPlayer player, String speciesKey, int researchReward) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion() || !state.eliteFirstKills.add(speciesKey)) return false;
        state.researchData = Math.max(0, state.researchData + Math.max(0, researchReward));
        setDirty();
        return true;
    }

    public boolean recordBossFirstKill(ServerPlayer player, String speciesKey, int researchReward, int bonusPoints) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion() || !state.bossFirstKills.add(speciesKey)) return false;
        state.researchData = Math.max(0, state.researchData + Math.max(0, researchReward));
        state.adaptationPoints = Math.max(0, state.adaptationPoints + Math.max(0, bonusPoints));
        setDirty();
        return true;
    }

    public void addResearchData(ServerPlayer player, int amount) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion() || amount == 0) return;
        state.researchData = Math.max(0, state.researchData + amount);
        setDirty();
    }

    public int addAdaptationXp(ServerPlayer player, int amount) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion() || amount <= 0 || state.adaptationLevel >= MAX_ADAPTATION_LEVEL) return 0;
        state.adaptationXp += amount;
        int levels = 0;
        while (state.adaptationLevel < MAX_ADAPTATION_LEVEL) {
            int needed = xpForNext(state.adaptationLevel);
            if (state.adaptationXp < needed) break;
            state.adaptationXp -= needed;
            state.adaptationLevel++;
            state.adaptationPoints++;
            levels++;
        }
        if (levels > 0 || amount > 0) setDirty();
        return levels;
    }

    public static int xpForNext(int currentLevel) {
        return 100 + Math.max(0, currentLevel - 1) * 40;
    }

    public boolean install(ServerPlayer player, AugmentationCatalog.Slot slot, String augmentId) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion() || state.installed.containsKey(slot)) return false;
        state.installed.put(slot, augmentId);
        setDirty();
        return true;
    }

    public String remove(ServerPlayer player, AugmentationCatalog.Slot slot) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion()) return null;
        String removed = state.installed.remove(slot);
        if (removed != null) setDirty();
        return removed;
    }

    public boolean hasInstalled(ServerPlayer player, String augmentId) {
        return state(player).installed.containsValue(augmentId);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class State {
        private double sanity;
        private double heat;
        private int researchData;
        private final Set<String> normalFirstKills;
        private final Set<String> eliteFirstKills;
        private final Set<String> bossFirstKills;
        private final EnumMap<AugmentationCatalog.Slot, String> installed = new EnumMap<>(AugmentationCatalog.Slot.class);
        private int adaptationLevel;
        private int adaptationXp;
        private int adaptationPoints;
        private int schemaVersion;

        private State(double sanity, double heat, int researchData,
                      List<String> normalFirstKills, List<String> eliteFirstKills,
                      List<String> bossFirstKills, List<String> installedAugments,
                      int adaptationLevel, int adaptationXp, int adaptationPoints,
                      int schemaVersion) {
            this.sanity = clamp(sanity, 0.0D, 100.0D);
            this.heat = clamp(heat, 0.0D, 100.0D);
            this.researchData = Math.max(0, researchData);
            this.normalFirstKills = new LinkedHashSet<>(normalFirstKills);
            this.eliteFirstKills = new LinkedHashSet<>(eliteFirstKills);
            this.bossFirstKills = new LinkedHashSet<>(bossFirstKills);
            this.adaptationLevel = Math.max(1, Math.min(MAX_ADAPTATION_LEVEL, adaptationLevel));
            this.adaptationXp = Math.max(0, adaptationXp);
            this.adaptationPoints = Math.max(0, adaptationPoints);
            this.schemaVersion = schemaVersion;
            for (String entry : installedAugments) {
                int split = entry.indexOf('=');
                if (split <= 0 || split >= entry.length() - 1) continue;
                try {
                    AugmentationCatalog.Slot slot = AugmentationCatalog.Slot.valueOf(entry.substring(0, split));
                    String augment = entry.substring(split + 1);
                    if (AugmentationCatalog.byId(augment) != null) installed.put(slot, augment);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        private void migrateKnownSchemas() {
            if (schemaVersion < 1) schemaVersion = 1;
            if (schemaVersion == 1) schemaVersion = 2;
            if (schemaVersion == 2) schemaVersion = 3;
        }

        public boolean isWritableByCurrentVersion() {
            return schemaVersion <= CURRENT_SCHEMA_VERSION;
        }

        public double sanity() { return sanity; }
        public double heat() { return heat; }
        public int researchData() { return researchData; }
        public int adaptationLevel() { return adaptationLevel; }
        public int adaptationXp() { return adaptationXp; }
        public int adaptationPoints() { return adaptationPoints; }
        public boolean hasNormalFirstKill(String speciesKey) { return normalFirstKills.contains(speciesKey); }
        public boolean hasEliteFirstKill(String speciesKey) { return eliteFirstKills.contains(speciesKey); }
        public boolean hasBossFirstKill(String speciesKey) { return bossFirstKills.contains(speciesKey); }
        public int normalFirstKillCount() { return normalFirstKills.size(); }
        public int eliteFirstKillCount() { return eliteFirstKills.size(); }
        public String installed(AugmentationCatalog.Slot slot) { return installed.get(slot); }
        public boolean hasInstalled(String augmentId) { return installed.containsValue(augmentId); }
        public Map<AugmentationCatalog.Slot, String> installedView() { return Map.copyOf(installed); }
        public int schemaVersion() { return schemaVersion; }
    }
}
