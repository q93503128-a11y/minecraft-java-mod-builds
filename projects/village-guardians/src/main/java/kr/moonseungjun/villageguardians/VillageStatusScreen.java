package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Compact read-only page. Status and wave intelligence always fit without a selector or scrollbar. */
public final class VillageStatusScreen extends Screen {
    private static final int OVERLAY = 0x65000000;
    private static final int PANEL = 0xFFF0E5CC;
    private static final int SURFACE = 0xFFFFF8E8;
    private static final int SURFACE_ALT = 0xFFE6D9BE;
    private static final int BORDER = 0xFF75634C;
    private static final int TEXT = 0xFF241D17;
    private static final int MUTED = 0xFF6D6256;
    private static final int ACCENT = 0xFF2E8E80;
    private static final int RED = 0xFFB95050;

    private final VillageNetwork.OpenVillageUiPayload payload;

    public VillageStatusScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int maximumWidth = Math.min(820, Math.max(300, width - 20));
        int singleColumnWidth = maximumWidth - 54;
        List<FormattedCharSequence> singleLines = bodyLines(Math.max(120, singleColumnWidth));
        int maximumRows = Math.max(8, (height - 120) / 16);
        boolean twoColumns = singleLines.size() > maximumRows && maximumWidth >= 620;
        int columnWidth = twoColumns ? (maximumWidth - 70) / 2 : singleColumnWidth;
        List<FormattedCharSequence> lines = bodyLines(Math.max(120, columnWidth));
        int rows = twoColumns ? (lines.size() + 1) / 2 : lines.size();
        int panelHeight = Math.min(height - 12, Math.max(170, 92 + rows * 16));
        int panelWidth = Math.min(width - 12, maximumWidth);
        Layout layout = new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);

        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, layout.bottom(), ACCENT);

        int titleX = layout.left() + 20;
        int closeX = layout.right() - 39;
        graphics.text(font, payload.title(), titleX, layout.top() + 13, TEXT, false);
        graphics.text(font, payload.screenId().equals("wave_intel") ? "다음 웨이브 정찰 보고" : "현재 수호자 정보",
                titleX, layout.top() + 32, MUTED, false);
        boolean hovered = inside(mouseX, mouseY, closeX, layout.top() + 9, 29, 29);
        graphics.fill(closeX, layout.top() + 9, closeX + 29, layout.top() + 38,
                hovered ? 0xFFE6A6A6 : SURFACE_ALT);
        graphics.centeredText(font, "×", closeX + 14, layout.top() + 19, hovered ? RED : TEXT);

        int bodyLeft = layout.left() + 18;
        int bodyTop = layout.top() + 55;
        int bodyRight = layout.right() - 18;
        int bodyBottom = layout.bottom() - 16;
        graphics.fill(bodyLeft - 1, bodyTop - 1, bodyRight + 1, bodyBottom + 1, BORDER);
        graphics.fill(bodyLeft, bodyTop, bodyRight, bodyBottom, SURFACE);

        int firstX = bodyLeft + 20;
        int secondX = bodyLeft + 20 + columnWidth + 22;
        int textTop = bodyTop + 18;
        int split = twoColumns ? (lines.size() + 1) / 2 : lines.size();
        for (int i = 0; i < lines.size(); i++) {
            int column = twoColumns && i >= split ? 1 : 0;
            int row = column == 0 ? i : i - split;
            int x = column == 0 ? firstX : secondX;
            int y = textTop + row * 16;
            if (y <= bodyBottom - 13) graphics.text(font, lines.get(i), x, y, TEXT, false);
        }
        if (twoColumns) {
            int divider = secondX - 11;
            graphics.fill(divider, bodyTop + 12, divider + 1, bodyBottom - 12, 0xFFC9BCA3);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private List<FormattedCharSequence> bodyLines(int width) {
        List<FormattedCharSequence> result = new ArrayList<>();
        for (String paragraph : payload.body().split("\n", -1)) {
            if (paragraph.isBlank()) result.add(FormattedCharSequence.EMPTY);
            else result.addAll(font.split(Component.literal(paragraph), width));
        }
        while (!result.isEmpty() && result.getLast() == FormattedCharSequence.EMPTY) result.removeLast();
        return result;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
            int panelWidth = Math.min(width - 12, Math.min(820, Math.max(300, width - 20)));
            int left = (width - panelWidth) / 2;
            if (inside(click.x(), click.y(), left + panelWidth - 39, 6, 31, height - 12)) {
                // The close button is always in the upper-right of the centered panel.
                if (click.y() < height / 2) {
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
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
}
