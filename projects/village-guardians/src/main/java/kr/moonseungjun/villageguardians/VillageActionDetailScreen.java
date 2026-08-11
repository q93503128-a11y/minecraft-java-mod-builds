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
 * Detail-first action surface for facilities and management screens.
 * A list click only selects; impactful actions execute from a separate button and may request confirmation.
 */
public final class VillageActionDetailScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x70070A0D;
    private static final int TEXT = 0xFFF1F4F5;
    private static final int MUTED = 0xFFAAB5BA;
    private static final int CYAN = 0xFF52D9C2;
    private static final int GOLD = 0xFFFFC65C;
    private static final int RED = 0xFFE06E64;
    private static final int SURFACE = 0xD1131B1F;
    private static final int SURFACE_2 = 0xE51B282E;
    private static final int LINE = 0xA34B6873;

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
        VillageUiSafeArea.Rect safe = layout.safe();
        graphics.text(font, fit(font, heading, safe.width() - 18), safe.left() + 7, safe.top() + 4, CYAN, false);
        List<FormattedCharSequence> header = font.split(Component.literal(body), Math.max(80, safe.width() - 18));
        int y = safe.top() + 20;
        for (int i = 0; i < Math.min(2, header.size()); i++) {
            graphics.text(font, header.get(i), safe.left() + 7, y, MUTED, false);
            y += 11;
        }
        graphics.fill(safe.left() + 7, layout.dividerY(), safe.right() - 7, layout.dividerY() + 1, LINE);

        drawList(graphics, layout.list(), mouseX, mouseY);
        drawDetail(graphics, layout.detail(), mouseX, mouseY);
        graphics.text(font, "항목 선택 → 상세 확인 → 실행  ·  ESC 닫기", safe.left() + 4,
                safe.bottom() - 11, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), 0xB90D1519);
        if (actions.isEmpty()) {
            graphics.centeredText(font, "별도 조작 없이 자동 적용되는 시설입니다.",
                    pane.left() + pane.width() / 2, pane.top() + pane.height() / 2 - 5, CYAN);
            return;
        }
        int rowHeight = 47;
        int content = actions.size() * rowHeight;
        int maximum = Math.max(0, content - pane.height() + 10);
        scroll = VillageUiSafeArea.clamp(scroll, 0, maximum);
        graphics.enableScissor(pane.left(), pane.top(), pane.right(), pane.bottom());
        int y = pane.top() + 5 - scroll;
        for (int i = 0; i < actions.size(); i++) {
            ActionCard card = actions.get(i);
            int x = pane.left() + 5;
            int w = pane.width() - 10;
            int h = 42;
            boolean hover = inside(mouseX, mouseY, x, y, w, h);
            boolean active = selected == i;
            graphics.fill(x, y, x + w, y + h, active || hover ? SURFACE_2 : SURFACE);
            graphics.fill(x, y, x + 3, y + h, active ? GOLD : CYAN);
            graphics.text(font, fit(font, card.title(), w - 16), x + 10, y + 7,
                    active ? GOLD : TEXT, false);
            graphics.text(font, fit(font, card.subtitle(), w - 16), x + 10, y + 24, MUTED, false);
            y += rowHeight;
        }
        graphics.disableScissor();
        if (maximum > 0) {
            int track = pane.height() - 10;
            int thumb = Math.max(14, track * pane.height() / Math.max(pane.height(), content));
            int sy = pane.top() + 5 + (track - thumb) * scroll / maximum;
            graphics.fill(pane.right() - 3, pane.top() + 5, pane.right() - 1, pane.bottom() - 5, 0x555C686D);
            graphics.fill(pane.right() - 3, sy, pane.right() - 1, sy + thumb, CYAN);
        }
    }

    private void drawDetail(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), 0xC5121B20);
        if (selected < 0 || selected >= actions.size()) {
            List<FormattedCharSequence> lines = font.split(Component.literal(body), Math.max(80, pane.width() - 26));
            int y = pane.top() + 15;
            for (FormattedCharSequence line : lines) {
                if (y > pane.bottom() - 16) break;
                graphics.text(font, line, pane.left() + 13, y, TEXT, false);
                y += 12;
            }
            return;
        }
        ActionCard card = actions.get(selected);
        int left = pane.left() + 15;
        int right = pane.right() - 15;
        int y = pane.top() + 13;
        graphics.text(font, fit(font, card.title(), pane.width() - 30), left, y, GOLD, false);
        y += 21;
        if (!card.subtitle().isBlank()) {
            for (FormattedCharSequence line : font.split(Component.literal(card.subtitle()), Math.max(70, right - left))) {
                if (y > pane.bottom() - 70) break;
                graphics.text(font, line, left, y, TEXT, false);
                y += 12;
            }
        }
        y += 6;
        graphics.fill(left, y, right, y + 1, LINE);
        y += 12;
        String description = VillageActionDescriptions.describe(card.action(), card.title());
        for (FormattedCharSequence line : font.split(Component.literal(description), Math.max(70, right - left))) {
            if (y > pane.bottom() - 70) break;
            graphics.text(font, line, left, y, MUTED, false);
            y += 11;
        }

        int bw = Math.min(160, Math.max(96, pane.width() / 3));
        int bx = pane.right() - bw - 14;
        int by = pane.bottom() - 37;
        boolean hover = inside(mouseX, mouseY, bx, by, bw, 25);
        boolean confirm = confirmationRequired(card.action());
        graphics.fill(bx, by, bx + bw, by + 25, hover ? SURFACE_2 : SURFACE);
        graphics.fill(bx, by + 23, bx + bw, by + 25, hover ? GOLD : confirm ? GOLD : CYAN);
        String label = VillageActionDescriptions.executeLabel(card.action());
        graphics.centeredText(font, fit(font, label, bw - 10), bx + bw / 2, by + 8, TEXT);
        if (confirm) {
            graphics.text(font, "실행 전 확인창 표시", left, pane.bottom() - 21, MUTED, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (!actions.isEmpty()) {
            int y = layout.list().top() + 5 - scroll;
            for (int i = 0; i < actions.size(); i++) {
                if (inside(click.x(), click.y(), layout.list().left() + 5, y,
                        layout.list().width() - 10, 42)) {
                    selected = i;
                    return true;
                }
                y += 47;
            }
        }
        if (selected >= 0 && selected < actions.size()) {
            Pane pane = layout.detail();
            int bw = Math.min(160, Math.max(96, pane.width() / 3));
            int bx = pane.right() - bw - 14;
            int by = pane.bottom() - 37;
            if (inside(click.x(), click.y(), bx, by, bw, 25)) {
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
            scroll = Math.max(0, scroll - (int) Math.round(vertical * 38));
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

    private Layout layout() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int dividerY = safe.top() + (safe.height() < 250 ? 43 : 54);
        int top = dividerY + 7;
        int bottom = safe.bottom() - 18;
        int gap = 8;
        if (safe.width() < 390) {
            int split = top + Math.max(70, (bottom - top) * 42 / 100);
            return new Layout(safe, dividerY,
                    new Pane(safe.left() + 7, top, safe.right() - 7, split),
                    new Pane(safe.left() + 7, split + gap, safe.right() - 7, bottom));
        }
        int listWidth = VillageUiSafeArea.clamp(safe.width() * 34 / 100, 145, 320);
        Pane list = new Pane(safe.left() + 7, top, safe.left() + 7 + listWidth, bottom);
        Pane detail = new Pane(list.right() + gap, top, safe.right() - 7, bottom);
        return new Layout(safe, dividerY, list, detail);
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
    private record Pane(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
    private record Layout(VillageUiSafeArea.Rect safe, int dividerY, Pane list, Pane detail) {}
}
