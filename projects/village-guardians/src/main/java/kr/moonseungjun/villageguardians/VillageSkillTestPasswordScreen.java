package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class VillageSkillTestPasswordScreen extends Screen {
    private static final int OVERLAY = 0x88000000;
    private static final int PANEL = 0xFFF1E9D7;
    private static final int BORDER = 0xFF6F5B43;
    private static final int TEXT = 0xFF211A14;
    private static final int MUTED = 0xFF62584D;
    private static final int ACCENT = 0xFF267E73;
    private static final int BUTTON = 0xFFE1C98F;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private String input = "";
    private String localMessage = "";

    public VillageSkillTestPasswordScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(260, Math.max(210, width - 30));
        int panelHeight = Math.min(258, Math.max(224, height - 24));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, BORDER);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left, top, left + 5, top + panelHeight, ACCENT);
        graphics.centeredText(font, payload.title(), width / 2, top + 13, TEXT);
        String message = localMessage.isBlank() ? payload.body() : localMessage;
        graphics.centeredText(font, fit(message, panelWidth - 28), width / 2, top + 34, MUTED);

        int boxGap = 8;
        int boxWidth = 34;
        int total = boxWidth * 4 + boxGap * 3;
        int boxLeft = width / 2 - total / 2;
        for (int index = 0; index < 4; index++) {
            int x = boxLeft + index * (boxWidth + boxGap);
            graphics.fill(x - 1, top + 54, x + boxWidth + 1, top + 84, BORDER);
            graphics.fill(x, top + 55, x + boxWidth, top + 83, 0xFFF8F2E4);
            String shown = index < input.length() ? "●" : "";
            graphics.centeredText(font, shown, x + boxWidth / 2, top + 65, TEXT);
        }

        for (int index = 0; index < 12; index++) {
            Bounds bounds = bounds(index, top);
            boolean hovered = inside(mouseX, mouseY, bounds.x(), bounds.y(), bounds.w(), bounds.h());
            graphics.fill(bounds.x() - 1, bounds.y() - 1,
                    bounds.x() + bounds.w() + 1, bounds.y() + bounds.h() + 1,
                    hovered ? ACCENT : BORDER);
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.w(), bounds.y() + bounds.h(),
                    hovered ? 0xFFFFE8B5 : BUTTON);
            graphics.centeredText(font, label(index), bounds.x() + bounds.w() / 2,
                    bounds.y() + 6, TEXT);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int panelHeight = Math.min(258, Math.max(224, height - 24));
        int top = (height - panelHeight) / 2;
        for (int index = 0; index < 12; index++) {
            Bounds bounds = bounds(index, top);
            if (!inside(click.x(), click.y(), bounds.x(), bounds.y(), bounds.w(), bounds.h())) continue;
            if (index <= 8) append(Integer.toString(index + 1));
            else if (index == 9) backspace();
            else if (index == 10) append("0");
            else submit();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void append(String digit) {
        if (input.length() < 4) input += digit;
        localMessage = "";
    }

    private void backspace() {
        if (!input.isEmpty()) input = input.substring(0, input.length() - 1);
        localMessage = "";
    }

    private void submit() {
        if (input.length() != 4) {
            localMessage = "네 자리 접근 코드를 모두 입력하세요.";
            return;
        }
        ClientPacketDistributor.sendToServer(
                new VillageNetwork.VillageUiActionPayload("skill_test_password:" + input));
        onClose();
    }

    private Bounds bounds(int index, int top) {
        int buttonWidth = 54;
        int buttonHeight = 24;
        int gap = 7;
        int totalWidth = buttonWidth * 3 + gap * 2;
        int left = width / 2 - totalWidth / 2;
        int row = index / 3;
        int column = index % 3;
        return new Bounds(left + column * (buttonWidth + gap), top + 101 + row * 31,
                buttonWidth, buttonHeight);
    }

    private String label(int index) {
        if (index <= 8) return Integer.toString(index + 1);
        if (index == 9) return "지우기";
        if (index == 10) return "0";
        return "입장";
    }

    private String fit(String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (font.width(normalized) <= maxWidth) return normalized;
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + "…") > maxWidth) end--;
        return normalized.substring(0, end) + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Bounds(int x, int y, int w, int h) {}
}
