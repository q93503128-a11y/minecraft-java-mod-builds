package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Boss relic reward: three monumental choices, not a generic facility list. */
public final class VillageRelicChoiceScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x76040A0C;
    private static final int TEXT = 0xFFFFF5DB;
    private static final int MUTED = 0xFFC7BFAE;
    private static final int GOLD = 0xFFFFC85A;
    private static final int FRAME = 0xFF7A5D2B;
    private static final int FACE = 0xE51C1715;
    private static final int HOVER = 0xF02E2419;

    private final String body;
    private final String[] actions;
    private final List<Choice> choices = new ArrayList<>();

    public VillageRelicChoiceScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        body = payload.body();
        actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        for (int index = 0; index < Math.min(actions.length, labels.length); index++) {
            String[] p = labels[index].split("\\|", 2);
            choices.add(new Choice(p[0], p.length > 1 ? p[1] : ""));
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2;
        int top = Math.max(15, height / 12);
        graphics.centeredText(font, "✦  " + title.getString() + "  ✦", cx, top, GOLD);
        graphics.centeredText(font, fit(body.replace('\n', ' '), Math.max(120, width - 50)), cx, top + 18, MUTED);
        graphics.fill(Math.max(30, cx - 170), top + 34, Math.min(width - 30, cx + 170), top + 35, 0x88A77A32);

        Layout layout = layout(top + 49);
        for (int index = 0; index < choices.size(); index++) {
            Bounds b = bounds(index, choices.size(), layout);
            boolean hovered = inside(mouseX, mouseY, b);
            drawRelicCard(graphics, b, choices.get(index), hovered, index);
        }
        graphics.centeredText(font, "하나를 선택하면 즉시 영구 적용됩니다.", cx,
                Math.min(height - 18, layout.bottom() + 14), MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawRelicCard(GuiGraphicsExtractor graphics, Bounds b, Choice choice, boolean hovered, int index) {
        int cap = Math.min(24, Math.max(12, b.width() / 7));
        int center = b.x() + b.width() / 2;
        for (int i = 0; i < cap; i++) {
            int inset = cap - i;
            graphics.fill(b.x() + inset, b.y() + i, b.x() + b.width() - inset, b.y() + i + 1,
                    hovered ? GOLD : FRAME);
        }
        graphics.fill(b.x(), b.y() + cap, b.x() + b.width(), b.y() + b.height() - cap,
                hovered ? GOLD : FRAME);
        for (int i = 0; i < cap; i++) {
            int inset = i;
            graphics.fill(b.x() + inset, b.y() + b.height() - cap + i,
                    b.x() + b.width() - inset, b.y() + b.height() - cap + i + 1,
                    hovered ? GOLD : FRAME);
        }

        int inner = 2;
        graphics.fill(b.x() + inner, b.y() + cap + 1,
                b.x() + b.width() - inner, b.y() + b.height() - cap - 1,
                hovered ? HOVER : FACE);
        VillageQuickChatScreen.drawDiamond(graphics, center, b.y() + 42, hovered ? 22 : 18, 0xE5162023);
        VillageQuickChatScreen.drawDiamondOutline(graphics, center, b.y() + 42, hovered ? 22 : 18,
                hovered ? GOLD : 0xFFAE8950);
        graphics.centeredText(font, Integer.toString(index + 1), center, b.y() + 38, hovered ? GOLD : MUTED);

        graphics.centeredText(font, fit(choice.name(), b.width() - 20), center, b.y() + 78,
                hovered ? GOLD : TEXT);
        List<FormattedCharSequence> lines = font.split(Component.literal(choice.description()),
                Math.max(50, b.width() - 24));
        int y = b.y() + 98;
        for (int line = 0; line < Math.min(5, lines.size()); line++) {
            int lineWidth = font.width(lines.get(line));
            graphics.text(font, lines.get(line), center - lineWidth / 2, y, MUTED, false);
            y += 12;
        }
        if (hovered) graphics.centeredText(font, "선택", center, b.y() + b.height() - 34, GOLD);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int top = Math.max(15, height / 12);
        Layout layout = layout(top + 49);
        for (int index = 0; index < choices.size(); index++) {
            if (inside(click.x(), click.y(), bounds(index, choices.size(), layout))) {
                ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(actions[index]));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private Layout layout(int top) {
        int count = Math.max(1, choices.size());
        int gap = width < 500 ? 7 : 14;
        int maxCard = width < 500 ? 150 : 190;
        int usable = Math.max(180, width - 26 - gap * (count - 1));
        int cardWidth = Math.min(maxCard, usable / count);
        int total = cardWidth * count + gap * (count - 1);
        int cardHeight = clamp(height - top - 48, 150, 260);
        return new Layout((width - total) / 2, top, cardWidth, cardHeight, gap,
                top + cardHeight);
    }

    private Bounds bounds(int index, int count, Layout layout) {
        return new Bounds(layout.left() + index * (layout.cardWidth() + layout.gap()), layout.top(),
                layout.cardWidth(), layout.cardHeight());
    }

    private String fit(String value, int maxWidth) {
        String safe = value == null ? "" : value;
        if (maxWidth <= 0 || font.width(safe) <= maxWidth) return maxWidth <= 0 ? "" : safe;
        int end = safe.length();
        while (end > 0 && font.width(safe.substring(0, end) + "…") > maxWidth) end--;
        return safe.substring(0, end) + "…";
    }

    private static boolean inside(double x, double y, Bounds b) {
        return x >= b.x() && x < b.x() + b.width() && y >= b.y() && y < b.y() + b.height();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Choice(String name, String description) {}
    private record Bounds(int x, int y, int width, int height) {}
    private record Layout(int left, int top, int cardWidth, int cardHeight, int gap, int bottom) {}
}
