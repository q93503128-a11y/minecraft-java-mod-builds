package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/** Server-wide owner/host exclusivity and permanent non-spendable biological data. */
public final class ParasiteSavedData extends SavedData {
    private static final String KEY = "anomalousclasses_parasite";
    private static final Factory<ParasiteSavedData> FACTORY = new Factory<>(
            ParasiteSavedData::new, ParasiteSavedData::load, DataFixTypes.LEVEL);
    public static final class State {
        private long bd;
        private final Map<String, Integer> mastery = new HashMap<>();
        private final Map<String, Integer> masteryRemainder = new HashMap<>();
        private final Set<String> biomes = new HashSet<>();
        private UUID host;
        private String dimension = "";
        private long deathLockUntil;
        private int charges;
        private long validActivityTicks;
        private long chargeActivityTicks;
        private int revision;

        public long bd() { return bd; }
        public int mastery(String species) { return mastery.getOrDefault(species, 0); }
        public UUID host() { return host; }
        public String dimension() { return dimension; }
        public long deathLockUntil() { return deathLockUntil; }
        public int charges() { return charges; }
        public int revision() { return revision; }
    }

    private final Map<UUID, State> players = new HashMap<>();
    private final Map<UUID, UUID> claimedHosts = new HashMap<>();

    public static ParasiteSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, KEY);
    }
    public State player(UUID owner) { return players.computeIfAbsent(owner, unused -> new State()); }
    public UUID ownerOf(UUID host) { return claimedHosts.get(host); }

    /** Atomic reservation of one existing entity, retaining the old claim until the new claim is valid. */
    public boolean claim(UUID owner, UUID target, String dimension) {
        State state = player(owner);
        if (owner == null || target == null || owner.equals(target) || dimension == null
                || ResourceLocation.tryParse(dimension) == null) return false;
        UUID occupant = claimedHosts.get(target);
        if (occupant != null && !occupant.equals(owner)) return false;
        UUID prior = state.host;
        if (prior != null && !prior.equals(target)) claimedHosts.remove(prior, owner);
        claimedHosts.put(target, owner);
        state.host = target;
        state.dimension = dimension;
        state.revision++;
        setDirty();
        return true;
    }
    public boolean release(UUID owner, UUID host) {
        State state = player(owner);
        if (host == null || !host.equals(state.host)) return false;
        claimedHosts.remove(host, owner);
        state.host = null;
        state.dimension = "";
        state.revision++;
        setDirty();
        return true;
    }
    public boolean hostDied(UUID owner, UUID host, long gameTick, int lockTicks) {
        if (!release(owner, host)) return false;
        State state = player(owner);
        state.deathLockUntil = Math.max(state.deathLockUntil, gameTick + Math.max(0, lockTicks));
        setDirty();
        return true;
    }
    /** Charges are consumed ONLY after a new host has been successfully claimed. */
    public boolean consumeEmergency(UUID owner, long tick) {
        State state = player(owner);
        if (tick >= state.deathLockUntil) return true;
        if (state.charges <= 0) return false;
        state.charges--;
        setDirty();
        return true;
    }
    public boolean mayAttempt(UUID owner, long tick) {
        State state = player(owner);
        return tick >= state.deathLockUntil || state.charges > 0;
    }
    public boolean firstBiome(UUID owner, String biome) {
        if (biome == null || ResourceLocation.tryParse(biome) == null) return false;
        if (!player(owner).biomes.add(biome)) return false;
        setDirty();
        return true;
    }
    public void reward(UUID owner, String species, int amount, PlayerClassProfile profile) {
        if (amount <= 0 || species == null || ResourceLocation.tryParse(species) == null) return;
        State state = player(owner);
        int bonus = ParasiteRules.rank(profile, "biological_analysis");
        long earned = Math.max(1, Math.round(amount * (1.0 + bonus * .004)));
        state.bd = Math.min(1_000_000_000_000L, state.bd + earned);
        int existing = state.mastery(species);
        int remainder = state.masteryRemainder.getOrDefault(species, 0);
        double mult = 1 + .005 * ParasiteRules.rank(profile, "species_adaptation");
        int gained = (int) Math.min(Integer.MAX_VALUE, Math.round(earned * mult));
        int total = remainder + Math.min(10000, gained);
        state.mastery.put(species, Math.min(100, existing + total / 10));
        state.masteryRemainder.put(species, existing + total / 10 >= 100 ? 0 : total % 10);
        state.revision++;
        setDirty();
    }
    public boolean recordActiveTicks(UUID owner, int ticks, PlayerClassProfile profile) {
        if (ticks <= 0) return false;
        State state = player(owner);
        state.validActivityTicks += ticks;
        int limit = ParasiteRules.maxEmergencyCharges(profile);
        if (state.charges > limit) state.charges = limit;
        // A full emergency reserve does not bank infinite charge time for future deaths.
        if (limit <= 0 || state.charges >= limit) state.chargeActivityTicks = 0;
        else {
            state.chargeActivityTicks = Math.min(4800, state.chargeActivityTicks + ticks);
            if (state.chargeActivityTicks >= 4800) {
                state.charges++;
                state.chargeActivityTicks = 0;
            }
        }
        if (state.validActivityTicks < 4800) { setDirty(); return false; }
        state.validActivityTicks -= 4800;
        setDirty();
        return true;
    }
    public void normalizeCharges(UUID owner, PlayerClassProfile profile) {
        State state = player(owner);
        int maximum = ParasiteRules.maxEmergencyCharges(profile);
        if (state.charges > maximum) { state.charges = maximum; setDirty(); }
    }

    @Override
    public CompoundTag save(CompoundTag out, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (var entry : players.entrySet()) {
            State state = entry.getValue();
            CompoundTag tag = new CompoundTag();
            tag.putUUID("owner", entry.getKey());
            tag.putLong("bd", state.bd);
            if (state.host != null) tag.putUUID("host", state.host);
            tag.putString("dimension", state.dimension);
            tag.putLong("deathLockUntil", state.deathLockUntil);
            tag.putInt("charges", state.charges);
            tag.putLong("activityTicks", state.validActivityTicks);
            tag.putLong("chargeTicks", state.chargeActivityTicks);
            tag.putInt("revision", state.revision);
            CompoundTag mastery = new CompoundTag(), remainder = new CompoundTag();
            state.mastery.forEach(mastery::putInt);
            state.masteryRemainder.forEach(remainder::putInt);
            tag.put("mastery", mastery);
            tag.put("remainder", remainder);
            ListTag biomes = new ListTag();
            state.biomes.stream().sorted().forEach(b -> biomes.add(net.minecraft.nbt.StringTag.valueOf(b)));
            tag.put("biomes", biomes);
            list.add(tag);
        }
        out.put("players", list);
        out.putInt("schemaVersion", 1);
        return out;
    }
    private static ParasiteSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ParasiteSavedData result = new ParasiteSavedData();
        for (Tag element : tag.getList("players", Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag node) || !node.hasUUID("owner")) continue;
            UUID id = node.getUUID("owner");
            State state = new State();
            state.bd = Math.max(0, Math.min(1_000_000_000_000L, node.getLong("bd")));
            state.deathLockUntil = Math.max(0, node.getLong("deathLockUntil"));
            state.charges = Math.max(0, Math.min(3, node.getInt("charges")));
            state.validActivityTicks = Math.max(0, node.getLong("activityTicks") % 4800);
            state.chargeActivityTicks = Math.max(0, node.getLong("chargeTicks") % 4800);
            state.revision = Math.max(0, node.getInt("revision"));
            String dimension = node.getString("dimension");
            if (node.hasUUID("host") && ResourceLocation.tryParse(dimension) != null) {
                state.host = node.getUUID("host");
                state.dimension = dimension;
                if (result.claimedHosts.putIfAbsent(state.host, id) != null) {
                    state.host = null;
                    state.dimension = "";
                }
            }
            CompoundTag m = node.getCompound("mastery"), rem = node.getCompound("remainder");
            for (String species : m.getAllKeys()) {
                if (ResourceLocation.tryParse(species) == null) continue;
                state.mastery.put(species, Math.max(0, Math.min(100, m.getInt(species))));
                state.masteryRemainder.put(species, Math.max(0, Math.min(9, rem.getInt(species))));
            }
            for (Tag val : node.getList("biomes", Tag.TAG_STRING)) {
                String biome = val.getAsString();
                if (ResourceLocation.tryParse(biome) != null && state.biomes.size() < 4096) state.biomes.add(biome);
            }
            result.players.putIfAbsent(id, state);
        }
        return result;
    }
}
