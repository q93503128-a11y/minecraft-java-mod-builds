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

/** World-first battle UI. Persistent information stays on the edges; actors remain the primary target surface. */
public final class BattleScreen extends Screen {
    private static final int DEEP = 0xD80A0D12;
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int HP = 0xFFE65A5A;
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
                Component.literal("AUTO"), HEAL, ignored -> toggleAuto()));
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
        autoButton.setMessage(Component.literal(controls.autoLabel()));
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
            case "p02_time_leap", "p03_guard" -> "ALLY_SINGLE_EXCEPT_SELF";
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
        clearSelection(true); send("AUTO");
    }
    private void toggleSpeed() {
        var snapshot = ClientBattleState.snapshot();
        if (!settingsOpen && !snapshot.finished() && snapshot.speedAllowed()) send("SPEED");
    }
    private void flee() {
        if (settingsOpen) return;
        var snapshot = ClientBattleState.snapshot();
        if (!snapshot.finished() && !snapshot.fleeAllowed()) return;
        clearSelection(true); send("FLEE");
    }
    private static void send(String command) { ClientPacketDistributor.sendToServer(new BattleCommandPayload(command)); }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            settingsOpen = !settingsOpen;
            if (settingsOpen) clearSelection(true);
            refresh(); return true;
        }
        if (settingsOpen) return true;
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_5) { skill(key - GLFW.GLFW_KEY_1); return true; }
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> { if (!selectedSkill.isBlank()) { cycleTarget(event.hasShiftDown() ? -1 : 1); return true; } }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { confirmAction(); return true; }
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
            refresh(); return true;
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
        if (!settingsOpen && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && !isOverInteractiveHud(event.x(), event.y())) {
            BattleCameraController.orbit(deltaX, deltaY); return true;
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

    private void drawTimeline(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        var panel = current.timeline();
        TurnboundFrameStyle.frame(graphics, panel.x(), panel.y(), panel.width(), panel.height(), GAUGE);
        int count = Math.min(8, snapshot.timeline().size());
        if (count == 0) return;
        int tokenWidth = Math.max(10, (panel.width() - 4) / count);
        int x = panel.x() + 3;
        for (int i = 0; i < count; i++) {
            ClientBattleState.Unit unit = findUnit(snapshot, snapshot.timeline().get(i));
            if (unit == null) continue;
            int color = "ALLY".equals(unit.side()) ? GAUGE : DANGER;
            if (unit.id().equals(snapshot.actorId())) graphics.fill(x, panel.y() + 2, x + tokenWidth - 1, panel.bottom() - 2, 0xE02A3442);
            graphics.fill(x, panel.bottom() - 3, x + tokenWidth - 1, panel.bottom() - 2, color);
            graphics.text(font, Component.literal(abbreviate(unit.name(), current.compact() ? 1 : 2)), x + 2, panel.y() + 5, TEXT, true);
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

    private void drawPartyLine(GuiGraphicsExtractor graphics, BattleHudLayout.Rect rect, ClientBattleState.Unit unit, boolean selected, boolean actor) {
        int accent = selected ? GAUGE : actor ? GOLD : 0xFF4B5668;
        TurnboundFrameStyle.frame(graphics, rect.x(), rect.y(), rect.width(), rect.height(), accent);
        String name = unit.downed() ? unit.name() + " DOWN" : unit.name();
        graphics.text(font, Component.literal(name), rect.x() + 6, rect.y() + 4, unit.downed() ? MUTED : TEXT, true);
        int barX = rect.x() + 5, barY = rect.bottom() - 5, barW = rect.width() - 10;
        graphics.fill(barX, barY, barX + barW, barY + 2, 0xFF080A0E);
        int hpW = unit.maxHp() <= 0 ? 0 : (int)Math.round(barW * unit.hp() / (double)unit.maxHp());
        if (hpW > 0) graphics.fill(barX, barY, barX + Math.min(barW, hpW), barY + 2, HP);
    }

    /** Reference-game hierarchy: enemy HP and selection live next to the actual 3D actor, not in a second HUD wall. */
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
            int barW = enemy ? 46 : 40;
            int x = cx - barW / 2;
            if (selected) {
                int color = enemy ? DANGER : GAUGE;
                String arrow = "▼";
                graphics.text(font, Component.literal(arrow), cx - font.width(arrow) / 2, y - 15, color, true);
            } else if (actor) {
                graphics.text(font, Component.literal("◆"), cx - 4, y - 13, GOLD, true);
            }
            if (enemy) {
                String name = abbreviate(unit.name(), 9);
                graphics.text(font, Component.literal(name), cx - font.width(name) / 2, y - 3, TEXT, true);
                graphics.fill(x, y + 8, x + barW, y + 11, 0xE0000000);
                int hpW = unit.maxHp() <= 0 ? 0 : (int)Math.round(barW * unit.hp() / (double)unit.maxHp());
                if (hpW > 0) graphics.fill(x, y + 8, x + Math.min(barW, hpW), y + 11, HP);
            }
        }
    }

    private void drawActionHeader(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        if (!canChooseSkill(snapshot) || settingsOpen) return;
        var rect = current.actionHeader();
        TurnboundFrameStyle.frame(graphics, rect.x(), rect.y(), rect.width(), rect.height(), GAUGE);
        ClientBattleState.Unit actor = findUnit(snapshot, snapshot.actorId());
        String actorName = actor == null ? "행동" : actor.name();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        String rule = clientTargetRule(selected);
        String hint = selected == null ? "스킬 선택" : BattleActionRules.needsSingleTarget(rule) && selectedTarget < 0
                ? "대상 클릭 · 스킬 재클릭=자동" : "한 번 더 클릭 = 사용";
        graphics.text(font, Component.literal(actorName), rect.x() + 6, rect.y() + 4, TEXT, true);
        int hx = rect.right() - 6 - font.width(hint);
        if (hx > rect.x() + 30) graphics.text(font, Component.literal(hint), hx, rect.y() + 4, SECONDARY, true);
    }

    private void drawSkillTooltip(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot, int mouseX, int mouseY) {
        if (!canChooseSkill(snapshot) || settingsOpen) return;
        int hovered = -1;
        for (int i = 0; i < current.skillButtons().size() && i < snapshot.skills().size(); i++) if (current.skillButtons().get(i).contains(mouseX, mouseY)) { hovered = i; break; }
        if (hovered < 0) return;
        ClientBattleState.Skill skill = snapshot.skills().get(hovered);
        var area = current.tooltipArea();
        List<String> lines = new ArrayList<>();
        for (String source : BattleSkillTooltip.lines(skill)) lines.addAll(wrap(source, Math.max(48, area.width() - 14)));
        int h = Math.min(area.height(), 10 + lines.size() * 11);
        TurnboundFrameStyle.frame(graphics, area.x(), area.y(), area.width(), h, GAUGE);
        int y = area.y() + 6;
        for (int i = 0; i < lines.size() && y + 9 <= area.y() + h; i++) {
            graphics.text(font, Component.literal(lines.get(i)), area.x() + 7, y, i == 0 ? TEXT : SECONDARY, true); y += 11;
        }
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (current.length() > 0 && font.width(current.toString() + c) > maxWidth) { lines.add(current.toString()); current.setLength(0); }
            current.append(c);
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private void drawResult(GuiGraphicsExtractor graphics, ClientBattleState.Snapshot snapshot) {
        if (!snapshot.finished()) return;
        String line = ("ALLY_VICTORY".equals(snapshot.outcome()) ? "승리" : "패배") + "  ·  R 복귀";
        int w = font.width(line) + 20, x = (width - w) / 2, y = height / 2 - 10;
        TurnboundFrameStyle.frame(graphics, x, y, w, 22, "ALLY_VICTORY".equals(snapshot.outcome()) ? HEAL : DANGER);
        graphics.text(font, Component.literal(line), x + 10, y + 7, TEXT, true);
    }

    private void drawSettings(GuiGraphicsExtractor graphics, BattleHudLayout.Layout current, ClientBattleState.Snapshot snapshot) {
        var panel = current.settingsPanel();
        TurnboundFrameStyle.frame(graphics, panel.x(), panel.y(), panel.width(), panel.height(), GAUGE);
        int x = panel.x() + 12, y = panel.y() + 10;
        graphics.text(font, Component.literal("전투 설정"), x, y, TEXT, true);
        graphics.text(font, Component.literal("드래그 회전 · 휠 줌"), x, y + 18, SECONDARY, true);
        graphics.text(font, Component.literal("캐릭터/Tab 대상 · 같은 대상 2번 = 사용"), x, y + 34, SECONDARY, true);
        graphics.text(font, Component.literal("같은 스킬 2번 = 자동 대상 후 사용 · Enter 확정"), x, y + 50, SECONDARY, true);
        String controls = (snapshot.autoAllowed() ? "A 자동" : "A 자동 잠금") + " · "
                + (snapshot.speedAllowed() ? "X 배속" : "X 배속 잠금") + " · "
                + (snapshot.fleeAllowed() ? "R 도주" : "R 도주 불가");
        graphics.text(font, Component.literal(controls), x, y + 68, SECONDARY, true);
        graphics.text(font, Component.literal("RMB 선택 취소 · Esc 닫기"), x, y + 86, MUTED, true);
    }

    private BattleHudLayout.Layout currentLayout() { if (layout == null) layout = BattleHudLayout.calculate(width, height); return layout; }
    private static ClientBattleState.Unit findUnit(ClientBattleState.Snapshot snapshot, String id) { for (var unit : snapshot.units()) if (unit.id().equals(id)) return unit; return null; }
    private static String abbreviate(String value, int max) { return value.substring(0, Math.min(max, value.length())); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { }
}
