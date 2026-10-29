package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents reroll UI overlap/double delivery from becoming an accidental candidate selection. */
@Mixin(AugmentDeviceMenu.class)
abstract class AugmentMenuRerollDebounceMixin {
    @Unique private static final long AUGMENTATION_SURVIVAL$SELECTION_GUARD_TICKS = 3L;
    @Unique private long augmentationSurvival$lastRerollTick = Long.MIN_VALUE;

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$guardRerollTransaction(
            Player player,
            int buttonId,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        long now = serverPlayer.level().getGameTime();
        boolean selection = buttonId >= AugmentDeviceMenu.candidateButtonId(0)
                && buttonId <= AugmentDeviceMenu.candidateButtonId(2);
        boolean reroll = buttonId >= AugmentDeviceMenu.rerollButtonId(0)
                && buttonId <= AugmentDeviceMenu.rerollButtonId(2);

        if (reroll) {
            if (augmentationSurvival$lastRerollTick == now) {
                cir.setReturnValue(true);
                return;
            }
            augmentationSurvival$lastRerollTick = now;
            return;
        }
        if (selection
                && augmentationSurvival$lastRerollTick != Long.MIN_VALUE
                && now >= augmentationSurvival$lastRerollTick
                && now - augmentationSurvival$lastRerollTick <= AUGMENTATION_SURVIVAL$SELECTION_GUARD_TICKS) {
            cir.setReturnValue(true);
        }
    }
}
