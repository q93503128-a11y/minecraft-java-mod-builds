package dev.moonseungjun.augmentationsurvival.augment.presentation;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** World-space impact feedback for Wall Slam. No HUD/banner channel is used. */
public final class WallSlamPresentationService {
    private WallSlamPresentationService() {
    }

    public static void impact(ServerPlayer owner, LivingEntity target, Vec3 impactDirection) {
        if (owner == null || target == null || target.isRemoved() || owner.level() != target.level()) {
            return;
        }
        ServerLevel level = (ServerLevel) target.level();
        Vec3 horizontal = new Vec3(impactDirection.x, 0.0D, impactDirection.z);
        if (horizontal.lengthSqr() <= 1.0E-7D) {
            horizontal = new Vec3(
                    target.getX() - owner.getX(),
                    0.0D,
                    target.getZ() - owner.getZ()
            );
        }
        if (horizontal.lengthSqr() <= 1.0E-7D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontal = horizontal.normalize();
        }

        Vec3 center = target.getBoundingBox().getCenter();
        double edge = Math.max(0.45D, target.getBbWidth() * 0.55D + 0.18D);
        Vec3 impact = center.add(horizontal.scale(edge));

        level.sendParticles(ParticleTypes.EXPLOSION,
                impact.x, impact.y, impact.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.POOF,
                impact.x, impact.y, impact.z,
                16, 0.20D, 0.24D, 0.20D, 0.085D);
        level.sendParticles(ParticleTypes.CRIT,
                impact.x, impact.y, impact.z,
                10, 0.24D, 0.28D, 0.24D, 0.13D);

        Vec3 trailStart = center.subtract(horizontal.scale(0.25D));
        Vec3 span = impact.subtract(trailStart);
        for (int i = 1; i <= 5; i++) {
            Vec3 point = trailStart.add(span.scale(i / 5.0D));
            level.sendParticles(ParticleTypes.END_ROD,
                    point.x, point.y, point.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        level.playSound(
                null,
                impact.x, impact.y, impact.z,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                0.46F,
                1.55F
        );
    }
}
