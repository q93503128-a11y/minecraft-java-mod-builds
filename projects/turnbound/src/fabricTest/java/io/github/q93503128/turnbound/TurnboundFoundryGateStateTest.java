package io.github.q93503128.turnbound;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TurnboundFoundryGateStateTest {
    @Test void allThreeRealInteractionsRequiredAndOffResetsReadiness() {
        var s = new TurnboundFoundryGateState();
        assertFalse(s.ready());
        assertFalse(s.isDirty());
        s.observeLever(0, true);
        s.observeLever(1, true);
        assertEquals(2, s.knownCount());
        assertFalse(s.ready());
        s.observeLever(2, true);
        assertTrue(s.ready());
        assertTrue(s.isDirty());
        s.observeLever(1, false);
        assertFalse(s.ready());
        s.observeLever(1, true);
        assertTrue(s.ready());
        assertThrows(IllegalArgumentException.class, () -> s.observeLever(3, true));
    }

    @Test void onlyAnActuallyReadyGateMayBeMarkedOpened() {
        var s = new TurnboundFoundryGateState();
        assertThrows(IllegalStateException.class, s::markOpened);
        for (int i = 0; i < 3; i++) s.observeLever(i, true);
        s.markOpened();
        assertTrue(s.opened());
        s.observeLever(2, false);
        assertTrue(s.opened()); // Physical open is a one-way latch.
    }

    @Test void persistsAcrossRestartAndRejectsCorruptFlags() {
        var s = new TurnboundFoundryGateState();
        for (int i = 0; i < 3; i++) s.observeLever(i, true);
        NbtCompound saved = s.writeNbt(new NbtCompound());
        var restored = TurnboundFoundryGateState.fromNbt(saved);
        assertTrue(restored.ready());
        assertFalse(restored.isDirty());
        restored.markOpened();
        assertTrue(TurnboundFoundryGateState.fromNbt(
                restored.writeNbt(new NbtCompound())).opened());
        saved.putInt("Powered", 8);
        assertThrows(IllegalArgumentException.class,
                () -> TurnboundFoundryGateState.fromNbt(saved));
        saved.putInt("Powered", 7);
        saved.putInt("Known", 6);
        assertThrows(IllegalArgumentException.class,
                () -> TurnboundFoundryGateState.fromNbt(saved));
        NbtCompound empty = new NbtCompound();
        assertThrows(IllegalArgumentException.class,
                () -> TurnboundFoundryGateState.fromNbt(empty));
    }

    @Test void unchangedObservationsDoNotMarkStateDirty() {
        var s = new TurnboundFoundryGateState();
        s.observeLever(0, true);
        var loaded = TurnboundFoundryGateState.fromNbt(s.writeNbt(new NbtCompound()));
        loaded.observeLever(0, true);
        assertFalse(loaded.isDirty());
        loaded.observeLever(0, false);
        assertTrue(loaded.isDirty());
    }
}
