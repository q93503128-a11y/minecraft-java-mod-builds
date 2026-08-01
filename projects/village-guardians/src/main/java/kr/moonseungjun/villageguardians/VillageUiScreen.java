package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

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
    private static final int CARD_HEIGHT = 48;
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
        selectedIndex = Math.min(actions.length, labels.length) == 1 ? 0 : -1;
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
        Areas areas = areas(layout);
        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, layout.bottom(), accent());
        renderHeader(graphics, mouseX, mouseY, layout);
        renderBody(graphics, areas);
        renderActions(graphics, mouseX, mouseY, areas);
        renderFooter(graphics, mouseX, mouseY, areas);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int left = layout.left() + 19;
        int closeX = layout.right() - 39;
        graphics.text(font, payload.title(), left, layout.top() + 13, TEXT, false);
        List<FormattedCharSequence> subtitleLines = font.split(Component.literal(subtitle()),
                Math.max(100, closeX - left - 14));
        if (!subtitleLines.isEmpty()) {
            graphics.text(font, subtitleLines.getFirst(), left, layout.top() + 32, MUTED, false);
        }
        boolean hovered = inside(mouseX, mouseY, closeX, layout.top() + 10, 29, 29);
        graphics.fill(closeX, layout.top() + 10, closeX + 29, layout.top() + 39,
                hovered ? 0xFF79343D : SURFACE_HOVER);
        graphics.centeredText(font, "×", closeX + 14, layout.top() + 20, hovered ? TEXT : MUTED);
    }

    private void renderBody(GuiGraphicsExtractor graphics, Areas areas) {
        drawPanel(graphics, areas.bodyLeft(), areas.bodyTop(), areas.bodyRight(), areas.bodyBottom(), SURFACE);
        int textLeft = areas.bodyLeft() + 16;
        int textRight = areas.bodyRight() - 16;
        List<FormattedCharSequence> lines = bodyLines(Math.max(100, textRight - textLeft));
        int contentHeight = Math.max(1, lines.size() * 14 + 4);
        int textTop = areas.bodyTop() + 13;
        int textBottom = areas.bodyBottom() - 10;
        int visible = Math.max(1, textBottom - textTop);
        int maxScroll = Math.max(0, contentHeight - visible);
        bodyScroll = clamp(bodyScroll, 0, maxScroll);
        graphics.enableScissor(areas.bodyLeft() + 2, areas.bodyTop() + 2,
                areas.bodyRight() - 2, areas.bodyBottom() - 2);
        int y = textTop - bodyScroll;
        for (FormattedCharSequence line : lines) {
            if (y >= textTop - 10 && y <= textBottom) {
                graphics.text(font, line, textLeft, y, TEXT, false);
            }
            y += 14;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, areas.bodyRight() - 7, areas.bodyTop() + 8,
                areas.bodyBottom() - 8, bodyScroll, maxScroll, visible, contentHeight);
    }

    private void renderActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        drawPanel(graphics, areas.actionLeft(), areas.actionTop(), areas.actionRight(), areas.actionBottom(), SURFACE_DARK);
        int count = Math.min(actions.length, labels.length);
        int columns = areas.actionWidth() >= 470 ? 2 : 1;
        int cardWidth = Math.max(132,
                (areas.actionWidth() - 24 - CARD_GAP * (columns - 1)) / columns);
        int rows = count == 0 ? 0 : (count + columns - 1) / columns;
        int contentHeight = rows == 0 ? 0 : rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
        int visible = Math.max(1, areas.actionHeight() - 20);
        int maxScroll = Math.max(0, contentHeight - visible);
        actionScroll = clamp(actionScroll, 0, maxScroll);

        graphics.enableScissor(areas.actionLeft() + 2, areas.actionTop() + 2,
                areas.actionRight() - 2, areas.actionBottom() - 2);
        for (int index = 0; index < count; index++) {
            int x = areas.actionLeft() + 11 + (index % columns) * (cardWidth + CARD_GAP);
            int y = areas.actionTop() + 10 + (index / columns) * (CARD_HEIGHT + CARD_GAP) - actionScroll;
            boolean visibleCard = y + CARD_HEIGHT > areas.actionTop() && y < areas.actionBottom();
            boolean hovered = visibleCard && inside(mouseX, mouseY, x, y, cardWidth, CARD_HEIGHT);
            boolean selected = selectedIndex == index;
            int color = selected ? GOLD : hovered ? accent() : BORDER;
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + CARD_HEIGHT + 1, color);
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT,
                    selected || hovered ? SURFACE_HOVER : SURFACE);
            graphics.fill(x, y, x + 5, y + CARD_HEIGHT, selected ? GOLD : accent());
            String[] parts = labelParts(labels[index]);
            graphics.text(font, compact(parts[0], Math.max(13, cardWidth / 7)),
                    x + 15, y + 8, TEXT, false);
            List<FormattedCharSequence> detailLines = font.split(Component.literal(parts[1]),
                    Math.max(86, cardWidth - 30));
            if (!detailLines.isEmpty()) {
                graphics.text(font, detailLines.getFirst(), x + 15, y + 27, MUTED, false);
            }
        }
        graphics.disableScissor();
        drawScrollbar(graphics, areas.actionRight() - 7, areas.actionTop() + 8,
                areas.actionBottom() - 8, actionScroll, maxScroll, visible, contentHeight);
    }

    private void renderFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        drawPanel(graphics, areas.footerLeft(), areas.footerTop(), areas.footerRight(), areas.footerBottom(), SURFACE);
        String title = selectedIndex < 0 ? "기능을 선택하세요" : labelParts(labels[selectedIndex])[0];
        String detail = selectedIndex < 0
                ? "기능을 선택하면 핵심 효과와 비용이 표시됩니다."
                : VillageActionDescriptions.describe(actions[selectedIndex], labels[selectedIndex]);
        graphics.text(font, compact(title, Math.max(18, areas.footerWidth() / 7)),
                areas.footerLeft() + 13, areas.footerTop() + 11, TEXT, false);

        boolean tallPane = areas.footerHeight() >= 150;
        int buttonWidth;
        int buttonX;
        int buttonY = areas.footerBottom() - 38;
        int detailRight;
        int textBottom;
        if (tallPane) {
            buttonWidth = Math.max(100, areas.footerWidth() - 26);
            buttonX = areas.footerLeft() + 13;
            detailRight = areas.footerRight() - 13;
            textBottom = buttonY - 8;
        } else {
            buttonWidth = Math.min(160, Math.max(104, areas.footerWidth() / 4));
            buttonX = areas.footerRight() - buttonWidth - 13;
            detailRight = buttonX - 10;
            textBottom = areas.footerBottom() - 8;
        }

        int textTop = areas.footerTop() + 30;
        int detailWidth = Math.max(90, detailRight - areas.footerLeft() - 13);
        List<FormattedCharSequence> lines = font.split(Component.literal(detail), detailWidth);
        int contentHeight = Math.max(1, lines.size() * 12);
        int visible = Math.max(1, textBottom - textTop);
        int maxScroll = Math.max(0, contentHeight - visible);
        footerScroll = clamp(footerScroll, 0, maxScroll);
        graphics.enableScissor(areas.footerLeft() + 2, textTop, detailRight, textBottom);
        int y = textTop - footerScroll;
        for (FormattedCharSequence line : lines) {
            if (y >= textTop - 10 && y <= textBottom) {
                graphics.text(font, line, areas.footerLeft() + 13, y, MUTED, false);
            }
            y += 12;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, detailRight - 5, textTop, textBottom,
                footerScroll, maxScroll, visible, contentHeight);

        boolean active = selectedIndex >= 0;
        boolean hovered = active && inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 27);
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonWidth + 1, buttonY + 28,
                hovered ? GOLD : active ? accent() : BORDER);
        graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + 27,
                hovered ? 0xFF3D341E : active ? SURFACE_HOVER : 0xFF172028);
        String execute = active ? VillageActionDescriptions.executeLabel(actions[selectedIndex]) : "선택 필요";
        graphics.centeredText(font, compact(execute, Math.max(11, buttonWidth / 7)),
                buttonX + buttonWidth / 2, buttonY + 9, active ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        Areas areas = areas(layout);
        if (inside(click.x(), click.y(), layout.right() - 39, layout.top() + 10, 29, 29)) {
            onClose();
            return true;
        }

        int count = Math.min(actions.length, labels.length);
        int columns = areas.actionWidth() >= 470 ? 2 : 1;
        int cardWidth = Math.max(132,
                (areas.actionWidth() - 24 - CARD_GAP * (columns - 1)) / columns);
        for (int index = 0; index < count; index++) {
            int x = areas.actionLeft() + 11 + (index % columns) * (cardWidth + CARD_GAP);
            int y = areas.actionTop() + 10 + (index / columns) * (CARD_HEIGHT + CARD_GAP) - actionScroll;
            if (y + CARD_HEIGHT > areas.actionTop() && y < areas.actionBottom()
                    && inside(click.x(), click.y(), x, y, cardWidth, CARD_HEIGHT)) {
                selectedIndex = index;
                footerScroll = 0;
                return true;
            }
        }

        ButtonArea button = buttonArea(areas);
        if (selectedIndex >= 0 && inside(click.x(), click.y(),
                button.x(), button.y(), button.width(), button.height())) {
            executeSelected();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Areas areas = areas(layout());
        int amount = (int) Math.round(vertical * 42);
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
        boolean tallPane = areas.footerHeight() >= 150;
        int width = tallPane
                ? Math.max(100, areas.footerWidth() - 26)
                : Math.min(160, Math.max(104, areas.footerWidth() / 4));
        int x = tallPane ? areas.footerLeft() + 13 : areas.footerRight() - width - 13;
        return new ButtonArea(x, areas.footerBottom() - 38, width, 27);
    }

    private void executeSelected() {
        if (selectedIndex < 0 || selectedIndex >= Math.min(actions.length, labels.length)) return;
        String action = actions[selectedIndex];
        String label = labelParts(labels[selectedIndex])[0];
        String detail = VillageActionDescriptions.describe(action, labels[selectedIndex]);
        if (VillageActionDescriptions.requiresConfirmation(action) && minecraft != null) {
            minecraft.gui.setScreen(new VillageConfirmScreen(this, action, label, detail));
            return;
        }
        ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
    }

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
                raw.length > 1 ? raw[1] : "선택해 내용을 확인하세요"};
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, BORDER);
        graphics.fill(left, top, right, bottom, color);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                               int scroll, int maxScroll, int visible, int content) {
        if (maxScroll <= 0 || content <= visible || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(18, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(scroll, 0, maxScroll) / maxScroll;
        graphics.fill(x, top, x + 4, bottom, 0xFF05090D);
        graphics.fill(x, y, x + 4, y + thumb, accent());
    }

    private int accent() {
        return switch (payload.screenId()) {
            case "game_over" -> RED;
            case "equipment_shop", "funding", "tower_control", "tower_detail" -> GOLD;
            default -> ACCENT;
        };
    }

    private String subtitle() {
        return switch (payload.screenId()) {
            case "equipment_shop" -> "레벨·방어 일수별 성장 장비";
            case "caller" -> "휴대용 상태·통신·귀환 메뉴";
            case "tower_control" -> "회관 방어탑·성벽 지휘";
            case "tower_detail" -> "방어탑 전문 분기";
            case "funding" -> "개인 주화로 공동 보급품 조달";
            case "vote" -> "시간 진행 투표";
            case "game_over" -> "방어 실패";
            case "victory" -> "방어 성공";
            default -> "마을 수호단";
        };
    }

    private Layout layout() {
        int margin = 4;
        int panelWidth = Math.min(1060, Math.max(1, width - margin * 2));
        int panelHeight = Math.min(760, Math.max(1, height - margin * 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2,
                panelWidth, panelHeight);
    }

    private Areas areas(Layout layout) {
        int left = layout.left() + 15;
        int right = layout.right() - 15;
        int top = layout.top() + 57;
        int bottom = layout.bottom() - 13;
        int contentWidth = Math.max(1, right - left);
        int contentHeight = Math.max(1, bottom - top);
        boolean sideBySide = contentWidth >= 600 && contentHeight < 500;

        if (sideBySide) {
            int rightPaneWidth = clamp(contentWidth * 27 / 100, 190, 290);
            int split = right - rightPaneWidth;
            int bodyHeight = clamp(contentHeight * 50 / 100, 130, 245);
            return new Areas(
                    left, top, split - 7, top + bodyHeight,
                    left, top + bodyHeight + 9, split - 7, bottom,
                    split + 7, top, right, bottom);
        }

        int footerHeight = clamp(contentHeight * 16 / 100, 72, 98);
        int bodyHeight = clamp(contentHeight * 46 / 100, 150, 280);
        int actionTop = top + bodyHeight + 9;
        int footerTop = bottom - footerHeight;
        int minimumActionHeight = CARD_HEIGHT + 18;
        if (footerTop - actionTop - 9 < minimumActionHeight) {
            int shortage = minimumActionHeight - (footerTop - actionTop - 9);
            bodyHeight = Math.max(110, bodyHeight - shortage);
            actionTop = top + bodyHeight + 9;
        }
        return new Areas(
                left, top, right, top + bodyHeight,
                left, actionTop, right, footerTop - 9,
                left, footerTop, right, bottom);
    }

    private String compact(String value, int max) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        return normalized.length() <= max ? normalized
                : normalized.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Layout(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }
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
