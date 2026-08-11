package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Permanent relic rewards stay monumental, but selection is no longer a single irreversible click. */
public final class VillageRelicChoiceConfirmScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x76040A0C;
    private static final int TEXT = 0xFFFFF5DB;
    private static final int MUTED = 0xFFC7BFAE;
    private static final int GOLD = 0xFFFFC85A;
    private static final int FRAME = 0xFF7A5D2B;
    private static final int FACE = 0xE51C1715;
    private static final int HOVER = 0xF02E2419;

    private final String body;
    private final List<Choice> choices = new ArrayList<>();

    public VillageRelicChoiceConfirmScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        body = plain(payload.body());
        parse(payload);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int cx = safe.centerX();
        int top = safe.top() + 4;
        graphics.centeredText(font, "✦  " + title.getString() + "  ✦", cx, top, GOLD);
        graphics.centeredText(font, fit(font, body.replace('\n', ' '), Math.max(120, safe.width() - 30)),
                cx, top + 18, MUTED);
        graphics.fill(Math.max(safe.left() + 8, cx - 170), top + 34,
                Math.min(safe.right() - 8, cx + 170), top + 35, 0x88A77A32);

        Layout layout = layout(safe, top + 49);
        for (int i = 0; i < choices.size(); i++) {
            Bounds b = bounds(i, layout);
            drawChoice(graphics, b, choices.get(i), inside(mouseX, mouseY, b), i);
        }
        graphics.centeredText(font, "카드를 누른 뒤 확인창에서 최종 선택합니다. 선택 후 영구 적용됩니다.",
                cx, Math.min(safe.bottom() - 12, layout.bottom() + 12), MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawChoice(GuiGraphicsExtractor graphics, Bounds b, Choice choice, boolean hovered, int index) {
        int cap = Math.min(22, Math.max(11, b.width() / 8));
        int center = b.x() + b.width() / 2;
        for (int i = 0; i < cap; i++) {
            int inset = cap - i;
            graphics.fill(b.x() + inset, b.y() + i, b.x() + b.width() - inset, b.y() + i + 1,
                    hovered ? GOLD : FRAME);
        }
        graphics.fill(b.x(), b.y() + cap, b.x() + b.width(), b.y() + b.height() - cap,
                hovered ? GOLD : FRAME);
        for (int i = 0; i < cap; i++) {
            graphics.fill(b.x() + i, b.y() + b.height() - cap + i,
                    b.x() + b.width() - i, b.y() + b.height() - cap + i + 1,
                    hovered ? GOLD : FRAME);
        }
        graphics.fill(b.x() + 2, b.y() + cap + 1, b.x() + b.width() - 2,
                b.y() + b.height() - cap - 1, hovered ? HOVER : FACE);

        int runeY = b.y() + Math.min(40, Math.max(27, b.height() / 4));
        VillageQuickChatSafeScreen.drawDiamond(graphics, center, runeY, hovered ? 20 : 17, 0xE5162023);
        VillageQuickChatSafeScreen.drawDiamondOutline(graphics, center, runeY, hovered ? 20 : 17,
                hovered ? GOLD : 0xFFAE8950);
        graphics.centeredText(font, Integer.toString(index + 1), center, runeY - 4, hovered ? GOLD : MUTED);
        int nameY = runeY + 31;
        graphics.centeredText(font, fit(font, choice.name(), b.width() - 20), center, nameY,
                hovered ? GOLD : TEXT);

        List<FormattedCharSequence> lines = font.split(Component.literal(choice.description()), Math.max(50, b.width() - 24));
        int y = nameY + 18;
        int maxLines = Math.max(1, Math.min(6, (b.y() + b.height() - 30 - y) / 11));
        for (int line = 0; line < Math.min(maxLines, lines.size()); line++) {
            int lineWidth = font.width(lines.get(line));
            graphics.text(font, lines.get(line), center - lineWidth / 2, y, MUTED, false);
            y += 11;
        }
        if (hovered) graphics.centeredText(font, "선택 검토", center, b.y() + b.height() - 22, GOLD);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        Layout layout = layout(safe, safe.top() + 53);
        for (int i = 0; i < choices.size(); i++) {
            if (!inside(click.x(), click.y(), bounds(i, layout))) continue;
            Choice choice = choices.get(i);
            if (minecraft != null) {
                minecraft.gui.setScreen(new VillageConfirmScreen(this, choice.action(), choice.name(),
                        choice.description() + "\n\n이 선택은 영구 적용되며 현재 보상 선택에서 되돌릴 수 없습니다."));
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void parse(VillageNetwork.OpenVillageUiPayload payload) {
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) {
            if ("facility_info".equals(actions[i])) continue;
            String[] p = labels[i].split("\\|", 2);
            choices.add(new Choice(actions[i], plain(p.length > 0 ? p[0] : "유물"),
                    plain(p.length > 1 ? p[1] : "")));
        }
    }

    private Layout layout(VillageUiSafeArea.Rect safe, int top) {
        int count = Math.max(1, choices.size());
        int gap = safe.width() < 500 ? 7 : 14;
        int maxCard = safe.width() < 500 ? 150 : 190;
        int usable = Math.max(150, safe.width() - 18 - gap * (count - 1));
        int cardWidth = Math.min(maxCard, usable / count);
        int total = cardWidth * count + gap * (count - 1);
        int availableHeight = Math.max(120, safe.bottom() - top - 29);
        int cardHeight = Math.min(242, availableHeight);
        return new Layout(safe.centerX() - total / 2, top, cardWidth, cardHeight, gap, top + cardHeight);
    }

    private Bounds bounds(int index, Layout layout) {
        return new Bounds(layout.left() + index * (layout.cardWidth() + layout.gap()), layout.top(),
                layout.cardWidth(), layout.cardHeight());
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static String fit(Font font, String value, int maxWidth) {
        String safe = value == null ? "" : value;
        if (maxWidth <= 0 || font.width(safe) <= maxWidth) return maxWidth <= 0 ? "" : safe;
        int end = safe.length();
        while (end > 0 && font.width(safe.substring(0, end) + "…") > maxWidth) end--;
        return safe.substring(0, end) + "…";
    }

    private static boolean inside(double x, double y, Bounds b) {
        return x >= b.x() && x < b.x() + b.width() && y >= b.y() && y < b.y() + b.height();
    }

    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Choice(String action, String name, String description) {}
    private record Bounds(int x, int y, int width, int height) {}
    private record Layout(int left, int top, int cardWidth, int cardHeight, int gap, int bottom) {}
}
