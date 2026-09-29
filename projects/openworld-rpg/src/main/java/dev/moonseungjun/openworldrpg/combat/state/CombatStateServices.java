package dev.moonseungjun.openworldrpg.combat.state;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * One server-owned access point for transient combat state.
 */
public final class CombatStateServices {
    private static final PlayerCombatStateStore STATES = new PlayerCombatStateStore();
    private static final PlayerCombatSnapshotStore COMBAT_SNAPSHOTS = new PlayerCombatSnapshotStore();
    private static final PlayerCombatBuildStore COMBAT_BUILDS = new PlayerCombatBuildStore();
    private static final PlayerDefenseStateStore DEFENSE_STATES = new PlayerDefenseStateStore();
    private static final PlayerDefenseSnapshotStore DEFENSE_SNAPSHOTS =
            new PlayerDefenseSnapshotStore();
    private static final PlayerPoiseStateStore PLAYER_POISE_STATES = new PlayerPoiseStateStore();
    private static final PlayerShockStateStore SHOCK_STATES = new PlayerShockStateStore();
    private static final PlayerNegativeStatusStateStore NEGATIVE_STATUS_STATES =
            new PlayerNegativeStatusStateStore();

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

    public static PlayerDefenseSnapshotStore defenseSnapshots() {
        return DEFENSE_SNAPSHOTS;
    }

    public static PlayerPoiseStateStore playerPoiseStates() {
        return PLAYER_POISE_STATES;
    }

    public static PlayerShockStateStore shockStates() {
        return SHOCK_STATES;
    }

    public static PlayerNegativeStatusStateStore negativeStatusStates() {
        return NEGATIVE_STATUS_STATES;
    }

    public static void persistRuntime(ServerPlayer player) {
        UUID playerId = player.getUUID();
        long gameTick = player.level().getGameTime();
        PlayerCombatState runtime = STATES.getOrCreate(playerId, gameTick);
        player.setAttached(
                PlayerCombatSessionAttachments.COMBAT_SESSION,
                runtime.persistentSnapshot(gameTick)
        );
    }

    public static void restoreRuntime(ServerPlayer player) {
        PlayerCombatSessionState snapshot = player.getAttachedOrSet(
                PlayerCombatSessionAttachments.COMBAT_SESSION,
                PlayerCombatSessionState.empty()
        );
        if (!snapshot.captured()) {
            return;
        }
        long gameTick = player.level().getGameTime();
        STATES.getOrCreate(player.getUUID(), gameTick)
                .restorePersistent(snapshot, gameTick);
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
        DEFENSE_SNAPSHOTS.remove(playerId);
        PLAYER_POISE_STATES.remove(playerId);
        SHOCK_STATES.remove(playerId);
        NEGATIVE_STATUS_STATES.remove(playerId);
    }
}
