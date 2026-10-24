package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityService;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveAugmentService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bridges Overtime into legacy Berserk without duplicating the established active engine. */
@Mixin(value = ActiveAbilityService.class, remap = false)
public abstract class ActiveAbilityOvertimeMixin {
    private static final String BERSERK_ID = AugmentationSurvival.MOD_ID + ":berserk";

    @Inject(method = "executeAbility", at = @At("RETURN"))
    private static void augmentationSurvival$recordManualBerserk(
            ServerPlayer player,
            String abilityId,
            int level,
            double efficiency,
            boolean replayInvocation,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ() && !replayInvocation && BERSERK_ID.equals(abilityId)) {
            AdvancedActiveAugmentService.onLegacyBerserkStarted(player, level);
        }
    }

    @ModifyReturnValue(method = "berserkOutgoingBonus", at = @At("RETURN"))
    private static double augmentationSurvival$appendOvertimeOutgoing(double original, ServerPlayer player) {
        return original + AdvancedActiveAugmentService.berserkOvertimeOutgoingBonus(player);
    }

    @ModifyReturnValue(method = "berserkIncomingMultiplier", at = @At("RETURN"))
    private static double augmentationSurvival$appendOvertimeIncoming(double original, ServerPlayer player) {
        return original + AdvancedActiveAugmentService.berserkOvertimeIncomingBonus(player);
    }
}
