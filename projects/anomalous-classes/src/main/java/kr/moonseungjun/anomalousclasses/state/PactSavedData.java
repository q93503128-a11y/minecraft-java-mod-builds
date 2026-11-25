package kr.moonseungjun.anomalousclasses.state;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import net.minecraft.nbt.StringTag;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
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
        private final Set<ResourceLocation> viarSurvivalBiomes=new LinkedHashSet<>();
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
        public Set<ResourceLocation> viarSurvivalBiomes() {
            return Set.copyOf(viarSurvivalBiomes);
        }
        public boolean nereaSanctumBreach() { return nereaSanctumBreach; }
        public int nereaReconciliation() { return nereaReconciliationThreats.size(); }
        public int nereaAffinityDebt() { return nereaAffinityDebt; }
    }
    public static final class Player {
        private final LinkedHashMap<String, Bond> bonds = new LinkedHashMap<>();
        private final Map<String, Long> cooldowns = new LinkedHashMap<>();
        /** Exact release bills retained after an expressly forced exit. */
        private final Map<String, List<PactCatalog.Material>> outstandingReleases = new LinkedHashMap<>();
        private final Set<String> completedTrials = new LinkedHashSet<>();
        /** At most 64 real innocent victims for one-time attribution across reloads. */
        private final Set<UUID> innocentVictims = new LinkedHashSet<>();
        private final java.util.ArrayList<String> recordedProfiles = new java.util.ArrayList<>();
        private int revision;
        public List<Bond> bonds() { return List.copyOf(bonds.values()); }
        public Bond bond(String id) { return bonds.get(id); }
        public boolean has(String id) { return bonds.containsKey(id); }
        public int count() { return bonds.size(); }
        public int revision() { return revision; }
        public boolean hasOutstandingRelease(String patron) {
            return outstandingReleases.containsKey(patron);
        }
        public List<PactCatalog.Material> outstandingRelease(String patron) {
            return outstandingReleases.getOrDefault(patron, List.of());
        }
        public Map<String, List<PactCatalog.Material>> outstandingReleases() {
            return Collections.unmodifiableMap(outstandingReleases);
        }
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
        if (PactCatalog.get(patron) == null || p.bonds.containsKey(patron)
                || p.hasOutstandingRelease(patron)) return false;
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
    /**
     * End the selected bond without paying now. Capture its existing release
     * price, plus actual outstanding Hastel combat debt; do not invent a new
     * penalty, tax, or time-dependent charge.
     */
    public boolean forceBreakBond(UUID owner, String patron) {
        Player p = player(owner);
        Bond bond = p.bond(patron);
        PactCatalog.Patron definition = PactCatalog.get(patron);
        if (bond == null || definition == null || p.hasOutstandingRelease(patron)) return false;
        Map<ResourceLocation, Integer> grouped = new LinkedHashMap<>();
        for (PactCatalog.Material material : definition.release())
            grouped.merge(material.itemId(), material.count(), Math::addExact);
        if (patron.equals("hastel") && bond.debt > 0)
            grouped.merge(ResourceLocation.withDefaultNamespace("gold_ingot"), bond.debt, Math::addExact);
        if (grouped.isEmpty()) return false;
        List<PactCatalog.Material> snapshot = new ArrayList<>();
        grouped.forEach((id, count) -> snapshot.add(new PactCatalog.Material(id, count)));
        p.outstandingReleases.put(patron, List.copyOf(snapshot));
        p.bonds.remove(patron);
        p.revision++;
        setDirty();
        return true;
    }

    /** Clear only the specific bill that the server has just charged. */
    public boolean settleForcedRelease(UUID owner, String patron) {
        Player p = player(owner);
        if (p.has(patron) || p.outstandingReleases.remove(patron) == null) return false;
        p.revision++;
        setDirty();
        return true;
    }

    /**
     * Only the verified Minecraft living-death hook can call this, with a
     * genuinely player-caused harmless-villager or nonthreatening-golem death.
     * The caller decides which signed patron condemns that same event.
     */
    public boolean recordInnocentSlaying(UUID owner, UUID victim) {
        Player p = player(owner);
        if (victim == null || (!p.has("isael") && !p.has("sevenwing"))
                || !p.innocentVictims.add(victim)) return false;
        while (p.innocentVictims.size() > 64) {
            var iterator = p.innocentVictims.iterator();
            iterator.next();
            iterator.remove();
        }
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
    /**
     * Only the real rooted-hit healing path can call this. Every qualifying
     * biome is counted once across restarts; the rite itself is stage-locked.
     */
    public boolean addViarBiomeProof(UUID owner,ResourceLocation biome) {
        Player p=player(owner);
        Bond bond=p.bond("viar");
        int required=PactDeepeningService.requirement("viar",1);
        if(bond==null || biome==null || bond.deepening!=1 || required<1
                || bond.progress>=required || !bond.viarSurvivalBiomes.add(biome)) return false;
        bond.progress++;
        p.revision++;
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
        bond.viarSurvivalBiomes.clear();
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
                if(b.patronId.equals("viar")) {
                    ListTag biomes=new ListTag();
                    b.viarSurvivalBiomes.forEach(biome->biomes.add(StringTag.valueOf(biome.toString())));
                    entry.put("viarRiteBiomes",biomes);
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
            ListTag unpaid = new ListTag();
            p.outstandingReleases.forEach((patron, materials) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("patron", patron);
                ListTag bill = new ListTag();
                for (PactCatalog.Material material : materials) {
                    CompoundTag item = new CompoundTag();
                    item.putString("item", material.itemId().toString());
                    item.putInt("count", material.count());
                    bill.add(item);
                }
                entry.put("bill", bill);
                unpaid.add(entry);
            });
            row.put("unpaidReleaseBills", unpaid);
            ListTag innocent = new ListTag();
            p.innocentVictims.forEach(victim -> {
                CompoundTag killed = new CompoundTag();
                killed.putUUID("victim", victim);
                innocent.add(killed);
            });
            row.put("innocentVictims", innocent);
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
                if(id.equals("viar") && bond.deepening==1) {
                    ListTag locations=item.getList("viarRiteBiomes",Tag.TAG_STRING);
                    int cap=Math.max(0,PactDeepeningService.requirement("viar",1));
                    for(int n=0;n<locations.size() && bond.viarSurvivalBiomes.size()<cap;n++) {
                        ResourceLocation biome=ResourceLocation.tryParse(locations.getString(n));
                        if(biome!=null)bond.viarSurvivalBiomes.add(biome);
                    }
                    // Do not generate proof from malformed NBT or infer from past visits.
                    bond.progress=Math.min(bond.progress,bond.viarSurvivalBiomes.size());
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
            ListTag unpaid = row.getList("unpaidReleaseBills", Tag.TAG_COMPOUND);
            for (int j = 0; j < unpaid.size() && j < PactCatalog.all().size(); j++) {
                CompoundTag entry = unpaid.getCompound(j);
                String patron = entry.getString("patron");
                if (PactCatalog.get(patron) == null || p.has(patron)
                        || p.outstandingReleases.containsKey(patron)) continue;
                ListTag bill = entry.getList("bill", Tag.TAG_COMPOUND);
                if (bill.isEmpty() || bill.size() > 12) continue;
                Map<ResourceLocation, Integer> grouped = new LinkedHashMap<>();
                boolean valid = true;
                for (int k = 0; k < bill.size(); k++) {
                    CompoundTag material = bill.getCompound(k);
                    ResourceLocation id = ResourceLocation.tryParse(material.getString("item"));
                    int count = material.getInt("count");
                    if (id == null || count < 1 || count > 4096 || grouped.containsKey(id)) {
                        valid = false;
                        break;
                    }
                    grouped.put(id, count);
                }
                if (!valid) continue;
                List<PactCatalog.Material> materials = new ArrayList<>();
                grouped.forEach((id, count) -> materials.add(new PactCatalog.Material(id, count)));
                p.outstandingReleases.put(patron, List.copyOf(materials));
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
            ListTag victims = row.getList("innocentVictims", Tag.TAG_COMPOUND);
            for (int j = 0; j < victims.size() && p.innocentVictims.size() < 64; j++) {
                CompoundTag evidence = victims.getCompound(j);
                if (evidence.hasUUID("victim")) p.innocentVictims.add(evidence.getUUID("victim"));
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
