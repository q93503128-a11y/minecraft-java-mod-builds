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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TitanPlayerData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 6;
    public static final int MAX_ADAPTATION_LEVEL = 50;
    public static final int MAX_AUGMENT_MK = 5;
    public static final int MAX_ENHANCEMENT = 10;
    private static final int[] MASTERY_THRESHOLDS = {0, 80, 260, 650, 1400, 2600};
    private static final String AUGMENT_ENCODING_VERSION = "v2";
    private static final String QUARANTINE_VERSION = "q1";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final Map<String, String> ID_ALIASES = buildIdAliasMap();

    public record AugmentInstance(String id, int mk, int enhancement, String serial,
                                  Map<String, String> customOptions, String damageState) {
        public AugmentInstance {
            id = canonicalKnownId(id == null ? "" : id.trim());
            mk = Math.max(1, Math.min(MAX_AUGMENT_MK, mk));
            enhancement = Math.max(0, Math.min(MAX_ENHANCEMENT, enhancement));
            if (serial == null || serial.isBlank()) serial = UUID.randomUUID().toString();
            customOptions = immutableOptions(customOptions);
            damageState = damageState == null ? "" : damageState;
        }

        public AugmentInstance(String id, int mk, int enhancement, String serial) {
            this(id, mk, enhancement, serial, Map.of(), "");
        }

        public static AugmentInstance fresh(String id) {
            return new AugmentInstance(id, 1, 0, UUID.randomUUID().toString(), Map.of(), "");
        }

        public String encode() {
            String options = customOptions.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> encodeComponent(entry.getKey()) + "=" + encodeComponent(entry.getValue()))
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
            return AUGMENT_ENCODING_VERSION + "|" + encodeComponent(id) + "|" + mk + "|" + enhancement + "|"
                    + encodeComponent(serial) + "|" + encodeComponent(damageState) + "|" + options;
        }

        public AugmentInstance withMk(int value) {
            return new AugmentInstance(id, value, enhancement, serial, customOptions, damageState);
        }

        public AugmentInstance withEnhancement(int value) {
            return new AugmentInstance(id, mk, value, serial, customOptions, damageState);
        }

        public AugmentInstance withCustomOptions(Map<String, String> value) {
            return new AugmentInstance(id, mk, enhancement, serial, value, damageState);
        }

        public AugmentInstance withDamageState(String value) {
            return new AugmentInstance(id, mk, enhancement, serial, customOptions, value);
        }

        public static AugmentInstance decode(String encoded) {
            if (encoded == null || encoded.isBlank()) return null;
            if (encoded.startsWith(AUGMENT_ENCODING_VERSION + "|")) return decodeCurrent(encoded);
            return decodeLegacy(encoded);
        }

        private static AugmentInstance decodeCurrent(String encoded) {
            String[] parts = encoded.split("\\|", 7);
            if (parts.length != 7 || !AUGMENT_ENCODING_VERSION.equals(parts[0])) return null;
            try {
                String id = decodeComponent(parts[1]);
                int mk = Integer.parseInt(parts[2]);
                int enhancement = Integer.parseInt(parts[3]);
                String serial = decodeComponent(parts[4]);
                if (serial.isBlank()) return null;
                String damageState = decodeComponent(parts[5]);
                Map<String, String> options = decodeOptions(parts[6]);
                return new AugmentInstance(id, mk, enhancement, serial, options, damageState);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        private static AugmentInstance decodeLegacy(String encoded) {
            String[] parts = encoded.split("\\|", 4);
            if (parts.length == 0 || parts[0].isBlank()) return null;
            int mk = 1;
            int enhancement = 0;
            try {
                if (parts.length > 1) mk = Integer.parseInt(parts[1]);
                if (parts.length > 2) enhancement = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
                return null;
            }
            String serial = parts.length > 3 && !parts[3].isBlank() ? parts[3] : UUID.randomUUID().toString();
            return new AugmentInstance(parts[0], mk, enhancement, serial, Map.of(), "");
        }
    }

    private record PlayerEntry(String uuid, double sanity, double heat, int researchData,
                               List<String> normalFirstKills, List<String> eliteFirstKills,
                               List<String> bossFirstKills, List<String> installedAugments,
                               List<String> vaultAugments, List<String> masteryXp,
                               int adaptationLevel, int adaptationXp, int adaptationPoints,
                               List<String> quarantinedAugments, int schemaVersion) {
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
                Codec.STRING.listOf().optionalFieldOf("quarantined_augments", List.of()).forGetter(PlayerEntry::quarantinedAugments),
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
        boolean rewriteNeeded = false;
        for (PlayerEntry entry : entries) {
            if (entry.schemaVersion() > CURRENT_SCHEMA_VERSION) {
                Titanbreak.LOGGER.warn("TITANBREAK player profile {} uses newer schema {} (supported: {}). Preserving it read-only.",
                        entry.uuid(), entry.schemaVersion(), CURRENT_SCHEMA_VERSION);
            }
            State state = new State(entry.sanity(), entry.heat(), entry.researchData(),
                    entry.normalFirstKills(), entry.eliteFirstKills(), entry.bossFirstKills(),
                    entry.installedAugments(), entry.vaultAugments(), entry.masteryXp(),
                    entry.adaptationLevel(), entry.adaptationXp(), entry.adaptationPoints(),
                    entry.quarantinedAugments(), entry.schemaVersion());
            boolean currentOrOlder = entry.schemaVersion() <= CURRENT_SCHEMA_VERSION;
            rewriteNeeded |= state.migrateKnownSchemas();
            if (currentOrOlder) rewriteNeeded |= state.normalizedDuringLoad;
            players.put(entry.uuid(), state);
        }
        if (rewriteNeeded) setDirty();
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
            List<String> quarantined = new ArrayList<>(state.quarantinedAugments);
            quarantined.sort(String::compareTo);
            return new PlayerEntry(entry.getKey(), state.sanity, state.heat, state.researchData,
                    sorted(state.normalFirstKills), sorted(state.eliteFirstKills), sorted(state.bossFirstKills),
                    installed, vault, mastery, state.adaptationLevel, state.adaptationXp,
                    state.adaptationPoints, quarantined, state.schemaVersion);
        }).toList();
    }

    private static List<String> sorted(Set<String> values) {
        List<String> result = new ArrayList<>(values);
        result.sort(String::compareTo);
        return result;
    }

    private static Map<String, String> buildIdAliasMap() {
        Map<String, String> aliases = new LinkedHashMap<>();
        for (AugmentationCatalog.Definition definition : AugmentationCatalog.DEFINITIONS) {
            if (!definition.itemId().equals(definition.id())) aliases.put(definition.itemId(), definition.id());
        }
        return Map.copyOf(aliases);
    }

    private static String canonicalKnownId(String id) {
        if (id == null || id.isBlank()) return "";
        if (AugmentationCatalog.byId(id) != null) return id;
        String aliased = ID_ALIASES.get(id);
        return aliased == null ? id : aliased;
    }

    private static Map<String, String> immutableOptions(Map<String, String> options) {
        if (options == null || options.isEmpty()) return Map.of();
        Map<String, String> safe = new LinkedHashMap<>();
        options.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> safe.put(entry.getKey(), entry.getValue()));
        return Map.copyOf(safe);
    }

    private static String encodeComponent(String value) {
        String safe = value == null ? "" : value;
        return URL_ENCODER.encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeComponent(String value) {
        return new String(URL_DECODER.decode(value), StandardCharsets.UTF_8);
    }

    private static Map<String, String> decodeOptions(String encoded) {
        if (encoded == null || encoded.isBlank()) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : encoded.split(",")) {
            int split = pair.indexOf('=');
            if (split < 0) throw new IllegalArgumentException("invalid augmentation option encoding");
            String key = decodeComponent(pair.substring(0, split));
            String value = decodeComponent(pair.substring(split + 1));
            result.put(key, value);
        }
        return Map.copyOf(result);
    }

    private static String quarantineInstalled(String slotName, String encoded) {
        return QUARANTINE_VERSION + "|i|" + encodeComponent(slotName) + "|" + encodeComponent(encoded);
    }

    private static String quarantineVault(String encoded) {
        return QUARANTINE_VERSION + "|v|" + encodeComponent(encoded);
    }

    private static String quarantineMastery(String encoded) {
        return QUARANTINE_VERSION + "|m|" + encodeComponent(encoded);
    }

    public static void verifyPersistenceContract() {
        Map<String, String> options = Map.of("calibration", "0.875", "mode", "adaptive|safe");
        AugmentInstance original = new AugmentInstance("ballistic_eye", 4, 7,
                "contract-installed", options, "surface_scored");
        AugmentInstance decoded = AugmentInstance.decode(original.encode());
        requireContract(original.equals(decoded), "current augmentation metadata round-trip failed");

        AugmentInstance legacy = AugmentInstance.decode("ballistic_correction_eye|3|5|legacy-instance");
        requireContract(legacy != null && "ballistic_eye".equals(legacy.id()), "catalog alias migration failed");
        requireContract(legacy.customOptions().isEmpty() && legacy.damageState().isEmpty(),
                "legacy augmentation defaults failed");

        AugmentInstance vaultInstance = new AugmentInstance("thermal_eye", 2, 3,
                "contract-vault", Map.of("lens", "thermal"), "");
        State state = new State(100.0D, 0.0D, 0, List.of(), List.of(), List.of(),
                List.of(AugmentationCatalog.Slot.EYE_2.name() + "=" + original.encode()),
                List.of(vaultInstance.encode()), List.of("ballistic_eye=650"),
                1, 0, 0, List.of(), CURRENT_SCHEMA_VERSION);
        requireContract(original.equals(state.installedInstance(AugmentationCatalog.Slot.EYE_2)),
                "installed slot metadata round-trip failed");
        requireContract(state.vaultView().size() == 1 && vaultInstance.equals(state.vaultView().getFirst()),
                "vault metadata round-trip failed");
        requireContract(state.masteryLevel("ballistic_eye") == 3, "mastery round-trip failed");

        AugmentInstance unknown = new AugmentInstance("future_removed_implant", 2, 3,
                "contract-unknown", Map.of("opaque", "preserve"), "unknown_damage");
        State quarantined = new State(100.0D, 0.0D, 0, List.of(), List.of(), List.of(),
                List.of(AugmentationCatalog.Slot.EYE_1.name() + "=" + unknown.encode()),
                List.of(), List.of(), 1, 0, 0, List.of(), CURRENT_SCHEMA_VERSION);
        requireContract(quarantined.installedInstance(AugmentationCatalog.Slot.EYE_1) == null,
                "unknown augmentation was activated instead of quarantined");
        requireContract(quarantined.quarantinedAugments.size() == 1,
                "unknown augmentation was not preserved in quarantine");
        State quarantineReload = new State(100.0D, 0.0D, 0, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), 1, 0, 0,
                List.copyOf(quarantined.quarantinedAugments), CURRENT_SCHEMA_VERSION);
        requireContract(quarantineReload.quarantinedAugments.equals(quarantined.quarantinedAugments),
                "quarantine round-trip failed");
    }

    private static void requireContract(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("TITANBREAK persistence contract: " + message);
    }

    public static TitanPlayerData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean ensureProfile(ServerPlayer player) {
        String key = player.getUUID().toString();
        if (players.containsKey(key)) return false;
        players.put(key, new State(100.0D, 0.0D, 0, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), 1, 0, 0, List.of(), CURRENT_SCHEMA_VERSION));
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
        String canonicalId = canonicalKnownId(augmentId);
        for (int i = 0; i < state.vault.size(); i++) {
            AugmentInstance instance = state.vault.get(i);
            if (!instance.id().equals(canonicalId)) continue;
            state.vault.remove(i);
            setDirty();
            return instance;
        }
        return null;
    }

    public AugmentInstance firstInstance(ServerPlayer player, String augmentId) {
        return state(player).firstInstance(canonicalKnownId(augmentId));
    }

    public boolean enhance(ServerPlayer player, String augmentId) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion()) return false;
        String canonicalId = canonicalKnownId(augmentId);
        AugmentInstance current = state.firstInstance(canonicalId);
        if (current == null || current.enhancement() >= MAX_ENHANCEMENT) return false;
        state.replaceInstance(current, current.withEnhancement(current.enhancement() + 1));
        setDirty();
        return true;
    }

    public boolean upgradeMk(ServerPlayer player, String augmentId) {
        State state = state(player);
        if (!state.isWritableByCurrentVersion()) return false;
        String canonicalId = canonicalKnownId(augmentId);
        AugmentInstance current = state.firstInstance(canonicalId);
        if (current == null || current.mk() >= MAX_AUGMENT_MK) return false;
        state.replaceInstance(current, current.withMk(current.mk() + 1));
        setDirty();
        return true;
    }

    public int addMasteryXp(ServerPlayer player, String augmentId, int amount) {
        State state = state(player);
        String canonicalId = canonicalKnownId(augmentId);
        if (!state.isWritableByCurrentVersion() || AugmentationCatalog.byId(canonicalId) == null || amount <= 0) return 0;
        int previousLevel = state.masteryLevel(canonicalId);
        int next = Math.max(0, state.masteryXp.getOrDefault(canonicalId, 0) + amount);
        state.masteryXp.put(canonicalId, next);
        setDirty();
        return Math.max(0, state.masteryLevel(canonicalId) - previousLevel);
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
        return state(player).hasInstalled(canonicalKnownId(augmentId));
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
        private final LinkedHashSet<String> quarantinedAugments = new LinkedHashSet<>();
        private int adaptationLevel;
        private int adaptationXp;
        private int adaptationPoints;
        private int schemaVersion;
        private boolean normalizedDuringLoad;

        private State(double sanity, double heat, int researchData,
                      List<String> normalFirstKills, List<String> eliteFirstKills,
                      List<String> bossFirstKills, List<String> installedAugments,
                      List<String> vaultAugments, List<String> masteryXp,
                      int adaptationLevel, int adaptationXp, int adaptationPoints,
                      List<String> quarantinedAugments, int schemaVersion) {
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
            this.quarantinedAugments.addAll(quarantinedAugments);
            for (String entry : installedAugments) loadInstalled(entry);
            for (String entry : vaultAugments) loadVault(entry);
            for (String entry : masteryXp) loadMastery(entry);
            restoreResolvableQuarantine();
        }

        private void loadMastery(String entry) {
            int split = entry.indexOf('=');
            if (split <= 0 || split >= entry.length() - 1) {
                preserveQuarantine(quarantineMastery(entry));
                return;
            }
            String rawId = entry.substring(0, split);
            String id = canonicalKnownId(rawId);
            if (AugmentationCatalog.byId(id) == null) {
                preserveQuarantine(quarantineMastery(entry));
                return;
            }
            try {
                masteryXp.put(id, Math.max(0, Integer.parseInt(entry.substring(split + 1))));
                if (!rawId.equals(id)) normalizedDuringLoad = true;
            } catch (NumberFormatException ignored) {
                preserveQuarantine(quarantineMastery(entry));
            }
        }

        private void loadVault(String encoded) {
            AugmentInstance augment = AugmentInstance.decode(encoded);
            if (augment == null || AugmentationCatalog.byId(augment.id()) == null) {
                preserveQuarantine(quarantineVault(encoded));
                return;
            }
            vault.add(augment);
            if (!encoded.equals(augment.encode())) normalizedDuringLoad = true;
        }

        private void loadInstalled(String entry) {
            int split = entry.indexOf('=');
            if (split <= 0 || split >= entry.length() - 1) {
                preserveQuarantine(quarantineInstalled("", entry));
                return;
            }
            String slotName = entry.substring(0, split);
            String encoded = entry.substring(split + 1);
            AugmentInstance augment = AugmentInstance.decode(encoded);
            if (augment == null && AugmentationCatalog.byId(canonicalKnownId(encoded)) != null) {
                augment = AugmentInstance.fresh(canonicalKnownId(encoded));
                normalizedDuringLoad = true;
            }
            if (augment == null || AugmentationCatalog.byId(augment.id()) == null) {
                preserveQuarantine(quarantineInstalled(slotName, encoded));
                return;
            }
            List<AugmentationCatalog.Slot> slots = resolveSavedSlots(slotName);
            if (slots.isEmpty() || !placementAcceptsAll(augment, slots)) {
                preserveQuarantine(quarantineInstalled(slotName, encoded));
                return;
            }
            for (AugmentationCatalog.Slot slot : slots) installed.putIfAbsent(slot, augment);
            if (!encoded.equals(augment.encode()) || !slotName.equals(slots.getFirst().name())) {
                normalizedDuringLoad = true;
            }
        }

        private boolean placementAcceptsAll(AugmentInstance augment, List<AugmentationCatalog.Slot> slots) {
            AugmentationCatalog.Definition definition = AugmentationCatalog.byId(augment.id());
            if (definition == null) return false;
            for (AugmentationCatalog.Slot slot : slots) if (!definition.canInstallAt(slot)) return false;
            return true;
        }

        private List<AugmentationCatalog.Slot> resolveSavedSlots(String slotName) {
            try {
                return List.of(AugmentationCatalog.Slot.valueOf(slotName));
            } catch (IllegalArgumentException ignored) {}
            return switch (slotName) {
                case "EYE" -> List.of(AugmentationCatalog.Slot.EYE_1);
                case "BRAIN" -> List.of(AugmentationCatalog.Slot.BRAIN_1);
                case "NERVES" -> List.of(AugmentationCatalog.Slot.NERVES_1);
                case "SPINE" -> List.of(AugmentationCatalog.Slot.SPINE_MAIN);
                case "SKELETON" -> List.of(AugmentationCatalog.Slot.SKELETON_1);
                case "SKIN" -> List.of(AugmentationCatalog.Slot.SKIN_1);
                case "LEFT_ARM" -> List.of(AugmentationCatalog.Slot.LEFT_ARM_MAIN);
                case "RIGHT_ARM" -> List.of(AugmentationCatalog.Slot.RIGHT_ARM_MAIN);
                case "LEGS" -> List.of(AugmentationCatalog.Slot.LEFT_LEG_MAIN, AugmentationCatalog.Slot.RIGHT_LEG_MAIN);
                default -> List.of();
            };
        }

        private void preserveQuarantine(String entry) {
            if (entry == null || entry.isBlank()) return;
            if (quarantinedAugments.add(entry)) normalizedDuringLoad = true;
        }

        private void restoreResolvableQuarantine() {
            if (quarantinedAugments.isEmpty()) return;
            List<String> resolved = new ArrayList<>();
            for (String entry : quarantinedAugments) {
                if (restoreQuarantineEntry(entry)) resolved.add(entry);
            }
            if (!resolved.isEmpty()) {
                quarantinedAugments.removeAll(resolved);
                normalizedDuringLoad = true;
            }
        }

        private boolean restoreQuarantineEntry(String entry) {
            if (entry == null || !entry.startsWith(QUARANTINE_VERSION + "|")) return false;
            String[] parts = entry.split("\\|", 4);
            if (parts.length < 3) return false;
            try {
                return switch (parts[1]) {
                    case "v" -> restoreQuarantinedVault(parts);
                    case "i" -> restoreQuarantinedInstalled(parts);
                    case "m" -> restoreQuarantinedMastery(parts);
                    default -> false;
                };
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }

        private boolean restoreQuarantinedVault(String[] parts) {
            if (parts.length != 3) return false;
            String encoded = decodeComponent(parts[2]);
            AugmentInstance augment = AugmentInstance.decode(encoded);
            if (augment == null || AugmentationCatalog.byId(augment.id()) == null) return false;
            for (AugmentInstance existing : vault) if (existing.serial().equals(augment.serial())) return true;
            vault.add(augment);
            return true;
        }

        private boolean restoreQuarantinedInstalled(String[] parts) {
            if (parts.length != 4) return false;
            String slotName = decodeComponent(parts[2]);
            String encoded = decodeComponent(parts[3]);
            AugmentInstance augment = AugmentInstance.decode(encoded);
            if (augment == null || AugmentationCatalog.byId(augment.id()) == null) return false;
            List<AugmentationCatalog.Slot> slots = resolveSavedSlots(slotName);
            if (slots.isEmpty() || !placementAcceptsAll(augment, slots)) return false;
            for (AugmentationCatalog.Slot slot : slots) {
                AugmentInstance existing = installed.get(slot);
                if (existing != null && !existing.serial().equals(augment.serial())) return false;
            }
            for (AugmentationCatalog.Slot slot : slots) installed.put(slot, augment);
            return true;
        }

        private boolean restoreQuarantinedMastery(String[] parts) {
            if (parts.length != 3) return false;
            String encoded = decodeComponent(parts[2]);
            int split = encoded.indexOf('=');
            if (split <= 0 || split >= encoded.length() - 1) return false;
            String id = canonicalKnownId(encoded.substring(0, split));
            if (AugmentationCatalog.byId(id) == null) return false;
            try {
                masteryXp.put(id, Math.max(0, Integer.parseInt(encoded.substring(split + 1))));
                return true;
            } catch (NumberFormatException ignored) {
                return false;
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

        private boolean migrateKnownSchemas() {
            if (schemaVersion > CURRENT_SCHEMA_VERSION) return false;
            boolean migrated = false;
            if (schemaVersion < 1) {
                schemaVersion = 1;
                migrated = true;
            }
            while (schemaVersion < CURRENT_SCHEMA_VERSION) {
                schemaVersion = switch (schemaVersion) {
                    case 1 -> 2;
                    case 2 -> 3;
                    case 3 -> 4;
                    case 4 -> 5;
                    case 5 -> 6;
                    default -> throw new IllegalStateException("Unsupported TITANBREAK save schema " + schemaVersion);
                };
                migrated = true;
            }
            return migrated;
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
            String canonicalId = canonicalKnownId(augmentId);
            for (AugmentInstance instance : installed.values()) if (instance.id().equals(canonicalId)) return instance;
            return null;
        }

        public AugmentInstance firstInstance(String augmentId) {
            String canonicalId = canonicalKnownId(augmentId);
            AugmentInstance installed = firstInstalledInstance(canonicalId);
            if (installed != null) return installed;
            for (AugmentInstance instance : vault) if (instance.id().equals(canonicalId)) return instance;
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
        public List<String> quarantinedView() { return List.copyOf(quarantinedAugments); }
        public int masteryXp(String augmentId) { return masteryXp.getOrDefault(canonicalKnownId(augmentId), 0); }
        public int masteryLevel(String augmentId) { return masteryLevelForXp(masteryXp(augmentId)); }
        public double powerLoadMultiplier(String augmentId) { return masteryLevel(augmentId) >= 1 ? 0.97D : 1.0D; }
        public double heatLoadMultiplier(String augmentId) { return masteryLevel(augmentId) >= 1 ? 0.97D : 1.0D; }
        public double neuralLoadMultiplier(String augmentId) { return masteryLevel(augmentId) >= 3 ? 0.95D : 1.0D; }
        public int schemaVersion() { return schemaVersion; }
    }
}
