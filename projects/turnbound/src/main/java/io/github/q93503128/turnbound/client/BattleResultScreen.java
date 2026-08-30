package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.BattleCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/** Compact post-battle result overlay. The 3D battlefield stays visible behind the reward summary. */
public final class BattleResultScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFF6C85F;
    private static final int DANGER = 0xFFFF7A59;
    private static final int GAUGE = 0xFF6DC6FF;
    private BattleHudButton continueButton;

    public BattleResultScreen() { super(Component.literal("TURNBOUND Result")); }

    @Override
    protected void init() {
        super.init();
        Rect panel = panel();
        continueButton = addRenderableWidget(new BattleHudButton(
                panel.right() - 76, panel.bottom() - 27, 64, 18,
                Component.literal("계속  R"), GREEN, ignored -> continueField()));
    }

    @Override
    public void tick() {
        super.tick();
        var snapshot = ClientBattleState.snapshot();
        if (!snapshot.active()) {
            if (minecraft != null) minecraft.gui.setScreen(null);
            return;
        }
        if (!snapshot.finished() && minecraft != null) minecraft.gui.setScreen(new BattleScreen());
    }

    private void continueField() {
        ClientPacketDistributor.sendToServer(new BattleCommandPayload("FLEE"));
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        ClientBattleState.Snapshot snapshot = ClientBattleState.snapshot();
        Rect panel = panel();
        boolean victory = "ALLY_VICTORY".equals(snapshot.outcome());
        int accent = victory ? GREEN : DANGER;
        TurnboundFrameStyle.frame(graphics, panel.x(), panel.y(), panel.width(), panel.height(), accent);

        int x = panel.x() + 13;
        int y = panel.y() + 10;
        String heading = victory ? "전투 승리" : "전투 패배";
        graphics.text(font, Component.literal(heading), x, y, accent, true);
        if (!victory) {
            graphics.text(font, Component.literal("보상 없음"), x, y + 20, SECONDARY, true);
            graphics.text(font, Component.literal("파티 상태는 거점 복귀 후 회복됩니다."), x, y + 37, TEXT, true);
            return;
        }

        ClientBattleState.Result result = snapshot.result();
        String reward = "XP  +" + result.xp() + "    Gold  +" + result.gold();
        graphics.text(font, Component.literal(reward), x, y + 18, GOLD, true);
        if (!result.firstClear()) graphics.text(font, Component.literal("재클리어 보상 없음"), x, y + 31, SECONDARY, true);

        int rowY = y + 43;
        int rowW = panel.width() - 26;
        for (int i = 0; i < result.party().size() && i < 4; i++) {
            ClientBattleState.PartyXp member = result.party().get(i);
            drawPartyXp(graphics, x, rowY, rowW, member);
            rowY += 24;
        }
    }

    private void drawPartyXp(GuiGraphicsExtractor graphics, int x, int y, int width, ClientBattleState.PartyXp member) {
        String level = member.levelAfter() > member.levelBefore()
                ? "Lv." + member.levelBefore() + " → " + member.levelAfter()
                : "Lv." + member.levelAfter();
        graphics.text(font, Component.literal(member.name()), x, y, TEXT, true);
        graphics.text(font, Component.literal(level), x + 92, y, member.levelAfter() > member.levelBefore() ? GOLD : SECONDARY, true);

        int barX = x;
        int barY = y + 12;
        int barW = Math.max(80, width - 84);
        graphics.fill(barX, barY, barX + barW, barY + 3, 0xE0080A0E);
        int fill;
        if (member.xpToNextAfter() <= 0) fill = barW;
        else fill = (int)Math.round(barW * Math.min(1.0, member.xpAfter() / (double)member.xpToNextAfter()));
        if (fill > 0) graphics.fill(barX, barY, barX + fill, barY + 3, GAUGE);
        String xp = member.xpToNextAfter() <= 0 ? "MAX" : member.xpAfter() + " / " + member.xpToNextAfter();
        graphics.text(font, Component.literal(xp), barX + barW + 7, y + 8, SECONDARY, true);
    }

    private Rect panel() {
        int desiredW = 340;
        int desiredH = 166;
        int w = Math.max(260, Math.min(desiredW, width - 24));
        int h = Math.max(138, Math.min(desiredH, height - 24));
        return new Rect((width - w) / 2, Math.max(12, height / 2 - h / 2), w, h);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_R || event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            continueField();
            return true;
        }
        return true;
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { }

    private record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
    }
}
