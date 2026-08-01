package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class VillageQuickChatScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x4A000000;
    private static final int PANEL = 0xE60A1118;
    private static final int CARD = 0xE6192935;
    private static final int CARD_HOVER = 0xF0284353;
    private static final int BORDER = 0xFF5A7284;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFC3D0D9;
    private static final int ACCENT = 0xFF45D8C0;

    private final String[] actions;
    private final String[] labels;

    public VillageQuickChatScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, layout.bottom(), ACCENT);
        graphics.text(font, "수호단 빠른 통신", layout.left() + 18, layout.top() + 13, TEXT, false);
        graphics.text(font, "누르는 즉시 전송됩니다 · C키로 열기 · ESC로 닫기",
                layout.left() + 18, layout.top() + 31, MUTED, false);

        int closeX = layout.right() - 34;
        boolean closeHovered = inside(mouseX, mouseY, closeX, layout.top() + 10, 24, 24);
        graphics.fill(closeX, layout.top() + 10, closeX + 24, layout.top() + 34,
                closeHovered ? 0xFF78343D : CARD_HOVER);
        graphics.centeredText(font, "×", closeX + 12, layout.top() + 18, closeHovered ? TEXT : MUTED);

        int count = Math.min(actions.length, labels.length);
        int columns = layout.width() >= 480 ? 2 : 1;
        int gap = 8;
        int padding = 16;
        int top = layout.top() + 54;
        int availableWidth = layout.width() - padding * 2;
        int cardWidth = Math.max(120, (availableWidth - gap * (columns - 1)) / columns);
        int rows = Math.max(1, (count + columns - 1) / columns);
        int availableHeight = layout.bottom() - top - 14;
        int cardHeight = Math.max(42, (availableHeight - gap * Math.max(0, rows - 1)) / rows);

        for (int index = 0; index < count; index++) {
            int x = layout.left() + padding + (index % columns) * (cardWidth + gap);
            int y = top + (index / columns) * (cardHeight + gap);
            boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, cardHeight);
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + cardHeight + 1,
                    hovered ? ACCENT : BORDER);
            graphics.fill(x, y, x + cardWidth, y + cardHeight, hovered ? CARD_HOVER : CARD);
            graphics.fill(x, y, x + 5, y + cardHeight, ACCENT);
            String[] parts = labelParts(labels[index]);
            graphics.text(font, parts[0], x + 15, y + 10, TEXT, false);
            if (cardHeight >= 45) {
                graphics.text(font, compact(parts[1], Math.max(14, cardWidth / 7)),
                        x + 15, y + 27, MUTED, false);
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 34, layout.top() + 10, 24, 24)) {
            onClose();
            return true;
        }

        int count = Math.min(actions.length, labels.length);
        int columns = layout.width() >= 480 ? 2 : 1;
        int gap = 8;
        int padding = 16;
        int top = layout.top() + 54;
        int availableWidth = layout.width() - padding * 2;
        int cardWidth = Math.max(120, (availableWidth - gap * (columns - 1)) / columns);
        int rows = Math.max(1, (count + columns - 1) / columns);
        int availableHeight = layout.bottom() - top - 14;
        int cardHeight = Math.max(42, (availableHeight - gap * Math.max(0, rows - 1)) / rows);
        for (int index = 0; index < count; index++) {
            int x = layout.left() + padding + (index % columns) * (cardWidth + gap);
            int y = top + (index / columns) * (cardHeight + gap);
            if (inside(click.x(), click.y(), x, y, cardWidth, cardHeight)) {
                ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(actions[index]));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private Layout layout() {
        int panelWidth = Math.min(700, Math.max(300, width - 24));
        int panelHeight = Math.min(220, Math.max(150, height - 24));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 4));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 4));
        return new Layout((width - panelWidth) / 2,
                Math.max(2, height - panelHeight - Math.max(12, height / 12)), panelWidth, panelHeight);
    }

    private String[] labelParts(String label) {
        String[] raw = label.split("\\|", 2);
        return new String[]{raw.length > 0 ? raw[0] : label,
                raw.length > 1 ? raw[1] : "즉시 전송"};
    }

    private String compact(String value, int max) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        return normalized.length() <= max ? normalized
                : normalized.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Layout(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }
    }
}
