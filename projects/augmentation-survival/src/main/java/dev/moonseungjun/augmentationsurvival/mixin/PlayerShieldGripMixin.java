package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Applies Shield Grip at the vanilla Player-owned shield-disable boundary. */
@Mixin(Player.class)
public abstract class PlayerShieldGripMixin {
    @ModifyArg(
            method = "blockUsingItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/component/BlocksAttacks;disable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/item/ItemStack;)V"
            ),
            index = 2
    )
    private float augmentationSurvival$reduceShieldDisableDuration(float originalSeconds) {
        if ((Object) this instanceof ServerPlayer player) {
            return UtilityAugmentService.modifyShieldDisableSeconds(player, originalSeconds);
        }
        return originalSeconds;
    }
}
