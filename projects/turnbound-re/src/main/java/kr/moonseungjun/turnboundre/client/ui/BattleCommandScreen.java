package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.BattleCommandOverlayState;
import kr.moonseungjun.turnboundre.client.BattleCommandSelection;
import kr.moonseungjun.turnboundre.client.BattlePresentationModel;
import kr.moonseungjun.turnboundre.client.BattleTargetMarkerState;
import kr.moonseungjun.turnboundre.client.input.BattleInputHandler;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Non-pausing production command picker. The screen owns only temporary selection state;
 * every action and target candidate originates from the current authoritative server snapshot.
 * Visible controls use the adopted Kenney-backed frame language rather than vanilla button skins.
 */
public final class BattleCommandScreen extends Screen {
    private final List<ActionButtonBinding> actionButtons = new ArrayList<>();
    private final List<TargetButtonBinding> targetButtons = new ArrayList<>();
    private BattleNetworkPayloads.SnapshotAction selectedAction;
    private final Set<String> selectedTargetIds = new LinkedHashSet<>();
    private Button targetBackButton;
    private Button targetConfirmButton;
    private Button targetPrevButton;
    private Button targetNextButton;
    private int targetPage;
    private int targetHeaderTextX;
    private int targetHeaderTextY;
    private int targetHeaderTextWidth;
    private String feedback = "";

    public BattleCommandScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.turnbound_re.battle_commands"));
    }

    @Override
    protected void init() {
        actionButtons.clear();
        targetButtons.clear();
        targetBackButton = null;
        targetConfirmButton = null;
        targetPrevButton = null;
        targetNextButton = null;
        targetHeaderTextX = 0;
        targetHeaderTextY = 0;
        targetHeaderTextWidth = 0;
        BattleCommandOverlayState.close();

        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (model == null || !model.awaitingPlayerCommand()) {
            BattleTargetMarkerState.clear();
            return;
        }
        BattleCommandOverlayState.open();

        if (selectedAction != null) {
            selectedAction = BattleCommandSelection.publishedAction(model, selectedAction).orElse(null);
            if (selectedAction == null || !selectedAction.usable()) {
                selectedAction = null;
                selectedTargetIds.clear();
                targetPage = 0;
            }
        }

        if (selectedAction == null) {
            BattleTargetMarkerState.clear();
            buildActionButtons(model);
        } else {
            buildTargetButtons(model);
        }
    }

    private void buildActionButtons(BattlePresentationModel model) {
        List<BattleNetworkPayloads.SnapshotAction> actions = model.availableActions();
        if (actions.isEmpty()) return;

        UiLayoutMetrics.Rect command = commandRegion();
        int gap = UiLayoutMetrics.SPACE_2;
        int totalGap = gap * Math.max(0, actions.size() - 1);
        int cell = Math.max(1, (command.width() - totalGap) / actions.size());
        int totalWidth = cell * actions.size() + totalGap;
        int x = command.x() + Math.max(0, (command.width() - totalWidth) / 2);
        int y = command.bottom() - 20;
        String actorName = model.currentActor().map(BattleCommandScreen::displayName).orElse("");

        for (BattleNetworkPayloads.SnapshotAction action : actions) {
            String actionName = conciseActionName(action.id(), actorName);
            String energy = action.energyCost() > 0 ? " E" + action.energyCost() : "";
            String rawLabel = BattleActionPresentation.slotLabel(action).getString()
                    + " · " + actionName + energy;
            Component label = Component.literal(fit(rawLabel, Math.max(1, cell - UiLayoutMetrics.SPACE_4)));
            Button button = Button.builder(label, ignored -> chooseAction(action))
                    .bounds(x, y, cell, 20)
                    .build();
            button.active = action.usable();
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
            BattleTargetMarkerState.clear();
            return;
        }

        UiLayoutMetrics.TargetChooserLayout layout = targetChooserLayout();
        int pageSize = layout.pageSize();
        int pageCount = Math.max(1, (candidates.size() + pageSize - 1) / pageSize);
        targetPage = Math.max(0, Math.min(targetPage, pageCount - 1));
        int from = targetPage * pageSize;
        int to = Math.min(candidates.size(), from + pageSize);
        List<BattleNetworkPayloads.SnapshotParticipant> page = candidates.subList(from, to);

        int gap = UiLayoutMetrics.SPACE_2;
        int columns = layout.columns();
        int buttonWidth = Math.max(1, (layout.grid().width() - gap * (columns - 1)) / columns);
        int rowHeight = 20;

        for (int i = 0; i < page.size(); i++) {
            BattleNetworkPayloads.SnapshotParticipant participant = page.get(i);
            int col = i % columns;
            int row = i / columns;
            int x = layout.grid().x() + col * (buttonWidth + gap);
            int y = layout.grid().y() + row * (rowHeight + gap);
            int ordinal = from + i + 1;
            boolean chosen = selectedTargetIds.contains(participant.id());
            String label = (chosen ? "◆ " : "  ") + "#" + ordinal + " " + displayName(participant);
            Button target = Button.builder(Component.literal(label), ignored -> toggleTarget(model, participant.id()))
                    .bounds(x, y, buttonWidth, rowHeight)
                    .build();
            targetButtons.add(new TargetButtonBinding(target, participant.id()));
        }

        UiLayoutMetrics.Rect header = layout.header();
        int headerHeight = header.height();
        int backWidth = 42;
        targetBackButton = Button.builder(Component.translatable("gui.cancel"), ignored -> clearTargetSelection())
                .bounds(header.x(), header.y(), backWidth, headerHeight)
                .build();

        int right = header.right();
        if (selectedAction.targetCount() > 1) {
            int confirmWidth = 64;
            right -= confirmWidth;
            targetConfirmButton = Button.builder(
                            Component.translatable("screen.turnbound_re.confirm_targets",
                                    selectedTargetIds.size(), selectedAction.targetCount()),
                            ignored -> submit(model, selectedAction, List.copyOf(selectedTargetIds)))
                    .bounds(right, header.y(), confirmWidth, headerHeight)
                    .build();
            targetConfirmButton.active = selectedTargetIds.size() == selectedAction.targetCount();
            right -= gap;
        }

        if (pageCount > 1) {
            int navWidth = 20;
            right -= navWidth;
            targetNextButton = Button.builder(Component.literal(">"), ignored -> changePage(1))
                    .bounds(right, header.y(), navWidth, headerHeight)
                    .build();
            targetNextButton.active = targetPage + 1 < pageCount;
            right -= gap + navWidth;
            targetPrevButton = Button.builder(Component.literal("<"), ignored -> changePage(-1))
                    .bounds(right, header.y(), navWidth, headerHeight)
                    .build();
            targetPrevButton.active = targetPage > 0;
            right -= gap;
        }

        targetHeaderTextX = header.x() + backWidth + UiLayoutMetrics.SPACE_4;
        targetHeaderTextY = header.y() + 5;
        targetHeaderTextWidth = Math.max(0, right - UiLayoutMetrics.SPACE_4 - targetHeaderTextX);

        publishTargetMarkers(model, null);
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
        BattleTargetMarkerState.clear();
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

        BattleTargetMarkerState.clear();
        ClientPacketDistributor.sendToServer(BattleNetworkPayloads.BattleCommandC2S.of(model.battleId(), command));
        this.minecraft.gui.setScreen(null);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && model != null) {
            if (selectedAction != null) {
                String stageTargetId = virtualStageTargetAt(model, event.x(), event.y());
                if (stageTargetId != null) {
                    toggleTarget(model, stageTargetId);
                    return true;
                }
            }

            int mouseX = (int) Math.floor(event.x());
            int mouseY = (int) Math.floor(event.y());
            for (ActionButtonBinding binding : actionButtons) {
                if (binding.button().active && contains(binding.button(), mouseX, mouseY)) {
                    chooseAction(binding.action());
                    return true;
                }
            }
            for (TargetButtonBinding binding : targetButtons) {
                if (binding.button().active && contains(binding.button(), mouseX, mouseY)) {
                    toggleTarget(model, binding.participantId());
                    return true;
                }
            }
            if (enabledHit(targetBackButton, mouseX, mouseY)) {
                clearTargetSelection();
                return true;
            }
            if (enabledHit(targetConfirmButton, mouseX, mouseY)) {
                submit(model, selectedAction, List.copyOf(selectedTargetIds));
                return true;
            }
            if (enabledHit(targetPrevButton, mouseX, mouseY)) {
                changePage(-1);
                return true;
            }
            if (enabledHit(targetNextButton, mouseX, mouseY)) {
                changePage(1);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (selectedAction == null && model != null && model.awaitingPlayerCommand()) {
            UiLayoutMetrics.Rect command = commandRegion();
            BattleNetworkPayloads.SnapshotParticipant actor = model.currentActor().orElse(null);
            if (actor != null) {
                String header = Component.translatable(
                        "hud.turnbound_re.command_header",
                        displayName(actor),
                        BattleInputHandler.openKeyName()).getString();
                UiVisualLanguage.titleBand(
                        graphics,
                        this.font,
                        command.x(),
                        command.y(),
                        command.width(),
                        16,
                        Component.literal(fit(header, command.width() - UiLayoutMetrics.SPACE_8)),
                        UiVisualLanguage.TEXT_FOCUS,
                        true);
            }
            if (model.availableActions().isEmpty()) {
                String unavailable = Component.translatable("hud.turnbound_re.no_actions").getString();
                graphics.text(this.font, Component.literal(fit(unavailable, command.width() - UiLayoutMetrics.SPACE_8)),
                        command.x() + UiLayoutMetrics.SPACE_4,
                        command.y() + 20,
                        UiVisualLanguage.TEXT_SECONDARY, true);
            }
        }

        if (selectedAction != null && targetHeaderTextWidth > 0) {
            String summary = BattleActionPresentation.selectedSummary(selectedAction).getString()
                    + " · "
                    + Component.translatable("screen.turnbound_re.select_targets",
                    selectedTargetIds.size(), selectedAction.targetCount()).getString();
            graphics.text(this.font, Component.literal(fit(summary, targetHeaderTextWidth)),
                    targetHeaderTextX, targetHeaderTextY, UiVisualLanguage.TEXT_FOCUS, true);
        }
        if (!feedback.isBlank()) {
            UiLayoutMetrics.Rect command = commandRegion();
            graphics.text(this.font, Component.literal(fit(feedback, command.width())),
                    command.x(), Math.max(UiLayoutMetrics.SPACE_8, command.y() - this.font.lineHeight - UiLayoutMetrics.SPACE_2),
                    UiVisualLanguage.TEXT_WARNING, true);
        }

        renderControls(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        for (ActionButtonBinding binding : actionButtons) {
            if (contains(binding.button(), mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(
                        this.font,
                        BattleActionPresentation.tooltip(binding.action()),
                        mouseX,
                        mouseY);
                break;
            }
        }

        String hoveredTargetId = null;
        if (model != null && selectedAction != null) {
            hoveredTargetId = virtualStageTargetAt(model, mouseX, mouseY);
        }
        if (hoveredTargetId == null) {
            for (TargetButtonBinding binding : targetButtons) {
                if (contains(binding.button(), mouseX, mouseY)) {
                    hoveredTargetId = binding.participantId();
                    break;
                }
            }
        }
        if (model != null && selectedAction != null) {
            publishTargetMarkers(model, hoveredTargetId);
        } else {
            BattleTargetMarkerState.clear();
        }
    }

    private void renderControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (ActionButtonBinding binding : actionButtons) {
            renderControl(graphics, binding.button(), false, mouseX, mouseY);
        }
        for (TargetButtonBinding binding : targetButtons) {
            renderControl(graphics, binding.button(), selectedTargetIds.contains(binding.participantId()), mouseX, mouseY);
        }
        renderControl(graphics, targetBackButton, false, mouseX, mouseY);
        renderControl(graphics, targetConfirmButton, false, mouseX, mouseY);
        renderControl(graphics, targetPrevButton, false, mouseX, mouseY);
        renderControl(graphics, targetNextButton, false, mouseX, mouseY);
    }

    private void renderControl(GuiGraphicsExtractor graphics, Button button, boolean selected, int mouseX, int mouseY) {
        if (button == null) return;
        UiVisualLanguage.FrameState state = !button.active
                ? UiVisualLanguage.FrameState.DISABLED
                : selected || contains(button, mouseX, mouseY)
                        ? UiVisualLanguage.FrameState.FOCUS
                        : UiVisualLanguage.FrameState.IDLE;
        UiVisualLanguage.frame(graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight(), state);
        Component message = button.getMessage();
        int x = button.getX() + Math.max(UiLayoutMetrics.SPACE_2, (button.getWidth() - this.font.width(message)) / 2);
        int y = button.getY() + Math.max(1, (button.getHeight() - this.font.lineHeight) / 2);
        graphics.text(this.font, message, x, y, UiVisualLanguage.textColor(state), true);
    }

    private static boolean enabledHit(Button button, int x, int y) {
        return button != null && button.active && contains(button, x, y);
    }

    private static boolean contains(Button button, int x, int y) {
        return button != null
                && x >= button.getX() && x < button.getRight()
                && y >= button.getY() && y < button.getY() + button.getHeight();
    }

    @Override
    public void removed() {
        BattleCommandOverlayState.close();
        BattleTargetMarkerState.clear();
        super.removed();
    }

    private void publishTargetMarkers(BattlePresentationModel model, String hoveredTargetId) {
        if (selectedAction == null) {
            BattleTargetMarkerState.clear();
            return;
        }
        BattleTargetMarkerState.publish(
                model.battleId(),
                model.revision(),
                BattleCommandSelection.eligibleTargets(model, selectedAction),
                selectedTargetIds,
                hoveredTargetId);
    }

    /**
     * Maps a pointer position to a virtual participant only after intersecting with the current
     * server-authored eligible target set. Live world entities stay on the world-marker path.
     */
    private String virtualStageTargetAt(BattlePresentationModel model, double mouseX, double mouseY) {
        if (selectedAction == null || !UiLayoutMetrics.supportsBattleHud(this.width, this.height)) return null;
        List<BattleNetworkPayloads.SnapshotParticipant> eligible =
                BattleCommandSelection.eligibleTargets(model, selectedAction);
        if (eligible.isEmpty()) return null;

        Set<String> eligibleIds = new LinkedHashSet<>();
        for (BattleNetworkPayloads.SnapshotParticipant participant : eligible) {
            eligibleIds.add(participant.id());
        }

        UiLayoutMetrics.Rect viewport = UiLayoutMetrics.battleHud(this.width, this.height).reservedWorldViewport();
        BattleStageLayout.Layout layout = BattleStageLayout.arrange(
                viewport, model.playerParty().size(), model.enemies().size());
        BattleStageLayout.Slot slot = BattleStageLayout.slotAt(layout, mouseX, mouseY).orElse(null);
        if (slot == null) return null;

        List<BattleNetworkPayloads.SnapshotParticipant> participants =
                slot.side() == BattleStageLayout.Side.ENEMY ? model.enemies() : model.playerParty();
        if (slot.participantIndex() >= participants.size()) return null;
        BattleNetworkPayloads.SnapshotParticipant participant = participants.get(slot.participantIndex());

        if (participant.entityId() != null || !eligibleIds.contains(participant.id())) return null;
        return participant.id();
    }

    private UiLayoutMetrics.Rect commandRegion() {
        if (UiLayoutMetrics.supportsBattleHud(this.width, this.height)) {
            return UiLayoutMetrics.battleHud(this.width, this.height).commandStrip();
        }
        int margin = Math.min(UiLayoutMetrics.SPACE_8, Math.max(0, Math.min(this.width, this.height) / 8));
        int x = Math.min(margin, Math.max(0, this.width - 1));
        int width = Math.max(1, this.width - x - margin);
        int height = Math.max(20, Math.min(58, Math.max(20, this.height - margin * 2)));
        int y = Math.max(0, this.height - height - margin);
        return new UiLayoutMetrics.Rect(x, y, width, height);
    }

    private UiLayoutMetrics.TargetChooserLayout targetChooserLayout() {
        if (UiLayoutMetrics.supportsBattleHud(this.width, this.height)) {
            return UiLayoutMetrics.targetChooser(this.width, this.height);
        }
        UiLayoutMetrics.Rect region = commandRegion();
        int headerHeight = Math.min(18, Math.max(1, region.height() / 3));
        int gridY = region.y() + headerHeight;
        int gridHeight = Math.max(1, region.bottom() - gridY);
        UiLayoutMetrics.Rect header = new UiLayoutMetrics.Rect(region.x(), region.y(), region.width(), headerHeight);
        UiLayoutMetrics.Rect grid = new UiLayoutMetrics.Rect(region.x(), gridY, region.width(), gridHeight);
        int columns = region.width() >= 280 ? 3 : 2;
        int rows = Math.max(1, Math.min(2, grid.height() / 20));
        return new UiLayoutMetrics.TargetChooserLayout(region, header, grid, columns, rows, columns * rows);
    }

    private static String displayName(BattleNetworkPayloads.SnapshotParticipant participant) {
        String source = participant.characterId().isBlank() ? participant.id() : participant.characterId();
        return humanizeId(source);
    }

    private static String conciseActionName(String actionId, String actorName) {
        String name = BattleActionPresentation.actionName(actionId);
        if (actorName == null || actorName.isBlank()) return name;
        String prefix = actorName + " ";
        return name.regionMatches(true, 0, prefix, 0, prefix.length()) ? name.substring(prefix.length()) : name;
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
    private record TargetButtonBinding(Button button, String participantId) {}

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
