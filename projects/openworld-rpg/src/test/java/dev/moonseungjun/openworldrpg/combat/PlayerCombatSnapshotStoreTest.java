package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatSnapshotStore;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerCombatSnapshotStoreTest {
    @Test
    void missingSnapshotDoesNotInventBootstrapStats() {
        PlayerCombatSnapshotStore store = new PlayerCombatSnapshotStore();
        assertTrue(store.snapshot(UUID.randomUUID()).isEmpty());
    }

    @Test
    void authoritativeSnapshotCanBeBoundAndRemoved() {
        PlayerCombatSnapshotStore store = new PlayerCombatSnapshotStore();
        UUID player = UUID.randomUUID();
        var snapshot = new ProjectImpactTransaction.DamageSourceSnapshot(
                8, 30.0, 20.0, 0.15, 1.20
        );

        store.bindAuthoritative(player, snapshot);
        assertEquals(snapshot, store.snapshot(player).orElseThrow());
        assertEquals(1, store.size());

        store.remove(player);
        assertTrue(store.snapshot(player).isEmpty());
        assertEquals(0, store.size());
    }
}
