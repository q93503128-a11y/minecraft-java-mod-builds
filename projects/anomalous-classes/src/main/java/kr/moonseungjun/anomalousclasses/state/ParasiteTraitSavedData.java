package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/** Cooldowns bind to player/ability IDs, not hotbar positions or hosts. */
public final class ParasiteTraitSavedData extends SavedData {
    private static final String KEY = "anomalousclasses_parasite_trait_cooldowns";
    private static final Factory<ParasiteTraitSavedData> FACTORY = new Factory<>(
            ParasiteTraitSavedData::new, ParasiteTraitSavedData::load, DataFixTypes.LEVEL);

    private static final class State {
        long globalUntil;
        final Map<String, Long> readyAt = new HashMap<>();
    }
    private final Map<UUID, State> players = new HashMap<>();

    public static ParasiteTraitSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, KEY);
    }
    private State state(UUID owner) {
        return players.computeIfAbsent(owner, ignored -> new State());
    }
    public long remaining(UUID owner, String ability, long now) {
        State state = state(owner);
        return Math.max(0L, Math.max(state.globalUntil, state.readyAt.getOrDefault(ability, 0L)) - now);
    }
    /** Atomic server-only activation. Rejected actions consume no cooldown. */
    public boolean cast(UUID owner, String ability, long now, int cooldown, int sharedRecovery) {
        if (owner == null || ResourceLocation.tryParse(ability) == null
                || cooldown < 1 || sharedRecovery < 0 || remaining(owner, ability, now) > 0) return false;
        State state = state(owner);
        state.globalUntil = now + sharedRecovery;
        state.readyAt.put(ability, now + cooldown);
        setDirty();
        return true;
    }
    @Override public CompoundTag save(CompoundTag out, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (var entry : players.entrySet()) {
            CompoundTag node = new CompoundTag();
            node.putUUID("owner", entry.getKey());
            node.putLong("globalUntil", entry.getValue().globalUntil);
            CompoundTag ready = new CompoundTag();
            entry.getValue().readyAt.forEach(ready::putLong);
            node.put("ready", ready);
            list.add(node);
        }
        out.put("players", list);
        out.putInt("schemaVersion", 1);
        return out;
    }
    private static ParasiteTraitSavedData load(CompoundTag in, HolderLookup.Provider registries) {
        ParasiteTraitSavedData data = new ParasiteTraitSavedData();
        for (Tag element : in.getList("players", Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag node) || !node.hasUUID("owner")) continue;
            State state = new State();
            state.globalUntil = Math.max(0, node.getLong("globalUntil"));
            CompoundTag ready = node.getCompound("ready");
            for (String id : ready.getAllKeys()) {
                if (ResourceLocation.tryParse(id) != null && state.readyAt.size() < 128)
                    state.readyAt.put(id, Math.max(0, ready.getLong(id)));
            }
            data.players.putIfAbsent(node.getUUID("owner"), state);
        }
        return data;
    }
}
