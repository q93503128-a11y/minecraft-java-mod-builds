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
import java.util.Locale;

/** Daily rotating store with equipment, armour and supply categories. */
public final class VillageShopScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x65000000;
    private static final int PANEL = 0xFFE4D8BF;
    private static final int SURFACE = 0xFFF1E9D7;
    private static final int SURFACE_ALT = 0xFFD8CBB1;
    private static final int SELECTED = 0xFFE1C98F;
    private static final int BORDER = 0xFF6F5B43;
    private static final int TEXT = 0xFF211A14;
    private static final int MUTED = 0xFF62584D;
    private static final int GOLD = 0xFFB87B20;
    private static final int TEAL = 0xFF267E73;
    private static final int RED = 0xFFAA4545;
    private static final int CARD_HEIGHT = 34;
    private static final int CARD_GAP = 3;
    private static final int ACTION_HEIGHT = 20;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final List<OfferCard> offers = new ArrayList<>();
    private Category category = Category.EQUIPMENT;
    private String utilityAction = "";
    private String utilityLabel = "";
    private int selectedIndex;
    private int listScroll;
    private int detailScroll;

    public VillageShopScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        parse();
        if (offers.stream().noneMatch(card -> card.category() == category)) {
            category = offers.isEmpty() ? Category.EQUIPMENT : offers.getFirst().category();
        }
        selectedIndex = firstVisibleIndex();
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
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, layout.bottom(), GOLD);
        renderHeader(graphics, mouseX, mouseY, layout);
        renderTabs(graphics, mouseX, mouseY, layout);
        ContentPanes panes = contentPanes(layout);
        panel(graphics, panes.list(), SURFACE_ALT);
        panel(graphics, panes.detail(), SURFACE);
        renderOfferList(graphics, mouseX, mouseY, panes.list());
        renderOfferDetail(graphics, mouseX, mouseY, panes.detail());
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int left = layout.left() + 18;
        int closeX = layout.right() - 36;
        int utilityWidth = utilityAction.isBlank() ? 0 : Math.min(112, Math.max(76, layout.width() / 7));
        int utilityX = closeX - utilityWidth - 7;
        int textRight = utilityAction.isBlank() ? closeX - 8 : utilityX - 8;
        graphics.text(font, fit("상점", Math.max(20, textRight - left)), left, layout.top() + 8, TEXT, false);
        graphics.text(font, fit(plain(payload.body()), Math.max(20, textRight - left)),
                left, layout.top() + 24, MUTED, false);

        if (!utilityAction.isBlank()) {
            boolean hovered = inside(mouseX, mouseY, utilityX, layout.top() + 7, utilityWidth, 24);
            graphics.fill(utilityX - 1, layout.top() + 6, utilityX + utilityWidth + 1, layout.top() + 32,
                    hovered ? GOLD : BORDER);
            graphics.fill(utilityX, layout.top() + 7, utilityX + utilityWidth, layout.top() + 31,
                    hovered ? SELECTED : SURFACE_ALT);
            graphics.centeredText(font, fit(utilityLabel, utilityWidth - 8), utilityX + utilityWidth / 2,
                    layout.top() + 14, hovered ? TEXT : MUTED);
        }

        boolean close = inside(mouseX, mouseY, closeX, layout.top() + 7, 27, 27);
        graphics.fill(closeX, layout.top() + 7, closeX + 27, layout.top() + 34,
                close ? 0xFFE2AAAA : SURFACE_ALT);
        graphics.centeredText(font, "×", closeX + 13, layout.top() + 16, close ? RED : TEXT);
    }

    private void renderTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int x = layout.left() + 16;
        int y = layout.top() + 39;
        int gap = 5;
        int available = layout.width() - 32;
        int tabWidth = Math.max(1, (available - gap * 2) / 3);
        for (Category value : Category.values()) {
            boolean active = category == value;
            boolean hovered = inside(mouseX, mouseY, x, y, tabWidth, 19);
            graphics.fill(x - 1, y - 1, x + tabWidth + 1, y + 20, active ? GOLD : BORDER);
            graphics.fill(x, y, x + tabWidth, y + 19, active ? SELECTED : hovered ? SURFACE : SURFACE_ALT);
            graphics.centeredText(font, fit(value.displayName(), tabWidth - 8), x + tabWidth / 2, y + 5,
                    active ? TEXT : MUTED);
            x += tabWidth + gap;
        }
    }

    private void renderOfferList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        List<Integer> visible = visibleIndices();
        int content = visible.isEmpty() ? 0 : visible.size() * CARD_HEIGHT + (visible.size() - 1) * CARD_GAP;
        int viewport = Math.max(1, pane.height() - 14);
        int maximum = Math.max(0, content - viewport);
        listScroll = clamp(listScroll, 0, maximum);
        int x = pane.left() + 7;
        int cardWidth = Math.max(30, pane.width() - 16);
        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2, pane.bottom() - 2);
        int y = pane.top() + 7 - listScroll;
        for (int actualIndex : visible) {
            OfferCard card = offers.get(actualIndex);
            boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, CARD_HEIGHT);
            boolean selected = selectedIndex == actualIndex;
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + CARD_HEIGHT + 1,
                    selected ? GOLD : hovered ? TEAL : BORDER);
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT, selected ? SELECTED : SURFACE);
            graphics.fill(x, y, x + 4, y + CARD_HEIGHT, selected ? GOLD : TEAL);
            graphics.text(font, fit(card.name(), cardWidth - 18), x + 10, y + 5, TEXT, false);
            graphics.text(font, fit(card.cost(), cardWidth - 18), x + 10, y + 19,
                    card.available() ? GOLD : MUTED, false);
            y += CARD_HEIGHT + CARD_GAP;
        }
        graphics.disableScissor();
        scrollbar(graphics, pane.right() - 5, pane.top() + 5, pane.bottom() - 5,
                listScroll, maximum, viewport, content);
    }

    private void renderOfferDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        if (selectedIndex < 0 || selectedIndex >= offers.size()
                || offers.get(selectedIndex).category() != category) {
            List<FormattedCharSequence> empty = font.split(Component.literal("오늘 이 분류에 입고된 상품이 없습니다."),
                    Math.max(40, pane.width() - 24));
            int y = pane.top() + 12;
            for (FormattedCharSequence line : empty) {
                graphics.text(font, line, pane.left() + 12, y, MUTED, false);
                y += 11;
            }
            return;
        }
        OfferCard card = offers.get(selectedIndex);
        int buttonWidth = Math.min(108, Math.max(70, pane.width() / 4));
        int buttonLeft = pane.right() - 13 - buttonWidth;
        int buttonTop = pane.bottom() - ACTION_HEIGHT - 10;
        int textLeft = pane.left() + 13;
        int textRight = pane.right() - 13;
        int textTop = pane.top() + 12;
        int textBottom = buttonTop - 7;
        List<DetailLine> lines = detailLines(card, Math.max(40, textRight - textLeft));
        int content = 2;
        for (DetailLine line : lines) content += line.height() + line.gap();
        int viewport = Math.max(1, textBottom - textTop);
        int maximum = Math.max(0, content - viewport);
        detailScroll = clamp(detailScroll, 0, maximum);
        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2,
                Math.max(pane.top() + 3, textBottom));
        int y = textTop - detailScroll;
        for (DetailLine line : lines) {
            y += line.gap();
            if (y + line.height() >= textTop && y <= textBottom) {
                graphics.text(font, line.text(), textLeft, y, line.color(), false);
            }
            y += line.height();
        }
        graphics.disableScissor();
        scrollbar(graphics, pane.right() - 5, textTop, textBottom,
                detailScroll, maximum, viewport, content);

        boolean active = card.available();
        boolean hovered = active && inside(mouseX, mouseY, buttonLeft, buttonTop, buttonWidth, ACTION_HEIGHT);
        graphics.fill(buttonLeft - 1, buttonTop - 1, buttonLeft + buttonWidth + 1,
                buttonTop + ACTION_HEIGHT + 1, active ? (hovered ? TEAL : GOLD) : BORDER);
        graphics.fill(buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + ACTION_HEIGHT,
                active ? (hovered ? 0xFFD7F1E9 : SELECTED) : SURFACE_ALT);
        graphics.centeredText(font, active ? "구매" : "주화 부족", buttonLeft + buttonWidth / 2,
                buttonTop + 5, active ? TEXT : MUTED);
    }

    private List<DetailLine> detailLines(OfferCard card, int width) {
        List<DetailLine> result = new ArrayList<>();
        addWrapped(result, card.name(), width, TEAL, 13, 0);
        addWrapped(result, card.effect(), width, TEXT, 11, 4);
        addWrapped(result, card.cost(), width, GOLD, 11, 6);
        addWrapped(result, card.status(), width, card.available() ? TEAL : MUTED, 11, 3);
        return result;
    }

    private void addWrapped(List<DetailLine> target, String text, int width, int color, int height, int gap) {
        boolean first = true;
        for (FormattedCharSequence line : font.split(Component.literal(plain(text)), width)) {
            target.add(new DetailLine(line, color, height, first ? gap : 0));
            first = false;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 36, layout.top() + 7, 27, 27)) {
            onClose();
            return true;
        }
        if (!utilityAction.isBlank()) {
            int closeX = layout.right() - 36;
            int utilityWidth = Math.min(112, Math.max(76, layout.width() / 7));
            int utilityX = closeX - utilityWidth - 7;
            if (inside(click.x(), click.y(), utilityX, layout.top() + 7, utilityWidth, 24)) {
                confirmOrSend(utilityAction, utilityLabel, "판매 가능한 전리품을 한 번에 정산합니다.");
                return true;
            }
        }
        int x = layout.left() + 16;
        int y = layout.top() + 39;
        int gap = 5;
        int tabWidth = Math.max(1, (layout.width() - 32 - gap * 2) / 3);
        for (Category value : Category.values()) {
            if (inside(click.x(), click.y(), x, y, tabWidth, 19)) {
                category = value;
                selectedIndex = firstVisibleIndex();
                listScroll = 0;
                detailScroll = 0;
                return true;
            }
            x += tabWidth + gap;
        }
        ContentPanes panes = contentPanes(layout);
        List<Integer> visible = visibleIndices();
        int cardX = panes.list().left() + 7;
        int cardY = panes.list().top() + 7 - listScroll;
        int cardWidth = Math.max(30, panes.list().width() - 16);
        for (int actualIndex : visible) {
            if (inside(click.x(), click.y(), cardX, cardY, cardWidth, CARD_HEIGHT)) {
                selectedIndex = actualIndex;
                detailScroll = 0;
                return true;
            }
            cardY += CARD_HEIGHT + CARD_GAP;
        }
        if (selectedIndex >= 0 && selectedIndex < offers.size()
                && offers.get(selectedIndex).category() == category) {
            Pane detail = panes.detail();
            OfferCard card = offers.get(selectedIndex);
            int buttonWidth = Math.min(108, Math.max(70, detail.width() / 4));
            int buttonLeft = detail.right() - 13 - buttonWidth;
            int buttonTop = detail.bottom() - ACTION_HEIGHT - 10;
            if (card.available() && inside(click.x(), click.y(), buttonLeft, buttonTop,
                    buttonWidth, ACTION_HEIGHT)) {
                confirmOrSend(card.action(), card.name(), card.effect() + "\n" + card.cost());
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        ContentPanes panes = contentPanes(layout());
        int amount = (int) Math.round(vertical * 32);
        if (inside(mouseX, mouseY, panes.list().left(), panes.list().top(),
                panes.list().width(), panes.list().height())) {
            listScroll = Math.max(0, listScroll - amount);
            return true;
        }
        if (inside(mouseX, mouseY, panes.detail().left(), panes.detail().top(),
                panes.detail().width(), panes.detail().height())) {
            detailScroll = Math.max(0, detailScroll - amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void confirmOrSend(String action, String title, String detail) {
        if (VillageActionDescriptions.requiresConfirmation(action) && minecraft != null) {
            minecraft.gui.setScreen(new VillageConfirmScreen(this, action, title, detail));
        } else {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        }
    }

    private void parse() {
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) {
            String[] p = labels[i].split("\\|", -1);
            if (p.length >= 7 && "shop".equals(p[0])) {
                offers.add(new OfferCard(actions[i], Category.parse(p[1]), plain(p[2]), plain(p[3]),
                        plain(p[4]), plain(p[5]), "available".equals(p[6])));
            } else if (p.length >= 2 && "shop_utility".equals(p[0])) {
                utilityAction = actions[i];
                utilityLabel = plain(p[1]);
            }
        }
    }

    private List<Integer> visibleIndices() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).category() == category) result.add(i);
        }
        return result;
    }

    private int firstVisibleIndex() {
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).category() == category) return i;
        }
        return -1;
    }

    private Layout layout() {
        int margin = 7;
        int panelWidth = Math.min(900, Math.max(300, width - margin * 2));
        int panelHeight = Math.min(560, Math.max(230, height - margin * 2));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 2));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private ContentPanes contentPanes(Layout layout) {
        int left = layout.left() + 14;
        int right = layout.right() - 14;
        int top = layout.top() + 66;
        int bottom = layout.bottom() - 12;
        int contentWidth = right - left;
        if (contentWidth < 330) {
            int availableHeight = Math.max(1, bottom - top);
            int listHeight = clamp(availableHeight * 36 / 100, 76,
                    Math.max(76, availableHeight - 96));
            return new ContentPanes(new Pane(left, top, right, top + listHeight),
                    new Pane(left, top + listHeight + 7, right, bottom));
        }
        int listWidth = clamp(contentWidth * 25 / 100, 112, 190);
        return new ContentPanes(new Pane(left, top, left + listWidth, bottom),
                new Pane(left + listWidth + 8, top, right, bottom));
    }

    private void panel(GuiGraphicsExtractor graphics, Pane pane, int color) {
        graphics.fill(pane.left() - 1, pane.top() - 1, pane.right() + 1, pane.bottom() + 1, BORDER);
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), color);
    }

    private void scrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                           int value, int maximum, int visible, int content) {
        if (maximum <= 0 || content <= visible || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(14, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(value, 0, maximum) / maximum;
        graphics.fill(x, top, x + 3, bottom, 0xFFB9AA91);
        graphics.fill(x, y, x + 3, y + thumb, GOLD);
    }

    private String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private String fit(String value, int maxWidth) {
        String normalized = plain(value).replace('\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        String suffix = "…";
        if (font.width(suffix) > maxWidth) return "";
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + suffix) > maxWidth) end--;
        return normalized.substring(0, end) + suffix;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private enum Category {
        EQUIPMENT("장비"), ARMOR("방어구"), OTHER("기타");
        private final String displayName;
        Category(String displayName) { this.displayName = displayName; }
        String displayName() { return displayName; }
        static Category parse(String value) {
            if (value == null) return OTHER;
            try { return valueOf(value.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { return OTHER; }
        }
    }

    private record OfferCard(String action, Category category, String name, String cost,
                             String effect, String status, boolean available) {}
    private record DetailLine(FormattedCharSequence text, int color, int height, int gap) {}
    private record Layout(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }
    }
    private record Pane(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
    private record ContentPanes(Pane list, Pane detail) {}
}
