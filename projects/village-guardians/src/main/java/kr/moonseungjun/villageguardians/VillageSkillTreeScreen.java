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
    private static final int PANEL = 0xF20C131B;
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

    private static double savedPanX;
    private static double savedPanY;
    private static double savedZoom = 1.0;

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
        renderHeader(graphics, mouseX, mouseY);
        renderDetail(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGrid(GuiGraphicsExtractor graphics, Viewport viewport) {
        graphics.fill(viewport.left(), viewport.top(), viewport.right(), viewport.bottom(), BACKGROUND);
        int spacing = Math.max(18, (int) Math.round(30 * savedZoom));
        int startX = viewport.left() + Math.floorMod((int) Math.round(savedPanX), spacing);
        int startY = viewport.top() + Math.floorMod((int) Math.round(savedPanY), spacing);
        for (int x = startX; x < viewport.right(); x += spacing) graphics.fill(x, viewport.top(), x + 1, viewport.bottom(), GRID);
        for (int y = startY; y < viewport.bottom(); y += spacing) graphics.fill(viewport.left(), y, viewport.right(), y + 1, GRID);
    }

    private void renderConnections(GuiGraphicsExtractor graphics, Viewport viewport) {
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        for (NodeVisual node : nodes) {
            NodeVisual previous = previous(node);
            double x0 = previous == null ? 0 : previous.worldX();
            double y0 = previous == null ? 0 : previous.worldY();
            int color = previous == null || "습득".equals(previous.status()) ? branchColor(node.branch()) : 0xFF334250;
            drawLine(graphics,
                    screenX(viewport, x0), screenY(viewport, y0),
                    screenX(viewport, node.worldX()), screenY(viewport, node.worldY()), color);
        }
        graphics.disableScissor();
    }

    private void renderCore(GuiGraphicsExtractor graphics, Viewport viewport) {
        int size = scaledNodeSize() - 8;
        int cx = screenX(viewport, 0);
        int cy = screenY(viewport, 0);
        int x = cx - size / 2;
        int y = cy - size / 2;
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        graphics.fill(x - 3, y - 3, x + size + 3, y + size + 3, GOLD);
        graphics.fill(x, y, x + size, y + size, 0xFF202A31);
        graphics.fill(cx - 3, y + 6, cx + 3, y + size - 6, GOLD);
        graphics.fill(x + 6, cy - 3, x + size - 6, cy + 3, GOLD);
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
            if (x + nodeSize < viewport.left() || x > viewport.right() || y + nodeSize < viewport.top() || y > viewport.bottom()) continue;
            int color = branchColor(node.branch());
            int border = switch (node.status()) {
                case "습득" -> color;
                case "습득 가능" -> GOLD;
                case "데이터 잠금" -> RED;
                default -> 0xFF354552;
            };
            boolean hovered = inside(mouseX, mouseY, x, y, nodeSize, nodeSize);
            boolean selected = selectedIndex == index;
            graphics.fill(x - 3, y - 3, x + nodeSize + 3, y + nodeSize + 3, selected ? GOLD : hovered ? border : 0xFF18242E);
            graphics.fill(x - 1, y - 1, x + nodeSize + 1, y + nodeSize + 1, border);
            graphics.fill(x, y, x + nodeSize, y + nodeSize, hovered || selected ? 0xFF1C2A35 : SURFACE);
            drawNodeIcon(graphics, node.branch(), node.tier(), x + 6, y + 6, nodeSize - 12,
                    "습득".equals(node.status()) ? color : border);
            if (savedZoom >= 0.82) {
                String title = compact(node.title(), savedZoom >= 1.18 ? 16 : 10);
                graphics.centeredText(font, title, cx, y + nodeSize + 6, hovered || selected ? TEXT : MUTED);
            }
        }
        graphics.disableScissor();
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, 48, PANEL);
        graphics.fill(0, 46, width, 48, BORDER);
        graphics.text(font, "전술 발전", 15, 11, TEXT, false);
        graphics.text(font, compact(payload.body(), Math.max(24, Math.min(58, width / 10))), 15, 28, MUTED, false);
        int closeX = width - 35;
        int centerX = closeX - 53;
        int plusX = centerX - 34;
        int percentX = plusX - 48;
        int minusX = percentX - 34;
        drawHeaderButton(graphics, mouseX, mouseY, minusX, 10, 28, "−");
        graphics.centeredText(font, Math.round(savedZoom * 100) + "%", percentX + 22, 18, MUTED);
        drawHeaderButton(graphics, mouseX, mouseY, plusX, 10, 28, "+");
        drawHeaderButton(graphics, mouseX, mouseY, centerX, 10, 46, "중앙");
        drawHeaderButton(graphics, mouseX, mouseY, closeX, 10, 26, "×");
    }

    private void drawHeaderButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int w, String label) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, 25);
        graphics.fill(x, y, x + w, y + 25, hovered ? 0xFF253642 : 0xFF101923);
        graphics.centeredText(font, label, x + w / 2, y + 8, hovered ? GOLD : MUTED);
    }

    private void renderDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int top = Math.max(112, height - 88);
        graphics.fill(0, top, width, height, PANEL);
        graphics.fill(0, top, width, top + 2, BORDER);
        String title = "노드를 선택하세요";
        String description = "빈 공간 드래그: 이동 · 마우스 휠 또는 +/−: 확대·축소";
        String status = "";
        int color = MUTED;
        if (selectedIndex >= 0 && selectedIndex < nodes.size()) {
            NodeVisual node = nodes.get(selectedIndex);
            title = node.title();
            description = node.description();
            status = node.status();
            color = branchColor(node.branch());
        }
        graphics.text(font, compact(title, Math.max(18, width / 10)), 16, top + 12, color, false);
        if (!status.isBlank()) graphics.text(font, status, 16, top + 29, statusColor(status), false);
        int buttonWidth = Math.min(126, Math.max(92, width / 5));
        int buttonX = width - buttonWidth - 16;
        int buttonY = top + 29;
        int descriptionRight = buttonX - 12;
        List<FormattedCharSequence> lines = font.split(Component.literal(description), Math.max(80, descriptionRight - 16));
        int y = top + 48;
        for (FormattedCharSequence line : lines) {
            if (y > height - 10) break;
            graphics.text(font, line, 16, y, MUTED, false);
            y += 11;
        }
        boolean purchasable = selectedIndex >= 0 && "습득 가능".equals(nodes.get(selectedIndex).status());
        boolean hovered = purchasable && inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 25);
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonWidth + 1, buttonY + 26,
                hovered ? GOLD : purchasable ? color : 0xFF35424D);
        graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + 25,
                hovered ? 0xFF3C3420 : purchasable ? SURFACE : 0xFF171E25);
        String label = purchasable ? "습득 · 1P" : selectedIndex < 0 ? "노드 선택 필요" : nodes.get(selectedIndex).status();
        graphics.centeredText(font, label, buttonX + buttonWidth / 2, buttonY + 8, purchasable ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int closeX = width - 35;
        int centerX = closeX - 53;
        int plusX = centerX - 34;
        int percentX = plusX - 48;
        int minusX = percentX - 34;
        if (inside(click.x(), click.y(), closeX, 10, 26, 25)) { onClose(); return true; }
        if (inside(click.x(), click.y(), centerX, 10, 46, 25)) { savedPanX = 0; savedPanY = 0; savedZoom = 1.0; return true; }
        if (inside(click.x(), click.y(), minusX, 10, 28, 25)) { setZoom(savedZoom - 0.15, width / 2.0, height / 2.0); return true; }
        if (inside(click.x(), click.y(), plusX, 10, 28, 25)) { setZoom(savedZoom + 0.15, width / 2.0, height / 2.0); return true; }

        int top = Math.max(112, height - 88);
        int buttonWidth = Math.min(126, Math.max(92, width / 5));
        int buttonX = width - buttonWidth - 16;
        if (selectedIndex >= 0 && "습득 가능".equals(nodes.get(selectedIndex).status())
                && inside(click.x(), click.y(), buttonX, top + 29, buttonWidth, 25)) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(nodes.get(selectedIndex).action()));
            return true;
        }

        Viewport viewport = viewport();
        if (!inside(click.x(), click.y(), viewport.left(), viewport.top(), viewport.width(), viewport.height())) return super.mouseClicked(click, doubled);
        int nodeSize = scaledNodeSize();
        for (int index = 0; index < nodes.size(); index++) {
            NodeVisual node = nodes.get(index);
            int x = screenX(viewport, node.worldX()) - nodeSize / 2;
            int y = screenY(viewport, node.worldY()) - nodeSize / 2;
            if (inside(click.x(), click.y(), x, y, nodeSize, nodeSize)) { selectedIndex = index; return true; }
        }
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
            setZoom(savedZoom + vertical * 0.10, mouseX, mouseY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void setZoom(double requested, double mouseX, double mouseY) {
        Viewport viewport = viewport();
        double old = savedZoom;
        double next = clamp(requested, 0.55, 1.75);
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
            String[] parts = labels[index].split("\\|", 3);
            String title = parts.length > 0 ? parts[0] : labels[index];
            String description = parts.length > 1 ? parts[1] : "상세 효과 없음";
            String status = parts.length > 2 ? parts[2] : "잠김";
            String id = actions[index].startsWith("skill_node:") ? actions[index].substring(11) : actions[index];
            Branch branch = branch(id);
            int tier = tier(id);
            double distance = 105.0 + tier * 13.0;
            double worldX = switch (branch) {
                case POWER -> tier * distance;
                case GUARD -> -tier * distance;
                default -> 0;
            };
            double worldY = switch (branch) {
                case RANGED -> -tier * distance;
                case SUPPORT -> tier * distance;
                default -> 0;
            };
            nodes.add(new NodeVisual(actions[index], id, title, description, status, branch, tier, worldX, worldY));
        }
    }

    private NodeVisual previous(NodeVisual node) {
        if (node.tier() <= 1) return null;
        for (NodeVisual candidate : nodes) if (candidate.branch() == node.branch() && candidate.tier() == node.tier() - 1) return candidate;
        return null;
    }

    private Branch branch(String id) {
        if (id.startsWith("guard_")) return Branch.GUARD;
        if (id.startsWith("support_")) return Branch.SUPPORT;
        if (id.startsWith("ranged_")) return Branch.RANGED;
        return Branch.POWER;
    }

    private int tier(String id) {
        int split = id.lastIndexOf('_');
        if (split >= 0) try { return Math.max(1, Integer.parseInt(id.substring(split + 1))); } catch (NumberFormatException ignored) {}
        return 1;
    }

    private int screenX(Viewport viewport, double worldX) {
        return (int) Math.round((viewport.left() + viewport.right()) / 2.0 + savedPanX + worldX * savedZoom);
    }

    private int screenY(Viewport viewport, double worldY) {
        return (int) Math.round((viewport.top() + viewport.bottom()) / 2.0 + savedPanY + worldY * savedZoom);
    }

    private int scaledNodeSize() { return (int) Math.round(clamp(44 * savedZoom, 28, 62)); }

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

    private void drawNodeIcon(GuiGraphicsExtractor graphics, Branch branch, int tier, int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        switch (branch) {
            case POWER -> { graphics.fill(cx - 2, y + 2, cx + 2, y + size - 3, color); graphics.fill(cx - 7, y + 3, cx + 7, y + 7, color); }
            case GUARD -> { graphics.fill(cx - 8, y + 4, cx + 8, cy + 2, color); graphics.fill(cx - 5, cy, cx + 5, y + size - 4, color); }
            case RANGED -> { graphics.fill(x + 4, cy - 1, x + size - 4, cy + 1, color); graphics.fill(x + size - 8, cy - 5, x + size - 4, cy + 5, color); }
            case SUPPORT -> { graphics.fill(cx - 3, y + 3, cx + 3, y + size - 3, color); graphics.fill(x + 3, cy - 3, x + size - 3, cy + 3, color); }
        }
        if (tier >= 4) graphics.fill(x + 3, y + 3, x + 7, y + 7, GOLD);
    }

    private int branchColor(Branch branch) {
        return switch (branch) { case POWER -> RED; case GUARD -> BLUE; case RANGED -> PURPLE; case SUPPORT -> GREEN; };
    }

    private int statusColor(String status) {
        return switch (status) { case "습득" -> ACCENT; case "습득 가능" -> GOLD; case "데이터 잠금" -> RED; default -> MUTED; };
    }

    private Viewport viewport() { return new Viewport(0, 48, width, Math.max(49, height - 88)); }

    private String compact(String value, int max) {
        String normalized = value.replace('\n', ' ');
        return normalized.length() <= max ? normalized : normalized.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private enum Branch { POWER, GUARD, RANGED, SUPPORT }
    private record Viewport(int left, int top, int right, int bottom) { int width() { return right - left; } int height() { return bottom - top; } }
    private record NodeVisual(String action, String id, String title, String description, String status, Branch branch, int tier, double worldX, double worldY) {}
}
