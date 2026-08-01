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
 * Facility-only interface. Information receives the large pane while the action
 * selector and execute control stay compact and separate.
 */
public final class VillageFacilityScreen extends Screen {
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
    private static final int CARD_HEIGHT = 44;
    private static final int CARD_GAP = 6;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private int selectedIndex;
    private int informationScroll;
    private int actionScroll;

    public VillageFacilityScreen(VillageNetwork.OpenVillageUiPayload payload) {
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
        renderInformation(graphics, areas);
        renderActions(graphics, mouseX, mouseY, areas);
        renderExecuteButton(graphics, mouseX, mouseY, areas);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int titleX = layout.left() + 20;
        int closeX = layout.right() - 39;
        graphics.text(font, payload.title(), titleX, layout.top() + 14, TEXT, false);
        graphics.text(font, payload.screenId().equals("management")
                        ? "현장에서 바로 수리·강화" : "시설 기능과 현재 효과",
                titleX, layout.top() + 33, MUTED, false);
        boolean hovered = inside(mouseX, mouseY, closeX, layout.top() + 10, 29, 29);
        graphics.fill(closeX, layout.top() + 10, closeX + 29, layout.top() + 39,
                hovered ? 0xFF79343D : SURFACE_HOVER);
        graphics.centeredText(font, "×", closeX + 14, layout.top() + 20, hovered ? TEXT : MUTED);
    }

    private void renderInformation(GuiGraphicsExtractor graphics, Areas areas) {
        drawPanel(graphics, areas.infoLeft(), areas.infoTop(), areas.infoRight(), areas.infoBottom(), SURFACE);
        int textLeft = areas.infoLeft() + 18;
        int textRight = areas.infoRight() - 18;
        int textTop = areas.infoTop() + 17;
        int textBottom = areas.infoBottom() - 13;
        List<FormattedCharSequence> lines = informationLines(Math.max(110, textRight - textLeft));
        int contentHeight = Math.max(1, lines.size() * 15 + 5);
        int visible = Math.max(1, textBottom - textTop);
        int maxScroll = Math.max(0, contentHeight - visible);
        informationScroll = clamp(informationScroll, 0, maxScroll);

        graphics.enableScissor(areas.infoLeft() + 2, areas.infoTop() + 2,
                areas.infoRight() - 2, areas.infoBottom() - 2);
        int y = textTop - informationScroll;
        for (FormattedCharSequence line : lines) {
            if (y >= textTop - 12 && y <= textBottom) graphics.text(font, line, textLeft, y, TEXT, false);
            y += 15;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, areas.infoRight() - 8, areas.infoTop() + 9, areas.infoBottom() - 9,
                informationScroll, maxScroll, visible, contentHeight);
    }

    private void renderActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        drawPanel(graphics, areas.actionLeft(), areas.actionTop(), areas.actionRight(), areas.actionBottom(), SURFACE_DARK);
        graphics.text(font, "기능", areas.actionLeft() + 13, areas.actionTop() + 11, MUTED, false);
        int count = Math.min(actions.length, labels.length);
        int cardWidth = Math.max(116, areas.actionWidth() - 22);
        int contentHeight = count == 0 ? 0 : count * CARD_HEIGHT + Math.max(0, count - 1) * CARD_GAP;
        int listTop = areas.actionTop() + 30;
        int visible = Math.max(1, areas.actionBottom() - listTop - 9);
        int maxScroll = Math.max(0, contentHeight - visible);
        actionScroll = clamp(actionScroll, 0, maxScroll);

        graphics.enableScissor(areas.actionLeft() + 2, listTop,
                areas.actionRight() - 2, areas.actionBottom() - 2);
        for (int index = 0; index < count; index++) {
            int x = areas.actionLeft() + 11;
            int y = listTop + index * (CARD_HEIGHT + CARD_GAP) - actionScroll;
            boolean visibleCard = y + CARD_HEIGHT > listTop && y < areas.actionBottom();
            boolean hovered = visibleCard && inside(mouseX, mouseY, x, y, cardWidth, CARD_HEIGHT);
            boolean selected = selectedIndex == index;
            int outline = selected ? GOLD : hovered ? accent() : BORDER;
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + CARD_HEIGHT + 1, outline);
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT,
                    selected || hovered ? SURFACE_HOVER : SURFACE);
            graphics.fill(x, y, x + 4, y + CARD_HEIGHT, selected ? GOLD : accent());
            String[] parts = labelParts(labels[index]);
            graphics.text(font, compact(parts[0], Math.max(14, cardWidth / 7)), x + 12, y + 8, TEXT, false);
            List<FormattedCharSequence> oneLine = font.split(Component.literal(parts[1]), Math.max(80, cardWidth - 24));
            if (!oneLine.isEmpty()) graphics.text(font, oneLine.getFirst(), x + 12, y + 25, MUTED, false);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, areas.actionRight() - 7, listTop + 4, areas.actionBottom() - 7,
                actionScroll, maxScroll, visible, contentHeight);
    }

    private void renderExecuteButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        boolean active = selectedIndex >= 0;
        boolean hovered = active && inside(mouseX, mouseY,
                areas.buttonLeft(), areas.buttonTop(), areas.buttonWidth(), areas.buttonHeight());
        graphics.fill(areas.buttonLeft() - 1, areas.buttonTop() - 1,
                areas.buttonRight() + 1, areas.buttonBottom() + 1,
                hovered ? GOLD : active ? accent() : BORDER);
        graphics.fill(areas.buttonLeft(), areas.buttonTop(), areas.buttonRight(), areas.buttonBottom(),
                hovered ? 0xFF3D341E : active ? SURFACE_HOVER : 0xFF172028);
        String text = active ? VillageActionDescriptions.executeLabel(actions[selectedIndex]) : "기능 선택";
        graphics.centeredText(font, compact(text, Math.max(12, areas.buttonWidth() / 7)),
                areas.buttonLeft() + areas.buttonWidth() / 2, areas.buttonTop() + 10,
                active ? TEXT : MUTED);
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
        int cardWidth = Math.max(116, areas.actionWidth() - 22);
        int listTop = areas.actionTop() + 30;
        for (int index = 0; index < count; index++) {
            int x = areas.actionLeft() + 11;
            int y = listTop + index * (CARD_HEIGHT + CARD_GAP) - actionScroll;
            if (y + CARD_HEIGHT > listTop && y < areas.actionBottom()
                    && inside(click.x(), click.y(), x, y, cardWidth, CARD_HEIGHT)) {
                selectedIndex = index;
                informationScroll = 0;
                return true;
            }
        }
        if (selectedIndex >= 0 && inside(click.x(), click.y(),
                areas.buttonLeft(), areas.buttonTop(), areas.buttonWidth(), areas.buttonHeight())) {
            executeSelected();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Areas areas = areas(layout());
        int amount = (int) Math.round(vertical * 42);
        if (inside(mouseX, mouseY, areas.infoLeft(), areas.infoTop(), areas.infoWidth(), areas.infoHeight())) {
            informationScroll = Math.max(0, informationScroll - amount);
            return true;
        }
        if (inside(mouseX, mouseY, areas.actionLeft(), areas.actionTop(), areas.actionWidth(), areas.actionHeight())) {
            actionScroll = Math.max(0, actionScroll - amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void executeSelected() {
        if (selectedIndex < 0 || selectedIndex >= Math.min(actions.length, labels.length)) return;
        String action = actions[selectedIndex];
        String[] parts = labelParts(labels[selectedIndex]);
        String cleanDetail = parts[0] + "\n" + parts[1];
        String description = VillageActionDescriptions.describe(action, cleanDetail);
        if (VillageActionDescriptions.requiresConfirmation(action) && minecraft != null) {
            minecraft.gui.setScreen(new VillageConfirmScreen(this, action, parts[0], description));
            return;
        }
        ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
    }

    private List<FormattedCharSequence> informationLines(int lineWidth) {
        List<FormattedCharSequence> result = new ArrayList<>();
        appendParagraphs(result, payload.body(), lineWidth);
        result.add(FormattedCharSequence.EMPTY);
        result.addAll(font.split(Component.literal(selectedIndex < 0 ? "§b기능 설명" : "§e선택한 기능"), lineWidth));
        result.add(FormattedCharSequence.EMPTY);
        if (selectedIndex < 0) {
            result.addAll(font.split(Component.literal("§7오른쪽의 기능을 선택하면 효과와 비용이 이 넓은 영역에 표시됩니다."), lineWidth));
        } else {
            String[] parts = labelParts(labels[selectedIndex]);
            result.addAll(font.split(Component.literal("§f" + parts[0]), lineWidth));
            result.addAll(font.split(Component.literal("§7" + parts[1]), lineWidth));
            result.add(FormattedCharSequence.EMPTY);
            result.addAll(font.split(Component.literal("§f" + VillageActionDescriptions.describe(
                    actions[selectedIndex], parts[0] + " · " + parts[1])), lineWidth));
        }
        return result;
    }

    private void appendParagraphs(List<FormattedCharSequence> result, String value, int lineWidth) {
        for (String paragraph : value.split("\n", -1)) {
            if (paragraph.isBlank()) result.add(FormattedCharSequence.EMPTY);
            else result.addAll(font.split(Component.literal(paragraph), lineWidth));
        }
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
                               int value, int maximum, int visible, int content) {
        if (maximum <= 0 || content <= visible || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(18, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(value, 0, maximum) / maximum;
        graphics.fill(x, top, x + 4, bottom, 0xFF05090D);
        graphics.fill(x, y, x + 4, y + thumb, accent());
    }

    private int accent() { return payload.screenId().equals("management") ? GOLD : ACCENT; }

    private Layout layout() {
        int margin = 5;
        int panelWidth = Math.min(1060, Math.max(320, width - margin * 2));
        int panelHeight = Math.min(760, Math.max(240, height - margin * 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private Areas areas(Layout layout) {
        int left = layout.left() + 17;
        int right = layout.right() - 17;
        int top = layout.top() + 58;
        int bottom = layout.bottom() - 15;
        int contentWidth = right - left;
        int contentHeight = bottom - top;
        int buttonHeight = 32;

        if (contentWidth >= 610) {
            int actionWidth = clamp(contentWidth * 29 / 100, 205, 300);
            int actionLeft = right - actionWidth;
            int buttonTop = bottom - buttonHeight;
            return new Areas(
                    left, top, actionLeft - 10, bottom,
                    actionLeft, top, right, buttonTop - 8,
                    actionLeft, buttonTop, right, bottom);
        }

        int infoHeight = clamp(contentHeight * 57 / 100, 120, Math.max(120, contentHeight - 120));
        int actionTop = top + infoHeight + 8;
        int buttonTop = bottom - buttonHeight;
        return new Areas(
                left, top, right, top + infoHeight,
                left, actionTop, right, buttonTop - 8,
                left, buttonTop, right, bottom);
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

    private record Areas(int infoLeft, int infoTop, int infoRight, int infoBottom,
                         int actionLeft, int actionTop, int actionRight, int actionBottom,
                         int buttonLeft, int buttonTop, int buttonRight, int buttonBottom) {
        int infoWidth() { return infoRight - infoLeft; }
        int infoHeight() { return infoBottom - infoTop; }
        int actionWidth() { return actionRight - actionLeft; }
        int actionHeight() { return actionBottom - actionTop; }
        int buttonWidth() { return buttonRight - buttonLeft; }
        int buttonHeight() { return buttonBottom - buttonTop; }
    }
}
