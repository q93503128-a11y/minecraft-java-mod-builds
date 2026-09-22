package dev.moonseungjun.openworldrpg.combat.state;

import java.util.UUID;

/**
 * One server-owned access point for transient combat state.
 */
public final class CombatStateServices {
    private static final PlayerCombatStateStore STATES = new PlayerCombatStateStore();
    private static final PlayerCombatSnapshotStore COMBAT_SNAPSHOTS = new PlayerCombatSnapshotStore();
    private static final PlayerCombatBuildStore COMBAT_BUILDS = new PlayerCombatBuildStore();
    private static final PlayerDefenseStateStore DEFENSE_STATES = new PlayerDefenseStateStore();

    private CombatStateServices() {
    }

    public static PlayerCombatStateStore states() {
        return STATES;
    }

    public static PlayerCombatSnapshotStore combatSnapshots() {
        return COMBAT_SNAPSHOTS;
    }

    public static PlayerCombatBuildStore combatBuilds() {
        return COMBAT_BUILDS;
    }

    public static PlayerDefenseStateStore defenseStates() {
        return DEFENSE_STATES;
    }

    public static void markCombatActivity(UUID playerId, long gameTick) {
        STATES.getOrCreate(playerId, gameTick).markCombatActivity(gameTick);
    }

    public static void markHostileHpActivity(UUID playerId, long gameTick) {
        STATES.getOrCreate(playerId, gameTick).markHostileHpActivity(gameTick);
    }

    public static void disconnect(UUID playerId) {
        STATES.remove(playerId);
        COMBAT_SNAPSHOTS.remove(playerId);
        COMBAT_BUILDS.remove(playerId);
        DEFENSE_STATES.remove(playerId);
    }
}
