package kr.moonseungjun.anomalousclasses.client;

import java.util.ArrayList;
import java.util.List;
import kr.moonseungjun.anomalousclasses.network.payload.RequestProfilePayload;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeStatePayload;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.SkillTreeCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SkillTreeScreen extends Screen {
    private SkillTreeStatePayload state;
    private ClassId classId;
    private List<SkillTreeCatalog.Node> nodes = List.of();
    private final List<NodeButton> nodeButtons = new ArrayList<>();
    private MenuUi.Box outer, identity, graph, details;
    private MenuUi.Button investOne, investBreakpoint, investMax, resetButton, profileButton;
    private int selectedNode;
    private int nodeSize;
    private int graphHeader;
    private int investY;
    private int actionHeight;
    private boolean compact, pending;

    public SkillTreeScreen(SkillTreeStatePayload state) {
        super(Component.translatable("screen.anomalousclasses.skill_tree.title"));
        this.state = state;
        this.classId = ClassId.fromNetworkIndex(state.classIndex()).orElse(ClassId.MENTAL_DOMINATOR);
        this.nodes = SkillTreeCatalog.nodes(classId);
    }

    @Override
    protected void init() {
        classId = ClassId.fromNetworkIndex(state.classIndex()).orElse(classId);
        nodes = SkillTreeCatalog.nodes(classId);
        compact = width < 700 || height < 400;
        int margin = width < 360 ? 4 : 8;
        int top = height < 250 ? 3 : 8;
        outer = new MenuUi.Box(margin, top, Math.max(1, width - margin * 2), Math.max(1, height - top * 2));
        int header = height < 280 ? 18 : 26;
        nodeButtons.clear();
        if (compact) initCompact(header);
        else initWide(header);
        int rows = nodes.stream().mapToInt(SkillTreeCatalog.Node::row).max().orElse(0) + 1;
        graphHeader = compact ? 12 : 28;
        int maxSize = (graph.height() - graphHeader - 6 - Math.max(0, rows - 1) * 2) / Math.max(1, rows);
        nodeSize = Math.max(compact ? 18 : 32, Math.min(compact ? 36 : 48, maxSize));
        for (int index = 0; index < nodes.size(); index++) {
            nodeButtons.add(addRenderableWidget(new NodeButton(nodeBox(index), index)));
        }
        refreshButtons();
    }

    private void initCompact(int header) {
        int gap = 4;
        int contentX = outer.x() + 5;
        int contentWidth = Math.max(1, outer.width() - 10);
        int identityHeight = height < 300 ? 22 : 24;
        identity = new MenuUi.Box(contentX, outer.y() + header + 3, contentWidth, identityHeight);

        actionHeight = Math.max(16, Math.min(20, height / 18));
        int footerY = outer.bottom() - 4 - actionHeight;
        investY = footerY - 3 - actionHeight;
        int detailsHeight = Math.max(52, Math.min(62, height / 5));
        details = new MenuUi.Box(contentX, investY - 4 - detailsHeight, contentWidth, detailsHeight);
        int graphY = identity.bottom() + 4;
        graph = new MenuUi.Box(contentX, graphY, contentWidth, Math.max(1, details.y() - graphY - 4));

        int investWidth = Math.max(1, (contentWidth - 2 * gap) / 3);
        int investX = contentX + (contentWidth - (investWidth * 3 + gap * 2)) / 2;
        investOne = button(investX, investY, investWidth, actionHeight,
                "screen.anomalousclasses.skill_tree.invest_one", SkillTreeActionPayload.Action.INVEST_ONE);
        investBreakpoint = button(investX + investWidth + gap, investY, investWidth, actionHeight,
                "screen.anomalousclasses.skill_tree.invest_breakpoint", SkillTreeActionPayload.Action.INVEST_TO_BREAKPOINT);
        investMax = button(investX + 2 * (investWidth + gap), investY, investWidth, actionHeight,
                "screen.anomalousclasses.skill_tree.invest_max", SkillTreeActionPayload.Action.INVEST_MAX);

        int footerWidth = Math.max(1, Math.min(156, (contentWidth - gap) / 2));
        int footerX = contentX + (contentWidth - (footerWidth * 2 + gap)) / 2;
        resetButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(footerX, footerY, footerWidth, actionHeight),
                Component.translatable("screen.anomalousclasses.skill_tree.reset"),
                button -> submit(SkillTreeActionPayload.Action.RESET)));
        profileButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(footerX + footerWidth + gap, footerY,
                footerWidth, actionHeight), Component.translatable("screen.anomalousclasses.skill_tree.profile"),
                button -> requestProfile()));
    }

    private void initWide(int header) {
        int bodyY = outer.y() + header + 5;
        int bodyHeight = Math.max(1, outer.bottom() - bodyY - 7);
        int contentX = outer.x() + 7;
        int identityWidth = Math.max(148, Math.min(202, outer.width() / 5));
        int detailWidth = Math.max(190, Math.min(258, outer.width() / 4));
        int gap = 6;
        identity = new MenuUi.Box(contentX, bodyY, identityWidth, bodyHeight);
        int detailX = outer.right() - 7 - detailWidth;
        int graphX = identity.right() + gap;
        graph = new MenuUi.Box(graphX, bodyY, Math.max(1, detailX - graphX - gap), bodyHeight);
        details = new MenuUi.Box(detailX, bodyY, detailWidth, bodyHeight);

        actionHeight = 20;
        investY = details.bottom() - 50 - actionHeight;
        int actionGap = 4;
        int investWidth = Math.max(1, (details.width() - 12 - actionGap * 2) / 3);
        int investX = details.x() + (details.width() - (investWidth * 3 + actionGap * 2)) / 2;
        investOne = button(investX, investY, investWidth, actionHeight,
                "screen.anomalousclasses.skill_tree.invest_one", SkillTreeActionPayload.Action.INVEST_ONE);
        investBreakpoint = button(investX + investWidth + actionGap, investY, investWidth, actionHeight,
                "screen.anomalousclasses.skill_tree.invest_breakpoint", SkillTreeActionPayload.Action.INVEST_TO_BREAKPOINT);
        investMax = button(investX + 2 * (investWidth + actionGap), investY, investWidth, actionHeight,
                "screen.anomalousclasses.skill_tree.invest_max", SkillTreeActionPayload.Action.INVEST_MAX);

        int footerWidth = Math.max(1, Math.min(132, details.width() - 12));
        int footerX = details.x() + (details.width() - footerWidth) / 2;
        int footerY = details.bottom() - 25;
        resetButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(identity.x() + 8, identity.bottom() - 27,
                identity.width() - 16, 21), Component.translatable("screen.anomalousclasses.skill_tree.reset"),
                button -> submit(SkillTreeActionPayload.Action.RESET)));
        profileButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(footerX, footerY, footerWidth, 20),
                Component.translatable("screen.anomalousclasses.skill_tree.profile"), button -> requestProfile()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x70201D25);
        MenuUi.drawPanel(graphics, outer);
        MenuUi.drawTitle(graphics, font, title, outer, width, height);

        if (compact) drawCompactIdentity(graphics);
        else drawWideIdentity(graphics);

        MenuUi.drawInsetSurface(graphics, graph);
        drawGroups(graphics);
        drawConnections(graphics);

        MenuUi.drawInsetSurface(graphics, details);
        drawSelectedDetails(graphics);
        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    public void applyServerState(SkillTreeStatePayload state) {
        this.state = state;
        this.classId = ClassId.fromNetworkIndex(state.classIndex()).orElse(classId);
        this.nodes = SkillTreeCatalog.nodes(classId);
        this.pending = false;
        if (selectedNode >= nodes.size()) selectedNode = Math.max(0, nodes.size() - 1);
        refreshButtons();
    }

    @Override public boolean isPauseScreen() { return false; }

    private MenuUi.Button button(int x, int y, int w, int h, String key,
                                 SkillTreeActionPayload.Action action) {
        return addRenderableWidget(new MenuUi.Button(new MenuUi.Box(x, y, w, h), Component.translatable(key),
                button -> submit(action)));
    }

    private void drawCompactIdentity(GuiGraphics graphics) {
        MenuUi.drawInsetSurface(graphics, identity);
        int iconSize = Math.min(18, identity.height() - 4);
        MenuUi.drawClassIcon(graphics, classId, identity.x() + 5, identity.y() + (identity.height() - iconSize) / 2, iconSize);
        Component className = Component.translatable("class.anomalousclasses." + classId.key());
        int classX = identity.x() + iconSize + 10;
        int pointsWidth = Math.min(145, identity.width() / 2);
        MenuUi.drawFit(graphics, font, className, classX, identity.y() + (identity.height() - font.lineHeight) / 2,
                Math.max(1, identity.right() - classX - pointsWidth - 8), 0xFFF1E7D7);
        Component points = Component.translatable("screen.anomalousclasses.skill_tree.points_summary",
                state.skillPoints(), SkillTreeCatalog.spent(classId, state.ranks()));
        MenuUi.drawFit(graphics, font, points, identity.right() - pointsWidth - 5,
                identity.y() + (identity.height() - font.lineHeight) / 2, pointsWidth, 0xFFE6D1A6);
    }

    private void drawWideIdentity(GuiGraphics graphics) {
        MenuUi.drawInsetSurface(graphics, identity);
        int padding = 6;
        int artWidth = identity.width() - padding * 2;
        int artHeight = Math.max(1, Math.min(116, artWidth * 9 / 16));
        MenuUi.drawArtCover(graphics, classId, new MenuUi.Box(identity.x() + padding, identity.y() + padding,
                artWidth, artHeight));
        int iconSize = Math.min(28, font.lineHeight + 8);
        int nameY = identity.y() + padding + artHeight + 6;
        MenuUi.drawClassIcon(graphics, classId, identity.x() + padding, nameY, iconSize);
        int nameX = identity.x() + padding + iconSize + 5;
        MenuUi.drawWrapped(graphics, font, Component.translatable("class.anomalousclasses." + classId.key()),
                nameX, nameY + Math.max(0, (iconSize - font.lineHeight) / 2),
                Math.max(1, identity.right() - nameX - padding), 2, 0xFFF1E7D7);

        int statsY = nameY + iconSize + 8;
        MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.skill_tree.spent_points",
                SkillTreeCatalog.spent(classId, state.ranks())), identity.x() + padding, statsY,
                identity.width() - padding * 2, 0xFFD8D0C5);
        MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.skill_tree.skill_points",
                state.skillPoints()), identity.x() + padding, statsY + font.lineHeight + 3,
                identity.width() - padding * 2, 0xFFE6D1A6);
    }

    private void drawGroups(GuiGraphics graphics) {
        for (int column = 0; column < 3; column++) {
            int targetColumn = column;
            SkillTreeCatalog.Node representative = nodes.stream()
                    .filter(node -> node.column() == targetColumn).findFirst().orElse(null);
            if (representative == null) continue;
            int start = graph.x() + graph.width() * column / 3;
            int end = graph.x() + graph.width() * (column + 1) / 3;
            int center = (start + end) / 2;
            MenuUi.drawCentered(graphics, font, Component.translatable(representative.groupKey()), center,
                    graph.y() + 4, Math.max(1, end - start - 8), 0xFFD9C7A7);
        }
    }

    private void drawConnections(GuiGraphics graphics) {
        for (int column = 0; column < 3; column++) {
            MenuUi.Box previous = null;
            for (int index = 0; index < nodes.size(); index++) {
                SkillTreeCatalog.Node node = nodes.get(index);
                if (node.column() != column) continue;
                MenuUi.Box current = nodeBox(index);
                if (previous != null) {
                    int fromX = previous.x() + previous.width() / 2;
                    int toX = current.x() + current.width() / 2;
                    int top = previous.bottom() - 2;
                    int bottom = current.y() + 2;
                    int pathWidth = Math.max(18, Math.abs(toX - fromX) + 18);
                    int pathHeight = Math.max(12, bottom - top + 10);
                    int pathX = (fromX + toX - pathWidth) / 2;
                    int pathY = top - 5;
                    int type = fromX == toX ? 0 : (toX > fromX ? 1 : 2);
                    MenuUi.drawConnection(graphics, type,
                            new MenuUi.Box(pathX, pathY, pathWidth, pathHeight));
                }
                previous = current;
            }
        }
    }

    private MenuUi.Box nodeBox(int index) {
        SkillTreeCatalog.Node node = nodes.get(index);
        int columnStart = graph.x() + graph.width() * node.column() / 3;
        int columnEnd = graph.x() + graph.width() * (node.column() + 1) / 3;
        int centerX = (columnStart + columnEnd) / 2;
        int offset = node.row() % 2 == 0 ? 0 : (node.column() == 1 ? 10 : (node.column() == 0 ? 8 : -8));
        centerX += offset;
        centerX = Math.max(graph.x() + nodeSize / 2 + 4,
                Math.min(graph.right() - nodeSize / 2 - 4, centerX));
        int maxRows = nodes.stream().filter(value -> value.column() == node.column())
                .mapToInt(SkillTreeCatalog.Node::row).max().orElse(0) + 1;
        int top = graph.y() + graphHeader + nodeSize / 2 + 2;
        int bottom = graph.bottom() - nodeSize / 2 - 4;
        int step = maxRows <= 1 ? 0 : Math.max(1, (bottom - top) / (maxRows - 1));
        int centerY = top + node.row() * step;
        return new MenuUi.Box(centerX - nodeSize / 2, centerY - nodeSize / 2, nodeSize, nodeSize);
    }

    private void drawSelectedDetails(GuiGraphics graphics) {
        if (selectedNode < 0 || selectedNode >= nodes.size()) return;
        SkillTreeCatalog.Node node = nodes.get(selectedNode);
        int rank = rank(selectedNode);
        if (compact) drawCompactDetails(graphics, node, rank);
        else drawWideDetails(graphics, node, rank);
    }

    private void drawCompactDetails(GuiGraphics graphics, SkillTreeCatalog.Node node, int rank) {
        int padding = 5;
        int iconSize = Math.min(18, details.height() - 6);
        MenuUi.drawSkillIcon(graphics, classId, selectedNode, details.x() + padding,
                details.y() + 3, iconSize);
        int nameX = details.x() + padding + iconSize + 5;
        int contentWidth = Math.max(1, details.right() - nameX - padding);
        MenuUi.drawFit(graphics, font, Component.translatable(node.nameKey(classId)), nameX,
                details.y() + 3, contentWidth, 0xFFF3E8D8);

        int nextCost = rank < node.maxRank() ? node.rankCosts()[rank] : 0;
        int breakpoint = node.nextBreakpoint(rank);
        Component summary = breakpoint >= 0
                ? Component.translatable("screen.anomalousclasses.skill_tree.compact_breakpoint_summary",
                        rank, node.maxRank(), nextCost, breakpoint, node.costFromTo(rank, breakpoint))
                : Component.translatable("screen.anomalousclasses.skill_tree.compact_summary",
                        rank, node.maxRank(), nextCost);
        MenuUi.drawFit(graphics, font, summary, details.x() + padding, details.y() + font.lineHeight + 5,
                details.width() - padding * 2, 0xFFE6C788);

        Component status = statusMessage();
        Component effect = status == null ? Component.translatable(node.effectKey(classId)) : status;
        int effectY = details.y() + font.lineHeight * 2 + 7;
        int maxLines = Math.max(1, (details.bottom() - effectY - 3) / font.lineHeight);
        MenuUi.drawWrapped(graphics, font, effect, details.x() + padding, effectY,
                details.width() - padding * 2, maxLines, status == null ? 0xFFE0D8CE : statusColor());
    }

    private void drawWideDetails(GuiGraphics graphics, SkillTreeCatalog.Node node, int rank) {
        int padding = 9;
        int iconSize = 31;
        MenuUi.drawSkillIcon(graphics, classId, selectedNode, details.x() + padding, details.y() + padding, iconSize);
        int nameX = details.x() + padding + iconSize + 7;
        int nameWidth = Math.max(1, details.right() - nameX - padding);
        Component name = Component.translatable(node.nameKey(classId));
        List<net.minecraft.util.FormattedCharSequence> nameLines = font.split(name, nameWidth);
        int nameLineCount = Math.max(1, Math.min(2, nameLines.size()));
        MenuUi.drawWrapped(graphics, font, name, nameX, details.y() + padding + 2, nameWidth,
                nameLineCount, 0xFFF3E8D8);

        int rankY = details.y() + 48;
        MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.skill_tree.rank",
                rank, node.maxRank()), details.x() + padding, rankY,
                details.width() - padding * 2, 0xFFD6C6AC);

        int nextBreakpoint = node.nextBreakpoint(rank);
        int nextCost = rank < node.maxRank() ? node.rankCosts()[rank] : 0;
        int targetRank = nextBreakpoint >= 0 ? nextBreakpoint : node.maxRank();
        int stageStart = 0;
        for (int breakpoint : node.breakpoints()) if (breakpoint <= rank) stageStart = breakpoint;
        double progress = targetRank <= stageStart ? 1.0 : (rank - stageStart) / (double) (targetRank - stageStart);
        int meterY = rankY + font.lineHeight + 3;
        MenuUi.drawRankProgress(graphics, details.x() + padding, meterY,
                Math.max(1, details.width() - padding * 2), 7, progress);

        int costY = meterY + 11;
        MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.skill_tree.next_cost", nextCost),
                details.x() + padding, costY, details.width() - padding * 2, 0xFFE6C788);
        Component milestone = nextBreakpoint >= 0
                ? Component.translatable("screen.anomalousclasses.skill_tree.breakpoint_cost", nextBreakpoint,
                        node.costFromTo(rank, nextBreakpoint))
                : Component.translatable("screen.anomalousclasses.skill_tree.no_breakpoint");
        int milestoneY = costY + font.lineHeight + 2;
        MenuUi.drawFit(graphics, font, milestone, details.x() + padding, milestoneY,
                details.width() - padding * 2, 0xFFD6C6AC);

        int effectY = milestoneY + font.lineHeight + 8;
        int effectBottom = investY - 9;
        Component status = statusMessage();
        int statusSpace = status == null ? 0 : font.lineHeight + 3;
        int effectLines = Math.max(1, (effectBottom - effectY - statusSpace) / font.lineHeight);
        MenuUi.drawWrapped(graphics, font, Component.translatable(node.effectKey(classId)),
                details.x() + padding, effectY, details.width() - padding * 2, effectLines, 0xFFE0D8CE);
        if (status != null) {
            MenuUi.drawFit(graphics, font, status, details.x() + padding, effectBottom - font.lineHeight,
                    details.width() - padding * 2, statusColor());
        }
    }

    private int rank(int index) {
        return index >= 0 && index < state.ranks().length ? state.ranks()[index] : 0;
    }

    private Component statusMessage() {
        if (pending) return Component.translatable("screen.anomalousclasses.skill_tree.pending");
        return switch (state.result()) {
            case UPDATED -> Component.translatable("screen.anomalousclasses.skill_tree.saved");
            case RESET -> Component.translatable("screen.anomalousclasses.skill_tree.reset_done");
            case INSUFFICIENT_SP -> Component.translatable("screen.anomalousclasses.skill_tree.insufficient");
            case MAX_RANK -> Component.translatable("screen.anomalousclasses.skill_tree.max_rank");
            case NO_BREAKPOINT -> Component.translatable("screen.anomalousclasses.skill_tree.no_breakpoint");
            case COMBAT_LOCKED -> Component.translatable("screen.anomalousclasses.skill_tree.combat_lock");
            case STALE_REQUEST -> Component.translatable("screen.anomalousclasses.skill_tree.stale");
            case INVALID_ACTION, INVALID_CLASS, INVALID_NODE -> Component.translatable("screen.anomalousclasses.skill_tree.rejected");
            default -> null;
        };
    }

    private int statusColor() {
        return switch (state.result()) {
            case UPDATED, RESET -> 0xFFB9D9A7;
            case INSUFFICIENT_SP, MAX_RANK, NO_BREAKPOINT -> 0xFFE6C788;
            case COMBAT_LOCKED, INVALID_ACTION, INVALID_CLASS, INVALID_NODE -> 0xFFF0B3A3;
            default -> 0xFFD8D0C5;
        };
    }

    private void submit(SkillTreeActionPayload.Action action) {
        if (pending) return;
        if (action == SkillTreeActionPayload.Action.RESET && state.combatLocked()) {
            state = new SkillTreeStatePayload(state.classIndex(), state.revision(), state.skillPoints(), state.ranks(), true,
                    SkillTreeStatePayload.Result.COMBAT_LOCKED);
            refreshButtons();
            return;
        }
        pending = true;
        refreshButtons();
        PacketDistributor.sendToServer(new SkillTreeActionPayload(classId.networkIndex(), selectedNode,
                action.ordinal(), state.revision()));
    }

    private void requestProfile() {
        if (pending) return;
        pending = true;
        refreshButtons();
        PacketDistributor.sendToServer(RequestProfilePayload.INSTANCE);
    }

    private void refreshButtons() {
        int rank = rank(selectedNode);
        boolean canInvest = !pending && selectedNode < nodes.size() && rank < nodes.get(selectedNode).maxRank()
                && state.skillPoints() > 0;
        if (investOne != null) investOne.active = canInvest;
        if (investBreakpoint != null) {
            boolean hasMilestone = selectedNode < nodes.size() && nodes.get(selectedNode).nextBreakpoint(rank) >= 0;
            investBreakpoint.active = canInvest && hasMilestone;
        }
        if (investMax != null) investMax.active = canInvest;
        if (resetButton != null) resetButton.active = !pending && !state.combatLocked();
        if (profileButton != null) profileButton.active = !pending;
        for (NodeButton nodeButton : nodeButtons) nodeButton.active = !pending;
    }

    private final class NodeButton extends AbstractButton {
        private final int index;

        private NodeButton(MenuUi.Box box, int index) {
            super(box.x(), box.y(), box.width(), box.height(), Component.translatable(nodes.get(index).nameKey(classId)));
            this.index = index;
        }

        @Override public void onPress() {
            selectedNode = index;
            state = new SkillTreeStatePayload(state.classIndex(), state.revision(), state.skillPoints(), state.ranks(),
                    state.combatLocked(), SkillTreeStatePayload.Result.STATUS);
            refreshButtons();
        }

        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int rank = rank(index);
            int frameState = index == selectedNode ? 2 : (isHoveredOrFocused() ? 3 : (rank > 0 ? 1 : 0));
            MenuUi.drawNodeFrame(graphics, frameState, getX(), getY(), width);
            int iconSize = Math.max(10, width - (compact ? 9 : 16));
            int iconY = getY() + (compact ? 2 : 3);
            MenuUi.drawSkillIcon(graphics, classId, index, getX() + (width - iconSize) / 2, iconY, iconSize);
            Component rankLabel = compact ? Component.literal(Integer.toString(rank))
                    : Component.literal(rank + "/" + nodes.get(index).maxRank());
            MenuUi.drawCentered(graphics, Minecraft.getInstance().font, rankLabel, getX() + width / 2,
                    getY() + height - Minecraft.getInstance().font.lineHeight - 1, width - 3,
                    rank > 0 ? 0xFFFFE2A4 : 0xFFD4C9BB);
        }
    }
}
