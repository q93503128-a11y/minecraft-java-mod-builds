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

public final class BattleScreen extends Screen {
    private static final Identifier PANEL = Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "turnbound/panel_blue");
    private static final Identifier INSET = Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "turnbound/panel_inset_blue");
    private final List<KenneyButton> skillButtons = new ArrayList<>();
    private final List<KenneyButton> targetButtons = new ArrayList<>();
    private KenneyButton autoButton;
    private KenneyButton speedButton;
    private KenneyButton fleeButton;
    private String selectedSkill = "";
    private long seen = -1;

    public BattleScreen() {
        super(Component.literal("TURNBOUND"));
    }

    @Override
    protected void init() {
        super.init();
        skillButtons.clear();
        targetButtons.clear();
        int skillWidth = Math.min(150, Math.max(105, width / 6));
        int skillY = height - 62;
        for (int i = 0; i < 5; i++) {
            final int index = i;
            KenneyButton button = new KenneyButton(
                    width / 2 - ((skillWidth + 4) * 5) / 2 + i * (skillWidth + 4),
                    skillY, skillWidth, 42, Component.empty(), ignored -> skill(index));
            skillButtons.add(addRenderableWidget(button));
        }
        int targetWidth = Math.min(138, Math.max(96, width / 7));
        for (int i = 0; i < 9; i++) {
            final int index = i;
            int x = i < 4 ? 18 : width - targetWidth - 18;
            int row = i < 4 ? i : i - 4;
            int y = 58 + row * 50;
            KenneyButton button = new KenneyButton(x, y, targetWidth, 42, Component.empty(), ignored -> target(index));
            targetButtons.add(addRenderableWidget(button));
        }
        autoButton = addRenderableWidget(new KenneyButton(width - 228, 12, 66, 32, Component.literal("AUTO"), b -> send("AUTO")));
        speedButton = addRenderableWidget(new KenneyButton(width - 156, 12, 66, 32, Component.literal("×1"), b -> send("SPEED")));
        fleeButton = addRenderableWidget(new KenneyButton(width - 84, 12, 66, 32, Component.literal("퇴각"), b -> send("FLEE")));
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
        if (!selectedSkill.isBlank() && snapshot.skills().stream().noneMatch(skill -> skill.id().equals(selectedSkill))) {
            selectedSkill = "";
        }
        for (int i = 0; i < skillButtons.size(); i++) {
            KenneyButton button = skillButtons.get(i);
            if (i < snapshot.skills().size()) {
                var skill = snapshot.skills().get(i);
                button.setMessage(Component.literal((selectedSkill.equals(skill.id()) ? "▶ " : "") + skill.name() + (skill.remaining() > 0 ? "  CD " + skill.remaining() : "")));
                button.active = !snapshot.finished() && !snapshot.auto() && snapshot.actorId().startsWith("ally_") && skill.remaining() == 0;
                button.visible = true;
            } else {
                button.visible = false;
            }
        }
        ClientBattleState.Skill selected = snapshot.skills().stream().filter(skill -> skill.id().equals(selectedSkill)).findFirst().orElse(null);
        for (int i = 0; i < targetButtons.size(); i++) {
            KenneyButton button = targetButtons.get(i);
            if (i < snapshot.units().size()) {
                var unit = snapshot.units().get(i);
                String barrier = unit.barrier() > 0 ? " +" + unit.barrier() : "";
                button.setMessage(Component.literal(unit.name() + "  " + unit.hp() + "/" + unit.maxHp() + barrier));
                button.active = !snapshot.finished() && selected != null && validTarget(selected.targetRule(), unit, snapshot.actorId());
                button.visible = true;
            } else {
                button.visible = false;
            }
        }
        autoButton.setMessage(Component.literal(snapshot.auto() ? "AUTO ON" : "AUTO"));
        speedButton.setMessage(Component.literal("×" + snapshot.speed()));
        fleeButton.setMessage(Component.literal(snapshot.finished() ? "복귀" : "퇴각"));
        autoButton.active = !snapshot.finished();
        speedButton.active = !snapshot.finished();
        fleeButton.active = true;
    }

    private static boolean validTarget(String rule, ClientBattleState.Unit unit, String actorId) {
        boolean ally = "ALLY".equals(unit.side());
        return switch (rule) {
            case "ALLY_SINGLE" -> ally && !unit.downed();
            case "ENEMY_SINGLE" -> !ally && !unit.downed();
            case "DEAD_ALLY_SINGLE" -> ally && unit.downed();
            default -> false;
        } && !actorId.isBlank();
    }

    private void skill(int index) {
        var snapshot = ClientBattleState.snapshot();
        if (index >= snapshot.skills().size()) return;
        var skill = snapshot.skills().get(index);
        selectedSkill = skill.id();
        if (skill.targetRule().equals("SELF") || skill.targetRule().endsWith("_ALL")) {
            send("ACT|" + snapshot.actorId() + "|" + skill.id() + "|");
            selectedSkill = "";
        } else {
            refresh();
        }
    }

    private void target(int index) {
        var snapshot = ClientBattleState.snapshot();
        if (selectedSkill.isBlank() || index >= snapshot.units().size()) return;
        ClientBattleState.Skill selected = snapshot.skills().stream().filter(skill -> skill.id().equals(selectedSkill)).findFirst().orElse(null);
        var unit = snapshot.units().get(index);
        if (selected == null || !validTarget(selected.targetRule(), unit, snapshot.actorId())) return;
        send("ACT|" + snapshot.actorId() + "|" + selectedSkill + "|" + unit.id());
        selectedSkill = "";
    }

    private static void send(String command) {
        ClientPacketDistributor.sendToServer(new BattleCommandPayload(command));
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            switch (keyEvent.key()) {
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
                default -> { }
            }
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x5510131A);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, 8, 48, Math.min(160, width / 4), 220);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, width - Math.min(160, width / 4) - 8, 48, Math.min(160, width / 4), 270);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INSET, width / 2 - 230, 8, 460, 42);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INSET, width / 2 - 390, height - 72, 780, 62);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        var snapshot = ClientBattleState.snapshot();
        graphics.text(font, Component.literal("TURNBOUND · " + (snapshot.finished() ? result(snapshot.outcome()) : "전투 진행")), width / 2 - 65, 17, 0xFFF4F0E6, true);
        graphics.text(font, Component.literal("방향키: 시점 조절"), width / 2 - 48, 49, 0xFFB9C6D8, true);
        int x = width / 2 - 190;
        for (String id : snapshot.timeline()) {
            var unit = snapshot.units().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
            if (unit != null) {
                graphics.text(font, Component.literal(unit.name().substring(0, Math.min(2, unit.name().length()))), x, 35, unit.side().equals("ALLY") ? 0xFF6DC6FF : 0xFFFF7A59, true);
                x += 48;
            }
        }
        if (!snapshot.message().isBlank()) {
            graphics.text(font, Component.literal(snapshot.message()), width / 2 - 150, height - 84, 0xFFAEB7C6, true);
        }
    }

    private static String result(String outcome) {
        return outcome.equals("ALLY_VICTORY") ? "승리" : outcome.equals("ENEMY_VICTORY") ? "패배" : "종료";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (ClientBattleState.snapshot().finished()) send("FLEE");
    }
}
