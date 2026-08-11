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

/** Compact signal wheel with dedicated, non-overlapping label and description zones. */
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
        Layout layout = layout(safe);
        int hoveredIndex = -1;

        graphics.centeredText(font, "V  수호단 통신", layout.cx(), safe.top() + 5, TEXT);
        graphics.centeredText(font, "신호를 선택하면 즉시 전송됩니다", layout.cx(), safe.top() + 20, MUTED);
        graphics.fill(safe.left() + 18, safe.top() + 34, safe.right() - 18, safe.top() + 35, 0x705A747D);

        drawDiamond(graphics, layout.cx(), layout.cy(), 14, 0xB90A151B);
        drawDiamondOutline(graphics, layout.cx(), layout.cy(), 14, ACCENT);
        graphics.centeredText(font, "V", layout.cx(), layout.cy() - 4, ACCENT);

        for (int i = 0; i < entries.size(); i++) {
            Point point = pointFor(i, entries.size(), layout);
            Entry entry = entries.get(i);
            boolean hovered = insideDiamond(mouseX, mouseY, point.x(), point.y(), layout.nodeRadius() + 4);
            if (hovered) hoveredIndex = i;
            drawDiamond(graphics, point.x(), point.y(), layout.nodeRadius(), hovered ? NODE_HOVER : NODE);
            drawDiamondOutline(graphics, point.x(), point.y(), layout.nodeRadius(), hovered ? GOLD : ACCENT);
            graphics.centeredText(font, Integer.toString(i + 1), point.x(), point.y() - 5,
                    hovered ? GOLD : TEXT);
            drawSignalLabel(graphics, safe, layout, point, i, entry.title(), hovered);
        }

        int infoTop = safe.bottom() - 29;
        graphics.fill(safe.left() + 12, infoTop - 5, safe.right() - 12, infoTop - 4, 0x665A747D);
        if (hoveredIndex >= 0 && !entries.get(hoveredIndex).description().isBlank()) {
            String description = fit(font, entries.get(hoveredIndex).description(), Math.max(80, safe.width() - 105));
            graphics.centeredText(font, description, safe.centerX(), infoTop, MUTED);
        } else {
            graphics.centeredText(font, "신호 위에 커서를 올리면 상세 내용을 확인할 수 있습니다.",
                    safe.centerX(), infoTop, MUTED);
        }
        graphics.text(font, "ESC 닫기", safe.left() + 5, safe.bottom() - 11, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSignalLabel(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe, Layout layout,
                                 Point point, int index, String value, boolean hovered) {
        int color = hovered ? GOLD : TEXT;
        int maxSide = Math.max(55, Math.min(150, safe.width() / 3));
        String label = fit(font, value, maxSide);
        int direction = index & 3;
        if (direction == 0) {
            int y = Math.max(safe.top() + 39, point.y() - layout.nodeRadius() - 15);
            graphics.centeredText(font, label, point.x(), y, color);
        } else if (direction == 1) {
            int x = Math.min(safe.right() - font.width(label) - 4,
                    point.x() + layout.nodeRadius() + 12);
            graphics.text(font, label, x, point.y() - 4, color, false);
        } else if (direction == 2) {
            int y = Math.min(safe.bottom() - 48, point.y() + layout.nodeRadius() + 7);
            graphics.centeredText(font, label, point.x(), y, color);
        } else {
            int right = Math.max(safe.left() + font.width(label) + 4,
                    point.x() - layout.nodeRadius() - 12);
            graphics.text(font, label, right - font.width(label), point.y() - 4, color, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        Layout layout = layout(safe);
        for (int i = 0; i < entries.size(); i++) {
            Point point = pointFor(i, entries.size(), layout);
            if (insideDiamond(click.x(), click.y(), point.x(), point.y(), layout.nodeRadius() + 5)) {
                ClientPacketDistributor.sendToServer(
                        new VillageNetwork.VillageUiActionPayload(entries.get(i).action()));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private Layout layout(VillageUiSafeArea.Rect safe) {
        int nodeRadius = VillageUiSafeArea.clamp(Math.min(safe.width(), safe.height()) / 9, 21, 27);
        int contentTop = safe.top() + 49;
        int contentBottom = safe.bottom() - 49;
        int cy = (contentTop + contentBottom) / 2;
        int radiusX = VillageUiSafeArea.clamp(safe.width() / 4, 74, 126);
        int verticalRoom = Math.max(34, (contentBottom - contentTop) / 2 - nodeRadius - 5);
        int radiusY = VillageUiSafeArea.clamp(verticalRoom, 34, 68);
        return new Layout(safe.centerX(), cy, radiusX, radiusY, nodeRadius);
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

    private static Point pointFor(int index, int count, Layout layout) {
        if (count <= 1) return new Point(layout.cx(), layout.cy() - layout.radiusY());
        return switch (index & 3) {
            case 0 -> new Point(layout.cx(), layout.cy() - layout.radiusY());
            case 1 -> new Point(layout.cx() + layout.radiusX(), layout.cy());
            case 2 -> new Point(layout.cx(), layout.cy() + layout.radiusY());
            default -> new Point(layout.cx() - layout.radiusX(), layout.cy());
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

    private static boolean insideDiamond(double x, double y, int cx, int cy, int radius) {
        return Math.abs(x - cx) + Math.abs(y - cy) <= radius;
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
    private record Layout(int cx, int cy, int radiusX, int radiusY, int nodeRadius) {}
}
