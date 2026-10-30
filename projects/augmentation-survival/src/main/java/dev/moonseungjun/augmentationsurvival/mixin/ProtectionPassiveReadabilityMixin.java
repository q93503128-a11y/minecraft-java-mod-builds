package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.presentation.ConditionalPassiveReadabilityService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Protection readability plus Last Stand suppression of new delayed-damage debt. */
@Mixin(value = ProtectionAugmentService.class, remap = false)
public abstract class ProtectionPassiveReadabilityMixin {
    private static final String AUGMENTATION_SURVIVAL$LAST_STAND_ID =
            AugmentationSurvival.MOD_ID + ":last_stand";

    @Inject(method = "afterDamage", at = @At("TAIL"))
    private static void augmentationSurvival$presentDamageAdaptationArmed(
            ServerPlayer player,
            DamageSource source,
            float damageTaken,
            boolean blocked,
            CallbackInfo ci
    ) {
        ConditionalPassiveReadabilityService.presentDamageAdaptationArmed(player, source);
    }

    @Inject(
            method = "modifyAfterReductions",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/moonseungjun/augmentationsurvival/augment/combat/ProtectionAugmentService;safeScale(FD)F",
                    ordinal = 0
            )
    )
    private static void augmentationSurvival$presentDamageAdaptationProc(
            ServerPlayer player,
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Float> cir
    ) {
        ConditionalPassiveReadabilityService.presentDamageAdaptationProc(player);
    }

    @ModifyExpressionValue(
            method = "modifyAfterReductions",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/moonseungjun/augmentationsurvival/augment/combat/ProtectionAugmentService;boundedLevel(Ldev/moonseungjun/augmentationsurvival/state/AugmentationPlayerState;Ljava/lang/String;)I",
                    ordinal = 1
            )
    )
    private static int augmentationSurvival$suppressDelayedReservationDuringLastStand(
            int original,
            ServerPlayer player,
            DamageSource source,
            float damage
    ) {
        return augmentationSurvival$isLastStandProtectionActive(player) ? 0 : original;
    }

    private static boolean augmentationSurvival$isLastStandProtectionActive(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (state.level(AUGMENTATION_SURVIVAL$LAST_STAND_ID) <= 0) {
            return false;
        }
        long now = player.level().getGameTime();
        long readyTick = state.cooldownReadyTick(AUGMENTATION_SURVIVAL$LAST_STAND_ID);
        CombatTuning.Snapshot tuning = CombatTuning.current();
        long triggeredAt = readyTick - tuning.lastStandCooldownTicks();
        return readyTick > now
                && triggeredAt <= now
                && now < triggeredAt + tuning.lastStandInvulnerableTicks();
    }
}
