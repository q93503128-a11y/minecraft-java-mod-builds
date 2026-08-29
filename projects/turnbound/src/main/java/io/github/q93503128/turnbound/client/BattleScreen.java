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

public final class BattleScreen extends Screen {
    private static final int DEEP = 0xD910131A;
    private static final int PANEL = 0xD9171C26;
    private static final int PANEL_LIGHT = 0xE0222A38;
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int HP = 0xFFE65A5A;
    private static final int HEAL = 0xFF62D39A;
    private static final int GAUGE = 0xFF6DC6FF;
    private static final int COOLDOWN = 0xFF7F8796;
    private static final int DANGER = 0xFFFF7A59;

    private final List<GlassButton> skillButtons = new ArrayList<>();
    private GlassButton autoButton;
    private GlassButton speedButton;
    private GlassButton fleeButton;
    private BattleScreenLayout.Layout layout;
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
        layout = BattleScreenLayout.calculate(width, height);

        for (int i = 0; i < BattleScreenLayout.SKILL_COUNT; i++) {
            final int index = i;
            BattleScreenLayout.Rect bounds = layout.skillButtons().get(i);
            GlassButton button = new GlassButton(
                    bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Component.empty(), GAUGE, ignored -> skill(index));
            skillButtons.add(addRenderableWidget(button));
        }

        BattleScreenLayout.Rect autoBounds = layout.autoButton();
        autoButton = addRenderableWidget(new GlassButton(
                autoBounds.x(), autoBounds.y(), autoBounds.width(), autoBounds.height(),
                Component.literal("AUTO"), HEAL, ignored -> toggleAuto()));

        BattleScreenLayout.Rect speedBounds = layout.speedButton();
        speedButton = addRenderableWidget(new GlassButton(
                speedBounds.x(), speedBounds.y(), speedBounds.width(), speedBounds.height(),
                Component.literal("×1"), GAUGE, ignored -> toggleSpeed()));

        BattleScreenLayout.Rect fleeBounds = layout.fleeButton();
        fleeButton = addRenderableWidget(new GlassButton(
                fleeBounds.x(), fleeBounds.y(), fleeBounds.width(), fleeBounds.height(),
                Component.literal("도주"), DANGER, ignored -> flee()));

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

        if (!selectedSkill.isBlank()
                && snapshot.skills().stream().noneMatch(skill -> skill.id().equals(selectedSkill))) {
            clearSelection(true);
        }

        boolean canAct = canChooseSkill(snapshot) && !settingsOpen;
        for (int i = 0; i < skillButtons.size(); i++) {
            GlassButton button = skillButtons.get(i);
            if (i < snapshot.skills().size() && canAct) {
                var skill = snapshot.skills().get(i);
                String prefix = selectedSkill.equals(skill.id()) ? "▶ " : "";
                String cooldown = skill.remaining() > 0 ? "  CD " + skill.remaining() : "";
                button.setMessage(Component.literal(prefix + (i + 1) + "  " + skill.name() + cooldown));
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
        } else if (selectedTarget < 0
                || selectedTarget >= snapshot.units().size()
                || !BattleTargeting.validTarget(selected.targetRule(), snapshot.units().get(selectedTarget), snapshot.actorId())) {
            selectedTarget = BattleTargeting.firstValid(snapshot.units(), selected.targetRule(), snapshot.actorId());
            syncSelectedTarget(snapshot);
        } else {
            syncSelectedTarget(snapshot);
        }

        autoButton.setMessage(Component.literal(snapshot.auto() ? "AUTO ON" : "AUTO"));
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
        return !snapshot.finished()
                && !snapshot.auto()
                && snapshot.actorId().startsWith("ally_");
    }

    private ClientBattleState.Skill selectedSkill(ClientBattleState.Snapshot snapshot) {
        if (selectedSkill.isBlank()) return null;
        return snapshot.skills().stream()
                .filter(skill -> skill.id().equals(selectedSkill))
                .findFirst()
                .orElse(null);
    }

    private void skill(int index) {
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        if (!canChooseSkill(snapshot) || index < 0 || index >= snapshot.skills().size()) return;

        var skill = snapshot.skills().get(index);
        if (skill.remaining() > 0) return;

        selectedSkill = skill.id();
        selectedTarget = -1;
        if (skill.targetRule().equals("SELF") || skill.targetRule().endsWith("_ALL")) {
            send("ACT|" + snapshot.actorId() + "|" + skill.id() + "|");
            clearSelection(true);
        } else {
            selectedTarget = BattleTargeting.firstValid(snapshot.units(), skill.targetRule(), snapshot.actorId());
            syncSelectedTarget(snapshot);
            refresh();
        }
    }

    private void target(int index) {
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null || index < 0 || index >= snapshot.units().size()) return;

        var unit = snapshot.units().get(index);
        if (!BattleTargeting.validTarget(selected.targetRule(), unit, snapshot.actorId())) return;

        send("ACT|" + snapshot.actorId() + "|" + selected.id() + "|" + unit.id());
        clearSelection(true);
    }

    private void cycleTarget(int direction) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null) return;
        selectedTarget = BattleTargeting.cycle(
                snapshot.units(), selected.targetRule(), snapshot.actorId(), selectedTarget, direction);
        syncSelectedTarget(snapshot);
    }

    private void confirmTarget() {
        if (selectedTarget >= 0) target(selectedTarget);
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
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        if (snapshot.finished()) return;
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
                if (!selectedSkill.isBlank()) {
                    confirmTarget();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_A -> {
                toggleAuto();
                return true;
            }
            case GLFW.GLFW_KEY_X -> {
                toggleSpeed();
                return true;
            }
            case GLFW.GLFW_KEY_R -> {
                flee();
                return true;
            }
            default -> {
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (settingsOpen) {
                settingsOpen = false;
                refresh();
            } else if (!selectedSkill.isBlank()) {
                clearSelection(true);
                refresh();
            }
            return true;
        }

        if (settingsOpen) return true;
        if (super.mouseClicked(event, doubleClick)) return true;

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && !selectedSkill.isBlank()) {
            int clicked = targetAt(event.x(), event.y());
            if (clicked >= 0) {
                target(clicked);
                return true;
            }
        }

        // Empty-world LMB is consumed so dragging can orbit the battle view instead of attacking the world.
        return event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!settingsOpen
                && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
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
        BattleScreenLayout.Layout current = currentLayout();
        if (current.actionPanel().contains(x, y)) return true;
        for (BattleScreenLayout.Rect rect : current.allyHud()) if (rect.contains(x, y)) return true;
        for (BattleScreenLayout.Rect rect : current.enemyHud()) if (rect.contains(x, y)) return true;
        return false;
    }

    private int targetAt(double x, double y) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null) return -1;

        int allySlot = 0;
        int enemySlot = 0;
        for (int i = 0; i < snapshot.units().size(); i++) {
            var unit = snapshot.units().get(i);
            boolean ally = "ALLY".equals(unit.side());
            int slot = ally ? allySlot++ : enemySlot++;
            List<BattleScreenLayout.Rect> slots = ally ? currentLayout().allyHud() : currentLayout().enemyHud();
            if (slot >= slots.size()) continue;
            if (slots.get(slot).contains(x, y)
                    && BattleTargeting.validTarget(selected.targetRule(), unit, snapshot.actorId())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally empty: alpha.5 keeps the 3D world visible instead of placing a full-screen veil over combat.
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        BattleScreenLayout.Layout current = currentLayout();
        var snapshot = ClientBattleState.snapshot();

        drawHudBackplates(graphics, current, snapshot);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawTimeline(graphics, current, snapshot);
        drawUnits(graphics, current, snapshot);
        drawActionHeader(graphics, current, snapshot);
        drawResult(graphics, snapshot);
        if (settingsOpen) drawSettings(graphics, current);
    }

    private void drawHudBackplates(
            GuiGraphicsExtractor graphics,
            BattleScreenLayout.Layout current,
            ClientBattleState.Snapshot snapshot
    ) {
        graphics.fill(current.timelinePanel().x(), current.timelinePanel().y(),
                current.timelinePanel().right(), current.timelinePanel().bottom(), DEEP);

        if (canChooseSkill(snapshot) && !settingsOpen) {
            graphics.fill(current.actionPanel().x(), current.actionPanel().y(),
                    current.actionPanel().right(), current.actionPanel().bottom(), PANEL);
            graphics.fill(current.actionPanel().x(), current.actionPanel().y(),
                    current.actionPanel().x() + 2, current.actionPanel().bottom(), GAUGE);
        }
    }

    private void drawTimeline(
            GuiGraphicsExtractor graphics,
            BattleScreenLayout.Layout current,
            ClientBattleState.Snapshot snapshot
    ) {
        BattleScreenLayout.Rect panel = current.timelinePanel();
        graphics.text(font, Component.literal("TURN"), panel.x() + 6, panel.y() + 5, SECONDARY, true);

        int tokenX = panel.x() + 42;
        int tokenY = panel.y() + 4;
        int tokenHeight = Math.max(12, panel.height() - 8);
        int tokenWidth = current.compact() ? 22 : 28;
        int gap = 3;
        int shown = 0;
        for (String id : snapshot.timeline()) {
            if (shown >= 8 || tokenX + tokenWidth > panel.right() - 4) break;
            var unit = findUnit(snapshot, id);
            if (unit == null) continue;
            int accent = "ALLY".equals(unit.side()) ? GAUGE : DANGER;
            int bg = id.equals(snapshot.actorId()) ? PANEL_LIGHT : DEEP;
            graphics.fill(tokenX, tokenY, tokenX + tokenWidth, tokenY + tokenHeight, bg);
            graphics.fill(tokenX, tokenY, tokenX + 2, tokenY + tokenHeight, accent);
            String text = abbreviate(unit.name(), current.compact() ? 1 : 2);
            graphics.text(font, Component.literal(text), tokenX + 5, tokenY + 3, TEXT, true);
            tokenX += tokenWidth + gap;
            shown++;
        }
    }

    private void drawUnits(
            GuiGraphicsExtractor graphics,
            BattleScreenLayout.Layout current,
            ClientBattleState.Snapshot snapshot
    ) {
        int allySlot = 0;
        int enemySlot = 0;
        for (int i = 0; i < snapshot.units().size(); i++) {
            var unit = snapshot.units().get(i);
            boolean ally = "ALLY".equals(unit.side());
            int slot = ally ? allySlot++ : enemySlot++;
            List<BattleScreenLayout.Rect> slots = ally ? current.allyHud() : current.enemyHud();
            if (slot >= slots.size()) continue;
            drawUnitCard(graphics, slots.get(slot), unit,
                    i == selectedTarget, unit.id().equals(snapshot.actorId()), current.compact());
        }
    }

    private void drawUnitCard(
            GuiGraphicsExtractor graphics,
            BattleScreenLayout.Rect rect,
            ClientBattleState.Unit unit,
            boolean selected,
            boolean actor,
            boolean compact
    ) {
        int background = unit.downed() ? 0xB010131A : PANEL;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), background);

        int accent = selected ? DANGER : actor ? GAUGE : 0xFF394354;
        graphics.fill(rect.x(), rect.y(), rect.x() + (selected || actor ? 3 : 1), rect.bottom(), accent);
        if (selected) graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 2, DANGER);

        int textX = rect.x() + 6;
        int textY = rect.y() + 3;
        String name = compact && unit.name().length() > 8 ? unit.name().substring(0, 8) : unit.name();
        String state = unit.downed() ? "  DOWN" : "";
        graphics.text(font, Component.literal((selected ? "▼ " : "") + name + state),
                textX, textY, unit.downed() ? COOLDOWN : TEXT, true);

        int barX = rect.x() + 6;
        int barRight = rect.right() - 6;
        int barY = rect.bottom() - 8;
        int barHeight = 4;
        graphics.fill(barX, barY, barRight, barY + barHeight, 0xB0000000);
        int barWidth = Math.max(0, barRight - barX);
        int hpWidth = unit.maxHp() <= 0 ? 0 : (int) Math.round(barWidth * (unit.hp() / (double) unit.maxHp()));
        hpWidth = Math.max(0, Math.min(barWidth, hpWidth));
        if (hpWidth > 0) graphics.fill(barX, barY, barX + hpWidth, barY + barHeight, HP);
        if (unit.barrier() > 0 && barWidth > 0) {
            int barrierWidth = Math.min(barWidth,
                    (int) Math.round(barWidth * (unit.barrier() / (double) Math.max(1, unit.maxHp()))));
            if (barrierWidth > 0) graphics.fill(barX, barY - 2, barX + barrierWidth, barY - 1, GAUGE);
        }

        if (!compact && rect.width() >= 110) {
            String hpText = unit.hp() + "/" + unit.maxHp();
            int hpX = Math.max(textX, rect.right() - 6 - font.width(hpText));
            graphics.text(font, Component.literal(hpText), hpX, textY, SECONDARY, true);
        }
    }

    private void drawActionHeader(
            GuiGraphicsExtractor graphics,
            BattleScreenLayout.Layout current,
            ClientBattleState.Snapshot snapshot
    ) {
        if (!canChooseSkill(snapshot) || settingsOpen) return;
        BattleScreenLayout.Rect panel = current.actionPanel();
        ClientBattleState.Unit actor = findUnit(snapshot, snapshot.actorId());
        String actorName = actor == null ? "행동 선택" : actor.name();
        graphics.text(font, Component.literal(actorName), panel.x() + 8, panel.y() + 5, TEXT, true);
        String sub = selectedSkill.isBlank() ? "행동을 선택하십시오" : "타겟을 선택하십시오 · RMB 취소";
        graphics.text(font, Component.literal(sub), panel.x() + 8, panel.y() + 15, SECONDARY, true);
    }

    private void drawResult(GuiGraphicsExtractor graphics, ClientBattleState.Snapshot snapshot) {
        if (!snapshot.finished()) return;
        String label = "ALLY_VICTORY".equals(snapshot.outcome()) ? "승리" : "패배";
        String line = label + "   ·   R / 복귀";
        int w = font.width(line) + 22;
        int x = Math.max(4, (width - w) / 2);
        int y = Math.max(4, height / 2 - 12);
        graphics.fill(x, y, Math.min(width, x + w), y + 24, DEEP);
        graphics.fill(x, y, x + 3, y + 24, "ALLY_VICTORY".equals(snapshot.outcome()) ? HEAL : DANGER);
        graphics.text(font, Component.literal(line), x + 11, y + 8, TEXT, true);
    }

    private void drawSettings(GuiGraphicsExtractor graphics, BattleScreenLayout.Layout current) {
        BattleScreenLayout.Rect panel = current.settingsPanel();
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xF010131A);
        graphics.fill(panel.x(), panel.y(), panel.x() + 3, panel.bottom(), GAUGE);
        int x = panel.x() + 14;
        int y = panel.y() + 12;
        graphics.text(font, Component.literal("전투 설정"), x, y, TEXT, true);
        graphics.text(font, Component.literal("Esc / RMB  닫기"), x, y + 18, SECONDARY, true);
        graphics.text(font, Component.literal("드래그  카메라 회전    휠  줌"), x, y + 36, SECONDARY, true);
        graphics.text(font, Component.literal("1~5  행동    Tab  타겟 순환"), x, y + 52, SECONDARY, true);
        graphics.text(font, Component.literal("A  자동    X  배속    R  도주"), x, y + 68, SECONDARY, true);
        graphics.text(font, Component.literal("전투 계산은 메뉴를 열어도 진행하지 않습니다."), x, y + 92, COOLDOWN, true);
    }

    private BattleScreenLayout.Layout currentLayout() {
        if (layout == null) layout = BattleScreenLayout.calculate(width, height);
        return layout;
    }

    private static ClientBattleState.Unit findUnit(ClientBattleState.Snapshot snapshot, String id) {
        for (var unit : snapshot.units()) if (unit.id().equals(id)) return unit;
        return null;
    }

    private static String abbreviate(String value, int max) {
        return value.substring(0, Math.min(max, value.length()));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        // Server snapshot owns the battle lifecycle. The screen never silently destroys a live session.
    }
}
