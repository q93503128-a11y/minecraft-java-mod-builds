package dev.moonseungjun.fishinggame.client;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishingLocation;
import dev.moonseungjun.fishinggame.network.TravelRequestPayload;
import dev.moonseungjun.fishinggame.progression.FishingRods;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class TravelScreen extends Screen {
    private static final Identifier PANEL = FishingGameMod.id("textures/gui/kenney/grey_panel.png");
    private final KenneyButton[] travelButtons = new KenneyButton[FishingLocation.values().length];
    private int panelX;
    private int panelY;

    public TravelScreen() {
        super(Component.literal("낚시터 이동"));
    }

    @Override
    protected void init() {
        panelX = (width - 400) / 2;
        panelY = (height - 280) / 2;

        FishingLocation[] locations = FishingLocation.values();
        for (int i = 0; i < locations.length; i++) {
            FishingLocation location = locations[i];
            boolean current = location.displayName().equals(ClientFishingState.locationName());
            boolean unlocked = ClientFishingState.rodTier() >= location.minRodTier();
            Component label = Component.literal(current ? "현재 위치" : (unlocked ? "이동" : "잠김"));
            KenneyButton button = new KenneyButton(panelX + 202, panelY + 48 + i * 70, label, () -> {
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
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL, panelX + col * 100, panelY + row * 100, 0, 0, 100, 100, 100, 100);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, "낚시터 이동", panelX + 200, panelY + 15, 0xFFFFFFFF);
        graphics.text(font, "낚싯대를 성장시키면 더 먼 바다가 열립니다.", panelX + 18, panelY + 31, 0xFFD8D8D8, false);

        FishingLocation[] locations = FishingLocation.values();
        for (int i = 0; i < locations.length; i++) {
            FishingLocation location = locations[i];
            int y = panelY + 53 + i * 70;
            long speciesCount = FishCatalog.all().stream().filter(species -> species.location() == location).count();
            boolean current = location.displayName().equals(ClientFishingState.locationName());
            boolean unlocked = ClientFishingState.rodTier() >= location.minRodTier();

            graphics.text(font, location.displayName(), panelX + 18, y, current ? 0xFFB8FFCF : 0xFFFFFFFF, true);
            graphics.text(font, speciesCount + "종의 물고기", panelX + 18, y + 17, 0xFFB9E5FF, false);
            if (location.minRodTier() == 0) {
                graphics.text(font, "처음부터 이용 가능", panelX + 18, y + 34, 0xFFD0D0D0, false);
            } else {
                String required = FishingRods.byTier(location.minRodTier()).displayName();
                graphics.text(font, required + (unlocked ? " 보유" : " 필요"), panelX + 18, y + 34,
                        unlocked ? 0xFFB8FFCF : 0xFFFF9C9C, false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
