package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.client.CombatHudClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Strong local scene-color treatment while the caster stands inside their active Time Stop. */
@Mixin(value = CombatHudClient.class, remap = false)
abstract class TimeStopHudPresentationMixin {
    @Inject(method = "drawTimeStopEdge", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$drawTimeStopScene(
            GuiGraphicsExtractor g,
            int width,
            int height,
            CallbackInfo ci
    ) {
        ci.cancel();
        long phase = (System.currentTimeMillis() / 95L) % 12L;
        int pulse = (int) Math.abs(phase - 6L);
        int washAlpha = 0x36 + Math.max(0, 5 - pulse);
        int wash = (washAlpha << 24) | 0x343B43;
        g.fill(0, 0, width, height, wash);

        int outer = 14;
        int middle = 7;
        g.fill(0, 0, width, outer, 0x7A171C22);
        g.fill(0, height - outer, width, height, 0x7A171C22);
        g.fill(0, outer, outer, height - outer, 0x7A171C22);
        g.fill(width - outer, outer, width, height - outer, 0x7A171C22);

        g.fill(outer, outer, width - outer, outer + middle, 0x403F4B56);
        g.fill(outer, height - outer - middle, width - outer, height - outer, 0x403F4B56);
        g.fill(outer, outer + middle, outer + middle, height - outer - middle, 0x403F4B56);
        g.fill(width - outer - middle, outer + middle, width - outer, height - outer - middle, 0x403F4B56);

        int line = 2;
        int edge = 0xC0B7C8D3;
        g.fill(outer, outer, width - outer, outer + line, edge);
        g.fill(outer, height - outer - line, width - outer, height - outer, edge);
        g.fill(outer, outer, outer + line, height - outer, edge);
        g.fill(width - outer - line, outer, width - outer, height - outer, edge);
    }
}
