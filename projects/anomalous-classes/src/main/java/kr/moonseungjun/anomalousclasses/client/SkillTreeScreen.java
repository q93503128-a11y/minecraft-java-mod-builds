package kr.moonseungjun.anomalousclasses.client;

import java.util.ArrayList;
import java.util.List;
import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SkillTreeScreen extends Screen {
    private SkillTreeStatePayload state;
    private ClassId classId;
    private List<SkillTreeCatalog.Node> nodes = List.of();
    private final List<NodeButton> nodeButtons = new ArrayList<>();
    private MenuUi.Box outer, graph, details;
    private MenuUi.Button investOne, investBreakpoint, investMax, resetButton, profileButton;
    private int selectedNode;
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
        int header = height < 250 ? 17 : 22;
        nodeButtons.clear();
        if (compact) {
            int actionRowTop = outer.bottom() - 46;
            int graphY = outer.y() + header + (height < 250 ? 10 : 8);
            int graphHeight = Math.max(56, Math.min(114, actionRowTop - graphY - 52));
            graph = new MenuUi.Box(outer.x() + 4, graphY, outer.width() - 8, graphHeight);
            details = new MenuUi.Box(outer.x() + 4, graph.bottom() + 4, outer.width() - 8,
                    Math.max(1, actionRowTop - graph.bottom() - 7));
            int gap = 4;
            int actionWidth = Math.max(1, (outer.width() - 20 - gap * 2) / 3);
            int actionX = outer.x() + (outer.width() - (actionWidth * 3 + gap * 2)) / 2;
            int actionH = Math.max(17, Math.min(20, height / 12));
            investOne = button(actionX, actionRowTop, actionWidth, actionH, "screen.anomalousclasses.skill_tree.invest_one", 0, SkillTreeActionPayload.Action.INVEST_ONE);
            investBreakpoint = button(actionX + actionWidth + gap, actionRowTop, actionWidth, actionH,
                    "screen.anomalousclasses.skill_tree.invest_breakpoint", 0, SkillTreeActionPayload.Action.INVEST_TO_BREAKPOINT);
            investMax = button(actionX + 2 * (actionWidth + gap), actionRowTop, actionWidth, actionH,
                    "screen.anomalousclasses.skill_tree.invest_max", 0, SkillTreeActionPayload.Action.INVEST_MAX);
            int bottomY = actionRowTop + actionH + 3;
            int bottomWidth = Math.max(1, (outer.width() - 20 - gap) / 2);
            int bottomX = outer.x() + (outer.width() - (bottomWidth * 2 + gap)) / 2;
            resetButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(bottomX, bottomY, bottomWidth, actionH),
                    Component.translatable("screen.anomalousclasses.skill_tree.reset"), MenuUi.accent(classId),
                    button -> submit(SkillTreeActionPayload.Action.RESET)));
            profileButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(bottomX + bottomWidth + gap, bottomY, bottomWidth, actionH),
                    Component.translatable("screen.anomalousclasses.skill_tree.profile"), MenuUi.accent(classId),
                    button -> requestProfile()));
        } else {
            int bodyY = outer.y() + header + 5;
            int footerH = 34;
            int bodyBottom = outer.bottom() - footerH - 8;
            int bodyH = Math.max(1, bodyBottom - bodyY);
            int detailW = Math.max(205, Math.min(280, outer.width() / 3));
            graph = new MenuUi.Box(outer.x() + 7, bodyY, outer.width() - detailW - 21, bodyH);
            details = new MenuUi.Box(graph.right() + 7, bodyY, detailW, bodyH);
            int controlGap = 5;
            int controlW = Math.max(1, (details.width() - 12 - controlGap * 2) / 3);
            int controlX = details.x() + (details.width() - (controlW * 3 + controlGap * 2)) / 2;
            int controlY = details.bottom() - 57;
            int controlH = 22;
            investOne = button(controlX, controlY, controlW, controlH, "screen.anomalousclasses.skill_tree.invest_one", 0, SkillTreeActionPayload.Action.INVEST_ONE);
            investBreakpoint = button(controlX + controlW + controlGap, controlY, controlW, controlH,
                    "screen.anomalousclasses.skill_tree.invest_breakpoint", 0, SkillTreeActionPayload.Action.INVEST_TO_BREAKPOINT);
            investMax = button(controlX + 2 * (controlW + controlGap), controlY, controlW, controlH,
                    "screen.anomalousclasses.skill_tree.invest_max", 0, SkillTreeActionPayload.Action.INVEST_MAX);
            int bottomY = controlY + controlH + 5;
            int bottomW = Math.max(1, (details.width() - 12 - controlGap) / 2);
            int bottomX = details.x() + (details.width() - (bottomW * 2 + controlGap)) / 2;
            resetButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(bottomX, bottomY, bottomW, controlH),
                    Component.translatable("screen.anomalousclasses.skill_tree.reset"), MenuUi.accent(classId),
                    button -> submit(SkillTreeActionPayload.Action.RESET)));
            profileButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(bottomX + bottomW + controlGap, bottomY, bottomW, controlH),
                    Component.translatable("screen.anomalousclasses.skill_tree.profile"), MenuUi.accent(classId),
                    button -> requestProfile()));
        }
        for (int index = 0; index < nodes.size(); index++) {
            MenuUi.Box box = nodeBox(index);
            nodeButtons.add(addRenderableWidget(new NodeButton(box, index)));
        }
        refreshButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x70201D25);
        MenuUi.drawPanel(graphics, outer, MenuUi.accent(classId));
        MenuUi.drawCentered(graphics, font, title, outer.x() + outer.width() / 2, outer.y() + 2,
                Math.max(1, outer.width() - (compact ? 110 : 20)), 0xFFF1E7D7);
        MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.skill_tree.effect_unavailable"),
                outer.x() + 7, outer.y() + 14, Math.max(1, outer.width() - 14), 0xFFB8AEB9);
        drawSkillPoints(graphics);
        MenuUi.drawPanel(graphics, graph, MenuUi.accent(classId));
        if (!compact) drawGroups(graphics);
        drawConnections(graphics);
        MenuUi.drawPanel(graphics, details, MenuUi.accent(classId));
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

    private MenuUi.Button button(int x, int y, int w, int h, String key, int node,
                                 SkillTreeActionPayload.Action action) {
        return addRenderableWidget(new MenuUi.Button(new MenuUi.Box(x, y, w, h), Component.translatable(key),
                MenuUi.accent(classId), button -> submit(action)));
    }

    private MenuUi.Box nodeBox(int index) {
        SkillTreeCatalog.Node node = nodes.get(index);
        int size = compact ? 23 : 46;
        int columnWidth = graph.width() / 3;
        int centerX = graph.x() + columnWidth * node.column() + columnWidth / 2;
        int maxRows = nodes.stream().filter(value -> value.column() == node.column()).mapToInt(SkillTreeCatalog.Node::row).max().orElse(0) + 1;
        int header = compact ? 8 : 30;
        int top = graph.y() + header + size / 2 + 2;
        int bottom = graph.bottom() - size / 2 - 4;
        int step = maxRows <= 1 ? 0 : Math.max(1, (bottom - top) / (maxRows - 1));
        int centerY = top + node.row() * step;
        return new MenuUi.Box(centerX - size / 2, centerY - size / 2, size, size);
    }

    private void drawGroups(GuiGraphics graphics) {
        for (int column = 0; column < 3; column++) {
            int columnIndex = column;
            SkillTreeCatalog.Node representative = nodes.stream().filter(node -> node.column() == columnIndex).findFirst().orElse(null);
            if (representative == null) continue;
            int center = graph.x() + graph.width() / 6 + graph.width() * column / 3;
            MenuUi.drawCentered(graphics, font, Component.translatable(representative.groupKey()), center,
                    graph.y() + 8, Math.max(1, graph.width() / 3 - 8), 0xFFD6C5A4);
            graphics.fill(graph.x() + graph.width() * column / 3 + 8, graph.y() + 23,
                    graph.x() + graph.width() * (column + 1) / 3 - 8, graph.y() + 24, 0xFF65576D);
        }
    }

    private void drawConnections(GuiGraphics graphics) {
        for (int column = 0; column < 3; column++) {
            int previousBottom = -1;
            int centerX = graph.x() + graph.width() / 6 + graph.width() * column / 3;
            for (int index = 0; index < nodes.size(); index++) {
                SkillTreeCatalog.Node node = nodes.get(index);
                if (node.column() != column) continue;
                MenuUi.Box box = nodeBox(index);
                if (previousBottom >= 0 && box.y() > previousBottom) {
                    graphics.fill(centerX, previousBottom, centerX + 1, box.y(), 0xFF796986);
                }
                previousBottom = box.bottom();
            }
        }
    }

    private void drawSkillPoints(GuiGraphics graphics) {
        Component points = Component.translatable("screen.anomalousclasses.skill_tree.skill_points", state.skillPoints());
        if (compact) {
            int maxWidth = Math.max(1, Math.min(92, outer.width() / 3));
            MenuUi.drawFit(graphics, font, points, outer.right() - maxWidth - 7, outer.y() + 5, maxWidth, 0xFFE6D1A6);
        } else {
            MenuUi.drawFit(graphics, font, Component.translatable("class.anomalousclasses." + classId.key()),
                    outer.x() + 9, outer.y() + 6, Math.max(1, outer.width() / 4), 0xFFD4C8BB);
            MenuUi.drawFit(graphics, font, points, outer.right() - font.width(points) - 10,
                    outer.y() + 6, font.width(points), 0xFFE6D1A6);
        }
    }

    private void drawSelectedDetails(GuiGraphics graphics) {
        if (selectedNode < 0 || selectedNode >= nodes.size()) return;
        SkillTreeCatalog.Node node = nodes.get(selectedNode);
        int rank = selectedNode < state.ranks().length ? state.ranks()[selectedNode] : 0;
        int padding = compact ? 5 : 9;
        int itemSize = compact ? 15 : 22;
        graphics.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(node.iconId()))),
                details.x() + padding, details.y() + padding);
        Component name = Component.translatable(node.nameKey(classId));
        int titleX = details.x() + padding + itemSize + 5;
        MenuUi.drawFit(graphics, font, name, titleX, details.y() + padding + 1,
                Math.max(1, details.right() - titleX - padding), 0xFFF3E8D8);
        Component rankText = Component.translatable("screen.anomalousclasses.skill_tree.rank", rank, node.maxRank());
        MenuUi.drawFit(graphics, font, rankText, details.x() + padding, details.y() + (compact ? 20 : 34),
                Math.max(1, details.width() - padding * 2), 0xFFD6C6AC);
        Component effect = Component.translatable(node.effectKey(classId));
        int effectY = details.y() + (compact ? 31 : 52);
        int effectBottom = compact ? details.bottom() - font.lineHeight - 5 : details.bottom() - 103;
        int effectLines = Math.max(1, (effectBottom - effectY) / font.lineHeight);
        Component status = statusMessage();
        if (compact && status != null) {
            MenuUi.drawFit(graphics, font, status, details.x() + padding, effectY,
                    Math.max(1, details.width() - padding * 2), statusColor());
        } else {
            MenuUi.drawWrapped(graphics, font, effect, details.x() + padding, effectY,
                    Math.max(1, details.width() - padding * 2), effectLines, 0xFFE0D8CE);
        }
        if (!compact) {
            int nextCost = rank < node.maxRank() ? node.rankCosts()[rank] : 0;
            int breakpoint = node.nextBreakpoint(rank);
            int milestoneCost = breakpoint >= 0 ? node.costFromTo(rank, breakpoint) : 0;
            int infoY = details.bottom() - 91;
            MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.skill_tree.next_cost", nextCost),
                    details.x() + padding, infoY, details.width() - padding * 2, 0xFFE6C788);
            Component milestone = breakpoint >= 0
                    ? Component.translatable("screen.anomalousclasses.skill_tree.breakpoint_cost", breakpoint, milestoneCost)
                    : Component.translatable("screen.anomalousclasses.skill_tree.no_breakpoint");
            MenuUi.drawFit(graphics, font, milestone,
                    details.x() + padding, infoY + font.lineHeight + 2, details.width() - padding * 2, 0xFFD6C6AC);
        }
        if (!compact && status != null) {
            int y = details.bottom() - 70;
            MenuUi.drawFit(graphics, font, status, details.x() + padding, y,
                    Math.max(1, details.width() - padding * 2), statusColor());
        }
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
        int rank = selectedNode >= 0 && selectedNode < state.ranks().length ? state.ranks()[selectedNode] : 0;
        boolean canInvest = !pending && rank < (selectedNode < nodes.size() ? nodes.get(selectedNode).maxRank() : 0)
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
        @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            SkillTreeCatalog.Node node = nodes.get(index);
            boolean selected = index == selectedNode;
            int rank = index < state.ranks().length ? state.ranks()[index] : 0;
            boolean hovered = isHoveredOrFocused();
            int accent = MenuUi.accent(classId);
            int border = selected ? accent : rank > 0 ? 0xFFB99A6F : hovered ? 0xFF9A8AA0 : 0xFF615B65;
            int inset = selected ? 3 : 1;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
            graphics.fill(getX() + inset, getY() + inset, getX() + width - inset, getY() + height - inset,
                    rank > 0 ? 0xFF21132A : 0xFF17151B);
            int iconSize = compact ? 15 : 32;
            int iconX = getX() + (width - iconSize) / 2;
            int iconY = getY() + (height - iconSize) / 2;
            graphics.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(node.iconId()))), iconX, iconY);
            String rankLabel = Integer.toString(rank);
            int badgeWidth = Math.min(width - 2, Minecraft.getInstance().font.width(rankLabel) + 2);
            int badgeX = getX() + width - badgeWidth - 1;
            int badgeY = getY() + height - 9;
            graphics.fill(badgeX, badgeY, badgeX + badgeWidth, getY() + height - 2, 0xEA0D0A11);
            graphics.drawString(Minecraft.getInstance().font, rankLabel, badgeX + 1,
                    badgeY, rank > 0 ? 0xFFF1D59B : 0xFFAAA39B, false);
        }
    }
}
