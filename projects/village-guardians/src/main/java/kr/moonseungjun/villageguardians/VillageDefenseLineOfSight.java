package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared block-collision LOS for defense fire that has no player camera/entity eye ray. */
public final class VillageDefenseLineOfSight {
    private VillageDefenseLineOfSight() {}

    public static boolean hasLine(ServerLevel level, Vec3 start, Mob target) {
        if (level == null || start == null || target == null || !target.isAlive()) return false;
        Vec3 end = target.position().add(0, Math.max(0.35, target.getBbHeight() * 0.55), 0);
        HitResult hit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
        return hit.getType() == HitResult.Type.MISS;
    }
}
