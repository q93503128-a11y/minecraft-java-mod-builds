package dev.moonseungjun.augmentationsurvival.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.client.AugmentIconClient;
import dev.moonseungjun.augmentationsurvival.client.ClientActiveLoadoutState;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import dev.moonseungjun.augmentationsurvival.client.screen.ActiveLoadoutScreen;
import dev.moonseungjun.augmentationsurvival.network.ActiveLoadoutEditPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces the cramped compact loadout presentation while retaining the existing server-backed edit payloads. */
@Mixin(value = ActiveLoadoutScreen.class, priority = 1300)
public abstract class ActiveLoadoutUiRescueMixin extends Screen {
    private static final int SCRIM = 0xA0000000;
    private static final int BACK = 0xF10B0F12;
    private static final int TOP = 0xFF141A1E;
    private static final int SURFACE = 0xFF11171A;
    private static final int SURFACE_HI = 0xFF182126;
    private static final int DEEP = 0xFF0A0F12;
    private static final int LINE = 0xFF344148;
    private static final int LINE_SOFT = 0xFF273238;
    private static final int LINE_HI = 0xFF7E8D95;
    private static final int TEXT = 0xFFF2F4F3;
    private static final int MUTED = 0xFFA3ADB0;
    private static final int FAINT = 0xFF677176;
    private static final int ACCENT = 0xFF8FC7C3;
    private static final int GOLD = 0xFFE0C279;

    @Shadow private int selectedSlot;
    @Shadow private String selectedAugmentId;
    @Shadow private String hoveredId;
    @Shadow private int page;

    @Unique private String augmentationSurvival$pendingHover = "";
    @Unique private long augmentationSurvival$hoverSince = Long.MIN_VALUE;

    protected ActiveLoadoutUiRescueMixin() {
        super(Component.literal("전투 증강 편성"));
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$render(
            GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci
    ) {
        ci.cancel();
        LoadoutGeometry l = augmentationSurvival$layout();
        hoveredId = "";

        g.fill(0, 0, width, height, SCRIM);
        g.fill(l.x(), l.y(), l.x() + l.w(), l.y() + l.h(), BACK);
        g.outline(l.x(), l.y(), l.w(), l.h(), 0xFF48545A);
        g.fill(l.x() + 1, l.y() + 1, l.x() + l.w() - 1, l.y() + 31, TOP);
        g.fill(l.x() + 2, l.y() + 2, l.x() + l.w() - 2, l.y() + 4, ACCENT);
        augmentationSurvival$corner(g, l.x() + 4, l.y() + 4, 1, 1);
        augmentationSurvival$corner(g, l.x() + l.w() - 5, l.y() + 4, -1, 1);
        augmentationSurvival$corner(g, l.x() + 4, l.y() + l.h() - 5, 1, -1);
        augmentationSurvival$corner(g, l.x() + l.w() - 5, l.y() + l.h() - 5, -1, -1);

        g.text(font, "전투 증강 편성", l.x() + 13, l.y() + 11, TEXT, false);
        int equipped = 0;
        for (String id : ClientActiveLoadoutState.slots()) if (!id.isBlank()) equipped++;
        String status = equipped + " / 10 장착";
        g.text(font, status, l.x() + l.w() - 13 - font.width(status), l.y() + 11, MUTED, false);

        augmentationSurvival$quickSlots(g, l, mouseX, mouseY);
        augmentationSurvival$available(g, l, mouseX, mouseY);

        long now = System.currentTimeMillis();
        if (!hoveredId.equals(augmentationSurvival$pendingHover)) {
            augmentationSurvival$pendingHover = hoveredId;
            augmentationSurvival$hoverSince = now;
        }
        if (!hoveredId.isBlank() && now - augmentationSurvival$hoverSince >= 80L) {
            augmentationSurvival$popover(g, hoveredId, mouseX, mouseY, l);
        }

        String help = selectedSlot >= 0
                ? "보유 액티브 선택: 장착 · 다른 슬롯: 교환 · 우클릭: 해제"
                : !selectedAugmentId.isBlank()
                    ? "상단 슬롯 선택: 장착 · 마우스: 효과 · Shift: 정확한 수치"
                    : "보유 액티브 → 슬롯 장착 · Alt 토글 후 1~0 사용 · Shift: 정확한 수치";
        centered(g, fit(help, l.w() - 28), l.x() + l.w() / 2, l.y() + l.h() - 14, MUTED);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$click(
            MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir
    ) {
        LoadoutGeometry l = augmentationSurvival$layout();
        for (int i = 0; i < 10; i++) {
            int sx = l.slotStartX() + i * (l.slotW() + l.slotGap());
            if (!inside(event.x(), event.y(), sx, l.slotY(), l.slotW(), l.slotH())) continue;
            if (event.button() == 1) {
                ClientPlayNetworking.send(new ActiveLoadoutEditPayload(ActiveLoadoutEditPayload.CLEAR, i, 0, ""));
                if (selectedSlot == i) selectedSlot = -1;
                cir.setReturnValue(true);
                return;
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
                cir.setReturnValue(true);
                return;
            }
        }
        if (event.button() != 0) return;

        List<String> available = ClientActiveLoadoutState.available();
        GridGeometry grid = augmentationSurvival$grid(l);
        int pages = Math.max(1, (available.size() + grid.pageSize() - 1) / grid.pageSize());
        page = Math.max(0, Math.min(page, pages - 1));
        int start = page * grid.pageSize();
        int end = Math.min(available.size(), start + grid.pageSize());
        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % grid.columns();
            int row = local / grid.columns();
            int x = grid.left() + col * (grid.cellW() + grid.gapX());
            int y = grid.top() + row * (grid.cellH() + grid.gapY());
            if (!inside(event.x(), event.y(), x, y, grid.cellW(), grid.cellH())) continue;
            String id = available.get(i);
            if (selectedSlot >= 0) {
                ClientPlayNetworking.send(new ActiveLoadoutEditPayload(
                        ActiveLoadoutEditPayload.ASSIGN, selectedSlot, 0, id));
                selectedSlot = -1;
                selectedAugmentId = "";
            } else {
                selectedAugmentId = id.equals(selectedAugmentId) ? "" : id;
            }
            cir.setReturnValue(true);
            return;
        }
        if (pages > 1 && inside(event.x(), event.y(), grid.leftPagerX(), grid.pagerY(), 28, 16) && page > 0) {
            page--;
            cir.setReturnValue(true);
            return;
        }
        if (pages > 1 && inside(event.x(), event.y(), grid.rightPagerX(), grid.pagerY(), 28, 16) && page + 1 < pages) {
            page++;
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void augmentationSurvival$quickSlots(GuiGraphicsExtractor g, LoadoutGeometry l, int mx, int my) {
        g.text(font, "QUICK SLOTS", l.x() + 13, l.quickLabelY(), FAINT, false);
        if (l.w() >= 390) {
            String hint = "Alt ON/OFF → 숫자키 1~0";
            g.text(font, hint, l.x() + l.w() - 13 - font.width(hint), l.quickLabelY(), MUTED, false);
        }
        int railY = l.slotY() - 5;
        g.fill(l.x() + 10, railY, l.x() + l.w() - 10, l.slotY() + l.slotH() + 5, 0xC9101518);
        g.outline(l.x() + 10, railY, l.w() - 20, l.slotH() + 10, LINE_SOFT);

        for (int i = 0; i < 10; i++) {
            int x = l.slotStartX() + i * (l.slotW() + l.slotGap());
            int y = l.slotY();
            String id = ClientActiveLoadoutState.slot(i);
            boolean hover = inside(mx, my, x, y, l.slotW(), l.slotH());
            boolean selected = selectedSlot == i;
            AugmentDefinition d = id.isBlank() ? null : safe(id);
            int color = d == null ? ACCENT : tierColor(d);
            if (selected) {
                g.fill(x - 2, y - 2, x + l.slotW() + 2, y + l.slotH() + 2, withAlpha(color, 0x28));
                g.outline(x - 2, y - 2, l.slotW() + 4, l.slotH() + 4, color);
            }
            g.fill(x, y, x + l.slotW(), y + l.slotH(), hover ? SURFACE_HI : SURFACE);
            g.outline(x, y, l.slotW(), l.slotH(), hover ? LINE_HI : LINE);
            int chipW = Math.min(14, Math.max(10, l.slotW() / 3));
            g.fill(x + 2, y + 2, x + 2 + chipW, y + 12, selected ? color : 0xFF20292E);
            centered(g, i == 9 ? "0" : Integer.toString(i + 1), x + 2 + chipW / 2, y + 4,
                    selected ? 0xFF071012 : MUTED);
            if (d != null) {
                g.item(AugmentIconClient.stackFor(id, d.tier()), x + l.slotW() / 2 - 8,
                        y + Math.max(11, (l.slotH() - 16) / 2));
                g.fill(x + 4, y + l.slotH() - 4, x + l.slotW() - 4, y + l.slotH() - 2, color);
                if (hover) hoveredId = id;
            } else {
                centered(g, "·", x + l.slotW() / 2, y + Math.max(12, l.slotH() / 2 - 3), FAINT);
            }
        }
    }

    @Unique
    private void augmentationSurvival$available(GuiGraphicsExtractor g, LoadoutGeometry l, int mx, int my) {
        g.fill(l.listX(), l.listY(), l.listX() + l.listW(), l.listY() + l.listH(), 0xEC0D1215);
        g.outline(l.listX(), l.listY(), l.listW(), l.listH(), LINE);
        g.fill(l.listX() + 1, l.listY() + 1, l.listX() + l.listW() - 1, l.listY() + 27, 0xFF141B1F);
        g.fill(l.listX() + 9, l.listY() + 26, l.listX() + l.listW() - 9, l.listY() + 27, LINE_SOFT);
        g.text(font, "보유 액티브", l.listX() + 10, l.listY() + 8, TEXT, false);
        String amount = ClientActiveLoadoutState.available().size() + "개";
        g.text(font, amount, l.listX() + l.listW() - 10 - font.width(amount), l.listY() + 8, MUTED, false);

        List<String> available = ClientActiveLoadoutState.available();
        GridGeometry grid = augmentationSurvival$grid(l);
        int pages = Math.max(1, (available.size() + grid.pageSize() - 1) / grid.pageSize());
        page = Math.max(0, Math.min(page, pages - 1));
        if (available.isEmpty()) {
            g.text(font, "아직 편성 가능한 액티브 증강이 없습니다.", l.listX() + 12, l.listY() + 42, TEXT, false);
            return;
        }
        int start = page * grid.pageSize();
        int end = Math.min(available.size(), start + grid.pageSize());
        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % grid.columns();
            int row = local / grid.columns();
            int x = grid.left() + col * (grid.cellW() + grid.gapX());
            int y = grid.top() + row * (grid.cellH() + grid.gapY());
            augmentationSurvival$activeTile(g, x, y, grid.cellW(), grid.cellH(), available.get(i), mx, my);
        }
        if (pages > 1) {
            boolean lh = page > 0 && inside(mx, my, grid.leftPagerX(), grid.pagerY(), 28, 16);
            boolean rh = page + 1 < pages && inside(mx, my, grid.rightPagerX(), grid.pagerY(), 28, 16);
            g.text(font, "‹", grid.leftPagerX() + 10, grid.pagerY() + 3, lh ? TEXT : FAINT, false);
            centered(g, (page + 1) + " / " + pages, l.listX() + l.listW() / 2, grid.pagerY() + 3, MUTED);
            g.text(font, "›", grid.rightPagerX() + 10, grid.pagerY() + 3, rh ? TEXT : FAINT, false);
        }
    }

    @Unique
    private void augmentationSurvival$activeTile(
            GuiGraphicsExtractor g, int x, int y, int w, int h, String id, int mx, int my
    ) {
        AugmentDefinition d = safe(id);
        if (d == null) return;
        int color = tierColor(d);
        boolean hover = inside(mx, my, x, y, w, h);
        boolean selected = id.equals(selectedAugmentId);
        boolean equipped = ClientActiveLoadoutState.slots().contains(id);
        if (selected) g.fill(x - 2, y - 2, x + w + 2, y + h + 2, withAlpha(color, 0x24));
        g.fill(x, y, x + w, y + h, selected ? 0xFF172226 : hover ? SURFACE_HI : SURFACE);
        g.fill(x, y, x + w, y + 2, color);
        g.outline(x, y, w, h, selected ? brighten(color, 16) : hover ? LINE_HI : LINE_SOFT);
        int iconX = x + 6;
        int iconY = y + Math.max(5, (h - 20) / 2);
        g.fill(iconX, iconY, iconX + 22, iconY + 20, DEEP);
        g.item(AugmentIconClient.stackFor(id, d.tier()), iconX + 3, iconY + 2);
        int tx = x + 33;
        g.text(font, fit(AugmentPresentationCatalog.get(d).displayName(), Math.max(18, w - 40)), tx, y + 5,
                hover || selected ? TEXT : 0xFFD6DDDE, false);
        if (h >= 26) {
            g.text(font, "Lv." + Math.max(1, ClientAugmentProgressState.level(id)), tx, y + 16, MUTED, false);
        }
        if (equipped) {
            g.fill(x + w - 5, y + 5, x + w - 3, y + h - 5, ACCENT);
            g.fill(x + w - 10, y + 5, x + w - 3, y + 7, ACCENT);
        }
        if (hover) hoveredId = id;
    }

    @Unique
    private void augmentationSurvival$popover(GuiGraphicsExtractor g, String id, int mx, int my, LoadoutGeometry l) {
        AugmentDefinition d = safe(id);
        if (d == null) return;
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        boolean detailed = shiftDown();
        String body = detailed ? p.detail() : p.summary();
        int color = tierColor(d);
        int maxW = Math.max(180, l.w() - 24);
        int panelW = Math.min(360, Math.max(230, maxW * 4 / 5));
        panelW = Math.min(panelW, maxW);
        List<String> lines = wrap(body, Math.max(40, panelW - 24));
        int lineH = 10;
        int maxH = Math.max(92, l.h() - 24);
        int panelH = 74 + lines.size() * lineH;
        if (panelH > maxH && panelW < maxW) {
            panelW = maxW;
            lines = wrap(body, Math.max(40, panelW - 24));
            panelH = 74 + lines.size() * lineH;
        }
        if (panelH > maxH && !lines.isEmpty()) {
            lineH = Math.max(8, (maxH - 74) / lines.size());
            panelH = Math.min(maxH, 74 + lines.size() * lineH);
        }
        int px = mx + 14;
        if (px + panelW > l.x() + l.w() - 6) px = mx - panelW - 14;
        px = Math.max(l.x() + 6, Math.min(px, l.x() + l.w() - panelW - 6));
        int py = my + 12;
        if (py + panelH > l.y() + l.h() - 6) py = my - panelH - 12;
        py = Math.max(l.y() + 6, Math.min(py, l.y() + l.h() - panelH - 6));

        if (d.tier().ordinal() >= 3) {
            int bloom = d.tier().ordinal() >= 5 ? 10 : d.tier().ordinal() == 4 ? 8 : 5;
            g.fill(px - bloom, py - bloom, px + panelW + bloom, py + panelH + bloom,
                    withAlpha(color, d.tier().ordinal() >= 5 ? 0x26 : 0x18));
        }
        g.fill(px - 3, py - 3, px + panelW + 3, py + panelH + 3, 0xB0000000);
        g.fill(px, py, px + panelW, py + panelH, 0xFA090E11);
        g.outline(px, py, panelW, panelH, brighten(color, 8));
        g.fill(px, py, px + panelW, py + 3, color);
        g.fill(px + 10, py + 11, px + 42, py + 43, DEEP);
        g.outline(px + 10, py + 11, 32, 32, shade(color, 0.84));
        g.item(AugmentIconClient.stackFor(id, d.tier()), px + 18, py + 19);
        int tx = px + 50;
        g.text(font, fit(p.displayName(), panelW - 62), tx, py + 12, TEXT, false);
        String meta = tierLabel(d) + " · 액티브 · Lv." + Math.max(1, ClientAugmentProgressState.level(id)) + "/" + d.maxLevel();
        g.text(font, fit(meta, panelW - 62), tx, py + 26, color, false);
        g.fill(px + 10, py + 48, px + panelW - 10, py + 49, LINE_SOFT);
        for (int i = 0; i < lines.size(); i++) {
            int yy = py + 54 + i * lineH;
            if (yy + 8 > py + panelH - 17) break;
            g.text(font, lines.get(i), px + 12, yy, detailed ? 0xFFE4E8E8 : 0xFFD1D8DA, false);
        }
        g.text(font, detailed ? "Shift · 정확한 수치 표시 중" : "Shift · 정확한 수치",
                px + 12, py + panelH - 14, detailed ? color : FAINT, false);
    }

    @Unique
    private LoadoutGeometry augmentationSurvival$layout() {
        int margin = width >= 300 && height >= 220 ? 8 : 4;
        int w = Math.max(1, Math.min(720, width - margin * 2));
        int h = Math.max(1, Math.min(390, height - margin * 2));
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        boolean compact = w < 540 || h < 300;
        int quickLabelY = y + (compact ? 34 : 38);
        int gap = w >= 620 ? 5 : w >= 430 ? 4 : w >= 300 ? 2 : 1;
        int budget = Math.max(1, w - 28);
        int rawW = Math.max(1, (budget - gap * 9) / 10);
        int slotW = Math.min(49, Math.max(10, rawW));
        if (slotW * 10 + gap * 9 > budget) slotW = rawW;
        int slotH = compact ? Math.max(28, Math.min(34, h / 7)) : 48;
        int total = slotW * 10 + gap * 9;
        int slotStartX = x + (w - total) / 2;
        int slotY = y + (compact ? 47 : 53);
        int listY = slotY + slotH + (compact ? 7 : 11);
        int footerY = y + h - (compact ? 20 : 24);
        int listH = Math.max(48, footerY - listY);
        int listX = x + 11;
        int listW = Math.max(40, w - 22);
        return new LoadoutGeometry(x, y, w, h, compact, quickLabelY,
                slotStartX, slotY, slotW, slotH, gap, listX, listY, listW, listH);
    }

    @Unique
    private GridGeometry augmentationSurvival$grid(LoadoutGeometry l) {
        int w = l.listW();
        int h = l.listH();
        int columns = w >= 520 ? 5 : w >= 340 ? 4 : w >= 240 ? 3 : 2;
        int gapX = 5;
        int gapY = 3;
        int cellH = 27;
        int left = l.listX() + 9;
        int top = l.listY() + 32;
        int usableW = Math.max(20, w - 18);
        int cellW = Math.max(36, (usableW - gapX * (columns - 1)) / columns);
        int pagerReserve = 17;
        int rows = Math.max(1, (h - 32 - pagerReserve + gapY) / (cellH + gapY));
        int pageSize = Math.max(1, columns * rows);
        int pagerY = l.listY() + h - 16;
        int center = l.listX() + w / 2;
        return new GridGeometry(left, top, cellW, cellH, gapX, gapY,
                columns, rows, pageSize, pagerY, center - 48, center + 22);
    }

    @Unique private boolean shiftDown() {
        return minecraft != null && (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT));
    }
    @Unique private AugmentDefinition safe(String id) {
        try { return AugmentCatalog.require(id); } catch (RuntimeException ignored) { return null; }
    }
    @Unique private int tierColor(AugmentDefinition d) {
        return switch (d.tier()) {
            case COMMON -> 0xFFB8BEC5; case UNCOMMON -> 0xFF51C982; case RARE -> 0xFF4596E4;
            case EPIC -> 0xFFAA58D4; case LEGENDARY -> 0xFFE5AA36; case MYTHIC -> 0xFFE44B4B;
        };
    }
    @Unique private String tierLabel(AugmentDefinition d) {
        return switch (d.tier()) {
            case COMMON -> "일반"; case UNCOMMON -> "고급"; case RARE -> "희귀";
            case EPIC -> "영웅"; case LEGENDARY -> "전설"; case MYTHIC -> "신화";
        };
    }
    @Unique private List<String> wrap(String value, int maxWidth) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(candidate) <= maxWidth) line = new StringBuilder(candidate);
            else {
                if (line.length() > 0) out.add(line.toString());
                if (font.width(word) <= maxWidth) line = new StringBuilder(word);
                else {
                    StringBuilder part = new StringBuilder();
                    for (int off = 0; off < word.length();) {
                        int cp = word.codePointAt(off);
                        String next = part.toString() + new String(Character.toChars(cp));
                        if (part.length() > 0 && font.width(next) > maxWidth) { out.add(part.toString()); part.setLength(0); }
                        part.appendCodePoint(cp); off += Character.charCount(cp);
                    }
                    line = part;
                }
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }
    @Unique private String fit(String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(value) <= maxWidth) return value;
        String result = value;
        while (!result.isEmpty() && font.width(result + "…") > maxWidth) {
            result = result.substring(0, result.offsetByCodePoints(result.length(), -1));
        }
        return result.isEmpty() ? "…" : result + "…";
    }
    @Unique private void centered(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(font, s, x - font.width(s) / 2, y, color, false);
    }
    @Unique private void augmentationSurvival$corner(GuiGraphicsExtractor g, int x, int y, int dx, int dy) {
        int hx = x + dx * 11, hy = y + dy * 2, vx = x + dx * 2, vy = y + dy * 11;
        g.fill(Math.min(x, hx), Math.min(y, hy), Math.max(x, hx), Math.max(y, hy), LINE_HI);
        g.fill(Math.min(x, vx), Math.min(y, vy), Math.max(x, vx), Math.max(y, vy), LINE_HI);
    }
    @Unique private static boolean inside(double x, double y, int l, int t, int w, int h) {
        return x >= l && x < l + w && y >= t && y < t + h;
    }
    @Unique private static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 255) + amount);
        int gg = Math.min(255, ((color >> 8) & 255) + amount);
        int b = Math.min(255, (color & 255) + amount);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
    @Unique private static int shade(int color, double factor) {
        int r = (int) (((color >> 16) & 255) * factor);
        int gg = (int) (((color >> 8) & 255) * factor);
        int b = (int) ((color & 255) * factor);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
    @Unique private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    @Unique private record LoadoutGeometry(
            int x, int y, int w, int h, boolean compact, int quickLabelY,
            int slotStartX, int slotY, int slotW, int slotH, int slotGap,
            int listX, int listY, int listW, int listH
    ) { }
    @Unique private record GridGeometry(
            int left, int top, int cellW, int cellH, int gapX, int gapY,
            int columns, int rows, int pageSize, int pagerY, int leftPagerX, int rightPagerX
    ) { }
}
