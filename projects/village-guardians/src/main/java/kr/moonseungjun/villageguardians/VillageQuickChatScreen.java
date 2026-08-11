package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Fast communication wheel that stays visually embedded in the world instead of opening a panel. */
public final class VillageQuickChatScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x34000000;
    private static final int INK = 0xFFF3F7F8;
    private static final int MUTED = 0xFFB2C0C8;
    private static final int ACCENT = 0xFF55E3C5;
    private static final int ACCENT_SOFT = 0xAA2E8878;
    private static final int TRACK = 0x885E7886;
    private static final int CORE = 0xD20B141B;

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

        graphics.centeredText(font, VillageClientKeys.quickCommunicationKeyName() + "  수호단 통신",
                centerX, Math.max(9, centerY - Math.min(116, height / 3) - 38), MUTED);

        drawDiamond(graphics, centerX, centerY, 18, 0xDD18252E);
        drawDiamondOutline(graphics, centerX, centerY, 18, TRACK);
        drawDiamond(graphics, centerX, centerY, 7, CORE);
        graphics.centeredText(font, "×", centerX, centerY - 4, MUTED);

        int hoveredIndex = -1;
        for (int index = 0; index < count; index++) {
            Node node = node(index, count, centerX, centerY);
            boolean hovered = insideDiamond(mouseX, mouseY, node.x(), node.y(), 26);
            if (hovered) hoveredIndex = index;
            drawLine(graphics, centerX, centerY, node.x(), node.y(), hovered ? ACCENT_SOFT : TRACK);
            drawDiamond(graphics, node.x(), node.y(), hovered ? 25 : 20,
                    hovered ? 0xE3295A55 : 0xD4132028);
            drawDiamondOutline(graphics, node.x(), node.y(), hovered ? 25 : 20,
                    hovered ? ACCENT : 0xCC6E8794);
            drawDiamond(graphics, node.x(), node.y(), hovered ? 7 : 5, hovered ? ACCENT : 0xFF7E939E);

            String[] parts = labelParts(labels[index]);
            int textY = node.y() + (node.y() < centerY ? -39 : 31);
            if (Math.abs(node.y() - centerY) < 28) textY = node.y() - 5;
            int textX = node.x();
            String title = fit(parts[0], Math.min(146, Math.max(70, width / 4)));
            graphics.centeredText(font, title, textX, textY, hovered ? INK : 0xFFD9E2E6);
        }

        String guide = hoveredIndex >= 0
                ? labelParts(labels[hoveredIndex])[1]
                : "신호를 선택하면 즉시 전송 · 중앙 클릭 또는 ESC 닫기";
        graphics.centeredText(font, fit(guide, Math.max(80, width - 40)), centerX,
                Math.min(height - 18, centerY + Math.min(116, height / 3) + 38),
                hoveredIndex >= 0 ? ACCENT : MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int centerX = width / 2;
        int centerY = height / 2;
        if (insideDiamond(click.x(), click.y(), centerX, centerY, 22)) {
            onClose();
            return true;
        }
        int count = Math.min(actions.length, labels.length);
        for (int index = 0; index < count; index++) {
            Node node = node(index, count, centerX, centerY);
            if (insideDiamond(click.x(), click.y(), node.x(), node.y(), 30)) {
                ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(actions[index]));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private Node node(int index, int count, int centerX, int centerY) {
        double angle = -Math.PI / 2.0 + index * Math.PI * 2.0 / Math.max(1, count);
        double radiusX = Math.min(168.0, Math.max(76.0, width * 0.24));
        double radiusY = Math.min(112.0, Math.max(62.0, height * 0.25));
        int x = (int) Math.round(centerX + Math.cos(angle) * radiusX);
        int y = (int) Math.round(centerY + Math.sin(angle) * radiusY);
        return new Node(clamp(x, 34, Math.max(34, width - 34)),
                clamp(y, 44, Math.max(44, height - 44)));
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

    static void drawDiamond(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = radius - Math.abs(dy);
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    static void drawDiamondOutline(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int i = 0; i < radius; i++) {
            graphics.fill(cx - i - 1, cy - radius + i, cx - i, cy - radius + i + 1, color);
            graphics.fill(cx + i, cy - radius + i, cx + i + 1, cy - radius + i + 1, color);
            graphics.fill(cx - i - 1, cy + radius - i, cx - i, cy + radius - i + 1, color);
            graphics.fill(cx + i, cy + radius - i, cx + i + 1, cy + radius - i + 1, color);
        }
    }

    static void drawLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
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

    private static boolean insideDiamond(double mx, double my, int cx, int cy, int radius) {
        return Math.abs(mx - cx) + Math.abs(my - cy) <= radius;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Node(int x, int y) {}
}
