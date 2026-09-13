package dev.moonseungjun.fishinggame.client;

import java.util.List;

import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishSizeGrade;
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
import net.minecraft.network.chat.Component;

public final class CatchBagScreen extends Screen {
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 300;
    private static final int MAX_VISIBLE_CATCHES = 9;

    private int panelX;
    private int panelY;
    private KenneyButton sellButton;
    private KenneyButton upgradeButton;

    public CatchBagScreen() {
        super(Component.literal("어획 가방"));
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FishingUiTheme.drawPanel(graphics, panelX, panelY, 4, 3);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        List<CatchEntry> catches = ClientFishingState.catches();
        RodDefinition current = FishingRods.byTier(ClientFishingState.rodTier());
        RodDefinition next = FishingRods.nextAfter(ClientFishingState.rodTier());
        int coins = ClientFishingState.coins();
        int bagValue = catches.stream().mapToInt(CatchEntry::value).sum();

        FishingUiTheme.drawHeader(
                graphics,
                font,
                "어획 가방",
                "잡은 물고기를 팔고 낚싯대를 성장시킵니다",
                panelX,
                panelY,
                PANEL_WIDTH
        );
        graphics.text(font, "코인  " + coins, panelX + 18, panelY + 50, FishingUiTheme.MONEY, false);
        graphics.text(
                font,
                "보관  " + catches.size() + "/" + PlayerFishingProfile.BAG_CAPACITY,
                panelX + 111,
                panelY + 50,
                FishingUiTheme.TEXT_PRIMARY,
                false
        );
        graphics.text(font, "판매가  " + bagValue + " C", panelX + 201, panelY + 50, FishingUiTheme.SUCCESS, false);

        FishingUiTheme.drawSectionTitle(graphics, font, "어획 목록", panelX + 18, panelY + 70);
        FishingUiTheme.drawSectionTitle(graphics, font, "낚싯대", panelX + 278, panelY + 70);
        graphics.fill(panelX + 268, panelY + 68, panelX + 269, panelY + 229, FishingUiTheme.BORDER);

        int start = Math.max(0, catches.size() - MAX_VISIBLE_CATCHES);
        int line = 0;
        for (int i = catches.size() - 1; i >= start; i--) {
            CatchEntry entry = catches.get(i);
            FishSpecies species = FishCatalog.byId(entry.speciesId());
            FishSizeGrade grade = FishSizeGrade.classify(species, entry.weightGrams(), entry.lengthMm());
            int y = panelY + 88 + line * 15;
            graphics.text(font, species.displayName(), panelX + 18, y, FishingUiTheme.rarityColor(species.rarity()), false);
            graphics.text(font, grade.displayName(), panelX + 90, y, FishingUiTheme.sizeGradeColor(grade), false);
            graphics.text(font, String.format("%.2fkg", entry.weightKg()), panelX + 132, y, FishingUiTheme.TEXT_PRIMARY, false);
            graphics.text(font, String.format("%.1fcm", entry.lengthCm()), panelX + 183, y, FishingUiTheme.TEXT_SECONDARY, false);
            graphics.text(font, entry.value() + " C", panelX + 226, y, FishingUiTheme.MONEY, false);
            line++;
        }

        if (catches.isEmpty()) {
            graphics.text(font, "아직 잡은 물고기가 없습니다.", panelX + 18, panelY + 91, FishingUiTheme.TEXT_SECONDARY, false);
        } else if (catches.size() > MAX_VISIBLE_CATCHES) {
            graphics.text(
                    font,
                    "최근 " + MAX_VISIBLE_CATCHES + "마리 · 총 " + catches.size() + "마리",
                    panelX + 18,
                    panelY + 224,
                    FishingUiTheme.TEXT_MUTED,
                    false
            );
        }

        int rodX = panelX + 278;
        graphics.text(font, "현재", rodX, panelY + 91, FishingUiTheme.TEXT_SECONDARY, false);
        graphics.text(font, current.displayName(), rodX, panelY + 106, FishingUiTheme.ACCENT, true);
        graphics.text(font, "힘  x" + String.format("%.2f", current.strength()), rodX, panelY + 128, FishingUiTheme.TEXT_PRIMARY, false);
        graphics.text(font, "제어  +" + Math.round(current.controlBonus() * 100) + "%", rodX, panelY + 145, FishingUiTheme.TEXT_PRIMARY, false);
        graphics.text(font, "행운  +" + Math.round(current.luck() * 100) + "%", rodX, panelY + 162, FishingUiTheme.TEXT_PRIMARY, false);

        if (next != null) {
            graphics.text(font, "다음", rodX, panelY + 188, FishingUiTheme.TEXT_SECONDARY, false);
            graphics.text(font, next.displayName(), rodX, panelY + 203, FishingUiTheme.TEXT_PRIMARY, false);
            boolean affordable = coins >= next.price();
            graphics.text(
                    font,
                    next.price() + " 코인",
                    rodX,
                    panelY + 220,
                    affordable ? FishingUiTheme.SUCCESS : FishingUiTheme.DANGER,
                    false
            );
            upgradeButton.active = affordable;
        } else {
            graphics.text(font, "최고 등급 낚싯대", rodX, panelY + 195, FishingUiTheme.SUCCESS, false);
            upgradeButton.active = false;
        }
        sellButton.active = !catches.isEmpty();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
