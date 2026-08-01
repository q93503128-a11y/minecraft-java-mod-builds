package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class VillageUiScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0xB8080B10;
    private static final int SHADOW = 0x99000000;
    private static final int PANEL = 0xFF101820;
    private static final int PANEL_SOFT = 0xFF1B2631;
    private static final int PANEL_RAISED = 0xFF253442;
    private static final int BORDER = 0xFF52677A;
    private static final int ACCENT = 0xFF42D8BC;
    private static final int GOLD = 0xFFFFC85A;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB8C4CF;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;

    private int actionPage;
    private int actionPageCount = 1;
    private int selectedIndex = -1;
    private int scrollOffset;
    private int maxScroll;
    private int bodyLeft;
    private int bodyTop;
    private int bodyRight;
    private int bodyBottom;
    private int detailLeft;
    private int detailTop;
    private int detailRight;
    private int detailBottom;

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
    protected void init() {
        actionPage = Math.max(0, actionPage);
        rebuildActionButtons();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        ContentLayout content = contentLayout(layout);
        renderFrame(graphics, mouseX, mouseY, layout);

        graphics.fill(content.infoLeft(), content.infoTop(),
                content.infoRight(), content.infoBottom(), PANEL_SOFT);
        graphics.fill(content.actionLeft(), content.actionTop(),
                content.actionRight(), content.actionBottom(), PANEL_SOFT);
        if (content.split()) {
            graphics.fill(content.divider(), content.infoTop(),
                    content.divider() + 1, content.infoBottom(), BORDER);
        }

        bodyLeft = content.infoLeft() + 10;
        bodyTop = content.infoTop() + 25;
        bodyRight = content.infoRight() - 10;
        bodyBottom = content.infoBottom() - 10;
        graphics.text(font, "현황", bodyLeft, content.infoTop() + 8, GOLD, false);
        graphics.text(font, actionHeader(),
                content.actionLeft() + 10, content.actionTop() + 8, ACCENT, false);
        renderBody(graphics);
        renderSelectedDetail(graphics);

        if (actions.length == 0) {
            graphics.text(font, "사용 가능한 동작이 없습니다.",
                    content.actionLeft() + 10, content.actionTop() + 34, MUTED, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void rebuildActionButtons() {
        clearWidgets();
        Layout layout = layout();
        ContentLayout content = contentLayout(layout);
        int count = Math.min(actions.length, labels.length);
        int areaWidth = content.actionRight() - content.actionLeft();
        int columns = content.split() && areaWidth >= 255 ? 2 : 1;
        int rows = content.split() ? 2 : 3;
        int pageSize = Math.max(1, columns * rows);
        actionPageCount = Math.max(1, (count + pageSize - 1) / pageSize);
        actionPage = Math.max(0, Math.min(actionPage, actionPageCount - 1));

        int start = actionPage * pageSize;
        int end = Math.min(count, start + pageSize);
        int gap = 6;
        int horizontalPadding = 10;
        int availableWidth = Math.max(80, areaWidth - horizontalPadding * 2);
        int buttonWidth = Math.max(72, (availableWidth - gap * (columns - 1)) / columns);
        int buttonHeight = 22;
        int buttonAreaTop = content.actionTop() + 27;
        int footerHeight = 31;
        int detailHeight = 53;
        int buttonAreaBottom = content.actionBottom() - footerHeight - detailHeight - 5;
        int usedRows = Math.max(1, (end - start + columns - 1) / columns);
        int totalHeight = usedRows * buttonHeight + Math.max(0, usedRows - 1) * gap;
        int startY = buttonAreaTop + Math.max(0, (buttonAreaBottom - buttonAreaTop - totalHeight) / 2);

        for (int index = start; index < end; index++) {
            int local = index - start;
            int column = local % columns;
            int row = local / columns;
            int x = content.actionLeft() + horizontalPadding + column * (buttonWidth + gap);
            int y = startY + row * (buttonHeight + gap);
            String label = compact(labels[index], columns == 2 ? 16 : 29);
            Button actionButton = Button.builder(
                            Component.literal((selectedIndex == index ? "▶ " : "") + label),
                            button -> selectAction(index))
                    .bounds(x, y, buttonWidth, buttonHeight)
                    .build();
            addRenderableWidget(actionButton);
        }

        detailLeft = content.actionLeft() + 10;
        detailRight = content.actionRight() - 10;
        detailBottom = content.actionBottom() - footerHeight - 4;
        detailTop = detailBottom - detailHeight;

        int footerY = content.actionBottom() - 27;
        int executeWidth = Math.min(142, Math.max(92, areaWidth / 2));
        int executeX = content.actionRight() - 10 - executeWidth;
        Button execute = Button.builder(
                        Component.literal(selectedIndex >= 0
                                ? VillageActionDescriptions.executeLabel(actions[selectedIndex])
                                : "동작을 먼저 선택하세요"),
                        button -> executeSelected())
                .bounds(executeX, footerY, executeWidth, 21)
                .build();
        execute.active = selectedIndex >= 0;
        addRenderableWidget(execute);

        if (actionPageCount > 1) {
            int navigationWidth = Math.max(42, Math.min(58, (executeX - content.actionLeft() - 27) / 2));
            Button previous = Button.builder(
                            Component.literal("이전"),
                            button -> changePage(-1))
                    .bounds(content.actionLeft() + 10, footerY, navigationWidth, 21)
                    .build();
            previous.active = actionPage > 0;
            addRenderableWidget(previous);

            Button next = Button.builder(
                            Component.literal("다음"),
                            button -> changePage(1))
                    .bounds(content.actionLeft() + 16 + navigationWidth, footerY, navigationWidth, 21)
                    .build();
            next.active = actionPage + 1 < actionPageCount;
            addRenderableWidget(next);
        }
    }

    private void selectAction(int index) {
        if (index < 0 || index >= Math.min(actions.length, labels.length)) {
            return;
        }
        selectedIndex = index;
        rebuildActionButtons();
    }

    private void changePage(int delta) {
        int next = Math.max(0, Math.min(actionPageCount - 1, actionPage + delta));
        if (next == actionPage) {
            return;
        }
        actionPage = next;
        selectedIndex = -1;
        rebuildActionButtons();
    }

    private void executeSelected() {
        if (selectedIndex < 0 || selectedIndex >= Math.min(actions.length, labels.length)) {
            return;
        }
        String action = actions[selectedIndex];
        String label = labels[selectedIndex];
        String detail = VillageActionDescriptions.describe(action, label);
        if (VillageActionDescriptions.requiresConfirmation(action)) {
            if (minecraft != null) {
                minecraft.gui.setScreen(new VillageConfirmScreen(this, action, label, detail));
            }
            return;
        }
        ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        onClose();
    }

    private void renderFrame(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            Layout layout) {
        int left = layout.left();
        int top = layout.top();
        int panelWidth = layout.width();
        int panelHeight = layout.height();
        graphics.fill(left + 5, top + 6, left + panelWidth + 5, top + panelHeight + 6, SHADOW);
        graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, BORDER);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left, top, left + 4, top + panelHeight, ACCENT);

        graphics.fill(left + 14, top + 11, left + 43, top + 40, 0xFF263544);
        graphics.fill(left + 14, top + 11, left + 18, top + 40, GOLD);
        graphics.centeredText(font, "VG", left + 29, top + 21, TEXT);
        graphics.text(font, payload.title(), left + 54, top + 14, TEXT, false);
        graphics.text(font, subtitle(), left + 54, top + 29, MUTED, false);

        int closeLeft = left + panelWidth - 35;
        int closeTop = top + 12;
        boolean closeHovered = isInside(mouseX, mouseY, closeLeft, closeTop, 23, 23);
        graphics.fill(closeLeft - 1, closeTop - 1, closeLeft + 24, closeTop + 24,
                closeHovered ? 0xFFE06A72 : BORDER);
        graphics.fill(closeLeft, closeTop, closeLeft + 23, closeTop + 23,
                closeHovered ? 0xFF743840 : 0xFF263544);
        graphics.centeredText(font, "×", closeLeft + 11, closeTop + 7,
                closeHovered ? 0xFFFFFFFF : MUTED);
    }

    private void renderBody(GuiGraphicsExtractor graphics) {
        List<FormattedCharSequence> lines = bodyLines(Math.max(80, bodyRight - bodyLeft));
        int lineHeight = 12;
        int contentHeight = lines.size() * lineHeight;
        int visibleHeight = Math.max(1, bodyBottom - bodyTop);
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        graphics.enableScissor(bodyLeft - 1, bodyTop, bodyRight + 1, bodyBottom);
        int y = bodyTop - scrollOffset;
        for (FormattedCharSequence line : lines) {
            if (y + 10 >= bodyTop && y <= bodyBottom) {
                graphics.text(font, line, bodyLeft, y, TEXT, false);
            }
            y += lineHeight;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackHeight = Math.max(1, bodyBottom - bodyTop);
            int thumbHeight = Math.max(12, trackHeight * visibleHeight / Math.max(visibleHeight, contentHeight));
            int thumbTravel = Math.max(0, trackHeight - thumbHeight);
            int thumbTop = bodyTop + thumbTravel * scrollOffset / maxScroll;
            graphics.fill(bodyRight + 3, bodyTop, bodyRight + 6, bodyBottom, 0xFF080B0F);
            graphics.fill(bodyRight + 3, thumbTop, bodyRight + 6, thumbTop + thumbHeight, ACCENT);
        }
    }

    private void renderSelectedDetail(GuiGraphicsExtractor graphics) {
        if (detailRight <= detailLeft || detailBottom <= detailTop) {
            return;
        }
        graphics.fill(detailLeft - 1, detailTop - 1, detailRight + 1, detailBottom + 1, BORDER);
        graphics.fill(detailLeft, detailTop, detailRight, detailBottom, PANEL_RAISED);
        String detail = selectedIndex < 0
                ? "동작을 선택하면 효과·비용·주의사항이 여기에 표시됩니다."
                : VillageActionDescriptions.describe(actions[selectedIndex], labels[selectedIndex]);
        List<FormattedCharSequence> lines = font.split(
                Component.literal(detail), Math.max(80, detailRight - detailLeft - 16));
        int y = detailTop + 8;
        for (FormattedCharSequence line : lines) {
            if (y > detailBottom - 11) {
                break;
            }
            graphics.text(font, line, detailLeft + 8, y, selectedIndex < 0 ? MUTED : TEXT, false);
            y += 11;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        Layout layout = layout();
        int closeLeft = layout.left() + layout.width() - 35;
        int closeTop = layout.top() + 12;
        if (click.button() == 0 && isInside(click.x(), click.y(), closeLeft, closeTop, 23, 23)) {
            onClose();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (maxScroll > 0
                && mouseX >= bodyLeft - 4
                && mouseX <= bodyRight + 8
                && mouseY >= bodyTop
                && mouseY <= bodyBottom) {
            int delta = (int) Math.round(vertical * 24.0);
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

    private String actionHeader() {
        return actionPageCount > 1
                ? "동작 선택 · " + (actionPage + 1) + "/" + actionPageCount
                : "동작 선택";
    }

    private String subtitle() {
        return switch (payload.screenId()) {
            case "status", "role_preview" -> "수호자 성장과 역할";
            case "building" -> "시설 현장 기능";
            case "management" -> "시설 수리와 강화";
            case "quick_chat" -> "팀 빠른 신호";
            case "vote" -> "멀티플레이 투표";
            case "game_over" -> "방어 실패";
            case "victory" -> "방어 성공";
            default -> "마을 지휘실";
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

    private String compact(String value, int maxCharacters) {
        if (value.length() <= maxCharacters) {
            return value;
        }
        return value.substring(0, Math.max(1, maxCharacters - 1)) + "…";
    }

    private Layout layout() {
        int panelWidth = Math.min(610, Math.max(300, width - 16));
        int panelHeight = Math.min(390, Math.max(226, height - 12));
        return new Layout(
                (width - panelWidth) / 2,
                (height - panelHeight) / 2,
                panelWidth,
                panelHeight);
    }

    private ContentLayout contentLayout(Layout layout) {
        int contentTop = layout.top() + 51;
        int contentBottom = layout.top() + layout.height() - 14;
        int left = layout.left() + 15;
        int right = layout.left() + layout.width() - 14;
        boolean split = layout.width() >= 430;
        if (split) {
            int divider = layout.left() + Math.max(185, layout.width() * 40 / 100);
            return new ContentLayout(
                    left, divider - 8, contentTop, contentBottom,
                    divider + 2, right, contentTop, contentBottom,
                    divider - 3, true);
        }
        int infoBottom = Math.min(contentBottom - 164, contentTop + 96);
        int actionTop = infoBottom + 6;
        return new ContentLayout(
                left, right, contentTop, infoBottom,
                left, right, actionTop, contentBottom,
                0, false);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Layout(int left, int top, int width, int height) {
    }

    private record ContentLayout(
            int infoLeft,
            int infoRight,
            int infoTop,
            int infoBottom,
            int actionLeft,
            int actionRight,
            int actionTop,
            int actionBottom,
            int divider,
            boolean split) {
    }
}
