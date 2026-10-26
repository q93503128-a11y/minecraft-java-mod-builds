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

/**
 * Server-backed active augment loadout screen.
 * Visual hierarchy follows an arsenal/loadout surface: equipped sockets first, owned actives second, details last.
 */
public final class ActiveLoadoutScreen extends Screen {
    private static final int SCRIM = 0xA0000000;
    private static final int BACK = 0xF10B0F12;
    private static final int BACK_TOP = 0xFF141A1E;
    private static final int RAIL = 0xFF20292E;
    private static final int SURFACE = 0xFF11171A;
    private static final int SURFACE_HI = 0xFF182126;
    private static final int LINE = 0xFF344148;
    private static final int LINE_SOFT = 0xFF273238;
    private static final int LINE_HI = 0xFF7E8D95;
    private static final int TEXT = 0xFFF2F4F3;
    private static final int MUTED = 0xFFA3ADB0;
    private static final int FAINT = 0xFF677176;
    private static final int ACCENT = 0xFF8FC7C3;
    private static final int GOLD = 0xFFE0C279;

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

        g.fill(0, 0, width, height, SCRIM);
        drawShell(g, layout);

        drawHeader(g, layout);
        drawQuickbar(g, layout, mouseX, mouseY);
        drawAvailablePanel(g, layout.listX(), layout.listY(), layout.listW(), layout.listH(), mouseX, mouseY);

        String inspect = inspectId();
        if (layout.detailPanel()) {
            drawDetailPanel(g, layout.detailX(), layout.detailY(), layout.detailW(), layout.detailH(), inspect);
        } else if (layout.detailH() > 0) {
            drawCompactDetail(g, layout.detailX(), layout.detailY(), layout.detailW(), layout.detailH(), inspect);
        }

        String help = selectedSlot >= 0
                ? "보유 액티브 선택: 장착 · 다른 슬롯 선택: 교환 · 우클릭: 해제"
                : !selectedAugmentId.isBlank()
                    ? "상단 슬롯을 선택해 장착 · Shift: 정확한 수치"
                    : "슬롯 또는 보유 액티브 선택 · Shift: 정확한 수치";
        centered(g, clip(help, layout.w() - 28), layout.x() + layout.w() / 2, layout.y() + layout.h() - 14, MUTED);
    }

    private void drawShell(GuiGraphicsExtractor g, Layout l) {
        g.fill(l.x(), l.y(), l.x() + l.w(), l.y() + l.h(), BACK);
        g.outline(l.x(), l.y(), l.w(), l.h(), 0xFF48545A);
        g.fill(l.x() + 1, l.y() + 1, l.x() + l.w() - 1, l.y() + 32, BACK_TOP);
        g.fill(l.x() + 2, l.y() + 2, l.x() + l.w() - 2, l.y() + 4, ACCENT);

        int railY = l.slotY() - 6;
        g.fill(l.x() + 10, railY, l.x() + l.w() - 10, l.slotY() + l.slotH() + 6, 0xC9101518);
        g.outline(l.x() + 10, railY, l.w() - 20, l.slotH() + 12, LINE_SOFT);

        drawCorner(g, l.x() + 4, l.y() + 4, 1, 1);
        drawCorner(g, l.x() + l.w() - 5, l.y() + 4, -1, 1);
        drawCorner(g, l.x() + 4, l.y() + l.h() - 5, 1, -1);
        drawCorner(g, l.x() + l.w() - 5, l.y() + l.h() - 5, -1, -1);
    }

    private void drawCorner(GuiGraphicsExtractor g, int x, int y, int dx, int dy) {
        int hx = x + dx * 11;
        int hy = y + dy * 2;
        int vx = x + dx * 2;
        int vy = y + dy * 11;
        g.fill(Math.min(x, hx), Math.min(y, hy), Math.max(x, hx), Math.max(y, hy), LINE_HI);
        g.fill(Math.min(x, vx), Math.min(y, vy), Math.max(x, vx), Math.max(y, vy), LINE_HI);
    }

    private void drawHeader(GuiGraphicsExtractor g, Layout l) {
        int half = Math.max(70, l.w() / 2 - 24);
        g.text(font, clip("전투 증강 편성", half), l.x() + 13, l.y() + 11, TEXT, false);

        int equipped = 0;
        for (String id : ClientActiveLoadoutState.slots()) if (!id.isBlank()) equipped++;
        String status = equipped + " / 10 장착";
        if (!l.compact() && l.w() >= 500) status += "  ·  Alt: 증강 모드 ON/OFF";
        status = clip(status, half);
        g.text(font, status, l.x() + l.w() - 13 - font.width(status), l.y() + 11, MUTED, false);
    }

    private String inspectId() {
        if (!hoveredId.isBlank()) return hoveredId;
        if (!selectedAugmentId.isBlank()) return selectedAugmentId;
        if (selectedSlot >= 0) return ClientActiveLoadoutState.slot(selectedSlot);
        return "";
    }

    private void drawQuickbar(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        g.text(font, "QUICK SLOTS", l.x() + 13, l.quickLabelY(), FAINT, false);
        if (l.w() >= 420) {
            String hint = "Alt ON → 숫자키 1~0";
            g.text(font, hint, l.x() + l.w() - 13 - font.width(hint), l.quickLabelY(), MUTED, false);
        }

        for (int i = 0; i < 10; i++) {
            int sx = l.slotStartX() + i * (l.slotW() + l.slotGap());
            int sy = l.slotY();
            String id = ClientActiveLoadoutState.slot(i);
            boolean hover = inside(mx, my, sx, sy, l.slotW(), l.slotH());
            boolean selected = selectedSlot == i;
            AugmentDefinition d = id.isBlank() ? null : safe(id);
            int color = d == null ? ACCENT : tierColor(d);

            if (selected) {
                g.fill(sx - 2, sy - 2, sx + l.slotW() + 2, sy + l.slotH() + 2, withAlpha(color, 0x28));
                g.outline(sx - 2, sy - 2, l.slotW() + 4, l.slotH() + 4, color);
            }
            g.fill(sx, sy, sx + l.slotW(), sy + l.slotH(), hover ? SURFACE_HI : SURFACE);
            g.outline(sx, sy, l.slotW(), l.slotH(), hover ? LINE_HI : LINE);

            int chipW = Math.min(14, Math.max(10, l.slotW() / 3));
            g.fill(sx + 2, sy + 2, sx + 2 + chipW, sy + 12, selected ? color : RAIL);
            String key = i == 9 ? "0" : Integer.toString(i + 1);
            centered(g, key, sx + 2 + chipW / 2, sy + 4, selected ? 0xFF071012 : MUTED);

            if (d == null) {
                centered(g, "·", sx + l.slotW() / 2, sy + Math.max(12, l.slotH() / 2 - 3), FAINT);
            } else {
                int iconY = sy + Math.max(11, (l.slotH() - 16) / 2);
                g.item(AugmentIconClient.stackFor(id, d.tier()), sx + l.slotW() / 2 - 8, iconY);
                g.fill(sx + 4, sy + l.slotH() - 4, sx + l.slotW() - 4, sy + l.slotH() - 2, color);
                if (hover) hoveredId = id;
            }
        }
    }

    private void drawAvailablePanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int mx, int my) {
        drawPanel(g, x, y, w, h, "보유 액티브", ClientActiveLoadoutState.available().size() + "개");
        List<String> available = ClientActiveLoadoutState.available();
        AvailableLayout grid = availableLayout(x, y, w, h);
        int pages = Math.max(1, (available.size() + grid.pageSize() - 1) / grid.pageSize());
        page = Math.max(0, Math.min(page, pages - 1));

        if (available.isEmpty()) {
            g.text(font, clip("아직 편성 가능한 액티브 증강이 없습니다.", w - 24), x + 12, y + 44, TEXT, false);
            if (h >= 78) g.text(font, clip("패시브 증강은 장착 없이 항상 적용됩니다.", w - 24), x + 12, y + 59, MUTED, false);
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

        if (pages > 1) drawPager(g, grid, page, pages, mx, my);
    }

    private void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title, String value) {
        g.fill(x, y, x + w, y + h, 0xEC0D1215);
        g.outline(x, y, w, h, LINE);
        g.fill(x + 1, y + 1, x + w - 1, y + 28, 0xFF141B1F);
        g.fill(x + 10, y + 27, x + w - 10, y + 28, LINE_SOFT);
        g.text(font, title, x + 10, y + 9, TEXT, false);
        if (value != null && !value.isBlank()) {
            g.text(font, value, x + w - 10 - font.width(value), y + 9, MUTED, false);
        }
    }

    private void drawAvailableCell(GuiGraphicsExtractor g, int x, int y, int w, int h, String id, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        boolean selected = id.equals(selectedAugmentId);
        boolean equipped = ClientActiveLoadoutState.slots().contains(id);
        AugmentDefinition d = safe(id);
        int accent = d == null ? LINE_HI : tierColor(d);

        if (selected) g.fill(x - 1, y - 1, x + w + 1, y + h + 1, withAlpha(accent, 0x24));
        g.fill(x, y, x + w, y + h, selected ? 0xFF172226 : hover ? SURFACE_HI : SURFACE);
        g.fill(x, y, x + 3, y + h, accent);
        g.outline(x, y, w, h, selected ? accent : hover ? LINE_HI : LINE_SOFT);

        if (d != null && w >= 50) {
            g.fill(x + 7, y + Math.max(5, (h - 20) / 2), x + 27, y + Math.max(5, (h - 20) / 2) + 20, 0xFF0C1114);
            g.item(AugmentIconClient.stackFor(id, d.tier()), x + 9, y + Math.max(7, (h - 16) / 2));
        }
        int textX = x + (w >= 50 ? 34 : 7);
        int reserve = equipped ? 54 : 8;
        g.text(font, clip(displayName(id), Math.max(18, w - (textX - x) - reserve)), textX, y + 5,
                hover || selected ? TEXT : 0xFFD6DDDE, false);
        if (h >= 28) {
            int level = Math.max(1, ClientAugmentProgressState.level(id));
            g.text(font, "Lv." + level, textX, y + 17, MUTED, false);
        }
        if (equipped && w >= 96) {
            String tag = "장착";
            g.fill(x + w - 36, y + 7, x + w - 7, y + 20, 0xFF1D2E2D);
            centered(g, tag, x + w - 21, y + 10, ACCENT);
        }
        if (hover) hoveredId = id;
    }

    private void drawPager(GuiGraphicsExtractor g, AvailableLayout grid, int current, int pages, int mx, int my) {
        boolean left = current > 0 && inside(mx, my, grid.leftPagerX(), grid.pagerY(), 26, 16);
        boolean right = current + 1 < pages && inside(mx, my, grid.rightPagerX(), grid.pagerY(), 26, 16);
        g.text(font, "‹", grid.leftPagerX() + 9, grid.pagerY() + 3, left ? TEXT : FAINT, false);
        centered(g, (current + 1) + " / " + pages, grid.centerX(), grid.pagerY() + 3, MUTED);
        g.text(font, "›", grid.rightPagerX() + 9, grid.pagerY() + 3, right ? TEXT : FAINT, false);
    }

    private void drawDetailPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, String id) {
        drawPanel(g, x, y, w, h, "증강 정보", "");
        if (id == null || id.isBlank()) {
            g.text(font, clip("보유 액티브나 슬롯에 마우스를 올리세요.", w - 24), x + 12, y + 45, MUTED, false);
            if (h >= 84) g.text(font, clip("선택한 액티브는 상단 슬롯에 배치할 수 있습니다.", w - 24), x + 12, y + 60, FAINT, false);
            return;
        }

        AugmentDefinition d = safe(id);
        if (d == null) return;
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int level = Math.max(1, ClientAugmentProgressState.level(id));
        int color = tierColor(d);

        g.fill(x + 1, y + 29, x + 5, y + h - 1, color);
        g.fill(x + 11, y + 38, x + 43, y + 70, 0xFF0A0F12);
        g.outline(x + 11, y + 38, 32, 32, shade(color, 0.78));
        g.item(AugmentIconClient.stackFor(id, d.tier()), x + 19, y + 46);

        int textX = x + 51;
        g.text(font, clip(p.displayName(), Math.max(28, w - 62)), textX, y + 40, TEXT, false);
        String meta = tierLabel(d) + " · " + p.kind().label() + " · Lv." + level + "/" + d.maxLevel();
        g.text(font, clip(meta, Math.max(28, w - 62)), textX, y + 54, color, false);

        int descY = y + 80;
        boolean detail = shiftDown();
        List<String> lines = wrap(detail ? p.detail() : p.summary(), Math.max(24, w - 24));
        int maxLines = Math.max(1, (h - 111) / 11);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            String line = lines.get(i);
            if (i == maxLines - 1 && lines.size() > maxLines) line = clip(line + "…", w - 24);
            g.text(font, line, x + 12, descY + i * 11, 0xFFD2D8D9, false);
        }

        if (h >= 96) {
            String footer = detail ? "Shift · 상세 수치 표시 중" : "Shift · 정확한 수치";
            g.text(font, clip(footer, w - 24), x + 12, y + h - 15, detail ? color : FAINT, false);
        }
    }

    private void drawCompactDetail(GuiGraphicsExtractor g, int x, int y, int w, int h, String id) {
        g.fill(x, y, x + w, y + h, 0xED0D1215);
        g.outline(x, y, w, h, LINE);
        if (id == null || id.isBlank()) {
            g.text(font, clip("마우스로 효과 확인 · Shift 상세", w - 18), x + 9, y + Math.max(5, h / 2 - 4), MUTED, false);
            return;
        }
        AugmentDefinition d = safe(id);
        if (d == null) return;
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int color = tierColor(d);
        g.fill(x, y, x + 3, y + h, color);
        String title = p.displayName() + " · " + tierLabel(d) + " · Lv." + Math.max(1, ClientAugmentProgressState.level(id));
        g.text(font, clip(title, w - 20), x + 9, y + 5, TEXT, false);
        if (h >= 31) {
            String value = shiftDown() ? p.detail() : p.summary();
            g.text(font, clip(value, w - 20), x + 9, y + 18, 0xFFC8D0D2, false);
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
                    ClientPlayNetworking.send(new ActiveLoadoutEditPayload(
                            ActiveLoadoutEditPayload.ASSIGN, i, 0, selectedAugmentId));
                    selectedAugmentId = "";
                    selectedSlot = -1;
                } else if (selectedSlot >= 0 && selectedSlot != i) {
                    ClientPlayNetworking.send(new ActiveLoadoutEditPayload(
                            ActiveLoadoutEditPayload.SWAP, selectedSlot, i, ""));
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
                ClientPlayNetworking.send(new ActiveLoadoutEditPayload(
                        ActiveLoadoutEditPayload.ASSIGN, selectedSlot, 0, id));
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
        int slotBudget = Math.max(1, w - 28);
        int rawSlotW = Math.max(1, (slotBudget - slotGap * 9) / 10);
        int slotW = Math.min(49, Math.max(10, rawSlotW));
        if (slotW * 10 + slotGap * 9 > slotBudget) slotW = rawSlotW;
        int slotH = compact ? Math.max(26, Math.min(36, h / 7)) : 48;
        int slotTotal = slotW * 10 + slotGap * 9;
        int slotStartX = x + (w - slotTotal) / 2;
        int slotY = y + 53;

        int bodyY = slotY + slotH + 11;
        int footerY = y + h - 24;
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
            detailW = Math.min(254, Math.max(188, innerW * 2 / 5));
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

        return new Layout(x, y, w, h, compact, quickLabelY,
                slotStartX, slotY, slotW, slotH, slotGap,
                listX, listY, listW, listH,
                detailX, detailY, detailW, detailH, detailPanel);
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
        int maxRows = Math.max(1, (h - 35 - pagerReserve + gapY) / (cellH + gapY));
        int pageSize = Math.max(1, columns * maxRows);
        int pagerY = y + h - 18;
        int centerX = x + w / 2;
        return new AvailableLayout(startX, startY, cellW, cellH, gapX, gapY,
                columns, maxRows, pageSize, pagerY, centerX, centerX - 48, centerX + 22);
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
        return ellipsize(value, maxWidth);
    }

    private String ellipsize(String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        String result = value;
        while (!result.isEmpty() && font.width(result + "…") > maxWidth) {
            int end = result.offsetByCodePoints(result.length(), -1);
            result = result.substring(0, end);
        }
        return result.isEmpty() ? "…" : result + "…";
    }

    private void centered(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(font, value, x - font.width(value) / 2, y, color, false);
    }

    private static int shade(int color, double factor) {
        int r = (int) (((color >> 16) & 255) * factor);
        int gg = (int) (((color >> 8) & 255) * factor);
        int b = (int) ((color & 255) * factor);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
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
