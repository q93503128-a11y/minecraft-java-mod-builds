package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiCodec;
import io.github.q93503128.turnbound.world.FieldUiSnapshot;

/** Client field state plus first-snapshot lifecycle used by the immediate loading cover. */
public final class ClientFieldState {
    private static FieldUiSnapshot snapshot = FieldUiSnapshot.inactive();
    private static long revision;
    private static boolean initialSnapshotReceived;

    private ClientFieldState() {}

    public static FieldUiSnapshot snapshot() { return snapshot; }
    public static long revision() { return revision; }
    public static boolean initialSnapshotReceived() { return initialSnapshotReceived; }

    /** Called when the client enters a new level so the first world frame can be covered before networking catches up. */
    public static void beginWorld() {
        snapshot = FieldUiSnapshot.inactive();
        initialSnapshotReceived = false;
        revision++;
    }

    public static void update(String encoded) {
        snapshot = FieldUiCodec.decode(encoded);
        initialSnapshotReceived = true;
        revision++;
    }
}
