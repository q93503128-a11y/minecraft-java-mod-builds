package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes every non-melee root-direct Lightning event through the repaired single-hit bridge. */
@Mixin(value = CombatExpansionService.class, remap = false)
abstract class LightningRuntimeFixMixin {
    private static final String LIGHTNING_ID = AugmentationSurvival.MOD_ID + ":lightning";

    @Inject(method = "triggerLightning", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$repairLightning(
            ServerPlayer attacker,
            LivingEntity primaryTarget,
            AugmentationPlayerState state,
            float actualDamage,
            CallbackInfo ci
    ) {
        int augmentLevel = Math.max(0, Math.min(3, state.level(LIGHTNING_ID)));
        if (augmentLevel <= 0) return;
        ci.cancel();

        // Player.attack is counted exactly once after the wrapped hurt call returns. Suppress the
        // generic AFTER_DAMAGE copy while that same melee hurt call is still on the stack.
        if (LightningDirectHitBridge.rootMeleeAttackInProgress()) return;
        LightningDirectHitBridge.onDirectHit(attacker, primaryTarget, state, actualDamage);
    }
}
