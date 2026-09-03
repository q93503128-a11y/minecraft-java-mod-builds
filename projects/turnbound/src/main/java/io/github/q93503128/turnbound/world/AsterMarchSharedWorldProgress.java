package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * Final physical-world gate authority for Aster March.
 *
 * Individual quest/collection state remains player-owned, but once a region is physically restored/opened the
 * authored map remembers that fact through {@link TurnboundWorldSavedData}. Existing pre-SavedData profiles are
 * reconciled on entry/tick so old worlds do not lose already-earned access.
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
    }

    public static boolean regionOpen(ServerLevel level, String regionId) {
        return level != null && level.getServer() != null
                && TurnboundWorldSavedData.get(level.getServer()).regionUnlocked(regionId);
    }
}
