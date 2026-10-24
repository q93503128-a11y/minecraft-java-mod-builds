package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.AdvancedUtilityAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reduces only the ender-pearl use-cooldown component at its shared vanilla application boundary. */
@Mixin(UseCooldown.class)
public abstract class UseCooldownMasteryMixin {
    @Inject(
            method = "apply(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void augmentationSurvival$applyPearlMasteryCooldown(
            ItemStack stack,
            LivingEntity user,
            CallbackInfo ci
    ) {
        if (!(user instanceof ServerPlayer player) || !stack.is(Items.ENDER_PEARL)) {
            return;
        }
        int vanillaTicks = ((UseCooldown) (Object) this).ticks();
        int modifiedTicks = AdvancedUtilityAugmentService.modifyPearlCooldownTicks(
                player,
                vanillaTicks
        );
        if (modifiedTicks == vanillaTicks) {
            return;
        }
        if (modifiedTicks > 0) {
            player.getCooldowns().addCooldown(stack, modifiedTicks);
        }
        ci.cancel();
    }
}
