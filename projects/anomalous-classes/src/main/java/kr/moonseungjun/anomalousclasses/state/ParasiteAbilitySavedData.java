package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/** Server-owned, persistent cooldowns and single-host effect identity for Z/X/C. */
public final class ParasiteAbilitySavedData extends SavedData {
    private static final String KEY = "anomalousclasses_parasite_abilities";
    private static final Factory<ParasiteAbilitySavedData> FACTORY = new Factory<>(
            ParasiteAbilitySavedData::new, ParasiteAbilitySavedData::load, DataFixTypes.LEVEL);
    public static final class State {
        private final long[] readyAt = new long[3];
        private long anchorUntil;
        private UUID healHost;
        private long healUntil;
        private float healRemaining;
        private UUID awakeHost;
        private long awakeUntil;

        public long readyAt(int slot) { return slot >= 0 && slot < 3 ? readyAt[slot] : Long.MAX_VALUE; }
        public long anchorUntil() { return anchorUntil; }
        public UUID healHost() { return healHost; }
        public float healRemaining() { return healRemaining; }
    }
    private final Map<UUID, State> players = new HashMap<>();

    public static ParasiteAbilitySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, KEY);
    }
    public State player(UUID owner) { return players.computeIfAbsent(owner, unused -> new State()); }

    public boolean ready(UUID owner, int slot, long tick) {
        return slot >= 0 && slot < 3 && player(owner).readyAt[slot] <= tick;
    }
    public boolean anchor(UUID owner, long tick) {
        if (!ready(owner, 0, tick)) return false;
        State s = player(owner);
        s.readyAt[0] = tick + 800;
        s.anchorUntil = tick + 160;
        setDirty();
        return true;
    }
    public boolean consumeAnchor(UUID owner, long tick) {
        State s = player(owner);
        if (s.anchorUntil <= tick) return false;
        s.anchorUntil = 0;
        setDirty();
        return true;
    }
    public boolean heal(UUID owner, UUID host, float amount, long tick) {
        if (host == null || !Float.isFinite(amount) || amount <= 0 || !ready(owner, 1, tick)) return false;
        State s = player(owner);
        s.readyAt[1] = tick + 900;
        s.healHost = host;
        s.healUntil = tick + 100;
        s.healRemaining = amount;
        setDirty();
        return true;
    }
    /** Total heal is decremented only when that original host actually receives health. */
    public float healTick(UUID owner, UUID host, long tick, float deficit) {
        State s = player(owner);
        if (!host.equals(s.healHost) || tick > s.healUntil || !Float.isFinite(deficit) || deficit <= 0) {
            stopHealing(owner, host);
            return 0;
        }
        long ticksLeft = Math.max(1L, s.healUntil - tick + 1);
        float amount = Math.min(Math.min(s.healRemaining, deficit),
                s.healRemaining / ticksLeft);
        if (amount <= 0) { stopHealing(owner, host); return 0; }
        s.healRemaining -= amount;
        if (s.healRemaining <= 0.00001F) {
            s.healRemaining = 0;
            s.healHost = null;
            s.healUntil = 0;
        }
        setDirty();
        return amount;
    }
    public void stopHealing(UUID owner, UUID host) {
        State s = player(owner);
        if (host == null || !host.equals(s.healHost)) return;
        s.healHost = null;
        s.healUntil = 0;
        s.healRemaining = 0;
        setDirty();
    }
    public boolean awaken(UUID owner, UUID host, long tick) {
        if (host == null || !ready(owner, 2, tick)) return false;
        State s = player(owner);
        s.readyAt[2] = tick + 700;
        s.awakeHost = host;
        s.awakeUntil = tick + 120;
        setDirty();
        return true;
    }
    public boolean isAwakened(UUID owner, UUID host, long tick) {
        if (host == null) return false;
        State s = player(owner);
        return host.equals(s.awakeHost) && tick < s.awakeUntil;
    }
    /** Leaving a host always cancels its benefits, never refunds the cast cooldown. */
    public void endHost(UUID owner, UUID host) {
        if (host == null) return;
        State s = player(owner);
        stopHealing(owner, host);
        if (host.equals(s.awakeHost)) {
            s.awakeHost = null;
            s.awakeUntil = 0;
            setDirty();
        }
    }

    @Override public CompoundTag save(CompoundTag output, HolderLookup.Provider registries) {
        ListTag saved = new ListTag();
        for (var entry : players.entrySet()) {
            State s = entry.getValue();
            CompoundTag node = new CompoundTag();
            node.putUUID("owner", entry.getKey());
            for (int i = 0; i < 3; i++) node.putLong("ready" + i, s.readyAt[i]);
            node.putLong("anchorUntil", s.anchorUntil);
            if (s.healHost != null) node.putUUID("healHost", s.healHost);
            node.putLong("healUntil", s.healUntil);
            node.putFloat("healRemaining", s.healRemaining);
            if (s.awakeHost != null) node.putUUID("awakeHost", s.awakeHost);
            node.putLong("awakeUntil", s.awakeUntil);
            saved.add(node);
        }
        output.put("players", saved);
        output.putInt("schemaVersion", 1);
        return output;
    }
    private static ParasiteAbilitySavedData load(CompoundTag input, HolderLookup.Provider registries) {
        ParasiteAbilitySavedData out = new ParasiteAbilitySavedData();
        for (Tag t : input.getList("players", Tag.TAG_COMPOUND)) {
            if (!(t instanceof CompoundTag node) || !node.hasUUID("owner")) continue;
            State s = new State();
            for (int i = 0; i < 3; i++) s.readyAt[i] = Math.max(0, node.getLong("ready" + i));
            s.anchorUntil = Math.max(0, node.getLong("anchorUntil"));
            if (node.hasUUID("healHost") && node.getFloat("healRemaining") > 0
                    && Float.isFinite(node.getFloat("healRemaining"))) {
                s.healHost = node.getUUID("healHost");
                s.healUntil = Math.max(0, node.getLong("healUntil"));
                s.healRemaining = Math.max(0, node.getFloat("healRemaining"));
            }
            if (node.hasUUID("awakeHost")) {
                s.awakeHost = node.getUUID("awakeHost");
                s.awakeUntil = Math.max(0, node.getLong("awakeUntil"));
            }
            out.players.putIfAbsent(node.getUUID("owner"), s);
        }
        return out;
    }
}
