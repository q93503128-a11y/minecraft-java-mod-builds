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
 * alpha.6 clean-room battle HUD.
 * Legacy full-width alpha.4/5 panels and target-card grids are intentionally not reused.
 */
public final class BattleScreen extends Screen {
    private static final int GLASS = 0xA8171C26;
    private static final int GLASS_STRONG = 0xD9171C26;
    private static final int TEXT = 0xFFF4F0E6;
    private static final int MUTED = 0xFFAEB7C6;
    private static final int HP = 0xFFE65A5A;
    private static final int HEAL = 0xFF62D39A;
    private static final int GAUGE = 0xFF6DC6FF;
    private static final int DANGER = 0xFFFF7A59;
    private static final int DISABLED = 0xFF7F8796;

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

    public BattleScreen() {
        super(Component.literal("TURNBOUND"));
    }

    @Override
    protected void init() {
        super.init();
        layout = BattleHudLayout.calculate(width, height);
        skillButtons.clear();

        for (int i = 0; i < BattleHudLayout.SKILL_COUNT; i++) {
            int index = i;
            BattleHudLayout.Rect r = layout.skillButtons().get(i);
            BattleHudButton button = new BattleHudButton(r.x(), r.y(), r.width(), r.height(),
                    Component.empty(), GAUGE, ignored -> skill(index));
            skillButtons.add(addRenderableWidget(button));
        }

        BattleHudLayout.Rect auto = layout.autoButton();
        autoButton = addRenderableWidget(new BattleHudButton(auto.x(), auto.y(), auto.width(), auto.height(),
                Component.literal("AUTO"), HEAL, ignored -> toggleAuto()));
        BattleHudLayout.Rect speed = layout.speedButton();
        speedButton = addRenderableWidget(new BattleHudButton(speed.x(), speed.y(), speed.width(), speed.height(),
                Component.literal("×1"), GAUGE, ignored -> toggleSpeed()));
        BattleHudLayout.Rect flee = layout.fleeButton();
        fleeButton = addRenderableWidget(new BattleHudButton(flee.x(), flee.y(), flee.width(), flee.height(),
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
        ClientBattleState.Snapshot snapshot = ClientBattleState.snapshot();

        if (!Objects.equals(selectedActor, snapshot.actorId())) {
            selectedActor = snapshot.actorId();
            clearSelection(true);
        }
        if (!selectedSkill.isBlank() && snapshot.skills().stream().noneMatch(s -> s.id().equals(selectedSkill))) {
            clearSelection(true);
        }

        boolean canAct = canChooseSkill(snapshot) && !settingsOpen;
        for (int i = 0; i < skillButtons.size(); i++) {
            BattleHudButton button = skillButtons.get(i);
            if (canAct && i < snapshot.skills().size()) {
                ClientBattleState.Skill skill = snapshot.skills().get(i);
                String cd = skill.remaining() > 0 ? "  · " + skill.remaining() : "";
                String mark = selectedSkill.equals(skill.id()) ? "▶ " : "";
                button.setMessage(Component.literal(mark + (i + 1) + "  " + skill.name() + cd));
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

        autoButton.setMessage(Component.literal(snapshot.auto() ? "AUTO•" : "AUTO"));
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
        return snapshot.skills().stream().filter(s -> s.id().equals(selectedSkill)).findFirst().orElse(null);
    }

    private void skill(int index) {
        if (settingsOpen) return;
        ClientBattleState.Snapshot snapshot = ClientBattleState.snapshot();
        if (!canChooseSkill(snapshot) || index < 0 || index >= snapshot.skills().size()) return;
        ClientBattleState.Skill skill = snapshot.skills().get(index);
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
        ClientBattleState.Snapshot snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill skill = selectedSkill(snapshot);
        if (skill == null || index < 0 || index >= snapshot.units().size()) return;
        ClientBattleState.Unit unit = snapshot.units().get(index);
        if (!BattleTargeting.validTarget(skill.targetRule(), unit, snapshot.actorId())) return;
        send("ACT|" + snapshot.actorId() + "|" + skill.id() + "|" + unit.id());
        clearSelection(true);
    }

    private void cycleTarget(int direction) {
        ClientBattleState.Snapshot snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill skill = selectedSkill(snapshot);
        if (skill == null) return;
        selectedTarget = BattleTargeting.cycle(snapshot.units(), skill.targetRule(), snapshot.actorId(), selectedTarget, direction);
        syncSelectedTarget(snapshot);
    }

    private void clearSelection(boolean clearWorldFocus) {
        selectedSkill = "";
        selectedTarget = -1;
        if (clearWorldFocus) setWorldFocus("");
    }

    private void syncSelectedTarget(ClientBattleState.Snapshot snapshot) {
        setWorldFocus(selectedTarget >= 0 && selectedTarget < snapshot.units().size()
                ? snapshot.units().get(selectedTarget).id() : "");
    }

    private void setWorldFocus(String targetId) {
        String next = targetId == null ? "" : targetId;
        if (Objects.equals(focusedTargetId, next)) return;
        focusedTargetId = next;
        send("FOCUS|" + next);
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
                if (selectedTarget >= 0) target(selectedTarget);
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
            int target = targetAt(event.x(), event.y());
            if (target >= 0) {
                target(target);
                return true;
            }
        }
        return event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!settingsOpen && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && !overHud(event.x(), event.y())) {
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

    private boolean overHud(double x, double y) {
        BattleHudLayout.Layout l = currentLayout();
        if (l.actionHeader().contains(x, y)) return true;
        for (BattleHudLayout.Rect r : l.skillButtons()) if (r.contains(x, y)) return true;
        for (BattleHudLayout.Rect r : l.allyBars()) if (r.contains(x, y)) return true;
        for (BattleHudLayout.Rect r : l.enemyBars()) if (r.contains(x, y)) return true;
        return l.autoButton().contains(x, y) || l.speedButton().contains(x, y) || l.fleeButton().contains(x, y);
    }

    private int targetAt(double x, double y) {
        ClientBattleState.Snapshot snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null) return -1;
        int allySlot = 0;
        int enemySlot = 0;
        for (int i = 0; i < snapshot.units().size(); i++) {
            ClientBattleState.Unit unit = snapshot.units().get(i);
            boolean ally = "ALLY".equals(unit.side());
            int slot = ally ? allySlot++ : enemySlot++;
            List<BattleHudLayout.Rect> bars = ally ? currentLayout().allyBars() : currentLayout().enemyBars();
            if (slot < bars.size() && bars.get(slot).contains(x, y)
                    && BattleTargeting.validTarget(selected.targetRule(), unit, snapshot.actorId())) return i;
        }
        return -1;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // World-first HUD: deliberately no full-screen tint or giant framed panel.
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ClientBattleState.Snapshot snapshot = ClientBattleState.snapshot();
        BattleHudLayout.Layout l = currentLayout();
        drawTimeline(graphics, l, snapshot);
        drawParty(graphics, l, snapshot);
        drawEnemies(graphics, l, snapshot);
        drawActionHeader(graphics, l, snapshot);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (snapshot.finished()) drawResult(graphics, snapshot);
        if (settingsOpen) drawSettings(graphics, l);
    }

    private void drawTimeline(GuiGraphicsExtractor g, BattleHudLayout.Layout l, ClientBattleState.Snapshot s) {
        BattleHudLayout.Rect r = l.timeline();
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x8F10131A);
        int count = Math.min(8, s.timeline().size());
        int gap = 2;
        int tokenW = Math.max(12, (r.width() - 8 - gap * Math.max(0, count - 1)) / Math.max(1, count));
        int x = r.x() + 4;
        for (int i = 0; i < count; i++) {
            String id = s.timeline().get(i);
            ClientBattleState.Unit u = findUnit(s, id);
            if (u == null) continue;
            int accent = "ALLY".equals(u.side()) ? GAUGE : DANGER;
            g.fill(x, r.y() + 3, Math.min(r.right() - 2, x + tokenW), r.bottom() - 3,
                    id.equals(s.actorId()) ? 0xD9222A38 : 0x9A171C26);
            g.fill(x, r.y() + 3, x + 2, r.bottom() - 3, accent);
            String text = abbreviate(u.name(), l.compact() ? 1 : 2);
            g.text(font, Component.literal(text), x + 4, r.y() + 5, TEXT, true);
            x += tokenW + gap;
        }
    }

    private void drawParty(GuiGraphicsExtractor g, BattleHudLayout.Layout l, ClientBattleState.Snapshot s) {
        int slot = 0;
        for (int i = 0; i < s.units().size() && slot < l.allyBars().size(); i++) {
            ClientBattleState.Unit u = s.units().get(i);
            if (!"ALLY".equals(u.side())) continue;
            drawStatusBar(g, l.allyBars().get(slot++), u, i == selectedTarget, u.id().equals(s.actorId()), l.compact());
        }
    }

    private void drawEnemies(GuiGraphicsExtractor g, BattleHudLayout.Layout l, ClientBattleState.Snapshot s) {
        int slot = 0;
        for (int i = 0; i < s.units().size() && slot < l.enemyBars().size(); i++) {
            ClientBattleState.Unit u = s.units().get(i);
            if ("ALLY".equals(u.side())) continue;
            drawStatusBar(g, l.enemyBars().get(slot++), u, i == selectedTarget, u.id().equals(s.actorId()), true);
        }
    }

    private void drawStatusBar(GuiGraphicsExtractor g, BattleHudLayout.Rect r, ClientBattleState.Unit u,
                               boolean selected, boolean actor, boolean compact) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), u.downed() ? 0x8810131A : GLASS);
        int accent = selected ? DANGER : actor ? GAUGE : 0x88394354;
        g.fill(r.x(), r.y(), r.x() + (selected || actor ? 2 : 1), r.bottom(), accent);

        String name = abbreviate(u.name(), compact ? 8 : 12);
        if (selected) name = "▼ " + name;
        if (u.downed()) name += " DOWN";
        g.text(font, Component.literal(name), r.x() + 4, r.y() + 2, u.downed() ? DISABLED : TEXT, true);

        int barX = r.x() + 4;
        int barRight = r.right() - 4;
        int barY = r.bottom() - 5;
        int barW = Math.max(0, barRight - barX);
        g.fill(barX, barY, barRight, barY + 3, 0xA0000000);
        int hpW = u.maxHp() <= 0 ? 0 : (int)Math.round(barW * (u.hp() / (double)u.maxHp()));
        hpW = Math.max(0, Math.min(barW, hpW));
        if (hpW > 0) g.fill(barX, barY, barX + hpW, barY + 3, HP);
        if (u.barrier() > 0 && barW > 0) {
            int bw = Math.min(barW, (int)Math.round(barW * (u.barrier() / (double)Math.max(1, u.maxHp()))));
            if (bw > 0) g.fill(barX, barY - 1, barX + bw, barY, GAUGE);
        }
    }

    private void drawActionHeader(GuiGraphicsExtractor g, BattleHudLayout.Layout l, ClientBattleState.Snapshot s) {
        if (!canChooseSkill(s) || settingsOpen) return;
        BattleHudLayout.Rect r = l.actionHeader();
        ClientBattleState.Unit actor = findUnit(s, s.actorId());
        String name = actor == null ? "행동" : actor.name();
        String prompt = selectedSkill.isBlank() ? "행동 선택" : "타겟 선택 · RMB 취소";
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x7810131A);
        g.text(font, Component.literal(name), r.x() + 4, r.y() + 3, TEXT, true);
        g.text(font, Component.literal(prompt), r.x() + 4, r.y() + 12, MUTED, true);
    }

    private void drawResult(GuiGraphicsExtractor g, ClientBattleState.Snapshot s) {
        String text = "ALLY_VICTORY".equals(s.outcome()) ? "승리  ·  R 복귀" : "패배  ·  R 복귀";
        int w = font.width(text) + 20;
        int x = Math.max(3, (width - w) / 2);
        int y = Math.max(3, height / 2 - 10);
        g.fill(x, y, Math.min(width, x + w), y + 20, GLASS_STRONG);
        g.fill(x, y, x + 2, y + 20, "ALLY_VICTORY".equals(s.outcome()) ? HEAL : DANGER);
        g.text(font, Component.literal(text), x + 10, y + 6, TEXT, true);
    }

    private void drawSettings(GuiGraphicsExtractor g, BattleHudLayout.Layout l) {
        BattleHudLayout.Rect r = l.settingsPanel();
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xED10131A);
        g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), GAUGE);
        int x = r.x() + 12;
        int y = r.y() + 10;
        g.text(font, Component.literal("전투 설정"), x, y, TEXT, true);
        g.text(font, Component.literal("Esc / RMB  닫기"), x, y + 18, MUTED, true);
        g.text(font, Component.literal("드래그  시점 회전   휠  줌"), x, y + 34, MUTED, true);
        g.text(font, Component.literal("1~5 행동   Tab 대상   A 자동"), x, y + 50, MUTED, true);
        g.text(font, Component.literal("X 배속   R 도주"), x, y + 66, MUTED, true);
    }

    private BattleHudLayout.Layout currentLayout() {
        if (layout == null) layout = BattleHudLayout.calculate(width, height);
        return layout;
    }

    private static ClientBattleState.Unit findUnit(ClientBattleState.Snapshot snapshot, String id) {
        for (ClientBattleState.Unit unit : snapshot.units()) if (unit.id().equals(id)) return unit;
        return null;
    }

    private static String abbreviate(String value, int max) {
        return value.substring(0, Math.min(max, value.length()));
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { }
}
