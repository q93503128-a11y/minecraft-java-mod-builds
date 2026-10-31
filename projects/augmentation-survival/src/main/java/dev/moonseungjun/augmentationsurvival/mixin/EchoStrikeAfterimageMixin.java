package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.EchoStrikeAfterimageService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retires the old charged-hit Echo path; confirmed Player.attack hits dispatch Echo directly. */
@Mixin(value = CombatExpansionService.class, remap = false)
abstract class EchoStrikeAfterimageMixin {
    @Inject(method = "initialize", at = @At("TAIL"))
    private static void augmentationSurvival$initializeEchoAfterimages(CallbackInfo ci) {
        EchoStrikeAfterimageService.initialize();
    }

    /** The old charged/counter trigger must never add a second Echo alongside the new melee hook. */
    @Inject(method = "triggerEchoStrike", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$retireOldEchoTrigger(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            float baseDamageTaken,
            long now,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
