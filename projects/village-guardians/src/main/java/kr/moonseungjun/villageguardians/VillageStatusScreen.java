package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Read-only status page. It intentionally has no action selector or execute pane. */
public final class VillageStatusScreen extends Screen {
    private static final int OVERLAY = 0xB805080D;
    private static final int PANEL = 0xFF0A1017;
    private static final int SURFACE = 0xFF14212C;
    private static final int SURFACE_HOVER = 0xFF203342;
    private static final int BORDER = 0xFF587083;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFC1CDD6;
    private static final int ACCENT = 0xFF45D8C0;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private int scroll;

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
        Layout layout = layout();
        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, layout.bottom(), ACCENT);

        int titleX = layout.left() + 20;
        int closeX = layout.right() - 39;
        graphics.text(font, payload.title(), titleX, layout.top() + 14, TEXT, false);
        graphics.text(font, "현재 수호자 정보", titleX, layout.top() + 33, MUTED, false);
        boolean closeHovered = inside(mouseX, mouseY, closeX, layout.top() + 10, 29, 29);
        graphics.fill(closeX, layout.top() + 10, closeX + 29, layout.top() + 39,
                closeHovered ? 0xFF79343D : SURFACE_HOVER);
        graphics.centeredText(font, "×", closeX + 14, layout.top() + 20, closeHovered ? TEXT : MUTED);

        int bodyLeft = layout.left() + 18;
        int bodyTop = layout.top() + 58;
        int bodyRight = layout.right() - 18;
        int bodyBottom = layout.bottom() - 16;
        drawPanel(graphics, bodyLeft, bodyTop, bodyRight, bodyBottom);

        int textLeft = bodyLeft + 22;
        int textTop = bodyTop + 19;
        int textRight = bodyRight - 22;
        int textBottom = bodyBottom - 16;
        List<FormattedCharSequence> lines = bodyLines(Math.max(120, textRight - textLeft));
        int contentHeight = Math.max(1, lines.size() * 17 + 4);
        int visible = Math.max(1, textBottom - textTop);
        int maxScroll = Math.max(0, contentHeight - visible);
        scroll = clamp(scroll, 0, maxScroll);

        graphics.enableScissor(bodyLeft + 2, bodyTop + 2, bodyRight - 2, bodyBottom - 2);
        int y = textTop - scroll;
        for (FormattedCharSequence line : lines) {
            if (y >= textTop - 13 && y <= textBottom) graphics.text(font, line, textLeft, y, TEXT, false);
            y += 17;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, bodyRight - 8, bodyTop + 10, bodyBottom - 10,
                scroll, maxScroll, visible, contentHeight);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
            Layout layout = layout();
            if (inside(click.x(), click.y(), layout.right() - 39, layout.top() + 10, 29, 29)) {
                onClose();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Layout layout = layout();
        if (inside(mouseX, mouseY, layout.left(), layout.top(), layout.width(), layout.height())) {
            scroll = Math.max(0, scroll - (int) Math.round(vertical * 44));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private List<FormattedCharSequence> bodyLines(int lineWidth) {
        List<FormattedCharSequence> result = new ArrayList<>();
        for (String paragraph : payload.body().split("\n", -1)) {
            if (paragraph.isBlank()) result.add(FormattedCharSequence.EMPTY);
            else result.addAll(font.split(Component.literal(paragraph), lineWidth));
        }
        return result;
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, BORDER);
        graphics.fill(left, top, right, bottom, SURFACE);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                               int value, int maximum, int visible, int content) {
        if (maximum <= 0 || content <= visible || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(20, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(value, 0, maximum) / maximum;
        graphics.fill(x, top, x + 4, bottom, 0xFF05090D);
        graphics.fill(x, y, x + 4, y + thumb, ACCENT);
    }

    private Layout layout() {
        int margin = 8;
        int panelWidth = Math.min(900, Math.max(320, width - margin * 2));
        int panelHeight = Math.min(620, Math.max(220, height - margin * 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
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
}
