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
    private static final int OVERLAY = 0xD005080D;
    private static final int PANEL = 0xFF0C1219;
    private static final int SURFACE = 0xFF131E28;
    private static final int SURFACE_HOVER = 0xFF1C2B38;
    private static final int BORDER = 0xFF405568;
    private static final int TEXT = 0xFFF3F7FA;
    private static final int MUTED = 0xFFA6B4C0;
    private static final int ACCENT = 0xFF43D6BC;
    private static final int GOLD = 0xFFF1C35D;
    private static final int RED = 0xFFE06A72;
    private static final int CARD_HEIGHT = 58;
    private static final int CARD_GAP = 8;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private int selectedIndex = -1;
    private int bodyScroll;
    private int actionScroll;
    private int footerScroll;

    public VillageUiScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
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
        graphics.fill(layout.left(), layout.top(), layout.left() + 4, layout.bottom(), accent());
        renderHeader(graphics, mouseX, mouseY, layout);
        renderBody(graphics, areas);
        renderActions(graphics, mouseX, mouseY, areas);
        renderFooter(graphics, mouseX, mouseY, areas);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int left = layout.left() + 18;
        int closeX = layout.right() - 37;
        graphics.text(font, payload.title(), left, layout.top() + 12, TEXT, false);
        List<FormattedCharSequence> subtitleLines = font.split(Component.literal(subtitle()),
                Math.max(100, closeX - left - 12));
        if (!subtitleLines.isEmpty()) {
            graphics.text(font, subtitleLines.getFirst(), left, layout.top() + 31, MUTED, false);
        }
        boolean hovered = inside(mouseX, mouseY, closeX, layout.top() + 10, 27, 27);
        graphics.fill(closeX, layout.top() + 10, closeX + 27, layout.top() + 37,
                hovered ? 0xFF6B3038 : SURFACE_HOVER);
        graphics.centeredText(font, "×", closeX + 13, layout.top() + 19, hovered ? TEXT : MUTED);
    }

    private void renderBody(GuiGraphicsExtractor graphics, Areas areas) {
        graphics.fill(areas.bodyLeft(), areas.bodyTop(), areas.bodyRight(), areas.bodyBottom(), SURFACE);
        int textLeft = areas.bodyLeft() + 14;
        int textRight = areas.bodyRight() - 14;
        List<FormattedCharSequence> lines = bodyLines(Math.max(100, textRight - textLeft));
        int contentHeight = Math.max(1, lines.size() * 12);
        int visible = Math.max(1, areas.bodyBottom() - areas.bodyTop() - 18);
        int maxScroll = Math.max(0, contentHeight - visible);
        bodyScroll = clamp(bodyScroll, 0, maxScroll);
        graphics.enableScissor(areas.bodyLeft(), areas.bodyTop(), areas.bodyRight(), areas.bodyBottom());
        int y = areas.bodyTop() + 9 - bodyScroll;
        for (FormattedCharSequence line : lines) {
            if (y + 10 >= areas.bodyTop() && y <= areas.bodyBottom()) {
                graphics.text(font, line, textLeft, y, TEXT, false);
            }
            y += 12;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, areas.bodyRight() - 5, areas.bodyTop() + 5,
                areas.bodyBottom() - 5, bodyScroll, maxScroll, visible, contentHeight);
    }

    private void renderActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        graphics.fill(areas.actionLeft(), areas.actionTop(), areas.actionRight(), areas.actionBottom(), 0xFF0A1017);
        int count = Math.min(actions.length, labels.length);
        int columns = areas.actionWidth() >= 520 ? 2 : 1;
        int cardWidth = Math.max(140,
                (areas.actionWidth() - 24 - CARD_GAP * (columns - 1)) / columns);
        int rows = Math.max(0, (count + columns - 1) / columns);
        int contentHeight = rows == 0 ? 0
                : rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
        int visible = Math.max(1, areas.actionHeight() - 18);
        int maxScroll = Math.max(0, contentHeight - visible);
        actionScroll = clamp(actionScroll, 0, maxScroll);

        graphics.enableScissor(areas.actionLeft(), areas.actionTop(), areas.actionRight(), areas.actionBottom());
        for (int index = 0; index < count; index++) {
            int x = areas.actionLeft() + 10 + (index % columns) * (cardWidth + CARD_GAP);
            int y = areas.actionTop() + 9 + (index / columns) * (CARD_HEIGHT + CARD_GAP) - actionScroll;
            boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, CARD_HEIGHT);
            boolean selected = selectedIndex == index;
            int color = selected ? GOLD : hovered ? accent() : BORDER;
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + CARD_HEIGHT + 1, color);
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT,
                    selected || hovered ? SURFACE_HOVER : SURFACE);
            graphics.fill(x, y, x + 5, y + CARD_HEIGHT, accent());
            String[] parts = labelParts(labels[index]);
            graphics.text(font, compact(parts[0], Math.max(14, cardWidth / 7)),
                    x + 15, y + 10, TEXT, false);
            List<FormattedCharSequence> detailLines = font.split(Component.literal(parts[1]),
                    Math.max(90, cardWidth - 30));
            int lineY = y + 29;
            for (int line = 0; line < Math.min(2, detailLines.size()); line++) {
                graphics.text(font, detailLines.get(line), x + 15, lineY, MUTED, false);
                lineY += 11;
            }
        }
        graphics.disableScissor();
        drawScrollbar(graphics, areas.actionRight() - 5, areas.actionTop() + 5,
                areas.actionBottom() - 5, actionScroll, maxScroll, visible, contentHeight);
    }

    private void renderFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        graphics.fill(areas.footerLeft(), areas.footerTop(), areas.footerRight(), areas.footerBottom(), SURFACE);
        String title = selectedIndex < 0 ? "항목을 선택하세요" : labelParts(labels[selectedIndex])[0];
        String detail = selectedIndex < 0
                ? "위 목록에서 항목을 선택하면 효과와 비용을 확인한 뒤 실행할 수 있습니다."
                : VillageActionDescriptions.describe(actions[selectedIndex], labels[selectedIndex]);
        graphics.text(font, compact(title, Math.max(20, areas.footerWidth() / 8)),
                areas.footerLeft() + 14, areas.footerTop() + 11, TEXT, false);

        int buttonWidth = Math.min(180, Math.max(110, areas.footerWidth() / 4));
        int buttonX = areas.footerRight() - buttonWidth - 14;
        int buttonY = areas.footerBottom() - 40;
        int detailRight = buttonX - 12;
        int detailWidth = Math.max(100, detailRight - areas.footerLeft() - 14);
        List<FormattedCharSequence> lines = font.split(Component.literal(detail), detailWidth);
        int contentHeight = Math.max(1, lines.size() * 12);
        int textTop = areas.footerTop() + 31;
        int textBottom = areas.footerBottom() - 9;
        int visible = Math.max(1, textBottom - textTop);
        int maxScroll = Math.max(0, contentHeight - visible);
        footerScroll = clamp(footerScroll, 0, maxScroll);
        graphics.enableScissor(areas.footerLeft(), textTop, detailRight, textBottom);
        int y = textTop - footerScroll;
        for (FormattedCharSequence line : lines) {
            if (y + 10 >= textTop && y <= textBottom) {
                graphics.text(font, line, areas.footerLeft() + 14, y, MUTED, false);
            }
            y += 12;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, detailRight - 4, textTop, textBottom,
                footerScroll, maxScroll, visible, contentHeight);

        boolean active = selectedIndex >= 0;
        boolean hovered = active && inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 29);
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonWidth + 1, buttonY + 30,
                hovered ? GOLD : active ? accent() : BORDER);
        graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + 29,
                hovered ? 0xFF342D1B : active ? SURFACE_HOVER : 0xFF171E25);
        String execute = active ? VillageActionDescriptions.executeLabel(actions[selectedIndex]) : "선택 필요";
        graphics.centeredText(font, compact(execute, Math.max(11, buttonWidth / 7)),
                buttonX + buttonWidth / 2, buttonY + 10, active ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        Areas areas = areas(layout);
        if (inside(click.x(), click.y(), layout.right() - 37, layout.top() + 10, 27, 27)) {
            onClose();
            return true;
        }

        int count = Math.min(actions.length, labels.length);
        int columns = areas.actionWidth() >= 520 ? 2 : 1;
        int cardWidth = Math.max(140,
                (areas.actionWidth() - 24 - CARD_GAP * (columns - 1)) / columns);
        for (int index = 0; index < count; index++) {
            int x = areas.actionLeft() + 10 + (index % columns) * (cardWidth + CARD_GAP);
            int y = areas.actionTop() + 9 + (index / columns) * (CARD_HEIGHT + CARD_GAP) - actionScroll;
            if (inside(click.x(), click.y(), x, y, cardWidth, CARD_HEIGHT)) {
                selectedIndex = index;
                footerScroll = 0;
                return true;
            }
        }

        int buttonWidth = Math.min(180, Math.max(110, areas.footerWidth() / 4));
        int buttonX = areas.footerRight() - buttonWidth - 14;
        int buttonY = areas.footerBottom() - 40;
        if (selectedIndex >= 0
                && inside(click.x(), click.y(), buttonX, buttonY, buttonWidth, 29)) {
            executeSelected();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Areas areas = areas(layout());
        int amount = (int) Math.round(vertical * 40);
        if (inside(mouseX, mouseY, areas.bodyLeft(), areas.bodyTop(),
                areas.bodyWidth(), areas.bodyHeight())) {
            bodyScroll = Math.max(0, bodyScroll - amount);
            return true;
        }
        if (inside(mouseX, mouseY, areas.actionLeft(), areas.actionTop(),
                areas.actionWidth(), areas.actionHeight())) {
            actionScroll = Math.max(0, actionScroll - amount);
            return true;
        }
        if (inside(mouseX, mouseY, areas.footerLeft(), areas.footerTop(),
                areas.footerWidth(), areas.footerHeight())) {
            footerScroll = Math.max(0, footerScroll - amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
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

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                               int scroll, int maxScroll, int visible, int content) {
        if (maxScroll <= 0 || content <= visible) return;
        int track = Math.max(1, bottom - top);
        int thumb = Math.max(18, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(scroll, 0, maxScroll) / maxScroll;
        graphics.fill(x, top, x + 3, bottom, 0xFF05080B);
        graphics.fill(x, y, x + 3, y + thumb, accent());
    }

    private int accent() {
        return switch (payload.screenId()) {
            case "game_over" -> RED;
            case "equipment_shop", "funding", "tower_control" -> GOLD;
            default -> ACCENT;
        };
    }

    private String subtitle() {
        return switch (payload.screenId()) {
            case "building" -> "시설 현장 기능";
            case "management" -> "시설 수리와 강화";
            case "equipment_shop" -> "레벨·방어 일수별 성장 장비";
            case "quick_chat" -> "빠른 수호단 신호";
            case "caller" -> "휴대용 상태·통신·귀환 메뉴";
            case "tower_control" -> "회관 방어탑·성벽 지휘";
            case "funding" -> "개인 주화로 공동 보급품 조달";
            case "vote" -> "시간 진행 투표";
            case "game_over" -> "방어 실패";
            case "victory" -> "방어 성공";
            default -> "마을 수호단";
        };
    }

    private Layout layout() {
        int margin = 5;
        int panelWidth = Math.max(350, Math.min(940, width - margin * 2));
        int panelHeight = Math.max(270, Math.min(620, height - margin * 2));
        panelWidth = Math.min(panelWidth, width - 2);
        panelHeight = Math.min(panelHeight, height - 2);
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2,
                panelWidth, panelHeight);
    }

    private Areas areas(Layout layout) {
        int left = layout.left() + 14;
        int right = layout.right() - 14;
        int contentTop = layout.top() + 55;
        int footerHeight = Math.max(88, Math.min(108, layout.height() / 4));
        int footerTop = layout.bottom() - footerHeight - 12;
        int available = Math.max(100, footerTop - contentTop - 8);
        int bodyHeight = Math.max(62, Math.min(104, available / 3));
        int bodyBottom = contentTop + bodyHeight;
        int actionTop = bodyBottom + 8;
        int actionBottom = Math.max(actionTop + 50, footerTop - 8);
        return new Areas(left, contentTop, right, bodyBottom,
                left, actionTop, right, actionBottom,
                left, footerTop, right, layout.bottom() - 12);
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
}
