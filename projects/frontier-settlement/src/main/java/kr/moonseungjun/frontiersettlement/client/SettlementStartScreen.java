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
        panelWidth = Math.min(520, Math.max(300, this.width - 16));
        panelHeight = Math.min(270, Math.max(220, this.height - 16));
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(8, (this.height - panelHeight) / 2);

        int buttonY = panelY + panelHeight - 34;
        int primaryWidth = Math.min(210, Math.max(150, panelWidth - 170));
        addRenderableWidget(Button.builder(Component.literal("현재 위치에 개척지 세우기"), button -> {
            if (sending) return;
            sending = true;
            button.active = false;
            ClientPacketDistributor.sendToServer(new FoundSettlementRequestPayload(true));
            this.minecraft.gui.setScreen(null);
        }).bounds(panelX + 14, buttonY, primaryWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("시작 방법"),
                button -> this.minecraft.gui.setScreen(new SettlementGuideScreen(this, 0)))
                .bounds(panelX + panelWidth - 142, buttonY, 78, 20).build());
        addRenderableWidget(Button.builder(Component.literal("닫기"), button -> this.onClose())
                .bounds(panelX + panelWidth - 58, buttonY, 44, 20).build());
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0121418);
        graphics.fill(panelX, panelY, panelX + 4, panelY + panelHeight, 0xFFD0A45C);
        graphics.fill(panelX + 4, panelY, panelX + panelWidth, panelY + 2, 0x704D412F);

        int x = panelX + 16, y = panelY + 14;
        graphics.text(this.font, Component.literal("FRONTIER SETTLEMENT"), x, y, 0xFFD0A45C, true);
        graphics.text(this.font, Component.literal("이 월드에는 아직 공동 개척지가 없습니다."), x, y + 20, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.literal("정착할 장소를 고른 뒤 아래 버튼으로 시작하세요."), x, y + 34, 0xFFD7D7D7, false);

        int boxY = y + 54;
        graphics.fill(x - 4, boxY - 5, panelX + panelWidth - 14, boxY + 66, 0x701F2328);
        graphics.text(this.font, Component.literal("• 표식과 공동 창고가 실제 월드에 생성됩니다."), x + 4, boxY, 0xFFE7E0D3, false);
        graphics.text(this.font, Component.literal("• 창고의 실제 아이템이 건설·식량 자원입니다."), x + 4, boxY + 15, 0xFFE7E0D3, false);
        graphics.text(this.font, Component.literal("• B에서 위치만 정하면 주민이 재료를 운반합니다."), x + 4, boxY + 30, 0xFFE7E0D3, false);
        graphics.text(this.font, Component.literal("• 평평하고 머리 위가 빈 오버월드 지면을 권장합니다."), x + 4, boxY + 45, 0xFFE7E0D3, false);
        graphics.text(this.font, Component.literal("첫 목표 · 주택 → 벌목소 → 농장 → 채석장 → 창고"), x, boxY + 77, 0xFFFFD58A, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
