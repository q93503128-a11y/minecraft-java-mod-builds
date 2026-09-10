package dev.moonseungjun.fishinggame.client;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishRarity;
import dev.moonseungjun.fishinggame.fishing.FishSpecies;
import dev.moonseungjun.fishinggame.fishing.ReelMath;
import dev.moonseungjun.fishinggame.profile.CatchEntry;
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
    private static final int BAR_INNER_WIDTH = 188;

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
        if (minecraft.player == null || minecraft.gui.hud.isHidden()) return;

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
            graphics.centeredText(minecraft.font, "물결을 보고 입질을 기다리세요", width / 2, height - 34, 0xFFFFFFFF);
        } else if (stage == 2) {
            renderFightHud(graphics, minecraft, rod, width, height);
        }

        renderRecentCatch(graphics, minecraft, width, height);

        String notice = ClientFishingState.notice();
        if (!notice.isBlank()) {
            graphics.centeredText(minecraft.font, notice, width / 2, 18, 0xFFFFFFFF);
        }
    }

    private static void renderFightHud(GuiGraphicsExtractor graphics, Minecraft minecraft, RodDefinition rod, int width, int height) {
        int barX = width / 2 - 95;
        int tensionY = height - 44;
        int progressY = height - 20;
        float tension = ClientFishingState.tension();
        float safeMin = ReelMath.safeMin(rod.controlBonus());
        float safeMax = ReelMath.safeMax(rod.controlBonus());

        graphics.centeredText(minecraft.font, ClientFishingState.speciesName(), width / 2, height - 84, 0xFFFFFFFF);
        graphics.centeredText(minecraft.font, tensionHint(tension, safeMin, safeMax), width / 2, height - 69, hintColor(tension, safeMin, safeMax));

        graphics.text(minecraft.font, "줄 장력", barX, tensionY - 11, 0xFFFFFFFF, false);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SLIDER, barX, tensionY, 0, 0, 190, 4, 190, 4);

        int safeStart = barX + 1 + Math.round(BAR_INNER_WIDTH * safeMin);
        int safeEnd = barX + 1 + Math.round(BAR_INNER_WIDTH * safeMax);
        graphics.fill(safeStart, tensionY, safeEnd, tensionY + 4, 0xFF71D18A);

        int markerX = barX + 1 + Math.round(BAR_INNER_WIDTH * Math.max(0.0f, Math.min(1.0f, tension)));
        int markerColor = tension >= ReelMath.BREAK_TENSION
                ? 0xFFFF6666
                : (tension > safeMax ? 0xFFFFB264 : (tension < safeMin ? 0xFFB9E5FF : 0xFFFFFFFF));
        graphics.fill(markerX - 1, tensionY - 2, markerX + 2, tensionY + 6, markerColor);

        graphics.text(minecraft.font, "포획 진척", barX, progressY - 11, 0xFFFFFFFF, false);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SLIDER, barX, progressY, 0, 0, 190, 4, 190, 4);
        int progressWidth = Math.max(0, Math.min(BAR_INNER_WIDTH, Math.round(BAR_INNER_WIDTH * ClientFishingState.progress())));
        graphics.fill(barX + 1, progressY, barX + 1 + progressWidth, progressY + 4, 0xFF7EE28A);
    }

    private static void renderRecentCatch(GuiGraphicsExtractor graphics, Minecraft minecraft, int width, int height) {
        CatchEntry catchEntry = ClientFishingState.recentCatch();
        if (catchEntry == null) return;

        FishSpecies species = FishCatalog.byId(catchEntry.speciesId());
        int cardX = width / 2 - 50;
        int cardY = height / 2 - 50;
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL, cardX, cardY, 0, 0, 100, 100, 100, 100);

        graphics.centeredText(minecraft.font, "어획 성공", width / 2, cardY + 10, 0xFFFFFFFF);
        graphics.centeredText(minecraft.font, species.displayName(), width / 2, cardY + 27, rarityColor(species.rarity()));
        graphics.centeredText(minecraft.font, species.rarity().displayName(), width / 2, cardY + 41, rarityColor(species.rarity()));
        graphics.centeredText(minecraft.font, String.format("%.2f kg", catchEntry.weightKg()), width / 2, cardY + 57, 0xFFFFFFFF);
        graphics.centeredText(minecraft.font, String.format("%.1f cm", catchEntry.lengthCm()), width / 2, cardY + 70, 0xFFDADADA);
        graphics.centeredText(minecraft.font, catchEntry.value() + " C", width / 2, cardY + 84, 0xFFFFD86B);
    }

    private static String tensionHint(float tension, float safeMin, float safeMax) {
        if (tension < safeMin) return "우클릭을 눌러 감기";
        if (tension > safeMax) return "우클릭을 떼어 줄 풀기";
        return "안정 구간 · 리듬 유지";
    }

    private static int hintColor(float tension, float safeMin, float safeMax) {
        if (tension >= ReelMath.BREAK_TENSION) return 0xFFFF6666;
        if (tension > safeMax) return 0xFFFFB264;
        if (tension < safeMin) return 0xFFB9E5FF;
        return 0xFFB8FFCF;
    }

    private static int rarityColor(FishRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0xFFFFFFFF;
            case UNCOMMON -> 0xFF8EF3A0;
            case RARE -> 0xFF86C5FF;
            case EPIC -> 0xFFC697FF;
            case LEGENDARY -> 0xFFFFCF66;
        };
    }
}
