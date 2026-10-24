package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Gigantification doubles outgoing melee knockback at the vanilla player knockback boundary. */
@Mixin(Player.class)
abstract class PlayerGigantificationKnockbackMixin {
    @ModifyVariable(method = "causeExtraKnockback", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float augmentationSurvival$giantKnockback(float strength) {
        Object self = this;
        if (!(self instanceof ServerPlayer player) || !(strength > 0.0F)) {
            return strength;
        }
        return (float) (strength * AdvancedWorldAugmentService.gigantificationKnockbackMultiplier(player));
    }
}
