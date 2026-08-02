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

/** Shop opens directly into weapon, armour and supply categories. */
public final class VillageShopScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x65000000;
    private static final int PANEL = 0xFFF1E6CF;
    private static final int SURFACE = 0xFFFFFAEE;
    private static final int SURFACE_ALT = 0xFFE9DCC1;
    private static final int SELECTED = 0xFFFFE1A2;
    private static final int BORDER = 0xFF6F5B43;
    private static final int TEXT = 0xFF211A14;
    private static final int MUTED = 0xFF62584D;
    private static final int GOLD = 0xFFB87B20;
    private static final int TEAL = 0xFF267E73;
    private static final int CARD_HEIGHT = 40;
    private static final int CARD_GAP = 4;
    private static final int ACTION_HEIGHT = 24;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final List<OfferCard> offers = new ArrayList<>();
    private Category category = Category.WEAPON;
    private int selectedIndex;
    private int listScroll;
    private int detailScroll;

    public VillageShopScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        parse();
        if (offers.stream().noneMatch(card -> card.category() == category)) {
            category = offers.isEmpty() ? Category.WEAPON : offers.getFirst().category();
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
        renderContent(graphics, mouseX, mouseY, layout);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int left = layout.left() + 20;
        int closeX = layout.right() - 39;
        graphics.text(font, "상점", left, layout.top() + 12, TEXT, false);
        graphics.text(font, "장비 구매 · 전투 물자 · 전리품 판매", left, layout.top() + 31, MUTED, false);
        boolean hovered = inside(mouseX, mouseY, closeX, layout.top() + 9, 29, 29);
        graphics.fill(closeX, layout.top() + 9, closeX + 29, layout.top() + 38,
                hovered ? 0xFFE6A6A6 : SURFACE_ALT);
        graphics.centeredText(font, "×", closeX + 14, layout.top() + 19, hovered ? 0xFFB95050 : TEXT);
    }

    private void renderTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int x = layout.left() + 18;
        int y = layout.top() + 51;
        int available = layout.width() - 36;
        int gap = 7;
        int tabWidth = (available - gap * 2) / 3;
        for (Category value : Category.values()) {
            boolean active = category == value;
            boolean hovered = inside(mouseX, mouseY, x, y, tabWidth, 27);
            graphics.fill(x - 1, y - 1, x + tabWidth + 1, y + 28, active ? GOLD : BORDER);
            graphics.fill(x, y, x + tabWidth, y + 27, active ? SELECTED : hovered ? SURFACE : SURFACE_ALT);
            graphics.centeredText(font, value.displayName(), x + tabWidth / 2, y + 9, active ? TEXT : MUTED);
            x += tabWidth + gap;
        }
    }

    private void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int left = layout.left() + 18;
        int right = layout.right() - 18;
        int top = layout.top() + 88;
        int bottom = layout.bottom() - 16;
        int listWidth = clamp((right - left) * 24 / 100, 118, 198);
        Pane list = new Pane(left, top, left + listWidth, bottom);
        Pane detail = new Pane(left + listWidth + 10, top, right, bottom);
        graphics.fill(list.left() - 1, list.top() - 1, list.right() + 1, list.bottom() + 1, BORDER);
        graphics.fill(list.left(), list.top(), list.right(), list.bottom(), SURFACE_ALT);
        graphics.fill(detail.left() - 1, detail.top() - 1, detail.right() + 1, detail.bottom() + 1, BORDER);
        graphics.fill(detail.left(), detail.top(), detail.right(), detail.bottom(), SURFACE);
        renderOfferList(graphics, mouseX, mouseY, list);
        renderOfferDetail(graphics, mouseX, mouseY, detail);
    }

    private void renderOfferList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        List<Integer> visible = visibleIndices();
        int contentHeight = visible.isEmpty() ? 0 : visible.size() * CARD_HEIGHT + (visible.size() - 1) * CARD_GAP;
        int viewport = Math.max(1, pane.height() - 18);
        int maxScroll = Math.max(0, contentHeight - viewport);
        listScroll = clamp(listScroll, 0, maxScroll);
        int x = pane.left() + 9;
        int cardWidth = pane.width() - 20;
        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2, pane.bottom() - 2);
        int y = pane.top() + 9 - listScroll;
        for (int actualIndex : visible) {
            OfferCard card = offers.get(actualIndex);
            boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, CARD_HEIGHT);
            boolean selected = selectedIndex == actualIndex;
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + CARD_HEIGHT + 1,
                    selected ? GOLD : hovered ? TEAL : BORDER);
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT, selected ? SELECTED : SURFACE);
            graphics.fill(x, y, x + 5, y + CARD_HEIGHT, selected ? GOLD : TEAL);
            graphics.text(font, compact(plain(card.name()), Math.max(11, cardWidth / 7)), x + 12, y + 6, TEXT, false);
            graphics.text(font, compact(plain(card.cost()), Math.max(11, cardWidth / 7)), x + 12, y + 23,
                    card.available() ? GOLD : MUTED, false);
            y += CARD_HEIGHT + CARD_GAP;
        }
        graphics.disableScissor();
        scrollbar(graphics, pane.right() - 7, pane.top() + 7, pane.bottom() - 7,
                listScroll, maxScroll, viewport, contentHeight);
    }

    private void renderOfferDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        if (selectedIndex < 0 || selectedIndex >= offers.size()) {
            graphics.text(font, "이 카테고리에는 상품이 없습니다.", pane.left() + 20, pane.top() + 20, MUTED, false);
            return;
        }
        OfferCard card = offers.get(selectedIndex);
        int buttonWidth = Math.min(142, Math.max(86, pane.width() / 3));
        int buttonRight = pane.right() - 15;
        int buttonLeft = buttonRight - buttonWidth;
        int buttonTop = pane.bottom() - ACTION_HEIGHT - 11;
        int textLeft = pane.left() + 15;
        int textRight = pane.right() - 15;
        int textTop = pane.top() + 14;
        int textBottom = buttonTop - 8;
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(font.split(Component.literal(plain(card.name())), Math.max(100, textRight - textLeft)));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.literal(plain(card.cost())), Math.max(100, textRight - textLeft)));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.literal(plain(card.effect())), Math.max(100, textRight - textLeft)));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.literal(plain(card.status())), Math.max(100, textRight - textLeft)));
        int contentHeight = Math.max(1, lines.size() * 16);
        int viewport = Math.max(1, textBottom - textTop);
        int maxScroll = Math.max(0, contentHeight - viewport);
        detailScroll = clamp(detailScroll, 0, maxScroll);
        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2, textBottom);
        int y = textTop - detailScroll;
        for (FormattedCharSequence line : lines) {
            if (y >= textTop - 12 && y <= textBottom) graphics.text(font, line, textLeft, y, TEXT, false);
            y += 16;
        }
        graphics.disableScissor();
        scrollbar(graphics, pane.right() - 8, textTop, textBottom,
                detailScroll, maxScroll, viewport, contentHeight);

        boolean active = card.available();
        boolean hovered = active && inside(mouseX, mouseY, buttonLeft, buttonTop, buttonWidth, ACTION_HEIGHT);
        graphics.fill(buttonLeft - 1, buttonTop - 1, buttonRight + 1, buttonTop + ACTION_HEIGHT + 1,
                active ? (hovered ? TEAL : GOLD) : BORDER);
        graphics.fill(buttonLeft, buttonTop, buttonRight, buttonTop + ACTION_HEIGHT,
                active ? (hovered ? 0xFFD7F1E9 : SELECTED) : SURFACE_ALT);
        graphics.centeredText(font, active ? actionLabel(card.action()) : "잠김", (buttonLeft + buttonRight) / 2,
                buttonTop + 7, active ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 39, layout.top() + 9, 29, 29)) {
            onClose();
            return true;
        }
        int x = layout.left() + 18;
        int y = layout.top() + 51;
        int gap = 7;
        int tabWidth = (layout.width() - 36 - gap * 2) / 3;
        for (Category value : Category.values()) {
            if (inside(click.x(), click.y(), x, y, tabWidth, 27)) {
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
        int cardX = panes.list().left() + 9;
        int cardY = panes.list().top() + 9 - listScroll;
        int cardWidth = panes.list().width() - 20;
        for (int actualIndex : visible) {
            if (inside(click.x(), click.y(), cardX, cardY, cardWidth, CARD_HEIGHT)) {
                selectedIndex = actualIndex;
                detailScroll = 0;
                return true;
            }
            cardY += CARD_HEIGHT + CARD_GAP;
        }
        if (selectedIndex >= 0 && selectedIndex < offers.size()) {
            Pane detail = panes.detail();
            OfferCard card = offers.get(selectedIndex);
            int buttonWidth = Math.min(142, Math.max(86, detail.width() / 3));
            int buttonLeft = detail.right() - 15 - buttonWidth;
            int buttonTop = detail.bottom() - ACTION_HEIGHT - 11;
            if (card.available() && inside(click.x(), click.y(), buttonLeft, buttonTop,
                    buttonWidth, ACTION_HEIGHT)) {
                execute(card);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        ContentPanes panes = contentPanes(layout());
        int amount = (int) Math.round(vertical * 40);
        if (inside(mouseX, mouseY, panes.list().left(), panes.list().top(), panes.list().width(), panes.list().height())) {
            listScroll = Math.max(0, listScroll - amount);
            return true;
        }
        if (inside(mouseX, mouseY, panes.detail().left(), panes.detail().top(), panes.detail().width(), panes.detail().height())) {
            detailScroll = Math.max(0, detailScroll - amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void execute(OfferCard card) {
        String detail = plain(card.effect()) + "\n" + plain(card.cost());
        if (VillageActionDescriptions.requiresConfirmation(card.action()) && minecraft != null) {
            minecraft.gui.setScreen(new VillageConfirmScreen(this, card.action(), card.name(), detail));
        } else {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(card.action()));
        }
    }

    private String actionLabel(String action) {
        if (action.equals("sell_loot")) return "전리품 모두 판매";
        if (action.startsWith("buy_")) return "구매";
        return "장비 구매";
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
            } else {
                String[] basic = labels[i].split("\\|", 2);
                offers.add(new OfferCard(actions[i], Category.OTHER, basic[0], "", basic.length > 1 ? basic[1] : "", "", true));
            }
        }
    }

    private List<Integer> visibleIndices() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) if (offers.get(i).category() == category) result.add(i);
        return result;
    }

    private int firstVisibleIndex() {
        for (int i = 0; i < offers.size(); i++) if (offers.get(i).category() == category) return i;
        return -1;
    }

    private Layout layout() {
        int margin = 8;
        int panelWidth = Math.min(980, Math.max(390, width - margin * 2));
        int panelHeight = Math.min(640, Math.max(280, height - margin * 2));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 2));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private ContentPanes contentPanes(Layout layout) {
        int left = layout.left() + 18;
        int right = layout.right() - 18;
        int top = layout.top() + 88;
        int bottom = layout.bottom() - 16;
        int listWidth = clamp((right - left) * 24 / 100, 118, 198);
        return new ContentPanes(new Pane(left, top, left + listWidth, bottom),
                new Pane(left + listWidth + 10, top, right, bottom));
    }

    private void scrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                           int value, int maximum, int visible, int content) {
        if (maximum <= 0 || content <= visible || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(18, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(value, 0, maximum) / maximum;
        graphics.fill(x, top, x + 4, bottom, 0xFFB9AA91);
        graphics.fill(x, y, x + 4, y + thumb, GOLD);
    }

    private String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
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

    private enum Category {
        WEAPON("무기"), ARMOR("방어구"), OTHER("기타");
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
