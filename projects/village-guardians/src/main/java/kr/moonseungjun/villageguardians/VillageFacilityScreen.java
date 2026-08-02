package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Compact facility menu with a narrow selector, wide description and small action button. */
public final class VillageFacilityScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x65000000;
    private static final int PANEL = 0xFFE4D8BF;
    private static final int SURFACE = 0xFFF1E9D7;
    private static final int SURFACE_ALT = 0xFFD8CBB1;
    private static final int SELECTED = 0xFFE1C98F;
    private static final int BORDER = 0xFF6F5B43;
    private static final int TEXT = 0xFF211A14;
    private static final int MUTED = 0xFF62584D;
    private static final int TEAL = 0xFF267E73;
    private static final int GOLD = 0xFFB87B20;
    private static final int RED = 0xFFAA4545;
    private static final int CARD_HEIGHT = 30;
    private static final int CARD_GAP = 3;
    private static final int ACTION_HEIGHT = 20;

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
        int left = layout.left() + 18;
        int closeX = layout.right() - 36;
        int textWidth = Math.max(32, closeX - left - 8);
        graphics.text(font, compact(plain(payload.title()), textWidth), left, layout.top() + 10, TEXT, false);
        graphics.text(font, compact(subtitle(), textWidth), left, layout.top() + 27, MUTED, false);
        boolean hovered = inside(mouseX, mouseY, closeX, layout.top() + 7, 27, 27);
        graphics.fill(closeX, layout.top() + 7, closeX + 27, layout.top() + 34,
                hovered ? 0xFFE2AAAA : SURFACE_ALT);
        graphics.centeredText(font, "×", closeX + 13, layout.top() + 16, hovered ? RED : TEXT);
    }

    private String subtitle() {
        return switch (payload.screenId()) {
            case "funding" -> "개인 주화를 공동 보급품으로 전환";
            case "tower_control", "tower_detail" -> "성벽과 방어탑 지휘";
            case "caller" -> "상태·성장·통신·귀환";
            case "management" -> "회관 전용 시설 관리";
            default -> "시설 고유 기능";
        };
    }

    private void renderList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        panel(graphics, areas.listLeft(), areas.listTop(), areas.listRight(), areas.listBottom(), SURFACE_ALT);
        int count = actionCount();
        int listTop = areas.listTop() + 7;
        int visibleHeight = Math.max(1, areas.listBottom() - listTop - 7);
        int contentHeight = count == 0 ? 0 : count * CARD_HEIGHT + Math.max(0, count - 1) * CARD_GAP;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        listScroll = clamp(listScroll, 0, maxScroll);
        int cardWidth = Math.max(76, areas.listWidth() - 15);

        graphics.enableScissor(areas.listLeft() + 2, listTop, areas.listRight() - 2, areas.listBottom() - 2);
        for (int index = 0; index < count; index++) {
            int x = areas.listLeft() + 6;
            int y = listTop + index * (CARD_HEIGHT + CARD_GAP) - listScroll;
            if (y + CARD_HEIGHT <= listTop || y >= areas.listBottom()) continue;
            boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, CARD_HEIGHT);
            boolean selected = selectedIndex == index;
            int outline = selected ? accent() : hovered ? GOLD : BORDER;
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + CARD_HEIGHT + 1, outline);
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT, selected ? SELECTED : SURFACE);
            graphics.fill(x, y, x + 4, y + CARD_HEIGHT, selected ? accent() : BORDER);
            String[] parts = labelParts(labels[index]);
            graphics.text(font, compact(parts[0], cardWidth - 19), x + 10, y + 6, TEXT, false);
            List<FormattedCharSequence> lines = font.split(Component.literal(plain(parts[1])), Math.max(60, cardWidth - 19));
            if (!lines.isEmpty()) graphics.text(font, lines.getFirst(), x + 10, y + 18, MUTED, false);
        }
        graphics.disableScissor();
        scrollbar(graphics, areas.listRight() - 5, listTop + 3, areas.listBottom() - 5,
                listScroll, maxScroll, visibleHeight, contentHeight);
    }

    private void renderDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Areas areas) {
        panel(graphics, areas.detailLeft(), areas.detailTop(), areas.detailRight(), areas.detailBottom(), SURFACE);
        int textLeft = areas.detailLeft() + 15;
        int textRight = areas.detailRight() - 15;
        boolean executable = selectedIndex >= 0 && !informationSelected();
        int buttonWidth = executable ? Math.min(108, Math.max(70, areas.detailWidth() / 4)) : 0;
        int buttonLeft = textRight - buttonWidth;
        int buttonTop = areas.detailBottom() - ACTION_HEIGHT - 11;
        int textTop = areas.detailTop() + 13;
        int textBottom = executable ? buttonTop - 8 : areas.detailBottom() - 11;

        List<DetailLine> lines = detailLines(Math.max(100, textRight - textLeft));
        int contentHeight = 2;
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
        scrollbar(graphics, areas.detailRight() - 6, textTop, textBottom,
                detailScroll, maxScroll, visibleHeight, contentHeight);

        if (!executable) return;
        boolean hovered = inside(mouseX, mouseY, buttonLeft, buttonTop, buttonWidth, ACTION_HEIGHT);
        graphics.fill(buttonLeft - 1, buttonTop - 1, buttonLeft + buttonWidth + 1,
                buttonTop + ACTION_HEIGHT + 1, hovered ? GOLD : accent());
        graphics.fill(buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + ACTION_HEIGHT,
                hovered ? 0xFFFFE8B5 : SELECTED);
        graphics.centeredText(font,
                compact(VillageActionDescriptions.executeLabel(actions[selectedIndex]), buttonWidth - 10),
                buttonLeft + buttonWidth / 2, buttonTop + 5, TEXT);
    }

    private List<DetailLine> detailLines(int width) {
        List<DetailLine> result = new ArrayList<>();
        if (selectedIndex >= 0) {
            String[] parts = labelParts(labels[selectedIndex]);
            addWrapped(result, parts[0], width, accent(), 13, 0);
            if (!parts[1].isBlank()) addWrapped(result, parts[1], width, TEXT, 11, 2);
            if (!informationSelected()) addWrapped(result,
                    VillageActionDescriptions.describe(actions[selectedIndex], parts[0]), width, MUTED, 11, 5);
        }
        if (shouldShowBody()) {
            for (String paragraph : plain(payload.body()).split("\n", -1)) {
                if (paragraph.isBlank()) result.add(new DetailLine(FormattedCharSequence.EMPTY, TEXT, 7, 0));
                else addWrapped(result, paragraph, width, TEXT, 11, 2);
            }
        }
        if (result.isEmpty()) addWrapped(result, "표시할 정보가 없습니다.", width, MUTED, 11, 0);
        return result;
    }

    private void addWrapped(List<DetailLine> target, String text, int width, int color, int height, int gap) {
        List<FormattedCharSequence> wrapped = font.split(Component.literal(plain(text)), width);
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
        if (inside(click.x(), click.y(), layout.right() - 36, layout.top() + 7, 27, 27)) {
            onClose();
            return true;
        }
        int count = actionCount();
        int listTop = areas.listTop() + 7;
        int cardWidth = Math.max(76, areas.listWidth() - 15);
        for (int index = 0; index < count; index++) {
            int x = areas.listLeft() + 6;
            int y = listTop + index * (CARD_HEIGHT + CARD_GAP) - listScroll;
            if (inside(click.x(), click.y(), x, y, cardWidth, CARD_HEIGHT)) {
                selectedIndex = index;
                detailScroll = 0;
                return true;
            }
        }
        if (selectedIndex >= 0 && !informationSelected()) {
            int buttonWidth = Math.min(108, Math.max(70, areas.detailWidth() / 4));
            int buttonLeft = areas.detailRight() - 15 - buttonWidth;
            int buttonTop = areas.detailBottom() - ACTION_HEIGHT - 11;
            if (inside(click.x(), click.y(), buttonLeft, buttonTop, buttonWidth, ACTION_HEIGHT)) {
                executeSelected();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Areas areas = areas(layout());
        int amount = (int) Math.round(vertical * 32);
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
        if (selectedIndex < 0 || selectedIndex >= actionCount() || informationSelected()) return;
        String action = actions[selectedIndex];
        String[] parts = labelParts(labels[selectedIndex]);
        String description = VillageActionDescriptions.describe(action, parts[0]);
        if (VillageActionDescriptions.requiresConfirmation(action) && minecraft != null) {
            minecraft.gui.setScreen(new VillageConfirmScreen(this, action, parts[0], description));
        } else {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        }
    }

    private int actionCount() { return Math.min(actions.length, labels.length); }

    private boolean informationSelected() {
        return selectedIndex >= 0 && selectedIndex < actionCount()
                && ("facility_info".equals(actions[selectedIndex])
                || actions[selectedIndex].startsWith("wave_info:"));
    }

    private boolean shouldShowBody() {
        return !payload.body().isBlank() && (actionCount() == 0 || informationSelected()
                || "game_over".equals(payload.screenId()));
    }

    private String[] labelParts(String label) {
        String[] raw = label.split("\\|", 2);
        return new String[]{plain(raw.length > 0 ? raw[0] : label),
                plain(raw.length > 1 ? raw[1] : "")};
    }

    private int accent() {
        return switch (payload.screenId()) {
            case "funding", "management", "tower_control", "tower_detail" -> GOLD;
            case "game_over" -> RED;
            default -> TEAL;
        };
    }

    private Layout layout() {
        int margin = 8;
        int panelWidth = Math.min(880, Math.max(330, width - margin * 2));
        int panelHeight = Math.min(540, Math.max(230, height - margin * 2));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 2));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private Areas areas(Layout layout) {
        int left = layout.left() + 14;
        int right = layout.right() - 14;
        int top = layout.top() + 43;
        int bottom = layout.bottom() - 12;
        int contentWidth = right - left;
        if (contentWidth >= 340) {
            int listWidth = clamp(contentWidth * 24 / 100, 118, 198);
            return new Areas(left, top, left + listWidth, bottom,
                    left + listWidth + 8, top, right, bottom);
        }
        int listHeight = clamp((bottom - top) * 38 / 100, 82, Math.max(82, bottom - top - 100));
        return new Areas(left, top, right, top + listHeight,
                left, top + listHeight + 7, right, bottom);
    }

    private void panel(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, BORDER);
        graphics.fill(left, top, right, bottom, color);
    }

    private void scrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                           int value, int maximum, int visible, int content) {
        if (maximum <= 0 || content <= visible || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(14, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(value, 0, maximum) / maximum;
        graphics.fill(x, top, x + 3, bottom, 0xFFB9AA91);
        graphics.fill(x, y, x + 3, y + thumb, accent());
    }

    private String compact(String value, int maxWidth) {
        String normalized = plain(value).replace('\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        String suffix = "…";
        if (font.width(suffix) > maxWidth) return "";
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + suffix) > maxWidth) end--;
        return normalized.substring(0, end) + suffix;
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record DetailLine(FormattedCharSequence text, int color, int height, int gap) {}
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
}
