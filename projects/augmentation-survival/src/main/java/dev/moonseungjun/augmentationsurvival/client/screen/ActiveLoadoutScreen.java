package dev.moonseungjun.augmentationsurvival.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.client.AugmentIconClient;
import dev.moonseungjun.augmentationsurvival.client.ClientActiveLoadoutState;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import dev.moonseungjun.augmentationsurvival.network.ActiveLoadoutEditPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Responsive server-backed active augment loadout screen for the ten-slot combat flow. */
public final class ActiveLoadoutScreen extends Screen {
    private static final int BG = 0xF30A0E12;
    private static final int PANEL = 0xF00E1419;
    private static final int SURFACE = 0xFF12191E;
    private static final int SURFACE_HI = 0xFF182229;
    private static final int LINE = 0xFF344047;
    private static final int LINE_HI = 0xFF718089;
    private static final int TEXT = 0xFFF1F5F7;
    private static final int MUTED = 0xFF929DA3;
    private static final int FAINT = 0xFF5C666C;
    private static final int ACCENT = 0xFF75C4D4;

    private int selectedSlot = -1;
    private String selectedAugmentId = "";
    private String hoveredId = "";
    private int page;

    public ActiveLoadoutScreen() {
        super(Component.literal("전투 증강 편성"));
    }

    @Override
    protected void init() {
        ClientPlayNetworking.send(new ActiveLoadoutEditPayload(ActiveLoadoutEditPayload.REQUEST, 0, 0, ""));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Layout layout = layout();
        hoveredId = "";

        g.fill(0, 0, width, height, 0x8B000000);
        g.fill(layout.x(), layout.y(), layout.x() + layout.w(), layout.y() + layout.h(), BG);
        g.outline(layout.x(), layout.y(), layout.w(), layout.h(), 0xFF46535B);
        g.fill(layout.x() + 1, layout.y() + 1, layout.x() + layout.w() - 1, layout.y() + 31, 0xFF141C22);
        g.fill(layout.x() + 2, layout.y() + 2, layout.x() + layout.w() - 2, layout.y() + 3, 0xFF76858E);

        int headerHalf = Math.max(70, layout.w() / 2 - 22);
        g.text(font, clip("전투 증강 편성", headerHalf), layout.x() + 12, layout.y() + 11, TEXT, false);
        String mode = layout.compact() ? "Alt: 모드 ON/OFF" : "Alt: 증강 모드 ON/OFF · 1~0 사용";
        mode = clip(mode, headerHalf);
        g.text(font, mode, layout.x() + layout.w() - 12 - font.width(mode), layout.y() + 11, MUTED, false);

        drawQuickbar(g, layout, mouseX, mouseY);
        drawAvailablePanel(g, layout.listX(), layout.listY(), layout.listW(), layout.listH(), mouseX, mouseY);

        String inspect = inspectId();
        if (layout.detailPanel()) {
            drawDetailPanel(g, layout.detailX(), layout.detailY(), layout.detailW(), layout.detailH(), inspect);
        } else if (layout.detailH() > 0) {
            drawCompactDetail(g, layout.detailX(), layout.detailY(), layout.detailW(), layout.detailH(), inspect);
        }

        String help = selectedSlot >= 0
                ? "보유 액티브를 선택해 장착 · 다른 슬롯 선택 시 교환 · 우클릭 해제"
                : !selectedAugmentId.isBlank()
                    ? "상단 슬롯을 선택해 장착 · Shift: 정확한 수치"
                    : "슬롯 또는 보유 액티브 선택 · Shift: 정확한 수치";
        help = clip(help, layout.w() - 24);
        centered(g, help, layout.x() + layout.w() / 2, layout.y() + layout.h() - 14, MUTED);
    }

    private String inspectId() {
        if (!hoveredId.isBlank()) return hoveredId;
        if (!selectedAugmentId.isBlank()) return selectedAugmentId;
        if (selectedSlot >= 0) return ClientActiveLoadoutState.slot(selectedSlot);
        return "";
    }

    private void drawQuickbar(GuiGraphicsExtractor g, Layout layout, int mx, int my) {
        g.text(font, "빠른 사용 슬롯", layout.x() + 12, layout.quickLabelY(), 0xFFC9D2D7, false);
        if (layout.w() >= 430) {
            String hint = "Alt 모드에서 숫자키로 사용";
            g.text(font, hint, layout.x() + layout.w() - 12 - font.width(hint), layout.quickLabelY(), FAINT, false);
        }

        for (int i = 0; i < 10; i++) {
            int sx = layout.slotStartX() + i * (layout.slotW() + layout.slotGap());
            int sy = layout.slotY();
            String id = ClientActiveLoadoutState.slot(i);
            boolean hover = inside(mx, my, sx, sy, layout.slotW(), layout.slotH());
            boolean selected = selectedSlot == i;
            int bg = selected ? 0xFF17262D : hover ? SURFACE_HI : SURFACE;
            g.fill(sx, sy, sx + layout.slotW(), sy + layout.slotH(), bg);
            g.outline(sx, sy, layout.slotW(), layout.slotH(), selected ? ACCENT : hover ? LINE_HI : LINE);

            String key = i == 9 ? "0" : Integer.toString(i + 1);
            g.text(font, key, sx + 3, sy + 3, selected ? ACCENT : MUTED, false);
            if (id.isBlank()) {
                centered(g, "—", sx + layout.slotW() / 2, sy + Math.max(11, layout.slotH() / 2 - 4), FAINT);
            } else {
                AugmentDefinition d = safe(id);
                if (d != null) {
                    int iconY = sy + Math.max(8, (layout.slotH() - 16) / 2);
                    g.item(AugmentIconClient.stackFor(id, d.tier()), sx + layout.slotW() / 2 - 8, iconY);
                    if (layout.slotH() >= 28) {
                        g.fill(sx + 4, sy + layout.slotH() - 3, sx + layout.slotW() - 4, sy + layout.slotH() - 1, tierColor(d));
                    }
                }
                if (hover) hoveredId = id;
            }
        }
    }

    private void drawAvailablePanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int mx, int my) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, LINE);
        List<String> available = ClientActiveLoadoutState.available();
        AvailableLayout grid = availableLayout(x, y, w, h);
        int pages = Math.max(1, (available.size() + grid.pageSize() - 1) / grid.pageSize());
        page = Math.max(0, Math.min(page, pages - 1));

        g.text(font, "보유 액티브", x + 10, y + 9, TEXT, false);
        String count = available.size() + "개";
        g.text(font, count, x + w - 10 - font.width(count), y + 9, MUTED, false);
        g.fill(x + 10, y + 26, x + w - 10, y + 27, 0xFF273138);

        if (available.isEmpty()) {
            g.text(font, clip("아직 액티브 증강을 획득하지 않았습니다.", w - 24), x + 12, y + 43, TEXT, false);
            if (h >= 78) g.text(font, clip("패시브 증강은 장착 없이 항상 적용됩니다.", w - 24), x + 12, y + 58, MUTED, false);
            return;
        }

        int start = page * grid.pageSize();
        int end = Math.min(available.size(), start + grid.pageSize());
        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % grid.columns();
            int row = local / grid.columns();
            int cx = grid.startX() + col * (grid.cellW() + grid.gapX());
            int cy = grid.startY() + row * (grid.cellH() + grid.gapY());
            drawAvailableCell(g, cx, cy, grid.cellW(), grid.cellH(), available.get(i), mx, my);
        }

        if (pages > 1) {
            drawPager(g, grid, page, pages, mx, my);
        }
    }

    private void drawAvailableCell(GuiGraphicsExtractor g, int x, int y, int w, int h, String id, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        boolean selected = id.equals(selectedAugmentId);
        boolean equipped = ClientActiveLoadoutState.slots().contains(id);
        AugmentDefinition d = safe(id);
        int accent = d == null ? LINE_HI : tierColor(d);
        g.fill(x, y, x + w, y + h, selected ? 0xFF172329 : hover ? SURFACE_HI : SURFACE);
        g.fill(x, y, x + 3, y + h, accent);
        if (selected) g.outline(x, y, w, h, accent);
        if (d != null && w >= 48) g.item(AugmentIconClient.stackFor(id, d.tier()), x + 7, y + Math.max(5, (h - 16) / 2));
        int textX = x + (w >= 48 ? 30 : 7);
        g.text(font, clip(displayName(id), Math.max(18, w - (textX - x) - 8)), textX, y + 5,
                hover || selected ? TEXT : 0xFFD2DADF, false);
        if (h >= 28) {
            String sub = equipped ? "장착 중" : "Lv." + Math.max(1, ClientAugmentProgressState.level(id));
            g.text(font, clip(sub, Math.max(18, w - (textX - x) - 8)), textX, y + 17, equipped ? ACCENT : MUTED, false);
        }
        if (hover) hoveredId = id;
    }

    private void drawPager(GuiGraphicsExtractor g, AvailableLayout grid, int current, int pages, int mx, int my) {
        boolean left = current > 0 && inside(mx, my, grid.leftPagerX(), grid.pagerY(), 26, 16);
        boolean right = current + 1 < pages && inside(mx, my, grid.rightPagerX(), grid.pagerY(), 26, 16);
        g.text(font, "‹", grid.leftPagerX() + 9, grid.pagerY() + 3, left ? TEXT : FAINT, false);
        String state = (current + 1) + " / " + pages;
        centered(g, state, grid.centerX(), grid.pagerY() + 3, MUTED);
        g.text(font, "›", grid.rightPagerX() + 9, grid.pagerY() + 3, right ? TEXT : FAINT, false);
    }

    private void drawDetailPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, String id) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, LINE);
        if (id == null || id.isBlank()) {
            g.text(font, "증강 정보", x + 11, y + 10, TEXT, false);
            g.fill(x + 11, y + 28, x + w - 11, y + 29, 0xFF273138);
            g.text(font, clip("보유 액티브나 슬롯에 마우스를 올리세요.", w - 22), x + 11, y + 45, MUTED, false);
            return;
        }
        AugmentDefinition d = safe(id);
        if (d == null) return;
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int level = Math.max(1, ClientAugmentProgressState.level(id));
        int color = tierColor(d);

        g.fill(x, y, x + 4, y + h, color);
        if (w >= 72) g.item(AugmentIconClient.stackFor(id, d.tier()), x + 12, y + 10);
        int textX = x + (w >= 72 ? 36 : 10);
        g.text(font, clip(p.displayName(), Math.max(20, x + w - 10 - textX)), textX, y + 9, TEXT, false);
        String meta = tierLabel(d) + " · " + p.kind().label() + " · Lv." + level + "/" + d.maxLevel();
        g.text(font, clip(meta, Math.max(20, x + w - 10 - textX)), textX, y + 22, MUTED, false);
        g.fill(x + 11, y + 40, x + w - 11, y + 41, 0xFF293239);

        boolean detail = shiftDown();
        String value = detail ? p.detail() : p.summary();
        List<String> lines = wrap(value, Math.max(24, w - 24));
        int maxLines = Math.max(1, (h - 72) / 11);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            String line = lines.get(i);
            if (i == maxLines - 1 && lines.size() > maxLines) line = clip(line + "…", w - 24);
            g.text(font, line, x + 12, y + 51 + i * 11, i == 0 ? 0xFFDCE3E7 : 0xFFC2CBD0, false);
        }
        String footer = detail ? "Shift 상세 표시 중" : "Shift: 정확한 수치";
        if (h >= 86) g.text(font, clip(footer, w - 24), x + 12, y + h - 15, detail ? color : FAINT, false);
    }

    private void drawCompactDetail(GuiGraphicsExtractor g, int x, int y, int w, int h, String id) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, LINE);
        if (id == null || id.isBlank()) {
            g.text(font, clip("항목에 마우스를 올리면 효과를 확인할 수 있습니다.", w - 18), x + 9, y + Math.max(5, h / 2 - 4), MUTED, false);
            return;
        }
        AugmentDefinition d = safe(id);
        if (d == null) return;
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int color = tierColor(d);
        g.fill(x, y, x + 3, y + h, color);
        int level = Math.max(1, ClientAugmentProgressState.level(id));
        String title = p.displayName() + " · " + tierLabel(d) + " · Lv." + level;
        g.text(font, clip(title, w - 20), x + 9, y + 5, TEXT, false);
        if (h >= 31) {
            String value = shiftDown() ? p.detail() : p.summary();
            g.text(font, clip(value, w - 20), x + 9, y + 18, 0xFFC7D0D5, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout layout = layout();
        for (int i = 0; i < 10; i++) {
            int sx = layout.slotStartX() + i * (layout.slotW() + layout.slotGap());
            if (!inside(event.x(), event.y(), sx, layout.slotY(), layout.slotW(), layout.slotH())) continue;
            if (event.button() == 1) {
                ClientPlayNetworking.send(new ActiveLoadoutEditPayload(ActiveLoadoutEditPayload.CLEAR, i, 0, ""));
                if (selectedSlot == i) selectedSlot = -1;
                return true;
            }
            if (event.button() == 0) {
                if (!selectedAugmentId.isBlank()) {
                    ClientPlayNetworking.send(new ActiveLoadoutEditPayload(ActiveLoadoutEditPayload.ASSIGN, i, 0, selectedAugmentId));
                    selectedAugmentId = "";
                    selectedSlot = -1;
                } else if (selectedSlot >= 0 && selectedSlot != i) {
                    ClientPlayNetworking.send(new ActiveLoadoutEditPayload(ActiveLoadoutEditPayload.SWAP, selectedSlot, i, ""));
                    selectedSlot = -1;
                } else {
                    selectedSlot = selectedSlot == i ? -1 : i;
                }
                return true;
            }
        }

        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        List<String> available = ClientActiveLoadoutState.available();
        AvailableLayout grid = availableLayout(layout.listX(), layout.listY(), layout.listW(), layout.listH());
        int pages = Math.max(1, (available.size() + grid.pageSize() - 1) / grid.pageSize());
        page = Math.max(0, Math.min(page, pages - 1));
        int start = page * grid.pageSize();
        int end = Math.min(available.size(), start + grid.pageSize());
        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % grid.columns();
            int row = local / grid.columns();
            int cx = grid.startX() + col * (grid.cellW() + grid.gapX());
            int cy = grid.startY() + row * (grid.cellH() + grid.gapY());
            if (!inside(event.x(), event.y(), cx, cy, grid.cellW(), grid.cellH())) continue;
            String id = available.get(i);
            if (selectedSlot >= 0) {
                ClientPlayNetworking.send(new ActiveLoadoutEditPayload(ActiveLoadoutEditPayload.ASSIGN, selectedSlot, 0, id));
                selectedSlot = -1;
                selectedAugmentId = "";
            } else {
                selectedAugmentId = id.equals(selectedAugmentId) ? "" : id;
            }
            return true;
        }
        if (pages > 1) {
            if (inside(event.x(), event.y(), grid.leftPagerX(), grid.pagerY(), 26, 16) && page > 0) {
                page--;
                return true;
            }
            if (inside(event.x(), event.y(), grid.rightPagerX(), grid.pagerY(), 26, 16) && page + 1 < pages) {
                page++;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Layout layout() {
        int margin = width >= 300 && height >= 220 ? 8 : 4;
        int w = Math.max(1, Math.min(720, width - margin * 2));
        int h = Math.max(1, Math.min(390, height - margin * 2));
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        boolean compact = w < 540 || h < 300;

        int quickLabelY = y + 38;
        int slotGap = w >= 620 ? 5 : w >= 430 ? 4 : w >= 300 ? 2 : 1;
        int slotBudget = Math.max(1, w - 24);
        int slotW = Math.min(49, Math.max(16, (slotBudget - slotGap * 9) / 10));
        int slotH = compact ? Math.max(26, Math.min(34, h / 7)) : 46;
        int slotTotal = slotW * 10 + slotGap * 9;
        int slotStartX = x + (w - slotTotal) / 2;
        int slotY = y + 51;

        int bodyY = slotY + slotH + 10;
        int footerY = y + h - 22;
        int bodyH = Math.max(42, footerY - bodyY);
        int innerX = x + 11;
        int innerW = Math.max(40, w - 22);

        boolean detailPanel = !compact && innerW >= 500 && bodyH >= 120;
        int listX = innerX;
        int listY = bodyY;
        int listW;
        int listH;
        int detailX;
        int detailY;
        int detailW;
        int detailH;
        if (detailPanel) {
            int gap = 11;
            detailW = Math.min(250, Math.max(185, innerW * 2 / 5));
            listW = Math.max(220, innerW - detailW - gap);
            detailX = listX + listW + gap;
            detailY = bodyY;
            detailH = bodyH;
            listH = bodyH;
        } else {
            int compactDetailH = bodyH >= 104 ? Math.min(42, Math.max(32, bodyH / 3)) : 0;
            int gap = compactDetailH > 0 ? 6 : 0;
            listW = innerW;
            listH = Math.max(42, bodyH - compactDetailH - gap);
            detailX = innerX;
            detailY = bodyY + listH + gap;
            detailW = innerW;
            detailH = compactDetailH;
        }
        return new Layout(x, y, w, h, compact, quickLabelY, slotStartX, slotY, slotW, slotH, slotGap,
                listX, listY, listW, listH, detailX, detailY, detailW, detailH, detailPanel);
    }

    private AvailableLayout availableLayout(int x, int y, int w, int h) {
        int columns = w >= 300 ? 2 : 1;
        int gapX = columns == 2 ? 6 : 0;
        int gapY = 4;
        int cellH = h >= 92 ? 31 : 26;
        int startX = x + 10;
        int startY = y + 35;
        int usableW = Math.max(20, w - 20);
        int cellW = Math.max(36, (usableW - gapX * (columns - 1)) / columns);
        int pagerReserve = 20;
        int rows = Math.max(1, (Math.max(cellH, h - 35 - pagerReserve) + gapY) / (cellH + gapY));
        int maxRowsByHeight = Math.max(1, (h - 35 - pagerReserve + gapY) / (cellH + gapY));
        rows = Math.min(rows, maxRowsByHeight);
        int pageSize = Math.max(1, columns * rows);
        int pagerY = y + h - 18;
        int centerX = x + w / 2;
        return new AvailableLayout(startX, startY, cellW, cellH, gapX, gapY, columns, rows, pageSize,
                pagerY, centerX, centerX - 48, centerX + 22);
    }

    private boolean shiftDown() {
        return minecraft != null && (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT));
    }

    private AugmentDefinition safe(String id) {
        try { return AugmentCatalog.require(id); }
        catch (RuntimeException ignored) { return null; }
    }

    private String displayName(String id) {
        AugmentDefinition d = safe(id);
        return d == null ? id.substring(Math.max(0, id.indexOf(':') + 1))
                : AugmentPresentationCatalog.get(d).displayName();
    }

    private int tierColor(AugmentDefinition d) {
        return switch (d.tier()) {
            case COMMON -> 0xFFB8BEC5;
            case UNCOMMON -> 0xFF51C982;
            case RARE -> 0xFF4596E4;
            case EPIC -> 0xFFAA58D4;
            case LEGENDARY -> 0xFFE5AA36;
            case MYTHIC -> 0xFFE44B4B;
        };
    }

    private String tierLabel(AugmentDefinition d) {
        return switch (d.tier()) {
            case COMMON -> "일반";
            case UNCOMMON -> "고급";
            case RARE -> "희귀";
            case EPIC -> "영웅";
            case LEGENDARY -> "전설";
            case MYTHIC -> "신화";
        };
    }

    private List<String> wrap(String value, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            if (font.width(word) > maxWidth) {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                StringBuilder part = new StringBuilder();
                for (int offset = 0; offset < word.length();) {
                    int codePoint = word.codePointAt(offset);
                    String next = part.toString() + new String(Character.toChars(codePoint));
                    if (part.length() > 0 && font.width(next) > maxWidth) {
                        lines.add(part.toString());
                        part.setLength(0);
                    }
                    part.appendCodePoint(codePoint);
                    offset += Character.charCount(codePoint);
                }
                line.append(part);
                continue;
            }
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(candidate) <= maxWidth) line = new StringBuilder(candidate);
            else {
                if (line.length() > 0) lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private String clip(String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(value) <= maxWidth) return value;
        String result = value;
        while (result.length() > 1 && font.width(result + "…") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }

    private void centered(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(font, value, x - font.width(value) / 2, y, color, false);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private record Layout(
            int x, int y, int w, int h, boolean compact,
            int quickLabelY, int slotStartX, int slotY, int slotW, int slotH, int slotGap,
            int listX, int listY, int listW, int listH,
            int detailX, int detailY, int detailW, int detailH, boolean detailPanel
    ) { }

    private record AvailableLayout(
            int startX, int startY, int cellW, int cellH, int gapX, int gapY,
            int columns, int rows, int pageSize, int pagerY, int centerX,
            int leftPagerX, int rightPagerX
    ) { }
}
