package dev.moonseungjun.fishinggame.client;

import java.util.List;

import dev.moonseungjun.fishinggame.fishing.CollectionRewards;
import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishSpecies;
import dev.moonseungjun.fishinggame.fishing.FishingLocation;
import dev.moonseungjun.fishinggame.profile.FishRecord;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BestiaryScreen extends Screen {
    private static final int PANEL_WIDTH = 500;
    private static final int PANEL_HEIGHT = 300;
    private static final int COLUMN_WIDTH = 160;
    private static final int ROW_HEIGHT = 25;

    private int panelX;
    private int panelY;

    public BestiaryScreen() {
        super(Component.literal("물고기 도감"));
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FishingUiTheme.drawPanel(graphics, panelX, panelY, 5, 3);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int discovered = ClientFishingState.records().size();
        FishingUiTheme.drawHeader(
                graphics,
                font,
                "물고기 도감  " + discovered + "/" + FishCatalog.all().size(),
                "새 어종을 발견하면 코인 보너스 · 지역을 완성하면 추가 보너스",
                panelX,
                panelY,
                PANEL_WIDTH
        );

        FishingLocation[] locations = FishingLocation.values();
        for (int i = 0; i < locations.length; i++) {
            int x = panelX + 10 + i * COLUMN_WIDTH;
            renderLocationColumn(graphics, locations[i], x, panelY + 55);
            if (i < locations.length - 1) {
                int dividerX = x + COLUMN_WIDTH - 5;
                graphics.fill(dividerX, panelY + 55, dividerX + 1, panelY + 286, FishingUiTheme.BORDER);
            }
        }
    }

    private void renderLocationColumn(
            GuiGraphicsExtractor graphics,
            FishingLocation location,
            int x,
            int y
    ) {
        List<FishSpecies> species = FishCatalog.all().stream()
                .filter(fish -> fish.location() == location)
                .toList();
        int found = CollectionRewards.discoveredCount(ClientFishingState.records(), location);
        int total = species.size();
        int completionReward = CollectionRewards.locationCompletionReward(location);
        boolean complete = found >= total && total > 0;

        FishingUiTheme.drawSectionTitle(graphics, font, location.displayName(), x, y);
        graphics.text(
                font,
                "수집 " + found + "/" + total,
                x + 7,
                y + 16,
                complete ? FishingUiTheme.MONEY : FishingUiTheme.SUCCESS,
                false
        );
        graphics.text(
                font,
                complete ? "지역 도감 완성" : "완성 보너스 +" + completionReward + " C",
                x + 7,
                y + 29,
                complete ? FishingUiTheme.MONEY : FishingUiTheme.TEXT_SECONDARY,
                false
        );

        int rowY = y + 47;
        for (FishSpecies fish : species) {
            FishRecord record = recordFor(fish.id());
            if (record == null) {
                graphics.text(font, "???", x + 7, rowY, FishingUiTheme.TEXT_MUTED, false);
                graphics.text(
                        font,
                        "미발견 · 보너스 있음",
                        x + 7,
                        rowY + 11,
                        FishingUiTheme.TEXT_DISABLED,
                        false
                );
            } else {
                graphics.text(
                        font,
                        fish.displayName() + "  x" + record.caughtCount(),
                        x + 7,
                        rowY,
                        FishingUiTheme.rarityColor(fish.rarity()),
                        false
                );
                graphics.text(
                        font,
                        String.format("최고 %.2fkg / %.1fcm", record.bestWeightKg(), record.bestLengthCm()),
                        x + 7,
                        rowY + 11,
                        FishingUiTheme.TEXT_SECONDARY,
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
