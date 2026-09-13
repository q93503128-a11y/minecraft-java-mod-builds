package dev.moonseungjun.fishinggame.client;

import java.util.List;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishRarity;
import dev.moonseungjun.fishinggame.fishing.FishSpecies;
import dev.moonseungjun.fishinggame.fishing.FishingLocation;
import dev.moonseungjun.fishinggame.profile.FishRecord;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class BestiaryScreen extends Screen {
    private static final Identifier PANEL = FishingGameMod.id("textures/gui/kenney/grey_panel.png");
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
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        PANEL,
                        panelX + col * 100,
                        panelY + row * 100,
                        0, 0, 100, 100, 100, 100
                );
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int discovered = ClientFishingState.records().size();
        graphics.centeredText(
                font,
                "물고기 도감  " + discovered + "/" + FishCatalog.all().size(),
                panelX + PANEL_WIDTH / 2,
                panelY + 13,
                0xFFFFFFFF
        );
        graphics.centeredText(
                font,
                "잡은 기록은 판매 후에도 남습니다",
                panelX + PANEL_WIDTH / 2,
                panelY + 28,
                0xFFBFC7CE
        );

        FishingLocation[] locations = FishingLocation.values();
        for (int i = 0; i < locations.length; i++) {
            renderLocationColumn(graphics, locations[i], panelX + 10 + i * COLUMN_WIDTH, panelY + 48);
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
        long found = species.stream().filter(fish -> recordFor(fish.id()) != null).count();

        graphics.text(font, location.displayName(), x, y, 0xFFFFFFFF, true);
        graphics.text(font, "수집 " + found + "/" + species.size(), x, y + 13, 0xFFB8FFCF, false);

        int rowY = y + 31;
        for (FishSpecies fish : species) {
            FishRecord record = recordFor(fish.id());
            if (record == null) {
                graphics.text(font, "???", x, rowY, 0xFF8D959D, false);
                graphics.text(font, "미발견", x, rowY + 11, 0xFF737B83, false);
            } else {
                graphics.text(
                        font,
                        fish.displayName() + "  x" + record.caughtCount(),
                        x,
                        rowY,
                        rarityColor(fish.rarity()),
                        false
                );
                graphics.text(
                        font,
                        String.format("%.2fkg / %.1fcm", record.bestWeightKg(), record.bestLengthCm()),
                        x,
                        rowY + 11,
                        0xFFD7DCE1,
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

    private static int rarityColor(FishRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0xFFFFFFFF;
            case UNCOMMON -> 0xFF8EF3A0;
            case RARE -> 0xFF86C5FF;
            case EPIC -> 0xFFC697FF;
            case LEGENDARY -> 0xFFFFCF66;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
