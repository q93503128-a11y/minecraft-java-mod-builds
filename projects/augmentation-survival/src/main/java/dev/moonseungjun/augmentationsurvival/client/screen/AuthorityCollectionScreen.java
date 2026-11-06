package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityPowerCatalog;
import dev.moonseungjun.augmentationsurvival.client.AuthorityPowerIconsClient;
import dev.moonseungjun.augmentationsurvival.client.ClientAuthorityCooldownState;
import dev.moonseungjun.augmentationsurvival.client.EntityIconClient;
import dev.moonseungjun.augmentationsurvival.network.AuthorityCollectionPayload;
import dev.moonseungjun.augmentationsurvival.network.AuthorityTogglePayload;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Server-backed authority ledger. Unsupported saved entries remain visible and untouched. */
public final class AuthorityCollectionScreen extends Screen {
    private static final int SHADE = 0xA8000000;
    private static final int STONE = 0xF022292B;
    private static final int SURFACE = 0xFF303A3B;
    private static final int SURFACE_ALT = 0xFF394442;
    private static final int TEXT = 0xFFF1E9D8;
    private static final int MUTED = 0xFFB2B6A8;
    private static final int COPPER = 0xFFBE8058;
    private static final int GREEN = 0xFF8EC6A0;
    private static final int DIM = 0xFF70766F;

    private Set<String> owned;
    private Set<String> supported;
    private Set<String> disabled;
    private List<AuthorityPowerCatalog.Entry> entries;
    private List<String> groups;
    private String group;
    private String selectedId;
    private int page;
    private int detailScroll;
    private int compactView;

    public AuthorityCollectionScreen(AuthorityCollectionPayload snapshot) {
        super(Component.literal("탈취한 권능"));
        applyState(snapshot, true);
    }

    public void applySnapshot(AuthorityCollectionPayload snapshot) {
        applyState(snapshot, false);
    }

    private void applyState(AuthorityCollectionPayload snapshot, boolean initialize) {
        owned = Set.copyOf(snapshot.ownedIds());
        supported = Set.copyOf(snapshot.supportedIds());
        disabled = Set.copyOf(snapshot.disabledIds());
        entries = AuthorityPowerCatalog.available(snapshot.supportedIds(), snapshot.ownedIds());
        LinkedHashSet<String> availableGroups = new LinkedHashSet<>();
        for (AuthorityPowerCatalog.Entry entry : entries) {
            if (!supported.contains(entry.id())) continue;
            if (!availableGroups.contains(entry.group())) availableGroups.add(entry.group());
        }
        if (entries.stream().anyMatch(entry -> owned.contains(entry.id()) && !supported.contains(entry.id()))) {
            availableGroups.add("기록 보존");
        }
        groups = List.copyOf(availableGroups);
        if (groups.isEmpty()) groups = List.of("바닐라");
        if (initialize || group == null || !groups.contains(group)) group = groups.getFirst();
        if (initialize || selectedId == null || visibleEntries().stream().noneMatch(e -> e.id().equals(selectedId))) {
            selectedId = visibleEntries().stream().findFirst().map(AuthorityPowerCatalog.Entry::id).orElse("");
            page = 0;
            detailScroll = 0;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Layout l = layout();
        g.fill(0, 0, width, height, SHADE);
        g.fill(l.x, l.y, l.x + l.w, l.y + l.h, STONE);
        g.outline(l.x, l.y, l.w, l.h, COPPER);
        g.fill(l.x + 2, l.y + 2, l.x + l.w - 2, l.y + 25, SURFACE);
        g.fill(l.x + 7, l.y + 28, l.x + l.w - 7, l.y + 29, 0xFF67594D);
        g.text(font, "탈취한 권능", l.x + 12, l.y + 9, TEXT, false);
        String count = owned.size() + " 획득";
        g.text(font, count, l.x + l.w - font.width(count) - 12, l.y + 9, GREEN, false);

        List<AuthorityPowerCatalog.Entry> visible = visibleEntries();
        if (l.compact) drawCompact(g, l, visible, mouseX, mouseY);
        else drawWide(g, l, visible, mouseX, mouseY);

        int remaining = ClientAuthorityCooldownState.remainingSeconds();
        String cooldown = remaining > 0 ? "재사용 대기 · " + remaining + "초" : "준비됨";
        int footerY = l.y + l.h - 14;
        g.text(font, fit("J 해방 · Shift+J 관리 · 공통 재사용 120초 · " + cooldown,
                l.w - 22), l.x + 11, footerY, remaining > 0 ? MUTED : GREEN, false);
    }

    private void drawWide(GuiGraphicsExtractor g, Layout l, List<AuthorityPowerCatalog.Entry> visible,
                          int mouseX, int mouseY) {
        int tabY = l.y + 32;
        drawGroupTabs(g, l, tabY, mouseX, mouseY, false);
        int listX = l.x + 12;
        int listY = l.y + 58;
        int listW = l.listW;
        int rowH = l.rowH;
        int rows = l.rows;
        int pages = pageCount(visible.size(), rows);
        page = clampPage(page, pages);
        int start = page * rows;
        for (int row = 0; row < rows; row++) {
            int index = start + row;
            if (index >= visible.size()) break;
            AuthorityPowerCatalog.Entry entry = visible.get(index);
            int y = listY + row * rowH;
            drawEntryRow(g, entry, listX, y, listW, rowH - 2,
                    entry.id().equals(selectedId), inside(mouseX, mouseY, listX, y, listW, rowH - 2));
        }
        drawPageNav(g, listX, listY + rows * rowH + 1, listW, page, pages);

        int dx = listX + listW + 12;
        int dy = listY;
        int dw = l.w - (dx - l.x) - 12;
        int dh = l.h - 90;
        drawDetail(g, selected(), dx, dy, dw, dh, mouseX, mouseY, false);
    }

    private void drawCompact(GuiGraphicsExtractor g, Layout l, List<AuthorityPowerCatalog.Entry> visible,
                             int mouseX, int mouseY) {
        int tabY = l.y + 28;
        String tabLabel = compactView == 0 ? "‹ " + group + " ›" : "‹ 목록";
        int tabW = Math.min(l.w - 16, font.width(tabLabel) + 14);
        int tabX = l.x + (l.w - tabW) / 2;
        g.fill(tabX, tabY, tabX + tabW, tabY + 16, SURFACE_ALT);
        g.outline(tabX, tabY, tabW, 16, COPPER);
        g.text(font, fit(tabLabel, tabW - 8), tabX + 4, tabY + 4, TEXT, false);

        if (compactView != 0) {
            drawCompactDetail(g, l, compactView == 2);
            return;
        }
        int listX = l.x + 8;
        int listY = l.y + 48;
        int rowH = l.rowH;
        int rows = l.rows;
        int pages = pageCount(visible.size(), rows);
        page = clampPage(page, pages);
        int start = page * rows;
        for (int row = 0; row < rows; row++) {
            int index = start + row;
            if (index >= visible.size()) break;
            AuthorityPowerCatalog.Entry entry = visible.get(index);
            int y = listY + row * rowH;
            drawEntryRow(g, entry, listX, y, l.w - 16, rowH - 1,
                    entry.id().equals(selectedId), inside(mouseX, mouseY, listX, y, l.w - 16, rowH - 1));
        }
        int navY = listY + rows * rowH;
        drawPageNav(g, listX, navY, l.w - 16, page, pages);
    }

    private void drawCompactDetail(GuiGraphicsExtractor g, Layout l, boolean effectView) {
        AuthorityPowerCatalog.Entry entry = selected();
        if (entry == null) return;
        int x = l.x + 8;
        int w = l.w - 16;
        int top = l.y + 49;
        boolean isOwned = owned.contains(entry.id());
        boolean isSupported = supported.contains(entry.id());
        g.item(AuthorityPowerIconsClient.stack(entry), x, top);
        g.text(font, fit(entry.bossName(), w - 24), x + 20, top + 1, TEXT, false);
        if (effectView) {
            List<String> lines = wrap(entry.effect(), w);
            int visibleLines = Math.max(1, (l.y + l.h - 38 - (top + 15)) / 9);
            int maxScroll = Math.max(0, lines.size() - visibleLines);
            detailScroll = Math.min(detailScroll, maxScroll);
            for (int i = 0; i < visibleLines && detailScroll + i < lines.size(); i++) {
                g.text(font, lines.get(detailScroll + i), x, top + 17 + i * 9, TEXT, false);
            }
        } else {
            String state = !isSupported ? "기록 보존 · 관련 모드 미설치"
                    : !isOwned ? "미획득" : disabled.contains(entry.id()) ? "해방 꺼짐" : "해방 켜짐";
            g.text(font, fit(state, w - 70), x, top + 17, isOwned ? GREEN : MUTED, false);
            String effect = "효과 보기 ›";
            g.text(font, effect, x, top + 29, COPPER, false);
            if (isOwned && isSupported) {
                int bx = l.x + l.w - 67;
                int by = l.y + l.h - 39;
                g.fill(bx, by, bx + 58, by + 18, disabled.contains(entry.id()) ? 0xFF5A5043 : 0xFF335143);
                g.outline(bx, by, 58, 18, COPPER);
                String label = disabled.contains(entry.id()) ? "켜기" : "끄기";
                g.text(font, label, bx + (58 - font.width(label)) / 2, by + 5, TEXT, false);
            }
        }
    }

    private void drawGroupTabs(GuiGraphicsExtractor g, Layout l, int y, int mouseX, int mouseY, boolean compact) {
        if (compact || l.w < 500) {
            String label = "‹ " + group + " ›";
            int w = Math.min(l.w - 24, font.width(label) + 14);
            int x = l.x + (l.w - w) / 2;
            g.fill(x, y, x + w, y + 15, SURFACE_ALT);
            g.outline(x, y, w, 15, COPPER);
            g.text(font, fit(label, w - 8), x + 4, y + 4, TEXT, false);
            return;
        }
        int x = l.x + 12;
        for (String item : groups) {
            int tabW = Math.max(42, font.width(item) + 14);
            boolean active = item.equals(group);
            boolean hover = inside(mouseX, mouseY, x, y, tabW, 18);
            g.fill(x, y, x + tabW, y + 18, active ? 0xFF5D473B : hover ? SURFACE_ALT : SURFACE);
            g.outline(x, y, tabW, 18, active ? COPPER : 0xFF56605A);
            g.text(font, fit(item, tabW - 8), x + 4, y + 5, active ? TEXT : MUTED, false);
            x += tabW + 4;
            if (x + 42 > l.x + l.w) break;
        }
    }

    private void drawEntryRow(GuiGraphicsExtractor g, AuthorityPowerCatalog.Entry entry,
                              int x, int y, int w, int h, boolean selected, boolean hover) {
        boolean isOwned = owned.contains(entry.id());
        boolean isSupported = supported.contains(entry.id());
        int fill = selected ? 0xFF514237 : hover ? 0xFF46504B : SURFACE;
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + 3, y + h, isOwned ? COPPER : 0xFF656B63);
        g.item(AuthorityPowerIconsClient.stack(entry), x + 7, y + Math.max(1, (h - 16) / 2));
        String name = fit(entry.bossName(), w - 100);
        g.text(font, name, x + 28, y + Math.max(2, (h - 8) / 2), isOwned ? TEXT : MUTED, false);
        String state = !isSupported ? "보존" : isOwned ? (disabled.contains(entry.id()) ? "꺼짐" : "켜짐") : "미획득";
        int color = !isSupported ? MUTED : isOwned && !disabled.contains(entry.id()) ? GREEN : DIM;
        g.text(font, state, x + w - font.width(state) - 7, y + Math.max(2, (h - 8) / 2), color, false);
    }

    private void drawDetail(GuiGraphicsExtractor g, AuthorityPowerCatalog.Entry entry,
                            int x, int y, int w, int h, int mouseX, int mouseY, boolean compact) {
        if (w <= 0 || h <= 0) return;
        g.fill(x, y, x + w, y + h, SURFACE);
        g.outline(x, y, w, h, 0xFF5F665D);
        if (entry == null) {
            String empty = "권능이 없습니다.";
            g.text(font, empty, x + 8, y + 8, MUTED, false);
            return;
        }
        boolean isOwned = owned.contains(entry.id());
        boolean isSupported = supported.contains(entry.id());
        int iconY = y + 8;
        g.fill(x + 8, iconY, x + 38, iconY + 30, 0xFF263133);
        if (!EntityIconClient.render(g, AuthorityPowerIconsClient.entityId(entry), x + 8, iconY, 30))
            g.item(AuthorityPowerIconsClient.stack(entry), x + 15, iconY + 7);
        g.text(font, fit(entry.group(), w - 54), x + 45, y + 8, COPPER, false);
        boolean canToggle = !compact && isOwned && isSupported;
        int nameWidth = canToggle ? Math.max(1, w - 54 - 86) : w - 54;
        g.text(font, fit(entry.bossName(), nameWidth), x + 45, y + 20, TEXT, false);
        String state = !isSupported ? "저장된 기록 · 현재 모드 미설치"
                : !isOwned ? "아직 획득하지 않음"
                : disabled.contains(entry.id()) ? "J 해방에서 제외됨" : "J 해방에 포함됨";
        int stateY = compact ? y + 40 : y + 47;
        g.text(font, fit(state, w - 16), x + 8, stateY, isOwned ? GREEN : MUTED, false);

        if (canToggle) {
            int bx = x + w - 88;
            int by = y + 7;
            boolean hover = inside(mouseX, mouseY, bx, by, 78, 20);
            g.fill(bx, by, bx + 78, by + 20, disabled.contains(entry.id()) ? 0xFF5A5043 : 0xFF335143);
            g.outline(bx, by, 78, 20, hover ? COPPER : 0xFF778277);
            String label = disabled.contains(entry.id()) ? "해방 켜기" : "해방 끄기";
            g.text(font, label, bx + (78 - font.width(label)) / 2, by + 6, TEXT, false);
        }

        int effectsY = compact ? stateY + 12 : y + 75;
        int maxLines = compact ? Math.max(1, (h - (effectsY - y) - 5) / 9)
                : Math.max(1, (h - (effectsY - y) - 12) / 11);
        List<String> effectLines = wrap(entry.effect(), w - 16);
        if (detailScroll > Math.max(0, effectLines.size() - maxLines)) {
            detailScroll = Math.max(0, effectLines.size() - maxLines);
        }
        for (int i = 0; i < maxLines && detailScroll + i < effectLines.size(); i++) {
            g.text(font, effectLines.get(detailScroll + i), x + 8,
                    effectsY + i * (compact ? 9 : 11), TEXT, false);
        }
        if (effectLines.size() > maxLines) {
            String more = "‹ 설명 스크롤 ›";
            g.text(font, more, x + w - font.width(more) - 7, y + h - 11, MUTED, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        Layout l = layout();
        if (l.compact || l.w < 500) {
            int tabsY = l.y + 28;
            if (inside(mx, my, l.x + 8, tabsY, l.w - 16, 16)) {
                if (compactView != 0) compactView = 0;
                else cycleGroup(mx < l.x + l.w / 2 ? -1 : 1);
                page = 0;
                return true;
            }
            if (compactView != 0) {
                AuthorityPowerCatalog.Entry entry = selected();
                if (compactView == 1 && entry != null) {
                    int bx = l.x + l.w - 67;
                    int by = l.y + l.h - 39;
                    if (owned.contains(entry.id()) && supported.contains(entry.id())
                            && inside(mx, my, bx, by, 58, 18)) {
                        ClientPlayNetworking.send(new AuthorityTogglePayload(entry.id(), disabled.contains(entry.id())));
                        return true;
                    }
                    if (inside(mx, my, l.x + 8, l.y + 77, l.w - 24, 14)) {
                        compactView = 2;
                        detailScroll = 0;
                        return true;
                    }
                }
                return true;
            }
            int listX = l.x + 8, listY = l.y + 48;
            int index = page * l.rows + (int) ((my - listY) / l.rowH);
            if (inside(mx, my, listX, listY, l.w - 16, l.rows * l.rowH)) {
                List<AuthorityPowerCatalog.Entry> visible = visibleEntries();
                if (index >= 0 && index < visible.size()) { select(visible.get(index)); compactView = 1; }
                return true;
            }
            int navY = listY + l.rows * l.rowH;
            if (inside(mx, my, listX, navY, (l.w - 16) / 3, 14)) { page = Math.max(0, page - 1); return true; }
            if (inside(mx, my, listX + (l.w - 16) * 2 / 3, navY, (l.w - 16) / 3, 14)) {
                page = Math.min(pageCount(visibleEntries().size(), l.rows) - 1, page + 1); return true;
            }
        } else {
            int tabY = l.y + 32;
            int x = l.x + 12;
            for (String item : groups) {
                int tabW = Math.max(42, font.width(item) + 14);
                if (inside(mx, my, x, tabY, tabW, 18)) { group = item; page = 0; detailScroll = 0; selectFirstVisible(); return true; }
                x += tabW + 4;
                if (x + 42 > l.x + l.w) break;
            }
            int listX = l.x + 12, listY = l.y + 58;
            if (inside(mx, my, listX, listY, l.listW, l.rows * l.rowH)) {
                int index = page * l.rows + (int) ((my - listY) / l.rowH);
                List<AuthorityPowerCatalog.Entry> visible = visibleEntries();
                if (index >= 0 && index < visible.size()) select(visible.get(index));
                return true;
            }
            int navY = listY + l.rows * l.rowH;
            if (inside(mx, my, listX, navY, l.listW / 3, 18)) { page = Math.max(0, page - 1); return true; }
            if (inside(mx, my, listX + l.listW * 2 / 3, navY, l.listW / 3, 18)) {
                page = Math.min(pageCount(visibleEntries().size(), l.rows) - 1, page + 1); return true;
            }
            AuthorityPowerCatalog.Entry selected = selected();
            int dx = listX + l.listW + 12;
            int dy = listY;
            int dw = l.w - (dx - l.x) - 12;
            int bx = dx + dw - 88;
            if (selected != null && owned.contains(selected.id()) && supported.contains(selected.id())
                    && inside(mx, my, bx, dy + 7, 78, 20)) {
                ClientPlayNetworking.send(new AuthorityTogglePayload(selected.id(), disabled.contains(selected.id())));
                return true;
            }
            if (inside(mx, my, dx, dy, dw, l.h - 90)) {
                detailScroll = Math.max(0, detailScroll + (my < dy + 45 ? -1 : 1));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout l = layout();
        List<AuthorityPowerCatalog.Entry> visible = visibleEntries();
        if (l.compact && compactView == 2) {
            detailScroll = Math.max(0, detailScroll + (verticalAmount < 0 ? 1 : -1));
        } else if ((l.compact || l.w < 500) && compactView == 0 && mouseY < l.y + 48 + l.rows * l.rowH) {
            int max = Math.max(0, pageCount(visible.size(), l.rows) - 1);
            page = Math.max(0, Math.min(max, page + (verticalAmount < 0 ? 1 : -1)));
        } else if (!(l.compact || l.w < 500)) {
            int maxLines = Math.max(1, (l.h - 92) / 11);
            int lineCount = selected() == null ? 0 : wrap(selected().effect(), l.w - l.listW - 40).size();
            detailScroll = Math.max(0, Math.min(Math.max(0, lineCount - maxLines), detailScroll + (verticalAmount < 0 ? 1 : -1)));
        }
        return true;
    }

    private void select(AuthorityPowerCatalog.Entry entry) { selectedId = entry.id(); detailScroll = 0; }
    private void selectFirstVisible() { selectedId = visibleEntries().stream().findFirst().map(AuthorityPowerCatalog.Entry::id).orElse(""); }
    private void cycleGroup(int direction) {
        int current = groups.indexOf(group);
        group = groups.get(Math.floorMod(current + direction, groups.size()));
        selectFirstVisible();
        detailScroll = 0;
    }

    private List<AuthorityPowerCatalog.Entry> visibleEntries() {
        if ("기록 보존".equals(group)) return entries.stream()
                .filter(e -> owned.contains(e.id()) && !supported.contains(e.id())).toList();
        return entries.stream().filter(e -> supported.contains(e.id()) && e.group().equals(group)).toList();
    }

    private AuthorityPowerCatalog.Entry selected() {
        return entries.stream().filter(e -> e.id().equals(selectedId)).findFirst().orElse(null);
    }

    private Layout layout() {
        int w = Math.max(1, Math.min(760, width - 16));
        int h = Math.max(1, Math.min(470, height - 16));
        int x = (width - w) / 2, y = (height - h) / 2;
        boolean compact = w < 500 || h < 310;
        int rowH = compact ? Math.max(16, Math.min(22, (h - 92) / 3)) : 27;
        int rows = compact ? Math.max(1, Math.min(4, (h - 92) / rowH))
                : Math.max(1, Math.min(11, (h - 108) / rowH));
        int listW = compact ? w - 24 : Math.max(195, (w - 42) * 42 / 100);
        return new Layout(x, y, w, h, compact, rowH, rows, listW);
    }

    private List<String> wrap(String value, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            StringBuilder part = new StringBuilder(word);
            while (!part.isEmpty()) {
                String chunk = part.toString();
                if (font.width(chunk) > maxWidth) {
                    int cut = chunk.length();
                    while (cut > 1 && font.width(chunk.substring(0, cut)) > maxWidth) cut--;
                    if (!line.isEmpty()) { lines.add(line.toString()); line.setLength(0); }
                    lines.add(chunk.substring(0, cut));
                    part.delete(0, cut);
                } else {
                    String next = line.isEmpty() ? chunk : line + " " + chunk;
                    if (font.width(next) > maxWidth && !line.isEmpty()) { lines.add(line.toString()); line.setLength(0); }
                    if (!line.isEmpty()) line.append(' ');
                    line.append(chunk);
                    part.setLength(0);
                }
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private String fit(String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        String text = value;
        while (!text.isEmpty() && font.width(text + "…") > maxWidth) text = text.substring(0, text.length() - 1);
        return text.equals(value) ? value : text + "…";
    }
    private void drawPageNav(GuiGraphicsExtractor g, int x, int y, int w, int page, int pages) {
        if (pages <= 1 || w < 54) return;
        int zoneW = w / 3;
        drawPageArrow(g, x + zoneW / 2 - 9, y + 1, 18, 14, "‹", page > 0);
        String label = (page + 1) + " / " + pages;
        int labelX = x + (w - font.width(label)) / 2;
        g.text(font, label, labelX, y + 4, MUTED, false);
        drawPageArrow(g, x + zoneW * 2 + (w - zoneW * 2) / 2 - 9, y + 1, 18, 14, "›", page + 1 < pages);
    }

    private void drawPageArrow(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean active) {
        g.fill(x, y, x + w, y + h, active ? 0xFF38433E : SURFACE);
        g.outline(x, y, w, h, active ? COPPER : 0xFF3F4945);
        g.text(font, label, x + (w - font.width(label)) / 2, y + 3, active ? TEXT : DIM, false);
    }

    private static int pageCount(int count, int rows) { return Math.max(1, (count + rows - 1) / rows); }
    private static int clampPage(int page, int pages) { return Math.max(0, Math.min(page, pages - 1)); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
    @Override public boolean isPauseScreen() { return false; }
    private record Layout(int x, int y, int w, int h, boolean compact, int rowH, int rows, int listW) { }
}
