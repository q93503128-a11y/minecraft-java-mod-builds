package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityPowerCatalog;
import dev.moonseungjun.augmentationsurvival.network.AuthorityCollectionPayload;
import dev.moonseungjun.augmentationsurvival.network.AuthorityTogglePayload;
import dev.moonseungjun.augmentationsurvival.client.ClientAuthorityCooldownState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Read-only, server-backed authority ledger. No release or state mutation is possible from this menu. */
public final class AuthorityCollectionScreen extends Screen {
    private static final int PANEL = 0xF2D7D4C9;
    private static final int TEXT = 0xFF283333;
    private static final int MUTED = 0xFF57625F;
    private static final int SELECTED = 0xFFE7D7BA;
    private static final int OWNED = 0xFF426C61;
    private static final int MISSING = 0xFF9B9B95;
    private static final int BORDER = 0xFF586963;
    private static final int HIGHLIGHT = 0xFFBF8961;

    private Set<String> owned;
    private Set<String> disabled;
    private List<AuthorityPowerCatalog.Entry> entries;
    private String selectedId;
    private int page;

    public AuthorityCollectionScreen(AuthorityCollectionPayload snapshot) {
        super(Component.literal("탈취한 권능"));
        this.owned = Set.copyOf(snapshot.ownedIds());
        this.disabled = Set.copyOf(snapshot.disabledIds());
        this.entries = AuthorityPowerCatalog.available(snapshot.ownedIds());
        this.selectedId = AuthorityPowerCatalog.ENTRIES.stream()
                .filter(entry -> owned.contains(entry.id()))
                .map(AuthorityPowerCatalog.Entry::id)
                .findFirst()
                .orElse(AuthorityPowerCatalog.ENTRIES.getFirst().id());
    }

    /** Apply server-confirmed toggles without discarding scroll or selection. */
    public void applySnapshot(AuthorityCollectionPayload snapshot) {
        owned = Set.copyOf(snapshot.ownedIds());
        disabled = Set.copyOf(snapshot.disabledIds());
        entries = AuthorityPowerCatalog.available(snapshot.ownedIds());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Layout layout = layout();
        g.fill(0, 0, width, height, 0x88000000);
        g.fill(layout.x(), layout.y(), layout.x() + layout.w(), layout.y() + layout.h(), PANEL);
        g.outline(layout.x(), layout.y(), layout.w(), layout.h(), BORDER);
        g.fill(layout.x() + 2, layout.y() + 2, layout.x() + layout.w() - 2, layout.y() + 4, HIGHLIGHT);

        g.text(font, "탈취한 권능", layout.x() + 13, layout.y() + 13, TEXT, false);
        String total = owned.size() + " / " + entries.size() + " 획득";
        g.text(font, total, layout.x() + layout.w() - 13 - font.width(total),
                layout.y() + 13, OWNED, false);
        g.fill(layout.x() + 11, layout.y() + 32,
                layout.x() + layout.w() - 11, layout.y() + 33, BORDER);

        int pages = Math.max(1, (entries.size() + layout.rows() - 1) / layout.rows());
        page = Math.max(0, Math.min(page, pages - 1));
        for (int index = 0; index < layout.rows(); index++) {
            int absolute = page * layout.rows() + index;
            if (absolute >= entries.size()) break;
            AuthorityPowerCatalog.Entry entry = entries.get(absolute);
            int y = layout.listY() + index * layout.rowHeight();
            boolean acquired = owned.contains(entry.id());
            boolean selected = entry.id().equals(selectedId);
            boolean hover = inside(mouseX, mouseY, layout.listX(), y, layout.listW(), layout.rowHeight() - 2);
            g.fill(layout.listX(), y, layout.listX() + layout.listW(), y + layout.rowHeight() - 2,
                    selected ? SELECTED : hover ? 0xFFF0E6D6 : 0xFFE4E1D9);
            g.fill(layout.listX(), y, layout.listX() + 3, y + layout.rowHeight() - 2,
                    acquired ? OWNED : MISSING);
            String label = (acquired ? "◆ " : "◇ ") + entry.bossName();
            String state = acquired ? (disabled.contains(entry.id()) ? "해방 꺼짐" : "해방 켜짐") : "미획득";
            g.text(font, fit(label, layout.listW() - 100), layout.listX() + 10, y + 6,
                    acquired ? TEXT : MUTED, false);
            g.text(font, state, layout.listX() + layout.listW() - 9 - font.width(state),
                    y + 6, acquired ? (disabled.contains(entry.id()) ? MUTED : OWNED) : MISSING, false);
        }
        int navY = layout.listY() + layout.rows() * layout.rowHeight() + 2;
        g.text(font, "‹ 이전", layout.listX() + 5, navY, page > 0 ? TEXT : MISSING, false);
        String currentPage = (page + 1) + " / " + pages;
        g.text(font, currentPage, layout.listX() + (layout.listW() - font.width(currentPage)) / 2, navY, MUTED, false);
        String next = "다음 ›";
        g.text(font, next, layout.listX() + layout.listW() - font.width(next) - 5, navY,
                page + 1 < pages ? TEXT : MISSING, false);

        AuthorityPowerCatalog.Entry selected = AuthorityPowerCatalog.find(selectedId);
        if (selected != null) {
            g.text(font, selected.group(), layout.detailsX() + 7, layout.detailsY() + 5, OWNED, false);
            g.text(font, fit(selected.bossName(), layout.detailsW() - 14),
                    layout.detailsX() + 7, layout.detailsY() + 23, TEXT, false);
            g.fill(layout.detailsX() + 7, layout.detailsY() + 43,
                    layout.detailsX() + layout.detailsW() - 7, layout.detailsY() + 44, BORDER);
            String status = !owned.contains(selected.id()) ? "아직 탈취하지 못한 권능"
                    : disabled.contains(selected.id()) ? "획득 · J 해방에서 제외됨" : "획득 · J 해방에 포함됨";
            g.text(font, status, layout.detailsX() + 7, layout.detailsY() + 53,
                    owned.contains(selected.id()) ? OWNED : MUTED, false);
            List<String> lines = wrap(selected.effect(), layout.detailsW() - 16);
            for (int i = 0; i < Math.min(lines.size(), 9); i++) {
                g.text(font, lines.get(i), layout.detailsX() + 7,
                        layout.detailsY() + 72 + i * 12, TEXT, false);
            }
        }
        int footerY = layout.y() + layout.h() - 31;
        g.fill(layout.x() + 12, footerY - 6, layout.x() + layout.w() - 12, footerY - 5, BORDER);
        int remaining = ClientAuthorityCooldownState.remainingSeconds();
        String cooldown = remaining > 0 ? "해방 대기 " + remaining + "초" : "해방 준비됨";
        g.text(font, fit("J: 켠 권능 해방 · 목록 우측을 클릭해 켜기/끄기 · 재사용 120초", layout.w() - 24),
                layout.x() + 13, footerY, TEXT, false);
        g.text(font, fit(cooldown + " · Esc: 닫기", layout.w() - 24),
                layout.x() + 13, footerY + 12, MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout layout = layout();
        if (inside(event.x(), event.y(), layout.listX(), layout.listY(),
                layout.listW(), layout.rows() * layout.rowHeight())) {
            int index = (int) (event.y() - layout.listY()) / layout.rowHeight();
            int absolute = page * layout.rows() + index;
            if (index >= 0 && absolute < entries.size()) {
                AuthorityPowerCatalog.Entry entry = entries.get(absolute);
                selectedId = entry.id();
                if (owned.contains(entry.id())
                        && event.x() >= layout.listX() + layout.listW() - 96) {
                    ClientPlayNetworking.send(new AuthorityTogglePayload(
                            entry.id(), disabled.contains(entry.id())));
                }
                return true;
            }
        }
        int navY = layout.listY() + layout.rows() * layout.rowHeight();
        if (inside(event.x(), event.y(), layout.listX(), navY, layout.listW() / 3, 18)) {
            page = Math.max(0, page - 1);
            return true;
        }
        if (inside(event.x(), event.y(), layout.listX() + layout.listW() * 2 / 3, navY,
                layout.listW() / 3, 18)) {
            page = Math.min(Math.max(0, (entries.size() - 1) / layout.rows()), page + 1);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private Layout layout() {
        int w = Math.max(1, Math.min(728, width - 24));
        int h = Math.max(1, Math.min(450, height - 24));
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        boolean compact = w < 430;
        int listX = x + 12;
        int listY = y + 43;
        int listW = compact ? w - 24 : Math.max(170, w * 44 / 100);
        int listH = compact ? Math.max(70, (h - 93) / 2) : h - 101;
        int rowHeight = 25;
        int rows = Math.max(1, (listH - 18) / rowHeight);
        int detailsX = compact ? listX : listX + listW + 12;
        int detailsY = compact ? listY + rows * rowHeight + 24 : listY;
        int detailsW = compact ? listW : w - 24 - listW - 12;
        return new Layout(x, y, w, h, listX, listY, listW, rowHeight, rows,
                detailsX, detailsY, detailsW);
    }

    private List<String> wrap(String value, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.width(next) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private String fit(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        String shortened = value;
        while (!shortened.isEmpty() && font.width(shortened + "…") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record Layout(int x, int y, int w, int h, int listX, int listY,
                          int listW, int rowHeight, int rows, int detailsX, int detailsY, int detailsW) { }
}
