package io.github.q93503128.turnbound;

import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TurnboundCheckpointStateTest {
    private static final String SAFE_ENTRY = """
            {"schema":1,"dimension":"minecraft:overworld",
             "x":101.5,"y":80.0,"z":-205.5,"yaw":90,"pitch":0}
            """;

    @Test void strictEntryHasNoInventedDefaultAndRejectsInvalidCoordinates() {
        var target = TurnboundCheckpointTarget.parseEntry(SAFE_ENTRY);
        assertEquals("minecraft:overworld", target.dimension());
        assertEquals(101.5, target.x());
        assertEquals(-205.5, target.z());
        assertThrows(RuntimeException.class,
                () -> TurnboundCheckpointTarget.parseEntry("{}"));
        assertThrows(RuntimeException.class,
                () -> TurnboundCheckpointTarget.parseEntry(SAFE_ENTRY.replace("101.5", "30000000")));
        assertThrows(RuntimeException.class,
                () -> TurnboundCheckpointTarget.parseEntry(SAFE_ENTRY.replace("80.0", "100000")));
        assertThrows(RuntimeException.class,
                () -> TurnboundCheckpointTarget.parseEntry(SAFE_ENTRY.replace("\"pitch\":0", "\"pitch\":91")));
        assertThrows(RuntimeException.class,
                () -> TurnboundCheckpointTarget.parseEntry(SAFE_ENTRY.replace("minecraft:overworld", "minecraft:the_nether")));
        assertThrows(RuntimeException.class,
                () -> TurnboundCheckpointTarget.parseEntry(SAFE_ENTRY.replace("\"yaw\":90", "\"yaw\":\"90\"")));
        assertThrows(RuntimeException.class,
                () -> TurnboundCheckpointTarget.parseEntry(SAFE_ENTRY.replace("}", ",\"hidden\":true}")));
    }

    @Test void checkpointStateIsUuidScopedAndSurvivesNbtRoundTrip() {
        UUID alice = UUID.fromString("00000000-0000-4000-8000-000000000001");
        UUID bob = UUID.fromString("00000000-0000-4000-8000-000000000002");
        var entry = TurnboundCheckpointTarget.parseEntry(SAFE_ENTRY);
        var other = new TurnboundCheckpointTarget("minecraft:overworld", -310.5, 74.0, 81.5, 180, 10);
        var state = new TurnboundCheckpointState();
        assertTrue(state.get(alice).isEmpty());
        assertFalse(state.isDirty());
        state.set(alice, entry);
        assertTrue(state.isDirty());
        state.set(bob, other);
        NbtCompound stored = state.writeNbt(new NbtCompound());
        assertEquals(1, stored.getInt("Schema"));
        var restored = TurnboundCheckpointState.fromNbt(stored);
        assertEquals(entry, restored.get(alice).orElseThrow());
        assertEquals(other, restored.get(bob).orElseThrow());
        assertTrue(restored.get(UUID.randomUUID()).isEmpty());
        assertFalse(restored.isDirty());
        restored.set(alice, entry);
        assertFalse(restored.isDirty());
        restored.set(alice, other);
        assertTrue(restored.isDirty());
        assertEquals(other, restored.get(bob).orElseThrow());
    }

    @Test void invalidOrDuplicateSavedEntriesRejectedWithoutSilentReset() {
        var state = new TurnboundCheckpointState();
        NbtCompound empty = new NbtCompound();
        assertThrows(IllegalArgumentException.class, () -> TurnboundCheckpointState.fromNbt(empty));
        UUID id = UUID.fromString("00000000-0000-4000-8000-000000000001");
        var target = TurnboundCheckpointTarget.parseEntry(SAFE_ENTRY);
        state.set(id, target);
        NbtCompound valid = state.writeNbt(new NbtCompound());
        NbtList list = valid.getList("Players", NbtElement.COMPOUND_TYPE);
        list.add(list.getCompound(0).copy());
        assertThrows(IllegalArgumentException.class, () -> TurnboundCheckpointState.fromNbt(valid));
    }

    @Test void targetNbtRejectsOutOfWorldPosition() {
        var target = TurnboundCheckpointTarget.parseEntry(SAFE_ENTRY);
        NbtCompound stored = target.toNbt();
        assertEquals(target, TurnboundCheckpointTarget.fromNbt(stored));
        stored.putDouble("Z", 30000000.0);
        assertThrows(IllegalArgumentException.class,
                () -> TurnboundCheckpointTarget.fromNbt(stored));
    }
}
