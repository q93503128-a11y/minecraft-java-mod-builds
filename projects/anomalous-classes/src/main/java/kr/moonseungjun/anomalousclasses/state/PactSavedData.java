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
    public static final int ISAEL_RESTITUTION_GOAL = 3;
    private static final String KEY = "anomalousclasses_pacts";
    private static final Factory<PactSavedData> FACTORY =
            new Factory<>(PactSavedData::new, PactSavedData::load, DataFixTypes.LEVEL);
    public static final class Bond {
        private final String patronId;
        private int relation, deepening, debt, progress;
        private boolean vowConflict;
        private int vowFaithDebt;
        private boolean nereaSanctumBreach;
        private int nereaAffinityDebt;
        private final Set<UUID> nereaReconciliationThreats=new LinkedHashSet<>();
        private final Set<UUID> restitutionThreats = new LinkedHashSet<>();
        private final Set<UUID> faithfulDefenseThreats = new LinkedHashSet<>();
        Bond(String patronId, int relation, int deepening, int debt) {
            this(patronId,relation,deepening,debt,0);
        }
        Bond(String patronId, int relation, int deepening, int debt, int progress) {
            this.patronId = patronId;
            this.relation = Math.clamp(relation, 0, 100);
            this.deepening = Math.clamp(deepening,0,2);
            this.progress = Math.clamp(progress,0,100);
            this.debt = Math.max(0, Math.min(24, debt));
        }
        public String id() { return patronId; }
        public int relation() { return relation; }
        public int deepening() { return deepening; }
        public int progress() { return progress; }
        public int debt() { return debt; }
        public boolean vowConflict() { return vowConflict; }
        public int restitution() { return restitutionThreats.size(); }
        public int unresolvedFaithCost() { return vowFaithDebt; }
        public boolean nereaSanctumBreach() { return nereaSanctumBreach; }
        public int nereaReconciliation() { return nereaReconciliationThreats.size(); }
        public int nereaAffinityDebt() { return nereaAffinityDebt; }
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
    /** One event on the SECOND incompatible signature, never a recurring timer. */
    public boolean markIsaelConflict(UUID owner) {
        Player p=player(owner);
        Bond isael=p.bond("isael");
        if(isael==null || !p.has("velkan") || isael.vowConflict)return false;
        isael.vowConflict=true;
        isael.restitutionThreats.clear();
        isael.vowFaithDebt=Math.min(10,isael.relation);
        isael.relation-=isael.vowFaithDebt;
        p.revision++;
        setDirty();
        return true;
    }
    /**
     * Restitution is possible only after ending the conflicting covenant.
     * Three UNIQUE attackers must be intercepted while defending villagers.
     * Evidence is owned by the actual server and persisted, not client packets.
     * The 2+2+6 trust refunded here repairs the single original -10 conflict
     * penalty, without a free increase for joining incompatible contracts.
     */
    public int protectVillagerForIsael(UUID owner, UUID validThreat) {
        Player p=player(owner);
        Bond bond=p.bond("isael");
        if(bond==null || !bond.vowConflict || p.has("velkan")
                || validThreat==null || !bond.restitutionThreats.add(validThreat))return 0;
        int steps=bond.restitutionThreats.size();
        // Only return the faith actually paid. A believer with 0 faith can
        // never turn an incompatible signature into a free reputation farm.
        int refund=steps==ISAEL_RESTITUTION_GOAL
                ? bond.vowFaithDebt : Math.min(2,bond.vowFaithDebt);
        bond.vowFaithDebt-=refund;
        bond.relation=Math.min(100,bond.relation+refund);
        if(steps>=ISAEL_RESTITUTION_GOAL) {
            bond.vowConflict=false;
            bond.restitutionThreats.clear();
        }
        p.revision++;
        setDirty();
        return steps;
    }
    /**
     * Normal trust recovery is earned from a real hostile attack against a
     * villager intercepted by Isael's ward, not from passive login timers.
     * Remember the eight most recent attackers across server restarts.
     */
    public boolean faithfulDefense(UUID owner, UUID validThreat) {
        Player player=player(owner);
        Bond bond=player.bond("isael");
        if(bond==null || bond.vowConflict || validThreat==null
                || bond.relation>=100
                || !bond.faithfulDefenseThreats.add(validThreat))return false;
        while(bond.faithfulDefenseThreats.size()>8) {
            var it=bond.faithfulDefenseThreats.iterator();
            it.next();
            it.remove();
        }
        bond.relation++;
        player.revision++;
        setDirty();
        return true;
    }
    /** A deliberate break of Nerea's own altar core is one covenant event. */
    public boolean defileNereaSanctum(UUID owner) {
        Player state=player(owner);
        Bond bond=state.bond("nerea");
        if(bond==null || bond.nereaSanctumBreach)return false;
        bond.nereaSanctumBreach=true;
        bond.nereaReconciliationThreats.clear();
        bond.nereaAffinityDebt=Math.min(8,bond.relation);
        bond.relation-=bond.nereaAffinityDebt;
        state.revision++;
        setDirty();
        return true;
    }
    /**
     * Repair the bond by two different real underwater tide-to-pressure
     * attacks. Proof of an actual hostile and underwater caster is enforced
     * by the combat caller, never by a client packet.
     */
    public int reconcileNereaSanctum(UUID owner,UUID hostile) {
        Player state=player(owner);
        Bond bond=state.bond("nerea");
        if(bond==null || !bond.nereaSanctumBreach || hostile==null
                || !bond.nereaReconciliationThreats.add(hostile))return 0;
        int progress=bond.nereaReconciliationThreats.size();
        int refund=progress>=2 ? bond.nereaAffinityDebt : Math.min(4,bond.nereaAffinityDebt);
        bond.nereaAffinityDebt-=refund;
        bond.relation=Math.min(100,bond.relation+refund);
        if(progress>=2) {
            bond.nereaSanctumBreach=false;
            bond.nereaReconciliationThreats.clear();
        }
        state.revision++;
        setDirty();
        return progress;
    }
    /** Successful, rate-limited real actions increment only the active rite. */
    public boolean addDeepeningProof(UUID owner,String patron,int cap) {
        Player state=player(owner);
        Bond bond=state.bond(patron);
        if(bond==null || cap!=PactDeepeningService.requirement(patron,bond.deepening())
                || cap<1 || bond.progress>=cap)return false;
        bond.progress++;
        state.revision++;
        setDirty();
        return true;
    }
    /** The caller must check materials and consent on the server thread first. */
    public boolean deepen(UUID owner,String patron,int expectedDepth,int needed) {
        Player state=player(owner);
        Bond bond=state.bond(patron);
        if(bond==null || expectedDepth!=bond.deepening || needed<1
                || bond.progress<needed
                || needed!=PactDeepeningService.requirement(patron,expectedDepth))
            return false;
        bond.deepening++;
        bond.progress=0;
        state.revision++;
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
                entry.putInt("deepeningProgress", b.progress);
                entry.putInt("debt", b.debt);
                if(b.patronId.equals("isael")) {
                    entry.putBoolean("vowConflict",b.vowConflict);
                    entry.putInt("vowFaithDebt",b.vowFaithDebt);
                    ListTag violations=new ListTag();
                    for(UUID threat:b.restitutionThreats) {
                        CompoundTag evidence=new CompoundTag();
                        evidence.putUUID("threat",threat);
                        violations.add(evidence);
                    }
                    entry.put("vowRestitution",violations);
                    ListTag defended=new ListTag();
                    for(UUID threat:b.faithfulDefenseThreats) {
                        CompoundTag evidence=new CompoundTag();
                        evidence.putUUID("threat",threat);
                        defended.add(evidence);
                    }
                    entry.put("faithfulDefenseThreats",defended);
                }
                if(b.patronId.equals("nerea")) {
                    entry.putBoolean("sanctumBreach",b.nereaSanctumBreach);
                    entry.putInt("sanctumDebt",b.nereaAffinityDebt);
                    ListTag recovered=new ListTag();
                    for(UUID hostile:b.nereaReconciliationThreats) {
                        CompoundTag marker=new CompoundTag();
                        marker.putUUID("hostile",hostile);
                        recovered.add(marker);
                    }
                    entry.put("sanctumRestitution",recovered);
                }
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
            boolean legacyIsael=false;
            for (int j=0; j<bonds.size(); j++) {
                CompoundTag item = bonds.getCompound(j);
                String id = item.getString("id");
                if (PactCatalog.get(id) == null || p.bonds.containsKey(id)) continue;
                Bond bond=new Bond(id, item.getInt("relation"), item.getInt("deepening"),
                        item.getInt("debt"), item.getInt("deepeningProgress"));
                if(id.equals("isael")) {
                    legacyIsael=!item.contains("vowConflict");
                    bond.vowConflict=item.getBoolean("vowConflict");
                    bond.vowFaithDebt=Math.clamp(item.getInt("vowFaithDebt"),0,10);
                    if(bond.vowConflict) {
                        ListTag evidence=item.getList("vowRestitution",Tag.TAG_COMPOUND);
                        for(int n=0;n<evidence.size() && bond.restitutionThreats.size()<ISAEL_RESTITUTION_GOAL-1;n++) {
                            CompoundTag victim=evidence.getCompound(n);
                            if(victim.hasUUID("threat"))
                                bond.restitutionThreats.add(victim.getUUID("threat"));
                        }
                    }
                    ListTag defended=item.getList("faithfulDefenseThreats",Tag.TAG_COMPOUND);
                    for(int n=0;n<defended.size() && bond.faithfulDefenseThreats.size()<8;n++) {
                        CompoundTag evidence=defended.getCompound(n);
                        if(evidence.hasUUID("threat"))
                            bond.faithfulDefenseThreats.add(evidence.getUUID("threat"));
                    }
                }
                if(id.equals("nerea")) {
                    bond.nereaSanctumBreach=item.getBoolean("sanctumBreach");
                    if(bond.nereaSanctumBreach) {
                        bond.nereaAffinityDebt=Math.clamp(item.getInt("sanctumDebt"),0,8);
                        ListTag proof=item.getList("sanctumRestitution",Tag.TAG_COMPOUND);
                        for(int k=0;k<proof.size() && bond.nereaReconciliationThreats.size()<1;k++) {
                            CompoundTag evidence=proof.getCompound(k);
                            if(evidence.hasUUID("hostile"))
                                bond.nereaReconciliationThreats.add(evidence.getUUID("hostile"));
                        }
                    }
                }
                p.bonds.put(id,bond);
            }
            // Old saves already applied the -10 once at sign time. Rehydrate
            // its ongoing conflict without charging the same breach again.
            if(legacyIsael && p.has("isael") && p.has("velkan")) {
                Bond isael=p.bond("isael");
                isael.vowConflict=true;
                // The original code already took the -10; do not re-charge.
                // Its original faith is unavailable, so cap restitution to
                // today's existing faith rather than minting 10 free points.
                isael.vowFaithDebt=Math.min(10,isael.relation);
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
