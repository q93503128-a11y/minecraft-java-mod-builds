package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * Final physical-world gate authority for Aster March.
 *
 * Individual quest/collection state remains player-owned, but once a region or authored physical gate is restored
 * the world remembers that fact through {@link TurnboundWorldSavedData}. Shared physical state is monotonic: a
 * lower-progress player may never re-close a door another player already opened for the world.
 */
public final class AsterMarchSharedWorldProgress {
    private AsterMarchSharedWorldProgress() {}

    public static void sync(ServerLevel level, UUID playerId) {
        if (level == null || level.getServer() == null) return;
        TurnboundWorldSavedData data = TurnboundWorldSavedData.get(level.getServer());
        data.reconcilePlayerProgress(playerId);

        AsterMarchWorldShell.setGateOpen(level, AsterMarchWorldShell.Gate.GLOAM_NORTH,
                data.regionUnlocked(TurnboundWorldSavedData.REGION_GLOAMWOOD));
        AsterMarchWorldShell.setGateOpen(level, AsterMarchWorldShell.Gate.AQUEDUCT_WEST,
                data.regionUnlocked(TurnboundWorldSavedData.REGION_BROKEN_AQUEDUCT));
        AsterMarchWorldShell.setGateOpen(level, AsterMarchWorldShell.Gate.QUARRY_PASS,
                data.regionUnlocked(TurnboundWorldSavedData.REGION_EMBER_QUARRY));
        AsterMarchWorldShell.setGateOpen(level, AsterMarchWorldShell.Gate.RELAY_EAST,
                data.regionUnlocked(TurnboundWorldSavedData.REGION_OLD_RELAY_APPROACH));

        // Internal chapter gates are only written when the shared world has unlocked them. We intentionally never
        // write a shared `false` here; chapter builders/session compatibility code may create locked gates initially,
        // but once any player opens one the final world authority only ever restores the open state.
        if (data.regionUnlocked(TurnboundWorldSavedData.GATE_SOUTHGATE_DEEP))
            SouthgateChapterWorld.setEntryGateOpen(level, true);
        if (data.regionUnlocked(TurnboundWorldSavedData.GATE_SOUTHGATE_BOSS))
            SouthgateChapterWorld.setBossGateOpen(level, true);

        if (data.regionUnlocked(TurnboundWorldSavedData.GATE_GLOAM_DEEP))
            GloamwoodChapterWorld.setDeepGateOpen(level, true);
        if (data.regionUnlocked(TurnboundWorldSavedData.GATE_GLOAM_BOSS))
            GloamwoodChapterWorld.setBossGateOpen(level, true);

        if (data.regionUnlocked(TurnboundWorldSavedData.GATE_AQUEDUCT_LOWER))
            BrokenAqueductChapterWorld.setLowerGateOpen(level, true);
        if (data.regionUnlocked(TurnboundWorldSavedData.GATE_AQUEDUCT_ORO))
            BrokenAqueductChapterWorld.setOroGateOpen(level, true);

        if (data.regionUnlocked(TurnboundWorldSavedData.GATE_QUARRY_ASH))
            EmberQuarryChapterWorld.setAshGateOpen(level, true);
        if (data.regionUnlocked(TurnboundWorldSavedData.GATE_QUARRY_BOSS))
            EmberQuarryChapterWorld.setBossGateOpen(level, true);

        if (data.regionUnlocked(TurnboundWorldSavedData.REGION_OLD_RELAY_APPROACH))
            OldRelayStationWorld.setEntranceOpen(level, true);
        if (data.regionUnlocked(TurnboundWorldSavedData.GATE_OLD_RELAY_BOSS))
            OldRelayStationWorld.setBossGateOpen(level, true);
    }

    public static boolean regionOpen(ServerLevel level, String regionId) {
        return level != null && level.getServer() != null
                && TurnboundWorldSavedData.get(level.getServer()).regionUnlocked(regionId);
    }
}
