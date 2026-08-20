package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Compatibility/status facade for the defense layer.
 *
 * Production combat is owned exclusively by VillagePlacedTurretSystem. The old fixed-corner
 * tower firing loop was retired in phase 2 and is intentionally absent here so it cannot be
 * accidentally re-entered by a future server-tick registration.
 */
public final class VillageDefenseSystem {
    private VillageDefenseSystem() {}

    public static void reset() {
        VillageTowerSpecializationSystem.resetTransientState();
    }

    public static boolean recognizeDefenseMob(Mob mob) {
        return VillageMercenarySystem.adoptLegacy(mob);
    }

    /** Compatibility facade: production hiring is owned by VillageMercenarySystem. */
    public static int mercenaryHireCost() {
        return VillageMercenarySystem.hireCost(VillageMercenarySystem.MercenaryClass.BASTION);
    }

    public static String hireMercenary(ServerPlayer player) {
        return VillageMercenarySystem.hire(player, VillageMercenarySystem.MercenaryClass.BASTION);
    }

    public static String status(ServerLevel level) {
        StringBuilder towers = new StringBuilder();
        for (VillageTowerSpecializationSystem.TowerKind kind : VillageTowerSpecializationSystem.TowerKind.values()) {
            if (!towers.isEmpty()) towers.append(" | ");
            towers.append(kind.displayName()).append(' ').append(VillageTowerSpecializationSystem.summary(kind));
        }
        return towers + " | " + VillageMercenarySystem.status(level.getServer());
    }
}
