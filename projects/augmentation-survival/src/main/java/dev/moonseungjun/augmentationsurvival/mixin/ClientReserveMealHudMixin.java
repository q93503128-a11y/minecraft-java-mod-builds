package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.resource.ClientConsumptionState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Overlays reserve-meal hunger points directly on the vanilla hunger row in gold. */
@Mixin(Hud.class)
public abstract class ClientReserveMealHudMixin {
    @Unique
    private static final Identifier AUGMENTATION_SURVIVAL$FOOD_HALF =
            Identifier.withDefaultNamespace("hud/food_half");
    @Unique
    private static final Identifier AUGMENTATION_SURVIVAL$FOOD_FULL =
            Identifier.withDefaultNamespace("hud/food_full");
    @Unique
    private static final int AUGMENTATION_SURVIVAL$RESERVE_GOLD =
            ARGB.color(255, 255, 196, 48);

    @Inject(method = "extractFood", at = @At("TAIL"))
    private void augmentationSurvival$renderReserveMeal(
            GuiGraphicsExtractor graphics,
            Player player,
            int yLineBase,
            int xRight,
            CallbackInfo ci
    ) {
        if (ClientConsumptionState.reserveMealLevel() <= 0) {
            return;
        }
        int reserve = Math.max(0, ClientConsumptionState.reservePoints());
        if (reserve <= 0) {
            return;
        }

        int capped = Math.min(20, reserve);
        for (int i = 0; i < 10; i++) {
            int pointsBefore = i * 2;
            if (pointsBefore >= capped) {
                break;
            }
            int x = xRight - i * 8 - 9;
            Identifier sprite = pointsBefore + 1 < capped
                    ? AUGMENTATION_SURVIVAL$FOOD_FULL
                    : AUGMENTATION_SURVIVAL$FOOD_HALF;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    sprite,
                    x,
                    yLineBase,
                    9,
                    9,
                    AUGMENTATION_SURVIVAL$RESERVE_GOLD
            );
        }
    }
}
