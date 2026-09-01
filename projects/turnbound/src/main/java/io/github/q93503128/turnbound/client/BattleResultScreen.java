package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.network.BattleCommandPayload;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/** Canon §131 post-battle Result page. The 3D victory presentation remains visible before the panel appears. */
public final class BattleResultScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFF6C85F;
    private static final int DANGER = 0xFFFF7A59;
    private static final int GAUGE = 0xFF6DC6FF;
    private static final int PANEL_DARK = 0xD90A0D13;
    /** Current authored victory clips peak around 1.7s; add the canonical 0.4s Result pause. */
    private static final int VICTORY_REVEAL_TICKS = 42;
    private static final int DEFEAT_REVEAL_TICKS = 12;

    private BattleHudButton continueButton;
    private int elapsedTicks;

    public BattleResultScreen() { super(Component.literal("TURNBOUND Result")); }

    @Override
    protected void init() {
        super.init();
        Rect panel = panel();
        continueButton = addRenderableWidget(new BattleHudButton(
                panel.right() - 96, panel.bottom() - 31, 82, 22,
                Component.literal("Continue  R"), GREEN, ignored -> continueField()));
        updateContinueVisibility();
    }

    @Override
    public void tick() {
        super.tick();
        var snapshot = ClientBattleState.snapshot();
        if (!snapshot.active()) {
            if (minecraft != null) minecraft.gui.setScreen(null);
            return;
        }
        if (!snapshot.finished() && minecraft != null) {
            minecraft.gui.setScreen(new BattleScreen());
            return;
        }
        elapsedTicks++;
        updateContinueVisibility();
    }

    private void updateContinueVisibility() {
        if (continueButton == null) return;
        boolean visible = resultVisible();
        continueButton.visible = visible;
        continueButton.active = visible;
    }

    private boolean resultVisible() {
        boolean victory = "ALLY_VICTORY".equals(ClientBattleState.snapshot().outcome());
        return elapsedTicks >= (victory ? VICTORY_REVEAL_TICKS : DEFEAT_REVEAL_TICKS);
    }

    private void continueField() {
        if (!resultVisible()) return;
        ClientPacketDistributor.sendToServer(new BattleCommandPayload("FLEE"));
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!resultVisible()) return;

        ClientBattleState.Snapshot snapshot = ClientBattleState.snapshot();
        Rect panel = panel();
        boolean victory = "ALLY_VICTORY".equals(snapshot.outcome());
        int accent = victory ? GREEN : DANGER;

        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), PANEL_DARK);
        TurnboundFrameStyle.frame(graphics, panel.x(), panel.y(), panel.width(), panel.height(), accent);
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.y() + 3, accent);

        int x = panel.x() + 16;
        int y = panel.y() + 13;
        graphics.text(font, Component.literal(victory ? "VICTORY" : "DEFEAT"), x, y, accent, true);
        String encounter = encounterLabel(ClientBattleState.encounterId());
        if (!encounter.isBlank()) graphics.text(font, Component.literal(encounter), x, y + 15, SECONDARY, false);

        if (!victory) {
            graphics.text(font, Component.literal("보상 없음"), x, y + 42, DANGER, true);
            graphics.text(font, Component.literal("패배 시 XP와 전투 보상은 지급되지 않습니다."), x, y + 59, TEXT, false);
            graphics.text(font, Component.literal("파티를 정비한 뒤 다시 도전할 수 있습니다."), x, y + 76, SECONDARY, false);
            return;
        }

        ClientBattleState.Result result = snapshot.result();
        if (result.firstClear()) {
            graphics.text(font, Component.literal("FIRST CLEAR"), panel.right() - 91, y, GOLD, true);
        }

        int rewardTop = y + (encounter.isBlank() ? 28 : 38);
        graphics.text(font, Component.literal("REWARDS"), x, rewardTop, SECONDARY, true);
        int cardY = rewardTop + 14;
        int cardGap = 5;
        int cardW = Math.max(72, (panel.width() - 32 - cardGap * 3) / 4);
        rewardCard(graphics, x, cardY, cardW, "Gold", result.gold(), GOLD);
        rewardCard(graphics, x + (cardW + cardGap), cardY, cardW, "XP", result.xp(), GAUGE);
        rewardCard(graphics, x + (cardW + cardGap) * 2, cardY, cardW, "Crystal", result.crystal(), result.crystal() > 0 ? GAUGE : MUTED);
        rewardCard(graphics, x + (cardW + cardGap) * 3, cardY, cardW, "Essence", result.starEssence(), result.starEssence() > 0 ? GOLD : MUTED);

        int extraY = cardY + 34;
        for (String item : result.equipmentRewards()) {
            graphics.text(font, Component.literal("획득 · " + item), x, extraY, GREEN, true);
            extraY += 14;
        }
        for (String notice : ClientBattleState.resultNotices()) {
            graphics.text(font, Component.literal(notice), x, extraY, GAUGE, true);
            extraY += 14;
        }
        if (!result.firstClear() && result.equipmentRewards().isEmpty() && ClientBattleState.resultNotices().isEmpty()) {
            graphics.text(font, Component.literal("반복 클리어 · 반복 보상만 지급"), x, extraY, SECONDARY, false);
            extraY += 14;
        }

        int sectionY = Math.max(extraY + 3, cardY + 39);
        graphics.fill(x, sectionY, panel.right() - 16, sectionY + 1, 0x55707987);
        graphics.text(font, Component.literal("PARTY GROWTH"), x, sectionY + 8, SECONDARY, true);
        int rowY = sectionY + 24;
        int rowW = panel.width() - 32;
        for (int i = 0; i < result.party().size() && i < 4; i++) {
            if (rowY + 24 > panel.bottom() - 37) break;
            drawPartyXp(graphics, x, rowY, rowW, result.party().get(i));
            rowY += 28;
        }
    }

    private void rewardCard(GuiGraphicsExtractor graphics, int x, int y, int width, String label, int value, int color) {
        graphics.fill(x, y, x + width, y + 27, 0xA4141922);
        graphics.fill(x, y, x + 2, y + 27, color);
        graphics.text(font, Component.literal(label), x + 7, y + 5, SECONDARY, false);
        graphics.text(font, Component.literal(value <= 0 ? "-" : "+" + value), x + 7, y + 16, color, true);
    }

    private void drawPartyXp(GuiGraphicsExtractor graphics, int x, int y, int width, ClientBattleState.PartyXp member) {
        boolean levelUp = member.levelAfter() > member.levelBefore();
        boolean cap = member.xpToNextAfter() <= 0;
        String level = levelUp ? "Lv." + member.levelBefore() + " → " + member.levelAfter() : "Lv." + member.levelAfter();
        graphics.text(font, Component.literal(member.name()), x, y, TEXT, true);
        graphics.text(font, Component.literal(level), x + 96, y, levelUp ? GOLD : SECONDARY, true);
        if (levelUp) graphics.text(font, Component.literal("LEVEL UP"), x + width - 56, y, GOLD, true);
        else if (cap) graphics.text(font, Component.literal("STAR CAP"), x + width - 52, y, GAUGE, true);

        int barX = x, barY = y + 13, barW = Math.max(90, width - 105);
        graphics.fill(barX, barY, barX + barW, barY + 4, 0xE0080A0E);
        int fill = cap ? barW : (int)Math.round(barW * Math.min(1.0, member.xpAfter() / (double)Math.max(1, member.xpToNextAfter())));
        if (fill > 0) graphics.fill(barX, barY, barX + fill, barY + 4, GAUGE);
        String xp = cap ? "CAP" : member.xpAfter() + " / " + member.xpToNextAfter();
        graphics.text(font, Component.literal(xp), barX + barW + 8, y + 9, cap ? GAUGE : SECONDARY, false);
    }

    private String encounterLabel(String rawId) {
        if (rawId == null || rawId.isBlank()) return "";
        try {
            if (rawId.matches("HARD_B0[1-5]")) {
                String bossId = rawId.substring("HARD_".length());
                return CanonicalData.definition(bossId).name() + " · HARD";
            }
            if (rawId.matches("RIFT_F\\d{2}")) {
                int floor = Integer.parseInt(rawId.substring("RIFT_F".length()));
                return "Rift Gate · F" + floor + " · Lv." + V04Catalogs.riftFloor(floor).level();
            }
            String canonical = CampaignProgressStore.canonicalEncounterId(rawId);
            if (V04Catalogs.hasEncounter(canonical)) return V04Catalogs.encounter(canonical).label();
        } catch (RuntimeException ignored) { }
        return rawId;
    }

    private Rect panel() {
        ClientBattleState.Result result = ClientBattleState.snapshot().result();
        int extras = result.equipmentRewards().size() + ClientBattleState.resultNotices().size();
        int partyRows = Math.min(4, result.party().size());
        int desiredW = 456;
        int desiredH = 148 + extras * 14 + partyRows * 28;
        if (!"ALLY_VICTORY".equals(ClientBattleState.snapshot().outcome())) desiredH = 154;
        int w = Math.max(304, Math.min(desiredW, width - 24));
        int h = Math.max(154, Math.min(desiredH, height - 24));
        return new Rect((width - w) / 2, Math.max(12, height / 2 - h / 2), w, h);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (resultVisible() && (event.key() == GLFW.GLFW_KEY_R || event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            continueField();
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
