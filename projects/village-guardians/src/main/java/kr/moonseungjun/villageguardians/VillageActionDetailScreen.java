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

/**
 * Compact detail-first action surface for facilities, management, funding and tower screens.
 * Selection and execution are deliberately separated, while the entire frame stays above the hotbar reserve.
 */
public final class VillageActionDetailScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x7204070A;
    private static final int PANEL = 0xF00A1116;
    private static final int PANEL_SOFT = 0xE9121E24;
    private static final int PANEL_HOVER = 0xED1B2B33;
    private static final int BORDER = 0xB34D6672;
    private static final int TEXT = 0xFFF3F5F5;
    private static final int MUTED = 0xFFA9B4B9;
    private static final int CYAN = 0xFF50D8C1;
    private static final int GOLD = 0xFFF1C25B;
    private static final int RED = 0xFFE36A63;
    private static final int ROW_HEIGHT = 39;
    private static final int ROW_GAP = 3;

    private final String heading;
    private final String body;
    private final List<ActionCard> actions = new ArrayList<>();
    private int selected = -1;
    private int scroll;

    public VillageActionDetailScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        heading = plain(payload.title());
        body = plain(payload.body());
        parse(payload);
        selected = actions.isEmpty() ? -1 : 0;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 4, layout.bottom(), accent());
        drawHeader(graphics, layout, mouseX, mouseY);
        drawList(graphics, layout.list(), mouseX, mouseY);
        drawDetail(graphics, layout.detail(), mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int left = layout.left() + 17;
        int closeX = layout.right() - 34;
        graphics.text(font, fit(font, heading, Math.max(60, closeX - left - 8)), left, layout.top() + 10,
                accent(), false);
        graphics.text(font, fit(font, body, Math.max(60, closeX - left - 8)), left, layout.top() + 27,
                MUTED, false);
        boolean close = inside(mouseX, mouseY, closeX, layout.top() + 8, 24, 24);
        graphics.fill(closeX, layout.top() + 8, closeX + 24, layout.top() + 32,
                close ? 0xFF71353A : PANEL_SOFT);
        graphics.centeredText(font, "×", closeX + 12, layout.top() + 15, close ? TEXT : MUTED);
        graphics.fill(layout.left() + 14, layout.top() + 43, layout.right() - 14, layout.top() + 44, BORDER);
    }

    private void drawList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), PANEL_SOFT);
        if (actions.isEmpty()) {
            List<FormattedCharSequence> empty = font.split(Component.literal("별도 조작 없이 자동 적용되는 시설입니다."),
                    Math.max(60, pane.width() - 24));
            int y = pane.top() + 13;
            for (FormattedCharSequence line : empty) {
                if (y > pane.bottom() - 12) break;
                graphics.text(font, line, pane.left() + 12, y, CYAN, false);
                y += 12;
            }
            return;
        }

        int content = actions.size() * ROW_HEIGHT + Math.max(0, actions.size() - 1) * ROW_GAP;
        int maximum = Math.max(0, content - Math.max(1, pane.height() - 12));
        scroll = VillageUiSafeArea.clamp(scroll, 0, maximum);
        graphics.enableScissor(pane.left() + 1, pane.top() + 1, pane.right() - 1, pane.bottom() - 1);
        int y = pane.top() + 6 - scroll;
        for (int i = 0; i < actions.size(); i++) {
            ActionCard card = actions.get(i);
            int x = pane.left() + 6;
            int w = pane.width() - 14;
            boolean hover = inside(mouseX, mouseY, x, y, w, ROW_HEIGHT);
            boolean active = selected == i;
            graphics.fill(x, y, x + w, y + ROW_HEIGHT, active ? PANEL_HOVER : hover ? 0xE51A282F : 0xD90D171C);
            graphics.fill(x, y, x + 3, y + ROW_HEIGHT, active ? GOLD : accent());
            graphics.text(font, fit(font, card.title(), w - 18), x + 10, y + 6,
                    active ? TEXT : MUTED, false);
            graphics.text(font, fit(font, card.subtitle(), w - 18), x + 10, y + 22,
                    active ? accent() : MUTED, false);
            y += ROW_HEIGHT + ROW_GAP;
        }
        graphics.disableScissor();
        if (maximum > 0) {
            int track = pane.height() - 12;
            int thumb = Math.max(14, track * Math.max(1, pane.height() - 12) / Math.max(1, content));
            int sy = pane.top() + 6 + (track - thumb) * scroll / maximum;
            graphics.fill(pane.right() - 4, pane.top() + 6, pane.right() - 2, pane.bottom() - 6, 0x555D6870);
            graphics.fill(pane.right() - 4, sy, pane.right() - 2, sy + thumb, accent());
        }
    }

    private void drawDetail(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), 0xDB0D171C);
        int left = pane.left() + 15;
        int right = pane.right() - 15;
        if (selected < 0 || selected >= actions.size()) {
            int y = pane.top() + 14;
            for (FormattedCharSequence line : font.split(Component.literal(body), Math.max(70, right - left))) {
                if (y > pane.bottom() - 14) break;
                graphics.text(font, line, left, y, TEXT, false);
                y += 12;
            }
            return;
        }

        ActionCard card = actions.get(selected);
        int y = pane.top() + 14;
        graphics.text(font, fit(font, card.title(), Math.max(60, right - left)), left, y, GOLD, false);
        y += 19;
        if (!card.subtitle().isBlank()) {
            for (FormattedCharSequence line : font.split(Component.literal(card.subtitle()), Math.max(70, right - left))) {
                if (y > pane.bottom() - 86) break;
                graphics.text(font, line, left, y, TEXT, false);
                y += 12;
            }
        }
        y += 5;
        graphics.fill(left, y, right, y + 1, BORDER);
        y += 11;
        String description = VillageActionDescriptions.describe(card.action(), card.title());
        for (FormattedCharSequence line : font.split(Component.literal(description), Math.max(70, right - left))) {
            if (y > pane.bottom() - 58) break;
            graphics.text(font, line, left, y, MUTED, false);
            y += 11;
        }

        Button button = actionButton(pane);
        boolean confirm = confirmationRequired(card.action());
        boolean hover = inside(mouseX, mouseY, button.x(), button.y(), button.w(), button.h());
        int edge = hover ? TEXT : confirm ? GOLD : accent();
        graphics.fill(button.x() - 1, button.y() - 1, button.x() + button.w() + 1,
                button.y() + button.h() + 1, edge);
        graphics.fill(button.x(), button.y(), button.x() + button.w(), button.y() + button.h(),
                hover ? PANEL_HOVER : PANEL_SOFT);
        graphics.centeredText(font, fit(font, VillageActionDescriptions.executeLabel(card.action()), button.w() - 10),
                button.x() + button.w() / 2, button.y() + 7, TEXT);
        if (confirm) graphics.text(font, "확인 후 실행", left, pane.bottom() - 20, GOLD, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 34, layout.top() + 8, 24, 24)) {
            onClose();
            return true;
        }
        if (!actions.isEmpty()) {
            int y = layout.list().top() + 6 - scroll;
            for (int i = 0; i < actions.size(); i++) {
                if (inside(click.x(), click.y(), layout.list().left() + 6, y,
                        layout.list().width() - 14, ROW_HEIGHT)) {
                    selected = i;
                    return true;
                }
                y += ROW_HEIGHT + ROW_GAP;
            }
        }
        if (selected >= 0 && selected < actions.size()) {
            Button button = actionButton(layout.detail());
            if (inside(click.x(), click.y(), button.x(), button.y(), button.w(), button.h())) {
                execute(actions.get(selected));
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Pane list = layout().list();
        if (inside(mouseX, mouseY, list.left(), list.top(), list.width(), list.height())) {
            scroll = Math.max(0, scroll - (int) Math.round(vertical * 32));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void execute(ActionCard card) {
        String action = card.action();
        if (confirmationRequired(action) && minecraft != null) {
            minecraft.gui.setScreen(new VillageConfirmScreen(this, action, card.title(),
                    VillageActionDescriptions.describe(action, card.title())));
        } else {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        }
    }

    private boolean confirmationRequired(String action) {
        if (action == null || action.startsWith("open_")) return false;
        return VillageActionDescriptions.requiresConfirmation(action)
                || action.startsWith("sell_item:")
                || action.startsWith("forge_enhance:")
                || action.startsWith("hire_mercenary:")
                || action.startsWith("defense_research:")
                || action.startsWith("research_skill_unlock:");
    }

    private Button actionButton(Pane pane) {
        int w = Math.min(148, Math.max(76, pane.width() / 3));
        w = Math.min(w, Math.max(1, pane.width() - 28));
        return new Button(pane.right() - w - 14, pane.bottom() - 34, w, 23);
    }

    private int accent() {
        String lower = heading.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("파괴") || lower.contains("수리")) return RED;
        if (lower.contains("강화") || lower.contains("보급") || lower.contains("방어탑")) return GOLD;
        return CYAN;
    }

    private Layout layout() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int panelWidth = Math.min(760, Math.max(280, safe.width() - 24));
        int panelHeight = Math.min(360, Math.max(210, safe.height() - 16));
        panelWidth = Math.min(panelWidth, safe.width());
        panelHeight = Math.min(panelHeight, safe.height());
        int left = safe.centerX() - panelWidth / 2;
        int top = safe.centerY() - panelHeight / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int contentTop = top + 52;
        int contentBottom = bottom - 11;
        int gap = 7;

        int availableHeight = Math.max(1, contentBottom - contentTop);
        if (panelWidth < 390 && panelHeight >= 250 && availableHeight >= 170) {
            int listHeight = VillageUiSafeArea.clamp(availableHeight * 38 / 100, 72,
                    Math.max(72, availableHeight - 94));
            Pane list = new Pane(left + 13, contentTop, right - 13, contentTop + listHeight);
            Pane detail = new Pane(left + 13, list.bottom() + gap, right - 13, contentBottom);
            return new Layout(left, top, right, bottom, list, detail);
        }
        int listWidth = VillageUiSafeArea.clamp(panelWidth * 30 / 100, 105, 220);
        listWidth = Math.min(listWidth, Math.max(86, panelWidth - 26 - gap - 112));
        Pane list = new Pane(left + 13, contentTop, left + 13 + listWidth, contentBottom);
        Pane detail = new Pane(list.right() + gap, contentTop, right - 13, contentBottom);
        return new Layout(left, top, right, bottom, list, detail);
    }

    private void parse(VillageNetwork.OpenVillageUiPayload payload) {
        String[] rawActions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(rawActions.length, labels.length);
        for (int i = 0; i < count; i++) {
            if ("facility_info".equals(rawActions[i])) continue;
            String[] p = labels[i].split("\\|", 2);
            actions.add(new ActionCard(rawActions[i], plain(p.length > 0 ? p[0] : rawActions[i]),
                    plain(p.length > 1 ? p[1] : "")));
        }
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
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

    private record ActionCard(String action, String title, String subtitle) {}
    private record Button(int x, int y, int w, int h) {}
    private record Pane(int left, int top, int right, int bottom) {
        int width() { return Math.max(1, right - left); }
        int height() { return Math.max(1, bottom - top); }
    }
    private record Layout(int left, int top, int right, int bottom, Pane list, Pane detail) {}
}
