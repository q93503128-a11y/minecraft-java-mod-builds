package kr.moonseungjun.anomalousclasses.client;

import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.network.payload.RequestProfilePayload;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeStatePayload;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.GraphSkillTreeCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/** Full-screen, data-driven graph map. Node art and class backdrops are separate runtime assets. */
public final class SkillTreeScreen extends Screen {
    private static final double MIN_ZOOM = 0.24;
    private static final double MAX_ZOOM = 1.85;
    private static final double DRAG_THRESHOLD_SQUARED = 16.0;
    private static final int HUD_HEIGHT = 34;

    private SkillTreeStatePayload state;
    private final SkillTreeNavigation.ReturnTarget returnTarget;
    private ClassId classId;
    private GraphSkillTreeCatalog.Tree tree;
    private MenuUi.Box viewport;
    private MenuUi.Button profileButton;
    private MenuUi.Button resetButton;
    private final Map<String, ResourceLocation> iconTextures = new HashMap<>();
    private final Map<String, GraphSkillTreeCatalog.PurchaseCheck> purchaseChecks = new HashMap<>();
    private final Map<String, ScreenNode> projectedById = new HashMap<>();
    private List<ScreenNode> visibleNodes = List.of();
    private List<VisibleEdge> visibleEdges = List.of();
    private Set<String> owned = Set.of();
    private String selectedNodeId;
    private double cameraX;
    private double cameraY;
    private double zoom = 0.5;
    private boolean cameraInitialized;
    private boolean mapPointerDown;
    private boolean dragging;
    private int pointerButton = -1;
    private double pressX;
    private double pressY;
    private double lastX;
    private double lastY;
    private String pressedNodeId;
    private boolean pending;
    private long resetConfirmUntil;
    private long feedbackUntil;

    public SkillTreeScreen(SkillTreeStatePayload state, SkillTreeNavigation.ReturnTarget returnTarget) {
        super(Component.translatable("screen.anomalousclasses.skill_tree.title"));
        this.state = state;
        this.returnTarget = returnTarget;
        this.classId = ClassId.fromNetworkIndex(state.classIndex()).orElse(ClassId.MENTAL_DOMINATOR);
        this.tree = GraphSkillTreeCatalog.tree(classId);
        rebuildOwnership();
    }

    @Override
    protected void init() {
        clearWidgets();
        int mapTop = Math.min(Math.max(25, HUD_HEIGHT), Math.max(1, height - 1));
        viewport = new MenuUi.Box(0, mapTop, Math.max(1, width), Math.max(1, height - mapTop));
        rebuildIconTextures();
        if (!cameraInitialized) {
            double graphWidth = Math.max(1, tree.maxX() - tree.minX() + 180);
            double graphHeight = Math.max(1, tree.maxY() - tree.minY() + 180);
            zoom = Math.max(MIN_ZOOM, Math.min(0.58,
                    Math.min(viewport.width() / graphWidth, viewport.height() / graphHeight) * 0.9));
            cameraX = (tree.minX() + tree.maxX()) * 0.5;
            cameraY = (tree.minY() + tree.maxY()) * 0.5;
            cameraInitialized = true;
        }

        int margin = width < 420 ? 5 : 9;
        int backWidth = Math.min(76, Math.max(54, width / 7));
        int resetWidth = Math.min(78, Math.max(54, width / 7));
        int spWidth = Math.min(112, Math.max(78, width / 4));
        int rightX = Math.max(margin, width - margin - resetWidth - 4 - spWidth);
        int availableClassWidth = Math.max(30, rightX - margin - backWidth - 4 - margin);
        int classWidth = Math.min(158, availableClassWidth);
        profileButton = addRenderableWidget(new MenuUi.Button(
                new MenuUi.Box(margin, 7, backWidth, 21),
                Component.translatable(returnTarget == SkillTreeNavigation.ReturnTarget.PROFILE
                        ? "screen.anomalousclasses.skill_tree.back"
                        : "screen.anomalousclasses.skill_tree.close"), button -> returnToOrigin()));
        resetButton = addRenderableWidget(new MenuUi.Button(
                new MenuUi.Box(width - margin - resetWidth, 7, resetWidth, 21),
                resetLabel(), button -> confirmOrReset()));
        clampCamera();
        refreshPurchaseChecks();
        rebuildCameraCache();
        refreshControls();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshControls();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        drawBackdrop(graphics);
        drawMap(graphics);
        drawHud(graphics);
        drawHoveredCard(graphics, mouseX, mouseY);
        drawFeedback(graphics);
        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBackdrop(GuiGraphics graphics) {
        ResourceLocation background = texture("backgrounds/" + classId.key() + ".png");
        int sourceWidth = 1672;
        int sourceHeight = 941;
        double cover = Math.max(width / (double) sourceWidth, height / (double) sourceHeight);
        int drawWidth = (int) Math.ceil(sourceWidth * cover);
        int drawHeight = (int) Math.ceil(sourceHeight * cover);
        graphics.blit(background, (width - drawWidth) / 2, (height - drawHeight) / 2,
                drawWidth, drawHeight, 0, 0, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
        graphics.fill(viewport.x(), viewport.y(), viewport.right(), viewport.bottom(), 0x26080712);
    }

    private void drawMap(GuiGraphics graphics) {
        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        for (VisibleEdge edge : visibleEdges) drawEdge(graphics, edge);
        for (ScreenNode screenNode : visibleNodes) drawNode(graphics, screenNode);
        graphics.disableScissor();
    }

    private void drawEdge(GuiGraphics graphics, VisibleEdge visible) {
        ScreenNode from = visible.from();
        ScreenNode to = visible.to();
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double length = Math.hypot(dx, dy);
        if (length < 1) return;
        double nx = dx / length;
        double ny = dy / length;
        double startOffset = from.size() * (from.node().kind() == GraphSkillTreeCatalog.Kind.MAJOR ? 0.40 : 0.34);
        double endOffset = to.size() * (to.node().kind() == GraphSkillTreeCatalog.Kind.MAJOR ? 0.40 : 0.34);
        double x1 = from.x() + nx * startOffset;
        double y1 = from.y() + ny * startOffset;
        double x2 = to.x() - nx * endOffset;
        double y2 = to.y() - ny * endOffset;
        double bend = Math.min(18.0, Math.hypot(x2 - x1, y2 - y1) * 0.105);
        double sign = ((visible.edge().from().hashCode() ^ visible.edge().to().hashCode()) & 1) == 0 ? 1 : -1;
        double px = -ny * bend * sign;
        double py = nx * bend * sign;
        double c1x = x1 + (x2 - x1) * 0.32 + px;
        double c1y = y1 + (y2 - y1) * 0.32 + py;
        double c2x = x1 + (x2 - x1) * 0.68 + px;
        double c2y = y1 + (y2 - y1) * 0.68 + py;
        int color = visible.active() ? MenuUi.accent(classId) :
                (visible.edge().type().equals("navigation") ? 0x7FDDC59F : 0x807B6B89);
        double prevX = x1;
        double prevY = y1;
        int segments = Math.max(3, Math.min(8, (int) Math.ceil(length / 18.0)));
        for (int segment = 1; segment <= segments; segment++) {
            double t = segment / (double) segments;
            double oneMinus = 1.0 - t;
            double x = oneMinus * oneMinus * oneMinus * x1 + 3 * oneMinus * oneMinus * t * c1x
                    + 3 * oneMinus * t * t * c2x + t * t * t * x2;
            double y = oneMinus * oneMinus * oneMinus * y1 + 3 * oneMinus * oneMinus * t * c1y
                    + 3 * oneMinus * t * t * c2y + t * t * t * y2;
            drawLineSegment(graphics, prevX, prevY, x, y, color, visible.active());
            prevX = x;
            prevY = y;
        }
    }

    private void drawLineSegment(GuiGraphics graphics, double x1, double y1, double x2, double y2,
                                 int color, boolean active) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        if (length < 0.2) return;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x1, y1, 0);
        pose.mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        if (active) graphics.fill(0, -2, (int) Math.ceil(length), 2, withAlpha(MenuUi.accent(classId), 0x48));
        graphics.fill(0, -1, (int) Math.ceil(length), 1, color);
        pose.popPose();
    }

    private void drawNode(GuiGraphics graphics, ScreenNode projected) {
        GraphSkillTreeCatalog.Node node = projected.node();
        int size = projected.size();
        int x = (int) Math.round(projected.x() - size / 2.0);
        int y = (int) Math.round(projected.y() - size / 2.0);
        if (node.kind() == GraphSkillTreeCatalog.Kind.NAVIGATION) {
            drawNavigationNode(graphics, node, x, y, size);
            return;
        }
        boolean isOwned = owned.contains(node.id());
        boolean isHighlighted = node.id().equals(selectedNodeId) || node.id().equals(hoveredNodeId);
        GraphSkillTreeCatalog.PurchaseCheck check = purchaseChecks.getOrDefault(node.id(),
                GraphSkillTreeCatalog.PurchaseCheck.NOT_CONNECTED);
        int frameState = isHighlighted ? 2 : isOwned ? 3 :
                check == GraphSkillTreeCatalog.PurchaseCheck.PURCHASABLE ? 1 : 0;
        if (node.major() && node.milestone()) {
            int outer = size + Math.max(3, size / 9);
            graphics.fill((int) Math.round(projected.x() - outer / 2.0),
                    (int) Math.round(projected.y() - outer / 2.0),
                    (int) Math.round(projected.x() + outer / 2.0),
                    (int) Math.round(projected.y() + outer / 2.0), isOwned ? 0xB7E4C17D : 0x73735C79);
        }
        graphics.fill(x + size / 4, y + size / 4, x + size * 3 / 4, y + size * 3 / 4,
                isOwned ? 0xE321132A : 0xD20D0915);
        MenuUi.drawNodeFrame(graphics, frameState, x, y, size);
        ResourceLocation icon = iconTextures.get(node.id());
        if (icon != null) {
            int iconSize = Math.max(8, size * (node.major() ? 58 : 61) / 100);
            graphics.blit(icon, (int) Math.round(projected.x() - iconSize / 2.0),
                    (int) Math.round(projected.y() - iconSize / 2.0), iconSize, iconSize,
                    0, 0, 256, 256, 256, 256);
        }
    }

    private void drawNavigationNode(GuiGraphics graphics, GraphSkillTreeCatalog.Node node, int x, int y, int size) {
        if (node.id().equals("origin")) {
            int iconSize = Math.max(18, size);
            MenuUi.drawClassIcon(graphics, classId, x + (size - iconSize) / 2, y + (size - iconSize) / 2, iconSize);
            return;
        }
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int outer = Math.max(4, size / 3);
        graphics.fill(centerX - outer, centerY - outer, centerX + outer + 1, centerY + outer + 1,
                withAlpha(MenuUi.accent(classId), 0xA8));
        graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, 0xFFF4E6CB);
    }

    private void drawHud(GuiGraphics graphics) {
        int margin = width < 420 ? 5 : 9;
        int spWidth = Math.min(112, Math.max(78, width / 4));
        int spX = resetButton.getX() - 4 - spWidth;
        int leftX = profileButton.getX() + profileButton.getWidth() + 4;
        int classRight = Math.max(leftX, spX - 6);
        int classWidth = Math.max(24, classRight - leftX);
        MenuUi.drawPanel(graphics, new MenuUi.Box(spX, 6, spWidth, 23));
        MenuUi.drawClassIcon(graphics, classId, leftX + 1, 8, 18);
        Component className = Component.translatable("class.anomalousclasses." + classId.key());
        MenuUi.drawFit(graphics, font, className, leftX + 23, 13, Math.max(1, classWidth - 24), 0xFFF4E8D4);
        Component points = Component.translatable("screen.anomalousclasses.skill_tree.points", state.skillPoints());
        Component spent = Component.translatable("screen.anomalousclasses.skill_tree.spent", state.spentPoints());
        int lineY = font.lineHeight <= 9 ? 8 : 7;
        MenuUi.drawFit(graphics, font, points, spX + 6, lineY, spWidth - 12, 0xFFF2E7DB);
        MenuUi.drawFit(graphics, font, spent, spX + 6, lineY + font.lineHeight, spWidth - 12, 0xFFCDBDD6);
    }

    private void drawHoveredCard(GuiGraphics graphics, int mouseX, int mouseY) {
        ScreenNode hovered = hoveredNode(mouseX, mouseY);
        if (hovered == null || hovered.node().kind() == GraphSkillTreeCatalog.Kind.NAVIGATION) return;
        GraphSkillTreeCatalog.Node node = hovered.node();
        List<CardLine> lines = tooltipLines(node);
        int maxWidth = Math.max(1, Math.min(160, width - 12));
        int measuredWidth = lines.stream().mapToInt(line -> font.width(line.text())).max().orElse(1);
        int minWidth = Math.min(116, maxWidth);
        int cardWidth = Math.max(minWidth, Math.min(maxWidth, measuredWidth + 20));
        int textWidth = Math.max(1, cardWidth - 16);
        List<WrappedLine> wrapped = new ArrayList<>();
        for (CardLine line : lines) {
            List<net.minecraft.util.FormattedCharSequence> split = font.split(line.text(), textWidth);
            for (net.minecraft.util.FormattedCharSequence part : split) wrapped.add(new WrappedLine(part, line.color()));
        }
        int maxCardLines = Math.max(3, (height - HUD_HEIGHT - 18) / (font.lineHeight + 2));
        if (wrapped.size() > maxCardLines) {
            wrapped = new ArrayList<>(wrapped.subList(0, maxCardLines));
            wrapped.set(maxCardLines - 1, new WrappedLine(font.split(Component.literal("…"), textWidth).get(0),
                    wrapped.get(maxCardLines - 1).color()));
        }
        int cardHeight = 10 + wrapped.size() * (font.lineHeight + 2);
        int nodeRight = (int) Math.round(hovered.x() + hovered.size() / 2.0);
        int nodeLeft = (int) Math.round(hovered.x() - hovered.size() / 2.0);
        int cardX = nodeRight + cardWidth + 12 <= width ? nodeRight + 9 : nodeLeft - cardWidth - 9;
        cardX = Math.max(6, Math.min(Math.max(6, width - cardWidth - 6), cardX));
        int cardY = Math.max(viewport.y() + 3, Math.min(height - cardHeight - 5, mouseY + 8));
        if (cardY < HUD_HEIGHT + 2) cardY = Math.min(height - cardHeight - 5, HUD_HEIGHT + 3);
        MenuUi.drawPanel(graphics, new MenuUi.Box(cardX, cardY, cardWidth, cardHeight));
        int y = cardY + 6;
        for (WrappedLine line : wrapped) {
            graphics.drawString(font, line.text(), cardX + 8, y, line.color(), true);
            y += font.lineHeight + 2;
        }
    }

    private List<CardLine> tooltipLines(GraphSkillTreeCatalog.Node node) {
        List<CardLine> lines = new ArrayList<>();
        lines.add(new CardLine(Component.translatable(node.nameKey(classId)), 0xFFFFE8BF));
        if (!node.major()) lines.add(new CardLine(Component.translatable(
                "screen.anomalousclasses.skill_tree.node_stat", statAmount(node)), 0xFFE4D8C9));
        if (node.major()) {
            lines.add(new CardLine(Component.translatable(node.effectKey(classId)), 0xFFE7DFD4));
        }
        lines.add(new CardLine(Component.translatable("screen.anomalousclasses.skill_tree.node_cost", node.cost()), 0xFFF3D48D));
        GraphSkillTreeCatalog.PurchaseCheck check = purchaseChecks.getOrDefault(node.id(),
                GraphSkillTreeCatalog.PurchaseCheck.NOT_CONNECTED);
        lines.add(new CardLine(Component.translatable(statusKey(check)), statusColor(check)));
        return lines;
    }

    private String statAmount(GraphSkillTreeCatalog.Node node) {
        double amount = GraphSkillTreeCatalog.statAmount(classId, node.stat());
        String formatted = String.format(Locale.ROOT, "%.2f", amount);
        return Component.translatable("screen.anomalousclasses.skill_tree.stat." + node.stat(), formatted).getString();
    }

    private String statusKey(GraphSkillTreeCatalog.PurchaseCheck check) {
        return switch (check) {
            case PURCHASABLE -> "screen.anomalousclasses.skill_tree.ready";
            case ALREADY_OWNED -> "screen.anomalousclasses.skill_tree.node_owned";
            case INSUFFICIENT_SP -> "screen.anomalousclasses.skill_tree.insufficient";
            case MISSING_REQUIREMENT, NOT_CONNECTED -> "screen.anomalousclasses.skill_tree.requirement_missing";
            case UNKNOWN_NODE, NOT_PURCHASABLE -> "screen.anomalousclasses.skill_tree.rejected";
        };
    }

    private int statusColor(GraphSkillTreeCatalog.PurchaseCheck check) {
        return switch (check) {
            case PURCHASABLE -> 0xFFBDE9AD;
            case ALREADY_OWNED -> 0xFFBDE9AD;
            case INSUFFICIENT_SP, MISSING_REQUIREMENT, NOT_CONNECTED -> 0xFFF1D18A;
            case UNKNOWN_NODE, NOT_PURCHASABLE -> 0xFFE5A39A;
        };
    }

    private void drawFeedback(GuiGraphics graphics) {
        if (!pending && System.currentTimeMillis() > feedbackUntil) return;
        String key = switch (state.result()) {
            case UPDATED -> "screen.anomalousclasses.skill_tree.saved";
            case RESET -> "screen.anomalousclasses.skill_tree.reset_done";
            case INSUFFICIENT_SP -> "screen.anomalousclasses.skill_tree.insufficient";
            case ALREADY_OWNED -> "screen.anomalousclasses.skill_tree.node_owned";
            case MISSING_REQUIREMENT, NOT_CONNECTED -> "screen.anomalousclasses.skill_tree.requirement_missing";
            case COMBAT_LOCKED -> "screen.anomalousclasses.skill_tree.combat_lock";
            case STALE_REQUEST -> "screen.anomalousclasses.skill_tree.stale";
            case INVALID_ACTION, INVALID_CLASS, INVALID_NODE -> "screen.anomalousclasses.skill_tree.rejected";
            case MIGRATION_FAILED -> "screen.anomalousclasses.skill_tree.migration_failed";
            default -> pending ? "screen.anomalousclasses.skill_tree.pending" : null;
        };
        if (key == null) return;
        Component text = Component.translatable(key);
        int maxWidth = Math.max(1, Math.min(width - 12, 260));
        int x = (width - Math.min(maxWidth, font.width(text) + 20)) / 2;
        int y = height - font.lineHeight - 18;
        int w = Math.min(maxWidth, font.width(text) + 20);
        MenuUi.drawPanel(graphics, new MenuUi.Box(x, y, w, font.lineHeight + 12));
        MenuUi.drawCentered(graphics, font, text, width / 2, y + 5, w - 12, 0xFFF2E7D9);
    }

    private void rebuildOwnership() {
        Set<String> values = new HashSet<>();
        for (int index : state.purchasedNodeIndexes()) {
            GraphSkillTreeCatalog.Node node = tree.purchaseNode(index);
            if (node != null) values.add(node.id());
        }
        owned = Set.copyOf(values);
        refreshPurchaseChecks();
    }

    private void rebuildIconTextures() {
        iconTextures.clear();
        for (GraphSkillTreeCatalog.Node node : tree.purchaseNodes()) {
            String path = node.iconPath(classId);
            if (path != null) iconTextures.put(node.id(),
                    ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, path));
        }
    }

    private void refreshPurchaseChecks() {
        purchaseChecks.clear();
        for (GraphSkillTreeCatalog.Node node : tree.purchaseNodes()) {
            purchaseChecks.put(node.id(), GraphSkillTreeCatalog.canPurchase(classId, owned, state.skillPoints(), node.id()));
        }
    }

    private void rebuildCameraCache() {
        projectedById.clear();
        List<ScreenNode> nextNodes = new ArrayList<>();
        for (GraphSkillTreeCatalog.Node node : tree.allNodes()) {
            double x = screenX(node.x());
            double y = screenY(node.y());
            int baseSize = switch (node.kind()) {
                case NAVIGATION -> node.id().equals("origin") ? 46 : 26;
                case MINOR -> 30;
                case MAJOR -> node.milestone() ? 49 : 43;
            };
            int size = Math.max(node.kind() == GraphSkillTreeCatalog.Kind.MAJOR ? 17 : 14,
                    Math.min(64, (int) Math.round(baseSize * zoom)));
            ScreenNode projected = new ScreenNode(node, x, y, size);
            projectedById.put(node.id(), projected);
            if (x + size >= viewport.x() && x - size <= viewport.right()
                    && y + size >= viewport.y() && y - size <= viewport.bottom()) nextNodes.add(projected);
        }
        nextNodes.sort(Comparator.comparingInt(value -> switch (value.node().kind()) {
            case NAVIGATION -> 0;
            case MINOR -> 1;
            case MAJOR -> 2;
        }));
        visibleNodes = List.copyOf(nextNodes);

        List<VisibleEdge> nextEdges = new ArrayList<>();
        for (GraphSkillTreeCatalog.Edge edge : tree.edges()) {
            ScreenNode from = projectedById.get(edge.from());
            ScreenNode to = projectedById.get(edge.to());
            if (from == null || to == null) continue;
            double left = Math.min(from.x(), to.x());
            double right = Math.max(from.x(), to.x());
            double top = Math.min(from.y(), to.y());
            double bottom = Math.max(from.y(), to.y());
            if (right < viewport.x() || left > viewport.right() || bottom < viewport.y() || top > viewport.bottom()) continue;
            boolean active = owned.contains(edge.from()) && owned.contains(edge.to());
            if (!active && purchaseChecks.get(edge.to()) == GraphSkillTreeCatalog.PurchaseCheck.PURCHASABLE) active = true;
            nextEdges.add(new VisibleEdge(edge, from, to, active));
        }
        visibleEdges = List.copyOf(nextEdges);
    }

    private double screenX(double worldX) {
        return viewport.x() + viewport.width() * 0.5 + (worldX - cameraX) * zoom;
    }

    private double screenY(double worldY) {
        return viewport.y() + viewport.height() * 0.5 + (worldY - cameraY) * zoom;
    }

    private double worldX(double screenX) {
        return cameraX + (screenX - (viewport.x() + viewport.width() * 0.5)) / zoom;
    }

    private double worldY(double screenY) {
        return cameraY + (screenY - (viewport.y() + viewport.height() * 0.5)) / zoom;
    }

    private ScreenNode hoveredNode(double mouseX, double mouseY) {
        return hitNode(mouseX, mouseY);
    }

    private String hoveredNodeId;

    private ScreenNode hitNode(double mouseX, double mouseY) {
        if (viewport == null || !viewport.contains(mouseX, mouseY)) return null;
        for (int index = visibleNodes.size() - 1; index >= 0; index--) {
            ScreenNode node = visibleNodes.get(index);
            if (node.node().kind() == GraphSkillTreeCatalog.Kind.NAVIGATION) continue;
            double halfSize = node.size() * 0.5;
            if (Math.abs(mouseX - node.x()) <= halfSize && Math.abs(mouseY - node.y()) <= halfSize) return node;
        }
        return null;
    }

    private void clampCamera() {
        if (viewport == null || zoom <= 0) return;
        double halfWidth = viewport.width() * 0.5 / zoom;
        double halfHeight = viewport.height() * 0.5 / zoom;
        double minX = tree.minX() - halfWidth + 32;
        double maxX = tree.maxX() + halfWidth - 32;
        double minY = tree.minY() - halfHeight + 32;
        double maxY = tree.maxY() + halfHeight - 32;
        cameraX = minX > maxX ? (tree.minX() + tree.maxX()) * 0.5 : Math.max(minX, Math.min(maxX, cameraX));
        cameraY = minY > maxY ? (tree.minY() + tree.maxY()) * 0.5 : Math.max(minY, Math.min(maxY, cameraY));
    }

    private void zoomAt(double mouseX, double mouseY, double scroll) {
        double beforeX = worldX(mouseX);
        double beforeY = worldY(mouseY);
        double factor = scroll > 0 ? 1.14 : 1.0 / 1.14;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        cameraX = beforeX - (mouseX - viewport.width() * 0.5) / zoom;
        cameraY = beforeY - (mouseY - (viewport.y() + viewport.height() * 0.5)) / zoom;
        clampCamera();
        rebuildCameraCache();
    }

    private void confirmOrReset() {
        if (pending || state.combatLocked()) return;
        long now = System.currentTimeMillis();
        if (now > resetConfirmUntil) {
            resetConfirmUntil = now + 4000;
            resetButton.setMessage(Component.translatable("screen.anomalousclasses.skill_tree.confirm_reset"));
            return;
        }
        resetConfirmUntil = 0;
        submit(SkillTreeActionPayload.Action.RESET, -1);
    }

    private void submit(SkillTreeActionPayload.Action action, int nodeIndex) {
        if (pending) return;
        pending = true;
        resetConfirmUntil = 0;
        refreshControls();
        PacketDistributor.sendToServer(new SkillTreeActionPayload(classId.networkIndex(), nodeIndex,
                action.ordinal(), state.revision()));
    }

    private void requestProfile() {
        pending = true;
        refreshControls();
        PacketDistributor.sendToServer(RequestProfilePayload.INSTANCE);
    }

    private void returnToOrigin() {
        if (returnTarget == SkillTreeNavigation.ReturnTarget.PROFILE) requestProfile();
        else onClose();
    }

    private void refreshControls() {
        if (profileButton != null) profileButton.active = !pending;
        if (resetButton != null) {
            resetButton.active = !pending && !state.combatLocked();
            if (System.currentTimeMillis() > resetConfirmUntil) {
                resetButton.setMessage(Component.translatable("screen.anomalousclasses.skill_tree.reset"));
            }
        }
    }

    public void applyServerState(SkillTreeStatePayload updated) {
        ClassId newClass = ClassId.fromNetworkIndex(updated.classIndex()).orElse(classId);
        boolean changedClass = newClass != classId;
        state = updated;
        pending = false;
        if (updated.result() != SkillTreeStatePayload.Result.OPEN && updated.result() != SkillTreeStatePayload.Result.STATUS) {
            feedbackUntil = System.currentTimeMillis() + 2400;
        }
        if (changedClass) {
            classId = newClass;
            tree = GraphSkillTreeCatalog.tree(classId);
            cameraInitialized = false;
            init();
            return;
        }
        rebuildOwnership();
        if (viewport != null) rebuildCameraCache();
        refreshControls();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 || viewport == null || !viewport.contains(mouseX, mouseY)) return false;
        mapPointerDown = true;
        pointerButton = button;
        pressX = lastX = mouseX;
        pressY = lastY = mouseY;
        ScreenNode node = hitNode(mouseX, mouseY);
        pressedNodeId = node == null ? null : node.node().id();
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (mapPointerDown && button == pointerButton) {
            double totalX = mouseX - pressX;
            double totalY = mouseY - pressY;
            if (!dragging && totalX * totalX + totalY * totalY > DRAG_THRESHOLD_SQUARED) dragging = true;
            if (dragging) {
                cameraX -= (mouseX - lastX) / zoom;
                cameraY -= (mouseY - lastY) / zoom;
                clampCamera();
                rebuildCameraCache();
            }
            lastX = mouseX;
            lastY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean widgetHandled = super.mouseReleased(mouseX, mouseY, button);
        if (mapPointerDown && button == pointerButton) {
            if (!dragging && pressedNodeId != null && !pending) {
                ScreenNode target = hitNode(mouseX, mouseY);
                if (target != null && target.node().id().equals(pressedNodeId)) {
                    selectedNodeId = pressedNodeId;
                    GraphSkillTreeCatalog.Node node = target.node();
                    if (purchaseChecks.get(node.id()) == GraphSkillTreeCatalog.PurchaseCheck.PURCHASABLE) {
                        submit(SkillTreeActionPayload.Action.PURCHASE_NODE, node.purchaseIndex());
                    }
                }
            }
            mapPointerDown = false;
            dragging = false;
            pointerButton = -1;
            pressedNodeId = null;
            return true;
        }
        return widgetHandled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (viewport != null && viewport.contains(mouseX, mouseY)) {
            zoomAt(mouseX, mouseY, scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        ScreenNode hovered = hitNode(mouseX, mouseY);
        String next = hovered == null ? null : hovered.node().id();
        if (!java.util.Objects.equals(hoveredNodeId, next)) {
            hoveredNodeId = next;
        }
    }

    @Override
    public void onClose() {
        if (returnTarget == SkillTreeNavigation.ReturnTarget.PROFILE) {
            requestProfile();
        } else {
            pending = false;
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "textures/gui/skilltree_v1/" + path);
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    private Component resetLabel() {
        return Component.translatable(System.currentTimeMillis() <= resetConfirmUntil
                ? "screen.anomalousclasses.skill_tree.confirm_reset"
                : "screen.anomalousclasses.skill_tree.reset");
    }

    private record ScreenNode(GraphSkillTreeCatalog.Node node, double x, double y, int size) {}
    private record VisibleEdge(GraphSkillTreeCatalog.Edge edge, ScreenNode from, ScreenNode to, boolean active) {}
    private record CardLine(Component text, int color) {}
    private record WrappedLine(net.minecraft.util.FormattedCharSequence text, int color) {}
}
