package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiCodec;
import io.github.q93503128.turnbound.world.FieldUiSnapshot;

public final class ClientFieldState {
    private static FieldUiSnapshot snapshot = FieldUiSnapshot.inactive();
    private static long revision;

    private ClientFieldState() {}

    public static FieldUiSnapshot snapshot() { return snapshot; }
    public static long revision() { return revision; }

    public static void update(String encoded) {
        snapshot = FieldUiCodec.decode(encoded);
        revision++;
    }
}
