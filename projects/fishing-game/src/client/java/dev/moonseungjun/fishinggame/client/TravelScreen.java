package dev.moonseungjun.fishinggame.client;

import dev.moonseungjun.fishinggame.fishing.CollectionRewards;
import dev.moonseungjun.fishinggame.fishing.FishingLocation;
import dev.moonseungjun.fishinggame.network.TravelRequestPayload;
import dev.moonseungjun.fishinggame.progression.FishingRods;
import dev.moonseungjun.fishinggame.ui.FishingUiLayout;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TravelScreen extends Screen {
    private static final int ROW_HEIGHT = 50;
    private static final int BUTTON_WIDTH = 82;
    private static final int BUTTON_HEIGHT = 28;

    private final KenneyButton[] travelButtons = new KenneyButton[FishingLocation.values().length];
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public TravelScreen() {
        super(Component.literal("낚시터 이동"));
    }

    @Override
    protected void init() {
        FishingUiLayout.Size panel = FishingUiLayout.travel(width, height);
        panelWidth = panel.width();
        panelHeight = panel.height();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        FishingLocation[] locations = FishingLocation.values();
        for (int i = 0; i < locations.length; i++) {
            FishingLocation location = locations[i];
            boolean current = location.displayName().equals(ClientFishingState.locationName());
            boolean unlocked = ClientFishingState.rodTier() >= location.minRodTier();
            Component label = Component.literal(current ? "현재" : (unlocked ? "이동" : "잠김"));
            int rowY = panelY + 43 + i * ROW_HEIGHT;
            KenneyButton button = new KenneyButton(
                    panelX + panelWidth - BUTTON_WIDTH - 12,
                    rowY + 7,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    label,
                    () -> {
                        if (ClientPlayNetworking.canSend(TravelRequestPayload.TYPE)) {
                            ClientPlayNetworking.send(new TravelRequestPayload(location.ordinal()));
                        }
                        minecraft.gui.setScreen(null);
                    }
            );
            button.active = !current && unlocked;
            travelButtons[i] = button;
            addRenderableWidget(button);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FishingUiTheme.drawPanelPixels(graphics, panelX, panelY, panelWidth, panelHeight);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FishingUiTheme.drawHeader(
                graphics,
                font,
                "낚시터 이동",
                "낚싯대 성장으로 새 수역을 엽니다",
                panelX,
                panelY,
                panelWidth
        );

        FishingLocation[] locations = FishingLocation.values();
        for (int i = 0; i < locations.length; i++) {
            FishingLocation location = locations[i];
            int rowY = panelY + 43 + i * ROW_HEIGHT;
            int total = CollectionRewards.speciesCount(location);
            int found = CollectionRewards.discoveredCount(ClientFishingState.records(), location);
            boolean complete = found >= total && total > 0;
            boolean current = location.displayName().equals(ClientFishingState.locationName());
            boolean unlocked = ClientFishingState.rodTier() >= location.minRodTier();

            if (i > 0) {
                graphics.fill(panelX + 12, rowY - 2, panelX + panelWidth - 12, rowY - 1, FishingUiTheme.BORDER);
            }
            FishingUiTheme.drawSectionTitle(graphics, font, location.displayName(), panelX + 14, rowY + 3);
            graphics.text(
                    font,
                    "수집 " + found + "/" + total + (complete ? " · 완성" : " · +" + CollectionRewards.locationCompletionReward(location) + " C"),
                    panelX + 20,
                    rowY + 18,
                    complete ? FishingUiTheme.MONEY : FishingUiTheme.ACCENT,
                    false
            );

            String requirement;
            int requirementColor;
            if (current) {
                requirement = "현재 낚시 중";
                requirementColor = FishingUiTheme.SUCCESS;
            } else if (location.minRodTier() == 0) {
                requirement = "처음부터 이용 가능";
                requirementColor = FishingUiTheme.TEXT_SECONDARY;
            } else {
                String required = FishingRods.byTier(location.minRodTier()).displayName();
                requirement = required + (unlocked ? " 보유" : " 필요");
                requirementColor = unlocked ? FishingUiTheme.SUCCESS : FishingUiTheme.DANGER;
            }
            graphics.text(font, requirement, panelX + 20, rowY + 32, requirementColor, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
