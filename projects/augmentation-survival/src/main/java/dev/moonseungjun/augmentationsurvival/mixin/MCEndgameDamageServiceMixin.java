package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.MCEndgameDamageCompatibility;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies Augmentation Survival's final player protection to MCEndgame's real calculated damage. */
@Pseudo
@Mixin(targets = "de.fuballer.mcendgame.main.component.damage.DamageService", remap = false)
abstract class MCEndgameDamageServiceMixin {
    @Inject(method = "applyDamage", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void augmentationSurvival$modifyCalculatedPlayerDamage(
            LivingEntity victim,
            DamageSource source,
            CallbackInfoReturnable<Optional<Float>> cir
    ) {
        if (!(victim instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        cir.setReturnValue(MCEndgameDamageCompatibility.modifyCalculatedDamage(
                player,
                level,
                source,
                cir.getReturnValue()
        ));
    }
}
