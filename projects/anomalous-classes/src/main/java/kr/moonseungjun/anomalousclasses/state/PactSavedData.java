package kr.moonseungjun.anomalousclasses.state;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import net.minecraft.nbt.StringTag;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/** All permanent contracts, relationships, and actual ability cooldowns are server-owned. */
public final class PactSavedData extends SavedData {
    private static final String KEY = "anomalousclasses_pacts";
    private static final Factory<PactSavedData> FACTORY =
            new Factory<>(PactSavedData::new, PactSavedData::load, DataFixTypes.LEVEL);
    public static final class Bond {
        private final String patronId;
        private int relation, deepening, debt;
        Bond(String patronId, int relation, int deepening, int debt) {
            this.patronId = patronId;
            this.relation = Math.clamp(relation, 0, 100);
            this.deepening = Math.max(0, deepening);
            this.debt = Math.max(0, Math.min(24, debt));
        }
        public String id() { return patronId; }
        public int relation() { return relation; }
        public int deepening() { return deepening; }
        public int debt() { return debt; }
    }
    public static final class Player {
        private final LinkedHashMap<String, Bond> bonds = new LinkedHashMap<>();
        private final Map<String, Long> cooldowns = new LinkedHashMap<>();
        private final Set<String> completedTrials = new LinkedHashSet<>();
        private final java.util.ArrayList<String> recordedProfiles = new java.util.ArrayList<>();
        private int revision;
        public List<Bond> bonds() { return List.copyOf(bonds.values()); }
        public Bond bond(String id) { return bonds.get(id); }
        public boolean has(String id) { return bonds.containsKey(id); }
        public int count() { return bonds.size(); }
        public int revision() { return revision; }
        public long readyAt(String spell) { return cooldowns.getOrDefault(spell, 0L); }
        public boolean hasProof(String patron) { return completedTrials.contains(patron); }
        public Set<String> completedTrials() { return Set.copyOf(completedTrials); }
        public List<String> observations() { return List.copyOf(recordedProfiles); }
    }
    private final Map<UUID, Player> players = new LinkedHashMap<>();
    public static PactSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, KEY);
    }
    public Player player(UUID id) { return players.computeIfAbsent(id, k -> new Player()); }
    public boolean sign(UUID owner, String patron) {
        Player p = player(owner);
        if (PactCatalog.get(patron) == null || p.bonds.containsKey(patron)) return false;
        p.bonds.put(patron, new Bond(patron, 50, 0, 0));
        p.revision++;
        setDirty();
        return true;
    }
    /** Earned once per player. Proof is preserved independently of signed bonds. */
    public boolean completeTrial(UUID owner, String patron) {
        PactCatalog.Patron definition = PactCatalog.get(patron);
        if (definition == null || (definition.tier() != 3 && definition.tier() != 4)) return false;
        Player p = player(owner);
        if (!p.completedTrials.add(patron)) return false;
        p.revision++;
        setDirty();
        return true;
    }
    /** Store at most two supported attack profiles; never run or deserialize mob code. */
    public void observe(UUID owner, String type) {
        if (!Set.of("melee","projectile","fire","impact").contains(type)) return;
        Player p=player(owner);
        p.recordedProfiles.remove(type);
        p.recordedProfiles.add(type);
        while(p.recordedProfiles.size()>2)p.recordedProfiles.remove(0);
        p.revision++;
        setDirty();
    }
    public boolean breakBond(UUID owner, String patron) {
        Player p = player(owner);
        if (p.bonds.remove(patron) == null) return false;
        p.revision++;
        setDirty();
        return true;
    }
    public void relation(UUID owner, String patron, int delta) {
        Player p = player(owner);
        Bond b = p.bonds.get(patron);
        if (b == null) return;
        int next = Math.clamp(b.relation + delta, 0, 100);
        if (next != b.relation) { b.relation = next; p.revision++; setDirty(); }
    }
    public void debt(UUID owner, String patron, int amount) {
        Player p = player(owner);
        Bond b = p.bonds.get(patron);
        if (b == null) return;
        int next = Math.clamp(amount, 0, 24);
        if (next != b.debt) { b.debt = next; p.revision++; setDirty(); }
    }
    public void cooldown(UUID owner, String ability, long ready) {
        Player p = player(owner);
        p.cooldowns.put(ability, ready);
        setDirty();
    }
    @Override
    public CompoundTag save(CompoundTag output, HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        players.forEach((id, p) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("player", id);
            row.putInt("revision", p.revision);
            ListTag entries = new ListTag();
            for (Bond b : p.bonds.values()) {
                CompoundTag entry = new CompoundTag();
                entry.putString("id", b.patronId);
                entry.putInt("relation", b.relation);
                entry.putInt("deepening", b.deepening);
                entry.putInt("debt", b.debt);
                entries.add(entry);
            }
            row.put("bonds", entries);
            ListTag completed = new ListTag();
            p.completedTrials.forEach(patronId -> completed.add(StringTag.valueOf(patronId)));
            row.put("completedTrials", completed);
            ListTag profiles = new ListTag();
            p.recordedProfiles.forEach(name->profiles.add(StringTag.valueOf(name)));
            row.put("observedAttacks", profiles);
            ListTag timers = new ListTag();
            p.cooldowns.forEach((key, time) -> {
                CompoundTag timer = new CompoundTag();
                timer.putString("spell", key);
                timer.putLong("ready", time);
                timers.add(timer);
            });
            row.put("cooldowns", timers);
            list.add(row);
        });
        output.put("players", list);
        return output;
    }
    private static PactSavedData load(CompoundTag input, HolderLookup.Provider lookup) {
        PactSavedData data = new PactSavedData();
        ListTag list = input.getList("players", Tag.TAG_COMPOUND);
        for (int i=0; i<list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            if (!row.hasUUID("player")) continue;
            Player p = new Player();
            p.revision = Math.max(0,row.getInt("revision"));
            ListTag bonds = row.getList("bonds", Tag.TAG_COMPOUND);
            for (int j=0; j<bonds.size(); j++) {
                CompoundTag item = bonds.getCompound(j);
                String id = item.getString("id");
                if (PactCatalog.get(id) == null || p.bonds.containsKey(id)) continue;
                p.bonds.put(id, new Bond(id, item.getInt("relation"), item.getInt("deepening"),
                        item.getInt("debt")));
            }
            ListTag completed = row.getList("completedTrials", Tag.TAG_STRING);
            for (int j=0; j<completed.size(); j++) {
                String patron = completed.getString(j);
                PactCatalog.Patron definition = PactCatalog.get(patron);
                if (definition != null && (definition.tier() == 3 || definition.tier() == 4))
                    p.completedTrials.add(patron);
            }
            ListTag profiles = row.getList("observedAttacks", Tag.TAG_STRING);
            for (int j=0; j<profiles.size(); j++) {
                String type = profiles.getString(j);
                if (Set.of("melee","projectile","fire","impact").contains(type)) {
                    p.recordedProfiles.remove(type);
                    p.recordedProfiles.add(type);
                    while(p.recordedProfiles.size()>2) p.recordedProfiles.remove(0);
                }
            }
            ListTag timers = row.getList("cooldowns", Tag.TAG_COMPOUND);
            for (int j=0; j<timers.size(); j++) {
                CompoundTag item = timers.getCompound(j);
                String ability = item.getString("spell");
                if (ability.length() < 128 && item.getLong("ready") > 0)
                    p.cooldowns.put(ability,item.getLong("ready"));
            }
            data.players.put(row.getUUID("player"), p);
        }
        return data;
    }
}
