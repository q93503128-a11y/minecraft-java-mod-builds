package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws every authoritative historical segment that Retrace actually checks for damage. */
@Mixin(value = ReactiveCombatAugmentService.class, remap = false)
abstract class RetracePathPresentationMixin {
    @Inject(method = "damagePathSegment", at = @At("HEAD"))
    private static void augmentationSurvival$showRetraceSegment(
            ServerPlayer owner,
            Vec3 start,
            Vec3 end,
            float damage,
            Set<Integer> hitIds,
            CallbackInfo ci
    ) {
        if (!(owner.level() instanceof ServerLevel level) || start == null || end == null) return;
        double length = start.distanceTo(end);
        int points = Math.max(2, Math.min(12, (int) Math.ceil(length * 4.0D)));
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            Vec3 point = start.lerp(end, t).add(0.0D, 0.85D, 0.0D);
            level.sendParticles(
                    (i & 1) == 0 ? ParticleTypes.PORTAL : ParticleTypes.END_ROD,
                    point.x, point.y, point.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D
            );
        }
    }
}
