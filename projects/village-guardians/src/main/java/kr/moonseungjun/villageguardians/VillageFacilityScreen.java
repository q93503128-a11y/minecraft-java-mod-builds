package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Bright game-style menu with navigation on the left and information/action on the right. */
public final class VillageFacilityScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x65000000;
    private static final int PANEL = 0xFFF0E5CC;
    private static final int SURFACE = 0xFFFFF8E8;
    private static final int SURFACE_ALT = 0xFFE6D9BE;
    private static final int SELECTED = 0xFFFFE2A8;
    private static final int BORDER = 0xFF75634C;
    private static final int TEXT = 0xFF241D17;
    private static final int MUTED = 0xFF6D6256;
    private static final int TEAL = 0xFF2E8E80;
    private static final int GOLD = 0xFFC78B2D;
    private static final int RED = 0xFFB95050;
    private static final int CARD_HEIGHT = 48;
    private static final int CARD_GAP = 6;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private int selectedIndex;
    private int listScroll;
    private int detailScroll;

    public VillageFacilityScreen(VillageNetwork.OpenVillageUiPayload payload) {
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
        Areas areas = areas(layout);
        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, layout.bottom(), accent());
        renderHeader(graphics, mouseX, mouseY, layout);
        renderList(graphics, mouseX, mouseY, areas);
        renderDetail(graphics, mouseX, mouseY, areas);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int left = layout.left() + 20;
        int closeX = layout.right() - 39;
        graphics.text(font, payload.title(), left, layout.top() + 13, TEXT, false);
        String subtitle = switch (payload.screenId()) {
            case "funding" -> "공동 보급품 조달";
            case "tower_control", "tower_detail" -> "성벽과 방어탑 지휘";
            case "equipment_shop" -> "장비와 전투 물자";
            case "caller" -> "상태·통신·귀환";
            case "management" -> "회관 전용 시설 관리";
            default -> "시설 현장 기능";
        };
        graphics.text(font, subtitle, left, layout.top() + 32, MUTED, false);
        boolean hovered = inside(mouseX, mouseY, closeX, layout.top() + 10, 29, 29);
        graphics.fill(closeX, layout.top() + 10, closeX + 29, layout.top() + 39,
                hovered ? 0xFFE6A6A6 : SURFACE_ALT);
        graphics.centeredText(font, "×", closeX + 14, layout.top() + 20, hovered ? RED : TEXT);
    }

    private void renderList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        panel(graphics, areas.listLeft(), areas.listTop(), areas.listRight(), areas.listBottom(), SURFACE_ALT);
        graphics.text(font, "선택", areas.listLeft() + 13, areas.listTop() + 11, MUTED, false);
        int count = actionCount();
        int listTop = areas.listTop() + 31;
        int visibleHeight = Math.max(1, areas.listBottom() - listTop - 9);
        int contentHeight = count == 0 ? 0 : count * CARD_HEIGHT + Math.max(0, count - 1) * CARD_GAP;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        listScroll = clamp(listScroll, 0, maxScroll);
        int cardWidth = Math.max(90, areas.listWidth() - 24);

        graphics.enableScissor(areas.listLeft() + 2, listTop, areas.listRight() - 2, areas.listBottom() - 2);
        for (int index = 0; index < count; index++) {
            int x = areas.listLeft() + 11;
            int y = listTop + index * (CARD_HEIGHT + CARD_GAP) - listScroll;
            if (y + CARD_HEIGHT <= listTop || y >= areas.listBottom()) continue;
            boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, CARD_HEIGHT);
            boolean selected = selectedIndex == index;
            int outline = selected ? accent() : hovered ? GOLD : BORDER;
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + CARD_HEIGHT + 1, outline);
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT, selected ? SELECTED : SURFACE);
            graphics.fill(x, y, x + 5, y + CARD_HEIGHT, selected ? accent() : BORDER);
            String[] parts = labelParts(labels[index]);
            graphics.text(font, compact(parts[0], Math.max(12, cardWidth / 7)), x + 14, y + 9, TEXT, false);
            List<FormattedCharSequence> lines = font.split(Component.literal(parts[1]), Math.max(72, cardWidth - 28));
            if (!lines.isEmpty()) graphics.text(font, lines.getFirst(), x + 14, y + 29, MUTED, false);
        }
        graphics.disableScissor();
        scrollbar(graphics, areas.listRight() - 7, listTop + 4, areas.listBottom() - 7,
                listScroll, maxScroll, visibleHeight, contentHeight);
    }

    private void renderDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        panel(graphics, areas.detailLeft(), areas.detailTop(), areas.detailRight(), areas.detailBottom(), SURFACE);
        int buttonHeight = selectedIndex >= 0 ? 34 : 0;
        int buttonTop = areas.detailBottom() - 14 - buttonHeight;
        int textLeft = areas.detailLeft() + 20;
        int textRight = areas.detailRight() - 20;
        int textTop = areas.detailTop() + 18;
        int textBottom = buttonTop - 12;
        List<DetailLine> lines = detailLines(Math.max(100, textRight - textLeft));
        int contentHeight = 5;
        for (DetailLine line : lines) contentHeight += line.height() + line.gap();
        int visibleHeight = Math.max(1, textBottom - textTop);
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        detailScroll = clamp(detailScroll, 0, maxScroll);

        graphics.enableScissor(areas.detailLeft() + 2, areas.detailTop() + 2,
                areas.detailRight() - 2, Math.max(areas.detailTop() + 3, textBottom));
        int y = textTop - detailScroll;
        for (DetailLine line : lines) {
            y += line.gap();
            if (y + line.height() >= textTop && y <= textBottom) {
                graphics.text(font, line.text(), textLeft, y, line.color(), false);
            }
            y += line.height();
        }
        graphics.disableScissor();
        scrollbar(graphics, areas.detailRight() - 8, textTop, textBottom,
                detailScroll, maxScroll, visibleHeight, contentHeight);

        if (selectedIndex < 0) return;
        int buttonLeft = areas.detailLeft() + 20;
        int buttonRight = areas.detailRight() - 20;
        boolean hovered = inside(mouseX, mouseY, buttonLeft, buttonTop,
                buttonRight - buttonLeft, buttonHeight);
        graphics.fill(buttonLeft - 1, buttonTop - 1, buttonRight + 1, buttonTop + buttonHeight + 1,
                hovered ? GOLD : accent());
        graphics.fill(buttonLeft, buttonTop, buttonRight, buttonTop + buttonHeight,
                hovered ? 0xFFFFE9B9 : SELECTED);
        graphics.centeredText(font,
                compact(VillageActionDescriptions.executeLabel(actions[selectedIndex]),
                        Math.max(14, (buttonRight - buttonLeft) / 7)),
                (buttonLeft + buttonRight) / 2, buttonTop + 12, TEXT);
    }

    private List<DetailLine> detailLines(int width) {
        List<DetailLine> result = new ArrayList<>();
        if (selectedIndex >= 0) {
            String[] parts = labelParts(labels[selectedIndex]);
            addWrapped(result, "§l" + parts[0], width, TEXT, 14, 0);
            addWrapped(result, parts[1], width, accent(), 13, 7);
            addWrapped(result, VillageActionDescriptions.describe(actions[selectedIndex], labels[selectedIndex]),
                    width, TEXT, 13, 10);
        }
        if (!payload.body().isBlank()) {
            addWrapped(result, "현재 정보", width, accent(), 14, 13);
            for (String paragraph : payload.body().split("\n", -1)) {
                if (paragraph.isBlank()) result.add(new DetailLine(FormattedCharSequence.EMPTY, TEXT, 8, 0));
                else addWrapped(result, paragraph, width, TEXT, 13, 0);
            }
        }
        if (result.isEmpty()) addWrapped(result, "표시할 정보가 없습니다.", width, MUTED, 13, 0);
        return result;
    }

    private void addWrapped(List<DetailLine> target, String text, int width, int color, int height, int gap) {
        List<FormattedCharSequence> wrapped = font.split(Component.literal(text), width);
        boolean first = true;
        for (FormattedCharSequence line : wrapped) {
            target.add(new DetailLine(line, color, height, first ? gap : 0));
            first = false;
        }
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
        int count = actionCount();
        int listTop = areas.listTop() + 31;
        int cardWidth = Math.max(90, areas.listWidth() - 24);
        for (int index = 0; index < count; index++) {
            int x = areas.listLeft() + 11;
            int y = listTop + index * (CARD_HEIGHT + CARD_GAP) - listScroll;
            if (inside(click.x(), click.y(), x, y, cardWidth, CARD_HEIGHT)) {
                selectedIndex = index;
                detailScroll = 0;
                return true;
            }
        }
        if (selectedIndex >= 0) {
            int buttonTop = areas.detailBottom() - 48;
            if (inside(click.x(), click.y(), areas.detailLeft() + 20, buttonTop,
                    areas.detailWidth() - 40, 34)) {
                executeSelected();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Areas areas = areas(layout());
        int amount = (int) Math.round(vertical * 40);
        if (inside(mouseX, mouseY, areas.listLeft(), areas.listTop(), areas.listWidth(), areas.listHeight())) {
            listScroll = Math.max(0, listScroll - amount);
            return true;
        }
        if (inside(mouseX, mouseY, areas.detailLeft(), areas.detailTop(), areas.detailWidth(), areas.detailHeight())) {
            detailScroll = Math.max(0, detailScroll - amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void executeSelected() {
        if (selectedIndex < 0 || selectedIndex >= actionCount()) return;
        String action = actions[selectedIndex];
        String[] parts = labelParts(labels[selectedIndex]);
        String description = VillageActionDescriptions.describe(action, labels[selectedIndex]);
        if (VillageActionDescriptions.requiresConfirmation(action) && minecraft != null) {
            minecraft.gui.setScreen(new VillageConfirmScreen(this, action, parts[0], description));
        } else {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        }
    }

    private int actionCount() { return Math.min(actions.length, labels.length); }

    private String[] labelParts(String label) {
        String[] raw = label.split("\\|", 2);
        return new String[]{raw.length > 0 ? raw[0] : label,
                raw.length > 1 ? raw[1] : "선택한 기능을 실행합니다."};
    }

    private int accent() {
        return switch (payload.screenId()) {
            case "funding", "management", "tower_control", "tower_detail", "equipment_shop" -> GOLD;
            case "game_over" -> RED;
            default -> TEAL;
        };
    }

    private Layout layout() {
        int margin = 8;
        int panelWidth = Math.min(960, Math.max(330, width - margin * 2));
        int panelHeight = Math.min(610, Math.max(230, height - margin * 2));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 2));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private Areas areas(Layout layout) {
        int left = layout.left() + 16;
        int right = layout.right() - 16;
        int top = layout.top() + 56;
        int bottom = layout.bottom() - 14;
        int contentWidth = right - left;
        if (contentWidth >= 500) {
            int listWidth = clamp(contentWidth * 34 / 100, 180, 300);
            return new Areas(left, top, left + listWidth, bottom,
                    left + listWidth + 10, top, right, bottom);
        }
        int listHeight = clamp((bottom - top) * 43 / 100, 100, Math.max(100, bottom - top - 110));
        return new Areas(left, top, right, top + listHeight,
                left, top + listHeight + 8, right, bottom);
    }

    private void panel(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, BORDER);
        graphics.fill(left, top, right, bottom, color);
    }

    private void scrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                           int value, int maximum, int visible, int content) {
        if (maximum <= 0 || content <= visible || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(18, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(value, 0, maximum) / maximum;
        graphics.fill(x, top, x + 4, bottom, 0xFFB9AA91);
        graphics.fill(x, y, x + 4, y + thumb, accent());
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

    private record Areas(int listLeft, int listTop, int listRight, int listBottom,
                         int detailLeft, int detailTop, int detailRight, int detailBottom) {
        int listWidth() { return listRight - listLeft; }
        int listHeight() { return listBottom - listTop; }
        int detailWidth() { return detailRight - detailLeft; }
        int detailHeight() { return detailBottom - detailTop; }
    }

    private record DetailLine(FormattedCharSequence text, int color, int height, int gap) {}
}
