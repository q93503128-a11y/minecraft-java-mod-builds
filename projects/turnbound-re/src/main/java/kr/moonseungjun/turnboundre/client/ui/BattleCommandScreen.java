package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.BattleCommandSelection;
import kr.moonseungjun.turnboundre.client.BattlePresentationModel;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Non-pausing production command picker. The screen owns only temporary selection state;
 * every action and target candidate originates from the current authoritative server snapshot.
 */
public final class BattleCommandScreen extends Screen {
    private static final int TARGETS_PER_PAGE = 6;
    private static final int TARGET_COLUMNS = 2;

    private final List<ActionButtonBinding> actionButtons = new ArrayList<>();
    private BattleNetworkPayloads.SnapshotAction selectedAction;
    private final Set<String> selectedTargetIds = new LinkedHashSet<>();
    private int targetPage;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private String feedback = "";

    public BattleCommandScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.turnbound_re.battle_commands"));
    }

    @Override
    protected void init() {
        actionButtons.clear();
        panelWidth = 0;
        panelHeight = 0;

        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (model == null || !model.awaitingPlayerCommand()) return;

        if (selectedAction != null) {
            selectedAction = BattleCommandSelection.publishedAction(model, selectedAction).orElse(null);
            if (selectedAction == null || !selectedAction.usable()) {
                selectedAction = null;
                selectedTargetIds.clear();
                targetPage = 0;
            }
        }

        buildActionButtons(model);
        if (selectedAction != null) buildTargetButtons(model);
    }

    private void buildActionButtons(BattlePresentationModel model) {
        List<BattleNetworkPayloads.SnapshotAction> actions = model.availableActions();
        if (actions.isEmpty()) return;

        int gap = UiLayoutMetrics.SPACE_2;
        int available = Math.max(200, this.width - UiLayoutMetrics.SPACE_16);
        int totalWidth = Math.min(420, available);
        int cell = Math.max(38, (totalWidth - gap * (actions.size() - 1)) / actions.size());
        totalWidth = cell * actions.size() + gap * (actions.size() - 1);
        int x = (this.width - totalWidth) / 2;
        int y = this.height - 28;

        for (BattleNetworkPayloads.SnapshotAction action : actions) {
            boolean selected = selectedAction != null && selectedAction.id().equals(action.id());
            Component label = selected
                    ? Component.literal("> ").append(BattleActionPresentation.slotLabel(action))
                    : BattleActionPresentation.slotLabel(action);
            Button button = Button.builder(label, ignored -> chooseAction(action))
                    .bounds(x, y, cell, 20)
                    .build();
            button.active = action.usable();
            this.addRenderableWidget(button);
            actionButtons.add(new ActionButtonBinding(button, action));
            x += cell + gap;
        }
    }

    private void chooseAction(BattleNetworkPayloads.SnapshotAction action) {
        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (model == null) return;
        BattleNetworkPayloads.SnapshotAction published = BattleCommandSelection.publishedAction(model, action).orElse(null);
        if (published == null || !published.usable()) return;

        List<String> forced = BattleCommandSelection.forcedTargetsIfUnambiguous(model, published);
        if (!forced.isEmpty()) {
            submit(model, published, forced);
            return;
        }

        selectedAction = published;
        selectedTargetIds.clear();
        targetPage = 0;
        feedback = "";
        this.rebuildWidgets();
    }

    private void buildTargetButtons(BattlePresentationModel model) {
        List<BattleNetworkPayloads.SnapshotParticipant> candidates =
                BattleCommandSelection.eligibleTargets(model, selectedAction);
        if (candidates.isEmpty()) {
            feedback = Component.translatable("screen.turnbound_re.no_targets").getString();
            return;
        }

        int pageCount = Math.max(1, (candidates.size() + TARGETS_PER_PAGE - 1) / TARGETS_PER_PAGE);
        targetPage = Math.max(0, Math.min(targetPage, pageCount - 1));
        int from = targetPage * TARGETS_PER_PAGE;
        int to = Math.min(candidates.size(), from + TARGETS_PER_PAGE);
        List<BattleNetworkPayloads.SnapshotParticipant> page = candidates.subList(from, to);

        panelWidth = Math.min(340, Math.max(260, this.width - 48));
        int rows = Math.max(1, (page.size() + TARGET_COLUMNS - 1) / TARGET_COLUMNS);
        panelHeight = 62 + rows * 24 + (pageCount > 1 ? 22 : 0);
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(UiLayoutMetrics.SPACE_16, (this.height - panelHeight) / 2 - UiLayoutMetrics.SPACE_8);

        int innerGap = UiLayoutMetrics.SPACE_4;
        int buttonWidth = (panelWidth - 24 - innerGap) / TARGET_COLUMNS;
        int startX = panelX + UiLayoutMetrics.SPACE_12;
        int startY = panelY + 36;

        for (int i = 0; i < page.size(); i++) {
            BattleNetworkPayloads.SnapshotParticipant participant = page.get(i);
            int col = i % TARGET_COLUMNS;
            int row = i / TARGET_COLUMNS;
            int x = startX + col * (buttonWidth + innerGap);
            int y = startY + row * 24;
            boolean chosen = selectedTargetIds.contains(participant.id());
            String label = (chosen ? "[✓] " : "") + displayName(participant);
            Button target = Button.builder(Component.literal(label), ignored -> toggleTarget(model, participant.id()))
                    .bounds(x, y, buttonWidth, 20)
                    .build();
            this.addRenderableWidget(target);
        }

        int footerY = panelY + panelHeight - 22;
        if (selectedAction.targetCount() > 1) {
            Button confirm = Button.builder(
                            Component.translatable("screen.turnbound_re.confirm_targets",
                                    selectedTargetIds.size(), selectedAction.targetCount()),
                            ignored -> submit(model, selectedAction, List.copyOf(selectedTargetIds)))
                    .bounds(panelX + panelWidth - 112, footerY, 100, 20)
                    .build();
            confirm.active = selectedTargetIds.size() == selectedAction.targetCount();
            this.addRenderableWidget(confirm);
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), ignored -> clearTargetSelection())
                .bounds(panelX + UiLayoutMetrics.SPACE_12, footerY, 62, 20)
                .build());

        if (pageCount > 1) {
            Button prev = Button.builder(Component.literal("<"), ignored -> changePage(-1))
                    .bounds(panelX + 80, footerY, 24, 20)
                    .build();
            prev.active = targetPage > 0;
            this.addRenderableWidget(prev);
            Button next = Button.builder(Component.literal(">"), ignored -> changePage(1))
                    .bounds(panelX + 108, footerY, 24, 20)
                    .build();
            next.active = targetPage + 1 < pageCount;
            this.addRenderableWidget(next);
        }
    }

    private void toggleTarget(BattlePresentationModel model, String targetId) {
        if (selectedAction == null) return;
        if (selectedAction.targetCount() == 1) {
            submit(model, selectedAction, List.of(targetId));
            return;
        }

        if (selectedTargetIds.contains(targetId)) {
            selectedTargetIds.remove(targetId);
        } else if (selectedTargetIds.size() < selectedAction.targetCount()) {
            selectedTargetIds.add(targetId);
        }
        feedback = "";
        this.rebuildWidgets();
    }

    private void clearTargetSelection() {
        selectedAction = null;
        selectedTargetIds.clear();
        targetPage = 0;
        feedback = "";
        this.rebuildWidgets();
    }

    private void changePage(int delta) {
        targetPage = Math.max(0, targetPage + delta);
        this.rebuildWidgets();
    }

    private void submit(
            BattlePresentationModel model,
            BattleNetworkPayloads.SnapshotAction action,
            List<String> targetIds
    ) {
        final BattleCommand command;
        try {
            command = BattleCommandSelection.buildCommand(
                    model, action, targetIds, "ui:" + UUID.randomUUID());
        } catch (IllegalArgumentException invalidSelection) {
            feedback = Component.translatable("screen.turnbound_re.selection_changed").getString();
            selectedTargetIds.clear();
            this.rebuildWidgets();
            return;
        }

        ClientPacketDistributor.sendToServer(BattleNetworkPayloads.BattleCommandC2S.of(model.battleId(), command));
        this.minecraft.gui.setScreen(null);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (selectedAction != null && panelWidth > 0 && panelHeight > 0) {
            graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xD0181818);
            graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFFE0C06A);
            graphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF5A4A2A);
            String title = Component.translatable("screen.turnbound_re.select_targets",
                    selectedTargetIds.size(), selectedAction.targetCount()).getString();
            graphics.text(this.font, Component.literal(title), panelX + UiLayoutMetrics.SPACE_12, panelY + 9,
                    0xFFFFFFFF, true);
            String actionName = conciseActionName(selectedAction.id());
            graphics.text(this.font, Component.literal(actionName),
                    panelX + panelWidth - UiLayoutMetrics.SPACE_12 - this.font.width(actionName), panelY + 9,
                    0xFFAAAAAA, false);
            String summary = BattleActionPresentation.selectedSummary(selectedAction).getString();
            graphics.text(this.font, Component.literal(fit(summary, panelWidth - UiLayoutMetrics.SPACE_24)),
                    panelX + UiLayoutMetrics.SPACE_12, panelY + 19, 0xFFAAAAAA, false);
        }
        if (!feedback.isBlank()) {
            graphics.centeredText(this.font, Component.literal(feedback), this.width / 2,
                    Math.max(UiLayoutMetrics.SPACE_8, this.height - 44), 0xFFFFCC66);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        for (ActionButtonBinding binding : actionButtons) {
            if (binding.button().isMouseOver(mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(
                        this.font,
                        BattleActionPresentation.tooltip(binding.action()),
                        mouseX,
                        mouseY);
                break;
            }
        }
    }

    private static String displayName(BattleNetworkPayloads.SnapshotParticipant participant) {
        String source = participant.characterId().isBlank() ? participant.id() : participant.characterId();
        return humanizeId(source);
    }

    private static String conciseActionName(String actionId) {
        String name = BattleActionPresentation.actionName(actionId);
        return name.length() <= 24 ? name : name.substring(0, 21) + "...";
    }

    private String fit(String text, int maxWidth) {
        if (text == null || text.isBlank() || maxWidth <= 0) return "";
        if (this.font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int suffixWidth = this.font.width(suffix);
        if (suffixWidth >= maxWidth) return "";
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end)) + suffixWidth > maxWidth) end--;
        return end <= 0 ? "" : text.substring(0, end) + suffix;
    }

    private static String humanizeId(String id) {
        if (id == null || id.isBlank()) return "?";
        int colon = id.indexOf(':');
        String raw = colon >= 0 ? id.substring(colon + 1) : id;
        String[] words = raw.replace('-', '_').split("_+");
        List<String> result = new ArrayList<>();
        for (String word : words) {
            if (word.isBlank()) continue;
            result.add(Character.toUpperCase(word.charAt(0)) + (word.length() > 1 ? word.substring(1).toLowerCase() : ""));
        }
        return result.isEmpty() ? "?" : String.join(" ", result);
    }

    private record ActionButtonBinding(Button button, BattleNetworkPayloads.SnapshotAction action) {}

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
