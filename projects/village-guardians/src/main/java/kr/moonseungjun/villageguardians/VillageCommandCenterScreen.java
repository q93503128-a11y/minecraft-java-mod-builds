package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Responsive command surfaces for town, shop, status and local building interaction.
 * Short menus expand to use the available screen instead of being forced into a tiny scroll viewport.
 */
public final class VillageCommandCenterScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = VillageDefenseUiTheme.BACKDROP;
    private static final int TEXT = VillageDefenseUiTheme.TEXT;
    private static final int MUTED = VillageDefenseUiTheme.MUTED;
    private static final int CYAN = VillageDefenseUiTheme.CYAN;
    private static final int GOLD = VillageDefenseUiTheme.GOLD;
    private static final int RED = VillageDefenseUiTheme.RED;
    private static final int SURFACE = VillageDefenseUiTheme.PANEL_SOFT;
    private static final int SURFACE_2 = VillageDefenseUiTheme.PANEL_ACTIVE;
    private static final int LINE = VillageDefenseUiTheme.EDGE;

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
        List<Entry> roles = entries.stream().filter(e -> "role".equals(e.kind())).toList();
        List<Entry> facilities = entries.stream().filter(e -> "facility".equals(e.kind())).toList();
        TownLayout layout = townLayout(safe, roles.size(), facilities.size());
        drawHeader(graphics, safe, layout.header(), "지휘 회관", GOLD);

        graphics.text(font, "직업 배치", safe.left() + 7, layout.roleTitleY(), CYAN, false);
        for (int i = 0; i < roles.size(); i++) {
            Cell c = roleCell(layout, i);
            Entry e = roles.get(i);
            boolean hovered = inside(mouseX, mouseY, c.x(), c.y(), c.w(), c.h());
            graphics.fill(c.x(), c.y(), c.x() + c.w(), c.y() + c.h(), hovered ? SURFACE_2 : SURFACE);
            graphics.fill(c.x(), c.y(), c.x() + c.w(), c.y() + 2, e.current() ? GOLD : CYAN);
            VillageQuickChatSafeScreen.drawDiamond(graphics, c.x() + 11, c.y() + 13, 5, 0xCC203036);
            VillageQuickChatSafeScreen.drawDiamondOutline(graphics, c.x() + 11, c.y() + 13, 5,
                    e.current() ? GOLD : CYAN);
            graphics.text(font, fit(font, e.title(), c.w() - 28), c.x() + 22, c.y() + 7,
                    e.current() ? GOLD : TEXT, false);
            String state = e.current() ? "현재 직업" : "배치";
            graphics.text(font, state, c.x() + 7, c.y() + c.h() - 13,
                    e.current() ? GOLD : CYAN, false);
        }

        graphics.text(font, "시설 지휘", safe.left() + 7, layout.facilityTitleY(), CYAN, false);
        drawTownFacilities(graphics, facilities, layout.facilityGrid(), mouseX, mouseY);
        footer(graphics, safe);
    }

    private void drawTownFacilities(GuiGraphicsExtractor graphics, List<Entry> facilities, Grid grid,
                                    int mouseX, int mouseY) {
        graphics.enableScissor(grid.left(), grid.top(), grid.right(), grid.bottom());
        for (int i = 0; i < facilities.size(); i++) {
            Entry e = facilities.get(i);
            Cell c = cell(grid, i, 0);
            boolean hovered = inside(mouseX, mouseY, c.x(), c.y(), c.w(), c.h());
            graphics.fill(c.x(), c.y(), c.x() + c.w(), c.y() + c.h(), hovered ? SURFACE_2 : SURFACE);
            graphics.fill(c.x(), c.y(), c.x() + 3, c.y() + c.h(), hovered ? GOLD : CYAN);
            graphics.text(font, fit(font, e.title(), c.w() - 15), c.x() + 9, c.y() + 5,
                    hovered ? GOLD : TEXT, false);
            graphics.text(font, fit(font, e.meta(), c.w() - 15), c.x() + 9, c.y() + 17, CYAN, false);
            int barY = c.y() + c.h() - 8;
            if (!e.sub().isBlank() && c.h() >= 48) {
                graphics.text(font, fit(font, e.sub(), c.w() - 15), c.x() + 9, c.y() + 29, MUTED, false);
            }
            if (e.maximum() > 0) {
                int left = c.x() + 9;
                int right = c.x() + c.w() - 9;
                int fill = left + (right - left) * Math.max(0, Math.min(e.currentValue(), e.maximum()))
                        / Math.max(1, e.maximum());
                graphics.fill(left, barY, right, barY + 3, 0xFF39464B);
                graphics.fill(left, barY, fill, barY + 3,
                        e.currentValue() * 3 < e.maximum() ? RED : CYAN);
            }
        }
        graphics.disableScissor();
    }

    private void renderShop(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe, int mouseX, int mouseY) {
        Header header = headerLayout(safe, body, 2);
        drawHeader(graphics, safe, header, "오늘의 진열대", GOLD);
        Grid grid = shopGrid(safe, header.bottom(), entries.size());
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
            graphics.text(font, fit(font, e.title(), c.w() - 14), c.x() + 9, c.y() + 6, TEXT, false);
            graphics.text(font, fit(font, e.meta(), c.w() - 14), c.x() + 9, c.y() + 19, GOLD, false);
            List<FormattedCharSequence> detail = font.split(Component.literal(e.sub()), Math.max(40, c.w() - 16));
            int y = c.y() + 33;
            int maxLines = Math.max(1, Math.min(2, (c.h() - 49) / 11 + 1));
            for (int line = 0; line < Math.min(maxLines, detail.size()); line++) {
                graphics.text(font, detail.get(line), c.x() + 9, y, MUTED, false);
                y += 11;
            }
            String state = e.available() ? "구매" : fit(font, e.state(), Math.max(30, c.w() / 2));
            graphics.text(font, state, c.x() + c.w() - font.width(state) - 7, c.y() + c.h() - 13,
                    e.available() ? CYAN : RED, false);
        }
        graphics.disableScissor();
        scrollbar(graphics, grid, scroll, maximum, content);
        footer(graphics, safe);
    }

    private void renderStatus(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe, int mouseX, int mouseY) {
        Header header = headerLayout(safe, "현재 전투·성장·마을 상태", 1);
        drawHeader(graphics, safe, header, "수호자 기록", CYAN);
        String[] lines = body.split("\\n", -1);
        int top = header.bottom() + 5;
        int bottom = safe.bottom() - 36;
        int available = Math.max(50, bottom - top);
        int rowHeight = Math.max(26, Math.min(48, available / Math.max(1, lines.length)));
        for (int i = 0; i < lines.length; i++) {
            int y = top + i * rowHeight;
            if (y + rowHeight > bottom + 1) break;
            String line = lines[i].trim();
            String title = line;
            String detail = "";
            int split = line.indexOf("  ");
            if (split > 0) {
                title = line.substring(0, split).trim();
                detail = line.substring(split).trim();
            }
            graphics.fill(safe.left() + 8, y, safe.right() - 8, y + rowHeight - 4,
                    (i & 1) == 0 ? SURFACE : 0xB90F171B);
            graphics.fill(safe.left() + 8, y, safe.left() + 11, y + rowHeight - 4,
                    i == 0 ? GOLD : CYAN);
            graphics.text(font, fit(font, title, 82), safe.left() + 19, y + 7,
                    i == 0 ? GOLD : CYAN, false);
            graphics.text(font, fit(font, detail, safe.width() - 122), safe.left() + 101, y + 7,
                    TEXT, false);
        }
        int buttonY = safe.bottom() - 29;
        int x = safe.right() - 8;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry e = entries.get(i);
            int w = Math.min(170, Math.max(88, font.width(e.title()) + 24));
            x -= w;
            boolean hover = inside(mouseX, mouseY, x, buttonY, w, 20);
            graphics.fill(x, buttonY, x + w, buttonY + 20, hover ? SURFACE_2 : SURFACE);
            graphics.fill(x, buttonY + 18, x + w, buttonY + 20, hover ? GOLD : CYAN);
            graphics.centeredText(font, fit(font, e.title(), w - 10), x + w / 2, buttonY + 6,
                    hover ? GOLD : TEXT);
            x -= 6;
        }
        footer(graphics, safe);
    }

    private void renderFacility(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe, int mouseX, int mouseY) {
        Header header = headerLayout(safe, body, safe.height() < 245 ? 2 : 3);
        drawHeader(graphics, safe, header, heading.isBlank() ? "시설 단말" : heading, CYAN);
        List<Entry> usable = entries.stream().filter(e -> !"facility_info".equals(e.action())).toList();
        Grid grid = facilityGrid(safe, header.bottom(), usable.size());
        int rows = rows(usable.size(), grid.columns());
        int content = rows * grid.rowHeight();
        int maximum = Math.max(0, content - grid.height());
        scroll = VillageUiSafeArea.clamp(scroll, 0, maximum);

        if (usable.isEmpty()) {
            int cy = grid.top() + grid.height() / 2;
            graphics.centeredText(font, "현재 시설은 자동 효과형입니다.", safe.centerX(), cy - 7, CYAN);
            graphics.centeredText(font, "별도 조작 없이 시설 레벨과 내구도에 따라 효과가 적용됩니다.",
                    safe.centerX(), cy + 9, MUTED);
            footer(graphics, safe);
            return;
        }

        graphics.enableScissor(grid.left(), grid.top(), grid.right(), grid.bottom());
        for (int i = 0; i < usable.size(); i++) {
            Entry e = usable.get(i);
            Cell c = cell(grid, i, scroll);
            if (!visible(c, grid)) continue;
            boolean hover = inside(mouseX, mouseY, c.x(), c.y(), c.w(), c.h());
            graphics.fill(c.x(), c.y(), c.x() + c.w(), c.y() + c.h(), hover ? SURFACE_2 : SURFACE);
            graphics.fill(c.x(), c.y(), c.x() + 3, c.y() + c.h(), hover ? GOLD : CYAN);
            VillageQuickChatSafeScreen.drawDiamond(graphics, c.x() + 16, c.y() + 17, 6, 0xCC203036);
            VillageQuickChatSafeScreen.drawDiamondOutline(graphics, c.x() + 16, c.y() + 17, 6,
                    hover ? GOLD : CYAN);
            graphics.text(font, fit(font, e.title(), c.w() - 42), c.x() + 31, c.y() + 8,
                    hover ? GOLD : TEXT, false);
            List<FormattedCharSequence> detail = font.split(Component.literal(e.sub()), Math.max(42, c.w() - 20));
            int y = c.y() + 29;
            int maxLines = Math.max(1, Math.min(4, (c.h() - 43) / 11));
            for (int line = 0; line < Math.min(maxLines, detail.size()); line++) {
                graphics.text(font, detail.get(line), c.x() + 10, y, MUTED, false);
                y += 11;
            }
            graphics.fill(c.x() + 10, c.y() + c.h() - 7,
                    c.x() + (hover ? c.w() - 10 : Math.min(c.w() - 10, 54)),
                    c.y() + c.h() - 5, hover ? GOLD : CYAN);
        }
        graphics.disableScissor();
        scrollbar(graphics, grid, scroll, maximum, content);
        footer(graphics, safe);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        Entry target = switch (mode()) {
            case TOWN -> townHit(safe, click.x(), click.y());
            case SHOP -> {
                Header header = headerLayout(safe, body, 2);
                yield gridHit(entries, shopGrid(safe, header.bottom(), entries.size()), click.x(), click.y());
            }
            case STATUS -> statusHit(safe, click.x(), click.y());
            case FACILITY -> {
                Header header = headerLayout(safe, body, safe.height() < 245 ? 2 : 3);
                List<Entry> usable = entries.stream().filter(e -> !"facility_info".equals(e.action())).toList();
                yield gridHit(usable, facilityGrid(safe, header.bottom(), usable.size()), click.x(), click.y());
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
        List<Entry> facilities = entries.stream().filter(e -> "facility".equals(e.kind())).toList();
        TownLayout layout = townLayout(safe, roles.size(), facilities.size());
        for (int i = 0; i < roles.size(); i++) {
            Cell c = roleCell(layout, i);
            if (inside(mx, my, c.x(), c.y(), c.w(), c.h())) return roles.get(i);
        }
        return gridHit(facilities, layout.facilityGrid(), mx, my, 0);
    }

    private Entry statusHit(VillageUiSafeArea.Rect safe, double mx, double my) {
        int buttonY = safe.bottom() - 29;
        int x = safe.right() - 8;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry e = entries.get(i);
            int w = Math.min(170, Math.max(88, font.width(e.title()) + 24));
            x -= w;
            if (inside(mx, my, x, buttonY, w, 20)) return e;
            x -= 6;
        }
        return null;
    }

    private Entry gridHit(List<Entry> list, Grid grid, double mx, double my) {
        return gridHit(list, grid, mx, my, scroll);
    }

    private Entry gridHit(List<Entry> list, Grid grid, double mx, double my, int scrollValue) {
        for (int i = 0; i < list.size(); i++) {
            Cell c = cell(grid, i, scrollValue);
            if (inside(mx, my, c.x(), c.y(), c.w(), c.h())
                    && c.y() < grid.bottom() && c.y() + c.h() > grid.top()) return list.get(i);
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mode() == Mode.TOWN || mode() == Mode.STATUS) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        scroll = Math.max(0, scroll - (int) Math.round(vertical * 38));
        return true;
    }

    private TownLayout townLayout(VillageUiSafeArea.Rect safe, int roleCount, int facilityCount) {
        Header header = headerLayout(safe, body, safe.height() < 245 ? 1 : 2);
        int roleTitleY = header.bottom() + 3;
        int roleTop = roleTitleY + 12;
        int roleHeight = safe.height() < 245 ? 34 : 42;
        int count = Math.max(1, roleCount);
        int roleGap = 4;
        int roleWidth = Math.max(52, (safe.width() - 14 - roleGap * (count - 1)) / count);
        roleWidth = Math.min(132, roleWidth);
        int roleTotal = roleWidth * count + roleGap * (count - 1);
        int roleLeft = safe.centerX() - roleTotal / 2;
        int facilityTitleY = roleTop + roleHeight + 7;
        int facilityTop = facilityTitleY + 12;
        int facilityBottom = safe.bottom() - 15;
        int facilityColumns = townFacilityColumns(safe.width(), facilityCount);
        int facilityRows = Math.max(1, rows(facilityCount, facilityColumns));
        int available = Math.max(facilityRows * 30, facilityBottom - facilityTop);
        int rowHeight = Math.max(30, Math.min(70, available / facilityRows));
        Grid facilityGrid = exactGrid(safe.left() + 7, facilityTop, safe.right() - 7,
                facilityBottom, facilityColumns, rowHeight, 5);
        return new TownLayout(header, roleTitleY, roleLeft, roleTop, roleWidth, roleHeight, roleGap,
                facilityTitleY, facilityGrid);
    }

    private int townFacilityColumns(int availableWidth, int count) {
        if (count <= 4) return Math.max(1, count);
        if (availableWidth >= 350) return 4;
        return 3;
    }

    private Cell roleCell(TownLayout layout, int index) {
        return new Cell(layout.roleLeft() + index * (layout.roleWidth() + layout.roleGap()),
                layout.roleTop(), layout.roleWidth(), layout.roleHeight());
    }

    private Grid shopGrid(VillageUiSafeArea.Rect safe, int headerBottom, int count) {
        int top = headerBottom + 5;
        int bottom = safe.bottom() - 15;
        int columns = safe.width() >= 390 ? 4 : 3;
        if (count > 0) columns = Math.min(columns, count);
        columns = Math.max(1, columns);
        int visibleRows = Math.max(1, rows(Math.min(count, columns * 3), columns));
        int available = Math.max(44, bottom - top);
        int rowHeight = count <= columns * 3
                ? Math.max(48, Math.min(82, available / Math.max(1, rows(count, columns))))
                : 68;
        return exactGrid(safe.left() + 7, top, safe.right() - 7, bottom, columns, rowHeight, 5);
    }

    private Grid facilityGrid(VillageUiSafeArea.Rect safe, int headerBottom, int count) {
        int top = headerBottom + 6;
        int bottom = safe.bottom() - 15;
        int columns;
        if (count <= 1) columns = 1;
        else if (count <= 4) columns = safe.width() >= 280 ? 2 : 1;
        else if (count <= 8) columns = safe.width() >= 380 ? 3 : 2;
        else columns = safe.width() >= 520 ? 4 : 3;
        columns = Math.max(1, Math.min(columns, Math.max(1, count)));
        int totalRows = Math.max(1, rows(count, columns));
        int available = Math.max(42, bottom - top);
        int rowHeight = count <= 8
                ? Math.max(42, Math.min(96, available / totalRows))
                : 70;
        return exactGrid(safe.left() + 9, top, safe.right() - 9, bottom, columns, rowHeight, 6);
    }

    private Grid exactGrid(int left, int top, int right, int bottom, int columns, int rowHeight, int gap) {
        int width = Math.max(1, right - left);
        int cols = Math.max(1, columns);
        int cellWidth = Math.max(42, (width - gap * (cols - 1)) / cols);
        return new Grid(left, top, right, Math.max(top + 1, bottom), cols, cellWidth,
                Math.max(28, rowHeight), gap);
    }

    private Header headerLayout(VillageUiSafeArea.Rect safe, String subtitle, int maxLines) {
        int textWidth = Math.max(80, safe.width() - 30);
        List<FormattedCharSequence> lines = font.split(Component.literal(subtitle == null ? "" : subtitle), textWidth);
        int count = Math.max(0, Math.min(Math.max(0, maxLines), lines.size()));
        int bodyTop = safe.top() + 19;
        int dividerY = bodyTop + Math.max(1, count) * 11 + 3;
        return new Header(bodyTop, dividerY + 2, lines, count, dividerY);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe, Header header,
                            String title, int accent) {
        graphics.text(font, fit(font, title, safe.width() - 30), safe.left() + 7, safe.top() + 4, accent, false);
        int y = header.bodyTop();
        for (int i = 0; i < header.lineCount(); i++) {
            graphics.text(font, header.lines().get(i), safe.left() + 7, y, MUTED, false);
            y += 11;
        }
        graphics.fill(safe.left() + 7, header.dividerY(), safe.right() - 7, header.dividerY() + 1, LINE);
    }

    private void footer(GuiGraphicsExtractor graphics, VillageUiSafeArea.Rect safe) {
        graphics.text(font, "ESC 닫기", safe.left() + 4, safe.bottom() - 10, MUTED, false);
    }

    private void scrollbar(GuiGraphicsExtractor graphics, Grid grid, int value, int maximum, int content) {
        if (maximum <= 0 || content <= grid.height()) return;
        int thumb = Math.max(12, grid.height() * grid.height() / content);
        int y = grid.top() + (grid.height() - thumb) * value / maximum;
        graphics.fill(grid.right() - 2, grid.top(), grid.right(), grid.bottom(), 0x555C686D);
        graphics.fill(grid.right() - 2, y, grid.right(), y + thumb, CYAN);
    }

    private static int rows(int count, int columns) {
        return count <= 0 ? 0 : (count + columns - 1) / columns;
    }

    private static Cell cell(Grid grid, int index, int scroll) {
        int row = index / grid.columns();
        int col = index % grid.columns();
        int x = grid.left() + col * (grid.cellWidth() + grid.gap());
        int y = grid.top() + row * grid.rowHeight() - scroll;
        return new Cell(x, y, grid.cellWidth(), Math.max(24, grid.rowHeight() - 4));
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
    private record Header(int bodyTop, int bottom, List<FormattedCharSequence> lines, int lineCount, int dividerY) {}
    private record TownLayout(Header header, int roleTitleY, int roleLeft, int roleTop, int roleWidth,
                              int roleHeight, int roleGap, int facilityTitleY, Grid facilityGrid) {}
    private record Grid(int left, int top, int right, int bottom, int columns,
                        int cellWidth, int rowHeight, int gap) {
        int height() { return bottom - top; }
    }
    private record Cell(int x, int y, int w, int h) {}
}
