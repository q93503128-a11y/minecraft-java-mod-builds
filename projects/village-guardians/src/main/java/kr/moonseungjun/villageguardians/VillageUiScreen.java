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
    private static final int OVERLAY = 0xC9080B10;
    private static final int SHADOW = 0x99000000;
    private static final int PANEL = 0xFF0F141B;
    private static final int PANEL_SOFT = 0xFF171E27;
    private static final int PANEL_RAISED = 0xFF202A35;
    private static final int PANEL_HOVER = 0xFF293744;
    private static final int BORDER = 0xFF344250;
    private static final int ACCENT = 0xFF3ED0B4;
    private static final int ACCENT_DARK = 0xFF1E776B;
    private static final int GOLD = 0xFFF1BC57;
    private static final int DANGER = 0xFFDC6570;
    private static final int TEXT = 0xFFF3F6F8;
    private static final int MUTED = 0xFF94A0AD;

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
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        int left = layout.left();
        int top = layout.top();
        int panelWidth = layout.width();
        int panelHeight = layout.height();

        graphics.fill(left + 5, top + 6, left + panelWidth + 5, top + panelHeight + 6, SHADOW);
        graphics.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, BORDER);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left, top, left + 3, top + panelHeight, ACCENT);

        graphics.fill(left + 12, top + 10, left + 38, top + 36, PANEL_RAISED);
        graphics.fill(left + 12, top + 10, left + 15, top + 36, GOLD);
        graphics.centeredText(font, "VG", left + 25, top + 19, TEXT);
        graphics.text(font, payload.title(), left + 48, top + 13, TEXT, false);
        graphics.text(font, subtitle(), left + 48, top + 26, MUTED, false);

        int closeLeft = left + panelWidth - 31;
        int closeTop = top + 10;
        boolean closeHovered = isInside(mouseX, mouseY, closeLeft, closeTop, 20, 20);
        graphics.fill(closeLeft, closeTop, closeLeft + 20, closeTop + 20,
                closeHovered ? 0xFF76343B : PANEL_RAISED);
        graphics.centeredText(font, "×", closeLeft + 10, closeTop + 6,
                closeHovered ? 0xFFFFFFFF : MUTED);

        int contentTop = top + 46;
        int contentBottom = top + panelHeight - 12;
        int innerLeft = left + 13;
        int innerRight = left + panelWidth - 12;
        boolean split = panelWidth >= 390;

        int actionLeft;
        int actionRight;
        if (split) {
            int divider = left + Math.max(170, panelWidth * 43 / 100);
            bodyLeft = innerLeft + 8;
            bodyTop = contentTop + 19;
            bodyRight = divider - 14;
            bodyBottom = contentBottom - 8;
            actionLeft = divider + 8;
            actionRight = innerRight;

            graphics.fill(innerLeft, contentTop, divider - 6, contentBottom, PANEL_SOFT);
            graphics.fill(divider + 1, contentTop, innerRight, contentBottom, PANEL_SOFT);
            graphics.fill(divider - 2, contentTop, divider - 1, contentBottom, BORDER);
            graphics.text(font, "현황", bodyLeft, contentTop + 7, GOLD, false);
            graphics.text(font, "명령", actionLeft + 8, contentTop + 7, ACCENT, false);
        } else {
            int infoBottom = Math.min(contentBottom - 72, contentTop + 76);
            bodyLeft = innerLeft + 8;
            bodyTop = contentTop + 18;
            bodyRight = innerRight - 8;
            bodyBottom = infoBottom - 6;
            actionLeft = innerLeft;
            actionRight = innerRight;

            graphics.fill(innerLeft, contentTop, innerRight, infoBottom, PANEL_SOFT);
            graphics.fill(innerLeft, infoBottom + 5, innerRight, contentBottom, PANEL_SOFT);
            graphics.text(font, "현황", bodyLeft, contentTop + 6, GOLD, false);
            graphics.text(font, "명령", actionLeft + 8, infoBottom + 12, ACCENT, false);
        }

        renderBody(graphics, panelWidth);
        renderActions(graphics, mouseX, mouseY, actionLeft, actionRight,
                split ? contentTop + 24 : bodyBottom + 32,
                contentBottom - 7);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderBody(GuiGraphicsExtractor graphics, int panelWidth) {
        List<FormattedCharSequence> lines = bodyLines(Math.max(90, bodyRight - bodyLeft));
        int lineHeight = 11;
        int contentHeight = lines.size() * lineHeight;
        int visibleHeight = Math.max(1, bodyBottom - bodyTop);
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        graphics.enableScissor(bodyLeft - 1, bodyTop, bodyRight + 1, bodyBottom);
        int y = bodyTop - scrollOffset;
        for (FormattedCharSequence line : lines) {
            if (y + 9 >= bodyTop && y <= bodyBottom) {
                graphics.text(font, line, bodyLeft, y, TEXT, false);
            }
            y += lineHeight;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackHeight = Math.max(1, bodyBottom - bodyTop);
            int thumbHeight = Math.max(10, trackHeight * visibleHeight / Math.max(visibleHeight, contentHeight));
            int thumbTravel = Math.max(0, trackHeight - thumbHeight);
            int thumbTop = bodyTop + (maxScroll == 0 ? 0 : thumbTravel * scrollOffset / maxScroll);
            graphics.fill(bodyRight + 3, bodyTop, bodyRight + 5, bodyBottom, 0xFF080B0F);
            graphics.fill(bodyRight + 3, thumbTop, bodyRight + 5, thumbTop + thumbHeight, ACCENT);
        }
    }

    private void renderActions(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int actionLeft,
            int actionRight,
            int actionTop,
            int actionBottom) {
        clickRegions.clear();
        int count = Math.min(actions.length, labels.length);
        if (count == 0) {
            graphics.text(font, "사용 가능한 명령이 없습니다.", actionLeft + 8, actionTop + 8, MUTED, false);
            return;
        }

        int availableWidth = Math.max(80, actionRight - actionLeft - 14);
        int columns = availableWidth >= 210 && count >= 4 ? 2 : 1;
        int gap = 5;
        int buttonWidth = (availableWidth - (columns - 1) * gap) / columns;
        int buttonHeight = 20;
        int rows = (count + columns - 1) / columns;
        int totalHeight = rows * buttonHeight + Math.max(0, rows - 1) * gap;
        int startY = actionTop + Math.max(0, (actionBottom - actionTop - totalHeight) / 2);

        for (int index = 0; index < count; index++) {
            int column = index % columns;
            int row = index / columns;
            int x = actionLeft + 7 + column * (buttonWidth + gap);
            int y = startY + row * (buttonHeight + gap);
            String action = actions[index];
            int accent = actionAccent(action);
            boolean hovered = isInside(mouseX, mouseY, x, y, buttonWidth, buttonHeight);

            graphics.fill(x + 2, y + 2, x + buttonWidth + 2, y + buttonHeight + 2, 0x55000000);
            graphics.fill(x - 1, y - 1, x + buttonWidth + 1, y + buttonHeight + 1,
                    hovered ? accent : BORDER);
            graphics.fill(x, y, x + buttonWidth, y + buttonHeight,
                    hovered ? PANEL_HOVER : PANEL_RAISED);
            graphics.fill(x, y, x + 3, y + buttonHeight, accent);
            graphics.text(font, compact(labels[index], columns == 2 ? 13 : 24), x + 9, y + 6, TEXT, false);
            graphics.text(font, ">", x + buttonWidth - 10, y + 6,
                    hovered ? GOLD : MUTED, false);
            clickRegions.add(new ClickRegion(x, y, buttonWidth, buttonHeight, action));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        Layout layout = layout();
        int closeLeft = layout.left() + layout.width() - 31;
        int closeTop = layout.top() + 10;
        if (click.button() == 0 && isInside(mouseX, mouseY, closeLeft, closeTop, 20, 20)) {
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
                && mouseX >= bodyLeft - 4
                && mouseX <= bodyRight + 6
                && mouseY >= bodyTop
                && mouseY <= bodyBottom) {
            int delta = (int) Math.round(vertical * 22.0);
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

    private String subtitle() {
        return switch (payload.screenId()) {
            case "status" -> "CHARACTER & ROLE";
            case "building" -> "FACILITY CONTROL";
            case "vote" -> "MULTIPLAYER VOTE";
            case "game_over" -> "DEFENCE FAILED";
            default -> "VILLAGE COMMAND";
        };
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
        return 0xFF86A8E8;
    }

    private String compact(String value, int maxCharacters) {
        if (value.length() <= maxCharacters) {
            return value;
        }
        return value.substring(0, Math.max(1, maxCharacters - 1)) + "…";
    }

    private Layout layout() {
        int panelWidth = Math.min(530, Math.max(300, width - 18));
        int panelHeight = Math.min(330, Math.max(188, height - 14));
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
