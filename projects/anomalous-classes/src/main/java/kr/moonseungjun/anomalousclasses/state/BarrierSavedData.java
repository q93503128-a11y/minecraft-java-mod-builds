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

/** Persist recent successful settings and real-time cooldown deadlines across reconnect/restart. */
public final class BarrierSavedData extends SavedData {
    private static final String NAME = "anomalousclasses_barrier";
    private static final Factory<BarrierSavedData> FACTORY =
            new Factory<>(BarrierSavedData::new, BarrierSavedData::load, DataFixTypes.LEVEL);
    private final Map<UUID, Record> players = new HashMap<>();

    public static final class Record {
        private int lastKind = -1;
        private int lastSize = -1;
        private final Map<Integer, Long> readyAtMs = new HashMap<>();
        public BarrierRules.Kind lastKind() { return BarrierRules.Kind.of(lastKind); }
        public BarrierRules.Size lastSize() { return BarrierRules.Size.of(lastSize); }
        public long readyAt(int ability) { return readyAtMs.getOrDefault(ability, 0L); }
    }
    public static BarrierSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }
    private Record record(UUID owner) { return players.computeIfAbsent(owner, ignored -> new Record()); }
    public BarrierRules.Kind lastKind(UUID owner) { return record(owner).lastKind(); }
    public BarrierRules.Size lastSize(UUID owner) { return record(owner).lastSize(); }
    public long readyAt(UUID owner, int ability) { return record(owner).readyAt(ability); }
    public void cast(UUID owner, int ability, long ready, BarrierRules.Kind kind, BarrierRules.Size size) {
        Record r = record(owner);
        r.readyAtMs.put(ability, ready);
        if (kind != null && size != null) {
            r.lastKind = kind.ordinal();
            r.lastSize = size.ordinal();
        }
        setDirty();
    }
    @Override
    public CompoundTag save(CompoundTag output, HolderLookup.Provider registries) {
        ListTag all = new ListTag();
        players.forEach((owner, value) -> {
            CompoundTag player = new CompoundTag();
            player.putUUID("owner", owner);
            player.putInt("kind", value.lastKind);
            player.putInt("size", value.lastSize);
            ListTag cooldowns = new ListTag();
            value.readyAtMs.forEach((ability, ready) -> {
                CompoundTag entry = new CompoundTag();
                entry.putInt("ability", ability);
                entry.putLong("ready", ready);
                cooldowns.add(entry);
            });
            player.put("cooldowns", cooldowns);
            all.add(player);
        });
        output.put("players", all);
        return output;
    }
    private static BarrierSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BarrierSavedData data = new BarrierSavedData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            if (!row.hasUUID("owner")) continue;
            Record record = new Record();
            if (BarrierRules.Kind.of(row.getInt("kind")) != null
                    && BarrierRules.Size.of(row.getInt("size")) != null) {
                record.lastKind = row.getInt("kind");
                record.lastSize = row.getInt("size");
            }
            ListTag cooldowns = row.getList("cooldowns", Tag.TAG_COMPOUND);
            for (int j = 0; j < cooldowns.size(); j++) {
                CompoundTag next = cooldowns.getCompound(j);
                int ability = next.getInt("ability");
                long ready = next.getLong("ready");
                if (ability >= 0 && ability <= 5 && ready > 0L)
                    record.readyAtMs.put(ability, ready);
            }
            data.players.put(row.getUUID("owner"), record);
        }
        return data;
    }
}
