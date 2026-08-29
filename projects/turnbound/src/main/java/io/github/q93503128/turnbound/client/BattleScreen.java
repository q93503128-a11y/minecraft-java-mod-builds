package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.network.BattleCommandPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BattleScreen extends Screen {
    private static final Identifier PANEL = Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "turnbound/panel_blue");
    private static final Identifier INSET = Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "turnbound/panel_inset_blue");

    private final List<KenneyButton> skillButtons = new ArrayList<>();
    private final List<KenneyButton> targetButtons = new ArrayList<>();
    private KenneyButton autoButton;
    private KenneyButton speedButton;
    private KenneyButton returnButton;
    private BattleScreenLayout.Layout layout;
    private String selectedSkill = "";
    private String selectedActor = "";
    private int selectedTarget = -1;
    private long seen = -1;

    public BattleScreen() {
        super(Component.literal("TURNBOUND"));
    }

    @Override
    protected void init() {
        super.init();
        skillButtons.clear();
        targetButtons.clear();
        layout = BattleScreenLayout.calculate(width, height);

        for (int i = 0; i < BattleScreenLayout.SKILL_COUNT; i++) {
            final int index = i;
            BattleScreenLayout.Rect bounds = layout.skillButtons().get(i);
            KenneyButton button = new KenneyButton(
                    bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Component.empty(), ignored -> skill(index));
            skillButtons.add(addRenderableWidget(button));
        }

        for (int i = 0; i < BattleScreenLayout.ALLY_TARGET_COUNT + BattleScreenLayout.ENEMY_TARGET_COUNT; i++) {
            final int index = i;
            BattleScreenLayout.Rect bounds = layout.targetButtons().get(i);
            KenneyButton button = new KenneyButton(
                    bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Component.empty(), ignored -> target(index));
            targetButtons.add(addRenderableWidget(button));
        }

        BattleScreenLayout.Rect autoBounds = layout.autoButton();
        autoButton = addRenderableWidget(new KenneyButton(
                autoBounds.x(), autoBounds.y(), autoBounds.width(), autoBounds.height(),
                Component.literal("[A] AUTO"), b -> toggleAuto()));

        BattleScreenLayout.Rect speedBounds = layout.speedButton();
        speedButton = addRenderableWidget(new KenneyButton(
                speedBounds.x(), speedBounds.y(), speedBounds.width(), speedBounds.height(),
                Component.literal("[X] ×1"), b -> toggleSpeed()));

        BattleScreenLayout.Rect returnBounds = layout.fleeButton();
        returnButton = addRenderableWidget(new KenneyButton(
                returnBounds.x(), returnBounds.y(), returnBounds.width(), returnBounds.height(),
                Component.literal("[R] 복귀"), b -> returnToField()));

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
            clearSelection();
        }

        if (!selectedSkill.isBlank() && snapshot.skills().stream().noneMatch(skill -> skill.id().equals(selectedSkill))) {
            clearSelection();
        }

        for (int i = 0; i < skillButtons.size(); i++) {
            KenneyButton button = skillButtons.get(i);
            if (i < snapshot.skills().size()) {
                var skill = snapshot.skills().get(i);
                String marker = selectedSkill.equals(skill.id()) ? "▶ " : "";
                String cooldown = skill.remaining() > 0 ? " · CD " + skill.remaining() : "";
                button.setMessage(Component.literal(marker + "[" + (i + 1) + "] " + skill.name() + cooldown));
                button.active = canChooseSkill(snapshot) && skill.remaining() == 0;
                button.visible = true;
            } else {
                button.visible = false;
            }
        }

        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null) {
            selectedTarget = -1;
        } else if (selectedTarget < 0
                || selectedTarget >= snapshot.units().size()
                || !BattleTargeting.validTarget(selected.targetRule(), snapshot.units().get(selectedTarget), snapshot.actorId())) {
            selectedTarget = BattleTargeting.firstValid(snapshot.units(), selected.targetRule(), snapshot.actorId());
        }

        for (int i = 0; i < targetButtons.size(); i++) {
            KenneyButton button = targetButtons.get(i);
            if (i < snapshot.units().size()) {
                var unit = snapshot.units().get(i);
                boolean selectedHere = i == selectedTarget && selected != null;
                String marker = selectedHere ? "▶ " : "";
                String state = unit.downed() ? " [DOWN]" : "";
                String barrier = unit.barrier() > 0 ? " +B" + unit.barrier() : "";
                button.setMessage(Component.literal(marker + unit.name() + state + "  " + unit.hp() + "/" + unit.maxHp() + barrier));
                button.active = !snapshot.finished()
                        && !snapshot.auto()
                        && selected != null
                        && BattleTargeting.validTarget(selected.targetRule(), unit, snapshot.actorId());
                button.visible = true;
            } else {
                button.visible = false;
            }
        }

        autoButton.setMessage(Component.literal(snapshot.auto() ? "[A] AUTO ON" : "[A] AUTO"));
        speedButton.setMessage(Component.literal("[X] ×" + snapshot.speed()));
        autoButton.active = !snapshot.finished();
        speedButton.active = !snapshot.finished();

        returnButton.setMessage(Component.literal("[R] 복귀"));
        returnButton.visible = snapshot.finished();
        returnButton.active = snapshot.finished();
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
        var snapshot = ClientBattleState.snapshot();
        if (!canChooseSkill(snapshot) || index < 0 || index >= snapshot.skills().size()) return;

        var skill = snapshot.skills().get(index);
        if (skill.remaining() > 0) return;

        selectedSkill = skill.id();
        selectedTarget = -1;
        if (skill.targetRule().equals("SELF") || skill.targetRule().endsWith("_ALL")) {
            send("ACT|" + snapshot.actorId() + "|" + skill.id() + "|");
            clearSelection();
        } else {
            selectedTarget = BattleTargeting.firstValid(snapshot.units(), skill.targetRule(), snapshot.actorId());
            refresh();
        }
    }

    private void target(int index) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null || index < 0 || index >= snapshot.units().size()) return;

        var unit = snapshot.units().get(index);
        if (!BattleTargeting.validTarget(selected.targetRule(), unit, snapshot.actorId())) return;

        send("ACT|" + snapshot.actorId() + "|" + selected.id() + "|" + unit.id());
        clearSelection();
    }

    private void cycleTarget(int direction) {
        var snapshot = ClientBattleState.snapshot();
        ClientBattleState.Skill selected = selectedSkill(snapshot);
        if (selected == null) return;
        selectedTarget = BattleTargeting.cycle(
                snapshot.units(), selected.targetRule(), snapshot.actorId(), selectedTarget, direction);
        refresh();
    }

    private void confirmTarget() {
        if (selectedTarget >= 0) target(selectedTarget);
    }

    private void clearSelection() {
        selectedSkill = "";
        selectedTarget = -1;
    }

    private void toggleAuto() {
        var snapshot = ClientBattleState.snapshot();
        if (snapshot.finished()) return;
        clearSelection();
        send("AUTO");
    }

    private void toggleSpeed() {
        if (!ClientBattleState.snapshot().finished()) send("SPEED");
    }

    private void returnToField() {
        if (ClientBattleState.snapshot().finished()) send("FLEE");
    }

    private static void send(String command) {
        ClientPacketDistributor.sendToServer(new BattleCommandPayload(command));
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int key = keyEvent.key();

        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_5) {
            skill(key - GLFW.GLFW_KEY_1);
            return true;
        }

        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                if (!selectedSkill.isBlank()) {
                    cycleTarget(keyEvent.hasShiftDown() ? -1 : 1);
                    return true;
                }
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (!selectedSkill.isBlank()) {
                    confirmTarget();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (!selectedSkill.isBlank()) {
                    clearSelection();
                    refresh();
                }
                return true;
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
                returnToField();
                return true;
            }
            default -> {
            }
        }

        var player = Minecraft.getInstance().player;
        if (player != null) {
            switch (key) {
                case GLFW.GLFW_KEY_LEFT -> {
                    player.setYRot(player.getYRot() - 10.0F);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    player.setYRot(player.getYRot() + 10.0F);
                    return true;
                }
                case GLFW.GLFW_KEY_UP -> {
                    player.setXRot(Math.max(-65.0F, player.getXRot() - 6.0F));
                    return true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    player.setXRot(Math.min(65.0F, player.getXRot() + 6.0F));
                    return true;
                }
                default -> {
                }
            }
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        BattleScreenLayout.Layout current = currentLayout();
        graphics.fill(0, 0, width, height, 0x5510131A);
        blit(graphics, PANEL, current.leftPanel());
        blit(graphics, PANEL, current.rightPanel());
        blit(graphics, INSET, current.topInset());
        blit(graphics, INSET, current.bottomInset());
    }

    private static void blit(GuiGraphicsExtractor graphics, Identifier sprite, BattleScreenLayout.Rect bounds) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                sprite,
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height()
        );
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        BattleScreenLayout.Layout current = currentLayout();
        var snapshot = ClientBattleState.snapshot();

        String status = snapshot.finished()
                ? result(snapshot.outcome())
                : actorLabel(snapshot);
        graphics.text(font, Component.literal("TURNBOUND · " + status),
                Math.max(8, width / 2 - 65), 17, 0xFFF4F0E6, true);

        graphics.text(font, Component.literal(inputHint(snapshot)),
                Math.max(8, width / 2 - 120), current.hintY(), 0xFFB9C6D8, true);

        int timelineCount = Math.min(8, snapshot.timeline().size());
        int x = Math.max(8, width / 2 - Math.max(0, timelineCount * 48 - 8) / 2);
        for (int i = 0; i < timelineCount; i++) {
            String id = snapshot.timeline().get(i);
            var unit = snapshot.units().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
            if (unit != null) {
                String token = unit.name().substring(0, Math.min(2, unit.name().length()));
                if (id.equals(snapshot.actorId())) token = "▶" + token;
                graphics.text(font, Component.literal(token), x, 35,
                        unit.side().equals("ALLY") ? 0xFF6DC6FF : 0xFFFF7A59, true);
                x += 48;
                if (x >= width - 24) break;
            }
        }

        if (!snapshot.message().isBlank()) {
            graphics.text(font, Component.literal(snapshot.message()),
                    Math.max(8, width / 2 - 150), current.messageY(), 0xFFAEB7C6, true);
        }
    }

    private static String actorLabel(ClientBattleState.Snapshot snapshot) {
        if (snapshot.actorId().isBlank()) return "턴 계산 중";
        return snapshot.units().stream()
                .filter(unit -> unit.id().equals(snapshot.actorId()))
                .findFirst()
                .map(unit -> unit.name() + " 행동")
                .orElse("전투 진행");
    }

    private String inputHint(ClientBattleState.Snapshot snapshot) {
        if (snapshot.finished()) return "R 또는 복귀 버튼: 필드로 돌아가기";
        if (snapshot.auto()) return "AUTO 진행 · A 해제 · X 배속 · 방향키 시점";
        if (!selectedSkill.isBlank()) return "Tab / Shift+Tab 대상 · Enter 확정 · Esc 취소";
        if (snapshot.actorId().startsWith("ally_")) return "1~5 스킬 · A AUTO · X 배속 · 방향키 시점";
        return "적 행동 중 · A AUTO · X 배속 · 방향키 시점";
    }

    private BattleScreenLayout.Layout currentLayout() {
        if (layout == null) layout = BattleScreenLayout.calculate(width, height);
        return layout;
    }

    private static String result(String outcome) {
        return outcome.equals("ALLY_VICTORY")
                ? "승리"
                : outcome.equals("ENEMY_VICTORY") ? "패배" : "종료";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        // Battle lifecycle is server-authoritative. ESC never tears down a live battle screen.
    }
}
