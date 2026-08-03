package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class VillageQuickChatScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x72000000;
    private static final int PANEL = 0xE60A1118;
    private static final int CARD = 0xF01A2B36;
    private static final int CARD_HOVER = 0xF02B4656;
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
        int count = Math.min(actions.length, labels.length);
        int centerX = width / 2;
        int centerY = height / 2;
        int centerWidth = 56;
        int centerHeight = 18;
        int centerLeft = centerX - centerWidth / 2;
        int centerTop = centerY - centerHeight / 2;

        graphics.text(font, fit(VillageClientKeys.quickCommunicationKeyName()
                        + " 빠른 통신 · 선택 즉시 전송 · ESC 닫기", Math.max(1, width - 20)),
                10, 9, MUTED, false);
        boolean closeHovered = inside(mouseX, mouseY, centerLeft, centerTop, centerWidth, centerHeight);
        graphics.fill(centerLeft - 1, centerTop - 1, centerLeft + centerWidth + 1,
                centerTop + centerHeight + 1, closeHovered ? ACCENT : BORDER);
        graphics.fill(centerLeft, centerTop, centerLeft + centerWidth, centerTop + centerHeight,
                closeHovered ? CARD_HOVER : PANEL);
        graphics.centeredText(font, "닫기", centerX, centerTop + 5, closeHovered ? TEXT : MUTED);

        for (int index = 0; index < count; index++) {
            OptionBounds bounds = optionBounds(index, count, centerX, centerY);
            int edgeX = clamp(centerX, bounds.x(), bounds.x() + bounds.width());
            int edgeY = clamp(centerY, bounds.y(), bounds.y() + bounds.height());
            drawLine(graphics, centerX, centerY, edgeX, edgeY, 0xAA5A7284);
            boolean hovered = inside(mouseX, mouseY, bounds.x(), bounds.y(), bounds.width(), bounds.height());
            graphics.fill(bounds.x() - 1, bounds.y() - 1,
                    bounds.x() + bounds.width() + 1, bounds.y() + bounds.height() + 1,
                    hovered ? ACCENT : BORDER);
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(),
                    hovered ? CARD_HOVER : CARD);
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + 4, bounds.y() + bounds.height(), ACCENT);
            String[] parts = labelParts(labels[index]);
            graphics.centeredText(font, fit(parts[0], bounds.width() - 18),
                    bounds.x() + bounds.width() / 2, bounds.y() + 8, TEXT);
            graphics.centeredText(font, fit(parts[1], bounds.width() - 18),
                    bounds.x() + bounds.width() / 2, bounds.y() + 23, MUTED);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int centerX = width / 2;
        int centerY = height / 2;
        int centerWidth = 56;
        int centerHeight = 18;
        int centerLeft = centerX - centerWidth / 2;
        int centerTop = centerY - centerHeight / 2;
        if (inside(click.x(), click.y(), centerLeft, centerTop, centerWidth, centerHeight)) {
            onClose();
            return true;
        }
        int count = Math.min(actions.length, labels.length);
        for (int index = 0; index < count; index++) {
            OptionBounds bounds = optionBounds(index, count, centerX, centerY);
            if (inside(click.x(), click.y(), bounds.x(), bounds.y(), bounds.width(), bounds.height())) {
                ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(actions[index]));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private OptionBounds optionBounds(int index, int count, int centerX, int centerY) {
        int cardWidth = clamp(width / 4, 104, 172);
        int cardHeight = 40;
        if (count <= 4 && width >= 300 && height >= 230) {
            double radiusX = Math.min(190.0, Math.max(92.0, (width - cardWidth - 24) / 2.7));
            double radiusY = Math.min(126.0, Math.max(70.0, (height - cardHeight - 72) / 2.7));
            double angle = -Math.PI / 2.0 + index * Math.PI * 2.0 / Math.max(1, count);
            int x = (int) Math.round(centerX + Math.cos(angle) * radiusX - cardWidth / 2.0);
            int y = (int) Math.round(centerY + Math.sin(angle) * radiusY - cardHeight / 2.0);
            return new OptionBounds(clamp(x, 6, Math.max(6, width - cardWidth - 6)),
                    clamp(y, 28, Math.max(28, height - cardHeight - 6)), cardWidth, cardHeight);
        }
        int columns = width >= 250 ? 2 : 1;
        int gap = 6;
        int rows = Math.max(1, (count + columns - 1) / columns);
        int totalWidth = columns * cardWidth + (columns - 1) * gap;
        int x = Math.max(6, (width - totalWidth) / 2 + (index % columns) * (cardWidth + gap));
        int totalHeight = rows * cardHeight + (rows - 1) * gap;
        int y = Math.max(30, (height - totalHeight) / 2 + (index / columns) * (cardHeight + gap));
        return new OptionBounds(clamp(x, 6, Math.max(6, width - cardWidth - 6)),
                clamp(y, 28, Math.max(28, height - cardHeight - 6)), cardWidth, cardHeight);
    }

    private String[] labelParts(String label) {
        String[] raw = label.split("\\|", 2);
        return new String[]{raw.length > 0 ? raw[0] : label,
                raw.length > 1 ? raw[1] : "즉시 전송"};
    }

    private String fit(String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        String suffix = "…";
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + suffix) > maxWidth) end--;
        return normalized.substring(0, end) + suffix;
    }

    private void drawLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record OptionBounds(int x, int y, int width, int height) {}
}
