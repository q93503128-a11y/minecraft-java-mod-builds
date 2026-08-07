package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class VillageRelicScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x69000000;
    private static final int PANEL = 0xFFE5D8BE;
    private static final int SURFACE = 0xFFFFF8E8;
    private static final int LOCKED = 0xFFD4CBB8;
    private static final int BORDER = 0xFF6F5B43;
    private static final int TEXT = 0xFF211A14;
    private static final int MUTED = 0xFF665D52;
    private static final int GOLD = 0xFFB87B20;
    private static final int TEAL = 0xFF267E73;
    private static final int RED = 0xFFAA4545;
    private static final int CARD_HEIGHT = 68;
    private static final int GAP = 7;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final List<RelicCard> cards = new ArrayList<>();
    private int scroll;

    public VillageRelicScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        parse();
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

        int closeX = layout.right() - 36;
        graphics.text(font, fit(plain(payload.title()), Math.max(40, closeX - layout.left() - 34)),
                layout.left() + 18, layout.top() + 10, TEXT, false);
        boolean close = inside(mouseX, mouseY, closeX, layout.top() + 7, 27, 27);
        graphics.fill(closeX, layout.top() + 7, closeX + 27, layout.top() + 34,
                close ? 0xFFE2AAAA : 0xFFD8CBB1);
        graphics.centeredText(font, "×", closeX + 13, layout.top() + 16, close ? RED : TEXT);

        List<FormattedCharSequence> summary = font.split(Component.literal(plain(payload.body())),
                Math.max(120, layout.width() - 48));
        int summaryLines = Math.min(4, summary.size());
        int y = layout.top() + 30;
        for (int index = 0; index < summaryLines; index++) {
            graphics.text(font, summary.get(index), layout.left() + 18, y, index == 0 ? GOLD : MUTED, false);
            y += 11;
        }

        int contentTop = layout.top() + 38 + summaryLines * 11;
        int contentLeft = layout.left() + 14;
        int contentRight = layout.right() - 14;
        int contentBottom = layout.bottom() - 13;
        graphics.fill(contentLeft - 1, contentTop - 1, contentRight + 1, contentBottom + 1, BORDER);
        graphics.fill(contentLeft, contentTop, contentRight, contentBottom, 0xFFEFE4CF);

        int columns = contentRight - contentLeft >= 610 ? 2 : 1;
        int innerWidth = contentRight - contentLeft - 14;
        int cardWidth = Math.max(100, (innerWidth - (columns - 1) * GAP) / columns);
        int rows = (cards.size() + columns - 1) / columns;
        int contentHeight = rows == 0 ? 0 : rows * CARD_HEIGHT + (rows - 1) * GAP;
        int viewport = Math.max(1, contentBottom - contentTop - 14);
        int maximum = Math.max(0, contentHeight - viewport);
        scroll = clamp(scroll, 0, maximum);

        graphics.enableScissor(contentLeft + 2, contentTop + 2, contentRight - 2, contentBottom - 2);
        for (int index = 0; index < cards.size(); index++) {
            int row = index / columns;
            int column = index % columns;
            int x = contentLeft + 7 + column * (cardWidth + GAP);
            int cardY = contentTop + 7 + row * (CARD_HEIGHT + GAP) - scroll;
            renderCard(graphics, cards.get(index), x, cardY, cardWidth);
        }
        graphics.disableScissor();
        scrollbar(graphics, contentRight - 5, contentTop + 6, contentBottom - 6,
                scroll, maximum, viewport, contentHeight);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCard(GuiGraphicsExtractor graphics, RelicCard card, int x, int y, int width) {
        int accent = card.owned() ? GOLD : 0xFF8B8377;
        graphics.fill(x - 1, y - 1, x + width + 1, y + CARD_HEIGHT + 1, accent);
        graphics.fill(x, y, x + width, y + CARD_HEIGHT, card.owned() ? SURFACE : LOCKED);
        graphics.fill(x, y, x + 5, y + CARD_HEIGHT, card.owned() ? TEAL : 0xFF8B8377);
        graphics.text(font, fit(card.name(), width - 88), x + 12, y + 7,
                card.owned() ? TEXT : MUTED, false);
        String state = card.owned() ? "획득" : "미획득";
        int stateColor = card.owned() ? TEAL : MUTED;
        graphics.text(font, state, x + width - font.width(state) - 10, y + 7, stateColor, false);

        List<FormattedCharSequence> lines = font.split(Component.literal(card.description()), Math.max(60, width - 24));
        int textY = y + 24;
        for (int index = 0; index < Math.min(3, lines.size()); index++) {
            graphics.text(font, lines.get(index), x + 12, textY, card.owned() ? TEXT : MUTED, false);
            textY += 11;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
            Layout layout = layout();
            if (inside(click.x(), click.y(), layout.right() - 36, layout.top() + 7, 27, 27)) {
                onClose();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int) Math.round(vertical * 34));
        return true;
    }

    private void parse() {
        if (payload.labels().isBlank()) return;
        for (String raw : payload.labels().split(SEP, -1)) {
            String[] parts = raw.split("\\|", 5);
            if (parts.length < 5 || !"relic".equals(parts[0])) continue;
            cards.add(new RelicCard(parts[1], "owned".equals(parts[2]), plain(parts[3]), plain(parts[4])));
        }
    }

    private Layout layout() {
        int panelWidth = Math.min(820, Math.max(300, width - 16));
        int panelHeight = Math.min(height - 12, Math.max(230, height - 22));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private String fit(String text, int maxWidth) {
        String safe = plain(text);
        if (font.width(safe) <= maxWidth) return safe;
        int end = safe.length();
        while (end > 0 && font.width(safe.substring(0, end) + "…") > maxWidth) end--;
        return safe.substring(0, end) + "…";
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static void scrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                                  int scroll, int maximum, int viewport, int content) {
        if (maximum <= 0 || content <= 0) return;
        graphics.fill(x, top, x + 2, bottom, 0x556F5B43);
        int track = Math.max(1, bottom - top);
        int thumb = Math.max(16, Math.round(track * Math.min(1.0f, viewport / (float) content)));
        int travel = Math.max(0, track - thumb);
        int thumbY = top + Math.round(travel * (scroll / (float) maximum));
        graphics.fill(x - 1, thumbY, x + 3, thumbY + thumb, TEAL);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record RelicCard(String id, boolean owned, String name, String description) {}
    private record Layout(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }
    }
}
