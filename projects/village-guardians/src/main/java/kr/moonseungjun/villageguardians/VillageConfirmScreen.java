package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

public final class VillageConfirmScreen extends Screen {
    private static final int OVERLAY = 0xD9080B10;
    private static final int PANEL = 0xFF111A23;
    private static final int PANEL_SOFT = 0xFF1B2834;
    private static final int BORDER = 0xFF61788D;
    private static final int GOLD = 0xFFFFC85A;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB7C5D1;

    private final Screen parent;
    private final String action;
    private final String label;
    private final String detail;

    public VillageConfirmScreen(Screen parent, String action, String label, String detail) {
        super(Component.literal("동작 확인"));
        this.parent = parent;
        this.action = action;
        this.label = label;
        this.detail = detail;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(430, Math.max(270, width - 30));
        int panelHeight = Math.min(230, Math.max(176, height - 30));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int buttonY = top + panelHeight - 36;
        int buttonWidth = Math.max(88, (panelWidth - 44) / 2);

        addRenderableWidget(Button.builder(
                        Component.literal("확인하고 실행"),
                        button -> confirm())
                .bounds(left + 14, buttonY, buttonWidth, 22)
                .build());
        addRenderableWidget(Button.builder(
                        Component.literal("취소"),
                        button -> returnToParent())
                .bounds(left + panelWidth - 14 - buttonWidth, buttonY, buttonWidth, 22)
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(430, Math.max(270, width - 30));
        int panelHeight = Math.min(230, Math.max(176, height - 30));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left - 2, top - 2, right + 2, bottom + 2, BORDER);
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, left + 5, bottom, GOLD);
        graphics.text(font, "실행 전 확인", left + 18, top + 15, GOLD, false);
        graphics.text(font, label, left + 18, top + 34, TEXT, false);

        int detailTop = top + 55;
        int detailBottom = bottom - 50;
        graphics.fill(left + 14, detailTop, right - 14, detailBottom, PANEL_SOFT);
        List<FormattedCharSequence> lines = font.split(
                Component.literal(detail), Math.max(100, panelWidth - 54));
        int y = detailTop + 11;
        for (FormattedCharSequence line : lines) {
            if (y > detailBottom - 12) {
                break;
            }
            graphics.text(font, line, left + 27, y, MUTED, false);
            y += 12;
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void confirm() {
        ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        if (minecraft != null) {
            minecraft.gui.setScreen(null);
        }
    }

    private void returnToParent() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        returnToParent();
    }
}
