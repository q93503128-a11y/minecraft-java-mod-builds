package kr.moonseungjun.anomalousclasses.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World index for the original controlled entities. The entity's own persistent tag remains the link source. */
public final class MentalDominatorSavedData extends SavedData {
    private static final String DATA_NAME = "anomalousclasses_mental_dominator";
    private static final Factory<MentalDominatorSavedData> FACTORY = new Factory<>(
            MentalDominatorSavedData::new, MentalDominatorSavedData::load, DataFixTypes.LEVEL);

    public enum CommandMode { FOLLOW, STAY }
    public enum LinkState { ACTIVE, RELEASE_PENDING }

    public static final class PlayerState {
        private double psyche;
        private double lastMaximum;
        private boolean initialized;
        private boolean allowPlayerDamageByDefault;
        private int revision;
        private final Set<String> codex = new HashSet<>();

        public double psyche() { return psyche; }
        public void setPsyche(double value) { psyche = Math.max(0.0, Double.isFinite(value) ? value : 0.0); }
        public double lastMaximum() { return lastMaximum; }
        public void setLastMaximum(double value) { lastMaximum = Math.max(0.0, Double.isFinite(value) ? value : 0.0); }
        public boolean initialized() { return initialized; }
        public void setInitialized(boolean value) { initialized = value; }
        public boolean allowPlayerDamageByDefault() { return allowPlayerDamageByDefault; }
        public void setAllowPlayerDamageByDefault(boolean value) { allowPlayerDamageByDefault = value; }
        public int revision() { return revision; }
        public void bumpRevision() { revision = revision == Integer.MAX_VALUE ? 0 : revision + 1; }
        public Set<String> codex() { return Set.copyOf(codex); }
        public boolean recordSpecies(String species) { return species != null && !species.isBlank() && codex.add(species); }
    }

    public static final class ControlledRecord {
        private final UUID entityId;
        private final UUID ownerId;
        private final String entityType;
        private String displayName;
        private String dimension;
        private double x, y, z;
        private float lastHealth, lastMaxHealth;
        private double threat;
        private CommandMode command;
        private boolean allowPlayerDamage;
        private long capturedAt;
        private LinkState linkState;

        public ControlledRecord(UUID entityId, UUID ownerId, String entityType, String displayName,
                                String dimension, double x, double y, double z, float health, float maxHealth,
                                double threat, CommandMode command, boolean allowPlayerDamage,
                                long capturedAt, LinkState linkState) {
            this.entityId = entityId;
            this.ownerId = ownerId;
            this.entityType = safeText(entityType, 128);
            this.displayName = safeText(displayName, 128);
            this.dimension = safeText(dimension, 128);
            updateLocation(dimension, x, y, z, health, maxHealth);
            this.threat = finiteNonNegative(threat);
            this.command = command == null ? CommandMode.FOLLOW : command;
            this.allowPlayerDamage = allowPlayerDamage;
            this.capturedAt = Math.max(0L, capturedAt);
            this.linkState = linkState == null ? LinkState.ACTIVE : linkState;
        }

        public UUID entityId() { return entityId; }
        public UUID ownerId() { return ownerId; }
        public String entityType() { return entityType; }
        public String displayName() { return displayName; }
        public String dimension() { return dimension; }
        public double x() { return x; }
        public double y() { return y; }
        public double z() { return z; }
        public float lastHealth() { return lastHealth; }
        public float lastMaxHealth() { return lastMaxHealth; }
        public double threat() { return threat; }
        public CommandMode command() { return command; }
        public boolean allowPlayerDamage() { return allowPlayerDamage; }
        public long capturedAt() { return capturedAt; }
        public LinkState linkState() { return linkState; }
        public void setCommand(CommandMode value) { command = value == null ? CommandMode.FOLLOW : value; }
        public void setAllowPlayerDamage(boolean value) { allowPlayerDamage = value; }
        public void setLinkState(LinkState value) { linkState = value; }
        public void updateLocation(String dimensionKey, double px, double py, double pz, float health, float maxHealth) {
            if (dimensionKey != null && ResourceLocation.tryParse(dimensionKey) != null) dimension = dimensionKey;
            x = Double.isFinite(px) ? px : 0.0;
            y = Double.isFinite(py) ? py : 0.0;
            z = Double.isFinite(pz) ? pz : 0.0;
            lastMaxHealth = Float.isFinite(maxHealth) ? Math.max(0.0F, maxHealth) : 0.0F;
            lastHealth = Float.isFinite(health) ? Math.max(0.0F, Math.min(lastMaxHealth, health)) : 0.0F;
        }
        public void setThreat(double value) { threat = finiteNonNegative(value); }
    }

    private final Map<UUID, PlayerState> players = new HashMap<>();
    private final Map<UUID, ControlledRecord> controlled = new HashMap<>();
    private final Set<UUID> releasedEntities = new HashSet<>();

    public static MentalDominatorSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public PlayerState player(UUID playerId) { return players.computeIfAbsent(playerId, ignored -> new PlayerState()); }
    public ControlledRecord controlled(UUID entityId) { return controlled.get(entityId); }
    public boolean wasReleased(UUID entityId) { return releasedEntities.contains(entityId); }
    public void markReleased(UUID entityId) {
        if (entityId != null && releasedEntities.add(entityId)) setDirty();
    }
    public void clearReleased(UUID entityId) {
        if (entityId != null && releasedEntities.remove(entityId)) setDirty();
    }
    public List<ControlledRecord> controlledBy(UUID ownerId) {
        return controlled.values().stream().filter(record -> record.ownerId().equals(ownerId))
                .sorted(java.util.Comparator.comparingLong(ControlledRecord::capturedAt)).toList();
    }
    public List<ControlledRecord> allControlled() { return List.copyOf(controlled.values()); }
    public int activeCount(UUID ownerId) {
        return (int) controlled.values().stream().filter(record -> record.ownerId().equals(ownerId)
                && record.linkState() == LinkState.ACTIVE).count();
    }
    public void put(ControlledRecord record) { controlled.put(record.entityId(), record); setDirty(); }
    public void remove(UUID entityId) { if (controlled.remove(entityId) != null) setDirty(); }
    public void changed() { setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag playerList = new ListTag();
        for (Map.Entry<UUID, PlayerState> entry : players.entrySet()) {
            PlayerState state = entry.getValue();
            CompoundTag player = new CompoundTag();
            player.putUUID("id", entry.getKey());
            player.putDouble("psyche", state.psyche);
            player.putDouble("lastMaximum", state.lastMaximum);
            player.putBoolean("initialized", state.initialized);
            player.putBoolean("allowPlayerDamageByDefault", state.allowPlayerDamageByDefault);
            player.putInt("revision", state.revision);
            ListTag species = new ListTag();
            state.codex.stream().sorted().forEach(value -> species.add(net.minecraft.nbt.StringTag.valueOf(value)));
            player.put("codex", species);
            playerList.add(player);
        }
        tag.put("players", playerList);

        ListTag controlledList = new ListTag();
        for (ControlledRecord record : controlled.values()) {
            CompoundTag mob = new CompoundTag();
            mob.putUUID("entity", record.entityId);
            mob.putUUID("owner", record.ownerId);
            mob.putString("type", record.entityType);
            mob.putString("name", record.displayName);
            mob.putString("dimension", record.dimension);
            mob.putDouble("x", record.x); mob.putDouble("y", record.y); mob.putDouble("z", record.z);
            mob.putFloat("health", record.lastHealth); mob.putFloat("maxHealth", record.lastMaxHealth);
            mob.putDouble("threat", record.threat);
            mob.putInt("command", record.command.ordinal());
            mob.putBoolean("allowPlayerDamage", record.allowPlayerDamage);
            mob.putLong("capturedAt", record.capturedAt);
            mob.putInt("linkState", record.linkState.ordinal());
            controlledList.add(mob);
        }
        tag.put("controlled", controlledList);
        ListTag releasedList = new ListTag();
        for (UUID entityId : releasedEntities) {
            CompoundTag released = new CompoundTag();
            released.putUUID("entity", entityId);
            releasedList.add(released);
        }
        tag.put("released", releasedList);
        tag.putInt("schemaVersion", 1);
        return tag;
    }

    private static MentalDominatorSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MentalDominatorSavedData data = new MentalDominatorSavedData();
        ListTag playerList = tag.getList("players", Tag.TAG_COMPOUND);
        for (int index = 0; index < playerList.size(); index++) {
            CompoundTag encoded = playerList.getCompound(index);
            if (!encoded.hasUUID("id")) continue;
            PlayerState state = new PlayerState();
            state.setPsyche(encoded.getDouble("psyche"));
            state.setLastMaximum(encoded.getDouble("lastMaximum"));
            state.setInitialized(encoded.getBoolean("initialized"));
            state.setAllowPlayerDamageByDefault(encoded.getBoolean("allowPlayerDamageByDefault"));
            state.revision = Math.max(0, encoded.getInt("revision"));
            ListTag species = encoded.getList("codex", Tag.TAG_STRING);
            for (int entry = 0; entry < species.size(); entry++) {
                String value = species.getString(entry);
                if (value.length() <= 128 && ResourceLocation.tryParse(value) != null) state.codex.add(value);
            }
            data.players.put(encoded.getUUID("id"), state);
        }
        ListTag mobList = tag.getList("controlled", Tag.TAG_COMPOUND);
        for (int index = 0; index < mobList.size(); index++) {
            CompoundTag encoded = mobList.getCompound(index);
            if (!encoded.hasUUID("entity") || !encoded.hasUUID("owner")) continue;
            String type = encoded.getString("type");
            String dimension = encoded.getString("dimension");
            if (ResourceLocation.tryParse(type) == null || ResourceLocation.tryParse(dimension) == null) continue;
            CommandMode[] commands = CommandMode.values();
            LinkState[] states = LinkState.values();
            int commandIndex = encoded.getInt("command");
            int stateIndex = encoded.getInt("linkState");
            ControlledRecord record = new ControlledRecord(encoded.getUUID("entity"), encoded.getUUID("owner"),
                    type, encoded.getString("name"), dimension,
                    encoded.getDouble("x"), encoded.getDouble("y"), encoded.getDouble("z"),
                    encoded.getFloat("health"), encoded.getFloat("maxHealth"), encoded.getDouble("threat"),
                    commandIndex >= 0 && commandIndex < commands.length ? commands[commandIndex] : CommandMode.FOLLOW,
                    encoded.getBoolean("allowPlayerDamage"), encoded.getLong("capturedAt"),
                    stateIndex >= 0 && stateIndex < states.length ? states[stateIndex] : LinkState.ACTIVE);
            data.controlled.put(record.entityId(), record);
        }
        ListTag releasedList = tag.getList("released", Tag.TAG_COMPOUND);
        for (int index = 0; index < releasedList.size(); index++) {
            CompoundTag released = releasedList.getCompound(index);
            if (released.hasUUID("entity")) data.releasedEntities.add(released.getUUID("entity"));
        }
        return data;
    }

    private static String safeText(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
