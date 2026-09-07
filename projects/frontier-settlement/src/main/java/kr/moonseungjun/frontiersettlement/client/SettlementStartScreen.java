package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.FoundSettlementRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** First-run entry UI. Founding authority remains entirely server-side. */
public final class SettlementStartScreen extends Screen {
    private int panelX, panelY, panelWidth, panelHeight;
    private boolean sending;

    public SettlementStartScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("공동 개척지 시작"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(540, Math.max(300, this.width - FrontierUiTheme.L));
        panelHeight = Math.min(278, Math.max(214, this.height - FrontierUiTheme.L));
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(FrontierUiTheme.S, (this.height - panelHeight) / 2);

        int buttonY = panelY + panelHeight - 32;
        int primaryWidth = Math.min(220, Math.max(150, panelWidth - 176));
        addRenderableWidget(Button.builder(Component.literal(sending ? "개척 요청 중…" : "현재 위치에 개척지 세우기"), button -> {
            if (sending) return;
            sending = true;
            button.active = false;
            ClientPacketDistributor.sendToServer(new FoundSettlementRequestPayload(true));
            this.minecraft.gui.setScreen(null);
        }).bounds(panelX + FrontierUiTheme.M, buttonY, primaryWidth, 20).build());

        int closeWidth = 44;
        int guideWidth = 72;
        int closeX = panelX + panelWidth - FrontierUiTheme.M - closeWidth;
        addRenderableWidget(Button.builder(Component.literal("가이드"),
                button -> this.minecraft.gui.setScreen(new SettlementGuideScreen(this, 0)))
                .bounds(closeX - FrontierUiTheme.XS - guideWidth, buttonY, guideWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("닫기"), button -> this.onClose())
                .bounds(closeX, buttonY, closeWidth, 20).build());
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        FrontierUiTheme.panel(graphics, panelX, panelY, panelWidth, panelHeight);
        int x = panelX + FrontierUiTheme.M;
        int y = panelY + FrontierUiTheme.M;
        graphics.text(this.font, Component.literal("FRONTIER SETTLEMENT"), x, y, FrontierUiTheme.ACCENT, true);
        graphics.text(this.font, Component.literal("이 월드에는 아직 공동 개척지가 없습니다."),
                x, y + 21, FrontierUiTheme.TEXT_PRIMARY, true);
        graphics.text(this.font, Component.literal("정착할 지면을 고른 뒤 한 번만 개척지를 세우면 됩니다."),
                x, y + 36, FrontierUiTheme.TEXT_SECONDARY, false);

        int infoY = y + 57;
        FrontierUiTheme.surface(graphics, x, infoY, panelWidth - FrontierUiTheme.M * 2, 78);
        int tx = x + FrontierUiTheme.M;
        graphics.text(this.font, Component.literal("시작 전에 알아둘 것"), tx, infoY + 9, FrontierUiTheme.ACCENT, true);
        graphics.text(this.font, Component.literal("표식과 54칸 공동 보급고가 실제 월드에 생성됩니다."),
                tx, infoY + 25, FrontierUiTheme.TEXT_PRIMARY, false);
        graphics.text(this.font, Component.literal("보급고의 실제 아이템이 건설·식량 자원입니다."),
                tx, infoY + 40, FrontierUiTheme.TEXT_SECONDARY, false);
        graphics.text(this.font, Component.literal("평평하고 머리 위가 빈 오버월드 지면을 권장합니다."),
                tx, infoY + 55, FrontierUiTheme.TEXT_SECONDARY, false);

        int goalY = infoY + 94;
        FrontierUiTheme.divider(graphics, x, goalY - FrontierUiTheme.S, panelWidth - FrontierUiTheme.M * 2);
        graphics.text(this.font, Component.literal("첫 성장  ·  주택 → 벌목소 → 농장 → 채석장 → 창고"),
                x, goalY, FrontierUiTheme.WARNING, false);
        graphics.text(this.font, Component.literal("시작 후 M 메뉴에서 등급·다음 성장·건설을 한 화면에서 확인합니다."),
                x, goalY + 16, FrontierUiTheme.TEXT_SECONDARY, false);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
