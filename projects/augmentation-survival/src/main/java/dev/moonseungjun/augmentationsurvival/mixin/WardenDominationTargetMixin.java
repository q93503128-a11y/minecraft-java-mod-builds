package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla Wardens explicitly reject other Wardens in canTargetEntity. A dominated Warden instead follows
 * the server-owned domination hostility/owner-command rules, including an owner-commanded Warden target.
 */
@Mixin(Warden.class)
abstract class WardenDominationTargetMixin {
    @Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$allowDominatedCommandTarget(
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Warden self = (Warden) (Object) this;
        if (!CompanionAugmentService.isDominated(self)
                || !(target instanceof LivingEntity living)
                || self.level() != living.level()
                || living.isDeadOrDying()
                || living.isInvulnerable()
                || living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if (CompanionAugmentService.mayDominatedMobDamage(self, living)) {
            cir.setReturnValue(true);
        }
    }
}
