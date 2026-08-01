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
    private static final int OVERLAY = 0xC705080D;
    private static final int PANEL = 0xFF0C1219;
    private static final int SURFACE = 0xFF131E28;
    private static final int SURFACE_HOVER = 0xFF1C2B38;
    private static final int BORDER = 0xFF405568;
    private static final int TEXT = 0xFFF3F7FA;
    private static final int MUTED = 0xFFA6B4C0;
    private static final int ACCENT = 0xFF43D6BC;
    private static final int GOLD = 0xFFF1C35D;
    private static final int RED = 0xFFE06A72;
    private static final int BLUE = 0xFF7FA9EA;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private int selectedIndex = -1;
    private int bodyScroll;
    private int bodyMaxScroll;

    public VillageUiScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        this.actions = payload.actions().isBlank()
                ? new String[0] : payload.actions().split(SEP, -1);
        this.labels = payload.labels().isBlank()
                ? new String[0] : payload.labels().split(SEP, -1);
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
        int right = layout.left() + layout.width();
        int bottom = layout.top() + layout.height();

        graphics.fill(layout.left() - 2, layout.top() - 2, right + 2, bottom + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), right, bottom, PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, bottom, accentForScreen());

        graphics.text(font, payload.title(), layout.left() + 18, layout.top() + 14, TEXT, false);
        graphics.text(font, subtitle(), layout.left() + 18, layout.top() + 30, MUTED, false);

        int closeX = right - 34;
        int closeY = layout.top() + 11;
        boolean closeHovered = inside(mouseX, mouseY, closeX, closeY, 22, 22);
        graphics.fill(closeX, closeY, closeX + 22, closeY + 22,
                closeHovered ? 0xFF6E3038 : SURFACE_HOVER);
        graphics.centeredText(font, "×", closeX + 11, closeY + 7,
                closeHovered ? 0xFFFFFFFF : MUTED);

        Areas areas = areas(layout);
        renderBody(graphics, areas);
        renderActions(graphics, mouseX, mouseY, areas);
        renderFooter(graphics, mouseX, mouseY, areas);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderBody(GuiGraphicsExtractor graphics, Areas areas) {
        graphics.fill(areas.bodyLeft(), areas.bodyTop(),
                areas.bodyRight(), areas.bodyBottom(), SURFACE);
        graphics.text(font, bodyHeading(), areas.bodyLeft() + 11,
                areas.bodyTop() + 9, accentForScreen(), false);

        int textLeft = areas.bodyLeft() + 11;
        int textTop = areas.bodyTop() + 27;
        int textRight = areas.bodyRight() - 11;
        int textBottom = areas.bodyBottom() - 9;
        List<FormattedCharSequence> lines = bodyLines(Math.max(90, textRight - textLeft));
        int contentHeight = lines.size() * 11;
        int visibleHeight = Math.max(1, textBottom - textTop);
        bodyMaxScroll = Math.max(0, contentHeight - visibleHeight);
        bodyScroll = Math.max(0, Math.min(bodyScroll, bodyMaxScroll));

        graphics.enableScissor(textLeft - 1, textTop, textRight + 1, textBottom);
        int y = textTop - bodyScroll;
        for (FormattedCharSequence line : lines) {
            if (y + 9 >= textTop && y <= textBottom) {
                graphics.text(font, line, textLeft, y, TEXT, false);
            }
            y += 11;
        }
        graphics.disableScissor();

        if (bodyMaxScroll > 0) {
            int trackX = areas.bodyRight() - 5;
            int trackHeight = textBottom - textTop;
            int thumbHeight = Math.max(12,
                    trackHeight * visibleHeight / Math.max(visibleHeight, contentHeight));
            int travel = Math.max(0, trackHeight - thumbHeight);
            int thumbY = textTop + travel * bodyScroll / bodyMaxScroll;
            graphics.fill(trackX, textTop, trackX + 2, textBottom, 0xFF060A0F);
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, ACCENT);
        }
    }

    private void renderActions(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            Areas areas) {
        graphics.fill(areas.actionLeft(), areas.actionTop(),
                areas.actionRight(), areas.actionBottom(), SURFACE);

        List<ActionRegion> regions = actionRegions(areas);
        for (ActionRegion region : regions) {
            String action = actions[region.index()];
            String label = labels[region.index()];
            boolean hovered = inside(mouseX, mouseY,
                    region.x(), region.y(), region.width(), region.height());
            boolean selected = selectedIndex == region.index();
            int color = actionColor(action);

            graphics.fill(region.x() - 1, region.y() - 1,
                    region.x() + region.width() + 1,
                    region.y() + region.height() + 1,
                    selected ? color : hovered ? BORDER : 0xFF283947);
            graphics.fill(region.x(), region.y(),
                    region.x() + region.width(),
                    region.y() + region.height(),
                    hovered || selected ? SURFACE_HOVER : 0xFF101923);
            graphics.fill(region.x(), region.y(),
                    region.x() + 4, region.y() + region.height(), color);

            int iconX = region.x() + 11;
            int iconY = region.y() + (region.height() - 24) / 2;
            drawActionIcon(graphics, action, iconX, iconY, 24, color);

            int textX = iconX + 34;
            List<FormattedCharSequence> lines = font.split(
                    Component.literal(label),
                    Math.max(55, region.x() + region.width() - textX - 12));
            int y = region.y() + 10;
            for (FormattedCharSequence line : lines) {
                if (y > region.y() + region.height() - 11) {
                    break;
                }
                graphics.text(font, line, textX, y, TEXT, false);
                y += 11;
            }
        }
    }

    private void renderFooter(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            Areas areas) {
        graphics.fill(areas.footerLeft(), areas.footerTop(),
                areas.footerRight(), areas.footerBottom(), 0xFF0F1821);

        String title = selectedIndex < 0
                ? "항목을 선택하세요"
                : labels[selectedIndex];
        String detail = selectedIndex < 0
                ? "비용이 드는 동작은 항목 선택 후 아래 실행 버튼을 눌러야 적용됩니다."
                : VillageActionDescriptions.describe(actions[selectedIndex], labels[selectedIndex]);
        int titleColor = selectedIndex < 0 ? MUTED : actionColor(actions[selectedIndex]);
        graphics.text(font, compact(title, Math.max(18, areas.footerWidth() / 10)),
                areas.footerLeft() + 11, areas.footerTop() + 9, titleColor, false);

        int executeWidth = Math.min(132, Math.max(92, areas.footerWidth() / 4));
        int executeX = areas.footerRight() - executeWidth - 10;
        int executeY = areas.footerBottom() - 29;
        int detailRight = executeX - 10;
        List<FormattedCharSequence> lines = font.split(
                Component.literal(detail),
                Math.max(80, detailRight - areas.footerLeft() - 22));
        int y = areas.footerTop() + 25;
        for (FormattedCharSequence line : lines) {
            if (y > areas.footerBottom() - 10) {
                break;
            }
            graphics.text(font, line, areas.footerLeft() + 11, y, MUTED, false);
            y += 11;
        }

        boolean active = selectedIndex >= 0;
        boolean hovered = active && inside(mouseX, mouseY,
                executeX, executeY, executeWidth, 21);
        graphics.fill(executeX - 1, executeY - 1,
                executeX + executeWidth + 1, executeY + 22,
                hovered ? GOLD : active ? actionColor(actions[selectedIndex]) : 0xFF35414C);
        graphics.fill(executeX, executeY,
                executeX + executeWidth, executeY + 21,
                hovered ? 0xFF3C3421 : active ? SURFACE_HOVER : 0xFF171D24);
        String executeLabel = active
                ? VillageActionDescriptions.executeLabel(actions[selectedIndex])
                : "선택 필요";
        graphics.centeredText(font, executeLabel,
                executeX + executeWidth / 2, executeY + 7,
                active ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        Layout layout = layout();
        int right = layout.left() + layout.width();
        if (click.button() == 0 && inside(click.x(), click.y(),
                right - 34, layout.top() + 11, 22, 22)) {
            onClose();
            return true;
        }
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        Areas areas = areas(layout);
        for (ActionRegion region : actionRegions(areas)) {
            if (!inside(click.x(), click.y(),
                    region.x(), region.y(), region.width(), region.height())) {
                continue;
            }
            String action = actions[region.index()];
            if (isImmediate(action)) {
                execute(action);
            } else {
                selectedIndex = region.index();
            }
            return true;
        }

        if (selectedIndex >= 0) {
            int executeWidth = Math.min(132, Math.max(92, areas.footerWidth() / 4));
            int executeX = areas.footerRight() - executeWidth - 10;
            int executeY = areas.footerBottom() - 29;
            if (inside(click.x(), click.y(), executeX, executeY, executeWidth, 21)) {
                execute(actions[selectedIndex]);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Areas areas = areas(layout());
        if (bodyMaxScroll > 0
                && mouseX >= areas.bodyLeft()
                && mouseX <= areas.bodyRight()
                && mouseY >= areas.bodyTop()
                && mouseY <= areas.bodyBottom()) {
            bodyScroll = Math.max(0, Math.min(bodyMaxScroll,
                    bodyScroll - (int) Math.round(vertical * 24.0)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void execute(String action) {
        ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        if (isTerminalAction(action)) {
            onClose();
        }
    }

    private boolean isImmediate(String action) {
        return action.startsWith("open_")
                || action.startsWith("manage:")
                || action.startsWith("building:")
                || action.equals("defense_status")
                || action.equals("vote_yes")
                || action.equals("vote_no")
                || action.startsWith("chat_")
                || action.equals("claim_bread")
                || action.equals("use_infirmary");
    }

    private boolean isTerminalAction(String action) {
        return action.equals("return_village")
                || action.startsWith("restart_")
                || action.startsWith("chat_")
                || action.equals("vote_yes")
                || action.equals("vote_no");
    }

    private List<ActionRegion> actionRegions(Areas areas) {
        int count = Math.min(actions.length, labels.length);
        List<ActionRegion> result = new ArrayList<>(count);
        if (count == 0) {
            return result;
        }
        int columns = areas.actionWidth() >= 250 ? 2 : 1;
        int rows = (count + columns - 1) / columns;
        int gap = 7;
        int padding = 10;
        int width = Math.max(100,
                (areas.actionWidth() - padding * 2 - gap * (columns - 1)) / columns);
        int availableHeight = areas.actionHeight() - padding * 2 - gap * Math.max(0, rows - 1);
        int height = Math.max(30, Math.min(52, availableHeight / Math.max(1, rows)));
        for (int index = 0; index < count; index++) {
            int x = areas.actionLeft() + padding
                    + (index % columns) * (width + gap);
            int y = areas.actionTop() + padding
                    + (index / columns) * (height + gap);
            result.add(new ActionRegion(index, x, y, width, height));
        }
        return result;
    }

    private List<FormattedCharSequence> bodyLines(int width) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String paragraph : payload.body().split("\n", -1)) {
            if (paragraph.isBlank()) {
                lines.add(FormattedCharSequence.EMPTY);
            } else {
                lines.addAll(font.split(Component.literal(paragraph), width));
            }
        }
        return lines;
    }

    private void drawActionIcon(
            GuiGraphicsExtractor graphics,
            String action,
            int x,
            int y,
            int size,
            int color) {
        graphics.fill(x, y, x + size, y + size, 0xFF080E14);
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        if (action.startsWith("repair:")) {
            graphics.fill(centerX - 2, y + 4, centerX + 2, y + size - 4, color);
            graphics.fill(x + 5, centerY - 2, x + size - 5, centerY + 2, color);
        } else if (action.startsWith("upgrade:") || action.equals("forge_upgrade")
                || action.equals("skill_learn")) {
            graphics.fill(centerX - 2, y + 5, centerX + 2, y + size - 5, color);
            graphics.fill(centerX - 7, y + 7, centerX + 7, y + 11, color);
            graphics.fill(centerX - 7, y + 7, centerX - 3, y + 15, color);
            graphics.fill(centerX + 3, y + 7, centerX + 7, y + 15, color);
        } else if (action.startsWith("buy_") || action.equals("sell_loot")) {
            graphics.fill(x + 5, y + 8, x + size - 5, y + size - 6, color);
            graphics.fill(x + 8, y + 4, x + size - 8, y + 9, color);
            graphics.fill(x + 8, y + size - 5, x + 11, y + size - 2, color);
            graphics.fill(x + size - 11, y + size - 5, x + size - 8, y + size - 2, color);
        } else if (action.equals("return_village")) {
            graphics.fill(centerX - 2, y + 5, centerX + 2, y + size - 5, color);
            graphics.fill(x + 6, y + 7, centerX, y + 11, color);
            graphics.fill(x + 6, y + 7, x + 10, centerY, color);
        } else if (action.startsWith("chat_")) {
            graphics.fill(x + 4, y + 5, x + size - 4, y + size - 7, color);
            graphics.fill(x + 7, y + size - 7, x + 11, y + size - 3, color);
        } else if (action.equals("train") || action.equals("hire_mercenary")) {
            graphics.fill(centerX - 2, y + 4, centerX + 2, y + size - 4, color);
            graphics.fill(centerX - 7, y + 5, centerX + 7, y + 9, color);
            graphics.fill(centerX - 6, y + size - 9, centerX + 6, y + size - 5, color);
        } else {
            graphics.fill(centerX - 5, centerY - 5, centerX + 5, centerY + 5, color);
        }
    }

    private int actionColor(String action) {
        if (action.startsWith("repair:") || action.equals("use_infirmary")) {
            return ACCENT;
        }
        if (action.startsWith("upgrade:") || action.equals("forge_upgrade")
                || action.equals("skill_learn")) {
            return GOLD;
        }
        if (action.startsWith("restart_")) {
            return RED;
        }
        if (action.startsWith("chat_") || action.equals("defense_status")) {
            return BLUE;
        }
        return accentForScreen();
    }

    private int accentForScreen() {
        return switch (payload.screenId()) {
            case "game_over" -> RED;
            case "vote" -> GOLD;
            case "quick_chat" -> BLUE;
            case "management" -> ACCENT;
            case "building" -> GOLD;
            default -> ACCENT;
        };
    }

    private String bodyHeading() {
        return switch (payload.screenId()) {
            case "management" -> "시설 상태";
            case "building" -> "현장 기능";
            case "vote" -> "투표 내용";
            case "quick_chat" -> "수호단 통신";
            case "game_over" -> "방어 실패";
            case "victory" -> "정비 보고";
            default -> "정보";
        };
    }

    private String subtitle() {
        return switch (payload.screenId()) {
            case "management" -> "회관 시설 관리";
            case "building" -> "시설 전용 기능";
            case "vote" -> "마을 시간 진행";
            case "quick_chat" -> "빠른 신호";
            case "game_over" -> "재시작 선택";
            case "victory" -> "방어 성공";
            case "inventory_actions" -> "수호자 메뉴";
            default -> "마을 시스템";
        };
    }

    private String compact(String value, int maxCharacters) {
        String normalized = value.replace('\n', ' ');
        if (normalized.length() <= maxCharacters) {
            return normalized;
        }
        return normalized.substring(0, Math.max(1, maxCharacters - 1)) + "…";
    }

    private Layout layout() {
        int panelWidth = Math.min(700, Math.max(260, width - 18));
        int panelHeight = Math.min(460, Math.max(210, height - 14));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2,
                panelWidth, panelHeight);
    }

    private Areas areas(Layout layout) {
        int left = layout.left() + 14;
        int right = layout.left() + layout.width() - 14;
        int top = layout.top() + 50;
        int bottom = layout.top() + layout.height() - 14;
        int footerHeight = Math.max(62, Math.min(82, layout.height() / 5));
        int footerTop = bottom - footerHeight;
        int bodyHeight = Math.max(68, Math.min(108, layout.height() / 4));
        int bodyBottom = top + bodyHeight;
        int actionTop = bodyBottom + 8;
        int actionBottom = footerTop - 8;
        return new Areas(
                left, top, right, bodyBottom,
                left, actionTop, right, actionBottom,
                left, footerTop, right, bottom);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(null);
        }
    }

    private static boolean inside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    private record Layout(int left, int top, int width, int height) {
    }

    private record Areas(
            int bodyLeft,
            int bodyTop,
            int bodyRight,
            int bodyBottom,
            int actionLeft,
            int actionTop,
            int actionRight,
            int actionBottom,
            int footerLeft,
            int footerTop,
            int footerRight,
            int footerBottom) {
        int actionWidth() {
            return actionRight - actionLeft;
        }

        int actionHeight() {
            return actionBottom - actionTop;
        }

        int footerWidth() {
            return footerRight - footerLeft;
        }
    }

    private record ActionRegion(int index, int x, int y, int width, int height) {
    }
}
