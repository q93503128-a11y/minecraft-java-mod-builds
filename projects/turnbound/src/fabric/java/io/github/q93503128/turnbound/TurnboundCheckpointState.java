package io.github.q93503128.turnbound;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.PersistentState;

/**
 * Each participant's last trusted checkpoint, owned by the active copied
 * server world and saved through Minecraft's own persistent state manager.
 * No client packet can write this state.
 */
public final class TurnboundCheckpointState extends PersistentState {
    private static final int SCHEMA = 1;
    public static final String STORAGE_KEY = "turnbound_checkpoints_v1";
    private final Map<UUID, TurnboundCheckpointTarget> checkpoints = new HashMap<>();

    public static TurnboundCheckpointState fromNbt(NbtCompound root) {
        if (!root.contains("Schema", NbtElement.INT_TYPE) || root.getInt("Schema") != SCHEMA
                || !root.contains("Players", NbtElement.LIST_TYPE)) {
            throw new IllegalArgumentException("Unsupported TURNBOUND checkpoint storage schema");
        }
        TurnboundCheckpointState state = new TurnboundCheckpointState();
        NbtList list = root.getList("Players", NbtElement.COMPOUND_TYPE);
        if (list.size() > 100000) throw new IllegalArgumentException("Oversized checkpoint storage");
        for (int i = 0; i < list.size(); i++) {
            NbtCompound row = list.getCompound(i);
            if (!row.containsUuid("Player") || !row.contains("Target", NbtElement.COMPOUND_TYPE)) {
                throw new IllegalArgumentException("Invalid checkpoint player record");
            }
            UUID id = row.getUuid("Player");
            TurnboundCheckpointTarget target = TurnboundCheckpointTarget.fromNbt(row.getCompound("Target"));
            if (state.checkpoints.putIfAbsent(id, target) != null) {
                throw new IllegalArgumentException("Duplicate checkpoint UUID");
            }
        }
        return state;
    }

    public Optional<TurnboundCheckpointTarget> get(UUID player) {
        return Optional.ofNullable(checkpoints.get(player));
    }

    public void set(UUID player, TurnboundCheckpointTarget location) {
        if (player == null || location == null) throw new IllegalArgumentException("Missing player/checkpoint");
        if (!location.equals(checkpoints.put(player, location))) {
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound root) {
        root.putInt("Schema", SCHEMA);
        NbtList rows = new NbtList();
        checkpoints.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    NbtCompound row = new NbtCompound();
                    row.putUuid("Player", entry.getKey());
                    row.put("Target", entry.getValue().toNbt());
                    rows.add(row);
                });
        root.put("Players", rows);
        return root;
    }
}
