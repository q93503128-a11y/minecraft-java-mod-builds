package dev.moonseungjun.fishinggame.client;

import java.util.List;

import dev.moonseungjun.fishinggame.fishing.CollectionRewards;
import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishingHotspot;
import dev.moonseungjun.fishinggame.fishing.FishSpecies;
import dev.moonseungjun.fishinggame.fishing.FishingLocation;
import dev.moonseungjun.fishinggame.profile.FishRecord;
import dev.moonseungjun.fishinggame.ui.FishingUiLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BestiaryScreen extends Screen {
    private static final int ROW_HEIGHT = 16;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public BestiaryScreen() {
        super(Component.literal("물고기 도감"));
    }

    @Override
    protected void init() {
        FishingUiLayout.Size panel = FishingUiLayout.bestiary(width, height);
        panelWidth = panel.width();
        panelHeight = panel.height();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FishingUiTheme.drawPanelPixels(graphics, panelX, panelY, panelWidth, panelHeight);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int discovered = ClientFishingState.records().size();
        FishingUiTheme.drawHeader(
                graphics,
                font,
                "물고기 도감  " + discovered + "/" + FishCatalog.all().size(),
                "미발견 어종은 추천 수역만 표시합니다",
                panelX,
                panelY,
                panelWidth
        );

        FishingLocation[] locations = FishingLocation.values();
        int contentX = panelX + 10;
        int contentWidth = panelWidth - 20;
        int columnWidth = contentWidth / locations.length;
        for (int i = 0; i < locations.length; i++) {
            int x = contentX + i * columnWidth;
            renderLocationColumn(graphics, locations[i], x, panelY + 43, columnWidth - 5);
            if (i < locations.length - 1) {
                int dividerX = contentX + (i + 1) * columnWidth - 3;
                graphics.fill(dividerX, panelY + 43, dividerX + 1, panelY + panelHeight - 10, FishingUiTheme.BORDER);
            }
        }
    }

    private void renderLocationColumn(
            GuiGraphicsExtractor graphics,
            FishingLocation location,
            int x,
            int y,
            int columnWidth
    ) {
        List<FishSpecies> species = FishCatalog.all().stream()
                .filter(fish -> fish.location() == location)
                .toList();
        int found = CollectionRewards.discoveredCount(ClientFishingState.records(), location);
        int total = species.size();
        boolean complete = found >= total && total > 0;

        FishingUiTheme.drawSectionTitle(graphics, font, location.displayName(), x, y);
        graphics.text(
                font,
                complete
                        ? "수집 " + found + "/" + total + " · 완성"
                        : "수집 " + found + "/" + total + " · +" + CollectionRewards.locationCompletionReward(location) + " C",
                x + 6,
                y + 14,
                complete ? FishingUiTheme.MONEY : FishingUiTheme.SUCCESS,
                false
        );

        int rowY = y + 31;
        for (FishSpecies fish : species) {
            FishRecord record = recordFor(fish.id());
            if (record == null) {
                FishingHotspot hotspot = FishingHotspot.primaryForSpecies(fish.id());
                graphics.text(
                        font,
                        "??? · " + hotspot.displayName(),
                        x + 6,
                        rowY,
                        FishingUiTheme.TEXT_MUTED,
                        false
                );
            } else {
                graphics.text(
                        font,
                        fish.displayName() + " x" + record.caughtCount() + " · " + String.format("%.1fkg", record.bestWeightKg()),
                        x + 6,
                        rowY,
                        FishingUiTheme.rarityColor(fish.rarity()),
                        false
                );
            }
            rowY += ROW_HEIGHT;
        }
    }

    private static FishRecord recordFor(String speciesId) {
        for (FishRecord record : ClientFishingState.records()) {
            if (record.speciesId().equals(speciesId)) return record;
        }
        return null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
