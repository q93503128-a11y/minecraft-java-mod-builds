package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces vanilla heart extraction; hunger/armor/air remain vanilla. */
@Mixin(Hud.class)
public abstract class ClientHudHealthMixin {
    @Inject(method = "extractHearts", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$replaceVanillaHearts(
            GuiGraphicsExtractor graphics,
            Player player,
            int xLeft,
            int yLineBase,
            int healthRowHeight,
            int heartOffsetIndex,
            float maxHealth,
            int currentHealth,
            int oldHealth,
            int absorption,
            boolean blink,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
