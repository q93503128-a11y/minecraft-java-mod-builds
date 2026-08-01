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
    private static final int BACKGROUND = 0xFF070B10;
    private static final int GRID = 0xFF101923;
    private static final int PANEL = 0xEE0C131B;
    private static final int SURFACE = 0xFF14202A;
    private static final int BORDER = 0xFF405568;
    private static final int TEXT = 0xFFF4F7FA;
    private static final int MUTED = 0xFFA5B3BF;
    private static final int ACCENT = 0xFF43D6BC;
    private static final int GOLD = 0xFFF2C25B;
    private static final int RED = 0xFFE36E76;
    private static final int BLUE = 0xFF78A7ED;
    private static final int GREEN = 0xFF55D49B;
    private static final int PURPLE = 0xFFB38AE8;
    private static final int NODE_SIZE = 44;

    private static double savedPanX;
    private static double savedPanY;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private final List<NodeVisual> nodes = new ArrayList<>();

    private int selectedIndex = -1;
    private boolean dragging;

    public VillageSkillTreeScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        this.actions = payload.actions().isBlank()
                ? new String[0] : payload.actions().split(SEP, -1);
        this.labels = payload.labels().isBlank()
                ? new String[0] : payload.labels().split(SEP, -1);
        buildNodes();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Viewport viewport = viewport();
        renderGrid(graphics, viewport);
        renderConnections(graphics, viewport);
        renderNodes(graphics, mouseX, mouseY, viewport);
        renderHeader(graphics, mouseX, mouseY);
        renderDetail(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGrid(GuiGraphicsExtractor graphics, Viewport viewport) {
        graphics.fill(viewport.left(), viewport.top(), viewport.right(), viewport.bottom(), BACKGROUND);
        int spacing = 28;
        int startX = viewport.left() + Math.floorMod((int) Math.round(savedPanX), spacing);
        int startY = viewport.top() + Math.floorMod((int) Math.round(savedPanY), spacing);
        for (int x = startX; x < viewport.right(); x += spacing) {
            graphics.fill(x, viewport.top(), x + 1, viewport.bottom(), GRID);
        }
        for (int y = startY; y < viewport.bottom(); y += spacing) {
            graphics.fill(viewport.left(), y, viewport.right(), y + 1, GRID);
        }
        drawCore(graphics, viewport, screenX(viewport, 0), screenY(viewport, 0));
    }

    private void renderConnections(GuiGraphicsExtractor graphics, Viewport viewport) {
        for (NodeVisual node : nodes) {
            int x1 = screenX(viewport, node.worldX());
            int y1 = screenY(viewport, node.worldY());
            int x0;
            int y0;
            if (node.prerequisiteId() == null) {
                x0 = screenX(viewport, 0);
                y0 = screenY(viewport, 0);
            } else {
                NodeVisual prerequisite = findNode(node.prerequisiteId());
                if (prerequisite == null) {
                    continue;
                }
                x0 = screenX(viewport, prerequisite.worldX());
                y0 = screenY(viewport, prerequisite.worldY());
            }
            int color = prerequisiteUnlocked(node) ? branchColor(node.branch()) : 0xFF334250;
            drawLine(graphics, viewport, x0, y0, x1, y1, color);
        }
    }

    private void renderNodes(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            Viewport viewport) {
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        for (int index = 0; index < nodes.size(); index++) {
            NodeVisual node = nodes.get(index);
            int centerX = screenX(viewport, node.worldX());
            int centerY = screenY(viewport, node.worldY());
            int x = centerX - NODE_SIZE / 2;
            int y = centerY - NODE_SIZE / 2;
            if (x + NODE_SIZE < viewport.left() || x > viewport.right()
                    || y + NODE_SIZE < viewport.top() || y > viewport.bottom()) {
                continue;
            }

            String status = node.status();
            int branchColor = branchColor(node.branch());
            int borderColor = switch (status) {
                case "습득" -> branchColor;
                case "습득 가능" -> GOLD;
                case "데이터 잠금" -> RED;
                default -> 0xFF354552;
            };
            boolean hovered = inside(mouseX, mouseY, x, y, NODE_SIZE, NODE_SIZE);
            boolean selected = selectedIndex == index;

            graphics.fill(x - 3, y - 3, x + NODE_SIZE + 3, y + NODE_SIZE + 3,
                    selected ? GOLD : hovered ? borderColor : 0xFF18242E);
            graphics.fill(x - 1, y - 1, x + NODE_SIZE + 1, y + NODE_SIZE + 1, borderColor);
            graphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE,
                    hovered || selected ? 0xFF1C2A35 : SURFACE);
            drawNodeIcon(graphics, node.branch(), node.tier(),
                    x + 7, y + 7, NODE_SIZE - 14,
                    "습득".equals(status) ? branchColor : borderColor);

            if ("습득".equals(status)) {
                graphics.fill(x + NODE_SIZE - 11, y + NODE_SIZE - 11,
                        x + NODE_SIZE - 4, y + NODE_SIZE - 4, ACCENT);
            } else if ("잠김".equals(status) || "포인트 필요".equals(status)) {
                graphics.fill(x + NODE_SIZE - 12, y + NODE_SIZE - 10,
                        x + NODE_SIZE - 4, y + NODE_SIZE - 5, 0xFF65717A);
                graphics.fill(x + NODE_SIZE - 10, y + NODE_SIZE - 14,
                        x + NODE_SIZE - 6, y + NODE_SIZE - 9, 0xFF65717A);
            }

            graphics.centeredText(font, Integer.toString(node.tier()), centerX,
                    y + NODE_SIZE + 6, hovered || selected ? TEXT : MUTED);
        }
        graphics.disableScissor();
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, 47, PANEL);
        graphics.fill(0, 46, width, 48, BORDER);
        graphics.text(font, "전술 발전", 16, 12, TEXT, false);
        graphics.text(font, compact(payload.body(), Math.max(30, Math.min(70, width / 9))),
                16, 28, MUTED, false);

        if (width >= 690) {
            int legendX = width / 2 - 140;
            drawLegend(graphics, legendX, 12, RED, "공격");
            drawLegend(graphics, legendX + 70, 12, BLUE, "방어");
            drawLegend(graphics, legendX + 140, 12, PURPLE, "사격");
            drawLegend(graphics, legendX + 210, 12, GREEN, "지원");
        }

        int recenterX = width - 92;
        int closeX = width - 42;
        boolean recenterHovered = inside(mouseX, mouseY, recenterX, 10, 42, 25);
        boolean closeHovered = inside(mouseX, mouseY, closeX, 10, 26, 25);
        graphics.fill(recenterX, 10, recenterX + 42, 35,
                recenterHovered ? SURFACE : 0xFF101923);
        graphics.centeredText(font, "중앙", recenterX + 21, 18,
                recenterHovered ? GOLD : MUTED);
        graphics.fill(closeX, 10, closeX + 26, 35,
                closeHovered ? 0xFF6E3038 : 0xFF101923);
        graphics.centeredText(font, "×", closeX + 13, 18,
                closeHovered ? 0xFFFFFFFF : MUTED);
    }

    private void renderDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int top = height - 82;
        graphics.fill(0, top, width, height, PANEL);
        graphics.fill(0, top, width, top + 2, BORDER);

        String title = "노드를 선택하세요";
        String description = "빈 공간을 드래그해 트리를 이동할 수 있습니다.";
        String status = "";
        int color = MUTED;
        if (selectedIndex >= 0 && selectedIndex < nodes.size()) {
            NodeVisual node = nodes.get(selectedIndex);
            title = node.title();
            description = node.description();
            status = node.status();
            color = branchColor(node.branch());
        }

        graphics.text(font, title, 16, top + 12, color, false);
        if (!status.isBlank()) {
            graphics.text(font, status, 16, top + 29,
                    statusColor(status), false);
        }

        int buttonWidth = Math.min(126, Math.max(92, width / 5));
        int buttonX = width - buttonWidth - 16;
        int buttonY = top + 28;
        int descriptionRight = buttonX - 14;
        List<FormattedCharSequence> lines = font.split(
                Component.literal(description), Math.max(80, descriptionRight - 16));
        int y = top + 47;
        for (FormattedCharSequence line : lines) {
            if (y > height - 9) {
                break;
            }
            graphics.text(font, line, 16, y, MUTED, false);
            y += 11;
        }

        boolean purchasable = selectedIndex >= 0
                && "습득 가능".equals(nodes.get(selectedIndex).status());
        boolean hovered = purchasable
                && inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 25);
        graphics.fill(buttonX - 1, buttonY - 1,
                buttonX + buttonWidth + 1, buttonY + 26,
                hovered ? GOLD : purchasable ? color : 0xFF35424D);
        graphics.fill(buttonX, buttonY,
                buttonX + buttonWidth, buttonY + 25,
                hovered ? 0xFF3C3420 : purchasable ? SURFACE : 0xFF171E25);
        String buttonText = purchasable ? "습득 · 1P" :
                selectedIndex < 0 ? "노드 선택 필요" : nodes.get(selectedIndex).status();
        graphics.centeredText(font, buttonText,
                buttonX + buttonWidth / 2, buttonY + 8,
                purchasable ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }
        if (inside(click.x(), click.y(), width - 42, 10, 26, 25)) {
            onClose();
            return true;
        }
        if (inside(click.x(), click.y(), width - 92, 10, 42, 25)) {
            savedPanX = 0;
            savedPanY = 0;
            return true;
        }

        int buttonWidth = Math.min(126, Math.max(92, width / 5));
        if (selectedIndex >= 0 && selectedIndex < nodes.size()) {
            int buttonX = width - buttonWidth - 16;
            int buttonY = height - 54;
            if ("습득 가능".equals(nodes.get(selectedIndex).status())
                    && inside(click.x(), click.y(), buttonX, buttonY, buttonWidth, 25)) {
                ClientPacketDistributor.sendToServer(
                        new VillageNetwork.VillageUiActionPayload(nodes.get(selectedIndex).action()));
                return true;
            }
        }

        Viewport viewport = viewport();
        if (!inside(click.x(), click.y(),
                viewport.left(), viewport.top(), viewport.width(), viewport.height())) {
            return super.mouseClicked(click, doubled);
        }
        for (int index = 0; index < nodes.size(); index++) {
            NodeVisual node = nodes.get(index);
            int x = screenX(viewport, node.worldX()) - NODE_SIZE / 2;
            int y = screenY(viewport, node.worldY()) - NODE_SIZE / 2;
            if (inside(click.x(), click.y(), x, y, NODE_SIZE, NODE_SIZE)) {
                selectedIndex = index;
                return true;
            }
        }

        dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging && event.button() == 0) {
            savedPanX = clamp(savedPanX + dragX, -760, 760);
            savedPanY = clamp(savedPanY + dragY, -760, 760);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    private void drawCore(
            GuiGraphicsExtractor graphics,
            Viewport viewport,
            int centerX,
            int centerY) {
        int size = 34;
        int x = centerX - size / 2;
        int y = centerY - size / 2;
        if (x + size < viewport.left() || x > viewport.right()
                || y + size < viewport.top() || y > viewport.bottom()) {
            return;
        }
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        graphics.fill(x - 3, y - 3, x + size + 3, y + size + 3, GOLD);
        graphics.fill(x, y, x + size, y + size, 0xFF202A31);
        graphics.fill(centerX - 3, y + 7, centerX + 3, y + size - 7, GOLD);
        graphics.fill(x + 7, centerY - 3, x + size - 7, centerY + 3, GOLD);
        graphics.disableScissor();
    }

    private void drawNodeIcon(
            GuiGraphicsExtractor graphics,
            Branch branch,
            int tier,
            int x,
            int y,
            int size,
            int color) {
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        switch (branch) {
            case POWER -> {
                graphics.fill(centerX - 2, y + 2, centerX + 2, y + size - 4, color);
                graphics.fill(centerX - 6, y + 3, centerX + 6, y + 7, color);
                graphics.fill(centerX - 6, y + size - 7, centerX + 6, y + size - 3, color);
            }
            case GUARD -> {
                graphics.fill(centerX - 8, y + 4, centerX + 8, y + 9, color);
                graphics.fill(centerX - 10, y + 9, centerX + 10, centerY + 3, color);
                graphics.fill(centerX - 6, centerY + 3, centerX + 6, y + size - 5, color);
                graphics.fill(centerX - 2, y + size - 6, centerX + 2, y + size - 2, color);
            }
            case RANGED -> {
                for (int i = 0; i < size - 8; i++) {
                    graphics.fill(x + 4 + i / 2, y + 3 + i,
                            x + 6 + i / 2, y + 5 + i, color);
                }
                graphics.fill(x + 4, centerY - 1, x + size - 4, centerY + 1, color);
                graphics.fill(x + size - 8, centerY - 5, x + size - 4, centerY + 5, color);
            }
            case SUPPORT -> {
                graphics.fill(centerX - 3, y + 3, centerX + 3, y + size - 3, color);
                graphics.fill(x + 3, centerY - 3, x + size - 3, centerY + 3, color);
            }
        }
        if (tier >= 2) {
            graphics.fill(x + 2, y + size - 4, x + 6, y + size, color);
        }
        if (tier >= 3) {
            graphics.fill(x + size - 6, y + size - 4, x + size - 2, y + size, color);
        }
    }

    private void drawLine(
            GuiGraphicsExtractor graphics,
            Viewport viewport,
            int x0,
            int y0,
            int x1,
            int y1,
            int color) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= 0) {
            return;
        }
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        for (int step = 0; step <= steps; step += 2) {
            int x = x0 + dx * step / steps;
            int y = y0 + dy * step / steps;
            graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
        }
        graphics.disableScissor();
    }

    private void drawLegend(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int color,
            String label) {
        graphics.fill(x, y + 2, x + 8, y + 10, color);
        graphics.text(font, label, x + 13, y + 1, MUTED, false);
    }

    private boolean prerequisiteUnlocked(NodeVisual node) {
        if (node.prerequisiteId() == null) {
            return true;
        }
        NodeVisual prerequisite = findNode(node.prerequisiteId());
        return prerequisite != null && "습득".equals(prerequisite.status());
    }

    private NodeVisual findNode(String id) {
        for (NodeVisual node : nodes) {
            if (node.id().equals(id)) {
                return node;
            }
        }
        return null;
    }

    private int statusColor(String status) {
        return switch (status) {
            case "습득" -> ACCENT;
            case "습득 가능" -> GOLD;
            case "데이터 잠금" -> RED;
            default -> MUTED;
        };
    }

    private int branchColor(Branch branch) {
        return switch (branch) {
            case POWER -> RED;
            case GUARD -> BLUE;
            case RANGED -> PURPLE;
            case SUPPORT -> GREEN;
        };
    }

    private int screenX(Viewport viewport, int worldX) {
        return viewport.left() + viewport.width() / 2
                + (int) Math.round(savedPanX) + worldX;
    }

    private int screenY(Viewport viewport, int worldY) {
        return viewport.top() + viewport.height() / 2
                + (int) Math.round(savedPanY) + worldY;
    }

    private void buildNodes() {
        int count = Math.min(actions.length, labels.length);
        for (int index = 0; index < count; index++) {
            String action = actions[index];
            if (!action.startsWith("skill_node:")) {
                continue;
            }
            String id = action.substring("skill_node:".length());
            String[] parts = labels[index].split("\\|", 3);
            String title = parts.length > 0 ? parts[0] : id;
            String description = parts.length > 1 ? parts[1] : "";
            String status = parts.length > 2 ? parts[2] : "잠김";
            Branch branch = branchOf(id);
            int tier = tierOf(id);
            Position position = positionOf(branch, tier);
            String prerequisite = tier <= 1 ? null : prefixOf(branch) + "_" + (tier - 1);
            nodes.add(new NodeVisual(
                    action, id, title, description, status,
                    branch, tier, prerequisite, position.x(), position.y()));
        }
    }

    private Branch branchOf(String id) {
        if (id.startsWith("guard_")) {
            return Branch.GUARD;
        }
        if (id.startsWith("ranged_")) {
            return Branch.RANGED;
        }
        if (id.startsWith("support_")) {
            return Branch.SUPPORT;
        }
        return Branch.POWER;
    }

    private int tierOf(String id) {
        int separator = id.lastIndexOf('_');
        if (separator < 0 || separator + 1 >= id.length()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(id.substring(separator + 1)));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private Position positionOf(Branch branch, int tier) {
        int safeTier = Math.max(1, Math.min(3, tier));
        return switch (branch) {
            case POWER -> new Position(130 * safeTier, -42 * (safeTier - 1));
            case GUARD -> new Position(-130 * safeTier, 42 * (safeTier - 1));
            case RANGED -> new Position(42 * (safeTier - 1), -130 * safeTier);
            case SUPPORT -> new Position(-42 * (safeTier - 1), 130 * safeTier);
        };
    }

    private String prefixOf(Branch branch) {
        return switch (branch) {
            case POWER -> "power";
            case GUARD -> "guard";
            case RANGED -> "ranged";
            case SUPPORT -> "support";
        };
    }

    private String compact(String value, int maxCharacters) {
        String normalized = value.replace('\n', ' ');
        if (normalized.length() <= maxCharacters) {
            return normalized;
        }
        return normalized.substring(0, Math.max(1, maxCharacters - 1)) + "…";
    }

    private Viewport viewport() {
        int bottom = Math.max(96, height - 82);
        return new Viewport(8, 48, Math.max(9, width - 8), bottom);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
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

    private enum Branch {
        POWER, GUARD, RANGED, SUPPORT
    }

    private record Viewport(int left, int top, int right, int bottom) {
        int width() {
            return right - left;
        }

        int height() {
            return bottom - top;
        }
    }

    private record Position(int x, int y) {
    }

    private record NodeVisual(
            String action,
            String id,
            String title,
            String description,
            String status,
            Branch branch,
            int tier,
            String prerequisiteId,
            int worldX,
            int worldY) {
    }
}
