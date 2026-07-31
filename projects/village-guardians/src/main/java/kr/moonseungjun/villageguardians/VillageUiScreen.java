package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class VillageUiScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0xD90A0D12;
    private static final int SHADOW = 0x99000000;
    private static final int PANEL = 0xFF141920;
    private static final int PANEL_SOFT = 0xFF1B222B;
    private static final int PANEL_RAISED = 0xFF222B35;
    private static final int BORDER = 0xFF3B4653;
    private static final int ACCENT = 0xFF43C6AC;
    private static final int ACCENT_DARK = 0xFF237A70;
    private static final int GOLD = 0xFFE6B65A;
    private static final int DANGER = 0xFFB94B55;
    private static final int TEXT = 0xFFF3F6F8;
    private static final int MUTED = 0xFF9BA7B4;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private final List<ClickRegion> clickRegions = new ArrayList<>();

    private int scrollOffset;
    private int maxScroll;
    private int bodyLeft;
    private int bodyTop;
    private int bodyRight;
    private int bodyBottom;

    public VillageUiScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        this.actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        this.labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
        for (int x = 0; x < width; x += 28) {
            graphics.fill(x, 0, x + 1, height, 0x12000000);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        int left = layout.left();
        int top = layout.top();
        int panelWidth = layout.width();
        int panelHeight = layout.height();

        graphics.fill(left + 7, top + 8, left + panelWidth + 7, top + panelHeight + 8, SHADOW);
        graphics.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, BORDER);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left, top, left + panelWidth, top + 4, ACCENT);

        graphics.fill(left + 18, top + 17, left + 54, top + 53, PANEL_RAISED);
        graphics.fill(left + 18, top + 17, left + 22, top + 53, GOLD);
        graphics.centeredText(font, "VG", left + 37, top + 31, TEXT);
        graphics.text(font, "VILLAGE COMMAND", left + 66, top + 19, MUTED, false);
        graphics.text(font, payload.title(), left + 66, top + 35, TEXT, false);

        int closeLeft = left + panelWidth - 43;
        int closeTop = top + 17;
        boolean closeHovered = isInside(mouseX, mouseY, closeLeft, closeTop, 25, 25);
        graphics.fill(closeLeft, closeTop, closeLeft + 25, closeTop + 25,
                closeHovered ? 0xFF7B3038 : PANEL_RAISED);
        graphics.centeredText(font, "×", closeLeft + 12, closeTop + 8,
                closeHovered ? 0xFFFFFFFF : MUTED);

        graphics.fill(left + 18, top + 65, left + panelWidth - 18, top + 66, BORDER);

        int buttonCount = Math.min(actions.length, labels.length);
        int columns = panelWidth < 440 ? 1 : 2;
        int buttonRows = (buttonCount + columns - 1) / columns;
        int buttonHeight = 30;
        int buttonGap = 8;
        int buttonAreaHeight = buttonRows * buttonHeight + Math.max(0, buttonRows - 1) * buttonGap;
        int startY = top + panelHeight - 18 - buttonAreaHeight;

        bodyLeft = left + 24;
        bodyTop = top + 82;
        bodyRight = left + panelWidth - 24;
        bodyBottom = Math.max(bodyTop + 28, startY - 16);

        graphics.fill(bodyLeft - 8, bodyTop - 9, bodyRight + 8, bodyBottom + 5, PANEL_SOFT);
        graphics.fill(bodyLeft - 8, bodyTop - 9, bodyLeft - 4, bodyBottom + 5, ACCENT_DARK);
        graphics.text(font, "현황 및 명령 정보", bodyLeft, bodyTop - 2, GOLD, false);

        List<FormattedCharSequence> lines = bodyLines(panelWidth - 64);
        int contentHeight = lines.size() * 13;
        int visibleHeight = Math.max(1, bodyBottom - bodyTop - 16);
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        int textTop = bodyTop + 15;
        graphics.enableScissor(bodyLeft - 1, textTop, bodyRight + 1, bodyBottom);
        int y = textTop - scrollOffset;
        for (FormattedCharSequence line : lines) {
            if (y + 10 >= textTop && y <= bodyBottom) {
                graphics.text(font, line, bodyLeft, y, TEXT, false);
            }
            y += 13;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackTop = textTop;
            int trackBottom = bodyBottom - 3;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            int thumbHeight = Math.max(14, trackHeight * visibleHeight / Math.max(visibleHeight, contentHeight));
            int thumbTravel = Math.max(0, trackHeight - thumbHeight);
            int thumbTop = trackTop + (maxScroll == 0 ? 0 : thumbTravel * scrollOffset / maxScroll);
            graphics.fill(bodyRight + 3, trackTop, bodyRight + 5, trackBottom, 0xFF0B0E12);
            graphics.fill(bodyRight + 3, thumbTop, bodyRight + 5, thumbTop + thumbHeight, ACCENT);
        }

        clickRegions.clear();
        int horizontalGap = 12;
        int buttonWidth = columns == 1
                ? panelWidth - 48
                : (panelWidth - 48 - horizontalGap) / 2;
        for (int index = 0; index < buttonCount; index++) {
            int column = index % columns;
            int row = index / columns;
            int x = left + 24 + column * (buttonWidth + horizontalGap);
            int by = startY + row * (buttonHeight + buttonGap);
            String action = actions[index];
            int accent = actionAccent(action);
            boolean hovered = isInside(mouseX, mouseY, x, by, buttonWidth, buttonHeight);

            graphics.fill(x + 2, by + 3, x + buttonWidth + 2, by + buttonHeight + 3, 0x66000000);
            graphics.fill(x - 1, by - 1, x + buttonWidth + 1, by + buttonHeight + 1,
                    hovered ? accent : BORDER);
            graphics.fill(x, by, x + buttonWidth, by + buttonHeight,
                    hovered ? 0xFF2B3540 : PANEL_RAISED);
            graphics.fill(x, by, x + 4, by + buttonHeight, accent);
            graphics.fill(x + 12, by + 9, x + 18, by + 15, accent);
            graphics.text(font, labels[index], x + 25, by + 10, TEXT, false);
            graphics.text(font, "›", x + buttonWidth - 16, by + 10,
                    hovered ? 0xFFFFFFFF : MUTED, false);
            clickRegions.add(new ClickRegion(x, by, buttonWidth, buttonHeight, action));
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        Layout layout = layout();
        int closeLeft = layout.left() + layout.width() - 43;
        int closeTop = layout.top() + 17;
        if (click.button() == 0 && isInside(mouseX, mouseY, closeLeft, closeTop, 25, 25)) {
            onClose();
            return true;
        }
        if (click.button() == 0) {
            for (ClickRegion region : clickRegions) {
                if (region.contains(mouseX, mouseY)) {
                    ClientPacketDistributor.sendToServer(
                            new VillageNetwork.VillageUiActionPayload(region.action()));
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (maxScroll > 0
                && mouseX >= bodyLeft - 8
                && mouseX <= bodyRight + 8
                && mouseY >= bodyTop
                && mouseY <= bodyBottom) {
            int delta = (int) Math.round(vertical * 26.0);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(null);
        }
    }

    private List<FormattedCharSequence> bodyLines(int lineWidth) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String paragraph : payload.body().split("\n", -1)) {
            if (paragraph.isBlank()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(font.split(Component.literal(paragraph), lineWidth));
        }
        return lines;
    }

    private int actionAccent(String action) {
        String normalized = action.toLowerCase();
        if (normalized.contains("restart") || normalized.contains("destroy") || normalized.contains("reject")) {
            return DANGER;
        }
        if (normalized.contains("advance") || normalized.contains("ready") || normalized.contains("skill")) {
            return ACCENT;
        }
        if (normalized.contains("repair") || normalized.contains("upgrade") || normalized.contains("buy")) {
            return GOLD;
        }
        return 0xFF7F95B8;
    }

    private Layout layout() {
        int panelWidth = Math.min(670, Math.max(318, width - 34));
        int panelHeight = Math.min(455, Math.max(218, height - 26));
        return new Layout(
                (width - panelWidth) / 2,
                (height - panelHeight) / 2,
                panelWidth,
                panelHeight);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Layout(int left, int top, int width, int height) {
    }

    private record ClickRegion(int x, int y, int width, int height, String action) {
        boolean contains(double mouseX, double mouseY) {
            return isInside(mouseX, mouseY, x, y, width, height);
        }
    }
}
