package dev.moonseungjun.openworldrpg.combat.state;

import java.util.UUID;

/**
 * One server-owned access point for transient combat state.
 */
public final class CombatStateServices {
    private static final PlayerCombatStateStore STATES = new PlayerCombatStateStore();
    private static final PlayerCombatSnapshotStore COMBAT_SNAPSHOTS = new PlayerCombatSnapshotStore();

    private CombatStateServices() {
    }

    public static PlayerCombatStateStore states() {
        return STATES;
    }

    public static PlayerCombatSnapshotStore combatSnapshots() {
        return COMBAT_SNAPSHOTS;
    }

    public static void markCombatActivity(UUID playerId, long gameTick) {
        STATES.getOrCreate(playerId, gameTick).markCombatActivity(gameTick);
    }

    public static void disconnect(UUID playerId) {
        STATES.remove(playerId);
        COMBAT_SNAPSHOTS.remove(playerId);
    }
}
