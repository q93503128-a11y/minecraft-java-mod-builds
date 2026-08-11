package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Compact signal wheel that always stays inside the shared safe viewport. */
public final class VillageQuickChatSafeScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x26000000;
    private static final int TEXT = 0xFFF4F7F8;
    private static final int MUTED = 0xFFB6C0C5;
    private static final int ACCENT = 0xFF52D9C2;
    private static final int GOLD = 0xFFFFC65C;
    private static final int NODE = 0xD918323A;
    private static final int NODE_HOVER = 0xE5254650;
    private final List<Entry> entries = new ArrayList<>();

    public VillageQuickChatSafeScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        parse(payload);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int cx = safe.centerX();
        int radius = VillageUiSafeArea.clamp(Math.min(safe.width(), safe.height()) / 5, 52, 76);
        int cy = VillageUiSafeArea.clamp(safe.centerY() - 8,
                safe.top() + radius + 46, safe.bottom() - radius - 48);

        graphics.centeredText(font, "V  수호단 통신", cx, safe.top() + 8, TEXT);
        graphics.centeredText(font, "신호를 선택하면 즉시 전송됩니다", cx, safe.top() + 24, MUTED);
        drawDiamond(graphics, cx, cy, 18, 0xB90A151B);
        drawDiamondOutline(graphics, cx, cy, 18, ACCENT);
        graphics.centeredText(font, "V", cx, cy - 4, ACCENT);

        for (int i = 0; i < entries.size(); i++) {
            Point point = pointFor(i, entries.size(), cx, cy, radius);
            Entry entry = entries.get(i);
            boolean hovered = inside(mouseX, mouseY, point.x() - 31, point.y() - 31, 62, 62);
            drawDiamond(graphics, point.x(), point.y(), 29, hovered ? NODE_HOVER : NODE);
            drawDiamondOutline(graphics, point.x(), point.y(), 29, hovered ? GOLD : ACCENT);
            graphics.centeredText(font, Integer.toString(i + 1), point.x(), point.y() - 5,
                    hovered ? GOLD : TEXT);
            String label = fit(font, entry.title(), 126);
            int labelY = point.y() + (point.y() < cy ? -45 : 36);
            if (Math.abs(point.y() - cy) < 10) labelY = point.y() - 5;
            int labelX = point.x();
            if (point.x() < cx - 20) labelX = point.x() - 69;
            else if (point.x() > cx + 20) labelX = point.x() + 69;
            graphics.centeredText(font, label, labelX, labelY, hovered ? GOLD : TEXT);
            if (hovered && !entry.description().isBlank()) {
                graphics.centeredText(font, fit(font, entry.description(), Math.max(120, safe.width() - 80)),
                        cx, safe.bottom() - 31, MUTED);
            }
        }
        graphics.centeredText(font, "ESC 닫기", cx, safe.bottom() - 13, MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int cx = safe.centerX();
        int radius = VillageUiSafeArea.clamp(Math.min(safe.width(), safe.height()) / 5, 52, 76);
        int cy = VillageUiSafeArea.clamp(safe.centerY() - 8,
                safe.top() + radius + 46, safe.bottom() - radius - 48);
        for (int i = 0; i < entries.size(); i++) {
            Point point = pointFor(i, entries.size(), cx, cy, radius);
            if (inside(click.x(), click.y(), point.x() - 31, point.y() - 31, 62, 62)) {
                ClientPacketDistributor.sendToServer(
                        new VillageNetwork.VillageUiActionPayload(entries.get(i).action()));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void parse(VillageNetwork.OpenVillageUiPayload payload) {
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) {
            String[] p = labels[i].split("\\|", 2);
            entries.add(new Entry(actions[i], plain(p.length > 0 ? p[0] : actions[i]),
                    plain(p.length > 1 ? p[1] : "")));
        }
    }

    private static Point pointFor(int index, int count, int cx, int cy, int radius) {
        if (count <= 1) return new Point(cx, cy - radius);
        return switch (index & 3) {
            case 0 -> new Point(cx, cy - radius);
            case 1 -> new Point(cx + radius, cy);
            case 2 -> new Point(cx, cy + radius);
            default -> new Point(cx - radius, cy);
        };
    }

    private static String plain(String value) {
        String result = ChatFormatting.stripFormatting(value == null ? "" : value);
        return result == null ? "" : result;
    }

    private static String fit(Font font, String value, int maxWidth) {
        if (value == null || maxWidth <= 0) return "";
        if (font.width(value) <= maxWidth) return value;
        String suffix = "…";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + suffix) > maxWidth) end--;
        return value.substring(0, end) + suffix;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static void drawDiamond(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int half = radius - Math.abs(y);
            graphics.fill(cx - half, cy + y, cx + half + 1, cy + y + 1, color);
        }
    }

    public static void drawDiamondOutline(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int half = radius - Math.abs(y);
            graphics.fill(cx - half, cy + y, cx - half + 1, cy + y + 1, color);
            graphics.fill(cx + half, cy + y, cx + half + 1, cy + y + 1, color);
        }
    }

    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Entry(String action, String title, String description) {}
    private record Point(int x, int y) {}
}
