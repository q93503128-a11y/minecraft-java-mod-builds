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

    private final List<BattleHudButton> skillButtons = new ArrayList<>();
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
    private String lastSkillClick = "";
    private long lastSkillClickAt;
    private int lastTargetClick = -1;
    private long lastTargetClickAt;

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
            setWorldFocus("");
        } else {
            String rule = clientTargetRule(selected);
            if (BattleActionRules.needsSingleTarget(rule)) {
                if (selectedTarget >= 0 && (selectedTarget >= snapshot.units().size()
                        || !BattleTargeting.validTarget(rule, snapshot.units().get(selectedTarget), snapshot.actorId()))) selectedTarget = -1;
                syncSelectedTarget(snapshot);
            } else if ("SELF".equals(rule)) {
                selectedTarget = BattleActionRules.defaultTarget(snapshot.units(), rule, snapshot.actorId());
                syncSelectedTarget(snapshot);
            } else {
                selectedTarget = -1;
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
        return !snapshot.finished() && !snapshot.auto() && snapshot.actorId().startsWith("ally_");
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
        } else if (BattleActionRules.needsSingleTarget(rule)) {
            if (selectedTarget >= 0 && (selectedTarget >= snapshot.units().size()
                    || !BattleTargeting.validTarget(rule, snapshot.units().get(selectedTarget), snapshot.actorId()))) selectedTarget = -1;
            if (repeated && selectedTarget < 0) {
                selectedTarget = BattleActionRules.defaultTarget(snapshot.units(), rule, snapshot.actorId());
            }
        } else {
            selectedTarget = -1;
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
        if (selected == null || !BattleActionRules.needsSingleTarget(rule)) return;
        if (index < 0 || index >= snapshot.units().size()) return;
        if (!BattleTargeting.validTarget(rule, snapshot.units().get(index), snapshot.actorId())) return;
        long now = System.currentTimeMillis();
        boolean repeated = selectedTarget == index && (platformDoubleClick || (lastTargetClick == index && now - lastTargetClickAt <= DOUBLE_COMMIT_MS));
        selectedTarget = index;
        lastTargetClick = index;
        lastTargetClickAt = now;
        syncSelectedTarget(snapshot);
        refresh();
        if (repeated) confirmAction();
    }

    private void cycleTarget(int direction) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        String rule = clientTargetRule(selected);
        if (selected == null || !BattleActionRules.needsSingleTarget(rule)) return;
        selectedTarget = BattleTargeting.cycle(snapshot.units(), rule, snapshot.actorId(), selectedTarget, direction);
        syncSelectedTarget(snapshot);
        refresh();
    }

    private void confirmAction() {
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null || !canChooseSkill(snapshot)) return;
        String rule = clientTargetRule(selected);
        String targetId = BattleActionRules.confirmedTarget(snapshot.units(), rule, snapshot.actorId(), selectedTarget);
        if (targetId == null) return;
        send("ACT|" + snapshot.actorId() + "|" + selected.id() + "|" + targetId);
        clearSelection(true);
    }

    private void clearSelection(boolean clearWorldFocus) {
        selectedSkill = "";
        selectedTarget = -1;
        lastTargetClick = -1;
        lastTargetClickAt = 0L;
        if (clearWorldFocus) setWorldFocus("");
    }

    private void syncSelectedTarget(ClientBattleState.Snapshot snapshot) {
        if (selectedTarget >= 0 && selectedTarget < snapshot.units().size()) setWorldFocus(snapshot.units().get(selectedTarget).id());
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
        // A selected skill turns left-click into a pure targeting gesture; no accidental camera drift while aiming.
        if (!settingsOpen && selectedSkill.isBlank() && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && !isOverInteractiveHud(event.x(), event.y())) {
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
        for (var rect : current.allyBars()) if (rect.contains(x, y)) return true;
        return current.autoButton().contains(x, y) || current.speedButton().contains(x, y) || current.fleeButton().contains(x, y);
    }

    private int worldTargetAt(double x, double y) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        String rule = clientTargetRule(selected);
        if (selected == null || !BattleActionRules.needsSingleTarget(rule)) return -1;
        return BattleLiveProjection.pick(snapshot.units(), rule, snapshot.actorId(), width, height, x, y);
    }

    private int hudTargetAt(double x, double y) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        String rule = clientTargetRule(selected);
        if (selected == null || !BattleActionRules.needsSingleTarget(rule)) return -1;
        int allySlot = 0;
        for (int i = 0; i < snapshot.units().size(); i++) {
            var unit = snapshot.units().get(i);
            if (!"ALLY".equals(unit.side())) continue;
            int slot = allySlot++;
            if (slot < currentLayout().allyBars().size() && currentLayout().allyBars().get(slot).contains(x, y)
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

    /** Small queue tokens only; the top of the screen must not become a second HUD wall. */
    private void drawTimeline(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        var panel = current.timeline();
        int count = Math.min(7, snapshot.timeline().size());
        if (count == 0) return;
        graphics.fill(panel.x(), panel.y() + 2, panel.right(), panel.bottom() - 1, 0x50080A0E);
        int tokenWidth = Math.max(10, panel.width() / count);
        int x = panel.x();
        for (int i = 0; i < count; i++) {
            ClientBattleState.Unit unit = findUnit(snapshot, snapshot.timeline().get(i));
            if (unit == null) { x += tokenWidth; continue; }
            int color = "ALLY".equals(unit.side()) ? GAUGE : DANGER;
            if (unit.id().equals(snapshot.actorId())) graphics.fill(x + 1, panel.y() + 2, x + tokenWidth - 1, panel.bottom() - 1, 0xA02A3442);
            graphics.fill(x + 1, panel.bottom() - 3, x + tokenWidth - 1, panel.bottom() - 1, color);
            String name = abbreviate(unit.name(), current.compact() ? 1 : 2);
            graphics.text(font, Component.literal(name), x + Math.max(2, (tokenWidth - font.width(name)) / 2), panel.y() + 4, TEXT, true);
            x += tokenWidth;
        }
    }

    private void drawParty(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        int slot = 0;
        for (int i = 0; i < snapshot.units().size(); i++) {
            var unit = snapshot.units().get(i);
            if (!"ALLY".equals(unit.side()) || slot >= current.allyBars().size()) continue;
            drawPartyLine(graphics, current.allyBars().get(slot++), unit, i == selectedTarget, unit.id().equals(snapshot.actorId()));
        }
    }

    /** Reference-style party status: name/value + thin HP bar, with almost no chrome. */
    private void drawPartyLine(GuiGraphicsExtractor graphics, BattleHudLayout.Rect rect, ClientBattleState.Unit unit, boolean selected, boolean actor) {
        int accent = selected ? GAUGE : actor ? GOLD : 0x884B5668;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0x42080A0E);
        graphics.fill(rect.x(), rect.y(), rect.x() + 2, rect.bottom(), accent);

        String hpText = unit.downed() ? "DOWN" : unit.hp() + "/" + unit.maxHp();
        int hpTextW = font.width(hpText);
        int nameMax = Math.max(12, rect.width() - hpTextW - 14);
        String name = UiTextLayout.fit(unit.name(), nameMax);
        graphics.text(font, Component.literal(name), rect.x() + 5, rect.y() + 2, unit.downed() ? MUTED : TEXT, true);
        graphics.text(font, Component.literal(hpText), rect.right() - hpTextW - 4, rect.y() + 2,
                unit.downed() ? MUTED : SECONDARY, false);

        int barX = rect.x() + 4;
        int barY = rect.bottom() - 4;
        int barW = rect.width() - 8;
        graphics.fill(barX, barY, barX + barW, barY + 2, 0xD0080A0E);
        int hpW = unit.maxHp() <= 0 ? 0 : (int)Math.round(barW * Math.max(0, unit.hp()) / (double)unit.maxHp());
        if (hpW > 0) graphics.fill(barX, barY, barX + Math.min(barW, hpW), barY + 2, HP);
    }

    /** Enemy name/HP and target selection stay attached to the actual 3D actor. */
    private void drawWorldStatus(GuiGraphicsExtractor graphics, ClientBattleState.Snapshot snapshot) {
        for (int i = 0; i < snapshot.units().size(); i++) {
            var unit = snapshot.units().get(i);
            var point = BattleLiveProjection.project(unit.x(), unit.y() + 2.05, unit.z(), width, height);
            if (point == null) continue;
            boolean enemy = "ENEMY".equals(unit.side());
            boolean selected = i == selectedTarget;
            boolean actor = unit.id().equals(snapshot.actorId());
            if (!enemy && !selected && !actor) continue;

            int cx = (int)Math.round(point.x());
            int y = (int)Math.round(point.y());
            if (selected) drawTargetArrow(graphics, cx, y - 11, enemy ? DANGER : GAUGE);
            else if (actor) graphics.text(font, Component.literal("◆"), cx - 4, y - 17, GOLD, true);

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
        if (!canChooseSkill(snapshot) || settingsOpen) return;
        var rect = current.actionHeader();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        String rule = clientTargetRule(selected);
        String hint = selected == null ? "스킬을 선택하세요"
                : BattleActionRules.needsSingleTarget(rule) && selectedTarget < 0 ? "대상을 선택하세요"
                : "한 번 더 클릭해 사용";
        String fitted = UiTextLayout.fit(hint, Math.max(12, rect.width() - 4));
        int x = rect.x() + Math.max(2, (rect.width() - font.width(fitted)) / 2);
        graphics.text(font, Component.literal(fitted), x, rect.y() + 5, selected == null ? SECONDARY : TEXT, true);
    }

    private void drawSkillTooltip(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot, int mouseX, int mouseY) {
        if (!canChooseSkill(snapshot) || settingsOpen) return;
        int hovered = -1;
        for (int i = 0; i < current.skillButtons().size() && i < snapshot.skills().size(); i++) {
            if (current.skillButtons().get(i).contains(mouseX, mouseY)) { hovered = i; break; }
        }
        if (hovered < 0) return;
        ClientBattleState.Skill skill = snapshot.skills().get(hovered);
        var area = current.tooltipArea();
        List<String> lines = new ArrayList<>();
        for (String source : BattleSkillTooltip.lines(skill)) lines.addAll(wrap(source, Math.max(48, area.width() - 14)));
        int h = Math.min(area.height(), 10 + lines.size() * 11);
        graphics.fill(area.x(), area.y(), area.right(), area.y() + h, DEEP);
        TurnboundFrameStyle.frame(graphics, area.x(), area.y(), area.width(), h, GAUGE);
        int y = area.y() + 6;
        for (int i = 0; i < lines.size() && y + 9 <= area.y() + h; i++) {
            graphics.text(font, Component.literal(lines.get(i)), area.x() + 7, y, i == 0 ? TEXT : SECONDARY, true);
            y += 11;
        }
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
        graphics.text(font, Component.literal("빈 공간 드래그 회전 · 휠 줌 · C 카메라 초기화"), x, y + 18, SECONDARY, true);
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
