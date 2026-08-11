package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Relic collection presented as a wide reliquary with independent row and label spacing. */
public final class VillageRelicScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x56000000;
    private static final int TEXT = 0xFFF7F0DD;
    private static final int MUTED = 0xFFAAA499;
    private static final int GOLD = 0xFFD8A84C;
    private static final int TEAL = 0xFF55CDB8;
    private static final int LOCKED = 0xFF606469;
    private static final int SHADOW = 0xCC12171B;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final List<Relic> relics = new ArrayList<>();
    private int selected;
    private int scroll;

    public VillageRelicScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        parse();
        for (int index = 0; index < relics.size(); index++) {
            if (relics.get(index).owned()) { selected = index; break; }
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int centerX = safe.centerX();
        int titleY = safe.top() + 4;
        graphics.centeredText(font, plain(payload.title()), centerX, titleY, GOLD);
        graphics.centeredText(font, fit(summaryLine(), Math.max(100, safe.width() - 32)),
                centerX, titleY + 15, MUTED);
        graphics.fill(safe.left() + 14, titleY + 29, safe.right() - 14, titleY + 30, 0x88A77B35);

        Grid grid = grid(safe, titleY + 37);
        int rows = relics.isEmpty() ? 0 : (relics.size() + grid.columns() - 1) / grid.columns();
        int contentHeight = rows * grid.rowHeight();
        int maximum = Math.max(0, contentHeight - grid.height());
        scroll = clamp(scroll, 0, maximum);
        graphics.enableScissor(grid.left() - 3, grid.top(), grid.right() + 3, grid.bottom());
        for (int index = 0; index < relics.size(); index++) {
            int row = index / grid.columns();
            int column = index % grid.columns();
            int cx = grid.left() + column * grid.cellWidth() + grid.cellWidth() / 2;
            int cy = grid.top() + row * grid.rowHeight() + grid.iconOffset() - scroll;
            if (cy < grid.top() - 28 || cy > grid.bottom() + 28) continue;
            Relic relic = relics.get(index);
            boolean hovered = insideDiamond(mouseX, mouseY, cx, cy, grid.radius() + 4);
            boolean active = index == selected;
            int outer = relic.owned() ? (active || hovered ? GOLD : TEAL) : LOCKED;
            drawRelic(graphics, cx, cy, grid.radius(), active, relic, outer);
            String name = relic.owned() ? relic.name() : "미획득";
            graphics.centeredText(font, fit(name, grid.cellWidth() - 8), cx,
                    cy + grid.radius() + 6, relic.owned() ? (active ? GOLD : TEXT) : MUTED);
        }
        graphics.disableScissor();
        scrollbar(graphics, grid, scroll, maximum, contentHeight);

        if (!relics.isEmpty()) {
            renderSelected(graphics, relics.get(clamp(selected, 0, relics.size() - 1)),
                    grid.bottom() + 5, safe);
        }
        graphics.text(font, "ESC 닫기", safe.left() + 4, safe.bottom() - 10, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawRelic(GuiGraphicsExtractor graphics, int cx, int cy, int radius,
                           boolean active, Relic relic, int outer) {
        VillageQuickChatSafeScreen.drawDiamond(graphics, cx, cy, active ? radius + 2 : radius, SHADOW);
        VillageQuickChatSafeScreen.drawDiamondOutline(graphics, cx, cy, active ? radius + 2 : radius, outer);
        int inner = Math.max(8, radius - 8);
        VillageQuickChatSafeScreen.drawDiamond(graphics, cx, cy, inner,
                relic.owned() ? 0xDD20363A : 0xDD2B2D30);
        String rune = relic.owned() && !relic.name().isBlank() ? relic.name().substring(0, 1) : "?";
        graphics.centeredText(font, rune, cx, cy - 4, relic.owned() ? TEXT : MUTED);
    }

    private void renderSelected(GuiGraphicsExtractor graphics, Relic relic, int top, VillageUiSafeArea.Rect safe) {
        int centerX = safe.centerX();
        graphics.centeredText(font, relic.owned() ? relic.name() : "미획득 유물", centerX, top,
                relic.owned() ? GOLD : MUTED);
        String description = relic.owned() ? relic.description()
                : "보스를 처치하고 제시된 유물 중 하나를 선택하면 공개됩니다.";
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(description),
                Math.min(620, Math.max(100, safe.width() - 28)));
        int y = top + 13;
        int maxLines = safe.height() < 245 ? 2 : 3;
        maxLines = Math.max(1, Math.min(maxLines, Math.max(1, (safe.bottom() - 19 - y) / 11)));
        for (int index = 0; index < Math.min(maxLines, lines.size()); index++) {
            int lineWidth = font.width(lines.get(index));
            graphics.text(font, lines.get(index), centerX - lineWidth / 2, y,
                    relic.owned() ? TEXT : MUTED, false);
            y += 11;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        Grid grid = grid(safe, safe.top() + 41);
        for (int index = 0; index < relics.size(); index++) {
            int row = index / grid.columns();
            int column = index % grid.columns();
            int cx = grid.left() + column * grid.cellWidth() + grid.cellWidth() / 2;
            int cy = grid.top() + row * grid.rowHeight() + grid.iconOffset() - scroll;
            if (cy < grid.top() || cy > grid.bottom()) continue;
            if (insideDiamond(click.x(), click.y(), cx, cy, grid.radius() + 5)) {
                selected = index;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int) Math.round(vertical * 34));
        return true;
    }

    private Grid grid(VillageUiSafeArea.Rect safe, int top) {
        int columns = safe.width() >= 360 ? 6 : safe.width() >= 290 ? 5 : 4;
        columns = Math.max(1, Math.min(columns, Math.max(1, relics.size())));
        int usableWidth = Math.max(1, safe.width() - 22);
        int cellWidth = Math.max(46, usableWidth / columns);
        int gridWidth = cellWidth * columns;
        int left = safe.centerX() - gridWidth / 2;
        int detailReserve = safe.height() < 245 ? 50 : 64;
        int bottom = Math.max(top + 48, safe.bottom() - detailReserve);
        int rowHeight = safe.height() < 245 ? 51 : 60;
        int radius = safe.height() < 245 ? 17 : 20;
        int iconOffset = radius + 3;
        return new Grid(left, top, left + gridWidth, bottom, columns, cellWidth, rowHeight, radius, iconOffset);
    }

    private void scrollbar(GuiGraphicsExtractor graphics, Grid grid, int value, int maximum, int content) {
        if (maximum <= 0 || content <= grid.height()) return;
        int thumb = Math.max(11, grid.height() * grid.height() / content);
        int y = grid.top() + (grid.height() - thumb) * value / maximum;
        graphics.fill(grid.right() - 2, grid.top(), grid.right(), grid.bottom(), 0x555C686D);
        graphics.fill(grid.right() - 2, y, grid.right(), y + thumb, TEAL);
    }

    private String summaryLine() {
        String value = plain(payload.body()).replace('\n', ' ');
        return value.isBlank() ? "획득한 유물과 누적 효과를 확인합니다." : value;
    }

    private void parse() {
        if (payload.labels().isBlank()) return;
        for (String raw : payload.labels().split(SEP, -1)) {
            String[] p = raw.split("\\|", 5);
            if (p.length < 5 || !"relic".equals(p[0])) continue;
            relics.add(new Relic(p[1], "owned".equals(p[2]), plain(p[3]), plain(p[4])));
        }
    }

    private String fit(String value, int maxWidth) {
        String safe = plain(value);
        if (maxWidth <= 0 || font.width(safe) <= maxWidth) return maxWidth <= 0 ? "" : safe;
        int end = safe.length();
        while (end > 0 && font.width(safe.substring(0, end) + "…") > maxWidth) end--;
        return safe.substring(0, end) + "…";
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static boolean insideDiamond(double x, double y, int cx, int cy, int radius) {
        return Math.abs(x - cx) + Math.abs(y - cy) <= radius;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Relic(String id, boolean owned, String name, String description) {}
    private record Grid(int left, int top, int right, int bottom, int columns, int cellWidth,
                        int rowHeight, int radius, int iconOffset) {
        int height() { return bottom - top; }
    }
}
