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
    private static final int OVERLAY = 0xC705080D;
    private static final int PANEL = 0xFF0C1219;
    private static final int SURFACE = 0xFF131E28;
    private static final int SURFACE_HOVER = 0xFF1C2B38;
    private static final int BORDER = 0xFF405568;
    private static final int TEXT = 0xFFF3F7FA;
    private static final int MUTED = 0xFFA6B4C0;
    private static final int ACCENT = 0xFF43D6BC;
    private static final int GOLD = 0xFFF1C35D;
    private static final int RED = 0xFFE06A72;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private int selectedIndex = -1;
    private int bodyScroll;
    private int actionScroll;

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
        graphics.text(font, payload.title(), layout.left() + 16, layout.top() + 12, TEXT, false);
        graphics.text(font, subtitle(), layout.left() + 16, layout.top() + 29, MUTED, false);
        int closeX = layout.right() - 34;
        boolean hovered = inside(mouseX, mouseY, closeX, layout.top() + 10, 24, 24);
        graphics.fill(closeX, layout.top() + 10, closeX + 24, layout.top() + 34,
                hovered ? 0xFF6B3038 : SURFACE_HOVER);
        graphics.centeredText(font, "×", closeX + 12, layout.top() + 18, hovered ? TEXT : MUTED);
    }

    private void renderBody(GuiGraphicsExtractor graphics, Areas areas) {
        graphics.fill(areas.bodyLeft(), areas.bodyTop(), areas.bodyRight(), areas.bodyBottom(), SURFACE);
        int textLeft = areas.bodyLeft() + 12;
        int textRight = areas.bodyRight() - 12;
        List<FormattedCharSequence> lines = bodyLines(Math.max(80, textRight - textLeft));
        int contentHeight = lines.size() * 12;
        int visible = Math.max(1, areas.bodyBottom() - areas.bodyTop() - 18);
        int maxScroll = Math.max(0, contentHeight - visible);
        bodyScroll = Math.max(0, Math.min(maxScroll, bodyScroll));
        graphics.enableScissor(areas.bodyLeft(), areas.bodyTop(), areas.bodyRight(), areas.bodyBottom());
        int y = areas.bodyTop() + 9 - bodyScroll;
        for (FormattedCharSequence line : lines) {
            if (y + 10 >= areas.bodyTop() && y <= areas.bodyBottom()) graphics.text(font, line, textLeft, y, TEXT, false);
            y += 12;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, areas.bodyRight() - 4, areas.bodyTop() + 5,
                areas.bodyBottom() - 5, bodyScroll, maxScroll, visible, contentHeight);
    }

    private void renderActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        graphics.fill(areas.actionLeft(), areas.actionTop(), areas.actionRight(), areas.actionBottom(), 0xFF0A1017);
        int count = Math.min(actions.length, labels.length);
        int columns = areas.actionRight() - areas.actionLeft() >= 390 ? 2 : 1;
        int gap = 7;
        int cardHeight = 49;
        int cardWidth = Math.max(120,
                (areas.actionRight() - areas.actionLeft() - 20 - gap * (columns - 1)) / columns);
        int rows = (count + columns - 1) / columns;
        int contentHeight = rows * (cardHeight + gap) - (rows > 0 ? gap : 0);
        int visible = Math.max(1, areas.actionBottom() - areas.actionTop() - 16);
        int maxScroll = Math.max(0, contentHeight - visible);
        actionScroll = Math.max(0, Math.min(maxScroll, actionScroll));
        graphics.enableScissor(areas.actionLeft(), areas.actionTop(), areas.actionRight(), areas.actionBottom());
        for (int index = 0; index < count; index++) {
            int column = index % columns;
            int row = index / columns;
            int x = areas.actionLeft() + 10 + column * (cardWidth + gap);
            int y = areas.actionTop() + 8 + row * (cardHeight + gap) - actionScroll;
            boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, cardHeight);
            boolean selected = selectedIndex == index;
            int color = selected ? GOLD : hovered ? accent() : BORDER;
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + cardHeight + 1, color);
            graphics.fill(x, y, x + cardWidth, y + cardHeight, selected || hovered ? SURFACE_HOVER : SURFACE);
            graphics.fill(x, y, x + 4, y + cardHeight, accent());
            String[] parts = labelParts(labels[index]);
            graphics.text(font, compact(parts[0], Math.max(12, cardWidth / 7)), x + 13, y + 10, TEXT, false);
            graphics.text(font, compact(parts[1], Math.max(16, cardWidth / 6)), x + 13, y + 28, MUTED, false);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, areas.actionRight() - 4, areas.actionTop() + 5,
                areas.actionBottom() - 5, actionScroll, maxScroll, visible, contentHeight);
    }

    private void renderFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        graphics.fill(areas.footerLeft(), areas.footerTop(), areas.footerRight(), areas.footerBottom(), SURFACE);
        String title = selectedIndex < 0 ? "항목을 선택하세요" : labelParts(labels[selectedIndex])[0];
        String detail = selectedIndex < 0
                ? "항목을 선택하면 효과와 비용을 확인한 뒤 실행할 수 있습니다."
                : VillageActionDescriptions.describe(actions[selectedIndex], labels[selectedIndex]);
        graphics.text(font, compact(title, Math.max(18, areas.footerWidth() / 10)),
                areas.footerLeft() + 12, areas.footerTop() + 10, TEXT, false);
        int buttonWidth = Math.min(146, Math.max(96, areas.footerWidth() / 4));
        int buttonX = areas.footerRight() - buttonWidth - 12;
        int buttonY = areas.footerTop() + 24;
        int detailWidth = Math.max(80, buttonX - areas.footerLeft() - 26);
        List<FormattedCharSequence> lines = font.split(Component.literal(detail), detailWidth);
        int y = areas.footerTop() + 28;
        for (FormattedCharSequence line : lines) {
            if (y > areas.footerBottom() - 9) break;
            graphics.text(font, line, areas.footerLeft() + 12, y, MUTED, false);
            y += 11;
        }
        boolean active = selectedIndex >= 0;
        boolean hovered = active && inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 25);
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonWidth + 1, buttonY + 26,
                hovered ? GOLD : active ? accent() : BORDER);
        graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + 25,
                hovered ? 0xFF342D1B : active ? SURFACE_HOVER : 0xFF171E25);
        String execute = active ? VillageActionDescriptions.executeLabel(actions[selectedIndex]) : "선택 필요";
        graphics.centeredText(font, compact(execute, Math.max(10, buttonWidth / 7)),
                buttonX + buttonWidth / 2, buttonY + 8, active ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        Areas areas = areas(layout);
        if (inside(click.x(), click.y(), layout.right() - 34, layout.top() + 10, 24, 24)) {
            onClose(); return true;
        }
        int count = Math.min(actions.length, labels.length);
        int columns = areas.actionRight() - areas.actionLeft() >= 390 ? 2 : 1;
        int gap = 7;
        int cardHeight = 49;
        int cardWidth = Math.max(120,
                (areas.actionRight() - areas.actionLeft() - 20 - gap * (columns - 1)) / columns);
        for (int index = 0; index < count; index++) {
            int x = areas.actionLeft() + 10 + (index % columns) * (cardWidth + gap);
            int y = areas.actionTop() + 8 + (index / columns) * (cardHeight + gap) - actionScroll;
            if (inside(click.x(), click.y(), x, y, cardWidth, cardHeight)) {
                selectedIndex = index;
                return true;
            }
        }
        int buttonWidth = Math.min(146, Math.max(96, areas.footerWidth() / 4));
        int buttonX = areas.footerRight() - buttonWidth - 12;
        int buttonY = areas.footerTop() + 24;
        if (selectedIndex >= 0 && inside(click.x(), click.y(), buttonX, buttonY, buttonWidth, 25)) {
            executeSelected();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Areas areas = areas(layout());
        if (inside(mouseX, mouseY, areas.bodyLeft(), areas.bodyTop(),
                areas.bodyRight() - areas.bodyLeft(), areas.bodyBottom() - areas.bodyTop())) {
            bodyScroll = Math.max(0, bodyScroll - (int) Math.round(vertical * 24));
            return true;
        }
        if (inside(mouseX, mouseY, areas.actionLeft(), areas.actionTop(),
                areas.actionRight() - areas.actionLeft(), areas.actionBottom() - areas.actionTop())) {
            actionScroll = Math.max(0, actionScroll - (int) Math.round(vertical * 35));
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

    private List<FormattedCharSequence> bodyLines(int width) {
        List<FormattedCharSequence> result = new ArrayList<>();
        for (String paragraph : payload.body().split("\n", -1)) {
            if (paragraph.isBlank()) result.add(FormattedCharSequence.EMPTY);
            else result.addAll(font.split(Component.literal(paragraph), width));
        }
        return result;
    }

    private String[] labelParts(String label) {
        String[] raw = label.split("\\|", 2);
        return new String[]{raw.length > 0 ? raw[0] : label, raw.length > 1 ? raw[1] : "선택해 내용을 확인하세요"};
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                               int scroll, int maxScroll, int visible, int content) {
        if (maxScroll <= 0 || content <= 0) return;
        int height = bottom - top;
        int thumb = Math.max(14, height * visible / Math.max(visible, content));
        int y = top + (height - thumb) * scroll / maxScroll;
        graphics.fill(x, top, x + 3, bottom, 0xFF05080B);
        graphics.fill(x, y, x + 3, y + thumb, accent());
    }

    private int accent() {
        return switch (payload.screenId()) {
            case "game_over" -> RED;
            case "equipment_shop" -> GOLD;
            default -> ACCENT;
        };
    }

    private String subtitle() {
        return switch (payload.screenId()) {
            case "building" -> "시설 현장 기능";
            case "management" -> "시설 수리와 강화";
            case "equipment_shop" -> "레벨·방어 일수별 성장 장비";
            case "quick_chat" -> "빠른 신호";
            case "vote" -> "시간 진행 투표";
            case "game_over" -> "방어 실패";
            case "victory" -> "방어 성공";
            default -> "마을 수호단";
        };
    }

    private Layout layout() {
        int panelWidth = Math.max(310, Math.min(720, width - 12));
        int panelHeight = Math.max(250, Math.min(500, height - 10));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private Areas areas(Layout layout) {
        int left = layout.left() + 14;
        int right = layout.right() - 14;
        int top = layout.top() + 50;
        int footerHeight = Math.max(72, Math.min(92, layout.height() / 4));
        int footerTop = layout.bottom() - footerHeight - 12;
        int bodyHeight = Math.max(58, Math.min(112, (footerTop - top) / 3));
        int bodyBottom = top + bodyHeight;
        int actionTop = bodyBottom + 8;
        int actionBottom = Math.max(actionTop + 40, footerTop - 8);
        return new Areas(left, top, right, bodyBottom,
                left, actionTop, right, actionBottom,
                left, footerTop, right, layout.bottom() - 12);
    }

    private String compact(String value, int max) {
        String normalized = value.replace('\n', ' ');
        return normalized.length() <= max ? normalized : normalized.substring(0, Math.max(1, max - 1)) + "…";
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
        int footerWidth() { return footerRight - footerLeft; }
    }
}
