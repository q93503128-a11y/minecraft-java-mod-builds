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
import java.util.UUID;

public final class TitanPlayerData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 5;
    public static final int MAX_ADAPTATION_LEVEL = 50;
    public static final int MAX_AUGMENT_MK = 5;
    public static final int MAX_ENHANCEMENT = 10;
    private static final int[] MASTERY_THRESHOLDS = {0, 80, 260, 650, 1400, 2600};

    public record AugmentInstance(String id, int mk, int enhancement, String serial) {
        public AugmentInstance {
            mk = Math.max(1, Math.min(MAX_AUGMENT_MK, mk));
            enhancement = Math.max(0, Math.min(MAX_ENHANCEMENT, enhancement));
            if (serial == null || serial.isBlank()) serial = UUID.randomUUID().toString();
        }

        public static AugmentInstance fresh(String id) {
            return new AugmentInstance(id, 1, 0, UUID.randomUUID().toString());
        }

        public String encode() {
            return id + "|" + mk + "|" + enhancement + "|" + serial;
        }

        public AugmentInstance withMk(int value) {
            return new AugmentInstance(id, value, enhancement, serial);
        }

        public AugmentInstance withEnhancement(int value) {
            return new AugmentInstance(id, mk, value, serial);
        }

        public static AugmentInstance decode(String encoded) {
            if (encoded == null || encoded.isBlank()) return null;
            String[] parts = encoded.split("\\|", 4);
            String id = parts[0];
            if (AugmentationCatalog.byId(id) == null) return null;
            int mk = 1;
            int enhancement = 0;
            try {
                if (parts.length > 1) mk = Integer.parseInt(parts[1]);
                if (parts.length > 2) enhancement = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {}
            String serial = parts.length > 3 && !parts[3].isBlank() ? parts[3] : UUID.randomUUID().toString();
            return new AugmentInstance(id, mk, enhancement, serial);
        }
    }

    private record PlayerEntry(String uuid, double sanity, double heat, int researchData,
                               List<String> normalFirstKills, List<String> eliteFirstKills,
                               List<String> bossFirstKills, List<String> installedAugments,
                               List<String> vaultAugments, List<String> masteryXp,
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
                Codec.STRING.listOf().optionalFieldOf("vault_augments", List.of()).forGetter(PlayerEntry::vaultAugments),
                Codec.STRING.listOf().optionalFieldOf("mastery_xp", List.of()).forGetter(PlayerEntry::masteryXp),
                Codec.INT.optionalFieldOf("adaptation_level", 1).forGetter(PlayerEntry::adaptationLevel),
                Codec.INT.optionalFieldOf("adaptation_xp", 0).forGetter(PlayerEntry::adaptationXp),
                Codec.INT.optionalFieldOf("adaptation_points", 0).forGetter(PlayerEntry::adaptationPoints),
                Codec.INT.optionalFieldOf("schema_version", 1).forGetter(PlayerEntry::schemaVersion)
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
            if (entry.schemaVersion() > CURRENT_SCHEMA_VERSION) {
                Titanbreak.LOGGER.warn("TITANBREAK player profile {} uses newer schema {} (supported: {}). Preserving it read-only.",
                        entry.uuid(), entry.schemaVersion(), CURRENT_SCHEMA_VERSION);
            }
            State state = new State(entry.sanity(), entry.heat(), entry.researchData(),
                    entry.normalFirstKills(), entry.eliteFirstKills(), entry.bossFirstKills(),
                    entry.installedAugments(), entry.vaultAugments(), entry.masteryXp(),
                    entry.adaptationLevel(), entry.adaptationXp(), entry.adaptationPoints(), entry.schemaVersion());
            state.migrateKnownSchemas();
            players.put(entry.uuid(), state);
        }
    }

    private List<PlayerEntry> entries() {
        return players.entrySet().stream().map(entry -> {
            State state = entry.getValue();
            List<String> installed = new ArrayList<>();
            for (Map.Entry<AugmentationCatalog.Slot, AugmentInstance> augment : state.installed.entrySet()) {
                installed.add(augment.getKey().name() + "=" + augment.getValue().encode());
            }
            installed.sort(String::compareTo);
            List<String> vault = state.vault.stream().map(AugmentInstance::encode).toList();
            List<String> mastery = state.masteryXp.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(value -> value.getKey() + "=" + value.getValue())
                    .toList();
            return new PlayerEntry(entry.getKey(), state.sanity, state.heat, state.researchData,
                    sorted(state.normalFirstKills), sorted(state.eliteFirstKills), sorted(state.bossFirstKills),
                    installed, vault, mastery, state.adaptationLevel, state.adaptationXp,
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
                List.of(), List.of(), List.of(), 1, 0, 0, CURRENT_SCHEMA_VERSION));
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
        setDirty();
        return levels;
    }

    public static int xpForNext(int currentLevel) {
        return 100 + Math.max(0, currentLevel - 1) * 40;
    }

    public boolean install(ServerPlayer player, AugmentationCatalog.Slot anchor, String augmentId) {
        return installInstance(player, anchor, AugmentInstance.fresh(augmentId));
    }

    public boolean installInstance(ServerPlayer player, AugmentationCatalog.Slot anchor, AugmentInstance instance) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion() || instance == null) return false;
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(instance.id());
        if (definition == null) return false;
        AugmentationCatalog.Placement placement = definition.placementFor(anchor);
        if (placement == null) return false;
        for (AugmentationCatalog.Slot slot : placement.slots()) if (state.installed.containsKey(slot)) return false;
        for (AugmentationCatalog.Slot slot : placement.slots()) state.installed.put(slot, instance);
        setDirty();
        return true;
    }

    public AugmentInstance removeInstance(ServerPlayer player, AugmentationCatalog.Slot slot) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion()) return null;
        AugmentInstance removed = state.installed.get(slot);
        if (removed == null) return null;
        state.installed.entrySet().removeIf(entry -> entry.getValue().serial().equals(removed.serial()));
        setDirty();
        return removed;
    }

    public String remove(ServerPlayer player, AugmentationCatalog.Slot slot) {
        AugmentInstance instance = removeInstance(player, slot);
        return instance == null ? null : instance.id();
    }

    public boolean storeVault(ServerPlayer player, AugmentInstance instance) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion() || instance == null) return false;
        for (AugmentInstance stored : state.vault) if (stored.serial().equals(instance.serial())) return false;
        state.vault.add(instance);
        setDirty();
        return true;
    }

    public AugmentInstance takeVault(ServerPlayer player, String augmentId) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion()) return null;
        for (int i = 0; i < state.vault.size(); i++) {
            AugmentInstance instance = state.vault.get(i);
            if (!instance.id().equals(augmentId)) continue;
            state.vault.remove(i);
            setDirty();
            return instance;
        }
        return null;
    }

    public AugmentInstance firstInstance(ServerPlayer player, String augmentId) {
        return state(player).firstInstance(augmentId);
    }

    public boolean enhance(ServerPlayer player, String augmentId) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion()) return false;
        AugmentInstance current = state.firstInstance(augmentId);
        if (current == null || current.enhancement() >= MAX_ENHANCEMENT) return false;
        state.replaceInstance(current, current.withEnhancement(current.enhancement() + 1));
        setDirty();
        return true;
    }

    public boolean upgradeMk(ServerPlayer player, String augmentId) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion()) return false;
        AugmentInstance current = state.firstInstance(augmentId);
        if (current == null || current.mk() >= MAX_AUGMENT_MK) return false;
        state.replaceInstance(current, current.withMk(current.mk() + 1));
        setDirty();
        return true;
    }

    public int addMasteryXp(ServerPlayer player, String augmentId, int amount) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion() || AugmentationCatalog.byId(augmentId) == null || amount <= 0) return 0;
        int previousLevel = state.masteryLevel(augmentId);
        int next = Math.max(0, state.masteryXp.getOrDefault(augmentId, 0) + amount);
        state.masteryXp.put(augmentId, next);
        setDirty();
        return Math.max(0, state.masteryLevel(augmentId) - previousLevel);
    }

    public void addMasteryXpToInstalled(ServerPlayer player, int amount) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion() || amount <= 0) return;
        Set<String> families = new LinkedHashSet<>();
        for (AugmentInstance instance : state.installed.values()) families.add(instance.id());
        if (families.isEmpty()) return;
        for (String id : families) state.masteryXp.put(id, state.masteryXp.getOrDefault(id, 0) + amount);
        setDirty();
    }

    public static int masteryLevelForXp(int xp) {
        int level = 0;
        for (int i = 1; i < MASTERY_THRESHOLDS.length; i++) {
            if (xp < MASTERY_THRESHOLDS[i]) break;
            level = i;
        }
        return level;
    }

    public boolean hasInstalled(ServerPlayer player, String augmentId) {
        return state(player).hasInstalled(augmentId);
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
        private final EnumMap<AugmentationCatalog.Slot, AugmentInstance> installed = new EnumMap<>(AugmentationCatalog.Slot.class);
        private final List<AugmentInstance> vault = new ArrayList<>();
        private final Map<String, Integer> masteryXp = new HashMap<>();
        private int adaptationLevel;
        private int adaptationXp;
        private int adaptationPoints;
        private int schemaVersion;

        private State(double sanity, double heat, int researchData,
                      List<String> normalFirstKills, List<String> eliteFirstKills,
                      List<String> bossFirstKills, List<String> installedAugments,
                      List<String> vaultAugments, List<String> masteryXp,
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
            for (String entry : installedAugments) loadInstalled(entry);
            for (String entry : vaultAugments) {
                AugmentInstance instance = AugmentInstance.decode(entry);
                if (instance != null) vault.add(instance);
            }
            for (String entry : masteryXp) loadMastery(entry);
        }

        private void loadMastery(String entry) {
            int split = entry.indexOf('=');
            if (split <= 0 || split >= entry.length() - 1) return;
            String id = entry.substring(0, split);
            if (AugmentationCatalog.byId(id) == null) return;
            try {
                masteryXp.put(id, Math.max(0, Integer.parseInt(entry.substring(split + 1))));
            } catch (NumberFormatException ignored) {}
        }

        private void loadInstalled(String entry) {
            int split = entry.indexOf('=');
            if (split <= 0 || split >= entry.length() - 1) return;
            String slotName = entry.substring(0, split);
            String encoded = entry.substring(split + 1);
            AugmentInstance augment = AugmentInstance.decode(encoded);
            if (augment == null && AugmentationCatalog.byId(encoded) != null) augment = AugmentInstance.fresh(encoded);
            if (augment == null) return;

            try {
                installed.putIfAbsent(AugmentationCatalog.Slot.valueOf(slotName), augment);
                return;
            } catch (IllegalArgumentException ignored) {}

            switch (slotName) {
                case "EYE" -> installed.putIfAbsent(AugmentationCatalog.Slot.EYE_1, augment);
                case "BRAIN" -> installed.putIfAbsent(AugmentationCatalog.Slot.BRAIN_1, augment);
                case "NERVES" -> installed.putIfAbsent(AugmentationCatalog.Slot.NERVES_1, augment);
                case "SPINE" -> installed.putIfAbsent(AugmentationCatalog.Slot.SPINE_MAIN, augment);
                case "SKELETON" -> installed.putIfAbsent(AugmentationCatalog.Slot.SKELETON_1, augment);
                case "SKIN" -> installed.putIfAbsent(AugmentationCatalog.Slot.SKIN_1, augment);
                case "LEFT_ARM" -> installed.putIfAbsent(AugmentationCatalog.Slot.LEFT_ARM_MAIN, augment);
                case "RIGHT_ARM" -> installed.putIfAbsent(AugmentationCatalog.Slot.RIGHT_ARM_MAIN, augment);
                case "LEGS" -> {
                    installed.putIfAbsent(AugmentationCatalog.Slot.LEFT_LEG_MAIN, augment);
                    installed.putIfAbsent(AugmentationCatalog.Slot.RIGHT_LEG_MAIN, augment);
                }
                default -> { }
            }
        }

        private void replaceInstance(AugmentInstance previous, AugmentInstance next) {
            for (Map.Entry<AugmentationCatalog.Slot, AugmentInstance> entry : installed.entrySet()) {
                if (entry.getValue().serial().equals(previous.serial())) entry.setValue(next);
            }
            for (int i = 0; i < vault.size(); i++) {
                if (vault.get(i).serial().equals(previous.serial())) vault.set(i, next);
            }
        }

        private void migrateKnownSchemas() {
            if (schemaVersion < 1) schemaVersion = 1;
            if (schemaVersion == 1) schemaVersion = 2;
            if (schemaVersion == 2) schemaVersion = 3;
            if (schemaVersion == 3) schemaVersion = 4;
            if (schemaVersion == 4) schemaVersion = 5;
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

        public String installed(AugmentationCatalog.Slot slot) {
            AugmentInstance instance = installed.get(slot);
            return instance == null ? null : instance.id();
        }

        public AugmentInstance installedInstance(AugmentationCatalog.Slot slot) { return installed.get(slot); }

        public AugmentInstance firstInstalledInstance(String augmentId) {
            for (AugmentInstance instance : installed.values()) if (instance.id().equals(augmentId)) return instance;
            return null;
        }

        public AugmentInstance firstInstance(String augmentId) {
            AugmentInstance installed = firstInstalledInstance(augmentId);
            if (installed != null) return installed;
            for (AugmentInstance instance : vault) if (instance.id().equals(augmentId)) return instance;
            return null;
        }

        public boolean hasInstalled(String augmentId) { return firstInstalledInstance(augmentId) != null; }

        public Map<AugmentationCatalog.Slot, String> installedView() {
            Map<AugmentationCatalog.Slot, String> result = new EnumMap<>(AugmentationCatalog.Slot.class);
            for (Map.Entry<AugmentationCatalog.Slot, AugmentInstance> entry : installed.entrySet()) result.put(entry.getKey(), entry.getValue().id());
            return Map.copyOf(result);
        }

        public Map<AugmentationCatalog.Slot, AugmentInstance> installedInstanceView() { return Map.copyOf(installed); }
        public List<AugmentInstance> vaultView() { return List.copyOf(vault); }
        public int masteryXp(String augmentId) { return masteryXp.getOrDefault(augmentId, 0); }
        public int masteryLevel(String augmentId) { return masteryLevelForXp(masteryXp(augmentId)); }
        public double powerLoadMultiplier(String augmentId) { return masteryLevel(augmentId) >= 1 ? 0.97D : 1.0D; }
        public double heatLoadMultiplier(String augmentId) { return masteryLevel(augmentId) >= 1 ? 0.97D : 1.0D; }
        public double neuralLoadMultiplier(String augmentId) { return masteryLevel(augmentId) >= 3 ? 0.95D : 1.0D; }
        public int schemaVersion() { return schemaVersion; }
    }
}
