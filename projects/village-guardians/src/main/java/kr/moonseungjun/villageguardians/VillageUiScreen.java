package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared action menu for facilities, funding, shops, towers and result screens.
 * The action viewport always receives a guaranteed height, even with a large
 * Minecraft GUI scale and a short logical framebuffer.
 */
public final class VillageUiScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0xB805080D;
    private static final int PANEL = 0xFF0A1017;
    private static final int SURFACE = 0xFF14212C;
    private static final int SURFACE_DARK = 0xFF0D171F;
    private static final int SURFACE_HOVER = 0xFF203342;
    private static final int BORDER = 0xFF587083;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFC1CDD6;
    private static final int ACCENT = 0xFF45D8C0;
    private static final int GOLD = 0xFFF4C861;
    private static final int RED = 0xFFE8757E;
    private static final int CARD_HEIGHT = 44;
    private static final int CARD_GAP = 6;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private int selectedIndex;
    private int bodyScroll;
    private int actionScroll;
    private int footerScroll;

    public VillageUiScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        selectedIndex = actionCount() > 0 ? 0 : -1;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, layout.bottom(), accent());
        renderHeader(graphics, mouseX, mouseY, layout);
        if (actionCount() <= 0) {
            renderReadOnlyBody(graphics, content(layout));
        } else {
            Areas areas = areas(layout);
            renderBody(graphics, areas);
            renderActions(graphics, mouseX, mouseY, areas);
            renderFooter(graphics, mouseX, mouseY, areas);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int left = layout.left() + 18;
        int closeX = layout.right() - 38;
        graphics.text(font, payload.title(), left, layout.top() + 11, TEXT, false);
        graphics.text(font, subtitle(), left, layout.top() + 29, MUTED, false);
        boolean hovered = inside(mouseX, mouseY, closeX, layout.top() + 8, 28, 28);
        graphics.fill(closeX, layout.top() + 8, closeX + 28, layout.top() + 36,
                hovered ? 0xFF79343D : SURFACE_HOVER);
        graphics.centeredText(font, "×", closeX + 14, layout.top() + 17, hovered ? TEXT : MUTED);
    }

    private void renderReadOnlyBody(GuiGraphicsExtractor graphics, Pane pane) {
        drawPanel(graphics, pane.left(), pane.top(), pane.right(), pane.bottom(), SURFACE);
        int textLeft = pane.left() + 18;
        int textTop = pane.top() + 16;
        int textRight = pane.right() - 18;
        int textBottom = pane.bottom() - 13;
        List<FormattedCharSequence> lines = bodyLines(Math.max(100, textRight - textLeft));
        int contentHeight = Math.max(1, lines.size() * 14 + 4);
        int visible = Math.max(1, textBottom - textTop);
        int maxScroll = Math.max(0, contentHeight - visible);
        bodyScroll = clamp(bodyScroll, 0, maxScroll);
        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2, pane.bottom() - 2);
        int y = textTop - bodyScroll;
        for (FormattedCharSequence line : lines) {
            if (y >= textTop - 11 && y <= textBottom) graphics.text(font, line, textLeft, y, TEXT, false);
            y += 14;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, pane.right() - 7, pane.top() + 8, pane.bottom() - 8,
                bodyScroll, maxScroll, visible, contentHeight);
    }

    private void renderBody(GuiGraphicsExtractor graphics, Areas areas) {
        drawPanel(graphics, areas.bodyLeft(), areas.bodyTop(), areas.bodyRight(), areas.bodyBottom(), SURFACE);
        int textLeft = areas.bodyLeft() + 14;
        int textTop = areas.bodyTop() + 10;
        int textRight = areas.bodyRight() - 14;
        int textBottom = areas.bodyBottom() - 8;
        List<FormattedCharSequence> lines = bodyLines(Math.max(100, textRight - textLeft));
        int contentHeight = Math.max(1, lines.size() * 12 + 2);
        int visible = Math.max(1, textBottom - textTop);
        int maxScroll = Math.max(0, contentHeight - visible);
        bodyScroll = clamp(bodyScroll, 0, maxScroll);
        graphics.enableScissor(areas.bodyLeft() + 2, areas.bodyTop() + 2,
                areas.bodyRight() - 2, areas.bodyBottom() - 2);
        int y = textTop - bodyScroll;
        for (FormattedCharSequence line : lines) {
            if (y >= textTop - 10 && y <= textBottom) graphics.text(font, line, textLeft, y, TEXT, false);
            y += 12;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, areas.bodyRight() - 7, areas.bodyTop() + 6, areas.bodyBottom() - 6,
                bodyScroll, maxScroll, visible, contentHeight);
    }

    private void renderActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        drawPanel(graphics, areas.actionLeft(), areas.actionTop(), areas.actionRight(), areas.actionBottom(), SURFACE_DARK);
        int count = actionCount();
        int columns = areas.actionWidth() >= 430 ? 2 : 1;
        int cardWidth = Math.max(118, (areas.actionWidth() - 22 - CARD_GAP * (columns - 1)) / columns);
        int rows = (count + columns - 1) / columns;
        int contentHeight = rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
        int viewportTop = areas.actionTop() + 8;
        int viewportBottom = areas.actionBottom() - 8;
        int visible = Math.max(1, viewportBottom - viewportTop);
        int maxScroll = Math.max(0, contentHeight - visible);
        actionScroll = clamp(actionScroll, 0, maxScroll);

        graphics.enableScissor(areas.actionLeft() + 2, areas.actionTop() + 2,
                areas.actionRight() - 2, areas.actionBottom() - 2);
        for (int index = 0; index < count; index++) {
            int x = areas.actionLeft() + 10 + (index % columns) * (cardWidth + CARD_GAP);
            int y = viewportTop + (index / columns) * (CARD_HEIGHT + CARD_GAP) - actionScroll;
            boolean visibleCard = y + CARD_HEIGHT > viewportTop && y < viewportBottom;
            boolean hovered = visibleCard && inside(mouseX, mouseY, x, y, cardWidth, CARD_HEIGHT);
            boolean selected = selectedIndex == index;
            int outline = selected ? GOLD : hovered ? accent() : BORDER;
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + CARD_HEIGHT + 1, outline);
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT,
                    selected || hovered ? SURFACE_HOVER : SURFACE);
            graphics.fill(x, y, x + 4, y + CARD_HEIGHT, selected ? GOLD : accent());
            String[] parts = labelParts(labels[index]);
            graphics.text(font, compact(parts[0], Math.max(13, cardWidth / 7)), x + 12, y + 7, TEXT, false);
            List<FormattedCharSequence> detail = font.split(Component.literal(parts[1]), Math.max(78, cardWidth - 24));
            if (!detail.isEmpty()) graphics.text(font, detail.getFirst(), x + 12, y + 25, MUTED, false);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, areas.actionRight() - 6, viewportTop, viewportBottom,
                actionScroll, maxScroll, visible, contentHeight);
    }

    private void renderFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        drawPanel(graphics, areas.footerLeft(), areas.footerTop(), areas.footerRight(), areas.footerBottom(), SURFACE);
        String[] parts = labelParts(labels[selectedIndex]);
        int buttonWidth = Math.min(158, Math.max(104, areas.footerWidth() / 4));
        int buttonX = areas.footerRight() - buttonWidth - 12;
        int buttonY = areas.footerTop() + Math.max(7, (areas.footerHeight() - 28) / 2);
        int textLeft = areas.footerLeft() + 13;
        int textRight = buttonX - 10;
        graphics.text(font, compact(parts[0], Math.max(14, (textRight - textLeft) / 7)),
                textLeft, areas.footerTop() + 8, TEXT, false);

        String detail = VillageActionDescriptions.describe(actions[selectedIndex], labels[selectedIndex]);
        List<FormattedCharSequence> lines = font.split(Component.literal(detail), Math.max(70, textRight - textLeft));
        int detailTop = areas.footerTop() + 25;
        int detailBottom = areas.footerBottom() - 7;
        int contentHeight = Math.max(1, lines.size() * 11);
        int visible = Math.max(1, detailBottom - detailTop);
        int maxScroll = Math.max(0, contentHeight - visible);
        footerScroll = clamp(footerScroll, 0, maxScroll);
        graphics.enableScissor(areas.footerLeft() + 2, detailTop, textRight, detailBottom);
        int y = detailTop - footerScroll;
        for (FormattedCharSequence line : lines) {
            if (y >= detailTop - 9 && y <= detailBottom) graphics.text(font, line, textLeft, y, MUTED, false);
            y += 11;
        }
        graphics.disableScissor();

        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 28);
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonWidth + 1, buttonY + 29,
                hovered ? GOLD : accent());
        graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + 28,
                hovered ? 0xFF3D341E : SURFACE_HOVER);
        graphics.centeredText(font,
                compact(VillageActionDescriptions.executeLabel(actions[selectedIndex]), Math.max(11, buttonWidth / 7)),
                buttonX + buttonWidth / 2, buttonY + 9, TEXT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 38, layout.top() + 8, 28, 28)) {
            onClose();
            return true;
        }
        if (actionCount() <= 0) return super.mouseClicked(click, doubled);

        Areas areas = areas(layout);
        int columns = areas.actionWidth() >= 430 ? 2 : 1;
        int cardWidth = Math.max(118, (areas.actionWidth() - 22 - CARD_GAP * (columns - 1)) / columns);
        int viewportTop = areas.actionTop() + 8;
        for (int index = 0; index < actionCount(); index++) {
            int x = areas.actionLeft() + 10 + (index % columns) * (cardWidth + CARD_GAP);
            int y = viewportTop + (index / columns) * (CARD_HEIGHT + CARD_GAP) - actionScroll;
            if (y + CARD_HEIGHT > viewportTop && y < areas.actionBottom() - 8
                    && inside(click.x(), click.y(), x, y, cardWidth, CARD_HEIGHT)) {
                selectedIndex = index;
                footerScroll = 0;
                return true;
            }
        }

        ButtonArea button = buttonArea(areas);
        if (inside(click.x(), click.y(), button.x(), button.y(), button.width(), button.height())) {
            executeSelected();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int amount = (int) Math.round(vertical * 38);
        if (actionCount() <= 0) {
            bodyScroll = Math.max(0, bodyScroll - amount);
            return true;
        }
        Areas areas = areas(layout());
        if (inside(mouseX, mouseY, areas.bodyLeft(), areas.bodyTop(), areas.bodyWidth(), areas.bodyHeight())) {
            bodyScroll = Math.max(0, bodyScroll - amount);
            return true;
        }
        if (inside(mouseX, mouseY, areas.actionLeft(), areas.actionTop(), areas.actionWidth(), areas.actionHeight())) {
            actionScroll = Math.max(0, actionScroll - amount);
            return true;
        }
        if (inside(mouseX, mouseY, areas.footerLeft(), areas.footerTop(), areas.footerWidth(), areas.footerHeight())) {
            footerScroll = Math.max(0, footerScroll - amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private ButtonArea buttonArea(Areas areas) {
        int width = Math.min(158, Math.max(104, areas.footerWidth() / 4));
        int x = areas.footerRight() - width - 12;
        int y = areas.footerTop() + Math.max(7, (areas.footerHeight() - 28) / 2);
        return new ButtonArea(x, y, width, 28);
    }

    private void executeSelected() {
        if (selectedIndex < 0 || selectedIndex >= actionCount()) return;
        String action = actions[selectedIndex];
        String label = labelParts(labels[selectedIndex])[0];
        String detail = VillageActionDescriptions.describe(action, labels[selectedIndex]);
        if (VillageActionDescriptions.requiresConfirmation(action) && minecraft != null) {
            minecraft.gui.setScreen(new VillageConfirmScreen(this, action, label, detail));
            return;
        }
        ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
    }

    private int actionCount() { return Math.min(actions.length, labels.length); }

    private List<FormattedCharSequence> bodyLines(int lineWidth) {
        List<FormattedCharSequence> result = new ArrayList<>();
        for (String paragraph : payload.body().split("\n", -1)) {
            if (paragraph.isBlank()) result.add(FormattedCharSequence.EMPTY);
            else result.addAll(font.split(Component.literal(paragraph), lineWidth));
        }
        return result;
    }

    private String[] labelParts(String label) {
        String[] raw = label.split("\\|", 2);
        return new String[]{raw.length > 0 ? raw[0] : label,
                raw.length > 1 ? raw[1] : "내용 확인"};
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, BORDER);
        graphics.fill(left, top, right, bottom, color);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                               int value, int maximum, int visible, int content) {
        if (maximum <= 0 || content <= visible || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(14, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(value, 0, maximum) / maximum;
        graphics.fill(x, top, x + 3, bottom, 0xFF05090D);
        graphics.fill(x, y, x + 3, y + thumb, accent());
    }

    private int accent() {
        return switch (payload.screenId()) {
            case "game_over" -> RED;
            case "equipment_shop", "funding", "tower_control", "tower_detail", "management" -> GOLD;
            default -> ACCENT;
        };
    }

    private String subtitle() {
        return switch (payload.screenId()) {
            case "building" -> "시설 기능 · 수리 · 강화";
            case "management" -> "내구도 복구와 시설 강화";
            case "equipment_shop" -> "레벨·방어 일수별 성장 장비";
            case "caller" -> "상태 · 통신 · 마을 귀환";
            case "tower_control" -> "성벽 강화 · 포탑 설치 조건 · 전문화";
            case "tower_detail" -> "포탑 설치 조건과 세 갈래 전문화";
            case "funding" -> "개인 주화로 공동 보급품 조달";
            case "vote" -> "시간 진행 투표";
            case "game_over" -> "방어 실패";
            case "victory" -> "방어 성공";
            default -> "마을 수호단";
        };
    }

    private Layout layout() {
        int margin = 4;
        int panelWidth = Math.min(1060, Math.max(300, width - margin * 2));
        int panelHeight = Math.min(760, Math.max(210, height - margin * 2));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 2));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private Pane content(Layout layout) {
        return new Pane(layout.left() + 14, layout.top() + 47,
                layout.right() - 14, layout.bottom() - 11);
    }

    private Areas areas(Layout layout) {
        Pane content = content(layout);
        int contentHeight = Math.max(1, content.height());
        int gap = 6;
        int bodyHeight = clamp(contentHeight / 5, 38, 62);
        int footerHeight = clamp(contentHeight / 4, 50, 72);
        int actionHeight = contentHeight - bodyHeight - footerHeight - gap * 2;
        if (actionHeight < CARD_HEIGHT + 12) {
            int missing = CARD_HEIGHT + 12 - actionHeight;
            int bodyCut = Math.min(Math.max(0, bodyHeight - 32), (missing + 1) / 2);
            bodyHeight -= bodyCut;
            missing -= bodyCut;
            footerHeight -= Math.min(Math.max(0, footerHeight - 44), missing);
            actionHeight = contentHeight - bodyHeight - footerHeight - gap * 2;
        }
        int bodyBottom = content.top() + bodyHeight;
        int actionTop = bodyBottom + gap;
        int actionBottom = Math.max(actionTop + 1, content.bottom() - footerHeight - gap);
        int footerTop = actionBottom + gap;
        return new Areas(
                content.left(), content.top(), content.right(), bodyBottom,
                content.left(), actionTop, content.right(), actionBottom,
                content.left(), footerTop, content.right(), content.bottom());
    }

    private String compact(String value, int maximum) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        return normalized.length() <= maximum ? normalized
                : normalized.substring(0, Math.max(1, maximum - 1)) + "…";
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Layout(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }
    }

    private record Pane(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }

    private record Areas(int bodyLeft, int bodyTop, int bodyRight, int bodyBottom,
                         int actionLeft, int actionTop, int actionRight, int actionBottom,
                         int footerLeft, int footerTop, int footerRight, int footerBottom) {
        int bodyWidth() { return bodyRight - bodyLeft; }
        int bodyHeight() { return bodyBottom - bodyTop; }
        int actionWidth() { return actionRight - actionLeft; }
        int actionHeight() { return actionBottom - actionTop; }
        int footerWidth() { return footerRight - footerLeft; }
        int footerHeight() { return footerBottom - footerTop; }
    }

    private record ButtonArea(int x, int y, int width, int height) {}
}
