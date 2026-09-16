package dev.moonseungjun.fishinggame.client;

import java.util.List;

import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishSizeGrade;
import dev.moonseungjun.fishinggame.fishing.FishSpecies;
import dev.moonseungjun.fishinggame.network.BuyRodPayload;
import dev.moonseungjun.fishinggame.network.RebirthPayload;
import dev.moonseungjun.fishinggame.network.SellAllPayload;
import dev.moonseungjun.fishinggame.profile.CatchEntry;
import dev.moonseungjun.fishinggame.profile.FishingPrestige;
import dev.moonseungjun.fishinggame.profile.PlayerFishingProfile;
import dev.moonseungjun.fishinggame.progression.FishingRods;
import dev.moonseungjun.fishinggame.progression.RodDefinition;
import dev.moonseungjun.fishinggame.ui.FishingUiLayout;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CatchBagScreen extends Screen {
    private static final int MAX_VISIBLE_CATCHES = 6;
    private static final int BUTTON_HEIGHT = 28;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int dividerX;
    private KenneyButton sellButton;
    private KenneyButton progressionButton;

    public CatchBagScreen() {
        super(Component.literal("어획 가방"));
    }

    @Override
    protected void init() {
        FishingUiLayout.Size panel = FishingUiLayout.catchBag(width, height);
        panelWidth = panel.width();
        panelHeight = panel.height();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        dividerX = panelX + Math.round(panelWidth * 0.66f);

        int buttonGap = 8;
        int buttonMargin = 10;
        int buttonWidth = (panelWidth - buttonMargin * 2 - buttonGap) / 2;
        int buttonY = panelY + panelHeight - BUTTON_HEIGHT - 7;

        sellButton = new KenneyButton(
                panelX + buttonMargin,
                buttonY,
                buttonWidth,
                BUTTON_HEIGHT,
                Component.literal("모두 판매"),
                () -> {
                    if (ClientPlayNetworking.canSend(SellAllPayload.TYPE)) {
                        ClientPlayNetworking.send(new SellAllPayload(true));
                    }
                }
        );
        progressionButton = new KenneyButton(
                panelX + buttonMargin + buttonWidth + buttonGap,
                buttonY,
                buttonWidth,
                BUTTON_HEIGHT,
                Component.literal("낚싯대 강화"),
                () -> {
                    RodDefinition next = FishingRods.nextAfter(ClientFishingState.rodTier());
                    if (next != null) {
                        if (ClientPlayNetworking.canSend(BuyRodPayload.TYPE)) {
                            ClientPlayNetworking.send(new BuyRodPayload(next.tier()));
                        }
                    } else if (ClientPlayNetworking.canSend(RebirthPayload.TYPE)) {
                        ClientPlayNetworking.send(new RebirthPayload(true));
                    }
                }
        );
        addRenderableWidget(sellButton);
        addRenderableWidget(progressionButton);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FishingUiTheme.drawPanelPixels(graphics, panelX, panelY, panelWidth, panelHeight);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        List<CatchEntry> catches = ClientFishingState.catches();
        RodDefinition current = FishingRods.byTier(ClientFishingState.rodTier());
        RodDefinition next = FishingRods.nextAfter(ClientFishingState.rodTier());
        int coins = ClientFishingState.coins();
        int rebirths = ClientFishingState.rebirths();
        int baseBagValue = catches.stream().mapToInt(CatchEntry::value).sum();
        int bagValue = FishingPrestige.boostedSaleValue(baseBagValue, rebirths);

        FishingUiTheme.drawHeader(
                graphics,
                font,
                "어획 가방",
                "판매 · 낚싯대 성장 · 환생",
                panelX,
                panelY,
                panelWidth
        );

        graphics.text(font, "코인 " + coins, panelX + 12, panelY + 42, FishingUiTheme.MONEY, false);
        graphics.text(
                font,
                "보관 " + catches.size() + "/" + PlayerFishingProfile.BAG_CAPACITY,
                panelX + 83,
                panelY + 42,
                FishingUiTheme.TEXT_PRIMARY,
                false
        );
        graphics.text(font, "판매 " + bagValue + " C", panelX + 151, panelY + 42, FishingUiTheme.SUCCESS, false);

        FishingUiTheme.drawSectionTitle(graphics, font, "어획 목록", panelX + 12, panelY + 59);
        FishingUiTheme.drawSectionTitle(graphics, font, "낚싯대", dividerX + 10, panelY + 59);
        graphics.fill(dividerX, panelY + 57, dividerX + 1, panelY + panelHeight - 42, FishingUiTheme.BORDER);

        int start = Math.max(0, catches.size() - MAX_VISIBLE_CATCHES);
        int line = 0;
        for (int i = catches.size() - 1; i >= start; i--) {
            CatchEntry entry = catches.get(i);
            FishSpecies species = FishCatalog.byId(entry.speciesId());
            FishSizeGrade grade = FishSizeGrade.classify(species, entry.weightGrams(), entry.lengthMm());
            int y = panelY + 76 + line * 15;
            graphics.text(font, species.displayName(), panelX + 12, y, FishingUiTheme.rarityColor(species.rarity()), false);
            graphics.text(font, grade.displayName(), panelX + 66, y, FishingUiTheme.sizeGradeColor(grade), false);
            graphics.text(
                    font,
                    String.format("%.1fkg·%.0fcm", entry.weightKg(), entry.lengthCm()),
                    panelX + 107,
                    y,
                    FishingUiTheme.TEXT_SECONDARY,
                    false
            );
            graphics.text(font, entry.value() + " C", dividerX - 38, y, FishingUiTheme.MONEY, false);
            line++;
        }

        if (catches.isEmpty()) {
            graphics.text(font, "아직 잡은 물고기가 없습니다.", panelX + 12, panelY + 78, FishingUiTheme.TEXT_SECONDARY, false);
        } else if (catches.size() > MAX_VISIBLE_CATCHES) {
            graphics.text(
                    font,
                    "최근 " + MAX_VISIBLE_CATCHES + "마리 · 총 " + catches.size() + "마리",
                    panelX + 12,
                    panelY + 170,
                    FishingUiTheme.TEXT_MUTED,
                    false
            );
        }

        int rodX = dividerX + 10;
        String rodName = current.displayName() + (rebirths > 0 ? " · R" + rebirths : "");
        graphics.text(font, rodName, rodX, panelY + 77, FishingUiTheme.ACCENT, false);
        graphics.text(
                font,
                "힘 x" + String.format("%.2f", current.strength()) + " · 제어 +" + Math.round(current.controlBonus() * 100) + "%",
                rodX,
                panelY + 92,
                FishingUiTheme.TEXT_PRIMARY,
                false
        );
        graphics.text(font, "행운 +" + Math.round(current.luck() * 100) + "%", rodX, panelY + 107, FishingUiTheme.TEXT_PRIMARY, false);
        graphics.text(
                font,
                "환생 " + rebirths + " · 판매 x" + String.format("%.2f", FishingPrestige.saleMultiplier(rebirths)),
                rodX,
                panelY + 122,
                rebirths > 0 ? FishingUiTheme.MONEY : FishingUiTheme.TEXT_MUTED,
                false
        );

        if (next != null) {
            boolean affordable = coins >= next.price();
            graphics.text(font, "다음 " + next.displayName(), rodX, panelY + 143, FishingUiTheme.TEXT_SECONDARY, false);
            graphics.text(
                    font,
                    next.price() + " 코인",
                    rodX,
                    panelY + 158,
                    affordable ? FishingUiTheme.SUCCESS : FishingUiTheme.DANGER,
                    false
            );
            progressionButton.setMessage(Component.literal("낚싯대 강화"));
            progressionButton.active = affordable;
        } else {
            int cost = FishingPrestige.nextCost(rebirths);
            boolean bagReady = catches.isEmpty();
            graphics.text(font, "다음 환생 " + cost + " C", rodX, panelY + 143, coins >= cost ? FishingUiTheme.SUCCESS : FishingUiTheme.DANGER, false);
            graphics.text(
                    font,
                    bagReady ? "판매 +30% 영구" : "가방을 먼저 판매",
                    rodX,
                    panelY + 158,
                    bagReady ? FishingUiTheme.MONEY : FishingUiTheme.WARNING,
                    false
            );
            progressionButton.setMessage(Component.literal("환생하기"));
            progressionButton.active = bagReady && coins >= cost;
        }
        sellButton.active = !catches.isEmpty();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
