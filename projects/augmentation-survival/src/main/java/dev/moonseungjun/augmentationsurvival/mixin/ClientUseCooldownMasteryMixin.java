package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.utility.AdvancedUtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mirrors Pearl Mastery's shortened use cooldown on the local client. */
@Mixin(UseCooldown.class)
public abstract class ClientUseCooldownMasteryMixin {
    private static final String PEARL_MASTERY_ID =
            AugmentationSurvival.MOD_ID + ":pearl_mastery";

    @Inject(
            method = "apply(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void augmentationSurvival$applyClientPearlCooldown(
            ItemStack stack,
            LivingEntity user,
            CallbackInfo ci
    ) {
        if (!(user instanceof LocalPlayer player) || !stack.is(Items.ENDER_PEARL)) {
            return;
        }
        int level = ClientAugmentProgressState.level(PEARL_MASTERY_ID);
        int vanillaTicks = ((UseCooldown) (Object) this).ticks();
        int modifiedTicks = AdvancedUtilityAugmentService.modifyPearlCooldownTicksForLevel(
                level,
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
