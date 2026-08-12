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
import java.util.Locale;

/** Categorized daily shop. Selecting a card only shows detail; the explicit action button performs it. */
public final class VillageShopCatalogScreen extends Screen {
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

    private final String body;
    private final List<OfferCard> offers = new ArrayList<>();
    private Category category = Category.ALL;
    private int selected = -1;
    private int scroll;

    public VillageShopCatalogScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        body = plain(payload.body());
        parse(payload);
        selected = firstVisible();
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
        graphics.text(font, "상점  //  오늘의 진열대", safe.left() + 7, safe.top() + 4, GOLD, false);
        graphics.text(font, fit(font, body, safe.width() - 18), safe.left() + 7, safe.top() + 20, MUTED, false);
        graphics.fill(safe.left() + 7, safe.top() + 35, safe.right() - 7, safe.top() + 36, LINE);
        drawTabs(graphics, layout, mouseX, mouseY);
        drawList(graphics, layout.list(), mouseX, mouseY);
        drawDetail(graphics, layout.detail(), mouseX, mouseY);
        graphics.text(font, "카드 선택 → 상세 확인 → 실행  ·  ESC 닫기", safe.left() + 4,
                safe.bottom() - 11, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTabs(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        VillageUiSafeArea.Rect safe = layout.safe();
        int count = Category.values().length;
        int gap = 4;
        int left = safe.left() + 7;
        int width = safe.width() - 14;
        int tabWidth = Math.max(36, (width - gap * (count - 1)) / count);
        int y = safe.top() + 43;
        for (int i = 0; i < count; i++) {
            Category value = Category.values()[i];
            int x = left + i * (tabWidth + gap);
            boolean active = category == value;
            boolean hover = inside(mouseX, mouseY, x, y, tabWidth, 22);
            graphics.fill(x, y, x + tabWidth, y + 22, active || hover ? SURFACE_2 : SURFACE);
            graphics.fill(x, y + 20, x + tabWidth, y + 22, active ? GOLD : hover ? CYAN : LINE);
            graphics.centeredText(font, fit(font, value.displayName(), tabWidth - 8), x + tabWidth / 2, y + 7,
                    active ? GOLD : TEXT);
        }
    }

    private void drawList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), 0xB90D1519);
        List<Integer> visible = visibleIndices();
        int rowHeight = 45;
        int content = visible.size() * rowHeight;
        int maximum = Math.max(0, content - pane.height() + 10);
        scroll = VillageUiSafeArea.clamp(scroll, 0, maximum);
        graphics.enableScissor(pane.left(), pane.top(), pane.right(), pane.bottom());
        int y = pane.top() + 5 - scroll;
        for (int actual : visible) {
            OfferCard card = offers.get(actual);
            int x = pane.left() + 5;
            int w = pane.width() - 10;
            int h = 40;
            boolean hover = inside(mouseX, mouseY, x, y, w, h);
            boolean active = selected == actual;
            graphics.fill(x, y, x + w, y + h, active || hover ? SURFACE_2 : SURFACE);
            graphics.fill(x, y, x + 3, y + h, active ? GOLD : card.category().accent());
            graphics.text(font, fit(font, card.name(), w - 16), x + 10, y + 6,
                    active ? GOLD : TEXT, false);
            String second = card.cost().isBlank() ? card.status() : card.cost() + " · " + card.status();
            graphics.text(font, fit(font, second, w - 16), x + 10, y + 22,
                    card.available() ? MUTED : RED, false);
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
        if (visible.isEmpty()) {
            graphics.centeredText(font, "이 분류에 표시할 항목이 없습니다.", pane.left() + pane.width() / 2,
                    pane.top() + Math.max(12, pane.height() / 2), MUTED);
        }
    }

    private void drawDetail(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), 0xC5121B20);
        if (selected < 0 || selected >= offers.size() || !category.accepts(offers.get(selected).category())) {
            graphics.text(font, "항목을 선택하세요.", pane.left() + 13, pane.top() + 13, MUTED, false);
            return;
        }
        OfferCard card = offers.get(selected);
        int left = pane.left() + 15;
        int right = pane.right() - 15;
        int y = pane.top() + 13;
        graphics.text(font, fit(font, card.name(), pane.width() - 30), left, y, card.category().accent(), false);
        y += 20;
        if (!card.cost().isBlank()) {
            graphics.text(font, fit(font, card.cost(), Math.max(40, right - left)), left, y, GOLD, false);
            y += 17;
        }
        graphics.text(font, fit(font, card.status(), Math.max(40, right - left)), left, y,
                card.available() ? CYAN : RED, false);
        y += 20;
        graphics.fill(left, y, right, y + 1, LINE);
        y += 12;

        int actionTop = pane.bottom() - 43;
        graphics.enableScissor(pane.left() + 1, pane.top() + 1, pane.right() - 1, Math.max(pane.top() + 2, actionTop));
        List<FormattedCharSequence> detail = font.split(Component.literal(card.effect()), Math.max(70, right - left));
        for (FormattedCharSequence line : detail) {
            if (y > actionTop - 8) break;
            graphics.text(font, line, left, y, TEXT, false);
            y += 12;
        }
        String guidance = guidance(card);
        List<FormattedCharSequence> guideLines = font.split(Component.literal(guidance), Math.max(70, right - left));
        y += 8;
        for (FormattedCharSequence line : guideLines) {
            if (y > actionTop - 8) break;
            graphics.text(font, line, left, y, MUTED, false);
            y += 11;
        }
        graphics.disableScissor();

        Button button = actionButton(pane);
        boolean enabled = card.available();
        boolean hover = enabled && inside(mouseX, mouseY, button.x(), button.y(), button.w(), button.h());
        graphics.fill(button.x(), button.y(), button.x() + button.w(), button.y() + button.h(),
                enabled ? (hover ? SURFACE_2 : SURFACE) : 0x8820272A);
        graphics.fill(button.x(), button.y() + button.h() - 2, button.x() + button.w(), button.y() + button.h(),
                enabled ? (hover ? GOLD : CYAN) : LINE);
        graphics.centeredText(font, fit(font, actionLabel(card), button.w() - 8),
                button.x() + button.w() / 2, button.y() + 8, enabled ? TEXT : MUTED);
    }

    private String guidance(OfferCard card) {
        if (card.action().equals("sell_loot")) {
            return "[판매용] 전리품과 구형 바닐라 잡템만 일괄 정산합니다. 수호 화살·전투 건량·마을 배급빵은 제외됩니다.";
        }
        if (card.action().equals("open_item_sell")) {
            return "개별 판매 화면에서 실제 판매 대상을 직접 고른 뒤 다시 실행합니다.";
        }
        if (card.action().equals("buy_arrows") || card.action().equals("buy_food")) {
            return "반복 구매용 보급품입니다. 실행 버튼을 누르면 별도 확인창 없이 즉시 구매하고 상점을 갱신합니다.";
        }
        return "주화가 소모되는 장비 구매는 마지막 확인창에서 한 번 더 검토할 수 있습니다.";
    }

    private String actionLabel(OfferCard card) {
        if (!card.available()) return "이용 불가";
        if (card.action().equals("open_item_sell")) return "판매 목록 열기";
        if (card.action().equals("sell_loot")) return "일괄 정산";
        return "구매";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        VillageUiSafeArea.Rect safe = layout.safe();
        int count = Category.values().length;
        int gap = 4;
        int tabWidth = Math.max(36, (safe.width() - 14 - gap * (count - 1)) / count);
        int tabY = safe.top() + 43;
        for (int i = 0; i < count; i++) {
            int x = safe.left() + 7 + i * (tabWidth + gap);
            if (inside(click.x(), click.y(), x, tabY, tabWidth, 22)) {
                category = Category.values()[i];
                selected = firstVisible();
                scroll = 0;
                return true;
            }
        }

        List<Integer> visible = visibleIndices();
        int y = layout.list().top() + 5 - scroll;
        for (int actual : visible) {
            if (inside(click.x(), click.y(), layout.list().left() + 5, y, layout.list().width() - 10, 40)) {
                selected = actual;
                return true;
            }
            y += 45;
        }

        if (selected >= 0 && selected < offers.size() && category.accepts(offers.get(selected).category())) {
            OfferCard card = offers.get(selected);
            Button button = actionButton(layout.detail());
            if (card.available() && inside(click.x(), click.y(), button.x(), button.y(), button.w(), button.h())) {
                execute(card);
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

    private void execute(OfferCard card) {
        String action = card.action();
        if (action.equals("open_item_sell") || action.equals("buy_arrows") || action.equals("buy_food")) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
            return;
        }
        boolean confirm = action.equals("sell_loot") || action.startsWith("gear:")
                || VillageActionDescriptions.requiresConfirmation(action);
        if (confirm && minecraft != null) {
            minecraft.gui.setScreen(new VillageConfirmScreen(this, action, card.name(),
                    card.effect() + (card.cost().isBlank() ? "" : "\n" + card.cost())));
        } else {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        }
    }

    private Button actionButton(Pane pane) {
        int width = Math.min(150, Math.max(82, pane.width() / 3));
        width = Math.min(width, Math.max(1, pane.width() - 28));
        return new Button(pane.right() - width - 14, pane.bottom() - 36, width, 24);
    }

    private Layout layout() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int top = safe.top() + 72;
        int bottom = safe.bottom() - 18;
        int gap = 8;
        int contentHeight = Math.max(1, bottom - top);
        int innerWidth = Math.max(1, safe.width() - 14);
        Pane list;
        Pane detail;
        boolean stacked = safe.width() < 390 && contentHeight >= 190;
        if (stacked) {
            int listHeight = VillageUiSafeArea.clamp(contentHeight * 42 / 100, 78,
                    Math.max(78, contentHeight - 104));
            list = new Pane(safe.left() + 7, top, safe.right() - 7, top + listHeight);
            detail = new Pane(safe.left() + 7, list.bottom() + gap, safe.right() - 7, bottom);
        } else {
            int listWidth = VillageUiSafeArea.clamp(innerWidth * 38 / 100, 105, 360);
            listWidth = Math.min(listWidth, Math.max(86, innerWidth - gap - 120));
            list = new Pane(safe.left() + 7, top, safe.left() + 7 + listWidth, bottom);
            detail = new Pane(list.right() + gap, top, safe.right() - 7, bottom);
        }
        return new Layout(safe, list, detail);
    }

    private void parse(VillageNetwork.OpenVillageUiPayload payload) {
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) {
            String action = actions[i];
            String[] p = labels[i].split("\\|", -1);
            if (p.length >= 7 && "shop".equals(p[0])) {
                offers.add(new OfferCard(action, Category.parse(p[1]), plain(p[2]), plain(p[3]),
                        plain(p[4]), plain(p[5]), "available".equals(p[6])));
            } else if (p.length >= 2 && "shop_utility".equals(p[0])) {
                String effect = action.equals("sell_loot")
                        ? "판매용 잡템만 안전하게 한 번에 정산"
                        : "보유품을 직접 확인하고 하나씩 선택 판매";
                offers.add(new OfferCard(action, Category.SALE, plain(p[1]), "", effect,
                        "이용 가능", true));
            }
        }
    }

    private List<Integer> visibleIndices() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) if (category.accepts(offers.get(i).category())) result.add(i);
        return result;
    }

    private int firstVisible() {
        for (int i = 0; i < offers.size(); i++) if (category.accepts(offers.get(i).category())) return i;
        return -1;
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

    private enum Category {
        ALL("전체", GOLD), EQUIPMENT("장비", CYAN), ARMOR("방어구", 0xFF71A8FF),
        CONSUMABLE("소모품", 0xFF75D98D), SALE("판매", 0xFFE38A74);
        private final String displayName;
        private final int accent;
        Category(String displayName, int accent) { this.displayName = displayName; this.accent = accent; }
        String displayName() { return displayName; }
        int accent() { return accent; }
        boolean accepts(Category candidate) { return this == ALL || this == candidate; }
        static Category parse(String raw) {
            String value = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
            return switch (value) {
                case "equipment" -> EQUIPMENT;
                case "armor" -> ARMOR;
                case "other", "consumable", "supply" -> CONSUMABLE;
                default -> CONSUMABLE;
            };
        }
    }

    private record OfferCard(String action, Category category, String name, String cost,
                             String effect, String status, boolean available) {}
    private record Button(int x, int y, int w, int h) {}
    private record Pane(int left, int top, int right, int bottom) {
        int width() { return Math.max(1, right - left); }
        int height() { return Math.max(1, bottom - top); }
    }
    private record Layout(VillageUiSafeArea.Rect safe, Pane list, Pane detail) {}
}
