package dev.moonseungjun.fishinggame.client;

import java.util.List;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishRarity;
import dev.moonseungjun.fishinggame.fishing.FishSpecies;
import dev.moonseungjun.fishinggame.network.BuyRodPayload;
import dev.moonseungjun.fishinggame.network.SellAllPayload;
import dev.moonseungjun.fishinggame.profile.CatchEntry;
import dev.moonseungjun.fishinggame.profile.PlayerFishingProfile;
import dev.moonseungjun.fishinggame.progression.FishingRods;
import dev.moonseungjun.fishinggame.progression.RodDefinition;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CatchBagScreen extends Screen {
    private static final Identifier PANEL = FishingGameMod.id("textures/gui/kenney/grey_panel.png");
    private int panelX;
    private int panelY;
    private KenneyButton sellButton;
    private KenneyButton upgradeButton;

    public CatchBagScreen() {
        super(Component.literal("어획 가방"));
    }

    @Override
    protected void init() {
        panelX = (width - 400) / 2;
        panelY = (height - 300) / 2;
        sellButton = new KenneyButton(panelX + 5, panelY + 243, Component.literal("모두 판매"), () -> {
            if (ClientPlayNetworking.canSend(SellAllPayload.TYPE)) {
                ClientPlayNetworking.send(new SellAllPayload(true));
            }
        });
        upgradeButton = new KenneyButton(panelX + 205, panelY + 243, Component.literal("낚싯대 강화"), () -> {
            RodDefinition next = FishingRods.nextAfter(ClientFishingState.rodTier());
            if (next != null && ClientPlayNetworking.canSend(BuyRodPayload.TYPE)) {
                ClientPlayNetworking.send(new BuyRodPayload(next.tier()));
            }
        });
        addRenderableWidget(sellButton);
        addRenderableWidget(upgradeButton);
    }

    @Override
    protected void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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

        List<CatchEntry> catches = ClientFishingState.catches();
        RodDefinition current = FishingRods.byTier(ClientFishingState.rodTier());
        RodDefinition next = FishingRods.nextAfter(ClientFishingState.rodTier());

        graphics.centeredText(font, "어획 가방", panelX + 200, panelY + 14, 0xFFFFFFFF);
        graphics.text(font, "코인  " + ClientFishingState.coins(), panelX + 18, panelY + 34, 0xFFFFD86B, false);
        graphics.text(font, "보관  " + catches.size() + "/" + PlayerFishingProfile.BAG_CAPACITY, panelX + 118, panelY + 34, 0xFFFFFFFF, false);
        graphics.text(font, "예상 판매가  " + catches.stream().mapToInt(CatchEntry::value).sum(), panelX + 218, panelY + 34, 0xFFB8FFCF, false);

        int start = Math.max(0, catches.size() - 10);
        int line = 0;
        for (int i = catches.size() - 1; i >= start; i--) {
            CatchEntry entry = catches.get(i);
            FishSpecies species = FishCatalog.byId(entry.speciesId());
            int color = rarityColor(species.rarity());
            int y = panelY + 58 + line * 16;
            graphics.text(font, species.displayName(), panelX + 18, y, color, false);
            graphics.text(font, String.format("%.2f kg  %.1f cm", entry.weightKg(), entry.lengthCm()), panelX + 100, y, 0xFFE3E3E3, false);
            graphics.text(font, entry.value() + " C", panelX + 210, y, 0xFFFFD86B, false);
            line++;
        }
        if (catches.isEmpty()) {
            graphics.text(font, "아직 잡은 물고기가 없습니다.", panelX + 18, panelY + 74, 0xFFD0D0D0, false);
        } else if (catches.size() > 10) {
            graphics.text(font, "최근 10마리 표시 · 총 " + catches.size() + "마리", panelX + 18, panelY + 222, 0xFFB8B8B8, false);
        }

        int rodX = panelX + 280;
        graphics.text(font, "현재 낚싯대", rodX, panelY + 60, 0xFFFFFFFF, true);
        graphics.text(font, current.displayName(), rodX, panelY + 78, 0xFFB9E5FF, false);
        graphics.text(font, "힘  x" + String.format("%.2f", current.strength()), rodX, panelY + 98, 0xFFD8D8D8, false);
        graphics.text(font, "제어  +" + Math.round(current.controlBonus() * 100) + "%", rodX, panelY + 114, 0xFFD8D8D8, false);
        graphics.text(font, "행운  +" + Math.round(current.luck() * 100) + "%", rodX, panelY + 130, 0xFFD8D8D8, false);
        if (next != null) {
            graphics.text(font, "다음: " + next.displayName(), rodX, panelY + 158, 0xFFFFFFFF, false);
            graphics.text(font, next.price() + " 코인", rodX, panelY + 176, ClientFishingState.coins() >= next.price() ? 0xFFB8FFCF : 0xFFFF9C9C, false);
            upgradeButton.active = true;
        } else {
            graphics.text(font, "최고 등급 낚싯대", rodX, panelY + 158, 0xFFB8FFCF, false);
            upgradeButton.active = false;
        }
        sellButton.active = !catches.isEmpty();
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
