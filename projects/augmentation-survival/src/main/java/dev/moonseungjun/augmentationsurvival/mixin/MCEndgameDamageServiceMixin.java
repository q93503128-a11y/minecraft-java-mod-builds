package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.MCEndgameDamageResultBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Integrates final augment protection at pinned MCEndgame's own calculation boundary. */
@Pseudo
@Mixin(targets = "de.fuballer.mcendgame.main.component.damage.DamageService", remap = false)
abstract class MCEndgameDamageServiceMixin {
    @Inject(
            method = "calculateFinalDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/server/level/ServerLevel;Lde/fuballer/mcendgame/main/component/damage/dealing/ExtendedDamageSource;F)Lde/fuballer/mcendgame/main/component/damage/DamageCalculationResult;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void augmentationSurvival$modifyCalculatedPlayerDamage(
            LivingEntity victim,
            ServerLevel level,
            @Coerce DamageSource source,
            float incomingDamage,
            CallbackInfoReturnable<Object> cir
    ) {
        if (!(victim instanceof Player player)) {
            return;
        }
        cir.setReturnValue(MCEndgameDamageResultBridge.modify(
                player,
                level,
                source,
                cir.getReturnValue()
        ));
    }
}
