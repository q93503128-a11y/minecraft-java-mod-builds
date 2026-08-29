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
import java.util.List;
import java.util.Objects;

/** World-first battle screen. It owns only edge HUD/input; the 3D battlefield remains the main visual. */
public final class BattleScreen extends Screen {
    private static final int DEEP = 0xB810131A;
    private static final int PANEL = 0xC9171C26;
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int HP = 0xFFE65A5A;
    private static final int HEAL = 0xFF62D39A;
    private static final int GAUGE = 0xFF6DC6FF;
    private static final int MUTED = 0xFF707987;
    private static final int DANGER = 0xFFFF7A59;

    private final List<BattleHudButton> skillButtons = new ArrayList<>();
    private BattleHudButton confirmButton;
    private BattleHudButton autoButton;
    private BattleHudButton speedButton;
    private BattleHudButton fleeButton;
    private BattleHudLayout.Layout layout;
    private String selectedSkill = "";
    private String selectedActor = "";
    private String focusedTargetId = "";
    private int selectedTarget = -1;
    private boolean settingsOpen;
    private long seen = -1;

    public BattleScreen() {
        super(Component.literal("TURNBOUND Battle"));
    }

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

        var confirm = layout.confirmButton();
        confirmButton = addRenderableWidget(new BattleHudButton(
                confirm.x(), confirm.y(), confirm.width(), confirm.height(), Component.literal("사용 확정"), HEAL,
                ignored -> confirmAction()));

        var auto = layout.autoButton();
        autoButton = addRenderableWidget(new BattleHudButton(
                auto.x(), auto.y(), auto.width(), auto.height(), Component.literal("AUTO"), HEAL,
                ignored -> toggleAuto()));

        var speed = layout.speedButton();
        speedButton = addRenderableWidget(new BattleHudButton(
                speed.x(), speed.y(), speed.width(), speed.height(), Component.literal("×1"), GAUGE,
                ignored -> toggleSpeed()));

        var flee = layout.fleeButton();
        fleeButton = addRenderableWidget(new BattleHudButton(
                flee.x(), flee.y(), flee.width(), flee.height(), Component.literal("도주"), DANGER,
                ignored -> flee()));
        refresh();
    }

    @Override
    public void tick() {
        super.tick();
        if (seen != ClientBattleState.revision()) refresh();
    }

    private void refresh() {
        seen = ClientBattleState.revision();
        var snapshot = ClientBattleState.snapshot();

        if (!Objects.equals(selectedActor, snapshot.actorId())) {
            selectedActor = snapshot.actorId();
            clearSelection(true);
        }
        if (!selectedSkill.isBlank() && snapshot.skills().stream().noneMatch(skill -> skill.id().equals(selectedSkill))) {
            clearSelection(true);
        }

        boolean canAct = canChooseSkill(snapshot) && !settingsOpen;
        for (int i = 0; i < skillButtons.size(); i++) {
            BattleHudButton button = skillButtons.get(i);
            if (i < snapshot.skills().size() && canAct) {
                var skill = snapshot.skills().get(i);
                String prefix = selectedSkill.equals(skill.id()) ? "▶ " : "";
                String cooldown = skill.remaining() > 0 ? "  " + skill.remaining() : "";
                button.setMessage(Component.literal(prefix + (i + 1) + " " + skill.name() + cooldown));
                button.active = skill.remaining() == 0;
                button.visible = true;
            } else {
                button.visible = false;
            }
        }

        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null) {
            selectedTarget = -1;
            setWorldFocus("");
        } else if (BattleActionRules.needsSingleTarget(selected.targetRule())) {
            if (selectedTarget < 0 || selectedTarget >= snapshot.units().size()
                    || !BattleTargeting.validTarget(selected.targetRule(), snapshot.units().get(selectedTarget), snapshot.actorId())) {
                selectedTarget = BattleActionRules.defaultTarget(snapshot.units(), selected.targetRule(), snapshot.actorId());
            }
            syncSelectedTarget(snapshot);
        } else if ("SELF".equals(selected.targetRule())) {
            selectedTarget = BattleActionRules.defaultTarget(snapshot.units(), selected.targetRule(), snapshot.actorId());
            syncSelectedTarget(snapshot);
        } else {
            selectedTarget = -1;
            setWorldFocus("");
        }

        String confirmed = selected == null ? null : BattleActionRules.confirmedTarget(
                snapshot.units(), selected.targetRule(), snapshot.actorId(), selectedTarget);
        confirmButton.visible = canAct && selected != null;
        confirmButton.active = confirmed != null;
        confirmButton.setMessage(Component.literal(selected == null ? "사용 확정" : "사용 확정"));

        autoButton.setMessage(Component.literal(snapshot.auto() ? "AUTO✓" : "AUTO"));
        speedButton.setMessage(Component.literal("×" + snapshot.speed()));
        fleeButton.setMessage(Component.literal(snapshot.finished() ? "복귀" : "도주"));
        autoButton.visible = !settingsOpen && !snapshot.finished();
        speedButton.visible = !settingsOpen && !snapshot.finished();
        fleeButton.visible = !settingsOpen;
        autoButton.active = !snapshot.finished();
        speedButton.active = !snapshot.finished();
        fleeButton.active = true;
    }

    private static boolean canChooseSkill(ClientBattleState.Snapshot snapshot) {
        return !snapshot.finished() && !snapshot.auto() && snapshot.actorId().startsWith("ally_");
    }

    private ClientBattleState.Skill selectedSkill(ClientBattleState.Snapshot snapshot) {
        if (selectedSkill.isBlank()) return null;
        return snapshot.skills().stream().filter(skill -> skill.id().equals(selectedSkill)).findFirst().orElse(null);
    }

    /** Selecting a skill only enters a pending state. No ACT packet is sent here. */
    private void skill(int index) {
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        if (!canChooseSkill(snapshot) || index < 0 || index >= snapshot.skills().size()) return;
        var skill = snapshot.skills().get(index);
        if (skill.remaining() > 0) return;
        selectedSkill = skill.id();
        selectedTarget = BattleActionRules.defaultTarget(snapshot.units(), skill.targetRule(), snapshot.actorId());
        syncSelectedTarget(snapshot);
        refresh();
    }

    private void selectTarget(int index) {
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null || !BattleActionRules.needsSingleTarget(selected.targetRule())) return;
        if (index < 0 || index >= snapshot.units().size()) return;
        if (!BattleTargeting.validTarget(selected.targetRule(), snapshot.units().get(index), snapshot.actorId())) return;
        selectedTarget = index;
        syncSelectedTarget(snapshot);
        refresh();
    }

    private void cycleTarget(int direction) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null || !BattleActionRules.needsSingleTarget(selected.targetRule())) return;
        selectedTarget = BattleTargeting.cycle(
                snapshot.units(), selected.targetRule(), snapshot.actorId(), selectedTarget, direction);
        syncSelectedTarget(snapshot);
        refresh();
    }

    private void confirmAction() {
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null || !canChooseSkill(snapshot)) return;
        String targetId = BattleActionRules.confirmedTarget(
                snapshot.units(), selected.targetRule(), snapshot.actorId(), selectedTarget);
        if (targetId == null) return;
        send("ACT|" + snapshot.actorId() + "|" + selected.id() + "|" + targetId);
        clearSelection(true);
    }

    private void clearSelection(boolean clearWorldFocus) {
        selectedSkill = "";
        selectedTarget = -1;
        if (clearWorldFocus) setWorldFocus("");
    }

    private void syncSelectedTarget(ClientBattleState.Snapshot snapshot) {
        if (selectedTarget >= 0 && selectedTarget < snapshot.units().size()) {
            setWorldFocus(snapshot.units().get(selectedTarget).id());
        } else {
            setWorldFocus("");
        }
    }

    private void setWorldFocus(String targetId) {
        String normalized = targetId == null ? "" : targetId;
        if (Objects.equals(focusedTargetId, normalized)) return;
        focusedTargetId = normalized;
        send("FOCUS|" + normalized);
    }

    private void toggleAuto() {
        if (settingsOpen || ClientBattleState.snapshot().finished()) return;
        clearSelection(true);
        send("AUTO");
    }

    private void toggleSpeed() {
        if (!settingsOpen && !ClientBattleState.snapshot().finished()) send("SPEED");
    }

    private void flee() {
        if (settingsOpen) return;
        clearSelection(true);
        send("FLEE");
    }

    private static void send(String command) {
        ClientPacketDistributor.sendToServer(new BattleCommandPayload(command));
    }

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

        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_5) {
            skill(key - GLFW.GLFW_KEY_1);
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                if (!selectedSkill.isBlank()) {
                    cycleTarget(event.hasShiftDown() ? -1 : 1);
                    return true;
                }
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                confirmAction();
                return true;
            }
            case GLFW.GLFW_KEY_A -> { toggleAuto(); return true; }
            case GLFW.GLFW_KEY_X -> { toggleSpeed(); return true; }
            case GLFW.GLFW_KEY_R -> { flee(); return true; }
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
            if (clicked >= 0) {
                selectTarget(clicked);
                return true;
            }
        }
        return event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!settingsOpen && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && !isOverInteractiveHud(event.x(), event.y())) {
            BattleCameraController.orbit(deltaX, deltaY);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!settingsOpen && scrollY != 0.0D) {
            BattleCameraController.zoom(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isOverInteractiveHud(double x, double y) {
        var current = currentLayout();
        if (current.actionHeader().contains(x, y) || current.confirmButton().contains(x, y)) return true;
        for (var rect : current.skillButtons()) if (rect.contains(x, y)) return true;
        for (var rect : current.allyBars()) if (rect.contains(x, y)) return true;
        for (var rect : current.enemyBars()) if (rect.contains(x, y)) return true;
        return current.autoButton().contains(x, y) || current.speedButton().contains(x, y) || current.fleeButton().contains(x, y);
    }

    private int worldTargetAt(double x, double y) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null || !BattleActionRules.needsSingleTarget(selected.targetRule())) return -1;
        return BattleTargetProjection.pick(snapshot.units(), selected.targetRule(), snapshot.actorId(),
                snapshot.arenaX(), snapshot.arenaY(), snapshot.arenaZ(), BattleCameraController.view(),
                width, height, x, y);
    }

    private int hudTargetAt(double x, double y) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null || !BattleActionRules.needsSingleTarget(selected.targetRule())) return -1;
        int allySlot = 0;
        int enemySlot = 0;
        for (int i = 0; i < snapshot.units().size(); i++) {
            var unit = snapshot.units().get(i);
            boolean ally = "ALLY".equals(unit.side());
            int slot = ally ? allySlot++ : enemySlot++;
            List<BattleHudLayout.Rect> slots = ally ? currentLayout().allyBars() : currentLayout().enemyBars();
            if (slot < slots.size() && slots.get(slot).contains(x, y)
                    && BattleTargeting.validTarget(selected.targetRule(), unit, snapshot.actorId())) return i;
        }
        return -1;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // No full-screen veil: the battle world is the background.
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        var snapshot = ClientBattleState.snapshot();
        var current = currentLayout();
        drawTimeline(graphics, current, snapshot);
        drawUnits(graphics, current, snapshot);
        drawActionHeader(graphics, current, snapshot);
        drawResult(graphics, snapshot);
        if (settingsOpen) drawSettings(graphics, current);
    }

    private void drawTimeline(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        var panel = current.timeline();
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), DEEP);
        int count = Math.min(8, snapshot.timeline().size());
        if (count == 0) return;
        int gap = 2;
        int tokenWidth = Math.max(12, (panel.width() - gap * (count - 1)) / count);
        int x = panel.x();
        for (int i = 0; i < count; i++) {
            String id = snapshot.timeline().get(i);
            ClientBattleState.Unit unit = findUnit(snapshot, id);
            if (unit == null) continue;
            int accent = "ALLY".equals(unit.side()) ? GAUGE : DANGER;
            if (id.equals(snapshot.actorId())) graphics.fill(x, panel.y(), x + tokenWidth, panel.bottom(), 0xE0222A38);
            graphics.fill(x, panel.y(), x + 2, panel.bottom(), accent);
            String label = abbreviate(unit.name(), current.compact() ? 1 : 2);
            graphics.text(font, Component.literal(label), x + 4, panel.y() + 4, TEXT, true);
            x += tokenWidth + gap;
        }
    }

    private void drawUnits(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        int allySlot = 0;
        int enemySlot = 0;
        for (int i = 0; i < snapshot.units().size(); i++) {
            var unit = snapshot.units().get(i);
            boolean ally = "ALLY".equals(unit.side());
            int slot = ally ? allySlot++ : enemySlot++;
            List<BattleHudLayout.Rect> slots = ally ? current.allyBars() : current.enemyBars();
            if (slot >= slots.size()) continue;
            drawUnitLine(graphics, slots.get(slot), unit, i == selectedTarget, unit.id().equals(snapshot.actorId()), current.compact());
        }
    }

    private void drawUnitLine(GuiGraphicsExtractor graphics, BattleHudLayout.Rect rect, ClientBattleState.Unit unit,
                              boolean selected, boolean actor, boolean compact) {
        String name = unit.downed() ? unit.name() + " DOWN" : unit.name();
        if (compact && name.length() > 9) name = name.substring(0, 9);
        if (selected) name = "▼ " + name;
        int textColor = unit.downed() ? MUTED : TEXT;
        graphics.text(font, Component.literal(name), rect.x(), rect.y(), textColor, true);

        int barY = rect.bottom() - 5;
        graphics.fill(rect.x(), barY, rect.right(), rect.bottom(), 0xB0000000);
        int width = rect.width();
        int hpWidth = unit.maxHp() <= 0 ? 0 : (int) Math.round(width * unit.hp() / (double) unit.maxHp());
        hpWidth = Math.max(0, Math.min(width, hpWidth));
        if (hpWidth > 0) graphics.fill(rect.x(), barY, rect.x() + hpWidth, rect.bottom(), HP);
        if (unit.barrier() > 0) {
            int barrierWidth = Math.min(width, (int) Math.round(width * unit.barrier() / (double) Math.max(1, unit.maxHp())));
            if (barrierWidth > 0) graphics.fill(rect.x(), barY - 2, rect.x() + barrierWidth, barY - 1, GAUGE);
        }
        if (selected) graphics.fill(rect.x(), rect.y() - 1, rect.right(), rect.y(), DANGER);
        else if (actor) graphics.fill(rect.x(), rect.y() - 1, rect.right(), rect.y(), GAUGE);
    }

    private void drawActionHeader(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        if (!canChooseSkill(snapshot) || settingsOpen) return;
        var rect = current.actionHeader();
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), PANEL);
        ClientBattleState.Unit actor = findUnit(snapshot, snapshot.actorId());
        String actorName = actor == null ? "행동" : actor.name();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        graphics.text(font, Component.literal(actorName), rect.x() + 5, rect.y() + 3, TEXT, true);
        String hint;
        if (selected == null) hint = "행동 선택";
        else if (BattleActionRules.needsSingleTarget(selected.targetRule())) hint = "대상 클릭 → 확정";
        else hint = "사용 확정";
        graphics.text(font, Component.literal(hint), rect.x() + 5, rect.y() + 14, SECONDARY, true);
    }

    private void drawResult(GuiGraphicsExtractor graphics, ClientBattleState.Snapshot snapshot) {
        if (!snapshot.finished()) return;
        String label = "ALLY_VICTORY".equals(snapshot.outcome()) ? "승리" : "패배";
        String line = label + "  ·  R 복귀";
        int w = font.width(line) + 18;
        int x = Math.max(4, (width - w) / 2);
        int y = Math.max(4, height / 2 - 10);
        graphics.fill(x, y, x + w, y + 22, DEEP);
        graphics.fill(x, y, x + 3, y + 22, "ALLY_VICTORY".equals(snapshot.outcome()) ? HEAL : DANGER);
        graphics.text(font, Component.literal(line), x + 9, y + 7, TEXT, true);
    }

    private void drawSettings(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current) {
        var panel = current.settingsPanel();
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xF010131A);
        graphics.fill(panel.x(), panel.y(), panel.x() + 3, panel.bottom(), GAUGE);
        int x = panel.x() + 12;
        int y = panel.y() + 10;
        graphics.text(font, Component.literal("전투 설정"), x, y, TEXT, true);
        graphics.text(font, Component.literal("LMB 드래그  회전   휠  줌"), x, y + 18, SECONDARY, true);
        graphics.text(font, Component.literal("캐릭터 클릭 / Tab  대상 선택"), x, y + 34, SECONDARY, true);
        graphics.text(font, Component.literal("Enter  사용 확정   RMB  취소"), x, y + 50, SECONDARY, true);
        graphics.text(font, Component.literal("A 자동   X 배속   R 도주"), x, y + 66, SECONDARY, true);
        graphics.text(font, Component.literal("Esc / RMB  닫기"), x, y + 86, MUTED, true);
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