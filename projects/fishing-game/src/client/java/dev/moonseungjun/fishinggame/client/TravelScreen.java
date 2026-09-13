package dev.moonseungjun.fishinggame.client;

import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishingLocation;
import dev.moonseungjun.fishinggame.network.TravelRequestPayload;
import dev.moonseungjun.fishinggame.progression.FishingRods;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TravelScreen extends Screen {
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 300;
    private final KenneyButton[] travelButtons = new KenneyButton[FishingLocation.values().length];
    private int panelX;
    private int panelY;

    public TravelScreen() {
        super(Component.literal("낚시터 이동"));
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;

        FishingLocation[] locations = FishingLocation.values();
        for (int i = 0; i < locations.length; i++) {
            FishingLocation location = locations[i];
            boolean current = location.displayName().equals(ClientFishingState.locationName());
            boolean unlocked = ClientFishingState.rodTier() >= location.minRodTier();
            Component label = Component.literal(current ? "현재 위치" : (unlocked ? "이동" : "잠김"));
            KenneyButton button = new KenneyButton(panelX + 202, panelY + 57 + i * 70, label, () -> {
                if (ClientPlayNetworking.canSend(TravelRequestPayload.TYPE)) {
                    ClientPlayNetworking.send(new TravelRequestPayload(location.ordinal()));
                }
                minecraft.gui.setScreen(null);
            });
            button.active = !current && unlocked;
            travelButtons[i] = button;
            addRenderableWidget(button);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FishingUiTheme.drawPanel(graphics, panelX, panelY, 4, 3);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FishingUiTheme.drawHeader(
                graphics,
                font,
                "낚시터 이동",
                "낚싯대를 성장시키면 더 먼 바다가 열립니다",
                panelX,
                panelY,
                PANEL_WIDTH
        );

        FishingLocation[] locations = FishingLocation.values();
        for (int i = 0; i < locations.length; i++) {
            FishingLocation location = locations[i];
            int rowY = panelY + 58 + i * 70;
            long speciesCount = FishCatalog.all().stream().filter(species -> species.location() == location).count();
            boolean current = location.displayName().equals(ClientFishingState.locationName());
            boolean unlocked = ClientFishingState.rodTier() >= location.minRodTier();

            if (i > 0) {
                graphics.fill(panelX + 16, rowY - 7, panelX + PANEL_WIDTH - 16, rowY - 6, FishingUiTheme.BORDER);
            }
            FishingUiTheme.drawSectionTitle(graphics, font, location.displayName(), panelX + 18, rowY);
            graphics.text(font, speciesCount + "종의 물고기", panelX + 25, rowY + 18, FishingUiTheme.ACCENT, false);

            if (current) {
                graphics.text(font, "현재 낚시 중인 지역", panelX + 25, rowY + 35, FishingUiTheme.SUCCESS, false);
            } else if (location.minRodTier() == 0) {
                graphics.text(font, "처음부터 이용 가능", panelX + 25, rowY + 35, FishingUiTheme.TEXT_SECONDARY, false);
            } else {
                String required = FishingRods.byTier(location.minRodTier()).displayName();
                graphics.text(
                        font,
                        required + (unlocked ? " 보유" : " 필요"),
                        panelX + 25,
                        rowY + 35,
                        unlocked ? FishingUiTheme.SUCCESS : FishingUiTheme.DANGER,
                        false
                );
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
