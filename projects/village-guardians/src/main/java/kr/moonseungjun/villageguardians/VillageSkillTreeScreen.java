package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class VillageSkillTreeScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0xC9080B10;
    private static final int PANEL = 0xFF0F141B;
    private static final int PANEL_SOFT = 0xFF171E27;
    private static final int PANEL_RAISED = 0xFF202A35;
    private static final int PANEL_HOVER = 0xFF293744;
    private static final int BORDER = 0xFF344250;
    private static final int ACCENT = 0xFF3ED0B4;
    private static final int GOLD = 0xFFF1BC57;
    private static final int TEXT = 0xFFF3F6F8;
    private static final int MUTED = 0xFF94A0AD;
    private static final int DANGER = 0xFFDC6570;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private final List<ClickRegion> clickRegions = new ArrayList<>();

    private int scrollOffset;
    private int maxScroll;
    private int viewportLeft;
    private int viewportTop;
    private int viewportRight;
    private int viewportBottom;
    private String hoveredDescription = "노드에 마우스를 올리면 효과와 상태를 확인할 수 있습니다.";

    public VillageSkillTreeScreen(VillageNetwork.OpenVillageUiPayload payload) {
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
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, BORDER);
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, left + 3, bottom, ACCENT);
        graphics.text(font, payload.title(), left + 14, top + 12, TEXT, false);
        graphics.text(font, compact(payload.body(), Math.max(24, panelWidth / 7)), left + 14, top + 27, MUTED, false);

        int closeLeft = right - 29;
        int closeTop = top + 9;
        boolean closeHovered = inside(mouseX, mouseY, closeLeft, closeTop, 20, 20);
        graphics.fill(closeLeft, closeTop, closeLeft + 20, closeTop + 20,
                closeHovered ? 0xFF76343B : PANEL_RAISED);
        graphics.centeredText(font, "×", closeLeft + 10, closeTop + 6,
                closeHovered ? 0xFFFFFFFF : MUTED);

        viewportLeft = left + 12;
        viewportTop = top + 45;
        viewportRight = right - 12;
        viewportBottom = bottom - 46;
        graphics.fill(viewportLeft, viewportTop, viewportRight, viewportBottom, PANEL_SOFT);

        clickRegions.clear();
        hoveredDescription = "노드에 마우스를 올리면 효과와 상태를 확인할 수 있습니다.";
        int count = Math.min(actions.length, labels.length);
        boolean wide = panelWidth >= 460;
        int columns = wide ? 3 : 1;
        int gap = 8;
        int innerWidth = viewportRight - viewportLeft - 18;
        int nodeWidth = Math.max(150, (innerWidth - gap * (columns - 1)) / columns);
        int nodeHeight = 48;
        int rows = (count + columns - 1) / columns;
        int totalHeight = rows * nodeHeight + Math.max(0, rows - 1) * gap + 12;
        int visibleHeight = Math.max(1, viewportBottom - viewportTop);
        maxScroll = Math.max(0, totalHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        graphics.enableScissor(viewportLeft, viewportTop, viewportRight, viewportBottom);
        for (int index = 0; index < count; index++) {
            int column = index % columns;
            int row = index / columns;
            int x = viewportLeft + 9 + column * (nodeWidth + gap);
            int y = viewportTop + 7 + row * (nodeHeight + gap) - scrollOffset;
            if (y + nodeHeight < viewportTop || y > viewportBottom) {
                continue;
            }
            String[] parts = labels[index].split("\\|", 3);
            String title = parts.length > 0 ? parts[0] : labels[index];
            String description = parts.length > 1 ? parts[1] : "";
            String status = parts.length > 2 ? parts[2] : "";
            int accent = statusColor(status);
            boolean hovered = inside(mouseX, mouseY, x, y, nodeWidth, nodeHeight);

            graphics.fill(x - 1, y - 1, x + nodeWidth + 1, y + nodeHeight + 1,
                    hovered ? accent : BORDER);
            graphics.fill(x, y, x + nodeWidth, y + nodeHeight,
                    hovered ? PANEL_HOVER : PANEL_RAISED);
            graphics.fill(x, y, x + 4, y + nodeHeight, accent);
            graphics.text(font, compact(title, Math.max(12, nodeWidth / 7)), x + 10, y + 8, TEXT, false);
            graphics.text(font, compact(description, Math.max(15, nodeWidth / 6)), x + 10, y + 22, MUTED, false);
            graphics.text(font, status, x + 10, y + 35, accent, false);

            if (hovered) {
                hoveredDescription = title + " · " + description + " · " + status;
            }
            if ("습득 가능".equals(status)) {
                clickRegions.add(new ClickRegion(x, y, nodeWidth, nodeHeight, actions[index]));
            }
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackHeight = viewportBottom - viewportTop;
            int thumbHeight = Math.max(12, trackHeight * trackHeight / Math.max(trackHeight, totalHeight));
            int travel = Math.max(0, trackHeight - thumbHeight);
            int thumbTop = viewportTop + travel * scrollOffset / maxScroll;
            graphics.fill(viewportRight - 4, viewportTop + 2, viewportRight - 2, viewportBottom - 2, 0xFF080B0F);
            graphics.fill(viewportRight - 4, thumbTop, viewportRight - 2, thumbTop + thumbHeight, ACCENT);
        }

        graphics.fill(left + 12, bottom - 38, right - 12, bottom - 10, PANEL_RAISED);
        graphics.text(font, compact(hoveredDescription, Math.max(28, panelWidth / 6)),
                left + 20, bottom - 29, TEXT, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        Layout layout = layout();
        int closeLeft = layout.left() + layout.width() - 29;
        int closeTop = layout.top() + 9;
        if (click.button() == 0 && inside(click.x(), click.y(), closeLeft, closeTop, 20, 20)) {
            onClose();
            return true;
        }
        if (click.button() == 0) {
            for (ClickRegion region : clickRegions) {
                if (region.contains(click.x(), click.y())) {
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
                && mouseX >= viewportLeft
                && mouseX <= viewportRight
                && mouseY >= viewportTop
                && mouseY <= viewportBottom) {
            scrollOffset = Math.max(0, Math.min(maxScroll,
                    scrollOffset - (int) Math.round(vertical * 28.0)));
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

    private int statusColor(String status) {
        return switch (status) {
            case "습득" -> ACCENT;
            case "습득 가능" -> GOLD;
            case "잠김", "포인트 필요" -> 0xFF59636E;
            case "데이터 잠금" -> DANGER;
            default -> 0xFF86A8E8;
        };
    }

    private String compact(String value, int maxCharacters) {
        if (value.length() <= maxCharacters) {
            return value;
        }
        return value.substring(0, Math.max(1, maxCharacters - 1)) + "…";
    }

    private Layout layout() {
        int panelWidth = Math.min(620, Math.max(280, width - 12));
        int panelHeight = Math.min(420, Math.max(176, height - 10));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Layout(int left, int top, int width, int height) {
    }

    private record ClickRegion(int x, int y, int width, int height, String action) {
        boolean contains(double mouseX, double mouseY) {
            return inside(mouseX, mouseY, x, y, width, height);
        }
    }
}
