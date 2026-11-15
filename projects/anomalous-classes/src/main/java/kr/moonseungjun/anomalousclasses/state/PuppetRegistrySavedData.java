package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-authoritative identity ledger. Every transfer between item and
 * deployed entity is a compare-and-swap revision transition.
 */
public final class PuppetRegistrySavedData extends SavedData {
    private static final String DATA_NAME = "anomalousclasses_puppets";
    private static final Factory<PuppetRegistrySavedData> FACTORY = new Factory<>(
            PuppetRegistrySavedData::new, PuppetRegistrySavedData::load, DataFixTypes.LEVEL);

    public enum Location { STORED, DEPLOYED, BROKEN, RETIRED }

    public record Entry(PuppetBlueprint blueprint, Location location, int revision) {
        public Entry {
            if (blueprint == null || location == null || revision <= 0)
                throw new IllegalArgumentException("Puppet entry must have an identity and revision");
        }
    }

    public record HoldPosition(String dimension, double x, double y, double z) {
        public HoldPosition {
            if (dimension == null || dimension.length() > 128
                    || net.minecraft.resources.ResourceLocation.tryParse(dimension) == null
                    || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                throw new IllegalArgumentException("Invalid puppet hold position");
        }
    }
    private final Map<UUID, Entry> entries = new HashMap<>();
    private final Map<UUID, HoldPosition> holds = new HashMap<>();

    public Optional<HoldPosition> hold(UUID id) {
        return Optional.ofNullable(holds.get(id));
    }

    /** Owner-controlled positional command, independent of the player's active skills. */
    public boolean setHold(UUID owner, UUID id, int revision, HoldPosition pos) {
        Entry e = entries.get(id);
        if (e == null || e.location() != Location.DEPLOYED || e.revision() != revision
                || !e.blueprint().ownerId().equals(owner)) return false;
        if (pos == null) holds.remove(id); else holds.put(id, pos);
        setDirty();
        return true;
    }

    public void clearHold(UUID id) {
        if (holds.remove(id) != null) setDirty();
    }

    public static PuppetRegistrySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Optional<Entry> find(UUID id) { return Optional.ofNullable(entries.get(id)); }

    public List<Entry> deployedBy(UUID owner) {
        return entries.values().stream().filter(e -> e.location() == Location.DEPLOYED
                && e.blueprint().ownerId().equals(owner))
                .sorted(Comparator.comparing(e -> e.blueprint().puppetId())).toList();
    }

    /** Sync last-known damage/recovery for unloaded, crash-recovered and cross-class recalls. */
    public boolean updateDeployedSnapshot(PuppetBlueprint snapshot, int revision) {
        Entry entry = entries.get(snapshot.puppetId());
        if (entry == null || entry.location() != Location.DEPLOYED
                || entry.revision() != revision
                || !entry.blueprint().ownerId().equals(snapshot.ownerId())) return false;
        if (!entry.blueprint().equals(snapshot)) {
            entries.put(snapshot.puppetId(), new Entry(snapshot, Location.DEPLOYED, revision));
            setDirty();
        }
        return true;
    }

    public long deployedCount(UUID owner) {
        return entries.values().stream().filter(e -> e.location() == Location.DEPLOYED
                && e.blueprint().ownerId().equals(owner)).count();
    }

    public boolean registerCrafted(PuppetBlueprint blueprint) {
        if (entries.containsKey(blueprint.puppetId())) return false;
        entries.put(blueprint.puppetId(), new Entry(blueprint, Location.STORED, 1));
        setDirty();
        return true;
    }

    /** The old item revision becomes stale as soon as deployment is committed. */
    public Optional<Entry> deploy(UUID owner, UUID id, int expectedRevision) {
        return transition(owner, id, expectedRevision, Location.STORED, Location.DEPLOYED, null);
    }

    /** Returns an updated snapshot for the new, exclusively valid stored item. */
    public Optional<Entry> recall(UUID owner, UUID id, int expectedRevision,
                                  PuppetBlueprint snapshot) {
        return transition(owner, id, expectedRevision, Location.DEPLOYED, Location.STORED, snapshot);
    }

    public Optional<Entry> breakPuppet(UUID owner, UUID id, int expectedRevision,
                                       PuppetBlueprint snapshot) {
        return transition(owner, id, expectedRevision, Location.DEPLOYED, Location.BROKEN, snapshot);
    }

    /**
     * CAS for an inventory-held puppet at the workbench. Never accepts caller-made
     * raw materials or ownership changes; all materials originate on the server.
     * Repairs may restore BROKEN to STORED, while size increases require STORED.
     */
    public Optional<Entry> reviseStored(UUID owner, UUID id, int expectedRevision,
                                         PuppetBlueprint expectedSnapshot, PuppetBlueprint updated,
                                         boolean repair) {
        Entry current = entries.get(id);
        if (current == null || current.revision() != expectedRevision
                || current.revision() == Integer.MAX_VALUE
                || !(current.location() == Location.STORED ||
                     repair && current.location() == Location.BROKEN)
                || !current.blueprint().ownerId().equals(owner)
                || !current.blueprint().equals(expectedSnapshot)
                || updated == null || !updated.puppetId().equals(id)
                || !updated.ownerId().equals(owner)) return Optional.empty();
        Entry replaced = new Entry(updated, Location.STORED, expectedRevision + 1);
        entries.put(id, replaced);
        setDirty();
        return Optional.of(replaced);
    }

    /** One-way disassembly: no later stale item may redeploy that ID. */
    public Optional<Entry> dismantle(UUID owner, UUID id, int expectedRevision) {
        Entry current = entries.get(id);
        if (current == null || (current.location() != Location.STORED &&
                current.location() != Location.BROKEN)) return Optional.empty();
        return transition(owner, id, expectedRevision, current.location(), Location.RETIRED, null);
    }

    private Optional<Entry> transition(UUID owner, UUID id, int revision, Location from,
                                       Location to, PuppetBlueprint replacement) {
        Entry existing = entries.get(id);
        if (existing == null || existing.revision() != revision || existing.location() != from
                || !existing.blueprint().ownerId().equals(owner)
                || (replacement != null && (!replacement.ownerId().equals(owner)
                        || !replacement.puppetId().equals(id)))) return Optional.empty();
        if (existing.revision() == Integer.MAX_VALUE) return Optional.empty();
        Entry updated = new Entry(replacement == null ? existing.blueprint() : replacement,
                to, existing.revision() + 1);
        entries.put(id, updated);
        if (to != Location.DEPLOYED) holds.remove(id);
        setDirty();
        return Optional.of(updated);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag item = new CompoundTag();
            item.put("blueprint", entry.blueprint().save());
            item.putString("location", entry.location().name());
            item.putInt("revision", entry.revision());
            list.add(item);
        }
        tag.put("entries", list);
        ListTag holdList = new ListTag();
        holds.forEach((id, pos) -> {
            CompoundTag hold = new CompoundTag();
            hold.putUUID("puppet", id);
            hold.putString("dimension", pos.dimension());
            hold.putDouble("x", pos.x());
            hold.putDouble("y", pos.y());
            hold.putDouble("z", pos.z());
            holdList.add(hold);
        });
        tag.put("holds", holdList);
        return tag;
    }

    private static PuppetRegistrySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PuppetRegistrySavedData result = new PuppetRegistrySavedData();
        for (Tag element : tag.getList("entries", Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag item)) continue;
            Optional<PuppetBlueprint> blueprint = PuppetBlueprint.load(item.getCompound("blueprint"));
            if (blueprint.isEmpty()) continue;
            try {
                Entry entry = new Entry(blueprint.get(),
                        Location.valueOf(item.getString("location")), item.getInt("revision"));
                result.entries.putIfAbsent(blueprint.get().puppetId(), entry);
            } catch (IllegalArgumentException ignored) {
                // Corrupt records never become deployable assets.
            }
        }
        for (Tag element : tag.getList("holds", Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag hold) || !hold.hasUUID("puppet")) continue;
            UUID id = hold.getUUID("puppet");
            Entry owner = result.entries.get(id);
            if (owner == null || owner.location() != Location.DEPLOYED) continue;
            try {
                result.holds.putIfAbsent(id, new HoldPosition(hold.getString("dimension"),
                        hold.getDouble("x"), hold.getDouble("y"), hold.getDouble("z")));
            } catch (IllegalArgumentException ignored) {
                // Invalid anchors never create permanent forced navigation.
            }
        }
        return result;
    }
}
