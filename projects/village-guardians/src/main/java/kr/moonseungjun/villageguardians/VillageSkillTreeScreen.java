package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class VillageSkillTreeScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int BACKGROUND = 0xFF090E13;
    private static final int GRID = 0xFF141C24;
    private static final int PANEL = 0xF20D151D;
    private static final int SURFACE = 0xFF17222B;
    private static final int SURFACE_HOVER = 0xFF1D2B35;
    private static final int BORDER = 0xFF364754;
    private static final int TEXT = 0xFFDCE5EA;
    private static final int MUTED = 0xFF8998A2;
    private static final int ACCENT = 0xFF4A9188;
    private static final int GOLD = 0xFFC3A45D;
    private static final int RED = 0xFFAA6068;
    private static final int BLUE = 0xFF5D7FA2;
    private static final int GREEN = 0xFF5F8E72;
    private static final int PURPLE = 0xFF7D6C99;
    private static final int HEADER_HEIGHT = 38;

    private static double savedPanX;
    private static double savedPanY;
    private static double savedZoom = 0.50;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private final List<NodeVisual> nodes = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean dragging;

    public VillageSkillTreeScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        buildNodes();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Viewport viewport = viewport();
        renderGrid(graphics, viewport);
        renderConnections(graphics, viewport);
        renderCore(graphics, viewport);
        renderNodes(graphics, mouseX, mouseY, viewport);
        renderBubble(graphics, mouseX, mouseY, viewport);
        renderHeader(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGrid(GuiGraphicsExtractor graphics, Viewport viewport) {
        graphics.fill(viewport.left(), viewport.top(), viewport.right(), viewport.bottom(), BACKGROUND);
        int spacing = Math.max(16, (int) Math.round(28 * savedZoom));
        int startX = viewport.left() + Math.floorMod((int) Math.round(savedPanX), spacing);
        int startY = viewport.top() + Math.floorMod((int) Math.round(savedPanY), spacing);
        for (int x = startX; x < viewport.right(); x += spacing) {
            graphics.fill(x, viewport.top(), x + 1, viewport.bottom(), GRID);
        }
        for (int y = startY; y < viewport.bottom(); y += spacing) {
            graphics.fill(viewport.left(), y, viewport.right(), y + 1, GRID);
        }
    }

    private void renderConnections(GuiGraphicsExtractor graphics, Viewport viewport) {
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        for (NodeVisual node : nodes) {
            NodeVisual previous = previous(node);
            double x0 = previous == null ? 0 : previous.worldX();
            double y0 = previous == null ? 0 : previous.worldY();
            int color = previous == null || "습득".equals(previous.status())
                    ? branchColor(node.branch()) : 0xFF2C3944;
            drawLine(graphics, screenX(viewport, x0), screenY(viewport, y0),
                    screenX(viewport, node.worldX()), screenY(viewport, node.worldY()), color);
        }
        graphics.disableScissor();
    }

    private void renderCore(GuiGraphicsExtractor graphics, Viewport viewport) {
        int size = Math.max(20, scaledNodeSize() - 7);
        int cx = screenX(viewport, 0);
        int cy = screenY(viewport, 0);
        int x = cx - size / 2;
        int y = cy - size / 2;
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, GOLD);
        graphics.fill(x, y, x + size, y + size, SURFACE);
        graphics.fill(cx - 2, y + 5, cx + 2, y + size - 5, GOLD);
        graphics.fill(x + 5, cy - 2, x + size - 5, cy + 2, GOLD);
        graphics.disableScissor();
    }

    private void renderNodes(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Viewport viewport) {
        int nodeSize = scaledNodeSize();
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        for (int index = 0; index < nodes.size(); index++) {
            NodeVisual node = nodes.get(index);
            int cx = screenX(viewport, node.worldX());
            int cy = screenY(viewport, node.worldY());
            int x = cx - nodeSize / 2;
            int y = cy - nodeSize / 2;
            if (x + nodeSize < viewport.left() || x > viewport.right()
                    || y + nodeSize < viewport.top() || y > viewport.bottom()) continue;
            int color = branchColor(node.branch());
            int border = node.tier() == 10 && !"습득".equals(node.status())
                    ? GOLD
                    : switch (node.status()) {
                        case "습득" -> color;
                        case "습득 가능" -> GOLD;
                        case "데이터 잠금" -> RED;
                        default -> BORDER;
                    };
            boolean hovered = inside(mouseX, mouseY, x, y, nodeSize, nodeSize);
            boolean selected = selectedIndex == index;
            graphics.fill(x - 2, y - 2, x + nodeSize + 2, y + nodeSize + 2,
                    selected ? GOLD : hovered ? border : 0xFF121B22);
            graphics.fill(x - 1, y - 1, x + nodeSize + 1, y + nodeSize + 1, border);
            graphics.fill(x, y, x + nodeSize, y + nodeSize,
                    hovered || selected ? SURFACE_HOVER : SURFACE);
            drawNodeIcon(graphics, node.branch(), node.tier(), x + 5, y + 5,
                    nodeSize - 10, "습득".equals(node.status()) ? color : border);
            if (hovered || selected || savedZoom >= 0.88) {
                graphics.centeredText(font, fit(node.title(), 82), cx, y + nodeSize + 4,
                        hovered || selected ? TEXT : MUTED);
            }
        }
        graphics.disableScissor();
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, HEADER_HEIGHT, PANEL);
        graphics.fill(0, HEADER_HEIGHT - 1, width, HEADER_HEIGHT, BORDER);
        int closeX = width - 28;
        int centerX = closeX - 43;
        int plusX = centerX - 27;
        int percentX = plusX - 36;
        int minusX = percentX - 27;
        int textRight = Math.max(90, minusX - 10);
        graphics.text(font, fit(payload.title().isBlank() ? "성장" : payload.title(), textRight - 10),
                10, 6, TEXT, false);
        graphics.text(font, fit(payload.body(), textRight - 10), 10, 20, MUTED, false);
        drawHeaderButton(graphics, mouseX, mouseY, minusX, 7, 22, "−");
        graphics.centeredText(font, Math.round(savedZoom * 100) + "%", percentX + 16, 13, MUTED);
        drawHeaderButton(graphics, mouseX, mouseY, plusX, 7, 22, "+");
        drawHeaderButton(graphics, mouseX, mouseY, centerX, 7, 38, "중앙");
        drawHeaderButton(graphics, mouseX, mouseY, closeX, 7, 20, "×");
    }

    private void drawHeaderButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                  int x, int y, int w, String label) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, 20);
        graphics.fill(x, y, x + w, y + 20, hovered ? SURFACE_HOVER : 0xFF101820);
        graphics.centeredText(font, fit(label, w - 4), x + w / 2, y + 6,
                hovered ? TEXT : MUTED);
    }

    private void renderBubble(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Viewport viewport) {
        Bubble bubble = bubble(viewport);
        if (bubble == null) return;
        NodeVisual node = bubble.node();
        int edgeX = bubble.x() > bubble.nodeX() ? bubble.x() : bubble.x() + bubble.width();
        int edgeY = clamp(bubble.nodeY(), bubble.y() + 12, bubble.y() + bubble.height() - 12);
        drawLine(graphics, bubble.nodeX(), bubble.nodeY(), edgeX, edgeY, branchColor(node.branch()));
        graphics.fill(bubble.x() + 3, bubble.y() + 3,
                bubble.x() + bubble.width() + 3, bubble.y() + bubble.height() + 3, 0x78000000);
        graphics.fill(bubble.x() - 1, bubble.y() - 1,
                bubble.x() + bubble.width() + 1, bubble.y() + bubble.height() + 1,
                branchColor(node.branch()));
        graphics.fill(bubble.x(), bubble.y(), bubble.x() + bubble.width(),
                bubble.y() + bubble.height(), PANEL);
        graphics.text(font, fit(branchName(node.branch()) + " " + node.tier() + "단계 · " + node.title(),
                        bubble.width() - 16), bubble.x() + 8, bubble.y() + 7, TEXT, false);
        graphics.text(font, fit(node.status(), bubble.width() - 16), bubble.x() + 8,
                bubble.y() + 20, statusColor(node.status()), false);
        int y = bubble.y() + 34;
        for (int i = 0; i < bubble.lineCount(); i++) {
            graphics.text(font, bubble.lines().get(i), bubble.x() + 8, y, MUTED, false);
            y += 11;
        }
        if (bubble.purchasable()) {
            boolean hovered = inside(mouseX, mouseY, bubble.buttonX(), bubble.buttonY(),
                    bubble.buttonWidth(), bubble.buttonHeight());
            graphics.fill(bubble.buttonX() - 1, bubble.buttonY() - 1,
                    bubble.buttonX() + bubble.buttonWidth() + 1,
                    bubble.buttonY() + bubble.buttonHeight() + 1, hovered ? GOLD : ACCENT);
            graphics.fill(bubble.buttonX(), bubble.buttonY(), bubble.buttonX() + bubble.buttonWidth(),
                    bubble.buttonY() + bubble.buttonHeight(), hovered ? 0xFF27332D : SURFACE);
            graphics.centeredText(font, "습득 · " + node.pointCost() + "P", bubble.buttonX() + bubble.buttonWidth() / 2,
                    bubble.buttonY() + 5, TEXT);
        }
    }

    private Bubble bubble(Viewport viewport) {
        if (selectedIndex < 0 || selectedIndex >= nodes.size()) return null;
        NodeVisual node = nodes.get(selectedIndex);
        int nodeX = screenX(viewport, node.worldX());
        int nodeY = screenY(viewport, node.worldY());
        int nodeHalf = scaledNodeSize() / 2;
        if (nodeX + nodeHalf < viewport.left() || nodeX - nodeHalf > viewport.right()
                || nodeY + nodeHalf < viewport.top() || nodeY - nodeHalf > viewport.bottom()) {
            return null;
        }
        int bubbleWidth = fitPopoverWidth(viewport.width(), 164, 246);
        boolean purchasable = "습득 가능".equals(node.status());
        List<FormattedCharSequence> lines = font.split(Component.literal(node.description()),
                Math.max(40, bubbleWidth - 16));
        int baseHeight = 45 + (purchasable ? 24 : 8);
        int lineCount = Math.min(lines.size(), Math.max(0, (viewport.height() - baseHeight - 10) / 11));
        int bubbleHeight = baseHeight + lineCount * 11;
        int nodeSize = scaledNodeSize();
        int x = nodeX + nodeSize / 2 + 9;
        if (x + bubbleWidth > viewport.right() - 5) {
            x = nodeX - nodeSize / 2 - bubbleWidth - 9;
        }
        x = clamp(x, viewport.left() + 5, Math.max(viewport.left() + 5, viewport.right() - bubbleWidth - 5));
        int y = clamp(nodeY - 25, viewport.top() + 5,
                Math.max(viewport.top() + 5, viewport.bottom() - bubbleHeight - 5));
        int buttonWidth = Math.max(1, Math.min(68, bubbleWidth - 14));
        int buttonHeight = 18;
        int buttonX = x + bubbleWidth - buttonWidth - 7;
        int buttonY = y + bubbleHeight - buttonHeight - 6;
        return new Bubble(x, y, bubbleWidth, bubbleHeight, buttonX, buttonY,
                buttonWidth, buttonHeight, purchasable, lines, lineCount, node, nodeX, nodeY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int closeX = width - 28;
        int centerX = closeX - 43;
        int plusX = centerX - 27;
        int percentX = plusX - 36;
        int minusX = percentX - 27;
        if (inside(click.x(), click.y(), closeX, 7, 20, 20)) { onClose(); return true; }
        if (inside(click.x(), click.y(), centerX, 7, 38, 20)) {
            savedPanX = 0; savedPanY = 0; savedZoom = 0.50; return true;
        }
        if (inside(click.x(), click.y(), minusX, 7, 22, 20)) {
            setZoom(savedZoom - 0.12, width / 2.0, height / 2.0); return true;
        }
        if (inside(click.x(), click.y(), plusX, 7, 22, 20)) {
            setZoom(savedZoom + 0.12, width / 2.0, height / 2.0); return true;
        }

        Viewport viewport = viewport();
        Bubble bubble = bubble(viewport);
        if (bubble != null) {
            if (bubble.purchasable() && inside(click.x(), click.y(), bubble.buttonX(), bubble.buttonY(),
                    bubble.buttonWidth(), bubble.buttonHeight())) {
                ClientPacketDistributor.sendToServer(
                        new VillageNetwork.VillageUiActionPayload(bubble.node().action()));
                return true;
            }
            if (inside(click.x(), click.y(), bubble.x(), bubble.y(), bubble.width(), bubble.height())) {
                return true;
            }
        }
        if (!inside(click.x(), click.y(), viewport.left(), viewport.top(),
                viewport.width(), viewport.height())) return super.mouseClicked(click, doubled);
        int nodeSize = scaledNodeSize();
        for (int index = 0; index < nodes.size(); index++) {
            NodeVisual node = nodes.get(index);
            int x = screenX(viewport, node.worldX()) - nodeSize / 2;
            int y = screenY(viewport, node.worldY()) - nodeSize / 2;
            if (inside(click.x(), click.y(), x, y, nodeSize, nodeSize)) {
                selectedIndex = selectedIndex == index ? -1 : index;
                return true;
            }
        }
        selectedIndex = -1;
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging && event.button() == 0) {
            savedPanX = clamp(savedPanX + dragX, -1200, 1200);
            savedPanY = clamp(savedPanY + dragY, -1200, 1200);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Viewport viewport = viewport();
        if (inside(mouseX, mouseY, viewport.left(), viewport.top(), viewport.width(), viewport.height())) {
            setZoom(savedZoom + vertical * 0.09, mouseX, mouseY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void setZoom(double requested, double mouseX, double mouseY) {
        Viewport viewport = viewport();
        double old = savedZoom;
        double next = clamp(requested, 0.38, 1.55);
        double centerX = (viewport.left() + viewport.right()) / 2.0;
        double centerY = (viewport.top() + viewport.bottom()) / 2.0;
        double worldX = (mouseX - centerX - savedPanX) / old;
        double worldY = (mouseY - centerY - savedPanY) / old;
        savedZoom = next;
        savedPanX = clamp(mouseX - centerX - worldX * next, -1200, 1200);
        savedPanY = clamp(mouseY - centerY - worldY * next, -1200, 1200);
    }

    private void buildNodes() {
        int count = Math.min(actions.length, labels.length);
        for (int index = 0; index < count; index++) {
            String[] parts = labels[index].split("\\|", 4);
            String title = parts.length > 0 ? parts[0] : labels[index];
            String description = parts.length > 1 ? parts[1] : "상세 효과 없음";
            String status = parts.length > 2 ? parts[2] : "잠김";
            int pointCost = 1;
            if (parts.length > 3) {
                try { pointCost = Math.max(1, Integer.parseInt(parts[3])); }
                catch (NumberFormatException ignored) { pointCost = 1; }
            }
            String id = actions[index].startsWith("skill_node:")
                    ? actions[index].substring(11) : actions[index];
            Branch branch = branch(id);
            int tier = tier(id);
            double distance = 72.0;
            double angle = Math.toRadians(branchAngleDegrees(branch));
            double worldX = Math.cos(angle) * tier * distance;
            double worldY = Math.sin(angle) * tier * distance;
            nodes.add(new NodeVisual(actions[index], id, title, description, status,
                    branch, tier, pointCost, worldX, worldY));
        }
    }

    private NodeVisual previous(NodeVisual node) {
        if (node.tier() <= 1) return null;
        for (NodeVisual candidate : nodes) {
            if (candidate.branch() == node.branch() && candidate.tier() == node.tier() - 1) {
                return candidate;
            }
        }
        return null;
    }

    private Branch branch(String id) {
        if (id.startsWith("guard_")) return Branch.GUARD;
        if (id.startsWith("support_")) return Branch.SUPPORT;
        if (id.startsWith("ranged_")) return Branch.RANGED;
        if (id.startsWith("mobility_")) return Branch.MOBILITY;
        return Branch.POWER;
    }

    private static double branchAngleDegrees(Branch branch) {
        return switch (branch) {
            case POWER -> -90.0;
            case RANGED -> -18.0;
            case MOBILITY -> 54.0;
            case SUPPORT -> 126.0;
            case GUARD -> 198.0;
        };
    }

    private int tier(String id) {
        int split = id.lastIndexOf('_');
        if (split >= 0) {
            try { return Math.max(1, Integer.parseInt(id.substring(split + 1))); }
            catch (NumberFormatException ignored) { }
        }
        return 1;
    }

    private static int fitPopoverWidth(int viewportWidth, int preferredMinimum, int preferredMaximum) {
        int maximum = Math.max(1, viewportWidth - 10);
        int minimum = Math.min(preferredMinimum, maximum);
        int preferred = Math.max(minimum, Math.min(maximum, viewportWidth / 3));
        return Math.min(maximum, Math.min(preferredMaximum, preferred));
    }

    private int screenX(Viewport viewport, double worldX) {
        return (int) Math.round((viewport.left() + viewport.right()) / 2.0
                + savedPanX + worldX * savedZoom);
    }

    private int screenY(Viewport viewport, double worldY) {
        return (int) Math.round((viewport.top() + viewport.bottom()) / 2.0
                + savedPanY + worldY * savedZoom);
    }

    private int scaledNodeSize() {
        return (int) Math.round(clamp(38 * savedZoom, 24, 52));
    }

    private void drawLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            graphics.fill(x0 - 1, y0 - 1, x0 + 2, y0 + 2, color);
            if (x0 == x1 && y0 == y1) break;
            int twice = 2 * error;
            if (twice >= dy) { error += dy; x0 += sx; }
            if (twice <= dx) { error += dx; y0 += sy; }
        }
    }

    private void drawNodeIcon(GuiGraphicsExtractor graphics, Branch branch, int tier,
                              int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        switch (branch) {
            case POWER -> {
                graphics.fill(cx - 2, y + 2, cx + 2, y + size - 3, color);
                graphics.fill(cx - 6, y + 3, cx + 6, y + 6, color);
            }
            case GUARD -> {
                graphics.fill(cx - 7, y + 4, cx + 7, cy + 2, color);
                graphics.fill(cx - 4, cy, cx + 4, y + size - 4, color);
            }
            case RANGED -> {
                graphics.fill(x + 4, cy - 1, x + size - 4, cy + 1, color);
                graphics.fill(x + size - 7, cy - 4, x + size - 4, cy + 4, color);
            }
            case SUPPORT -> {
                graphics.fill(cx - 2, y + 3, cx + 2, y + size - 3, color);
                graphics.fill(x + 3, cy - 2, x + size - 3, cy + 2, color);
            }
            case MOBILITY -> {
                graphics.fill(x + 3, y + size - 5, x + size - 4, y + size - 3, color);
                graphics.fill(x + size - 7, y + 3, x + size - 4, y + size - 3, color);
                graphics.fill(x + size - 10, y + 3, x + size - 4, y + 7, color);
            }
        }
        if (tier >= 9) {
            graphics.fill(x + 2, y + 2, x + 6, y + 4, GOLD);
            graphics.fill(x + size - 6, y + 2, x + size - 2, y + 4, GOLD);
            graphics.fill(x + 2, y + size - 4, x + 6, y + size - 2, GOLD);
            graphics.fill(x + size - 6, y + size - 4, x + size - 2, y + size - 2, GOLD);
        } else if (tier >= 5) {
            graphics.fill(x + 3, y + 3, x + 6, y + 6, GOLD);
        }
        if (tier == 10) graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, GOLD);
    }

    private int branchColor(Branch branch) {
        return switch (branch) {
            case POWER -> RED;
            case GUARD -> BLUE;
            case RANGED -> PURPLE;
            case SUPPORT -> GREEN;
            case MOBILITY -> ACCENT;
        };
    }

    private int statusColor(String status) {
        return switch (status) {
            case "습득" -> ACCENT;
            case "습득 가능" -> GOLD;
            case "데이터 잠금" -> RED;
            default -> MUTED;
        };
    }

    private String branchName(Branch branch) {
        return switch (branch) {
            case POWER -> "공격";
            case GUARD -> "방어";
            case SUPPORT -> "지원";
            case RANGED -> "사격";
            case MOBILITY -> "기동";
        };
    }

    private Viewport viewport() {
        return new Viewport(0, HEADER_HEIGHT, width, Math.max(HEADER_HEIGHT + 1, height));
    }

    private String fit(String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (maxWidth <= 0 || font.width(normalized) <= maxWidth) return normalized;
        String suffix = "…";
        int end = normalized.length();
        while (end > 1 && font.width(normalized.substring(0, end) + suffix) > maxWidth) end--;
        return normalized.substring(0, Math.max(1, end)) + suffix;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }

    private enum Branch { POWER, GUARD, RANGED, SUPPORT, MOBILITY }

    private record Viewport(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }

    private record NodeVisual(String action, String id, String title, String description,
                              String status, Branch branch, int tier, int pointCost,
                              double worldX, double worldY) {}

    private record Bubble(int x, int y, int width, int height,
                          int buttonX, int buttonY, int buttonWidth, int buttonHeight,
                          boolean purchasable, List<FormattedCharSequence> lines, int lineCount,
                          NodeVisual node, int nodeX, int nodeY) {}
}
