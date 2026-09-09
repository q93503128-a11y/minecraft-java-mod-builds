package dev.moonseungjun.fishinggame.client;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.profile.PlayerFishingProfile;
import dev.moonseungjun.fishinggame.progression.FishingRods;
import dev.moonseungjun.fishinggame.progression.RodDefinition;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class FishingHud {
    private static final Identifier PANEL = FishingGameMod.id("textures/gui/kenney/grey_panel.png");
    private static final Identifier SLIDER = FishingGameMod.id("textures/gui/kenney/grey_slider_horizontal.png");

    private FishingHud() {
    }

    public static void initialize() {
        HudElementRegistry.removeElement(VanillaHudElements.HOTBAR);
        HudElementRegistry.removeElement(VanillaHudElements.ARMOR_BAR);
        HudElementRegistry.removeElement(VanillaHudElements.HEALTH_BAR);
        HudElementRegistry.removeElement(VanillaHudElements.FOOD_BAR);
        HudElementRegistry.removeElement(VanillaHudElements.AIR_BAR);
        HudElementRegistry.removeElement(VanillaHudElements.MOUNT_HEALTH);
        HudElementRegistry.removeElement(VanillaHudElements.INFO_BAR);
        HudElementRegistry.removeElement(VanillaHudElements.EXPERIENCE_LEVEL);
        HudElementRegistry.removeElement(VanillaHudElements.HELD_ITEM_TOOLTIP);
        HudElementRegistry.removeElement(VanillaHudElements.MOB_EFFECTS);
        HudElementRegistry.removeElement(VanillaHudElements.OVERLAY_MESSAGE);
        HudElementRegistry.addLast(FishingGameMod.id("fishing_hud"), FishingHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        int x = 8;
        int y = 8;
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL, x, y, 0, 0, 100, 100, 100, 100);

        RodDefinition rod = FishingRods.byTier(ClientFishingState.rodTier());
        graphics.text(minecraft.font, "FISHING", x + 10, y + 10, 0xFFFFFFFF, true);
        graphics.text(minecraft.font, "코인  " + ClientFishingState.coins(), x + 10, y + 29, 0xFFFFD86B, false);
        graphics.text(minecraft.font, "가방  " + ClientFishingState.catches().size() + "/" + PlayerFishingProfile.BAG_CAPACITY, x + 10, y + 43, 0xFFFFFFFF, false);
        graphics.text(minecraft.font, rod.displayName(), x + 10, y + 57, 0xFFB9E5FF, false);
        graphics.text(minecraft.font, ClientFishingState.locationName(), x + 10, y + 71, 0xFFB8FFCF, false);
        graphics.text(minecraft.font, "[B] 어획 가방", x + 10, y + 85, 0xFFD6D6D6, false);

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int stage = ClientFishingState.stage();

        if (stage == 1) {
            graphics.centeredText(minecraft.font, "입질을 기다리는 중...", width / 2, height - 34, 0xFFFFFFFF);
        } else if (stage == 2) {
            int barX = width / 2 - 95;
            int tensionY = height - 48;
            int progressY = height - 28;
            graphics.centeredText(minecraft.font, ClientFishingState.speciesName(), width / 2, height - 68, 0xFFFFFFFF);
            graphics.text(minecraft.font, "장력", barX, tensionY - 11, 0xFFFFFFFF, false);
            graphics.blit(RenderPipelines.GUI_TEXTURED, SLIDER, barX, tensionY, 0, 0, 190, 4, 190, 4);
            int tensionWidth = Math.max(0, Math.min(188, Math.round(188 * ClientFishingState.tension())));
            graphics.fill(barX + 1, tensionY, barX + 1 + tensionWidth, tensionY + 4, ClientFishingState.tension() >= 0.92f ? 0xFFFF6666 : 0xFF69D9FF);

            graphics.text(minecraft.font, "포획", barX, progressY - 11, 0xFFFFFFFF, false);
            graphics.blit(RenderPipelines.GUI_TEXTURED, SLIDER, barX, progressY, 0, 0, 190, 4, 190, 4);
            int progressWidth = Math.max(0, Math.min(188, Math.round(188 * ClientFishingState.progress())));
            graphics.fill(barX + 1, progressY, barX + 1 + progressWidth, progressY + 4, 0xFF7EE28A);
        }

        String notice = ClientFishingState.notice();
        if (!notice.isBlank()) {
            graphics.centeredText(minecraft.font, notice, width / 2, 18, 0xFFFFFFFF);
        }
    }
}
