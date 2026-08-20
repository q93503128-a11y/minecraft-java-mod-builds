package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Compatibility/status facade for the defense layer.
 *
 * Production combat is owned exclusively by VillagePlacedTurretSystem. Fixed corner towers are
 * fortress architecture only; the retired global four-tower specialization state is not read.
 */
public final class VillageDefenseSystem {
    private VillageDefenseSystem() {}

    public static void reset() {
        // All production defense runtime state is reset by its owning systems.
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
        return "배치 포탑 " + VillagePlacedTurretSystem.activeCount() + "/"
                + VillagePlacedTurretSystem.count() + "기 가동 · 설치 "
                + VillagePlacedTurretSystem.count() + "/" + VillagePlacedTurretSystem.capacity()
                + " | " + VillageMercenarySystem.status(level.getServer());
    }
}
