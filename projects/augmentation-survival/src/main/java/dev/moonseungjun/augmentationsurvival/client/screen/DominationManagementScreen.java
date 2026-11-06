package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.client.EntityIconClient;
import dev.moonseungjun.augmentationsurvival.network.DominationManagementRequestPayload;
import dev.moonseungjun.augmentationsurvival.network.DominationManagementSnapshotPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Compact companion roster. All displayed state comes from the server snapshot. */
public final class DominationManagementScreen extends Screen {
    private static final int SHADE = 0xA8000000, STONE = 0xF022292B, SURFACE = 0xFF303A3B;
    private static final int ALT = 0xFF3A4541, TEXT = 0xFFF1E9D8, MUTED = 0xFFB2B6A8;
    private static final int COPPER = 0xFFBE8058, GREEN = 0xFF8EC6A0;
    private DominationManagementSnapshotPayload snapshot;
    private int page;
    private String selected = "", pendingRelease = "";
    private long pendingReleaseAt;
    private boolean compactDetail;

    public DominationManagementScreen(DominationManagementSnapshotPayload snapshot) {
        super(Component.literal("지배한 동료"));
        this.snapshot = snapshot;
        selectFirst();
        compactDetail = snapshot.companions().size() == 1;
    }

    public void applySnapshot(DominationManagementSnapshotPayload next) {
        snapshot = next;
        if (next.companions().stream().noneMatch(view -> view.uuid().equals(selected))) {
            selected = ""; pendingRelease = ""; selectFirst();
        }
        if (next.companions().isEmpty()) compactDetail = false;
        else if (next.companions().size() == 1) compactDetail = true;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        Layout l = layout();
        g.fill(0, 0, width, height, SHADE);
        g.fill(l.x, l.y, l.x + l.w, l.y + l.h, STONE);
        g.outline(l.x, l.y, l.w, l.h, COPPER);
        g.fill(l.x + 2, l.y + 2, l.x + l.w - 2, l.y + 26, SURFACE);
        g.text(font, "지배한 동료", l.x + 12, l.y + 9, TEXT, false);
        String count = snapshot.occupied() + " / " + snapshot.capacity() + " 슬롯";
        g.text(font, fit(count, l.w - 24), l.x + l.w - font.width(fit(count, l.w - 24)) - 12, l.y + 9, GREEN, false);
        if (snapshot.companions().isEmpty()) drawEmpty(g, l);
        else if (l.compact) {
            if (compactDetail) drawCompactDetail(g, l, mx, my); else drawCompactList(g, l, mx, my);
        } else drawWide(g, l, mx, my);
        String footer = l.compact ? "Shift + 슬롯: 관리 · Esc: 닫기" : "Shift + 지배 슬롯: 관리 · Esc: 닫기";
        g.text(font, fit(footer, l.w - 20), l.x + 10, l.y + l.h - 14, MUTED, false);
    }

    private void drawEmpty(GuiGraphicsExtractor g, Layout l) {
        int cx = l.x + l.w / 2, cy = l.y + l.h / 2;
        g.fill(cx - 23, cy - 26, cx + 23, cy + 14, SURFACE);
        g.outline(cx - 23, cy - 26, 46, 40, 0xFF56605A);
        g.item(new ItemStack(Items.LEAD), cx - 8, cy - 12);
        String label = "지배한 동료가 없습니다.";
        g.text(font, label, cx - font.width(label) / 2, cy + 21, MUTED, false);
    }

    private void drawWide(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        int listX = l.x + 12, listY = l.y + 39, pages = pages(l.rows);
        page = clamp(page, pages - 1);
        for (int row = 0; row < l.rows; row++) {
            int index = page * l.rows + row;
            if (index >= snapshot.companions().size()) break;
            var companion = snapshot.companions().get(index);
            int y = listY + row * l.rowH;
            drawRow(g, companion, listX, y, l.listW, l.rowH - 2, companion.uuid().equals(selected),
                    inside(mx, my, listX, y, l.listW, l.rowH - 2));
        }
        if (pages > 1) drawNav(g, listX, listY + l.rows * l.rowH + 2, l.listW, pages);
        var companion = selected();
        int dx = listX + l.listW + 12, dy = listY, dw = l.w - (dx - l.x) - 12, dh = l.h - 77;
        g.fill(dx, dy, dx + dw, dy + dh, SURFACE); g.outline(dx, dy, dw, dh, 0xFF56605A);
        if (companion == null) return;
        int portrait = Math.min(82, Math.max(44, dh - 28));
        drawPortrait(g, companion, dx + 10, dy + 11, portrait);
        int tx = dx + portrait + 20, textW = Math.max(20, dw - portrait - 30);
        g.text(font, fit(displayName(companion), textW), tx, dy + 13, TEXT, false);
        g.text(font, fit(status(companion), textW), tx, dy + 31, companion.loaded() ? GREEN : MUTED, false);
        g.text(font, "차원 · " + dimensionName(companion.dimension()), tx, dy + 46, MUTED, false);
        drawRelease(g, dx + dw - 126, dy + dh - 34, 112, 24, companion, mx, my);
    }

    private void drawCompactList(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        int x = l.x + 8, y0 = l.y + 32, pages = pages(l.rows);
        page = clamp(page, pages - 1);
        for (int row = 0; row < l.rows; row++) {
            int index = page * l.rows + row;
            if (index >= snapshot.companions().size()) break;
            var companion = snapshot.companions().get(index); int y = y0 + row * l.rowH;
            drawRow(g, companion, x, y, l.w - 16, l.rowH - 1, companion.uuid().equals(selected),
                    inside(mx, my, x, y, l.w - 16, l.rowH - 1));
        }
        if (pages > 1) drawNav(g, x, y0 + l.rows * l.rowH + 1, l.w - 16, pages);
    }

    private void drawCompactDetail(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        var companion = selected(); if (companion == null) { compactDetail = false; return; }
        int bx = l.x + 8, by = l.y + 29;
        g.fill(bx, by, bx + 58, by + 16, ALT); g.outline(bx, by, 58, 16, COPPER);
        g.text(font, "‹ 목록", bx + 5, by + 4, TEXT, false);
        int size = Math.min(42, Math.max(24, l.h - 72)), px = l.x + 9, py = l.y + 49;
        drawPortrait(g, companion, px, py, size);
        int tx = px + size + 8, tw = l.x + l.w - tx - 8;
        g.text(font, fit(displayName(companion), tw), tx, py, TEXT, false);
        g.text(font, fit(status(companion), tw), tx, py + 12, companion.loaded() ? GREEN : MUTED, false);
        if (l.h > 125) g.text(font, fit(dimensionName(companion.dimension()), tw), tx, py + 24, MUTED, false);
        int buttonW = Math.min(112, l.w - 20), buttonH = l.h < 140 ? 17 : 22;
        drawRelease(g, l.x + (l.w - buttonW) / 2, l.y + l.h - (l.h < 140 ? 37 : 46), buttonW, buttonH, companion, mx, my);
    }

    private void drawRow(GuiGraphicsExtractor g, DominationManagementSnapshotPayload.CompanionView c,
                         int x, int y, int w, int h, boolean selected, boolean hover) {
        g.fill(x, y, x + w, y + h, selected ? 0xFF514237 : hover ? 0xFF46504B : SURFACE);
        g.fill(x, y, x + 3, y + h, selected ? COPPER : 0xFF606A62);
        int icon = Math.min(22, h - 2); drawPortrait(g, c, x + 6, y + (h - icon) / 2, icon);
        String state = compactStatus(c); int stateW = font.width(state);
        g.text(font, fit(displayName(c), w - stateW - icon - 28), x + icon + 12, y + Math.max(2, (h - 8) / 2), TEXT, false);
        g.text(font, state, x + w - stateW - 7, y + Math.max(2, (h - 8) / 2), c.loaded() ? GREEN : MUTED, false);
    }

    private void drawPortrait(GuiGraphicsExtractor g, DominationManagementSnapshotPayload.CompanionView c, int x, int y, int size) {
        g.fill(x, y, x + size, y + size, 0xFF263133);
        if (!EntityIconClient.render(g, c.entityId(), x, y, size)) {
            g.item(EntityIconClient.stack(c.entityId()), x + (size - 16) / 2, y + (size - 16) / 2);
        }
    }

    private void drawRelease(GuiGraphicsExtractor g, int x, int y, int w, int h,
                             DominationManagementSnapshotPayload.CompanionView c, int mx, int my) {
        boolean confirm = pendingRelease.equals(c.uuid()) && System.currentTimeMillis() - pendingReleaseAt < 5000L;
        if (!confirm && pendingRelease.equals(c.uuid())) pendingRelease = "";
        g.fill(x, y, x + w, y + h, confirm ? 0xFF75463E : inside(mx, my, x, y, w, h) ? 0xFF57413A : 0xFF493B37);
        g.outline(x, y, w, h, confirm ? 0xFFD78B74 : COPPER);
        String label = confirm ? "다시 눌러 해제" : "지배 해제";
        label = fit(label, w - 10);
        g.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, TEXT, false);
    }

    private void drawNav(GuiGraphicsExtractor g, int x, int y, int w, int pages) {
        String prev = "‹ 이전", next = "다음 ›", label = (page + 1) + " / " + pages;
        g.text(font, prev, x + 3, y, page > 0 ? TEXT : 0xFF737A72, false);
        g.text(font, label, x + (w - font.width(label)) / 2, y, MUTED, false);
        g.text(font, next, x + w - font.width(next) - 3, y, page + 1 < pages ? TEXT : 0xFF737A72, false);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y(); Layout l = layout();
        if (snapshot.companions().isEmpty()) return true;
        if (l.compact) {
            if (compactDetail) {
                if (inside(mx, my, l.x + 8, l.y + 29, 58, 16)) { compactDetail = false; pendingRelease = ""; return true; }
                var c = selected(); if (c != null) {
                    int bw = Math.min(112, l.w - 20), bh = l.h < 140 ? 17 : 22;
                    int bx = l.x + (l.w - bw) / 2, by = l.y + l.h - (l.h < 140 ? 37 : 46);
                    if (inside(mx, my, bx, by, bw, bh)) releaseOrConfirm(c);
                }
                return true;
            }
            int x = l.x + 8, y = l.y + 32, index = page * l.rows + (int) ((my - y) / l.rowH);
            if (inside(mx, my, x, y, l.w - 16, l.rows * l.rowH)) {
                if (index >= 0 && index < snapshot.companions().size()) { selected = snapshot.companions().get(index).uuid(); compactDetail = true; pendingRelease = ""; }
                return true;
            }
            int navY = y + l.rows * l.rowH;
            if (inside(mx, my, x, navY, (l.w - 16) / 3, 16)) page = Math.max(0, page - 1);
            else if (inside(mx, my, x + (l.w - 16) * 2 / 3, navY, (l.w - 16) / 3, 16)) page = Math.min(pages(l.rows) - 1, page + 1);
            return true;
        }
        int x = l.x + 12, y = l.y + 39;
        if (inside(mx, my, x, y, l.listW, l.rows * l.rowH)) {
            int index = page * l.rows + (int) ((my - y) / l.rowH);
            if (index < snapshot.companions().size()) { selected = snapshot.companions().get(index).uuid(); pendingRelease = ""; }
            return true;
        }
        int pages = pages(l.rows), navY = y + l.rows * l.rowH;
        if (pages > 1 && inside(mx, my, x, navY, l.listW / 3, 18)) { page = Math.max(0, page - 1); return true; }
        if (pages > 1 && inside(mx, my, x + l.listW * 2 / 3, navY, l.listW / 3, 18)) { page = Math.min(pages - 1, page + 1); return true; }
        var c = selected(); if (c != null) {
            int dx = x + l.listW + 12, dy = y, dw = l.w - (dx - l.x) - 12, dh = l.h - 77;
            if (inside(mx, my, dx + dw - 126, dy + dh - 34, 112, 24)) { releaseOrConfirm(c); return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int pages = pages(layout().rows);
        if (pages > 1) page = Math.max(0, Math.min(pages - 1, page + (vertical < 0 ? 1 : -1)));
        return true;
    }

    private void releaseOrConfirm(DominationManagementSnapshotPayload.CompanionView c) {
        long now = System.currentTimeMillis();
        if (pendingRelease.equals(c.uuid()) && now - pendingReleaseAt < 5000L) {
            ClientPlayNetworking.send(new DominationManagementRequestPayload(false, c.uuid())); pendingRelease = "";
        } else { pendingRelease = c.uuid(); pendingReleaseAt = now; }
    }
    private void selectFirst() { if (selected.isBlank() && !snapshot.companions().isEmpty()) selected = snapshot.companions().getFirst().uuid(); }
    private DominationManagementSnapshotPayload.CompanionView selected() { return snapshot.companions().stream().filter(c -> c.uuid().equals(selected)).findFirst().orElse(null); }

    private String displayName(DominationManagementSnapshotPayload.CompanionView c) {
        String name = c.name() == null ? "" : c.name().strip();
        if (!name.isBlank() && !name.equals(c.entityId()) && !name.contains(":")) return name;
        Identifier key = Identifier.tryParse(c.entityId());
        var type = key == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
        return type == null ? "동료" : type.getDescription().getString();
    }
    private String status(DominationManagementSnapshotPayload.CompanionView c) {
        String current = Minecraft.getInstance().level == null ? "" : Minecraft.getInstance().level.dimension().identifier().toString();
        if (!c.loaded()) return !current.isBlank() && !current.equals(c.dimension()) ? "다른 차원 · 미로드" : "미로드";
        return current.equals(c.dimension()) ? "이 차원에서 활동" : "다른 차원에서 활동";
    }
    private String compactStatus(DominationManagementSnapshotPayload.CompanionView c) {
        if (!c.loaded()) return "미로드";
        String current = Minecraft.getInstance().level == null ? "" : Minecraft.getInstance().level.dimension().identifier().toString();
        return current.equals(c.dimension()) ? "활동" : "다른 차원";
    }
    private static String dimensionName(String id) {
        return switch (id) { case "minecraft:overworld" -> "오버월드"; case "minecraft:the_nether" -> "네더"; case "minecraft:the_end" -> "엔드"; default -> { int i = id.indexOf(':'); yield (i >= 0 ? id.substring(i + 1) : id).replace('_', ' '); } };
    }
    private int pages(int rows) { return Math.max(1, (snapshot.companions().size() + rows - 1) / rows); }
    private int clamp(int value, int max) { return Math.max(0, Math.min(value, max)); }
    private String fit(String value, int maxWidth) {
        if (maxWidth <= 0) return ""; String text = value;
        while (!text.isEmpty() && font.width(text + "…") > maxWidth) text = text.substring(0, text.length() - 1);
        return text.length() == value.length() ? value : text + "…";
    }
    private Layout layout() {
        int w = Math.max(1, Math.min(760, width - 16)); boolean empty = snapshot.companions().isEmpty();
        int wanted = empty || snapshot.companions().size() == 1 ? 164 : 440;
        int h = Math.max(1, Math.min(wanted, height - 16)); int x = (width - w) / 2, y = (height - h) / 2;
        boolean compact = w < 500 || h < 300;
        int rowH = compact ? Math.max(19, Math.min(26, (h - 72) / 3)) : 30;
        int rows = compact ? Math.max(1, Math.min(9, (h - 65) / rowH)) : Math.max(1, Math.min(10, (h - 88) / rowH));
        int listW = compact ? w - 24 : Math.max(180, (w - 36) * 43 / 100);
        return new Layout(x, y, w, h, compact, rowH, rows, listW);
    }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    @Override public boolean isPauseScreen() { return false; }
    private record Layout(int x, int y, int w, int h, boolean compact, int rowH, int rows, int listW) { }
}
