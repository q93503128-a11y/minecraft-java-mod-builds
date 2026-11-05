package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla Slime.contact attack only damages players and iron golems. A dominated
 * slime may contact-damage its current server-authorized hostile/owner-command
 * target, using the ORIGINAL slime dealDamage method (including size and cooldown).
 * MagmaCube inherits the same path; ordinary undominated slimes are untouched.
 */
@Mixin(AbstractCubeMob.class)
abstract class DominatedSlimeContactMixin {
    @Shadow
    protected abstract void dealDamage(LivingEntity target);

    @Inject(method = "push", at = @At("TAIL"))
    private void augmentationSurvival$dominatedSlimeContact(Entity entity, CallbackInfo ci) {
        AbstractCubeMob cube = (AbstractCubeMob) (Object) this;
        if (!(cube instanceof Slime || cube instanceof MagmaCube)) return;
        if (!(cube.level() instanceof ServerLevel) || !CompanionAugmentService.isDominated(cube)) return;
        if (!(entity instanceof LivingEntity target) || cube.getTarget() != target
                || !CompanionAugmentService.mayDominatedMobDamage(cube, target)) return;
        if (!cube.getBoundingBox().intersects(target.getBoundingBox().inflate(0.05D))) return;
        // Preserves vanilla touch mechanics and the zero damage of the smallest slime.
        dealDamage(target);
    }
}
