package dev.moonseungjun.fishinggame.client;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.fishing.CollectionRewards;
import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishSizeGrade;
import dev.moonseungjun.fishinggame.fishing.FishSpecies;
import dev.moonseungjun.fishinggame.fishing.FishingLocation;
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

public final class FishingHud {
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
        FishingUiTheme.drawPanel(graphics, x, y, 2, 1);

        RodDefinition rod = FishingRods.byTier(ClientFishingState.rodTier());
        FishingLocation location = currentLocation();
        int found = CollectionRewards.discoveredCount(ClientFishingState.records(), location);
        int total = CollectionRewards.speciesCount(location);

        graphics.text(minecraft.font, "FISHING", x + 11, y + 10, FishingUiTheme.TEXT_PRIMARY, false);
        graphics.text(
                minecraft.font,
                ClientFishingState.locationName() + "  " + found + "/" + total,
                x + 78,
                y + 10,
                found >= total ? FishingUiTheme.MONEY : FishingUiTheme.SUCCESS,
                false
        );
        graphics.fill(x + 10, y + 24, x + 190, y + 25, FishingUiTheme.BORDER);
        graphics.text(minecraft.font, "코인  " + ClientFishingState.coins(), x + 11, y + 31, FishingUiTheme.MONEY, false);
        graphics.text(
                minecraft.font,
                "가방  " + ClientFishingState.catches().size() + "/" + PlayerFishingProfile.BAG_CAPACITY,
                x + 104,
                y + 31,
                FishingUiTheme.TEXT_PRIMARY,
                false
        );
        graphics.text(minecraft.font, "낚싯대  " + rod.displayName(), x + 11, y + 50, FishingUiTheme.ACCENT, false);
        graphics.text(minecraft.font, "B 가방   J 도감   M 이동", x + 11, y + 82, FishingUiTheme.TEXT_SECONDARY, false);

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int stage = ClientFishingState.stage();

        if (FishingGameClient.isCastCharging()) {
            renderCastHud(graphics, minecraft, width, height);
        } else if (stage == 1) {
            graphics.centeredText(
                    minecraft.font,
                    "물결을 보고 입질을 기다리세요",
                    width / 2,
                    height - 34,
                    FishingUiTheme.OVERLAY_PRIMARY
            );
        } else if (stage == 2) {
            renderFightHud(graphics, minecraft, rod, width, height);
        }

        renderRecentCatch(graphics, minecraft, width, height);

        String notice = ClientFishingState.notice();
        if (!notice.isBlank()) {
            graphics.centeredText(minecraft.font, notice, width / 2, 18, FishingUiTheme.OVERLAY_PRIMARY);
        }
    }

    private static FishingLocation currentLocation() {
        for (FishingLocation location : FishingLocation.values()) {
            if (location.displayName().equals(ClientFishingState.locationName())) return location;
        }
        return FishingLocation.LAKESIDE;
    }

    private static void renderCastHud(GuiGraphicsExtractor graphics, Minecraft minecraft, int width, int height) {
        int barX = width / 2 - 95;
        int barY = height - 28;
        float charge = FishingGameClient.castChargeProgress();
        String hint = charge >= 1.0f ? "최대 거리 · 놓아서 던지기" : "캐스팅 · 놓아서 던지기";
        int hintColor = charge >= 1.0f ? FishingUiTheme.OVERLAY_MONEY : FishingUiTheme.OVERLAY_PRIMARY;

        graphics.centeredText(minecraft.font, hint, width / 2, barY - 16, hintColor);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FishingUiTheme.SLIDER, barX, barY, 0, 0, 190, 4, 190, 4);
        int fillWidth = Math.max(0, Math.min(BAR_INNER_WIDTH, Math.round(BAR_INNER_WIDTH * charge)));
        graphics.fill(
                barX + 1,
                barY,
                barX + 1 + fillWidth,
                barY + 4,
                charge >= 1.0f ? FishingUiTheme.OVERLAY_MONEY : FishingUiTheme.OVERLAY_ACCENT
        );
    }

    private static void renderFightHud(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            RodDefinition rod,
            int width,
            int height
    ) {
        int barX = width / 2 - 95;
        int tensionY = height - 44;
        int progressY = height - 20;
        float tension = ClientFishingState.tension();
        float safeMin = ReelMath.safeMin(rod.controlBonus());
        float safeMax = ReelMath.safeMax(rod.controlBonus());

        graphics.centeredText(
                minecraft.font,
                ClientFishingState.speciesName(),
                width / 2,
                height - 84,
                FishingUiTheme.OVERLAY_PRIMARY
        );
        graphics.centeredText(
                minecraft.font,
                tensionHint(tension, safeMin, safeMax),
                width / 2,
                height - 69,
                hintColor(tension, safeMin, safeMax)
        );

        graphics.text(minecraft.font, "줄 장력", barX, tensionY - 11, FishingUiTheme.OVERLAY_PRIMARY, false);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FishingUiTheme.SLIDER, barX, tensionY, 0, 0, 190, 4, 190, 4);
        int safeStart = barX + 1 + Math.round(BAR_INNER_WIDTH * safeMin);
        int safeEnd = barX + 1 + Math.round(BAR_INNER_WIDTH * safeMax);
        graphics.fill(safeStart, tensionY, safeEnd, tensionY + 4, 0xFF71D18A);
        int markerX = barX + 1 + Math.round(BAR_INNER_WIDTH * Math.max(0.0f, Math.min(1.0f, tension)));
        int markerColor = tension >= ReelMath.BREAK_TENSION
                ? FishingUiTheme.OVERLAY_DANGER
                : (tension > safeMax
                        ? FishingUiTheme.OVERLAY_WARNING
                        : (tension < safeMin ? FishingUiTheme.OVERLAY_ACCENT : FishingUiTheme.OVERLAY_PRIMARY));
        graphics.fill(markerX - 1, tensionY - 2, markerX + 2, tensionY + 6, markerColor);

        graphics.text(minecraft.font, "포획 진척", barX, progressY - 11, FishingUiTheme.OVERLAY_PRIMARY, false);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FishingUiTheme.SLIDER, barX, progressY, 0, 0, 190, 4, 190, 4);
        int progressWidth = Math.max(
                0,
                Math.min(BAR_INNER_WIDTH, Math.round(BAR_INNER_WIDTH * ClientFishingState.progress()))
        );
        graphics.fill(barX + 1, progressY, barX + 1 + progressWidth, progressY + 4, 0xFF7EE28A);
    }

    private static void renderRecentCatch(GuiGraphicsExtractor graphics, Minecraft minecraft, int width, int height) {
        RecentCatchPresentation recent = ClientFishingState.recentCatch();
        if (recent == null) return;

        CatchEntry catchEntry = recent.catchEntry();
        FishSpecies species = FishCatalog.byId(catchEntry.speciesId());
        int cardX = width / 2 - 100;
        int cardY = height / 2 - 50;
        FishingUiTheme.drawPanel(graphics, cardX, cardY, 2, 1);

        String title = recent.locationCompleted()
                ? "지역 도감 완성!"
                : (recent.firstDiscovery()
                        ? "새 어종 발견!"
                        : (recent.personalBest()
                                ? "개인 최고기록!"
                                : (recent.sizeGrade() == FishSizeGrade.MONSTER ? "괴물급 어획!" : "어획 성공")));
        int titleColor = recent.locationCompleted() || recent.firstDiscovery() || recent.personalBest()
                ? FishingUiTheme.MONEY
                : FishingUiTheme.TEXT_PRIMARY;

        graphics.centeredText(minecraft.font, title, width / 2, cardY + 8, titleColor);
        graphics.centeredText(
                minecraft.font,
                species.displayName(),
                width / 2,
                cardY + 24,
                FishingUiTheme.rarityColor(species.rarity())
        );
        graphics.centeredText(
                minecraft.font,
                species.rarity().displayName() + " · " + recent.sizeGrade().displayName(),
                width / 2,
                cardY + 38,
                FishingUiTheme.sizeGradeColor(recent.sizeGrade())
        );
        graphics.centeredText(
                minecraft.font,
                String.format("%.2f kg   ·   %.1f cm", catchEntry.weightKg(), catchEntry.lengthCm()),
                width / 2,
                cardY + 54,
                FishingUiTheme.TEXT_PRIMARY
        );
        String valueText = catchEntry.value() + " C";
        if (recent.collectionRewardCoins() > 0) valueText += "   ·   도감 +" + recent.collectionRewardCoins() + " C";
        graphics.centeredText(minecraft.font, valueText, width / 2, cardY + 69, FishingUiTheme.MONEY);

        String highlight = recent.highlightText();
        graphics.centeredText(
                minecraft.font,
                highlight.isBlank() ? "J 도감에서 기록 확인" : highlight,
                width / 2,
                cardY + 84,
                highlight.isBlank() ? FishingUiTheme.TEXT_SECONDARY : FishingUiTheme.SUCCESS
        );
    }

    private static String tensionHint(float tension, float safeMin, float safeMax) {
        if (tension < safeMin) return "우클릭을 눌러 감기";
        if (tension > safeMax) return "우클릭을 떼어 줄 풀기";
        return "안정 구간 · 리듬 유지";
    }

    private static int hintColor(float tension, float safeMin, float safeMax) {
        if (tension >= ReelMath.BREAK_TENSION) return FishingUiTheme.OVERLAY_DANGER;
        if (tension > safeMax) return FishingUiTheme.OVERLAY_WARNING;
        if (tension < safeMin) return FishingUiTheme.OVERLAY_ACCENT;
        return FishingUiTheme.OVERLAY_SUCCESS;
    }
}
