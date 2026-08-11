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
import java.util.Locale;

/**
 * Shared safe renderer for the high-frequency non-tree menus.
 * Each mode keeps its own visual grammar while sharing one collision-free viewport contract.
 */
public final class VillageCommandCenterScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x70070A0D;
    private static final int TEXT = 0xFFF1F4F5;
    private static final int MUTED = 0xFFAAB5BA;
    private static final int CYAN = 0xFF52D9C2;
    private static final int GOLD = 0xFFFFC65C;
    private static final int RED = 0xFFE06E64;
    private static final int SURFACE = 0xD1131B1F;
    private static final int SURFACE_2 = 0xD51A252A;
    private static final int LINE = 0xA34B6873;

    private final String screenId;
    private final String heading;
    private final String body;
    private final List<Entry> entries = new ArrayList<>();
    private int scroll;

    public VillageCommandCenterScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        screenId = payload.screenId();
        heading = plain(payload.title());
        body = plain(payload.body());
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
        switch (mode()) {
            case TOWN -> renderTown(graphics, safe, mouseX, mouseY);
            case SHOP -> renderShop(graphics, safe, mouseX, mouseY);
            case STATUS -> renderStatus(graphics, safe, mouseX, mouseY);
            case FACILITY -> renderFacility(graphics, safe, mouseX, mouseY);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTown(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe, int mouseX, int mouseY) {
        header(graphics, safe, "지휘 회관", body, GOLD);
        int top = safe.top() + 48;
        int bottom = safe.bottom() - 19;
        List<Entry> roles = entries.stream().filter(e -> "role".equals(e.kind())).toList();
        List<Entry> facilities = entries.stream().filter(e -> "facility".equals(e.kind())).toList();

        graphics.text(font, "직업 배치", safe.left() + 8, top, CYAN, false);
        int roleTop = top + 15;
        int roleCount = Math.max(1, roles.size());
        int roleGap = 6;
        int roleWidth = Math.max(58, (safe.width() - 16 - roleGap * (roleCount - 1)) / roleCount);
        roleWidth = Math.min(132, roleWidth);
        int total = roleCount * roleWidth + Math.max(0, roleCount - 1) * roleGap;
        int roleLeft = safe.centerX() - total / 2;
        for (int i = 0; i < roles.size(); i++) {
            Entry entry = roles.get(i);
            int x = roleLeft + i * (roleWidth + roleGap);
            int h = 54;
            boolean hover = inside(mouseX, mouseY, x, roleTop, roleWidth, h);
            graphics.fill(x, roleTop, x + roleWidth, roleTop + h, hover ? SURFACE_2 : SURFACE);
            graphics.fill(x, roleTop, x + roleWidth, roleTop + 2, entry.current() ? GOLD : CYAN);
            VillageQuickChatSafeScreen.drawDiamond(graphics, x + 13, roleTop + 15, 6, 0xCC203036);
            VillageQuickChatSafeScreen.drawDiamondOutline(graphics, x + 13, roleTop + 15, 6,
                    entry.current() ? GOLD : CYAN);
            graphics.text(font, fit(font, entry.title(), roleWidth - 30), x + 25, roleTop + 7,
                    entry.current() ? GOLD : TEXT, false);
            graphics.text(font, fit(font, entry.sub(), roleWidth - 12), x + 7, roleTop + 26, MUTED, false);
            graphics.text(font, entry.current() ? "현재" : "배치", x + 7, roleTop + 39,
                    entry.current() ? GOLD : CYAN, false);
        }

        int facilityTitle = roleTop + 67;
        graphics.text(font, "시설 지휘", safe.left() + 8, facilityTitle, CYAN, false);
        Grid grid = grid(safe.left() + 8, facilityTitle + 15, safe.right() - 8, bottom,
                facilities.size(), 3, 4, 56);
        drawFacilityCards(graphics, facilities, grid, mouseX, mouseY, true);
        footer(graphics, safe);
    }

    private void renderShop(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe, int mouseX, int mouseY) {
        header(graphics, safe, "오늘의 진열대", body, GOLD);
        int top = safe.top() + 49;
        int bottom = safe.bottom() - 20;
        Grid grid = grid(safe.left() + 8, top, safe.right() - 8, bottom, entries.size(), 3, 5, 56);
        int rows = rows(entries.size(), grid.columns());
        int content = rows * grid.rowHeight();
        int maximum = Math.max(0, content - grid.height());
        scroll = VillageUiSafeArea.clamp(scroll, 0, maximum);
        graphics.enableScissor(grid.left(), grid.top(), grid.right(), grid.bottom());
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            Cell c = cell(grid, i, scroll);
            if (!visible(c, grid)) continue;
            boolean hover = inside(mouseX, mouseY, c.x(), c.y(), c.w(), c.h());
            int accent = rarityColor(e.title());
            graphics.fill(c.x(), c.y(), c.x() + c.w(), c.y() + c.h(), hover ? SURFACE_2 : SURFACE);
            graphics.fill(c.x(), c.y(), c.x() + 3, c.y() + c.h(), accent);
            graphics.text(font, fit(font, e.title(), c.w() - 13), c.x() + 9, c.y() + 6, TEXT, false);
            graphics.text(font, fit(font, e.meta(), c.w() - 13), c.x() + 9, c.y() + 20, GOLD, false);
            graphics.text(font, fit(font, e.sub(), c.w() - 13), c.x() + 9, c.y() + 34, MUTED, false);
            String state = e.available() ? "구매" : fit(font, e.state(), Math.max(30, c.w() / 2));
            graphics.text(font, state, c.x() + c.w() - font.width(state) - 7, c.y() + c.h() - 13,
                    e.available() ? CYAN : RED, false);
        }
        graphics.disableScissor();
        scrollbar(graphics, grid, scroll, maximum, content);
        footer(graphics, safe);
    }

    private void renderStatus(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe, int mouseX, int mouseY) {
        header(graphics, safe, "수호자 기록", "현재 전투·성장·마을 상태", CYAN);
        String[] lines = body.split("\\n", -1);
        int top = safe.top() + 52;
        int available = Math.max(80, safe.bottom() - top - 48);
        int rowHeight = Math.max(30, Math.min(48, available / Math.max(1, lines.length)));
        for (int i = 0; i < lines.length; i++) {
            int y = top + i * rowHeight;
            if (y + rowHeight > safe.bottom() - 39) break;
            String line = lines[i].trim();
            String title = line;
            String detail = "";
            int split = line.indexOf("  ");
            if (split > 0) {
                title = line.substring(0, split).trim();
                detail = line.substring(split).trim();
            }
            graphics.fill(safe.left() + 10, y, safe.right() - 10, y + rowHeight - 5,
                    (i & 1) == 0 ? SURFACE : 0xB90F171B);
            graphics.fill(safe.left() + 10, y, safe.left() + 13, y + rowHeight - 5,
                    i == 0 ? GOLD : CYAN);
            graphics.text(font, fit(font, title, 85), safe.left() + 21, y + 7,
                    i == 0 ? GOLD : CYAN, false);
            graphics.text(font, fit(font, detail, safe.width() - 128), safe.left() + 106, y + 7,
                    TEXT, false);
        }
        int buttonY = safe.bottom() - 33;
        int x = safe.right() - 10;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry e = entries.get(i);
            int w = Math.min(170, Math.max(88, font.width(e.title()) + 24));
            x -= w;
            boolean hover = inside(mouseX, mouseY, x, buttonY, w, 22);
            graphics.fill(x, buttonY, x + w, buttonY + 22, hover ? SURFACE_2 : SURFACE);
            graphics.fill(x, buttonY + 20, x + w, buttonY + 22, hover ? GOLD : CYAN);
            graphics.centeredText(font, fit(font, e.title(), w - 10), x + w / 2, buttonY + 7,
                    hover ? GOLD : TEXT);
            x -= 7;
        }
        footerLeft(graphics, safe);
    }

    private void renderFacility(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe, int mouseX, int mouseY) {
        header(graphics, safe, heading.isBlank() ? "시설 단말" : heading, body, CYAN);
        int top = safe.top() + 62;
        int bottom = safe.bottom() - 20;
        List<Entry> usable = entries.stream().filter(e -> !"facility_info".equals(e.action())).toList();
        Grid grid = grid(safe.left() + 12, top, safe.right() - 12, bottom, usable.size(), 2, 4, 58);
        int rows = rows(usable.size(), grid.columns());
        int content = rows * grid.rowHeight();
        int maximum = Math.max(0, content - grid.height());
        scroll = VillageUiSafeArea.clamp(scroll, 0, maximum);
        graphics.enableScissor(grid.left(), grid.top(), grid.right(), grid.bottom());
        for (int i = 0; i < usable.size(); i++) {
            Entry e = usable.get(i);
            Cell c = cell(grid, i, scroll);
            if (!visible(c, grid)) continue;
            boolean hover = inside(mouseX, mouseY, c.x(), c.y(), c.w(), c.h());
            graphics.fill(c.x(), c.y(), c.x() + c.w(), c.y() + c.h(), hover ? SURFACE_2 : SURFACE);
            VillageQuickChatSafeScreen.drawDiamond(graphics, c.x() + 16, c.y() + 17, 6, 0xCC203036);
            VillageQuickChatSafeScreen.drawDiamondOutline(graphics, c.x() + 16, c.y() + 17, 6,
                    hover ? GOLD : CYAN);
            graphics.text(font, fit(font, e.title(), c.w() - 40), c.x() + 31, c.y() + 8,
                    hover ? GOLD : TEXT, false);
            graphics.text(font, fit(font, e.sub(), c.w() - 19), c.x() + 10, c.y() + 30, MUTED, false);
            graphics.fill(c.x() + 10, c.y() + c.h() - 7, c.x() + (hover ? c.w() - 10 : Math.min(c.w() - 10, 45)),
                    c.y() + c.h() - 5, hover ? GOLD : CYAN);
        }
        graphics.disableScissor();
        scrollbar(graphics, grid, scroll, maximum, content);
        footer(graphics, safe);
    }

    private void drawFacilityCards(GuiGraphicsExtractor graphics, List<Entry> list, Grid grid,
                                   int mouseX, int mouseY, boolean showDurability) {
        int rows = rows(list.size(), grid.columns());
        int content = rows * grid.rowHeight();
        int maximum = Math.max(0, content - grid.height());
        scroll = VillageUiSafeArea.clamp(scroll, 0, maximum);
        graphics.enableScissor(grid.left(), grid.top(), grid.right(), grid.bottom());
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            Cell c = cell(grid, i, scroll);
            if (!visible(c, grid)) continue;
            boolean hover = inside(mouseX, mouseY, c.x(), c.y(), c.w(), c.h());
            graphics.fill(c.x(), c.y(), c.x() + c.w(), c.y() + c.h(), hover ? SURFACE_2 : SURFACE);
            graphics.text(font, fit(font, e.title(), c.w() - 15), c.x() + 8, c.y() + 7,
                    hover ? GOLD : TEXT, false);
            graphics.text(font, fit(font, e.meta(), c.w() - 15), c.x() + 8, c.y() + 21, CYAN, false);
            if (showDurability && e.maximum() > 0) {
                int barLeft = c.x() + 8;
                int barRight = c.x() + c.w() - 8;
                int fill = barLeft + (barRight - barLeft) * Math.max(0, Math.min(e.currentValue(), e.maximum()))
                        / Math.max(1, e.maximum());
                graphics.fill(barLeft, c.y() + c.h() - 12, barRight, c.y() + c.h() - 9, 0xFF39464B);
                graphics.fill(barLeft, c.y() + c.h() - 12, fill, c.y() + c.h() - 9,
                        e.currentValue() * 3 < e.maximum() ? RED : CYAN);
                graphics.text(font, e.currentValue() + "/" + e.maximum(), barLeft,
                        c.y() + c.h() - 25, MUTED, false);
            }
        }
        graphics.disableScissor();
        scrollbar(graphics, grid, scroll, maximum, content);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        Entry target = switch (mode()) {
            case TOWN -> townHit(safe, click.x(), click.y());
            case SHOP -> gridHit(entries, grid(safe.left() + 8, safe.top() + 49,
                    safe.right() - 8, safe.bottom() - 20, entries.size(), 3, 5, 56), click.x(), click.y());
            case STATUS -> statusHit(safe, click.x(), click.y());
            case FACILITY -> {
                List<Entry> usable = entries.stream().filter(e -> !"facility_info".equals(e.action())).toList();
                yield gridHit(usable, grid(safe.left() + 12, safe.top() + 62,
                        safe.right() - 12, safe.bottom() - 20, usable.size(), 2, 4, 58), click.x(), click.y());
            }
        };
        if (target != null && !target.action().isBlank() && !"facility_info".equals(target.action())) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(target.action()));
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private Entry townHit(VillageUiSafeArea.Rect safe, double mx, double my) {
        List<Entry> roles = entries.stream().filter(e -> "role".equals(e.kind())).toList();
        int top = safe.top() + 48;
        int roleTop = top + 15;
        int count = Math.max(1, roles.size());
        int gap = 6;
        int roleWidth = Math.min(132, Math.max(58, (safe.width() - 16 - gap * (count - 1)) / count));
        int total = count * roleWidth + Math.max(0, count - 1) * gap;
        int left = safe.centerX() - total / 2;
        for (int i = 0; i < roles.size(); i++) {
            int x = left + i * (roleWidth + gap);
            if (inside(mx, my, x, roleTop, roleWidth, 54)) return roles.get(i);
        }
        List<Entry> facilities = entries.stream().filter(e -> "facility".equals(e.kind())).toList();
        int facilityTitle = roleTop + 67;
        return gridHit(facilities, grid(safe.left() + 8, facilityTitle + 15, safe.right() - 8,
                safe.bottom() - 19, facilities.size(), 3, 4, 56), mx, my);
    }

    private Entry statusHit(VillageUiSafeArea.Rect safe, double mx, double my) {
        int buttonY = safe.bottom() - 33;
        int x = safe.right() - 10;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry e = entries.get(i);
            int w = Math.min(170, Math.max(88, font.width(e.title()) + 24));
            x -= w;
            if (inside(mx, my, x, buttonY, w, 22)) return e;
            x -= 7;
        }
        return null;
    }

    private Entry gridHit(List<Entry> list, Grid grid, double mx, double my) {
        for (int i = 0; i < list.size(); i++) {
            Cell c = cell(grid, i, scroll);
            if (inside(mx, my, c.x(), c.y(), c.w(), c.h()) && c.y() < grid.bottom() && c.y() + c.h() > grid.top()) {
                return list.get(i);
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int) Math.round(vertical * 34));
        return true;
    }

    private void header(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe,
                        String title, String subtitle, int accent) {
        graphics.text(font, fit(font, title, safe.width() - 35), safe.left() + 8, safe.top() + 6, accent, false);
        graphics.text(font, fit(font, subtitle.replace('\n', ' '), safe.width() - 35),
                safe.left() + 8, safe.top() + 23, MUTED, false);
        graphics.fill(safe.left() + 8, safe.top() + 39, safe.right() - 8, safe.top() + 40, LINE);
    }

    private void footer(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe) {
        graphics.text(font, "ESC 닫기", safe.left() + 4, safe.bottom() - 11, MUTED, false);
    }
    private void footerLeft(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe) { footer(graphics, safe); }

    private void scrollbar(GuiGraphicsExtractor graphics, Grid grid, int value, int maximum, int content) {
        if (maximum <= 0 || content <= grid.height()) return;
        int thumb = Math.max(12, grid.height() * grid.height() / content);
        int y = grid.top() + (grid.height() - thumb) * value / maximum;
        graphics.fill(grid.right() - 2, grid.top(), grid.right(), grid.bottom(), 0x555C686D);
        graphics.fill(grid.right() - 2, y, grid.right(), y + thumb, CYAN);
    }

    private Grid grid(int left, int top, int right, int bottom, int count, int minCols, int maxCols, int rowHeight) {
        int width = Math.max(1, right - left);
        int columns = VillageUiSafeArea.clamp(width / 170, minCols, maxCols);
        if (count > 0) columns = Math.min(columns, Math.max(1, count));
        int gap = 6;
        int cellWidth = Math.max(64, (width - gap * (columns - 1)) / columns);
        return new Grid(left, top, right, Math.max(top + 28, bottom), columns, cellWidth, rowHeight, gap);
    }

    private static int rows(int count, int columns) {
        return count <= 0 ? 0 : (count + columns - 1) / columns;
    }

    private static Cell cell(Grid grid, int index, int scroll) {
        int row = index / grid.columns();
        int col = index % grid.columns();
        int x = grid.left() + col * (grid.cellWidth() + grid.gap());
        int y = grid.top() + row * grid.rowHeight() - scroll;
        return new Cell(x, y, grid.cellWidth(), grid.rowHeight() - 5);
    }

    private static boolean visible(Cell c, Grid grid) {
        return c.y() + c.h() > grid.top() && c.y() < grid.bottom();
    }

    private Mode mode() {
        if ("town_hall".equals(screenId)) return Mode.TOWN;
        if ("equipment_shop".equals(screenId)) return Mode.SHOP;
        if ("status".equals(screenId)) return Mode.STATUS;
        return Mode.FACILITY;
    }

    private void parse(VillageNetwork.OpenVillageUiPayload payload) {
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) entries.add(parseEntry(actions[i], labels[i]));
    }

    private Entry parseEntry(String action, String label) {
        String[] p = label.split("\\|", -1);
        if (p.length >= 9 && "role".equals(p[0])) {
            return new Entry(action, "role", plain(p[2]), plain(p[3]), plain(p[4]), plain(p[7]),
                    "current".equals(p[7]), true, 0, 0);
        }
        if (p.length >= 7 && "facility".equals(p[0])) {
            int current = p.length > 4 ? parseInt(p[4]) : 0;
            int maximum = p.length > 5 ? parseInt(p[5]) : 0;
            return new Entry(action, "facility", plain(p[2]), plain(p[6]), plain(p[3]), "",
                    false, true, current, maximum);
        }
        if (p.length >= 7 && "shop".equals(p[0])) {
            return new Entry(action, "shop", plain(p[2]), plain(p[4]), plain(p[3]), plain(p[5]),
                    false, "available".equals(p[6]), 0, 0);
        }
        if (p.length >= 2 && "shop_utility".equals(p[0])) {
            return new Entry(action, "utility", plain(p[1]), "상점 보조 기능", "", "", false, true, 0, 0);
        }
        String title = plain(p.length > 0 ? p[0] : action);
        String sub = plain(p.length > 1 ? p[1] : "");
        return new Entry(action, "action", title, sub, "", "", false, true, 0, 0);
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static int parseInt(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static int rarityColor(String title) {
        String v = title == null ? "" : title.toLowerCase(Locale.ROOT);
        if (v.contains("전설") || v.contains("legend")) return 0xFFFFB347;
        if (v.contains("영웅") || v.contains("epic")) return 0xFFD674FF;
        if (v.contains("희귀") || v.contains("rare")) return 0xFF71A8FF;
        if (v.contains("고급") || v.contains("uncommon")) return 0xFF75D98D;
        return CYAN;
    }

    private static String fit(Font font, String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + "…") > maxWidth) end--;
        return normalized.substring(0, end) + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private enum Mode { TOWN, SHOP, STATUS, FACILITY }
    private record Entry(String action, String kind, String title, String sub, String meta, String state,
                         boolean current, boolean available, int currentValue, int maximum) {}
    private record Grid(int left, int top, int right, int bottom, int columns,
                        int cellWidth, int rowHeight, int gap) {
        int height() { return bottom - top; }
    }
    private record Cell(int x, int y, int w, int h) {}
}
