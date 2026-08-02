package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

/** Small, explicit success/failure window for purchases and facility actions. */
public final class VillageResultScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x72000000;
    private static final int PANEL = 0xFFF1E9D7;
    private static final int BORDER = 0xFF6F5B43;
    private static final int TEXT = 0xFF211A14;
    private static final int MUTED = 0xFF62584D;
    private static final int ACCENT = 0xFF267E73;
    private static final int BUTTON = 0xFFE1C98F;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String returnAction;

    public VillageResultScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        this.returnAction = actions.length == 0 ? "" : actions[0];
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(330, Math.max(190, width - 28));
        int panelHeight = Math.min(148, Math.max(104, height - 28));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, BORDER);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left, top, left + 5, top + panelHeight, ACCENT);
        graphics.centeredText(font, fit(payload.title(), panelWidth - 32), width / 2, top + 12, TEXT);

        List<FormattedCharSequence> lines = font.split(Component.literal(payload.body()), panelWidth - 30);
        int y = top + 35;
        int maxY = top + panelHeight - 38;
        for (FormattedCharSequence line : lines) {
            if (y > maxY) break;
            graphics.text(font, line, left + 16, y, MUTED, false);
            y += 12;
        }

        int buttonWidth = 64;
        int buttonHeight = 18;
        int buttonLeft = width / 2 - buttonWidth / 2;
        int buttonTop = top + panelHeight - 28;
        boolean hovered = inside(mouseX, mouseY, buttonLeft, buttonTop, buttonWidth, buttonHeight);
        graphics.fill(buttonLeft - 1, buttonTop - 1, buttonLeft + buttonWidth + 1,
                buttonTop + buttonHeight + 1, hovered ? ACCENT : BORDER);
        graphics.fill(buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + buttonHeight,
                hovered ? 0xFFFFE8B5 : BUTTON);
        graphics.centeredText(font, "확인", width / 2, buttonTop + 5, TEXT);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int panelHeight = Math.min(148, Math.max(104, height - 28));
        int top = (height - panelHeight) / 2;
        int buttonWidth = 64;
        int buttonHeight = 18;
        int buttonLeft = width / 2 - buttonWidth / 2;
        int buttonTop = top + panelHeight - 28;
        if (!inside(click.x(), click.y(), buttonLeft, buttonTop, buttonWidth, buttonHeight)) {
            return super.mouseClicked(click, doubled);
        }
        if (!returnAction.isBlank()) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(returnAction));
        }
        onClose();
        return true;
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
}
