package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.BattleCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * World-first battle UI.  Reference-game hierarchy is deliberate:
 * world actors and target markers first, tiny party state at the bottom, current actions at the lower-right.
 */
public final class BattleScreen extends Screen {
    private static final int DEEP = 0xD80A0D12;
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int HP = 0xFF61E24B;
    private static final int HEAL = 0xFF62D39A;
    private static final int GAUGE = 0xFF6DC6FF;
    private static final int MUTED = 0xFF707987;
    private static final int DANGER = 0xFFFF5E57;
    private static final int GOLD = 0xFFFFC857;
    private static final long DOUBLE_COMMIT_MS = 560L;
    private static final long TIMELINE_MOTION_MS = 260L;

    private record SharedOwnerGroup(UUID ownerId, boolean local, List<Integer> unitIndices, BattleHudLayout.Rect rect) {}
    private record SharedPartyCell(int unitIndex, BattleHudLayout.Rect rect) {}

    private final List<BattleHudButton> skillButtons = new ArrayList<>();
    private BattleHudButton autoButton;
    private BattleHudButton speedButton;
    private BattleHudButton fleeButton;
    private BattleHudLayout.Layout layout;
    private String selectedSkill = "";
    private String selectedActor = "";
    private String focusedTargetId = "";
    private int selectedTarget = -1;
    private int selectedTarget2 = -1;
    private boolean settingsOpen;
    private long seen = -1;
    private String lastSkillClick = "";
    private long lastSkillClickAt;
    private int lastTargetClick = -1;
    private long lastTargetClickAt;
    private List<String> lastTimeline = List.of();
    private List<TurnOrderMotion.Move> timelineMotion = List.of();
    private long timelineMotionStartedAt;

    public BattleScreen() { super(Component.literal("TURNBOUND Battle")); }

    @Override
    protected void init() {
        super.init();
        skillButtons.clear();
        layout = BattleHudLayout.calculate(width, height);
        for (int i = 0; i < BattleHudLayout.SKILL_COUNT; i++) {
            final int index = i;
            var rect = layout.skillButtons().get(i);
            skillButtons.add(addRenderableWidget(new BattleHudButton(
                    rect.x(), rect.y(), rect.width(), rect.height(), Component.empty(), GAUGE,
                    ignored -> skill(index))));
        }
        var auto = layout.autoButton();
        autoButton = addRenderableWidget(new BattleHudButton(auto.x(), auto.y(), auto.width(), auto.height(),
                Component.literal("자동"), HEAL, ignored -> toggleAuto()));
        var speed = layout.speedButton();
        speedButton = addRenderableWidget(new BattleHudButton(speed.x(), speed.y(), speed.width(), speed.height(),
                Component.literal("×1"), GAUGE, ignored -> toggleSpeed()));
        var flee = layout.fleeButton();
        fleeButton = addRenderableWidget(new BattleHudButton(flee.x(), flee.y(), flee.width(), flee.height(),
                Component.literal("도주"), DANGER, ignored -> flee()));
        refresh();
    }

    @Override public void tick() { super.tick(); if (seen != ClientBattleState.revision()) refresh(); }

    private void refresh() {
        seen = ClientBattleState.revision();
        var snapshot = ClientBattleState.snapshot();
        List<String> nextTimeline = List.copyOf(snapshot.timeline().subList(0, Math.min(9, snapshot.timeline().size())));
        if (!lastTimeline.isEmpty() && !lastTimeline.equals(nextTimeline)) {
            timelineMotion = TurnOrderMotion.plan(lastTimeline, nextTimeline, 9);
            timelineMotionStartedAt = System.currentTimeMillis();
        } else if (lastTimeline.isEmpty()) {
            timelineMotion = List.of();
        }
        lastTimeline = nextTimeline;
        if (!Objects.equals(selectedActor, snapshot.actorId())) {
            selectedActor = snapshot.actorId();
            clearSelection(true);
        }
        if (!selectedSkill.isBlank() && snapshot.skills().stream().noneMatch(skill -> skill.id().equals(selectedSkill))) clearSelection(true);

        boolean canAct = canChooseSkill(snapshot) && !settingsOpen;
        for (int i = 0; i < skillButtons.size(); i++) {
            BattleHudButton button = skillButtons.get(i);
            if (i < snapshot.skills().size() && canAct) {
                var skill = snapshot.skills().get(i);
                String cooldown = skill.remaining() > 0 ? "  " + skill.remaining() : "";
                button.setMessage(Component.literal((i + 1) + "  " + skill.name() + cooldown));
                button.active = skill.remaining() == 0;
                button.visible = true;
                button.setSelected(selectedSkill.equals(skill.id()));
            } else {
                button.visible = false;
                button.setSelected(false);
            }
        }

        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null) {
            selectedTarget = -1;
            selectedTarget2 = -1;
            setWorldFocus("");
        } else {
            String rule = clientTargetRule(selected);
            if (BattleActionRules.needsManualTarget(rule)) {
                if (selectedTarget >= 0 && (selectedTarget >= snapshot.units().size()
                        || !BattleTargeting.validTarget(rule, snapshot.units().get(selectedTarget), snapshot.actorId()))) selectedTarget = -1;
                if (selectedTarget2 >= 0 && (selectedTarget2 >= snapshot.units().size()
                        || selectedTarget2 == selectedTarget
                        || !BattleTargeting.validTarget(rule, snapshot.units().get(selectedTarget2), snapshot.actorId()))) selectedTarget2 = -1;
                if (!"ENEMY_TWO".equals(rule)) selectedTarget2 = -1;
                syncSelectedTarget(snapshot);
            } else if ("ENEMY_ALL".equals(rule)) {
                if (selectedTarget >= 0 && (selectedTarget >= snapshot.units().size()
                        || !BattleTargeting.validTarget(rule, snapshot.units().get(selectedTarget), snapshot.actorId()))) {
                    selectedTarget = -1;
                }
                selectedTarget2 = -1;
                syncSelectedTarget(snapshot);
            } else if ("SELF".equals(rule)) {
                selectedTarget = BattleActionRules.defaultTarget(snapshot.units(), rule, snapshot.actorId());
                selectedTarget2 = -1;
                syncSelectedTarget(snapshot);
            } else {
                selectedTarget = -1;
                selectedTarget2 = -1;
                setWorldFocus("");
            }
        }

        BattleControlRules.State controls = BattleControlRules.state(snapshot);
        autoButton.setMessage(Component.literal(controls.autoLabel().replace("AUTO", "자동")));
        speedButton.setMessage(Component.literal(controls.speedLabel()));
        fleeButton.setMessage(Component.literal(controls.fleeLabel()));
        autoButton.visible = !settingsOpen && !snapshot.finished();
        speedButton.visible = !settingsOpen && !snapshot.finished();
        fleeButton.visible = !settingsOpen;
        autoButton.active = controls.autoActive();
        speedButton.active = controls.speedActive();
        fleeButton.active = controls.fleeActive();
    }

    private static boolean canChooseSkill(ClientBattleState.Snapshot snapshot) {
        if (snapshot.finished() || snapshot.auto() || snapshot.actorId().isBlank()) return false;
        return snapshot.units().stream().anyMatch(unit ->
                unit.id().equals(snapshot.actorId()) && "ALLY".equals(unit.side()));
    }

    private ClientBattleState.Skill selectedSkill(ClientBattleState.Snapshot snapshot) {
        if (selectedSkill.isBlank()) return null;
        return snapshot.skills().stream().filter(skill -> skill.id().equals(selectedSkill)).findFirst().orElse(null);
    }

    /** Client-only refinement for skills whose server effects explicitly forbid targeting the acting ally. */
    private static String clientTargetRule(ClientBattleState.Skill skill) {
        if (skill == null) return "";
        return switch (skill.id()) {
            case "p02_time_leap", "p03_guard_transfer" -> "ALLY_SINGLE_EXCEPT_SELF";
            default -> skill.targetRule();
        };
    }

    private static boolean supportsTargetClick(String rule) {
        return BattleActionRules.needsManualTarget(rule) || "ENEMY_ALL".equals(rule);
    }

    /** First click selects. Clicking the same skill again commits, choosing the first valid target only at commit time. */
    private void skill(int index) {
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        if (!canChooseSkill(snapshot) || index < 0 || index >= snapshot.skills().size()) return;
        var skill = snapshot.skills().get(index);
        if (skill.remaining() > 0) return;
        long now = System.currentTimeMillis();
        boolean repeated = selectedSkill.equals(skill.id()) && lastSkillClick.equals(skill.id()) && now - lastSkillClickAt <= DOUBLE_COMMIT_MS;
        selectedSkill = skill.id();
        String rule = clientTargetRule(skill);
        if ("SELF".equals(rule)) {
            selectedTarget = BattleActionRules.defaultTarget(snapshot.units(), rule, snapshot.actorId());
            selectedTarget2 = -1;
        } else if (BattleActionRules.needsManualTarget(rule)) {
            if (selectedTarget >= 0 && (selectedTarget >= snapshot.units().size()
                    || !BattleTargeting.validTarget(rule, snapshot.units().get(selectedTarget), snapshot.actorId()))) selectedTarget = -1;
            if (!"ENEMY_TWO".equals(rule)) selectedTarget2 = -1;
            if (repeated && selectedTarget < 0) {
                selectedTarget = BattleActionRules.defaultTarget(snapshot.units(), rule, snapshot.actorId());
            }
            if (repeated && "ENEMY_TWO".equals(rule)
                    && BattleActionRules.requiredTargetCount(snapshot.units(), rule, snapshot.actorId()) > 1
                    && selectedTarget >= 0 && selectedTarget2 < 0) {
                selectedTarget2 = BattleTargeting.cycle(snapshot.units(), rule, snapshot.actorId(), selectedTarget, 1, selectedTarget);
            }
        } else if ("ENEMY_ALL".equals(rule)) {
            if (selectedTarget >= 0 && (selectedTarget >= snapshot.units().size()
                    || !BattleTargeting.validTarget(rule, snapshot.units().get(selectedTarget), snapshot.actorId()))) {
                selectedTarget = -1;
            }
            selectedTarget2 = -1;
        } else {
            selectedTarget = -1;
            selectedTarget2 = -1;
        }
        lastSkillClick = skill.id();
        lastSkillClickAt = now;
        syncSelectedTarget(snapshot);
        refresh();
        if (repeated) confirmAction();
    }

    /** First click selects. A second click on that same visible actor commits the pending skill. */
    private void selectTarget(int index, boolean platformDoubleClick) {
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        String rule = clientTargetRule(selected);
        if (selected == null || !supportsTargetClick(rule)) return;
        if (index < 0 || index >= snapshot.units().size()) return;
        if (!BattleTargeting.validTarget(rule, snapshot.units().get(index), snapshot.actorId())) return;
        long now = System.currentTimeMillis();
        if ("ENEMY_ALL".equals(rule)) {
            boolean repeated = selectedTarget == index
                    && (platformDoubleClick || (lastTargetClick == index && now - lastTargetClickAt <= DOUBLE_COMMIT_MS));
            selectedTarget = index;
            selectedTarget2 = -1;
            if (repeated) {
                lastTargetClick = index;
                lastTargetClickAt = now;
                syncSelectedTarget(snapshot);
                refresh();
                confirmAction();
                return;
            }
        } else if ("ENEMY_TWO".equals(rule)) {
            int required = BattleActionRules.requiredTargetCount(snapshot.units(), rule, snapshot.actorId());
            if (selectedTarget < 0) {
                selectedTarget = index;
                selectedTarget2 = -1;
            } else if (required <= 1) {
                boolean repeated = selectedTarget == index
                        && (platformDoubleClick || (lastTargetClick == index && now - lastTargetClickAt <= DOUBLE_COMMIT_MS));
                selectedTarget = index;
                if (repeated) {
                    lastTargetClick = index;
                    lastTargetClickAt = now;
                    syncSelectedTarget(snapshot);
                    refresh();
                    confirmAction();
                    return;
                }
            } else if (index != selectedTarget) {
                boolean repeated = selectedTarget2 == index
                        && (platformDoubleClick || (lastTargetClick == index && now - lastTargetClickAt <= DOUBLE_COMMIT_MS));
                selectedTarget2 = index;
                if (repeated) {
                    lastTargetClick = index;
                    lastTargetClickAt = now;
                    syncSelectedTarget(snapshot);
                    refresh();
                    confirmAction();
                    return;
                }
            }
        } else {
            boolean repeated = selectedTarget == index
                    && (platformDoubleClick || (lastTargetClick == index && now - lastTargetClickAt <= DOUBLE_COMMIT_MS));
            selectedTarget = index;
            if (repeated) {
                lastTargetClick = index;
                lastTargetClickAt = now;
                syncSelectedTarget(snapshot);
                refresh();
                confirmAction();
                return;
            }
        }
        lastTargetClick = index;
        lastTargetClickAt = now;
        syncSelectedTarget(snapshot);
        refresh();
    }

    private void cycleTarget(int direction) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        String rule = clientTargetRule(selected);
        if (selected == null || !BattleActionRules.needsManualTarget(rule)) return;
        if ("ENEMY_TWO".equals(rule)) {
            int required = BattleActionRules.requiredTargetCount(snapshot.units(), rule, snapshot.actorId());
            if (selectedTarget < 0) {
                selectedTarget = BattleTargeting.firstValid(snapshot.units(), rule, snapshot.actorId());
            } else if (required > 1) {
                selectedTarget2 = BattleTargeting.cycle(snapshot.units(), rule, snapshot.actorId(),
                        selectedTarget2 >= 0 ? selectedTarget2 : selectedTarget, direction, selectedTarget);
            }
        } else {
            selectedTarget = BattleTargeting.cycle(snapshot.units(), rule, snapshot.actorId(), selectedTarget, direction);
        }
        syncSelectedTarget(snapshot);
        refresh();
    }

    private void confirmAction() {
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null || !canChooseSkill(snapshot)) return;
        String rule = clientTargetRule(selected);
        String targetId = BattleActionRules.confirmedTargets(snapshot.units(), rule, snapshot.actorId(), selectedTarget, selectedTarget2);
        if (targetId == null) return;
        send("ACT|" + snapshot.actorId() + "|" + selected.id() + "|" + targetId);
        clearSelection(true);
    }

    private void clearSelection(boolean clearWorldFocus) {
        selectedSkill = "";
        selectedTarget = -1;
        selectedTarget2 = -1;
        lastTargetClick = -1;
        lastTargetClickAt = 0L;
        if (clearWorldFocus) setWorldFocus("");
    }

    private void syncSelectedTarget(ClientBattleState.Snapshot snapshot) {
        if (selectedTarget2 >= 0 && selectedTarget2 < snapshot.units().size()) setWorldFocus(snapshot.units().get(selectedTarget2).id());
        else if (selectedTarget >= 0 && selectedTarget < snapshot.units().size()) setWorldFocus(snapshot.units().get(selectedTarget).id());
        else setWorldFocus("");
    }

    private void setWorldFocus(String targetId) {
        String normalized = targetId == null ? "" : targetId;
        if (Objects.equals(focusedTargetId, normalized)) return;
        focusedTargetId = normalized;
        send("FOCUS|" + normalized);
    }

    private void toggleAuto() {
        var snapshot = ClientBattleState.snapshot();
        if (settingsOpen || snapshot.finished() || !snapshot.autoAllowed()) return;
        clearSelection(true);
        send("AUTO");
    }

    private void toggleSpeed() {
        var snapshot = ClientBattleState.snapshot();
        if (!settingsOpen && !snapshot.finished() && snapshot.speedAllowed()) send("SPEED");
    }

    private void flee() {
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        if (!snapshot.finished() && !snapshot.fleeAllowed()) return;
        clearSelection(true);
        send("FLEE");
    }

    private static void send(String command) { ClientPacketDistributor.sendToServer(new BattleCommandPayload(command)); }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            settingsOpen = !settingsOpen;
            if (settingsOpen) clearSelection(true);
            refresh();
            return true;
        }
        if (settingsOpen) return true;
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_5) { skill(key - GLFW.GLFW_KEY_1); return true; }
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> { if (!selectedSkill.isBlank()) { cycleTarget(event.hasShiftDown() ? -1 : 1); return true; } }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { confirmAction(); return true; }
            case GLFW.GLFW_KEY_A -> { toggleAuto(); return true; }
            case GLFW.GLFW_KEY_X -> { toggleSpeed(); return true; }
            case GLFW.GLFW_KEY_R -> { flee(); return true; }
            case GLFW.GLFW_KEY_C -> { BattleCameraController.resetView(); return true; }
            case GLFW.GLFW_KEY_LEFT -> { BattleCameraController.nudgeOrbit(-12.0F, 0.0F); return true; }
            case GLFW.GLFW_KEY_RIGHT -> { BattleCameraController.nudgeOrbit(12.0F, 0.0F); return true; }
            case GLFW.GLFW_KEY_UP -> { BattleCameraController.nudgeOrbit(0.0F, -4.0F); return true; }
            case GLFW.GLFW_KEY_DOWN -> { BattleCameraController.nudgeOrbit(0.0F, 4.0F); return true; }
            default -> { }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (settingsOpen) settingsOpen = false;
            else if (!selectedSkill.isBlank()) clearSelection(true);
            refresh();
            return true;
        }
        if (settingsOpen) return true;
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && !selectedSkill.isBlank()) {
            int clicked = worldTargetAt(event.x(), event.y());
            if (clicked < 0) clicked = hudTargetAt(event.x(), event.y());
            if (clicked >= 0) { selectTarget(clicked, doubleClick); return true; }
        }
        return event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        // Left drag stays reserved for target selection. Right/middle drag always orbit, even while a skill is armed.
        boolean freeLeftDrag = event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && selectedSkill.isBlank();
        boolean cameraButton = freeLeftDrag
                || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
        if (!settingsOpen && cameraButton && !isOverInteractiveHud(event.x(), event.y())) {
            BattleCameraController.orbit(deltaX, deltaY);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!settingsOpen && scrollY != 0.0D) { BattleCameraController.zoom(scrollY); return true; }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isOverInteractiveHud(double x, double y) {
        var current = currentLayout();
        if (current.actionHeader().contains(x, y)) return true;
        for (var rect : current.skillButtons()) if (rect.contains(x, y)) return true;
        List<SharedOwnerGroup> shared = sharedOwnerGroups(ClientBattleState.snapshot(), current);
        if (!shared.isEmpty()) {
            for (SharedOwnerGroup group : shared) if (group.rect().contains(x, y)) return true;
        } else {
            for (var rect : current.allyBars()) if (rect.contains(x, y)) return true;
        }
        return current.autoButton().contains(x, y) || current.speedButton().contains(x, y) || current.fleeButton().contains(x, y);
    }

    private int worldTargetAt(double x, double y) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        String rule = clientTargetRule(selected);
        if (selected == null || !supportsTargetClick(rule)) return -1;
        return BattleLiveProjection.pick(snapshot.units(), rule, snapshot.actorId(), width, height, x, y);
    }

    private int hudTargetAt(double x, double y) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        String rule = clientTargetRule(selected);
        if (selected == null || !supportsTargetClick(rule)) return -1;

        var current = currentLayout();
        List<SharedOwnerGroup> shared = sharedOwnerGroups(snapshot, current);
        if (!shared.isEmpty()) {
            for (SharedOwnerGroup group : shared) {
                for (SharedPartyCell cell : sharedPartyCells(group)) {
                    if (cell.rect().contains(x, y)) {
                        var unit = snapshot.units().get(cell.unitIndex());
                        if (BattleTargeting.validTarget(rule, unit, snapshot.actorId())) return cell.unitIndex();
                    }
                }
            }
            return -1;
        }

        int allySlot = 0;
        for (int i = 0; i < snapshot.units().size(); i++) {
            var unit = snapshot.units().get(i);
            if (!"ALLY".equals(unit.side())) continue;
            int slot = allySlot++;
            if (slot < current.allyBars().size() && current.allyBars().get(slot).contains(x, y)
                    && BattleTargeting.validTarget(rule, unit, snapshot.actorId())) return i;
        }
        return -1;
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        var snapshot = ClientBattleState.snapshot();
        var current = currentLayout();
        drawTimeline(graphics, current, snapshot);
        drawParty(graphics, current, snapshot);
        drawWorldStatus(graphics, snapshot);
        drawActionHeader(graphics, current, snapshot);
        drawResult(graphics, snapshot);
        drawSkillTooltip(graphics, current, snapshot, mouseX, mouseY);
        if (settingsOpen) drawSettings(graphics, current, snapshot);
    }

    /**
     * Compact CR-style action gauge.
     *
     * The row order is the server TurnScheduler preview, while the thin bar shows current Gauge progress.
     * Raw Gauge/SPD numbers are intentionally hidden from the default combat view to preserve glanceability.
     */
    private void drawTimeline(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        var panel = current.timeline();
        int maxRows = height < 200 ? 5 : current.compact() ? 7 : 9;
        List<BattleTurnGaugeModel.Row> rows = BattleTurnGaugeModel.rows(snapshot, maxRows);
        if (rows.isEmpty()) return;

        int total = BattleTurnGaugeModel.scheduledLivingCount(snapshot);
        int hidden = Math.max(0, total - rows.size());
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0x66080A0E);
        TurnboundFrameStyle.frame(graphics, panel.x(), panel.y(), panel.width(), panel.height(), 0x784B5668);

        String title = hidden > 0 ? "행동 순서 +" + hidden : "행동 순서";
        graphics.text(font, Component.literal(title), panel.x() + 5, panel.y() + 3, TEXT, true);

        int rowH = height < 200 ? 11 : current.compact() ? 12 : 13;
        int y = panel.y() + (height < 200 ? 12 : 14);
        long elapsed = Math.max(0L, System.currentTimeMillis() - timelineMotionStartedAt);
        boolean motionActive = elapsed < TIMELINE_MOTION_MS && !timelineMotion.isEmpty();

        int rank = 1;
        for (BattleTurnGaugeModel.Row row : rows) {
            ClientBattleState.Unit unit = row.unit();
            boolean actor = unit.id().equals(snapshot.actorId());
            boolean tempoJump = motionActive && timelineMotion.stream()
                    .anyMatch(move -> move.unitId().equals(unit.id()) && move.tempoJump());
            int sideColor = "ALLY".equals(unit.side()) ? GAUGE : DANGER;
            int rowBg = actor ? 0x8C2A3442 : tempoJump ? 0x48304B63 : 0x22080A0E;

            graphics.fill(panel.x() + 3, y, panel.right() - 3, y + rowH - 1, rowBg);
            if (actor) graphics.fill(panel.x() + 3, y, panel.x() + 5, y + rowH - 1, GOLD);

            int rankW = height < 200 ? 9 : 11;
            drawScaledText(graphics, Integer.toString(rank), panel.x() + 5 + rankW, y + 2,
                    current.compact() ? 0.52F : 0.58F, actor ? GOLD : SECONDARY, true);

            int portrait = Math.max(8, rowH - 2);
            int portraitX = panel.x() + 7 + rankW;
            int portraitY = y + 1;
            boolean rendered = TurnboundPortraitRenderer.extract(
                    graphics, unit.defId(), portraitX, portraitY,
                    portraitX + portrait, portraitY + portrait, unit.downed());
            if (!rendered) {
                String fallback = abbreviate(unit.name(), 1);
                drawScaledText(graphics, fallback, portraitX + portrait / 2, portraitY + 2,
                        0.60F, TEXT, true);
            }

            int textX = portraitX + portrait + 4;
            int right = panel.right() - 5;
            String name = UiTextLayout.fit(unit.name(), Math.max(16, right - textX));
            graphics.text(font, Component.literal(name), textX, y + 1, actor ? GOLD : TEXT, true);

            int barX = textX;
            int barY = y + rowH - 3;
            int barW = Math.max(5, right - barX);
            graphics.fill(barX, barY, right, barY + 2, 0xC0080A0E);
            int fill = (int)Math.round(barW * BattleTurnGaugeModel.gaugeRatio(unit));
            if (fill > 0) graphics.fill(barX, barY, barX + Math.min(barW, fill), barY + 2, actor ? GOLD : sideColor);
            if (tempoJump) graphics.fill(barX, barY - 1, right, barY, 0x806DC6FF);

            y += rowH;
            rank++;
            if (y + rowH > panel.bottom() - 2) break;
        }
    }

    /** Right-aligned when requested; otherwise x is treated as the left edge. */
    private void drawScaledText(GuiGraphicsExtractor graphics, String text, float x, float y,
                                float scale, int color, boolean rightAligned) {
        if (text == null || text.isBlank()) return;
        float drawX = rightAligned ? x - font.width(text) * scale : x;
        graphics.pose().pushMatrix();
        graphics.pose().translate(drawX, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, Component.literal(text), 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    private void drawParty(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        List<SharedOwnerGroup> shared = sharedOwnerGroups(snapshot, current);
        if (!shared.isEmpty()) {
            drawSharedParty(graphics, snapshot, shared);
            return;
        }
        int slot = 0;
        for (int i = 0; i < snapshot.units().size(); i++) {
            var unit = snapshot.units().get(i);
            if (!"ALLY".equals(unit.side()) || slot >= current.allyBars().size()) continue;
            drawPartyLine(graphics, current.allyBars().get(slot++), unit, i == selectedTarget || i == selectedTarget2, unit.id().equals(snapshot.actorId()));
        }
    }

    private void drawSharedParty(GuiGraphicsExtractor graphics, ClientBattleState.Snapshot snapshot, List<SharedOwnerGroup> groups) {
        for (SharedOwnerGroup group : groups) {
            boolean actingGroup = group.unitIndices().stream().anyMatch(index -> snapshot.units().get(index).id().equals(snapshot.actorId()));
            int accent = actingGroup ? GOLD : group.local() ? GAUGE : 0x884B5668;
            var rect = group.rect();
            graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0x52080A0E);
            graphics.fill(rect.x(), rect.y(), rect.x() + 2, rect.bottom(), accent);

            String owner = ownerLabel(group.ownerId());
            if (group.local()) owner = "나 · " + owner;
            String title = UiTextLayout.fit(owner, Math.max(18, rect.width() - 10));
            graphics.text(font, Component.literal(title), rect.x() + 6, rect.y() + 3, actingGroup ? GOLD : group.local() ? TEXT : SECONDARY, true);

            for (SharedPartyCell cell : sharedPartyCells(group)) {
                var unit = snapshot.units().get(cell.unitIndex());
                drawSharedPartyCell(graphics, cell.rect(), unit,
                        cell.unitIndex() == selectedTarget || cell.unitIndex() == selectedTarget2, unit.id().equals(snapshot.actorId()));
            }
        }
    }

    private void drawSharedPartyCell(GuiGraphicsExtractor graphics, BattleHudLayout.Rect rect,
                                     ClientBattleState.Unit unit, boolean selected, boolean actor) {
        int accent = selected ? GAUGE : actor ? GOLD : unit.downed() ? MUTED : 0x553E4755;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0x36080A0E);
        graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), accent);

        int nameReserve = unit.downed() ? 14 : 5;
        String name = UiTextLayout.fit(unit.name(), Math.max(8, rect.width() - nameReserve));
        graphics.text(font, Component.literal(name), rect.x() + 3, rect.y() + 2,
                unit.downed() ? MUTED : actor ? GOLD : TEXT, false);
        if (unit.downed()) {
            BattleDownedIndicator.drawSkeletonSkull(graphics, rect.right() - 7, rect.y() + 6, 0.70F);
        }
        int barX = rect.x() + 3;
        int barW = Math.max(3, rect.width() - 6);
        int barY = rect.bottom() - 3;
        graphics.fill(barX, barY, barX + barW, barY + 2, 0xD0080A0E);
        int hpW = unit.maxHp() <= 0 ? 0 : (int)Math.round(barW * Math.max(0, unit.hp()) / (double)unit.maxHp());
        if (hpW > 0) graphics.fill(barX, barY, barX + Math.min(barW, hpW), barY + 2, HP);
    }

    private List<SharedOwnerGroup> sharedOwnerGroups(ClientBattleState.Snapshot snapshot, BattleHudLayout.Layout current) {
        if (snapshot == null || current.allyBars().isEmpty() || !ClientBattleState.sharedBattle()) return List.of();
        Map<String, ClientBattleState.ActorOwner> owners = ClientBattleState.actorOwners();
        LinkedHashMap<UUID, List<Integer>> grouped = new LinkedHashMap<>();
        LinkedHashMap<UUID, Boolean> local = new LinkedHashMap<>();

        for (int i = 0; i < snapshot.units().size(); i++) {
            var unit = snapshot.units().get(i);
            if (!"ALLY".equals(unit.side())) continue;
            var owner = owners.get(unit.id());
            if (owner == null) continue;
            grouped.computeIfAbsent(owner.playerId(), ignored -> new ArrayList<>()).add(i);
            if (owner.local()) local.put(owner.playerId(), true);
            else local.putIfAbsent(owner.playerId(), false);
        }
        if (grouped.size() <= 1) return List.of();

        int areaLeft = current.allyBars().getFirst().x();
        int areaRight = current.allyBars().getLast().right();
        int count = grouped.size();
        boolean grid = current.compact() && count > 2;
        int cols = grid ? 2 : count;
        int rows = grid ? 2 : 1;
        int gap = 4;
        int areaWidth = Math.max(1, areaRight - areaLeft);
        int groupW = Math.max(38, Math.min(grid ? 120 : 150, (areaWidth - gap * (cols - 1)) / cols));
        int groupH = current.compact() ? 40 : 48;
        int totalH = rows * groupH + gap * (rows - 1);
        int bottom = current.allyBars().getFirst().bottom();
        int startY = Math.max(current.timeline().bottom() + 4, bottom - totalH);

        List<SharedOwnerGroup> out = new ArrayList<>();
        int index = 0;
        for (var entry : grouped.entrySet()) {
            int col = index % cols;
            int row = index / cols;
            int x = areaLeft + col * (groupW + gap);
            int y = startY + row * (groupH + gap);
            out.add(new SharedOwnerGroup(entry.getKey(), local.getOrDefault(entry.getKey(), false),
                    List.copyOf(entry.getValue()), new BattleHudLayout.Rect(x, y, groupW, groupH)));
            index++;
        }
        return List.copyOf(out);
    }

    private List<SharedPartyCell> sharedPartyCells(SharedOwnerGroup group) {
        int count = group.unitIndices().size();
        if (count == 0) return List.of();
        int cols = count >= 5 ? 3 : count == 1 ? 1 : 2;
        int rows = (count + cols - 1) / cols;
        int gap = 1, headerH = 13;
        int innerX = group.rect().x() + 4;
        int innerY = group.rect().y() + headerH;
        int innerW = Math.max(1, group.rect().width() - 7);
        int innerH = Math.max(1, group.rect().height() - headerH - 3);
        int cellW = Math.max(8, (innerW - gap * (cols - 1)) / cols);
        int cellH = Math.max(8, (innerH - gap * (rows - 1)) / rows);
        List<SharedPartyCell> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int col = i % cols, row = i / cols;
            out.add(new SharedPartyCell(group.unitIndices().get(i),
                    new BattleHudLayout.Rect(innerX + col * (cellW + gap), innerY + row * (cellH + gap), cellW, cellH)));
        }
        return List.copyOf(out);
    }

    private static String ownerLabel(UUID ownerId) {
        if (ownerId == null) return "동료";
        return ClientMultiplayerPartyState.snapshot().members().stream()
                .filter(member -> ownerId.equals(member.id()))
                .map(ClientMultiplayerPartyState.Member::name)
                .findFirst().orElse("동료 " + ownerId.toString().substring(0, 4));
    }

    /** Compact party status with a shared live-3D portrait, HP and only the essential state. */
    private void drawPartyLine(GuiGraphicsExtractor graphics, BattleHudLayout.Rect rect, ClientBattleState.Unit unit, boolean selected, boolean actor) {
        int accent = selected ? GAUGE : actor ? GOLD : 0x884B5668;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0x42080A0E);
        graphics.fill(rect.x(), rect.y(), rect.x() + 2, rect.bottom(), accent);

        int portrait = Math.max(12, rect.height() - 3);
        int portraitX = rect.x() + 3;
        int portraitY = rect.y() + 1;
        boolean rendered = TurnboundPortraitRenderer.extract(
                graphics, unit.defId(), portraitX, portraitY, portraitX + portrait, portraitY + portrait, unit.downed());
        int textX = rendered ? portraitX + portrait + 3 : rect.x() + 5;

        String hpText = unit.downed() ? "" : unit.hp() + "/" + unit.maxHp();
        int hpTextW = font.width(hpText);
        int nameMax = Math.max(12, rect.right() - hpTextW - 4 - textX - 4);
        String name = UiTextLayout.fit(unit.name(), nameMax);
        graphics.text(font, Component.literal(name), textX, rect.y() + 2, unit.downed() ? MUTED : TEXT, true);
        if (!hpText.isBlank()) {
            graphics.text(font, Component.literal(hpText), rect.right() - hpTextW - 4, rect.y() + 2, SECONDARY, false);
        }

        int barX = textX;
        int barY = rect.bottom() - 4;
        int barW = Math.max(4, rect.right() - 4 - barX);
        graphics.fill(barX, barY, barX + barW, barY + 2, 0xD0080A0E);
        int hpW = unit.maxHp() <= 0 ? 0 : (int)Math.round(barW * Math.max(0, unit.hp()) / (double)unit.maxHp());
        if (hpW > 0) graphics.fill(barX, barY, barX + Math.min(barW, hpW), barY + 2, HP);
    }

    /** Enemy name/HP and target selection stay attached to the actual 3D actor. */
    private void drawWorldStatus(GuiGraphicsExtractor graphics, ClientBattleState.Snapshot snapshot) {
        ClientBattleState.Skill armedSkill = selectedSkill(snapshot);
        String armedRule = clientTargetRule(armedSkill);
        boolean allEnemiesSelected = "ENEMY_ALL".equals(armedRule) && selectedTarget >= 0;
        for (int i = 0; i < snapshot.units().size(); i++) {
            var unit = snapshot.units().get(i);
            var point = BattleLiveProjection.project(unit.x(), unit.y() + 2.05, unit.z(), width, height);
            if (point == null) continue;
            boolean enemy = "ENEMY".equals(unit.side());
            boolean selected = i == selectedTarget || i == selectedTarget2
                    || (allEnemiesSelected && enemy && !unit.downed());
            boolean actor = unit.id().equals(snapshot.actorId());
            if (!enemy && !selected && !actor) continue;

            int cx = (int)Math.round(point.x());
            int y = (int)Math.round(point.y());
            if (selected) {
                drawTargetArrow(graphics, cx, y - 11, enemy ? DANGER : GAUGE);
                if (selectedTarget2 >= 0 && !"ENEMY_ALL".equals(armedRule)) {
                    String order = i == selectedTarget ? "1" : "2";
                    graphics.text(font, Component.literal(order), cx + 7, y - 20, GOLD, true);
                }
            } else if (actor) graphics.text(font, Component.literal("◆"), cx - 4, y - 17, GOLD, true);

            if (enemy) {
                int barW = 56;
                int x = cx - barW / 2;
                String name = UiTextLayout.fit(unit.name(), 70);
                String hpText = unit.hp() + "/" + unit.maxHp();
                int infoW = font.width(name) + 5 + font.width(hpText);
                int infoX = cx - infoW / 2;
                graphics.text(font, Component.literal(name), infoX, y - 2, TEXT, true);
                graphics.text(font, Component.literal(hpText), infoX + font.width(name) + 5, y - 2, SECONDARY, false);
                graphics.fill(x, y + 9, x + barW, y + 12, 0xD0000000);
                int hpW = unit.maxHp() <= 0 ? 0 : (int)Math.round(barW * Math.max(0, unit.hp()) / (double)unit.maxHp());
                if (hpW > 0) graphics.fill(x, y + 9, x + Math.min(barW, hpW), y + 12, HP);
            }
        }
    }

    /** Large spatial target cue copied as an interaction principle, not as an art asset. */
    private void drawTargetArrow(GuiGraphicsExtractor graphics, int cx, int tipY, int color) {
        int shadow = 0x90000000 | (color & 0x00FFFFFF);
        graphics.fill(cx - 3, tipY - 22, cx + 4, tipY - 9, shadow);
        graphics.fill(cx - 2, tipY - 21, cx + 3, tipY - 8, color);
        graphics.fill(cx - 7, tipY - 10, cx + 8, tipY - 7, shadow);
        graphics.fill(cx - 6, tipY - 9, cx + 7, tipY - 6, color);
        graphics.fill(cx - 4, tipY - 6, cx + 5, tipY - 3, color);
        graphics.fill(cx - 2, tipY - 3, cx + 3, tipY, color);
    }

    /** The reference leaves only a short targeting instruction above the action buttons. */
    private void drawActionHeader(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        if (settingsOpen) return;
        var rect = current.actionHeader();
        if (!canChooseSkill(snapshot)) {
            var owner = ClientBattleState.ownerOf(snapshot.actorId());
            ClientBattleState.Unit actor = findUnit(snapshot, snapshot.actorId());
            if (owner != null && !owner.local() && actor != null && "ALLY".equals(actor.side())) {
                String waiting = ownerLabel(owner.playerId()) + " · " + actor.name() + " 행동";
                String fittedWaiting = UiTextLayout.fit(waiting, Math.max(12, rect.width() - 4));
                int waitingX = rect.x() + Math.max(2, (rect.width() - font.width(fittedWaiting)) / 2);
                graphics.text(font, Component.literal(fittedWaiting), waitingX, rect.y() + 5, GOLD, true);
            }
            return;
        }
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        String rule = clientTargetRule(selected);
        String contextual = snapshot.message() == null ? "" : snapshot.message().trim();
        boolean teaching = selected == null && !contextual.isBlank();
        int requiredTargets = selected == null ? 0
                : BattleActionRules.requiredTargetCount(snapshot.units(), rule, snapshot.actorId());
        String hint = teaching ? contextual
                : selected == null ? "스킬을 선택하세요"
                : "ENEMY_ALL".equals(rule) && selectedTarget < 0 ? "적을 선택하면 전체 적이 지정됩니다"
                : "ENEMY_ALL".equals(rule) ? "같은 적을 한 번 더 클릭해 전체 공격 사용"
                : "ENEMY_TWO".equals(rule) && selectedTarget < 0 ? "첫 번째 대상을 선택하세요"
                : "ENEMY_TWO".equals(rule) && requiredTargets > 1 && selectedTarget2 < 0 ? "두 번째 대상을 선택하세요"
                : BattleActionRules.needsSingleTarget(rule) && selectedTarget < 0 ? "대상을 선택하세요"
                : "한 번 더 클릭해 사용";
        String fitted = UiTextLayout.fit(hint, Math.max(12, rect.width() - 4));
        int x = rect.x() + Math.max(2, (rect.width() - font.width(fitted)) / 2);
        graphics.text(font, Component.literal(fitted), x, rect.y() + 5,
                teaching ? GOLD : selected == null ? SECONDARY : TEXT, true);
    }

    private void drawSkillTooltip(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot, int mouseX, int mouseY) {
        if (!canChooseSkill(snapshot) || settingsOpen) return;
        int hovered = -1;
        for (int i = 0; i < current.skillButtons().size() && i < snapshot.skills().size(); i++) {
            if (current.skillButtons().get(i).contains(mouseX, mouseY)) { hovered = i; break; }
        }
        if (hovered < 0) return;

        ClientBattleState.Skill skill = snapshot.skills().get(hovered);
        var base = current.tooltipArea();
        float textScale = current.compact() ? 0.86F : 0.90F;

        int dockLeft = current.actionHeader().x();
        int availableWidth = Math.max(base.width(), dockLeft - 10);
        int expandedWidth = Math.min(current.compact() ? 220 : 300, availableWidth);
        int panelWidth = base.width();

        List<String> lines = tooltipLines(skill, panelWidth, textScale);
        if (lines.size() > 5 && expandedWidth > panelWidth) {
            panelWidth = expandedWidth;
            lines = tooltipLines(skill, panelWidth, textScale);
        }

        int topLimit = current.timeline().bottom() + 4;
        int bottomLimit = Math.max(topLimit + 24, current.autoButton().y() - 5);
        int maxHeight = Math.max(24, bottomLimit - topLimit);
        int desiredHeight = 10 + (int)Math.ceil(lines.size() * 11.0F * textScale);
        if (desiredHeight > maxHeight && !lines.isEmpty()) {
            textScale = Math.max(0.80F, (maxHeight - 10.0F) / (lines.size() * 11.0F));
            lines = tooltipLines(skill, panelWidth, textScale);
            desiredHeight = 10 + (int)Math.ceil(lines.size() * 11.0F * textScale);
        }

        int panelHeight = Math.min(maxHeight, Math.max(base.height(), desiredHeight));
        int panelX = Math.max(4, dockLeft - 6 - panelWidth);
        int panelY = Math.max(topLimit, Math.min(base.y(), bottomLimit - panelHeight));

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, DEEP);
        TurnboundFrameStyle.frame(graphics, panelX, panelY, panelWidth, panelHeight, GAUGE);

        graphics.pose().pushMatrix();
        graphics.pose().translate(panelX + 7, panelY + 6);
        graphics.pose().scale(textScale, textScale);
        int localY = 0;
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(font, Component.literal(lines.get(i)), 0, localY, i == 0 ? TEXT : SECONDARY, true);
            localY += 11;
        }
        graphics.pose().popMatrix();
    }

    private List<String> tooltipLines(ClientBattleState.Skill skill, int panelWidth, float textScale) {
        int logicalWidth = Math.max(48, (int)Math.floor((panelWidth - 14) / Math.max(0.01F, textScale)));
        List<String> lines = new ArrayList<>();
        for (String source : BattleSkillTooltip.lines(skill)) lines.addAll(wrap(source, logicalWidth));
        return lines;
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (current.length() > 0 && font.width(current.toString() + c) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
            }
            current.append(c);
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private void drawResult(GuiGraphicsExtractor graphics, ClientBattleState.Snapshot snapshot) {
        if (!snapshot.finished()) return;
        String line = ("ALLY_VICTORY".equals(snapshot.outcome()) ? "승리" : "패배") + "  ·  R 복귀";
        int w = font.width(line) + 20;
        int x = (width - w) / 2;
        int y = height / 2 - 10;
        TurnboundFrameStyle.frame(graphics, x, y, w, 22, "ALLY_VICTORY".equals(snapshot.outcome()) ? HEAL : DANGER);
        graphics.text(font, Component.literal(line), x + 10, y + 7, TEXT, true);
    }

    private void drawSettings(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        var panel = current.settingsPanel();
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), DEEP);
        TurnboundFrameStyle.frame(graphics, panel.x(), panel.y(), panel.width(), panel.height(), GAUGE);
        int x = panel.x() + 12;
        int y = panel.y() + 10;
        graphics.text(font, Component.literal("전투 조작"), x, y, TEXT, true);
        graphics.text(font, Component.literal("빈 공간 드래그/방향키 회전 · 휠 줌 · C 초기화"), x, y + 18, SECONDARY, true);
        graphics.text(font, Component.literal("캐릭터/Tab 대상 · 같은 대상 2번 = 사용"), x, y + 34, SECONDARY, true);
        graphics.text(font, Component.literal("같은 스킬 2번 = 자동 대상 후 사용 · Enter 확정"), x, y + 50, SECONDARY, true);
        String controls = (snapshot.autoAllowed() ? "A 자동" : "A 자동 잠금") + " · "
                + (snapshot.speedAllowed() ? "X 배속" : "X 배속 잠금") + " · "
                + (snapshot.fleeAllowed() ? "R 도주" : "R 도주 불가");
        graphics.text(font, Component.literal(controls), x, y + 68, SECONDARY, true);
        graphics.text(font, Component.literal("우클릭 선택 취소 · Esc 닫기"), x, y + 86, MUTED, true);
    }

    private BattleHudLayout.Layout currentLayout() {
        if (layout == null) layout = BattleHudLayout.calculate(width, height);
        return layout;
    }

    private static ClientBattleState.Unit findUnit(ClientBattleState.Snapshot snapshot, String id) {
        for (var unit : snapshot.units()) if (unit.id().equals(id)) return unit;
        return null;
    }

    private static String abbreviate(String value, int max) {
        return value.substring(0, Math.min(max, value.length()));
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { }
}
