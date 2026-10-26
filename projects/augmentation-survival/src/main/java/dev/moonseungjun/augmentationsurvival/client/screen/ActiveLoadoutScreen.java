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

/** Server-backed active augment loadout screen designed around the actual 10-slot combat flow. */
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
    private static final int PAGE_SIZE = 12;

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
        int w = Math.min(720, width - 24);
        int h = Math.min(390, height - 24);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        hoveredId = "";

        g.fill(0, 0, width, height, 0x8B000000);
        g.fill(x, y, x + w, y + h, BG);
        g.outline(x, y, w, h, 0xFF46535B);
        g.fill(x + 1, y + 1, x + w - 1, y + 32, 0xFF141C22);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, 0xFF76858E);
        g.text(font, "전투 증강 편성", x + 15, y + 11, TEXT, false);
        String mode = "ALT + 1~0로 사용";
        g.text(font, mode, x + w - 15 - font.width(mode), y + 11, MUTED, false);

        drawQuickbar(g, x, y, w, mouseX, mouseY);

        int bodyY = y + 124;
        int margin = 15;
        int gap = 14;
        int listW = Math.min(400, (w - margin * 2 - gap) * 3 / 5);
        int detailX = x + margin + listW + gap;
        int detailW = x + w - margin - detailX;
        int bodyH = h - 151;

        drawAvailablePanel(g, x + margin, bodyY, listW, bodyH, mouseX, mouseY);

        String inspect = !hoveredId.isBlank() ? hoveredId
                : !selectedAugmentId.isBlank() ? selectedAugmentId
                : selectedSlot >= 0 ? ClientActiveLoadoutState.slot(selectedSlot) : "";
        drawDetailPanel(g, detailX, bodyY, detailW, bodyH, inspect);

        String help = selectedSlot >= 0
                ? "보유 액티브를 클릭해 슬롯에 장착 · 다른 슬롯 클릭 시 위치 교환 · 우클릭 해제"
                : !selectedAugmentId.isBlank()
                    ? "상단 슬롯을 클릭해 장착 · Shift: 정확한 수치"
                    : "슬롯 또는 보유 액티브를 먼저 선택 · Shift: 정확한 수치";
        g.text(font, help, x + 15, y + h - 17, MUTED, false);
    }

    private void drawQuickbar(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        g.text(font, "빠른 사용 슬롯", x + 15, y + 44, 0xFFC9D2D7, false);
        String hint = "평상시 핫바는 그대로 유지됩니다";
        g.text(font, hint, x + w - 15 - font.width(hint), y + 44, FAINT, false);

        int slotW = 49, slotH = 46, gap = 5;
        int total = 10 * slotW + 9 * gap;
        int startX = x + (w - total) / 2;
        int sy = y + 60;
        for (int i = 0; i < 10; i++) {
            int sx = startX + i * (slotW + gap);
            String id = ClientActiveLoadoutState.slot(i);
            boolean hover = inside(mx, my, sx, sy, slotW, slotH);
            boolean selected = selectedSlot == i;
            int bg = selected ? 0xFF17262D : hover ? SURFACE_HI : SURFACE;
            g.fill(sx, sy, sx + slotW, sy + slotH, bg);
            g.outline(sx, sy, slotW, slotH, selected ? ACCENT : hover ? LINE_HI : LINE);
            String key = i == 9 ? "0" : Integer.toString(i + 1);
            g.text(font, key, sx + 4, sy + 4, selected ? ACCENT : MUTED, false);
            if (id.isBlank()) {
                centered(g, "—", sx + slotW / 2, sy + 19, FAINT);
            } else {
                AugmentDefinition d = safe(id);
                if (d != null) {
                    g.item(AugmentIconClient.stackFor(id, d.tier()), sx + slotW / 2 - 8, sy + 12);
                    g.fill(sx + 7, sy + slotH - 4, sx + slotW - 7, sy + slotH - 2, tierColor(d));
                }
                if (hover) hoveredId = id;
            }
        }
    }

    private void drawAvailablePanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int mx, int my) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, LINE);
        List<String> available = ClientActiveLoadoutState.available();
        int pages = Math.max(1, (available.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));

        g.text(font, "보유 액티브", x + 12, y + 10, TEXT, false);
        String count = available.size() + "개";
        g.text(font, count, x + w - 12 - font.width(count), y + 10, MUTED, false);
        g.fill(x + 12, y + 27, x + w - 12, y + 28, 0xFF273138);

        if (available.isEmpty()) {
            g.text(font, "아직 액티브 증강을 획득하지 않았습니다.", x + 13, y + 48, TEXT, false);
            g.text(font, "패시브 증강은 슬롯에 장착하지 않아도 항상 적용됩니다.", x + 13, y + 67, MUTED, false);
            return;
        }

        int start = page * PAGE_SIZE;
        int end = Math.min(available.size(), start + PAGE_SIZE);
        int gapX = 7, gapY = 5;
        int cellW = (w - 24 - gapX) / 2;
        int cellH = 31;
        int startY = y + 38;
        for (int i = start; i < end; i++) {
            int local = i - start, col = local % 2, row = local / 2;
            int cx = x + 12 + col * (cellW + gapX);
            int cy = startY + row * (cellH + gapY);
            drawAvailableCell(g, cx, cy, cellW, cellH, available.get(i), mx, my);
        }

        if (pages > 1) {
            String pager = "‹  " + (page + 1) + " / " + pages + "  ›";
            centered(g, pager, x + w / 2, y + h - 18, MUTED);
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
        if (d != null) g.item(AugmentIconClient.stackFor(id, d.tier()), x + 8, y + 7);
        g.text(font, clip(displayName(id), w - 65), x + 31, y + 6, hover || selected ? TEXT : 0xFFD2DADF, false);
        String sub = equipped ? "장착 중" : "Lv." + Math.max(1, ClientAugmentProgressState.level(id));
        g.text(font, sub, x + 31, y + 18, equipped ? ACCENT : MUTED, false);
        if (hover) hoveredId = id;
    }

    private void drawDetailPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, String id) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, LINE);
        if (id == null || id.isBlank()) {
            g.text(font, "증강 정보", x + 13, y + 11, TEXT, false);
            g.fill(x + 13, y + 29, x + w - 13, y + 30, 0xFF273138);
            g.text(font, "보유 액티브나 슬롯에 마우스를 올려", x + 13, y + 52, MUTED, false);
            g.text(font, "효과와 정확한 수치를 확인할 수 있습니다.", x + 13, y + 66, MUTED, false);
            return;
        }
        AugmentDefinition d = safe(id);
        if (d == null) return;
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int level = Math.max(1, ClientAugmentProgressState.level(id));
        int color = tierColor(d);

        g.fill(x, y, x + 4, y + h, color);
        g.item(AugmentIconClient.stackFor(id, d.tier()), x + 14, y + 12);
        g.text(font, p.displayName(), x + 38, y + 10, TEXT, false);
        String meta = tierLabel(d) + " · " + p.kind().label() + " · Lv." + level + "/" + d.maxLevel();
        g.text(font, meta, x + 38, y + 23, MUTED, false);
        g.fill(x + 13, y + 42, x + w - 13, y + 43, 0xFF293239);

        boolean detail = shiftDown();
        String value = detail ? p.detail() : p.summary();
        List<String> lines = wrap(value, w - 28);
        int maxLines = Math.max(1, (h - 78) / 12);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            g.text(font, lines.get(i), x + 14, y + 55 + i * 12, i == 0 ? 0xFFDCE3E7 : 0xFFC2CBD0, false);
        }
        String footer = detail ? "Shift 상세 표시 중" : "Shift: 정확한 수치";
        g.text(font, footer, x + 14, y + h - 17, detail ? color : FAINT, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int w = Math.min(720, width - 24);
        int h = Math.min(390, height - 24);
        int x = (width - w) / 2;
        int y = (height - h) / 2;

        int slotW = 49, slotH = 46, slotGap = 5;
        int total = 10 * slotW + 9 * slotGap;
        int startX = x + (w - total) / 2;
        int slotY = y + 60;
        for (int i = 0; i < 10; i++) {
            int sx = startX + i * (slotW + slotGap);
            if (!inside(event.x(), event.y(), sx, slotY, slotW, slotH)) continue;
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
        int bodyY = y + 124, margin = 15, gap = 14;
        int listW = Math.min(400, (w - margin * 2 - gap) * 3 / 5);
        int listX = x + margin;
        int bodyH = h - 151;
        List<String> available = ClientActiveLoadoutState.available();
        int pages = Math.max(1, (available.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int start = page * PAGE_SIZE;
        int end = Math.min(available.size(), start + PAGE_SIZE);
        int gapX = 7, gapY = 5;
        int cellW = (listW - 24 - gapX) / 2, cellH = 31;
        int startY = bodyY + 38;
        for (int i = start; i < end; i++) {
            int local = i - start, col = local % 2, row = local / 2;
            int cx = listX + 12 + col * (cellW + gapX);
            int cy = startY + row * (cellH + gapY);
            if (!inside(event.x(), event.y(), cx, cy, cellW, cellH)) continue;
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
        if (pages > 1 && event.y() >= bodyY + bodyH - 30) {
            int center = listX + listW / 2;
            if (event.x() < center && page > 0) { page--; return true; }
            if (event.x() >= center && page + 1 < pages) { page++; return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
            case MYTHIC -> 0xFFE54C76;
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
}
