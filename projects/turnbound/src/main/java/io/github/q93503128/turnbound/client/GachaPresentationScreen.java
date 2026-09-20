package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.progression.GachaCatalog;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * World-first summon presentation. The server owns RNG and a private 3D hero actor in front of the player;
 * this screen only overlays readable result information and the final ten-pull summary.
 */
public final class GachaPresentationScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int BLUE = 0xFF6DC6FF;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFFFC857;
    private static final int PURPLE = 0xFFC794FF;

    public record Pull(String characterId, int stars, boolean newlyOwned, int essence, int pityAfter) {}
    public record Batch(String action, int crystalSpent, List<Pull> pulls) {
        public Batch { pulls = List.copyOf(pulls == null ? List.of() : pulls); }
    }

    private final Batch batch;
    private final List<Pull> revealPulls;
    private int ticks;
    private boolean audioQueued;
    private boolean finishing;

    public GachaPresentationScreen(Batch batch) {
        super(Component.literal("정령의 기록"));
        this.batch = batch == null ? new Batch("", 0, List.of()) : batch;
        List<Pull> newlyOwned = this.batch.pulls().stream().filter(Pull::newlyOwned).toList();
        if (!newlyOwned.isEmpty()) this.revealPulls = newlyOwned;
        else this.revealPulls = this.batch.pulls().stream()
                .max(Comparator.comparingInt(Pull::stars)).map(List::of).orElseGet(List::of);
    }

    public static Batch decode(String raw) {
        String action = "";
        int spent = 0;
        List<Pull> pulls = new ArrayList<>();
        if (raw == null) raw = "";
        for (String line : raw.split("\n")) {
            if (line.isBlank()) continue;
            String[] p = line.split("\\|", -1);
            try {
                switch (p[0]) {
                    case "H" -> { action = p[1]; spent = Integer.parseInt(p[3]); }
                    case "P" -> pulls.add(new Pull(p[1], Integer.parseInt(p[2]), "1".equals(p[3]),
                            Integer.parseInt(p[4]), Integer.parseInt(p[5])));
                    default -> { }
                }
            } catch (RuntimeException ignored) { }
        }
        return new Batch(action, spent, pulls);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new BattleHudButton(width - 98, 16, 80, 22,
                Component.literal("건너뛰기"), MUTED, ignored -> finish()));
        if (!audioQueued) {
            audioQueued = true;
            int priority = batch.pulls().stream().anyMatch(p -> p.stars() >= 5) ? 3 : 2;
            ClientAudioDirector.acceptBatch("spawn|SYSTEM|" + priority + "||||0");
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (ticks >= totalDurationTicks()) finish();
    }

    private int totalDurationTicks() {
        int reveal = Math.max(1, revealPulls.size()) * 30;
        return reveal + (batch.pulls().size() <= 1 ? 38 : 86);
    }

    private void finish() {
        if (finishing) return;
        finishing = true;
        ClientPacketDistributor.sendToServer(new MetaCommandPayload("GACHA_DONE"));
        if (minecraft != null) minecraft.gui.setScreen(new MetaMenuScreen(MetaMenuScreen.Tab.ARCHIVE));
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.text(font, Component.literal("정령의 기록"), 18, 18, TEXT, true);
        graphics.text(font, Component.literal(batch.pulls().size() + "회 소환 · 크리스탈 -" + batch.crystalSpent()),
                18, 34, SECONDARY, false);

        Pull focus = currentReveal();
        if (focus != null) drawWorldRevealOverlay(graphics, focus);
        else if (batch.pulls().size() <= 1) drawSingleSummary(graphics);
        else drawTenSummary(graphics);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private Pull currentReveal() {
        if (revealPulls.isEmpty()) return null;
        int index = ticks / 30;
        return index >= 0 && index < revealPulls.size() ? revealPulls.get(index) : null;
    }

    private void drawWorldRevealOverlay(GuiGraphicsExtractor graphics, Pull pull) {
        int w = Math.min(410, width - 44);
        int h = 76;
        int x = (width - w) / 2;
        int y = Math.max(62, height - h - 34);
        int accent = starColor(pull.stars());
        TurnboundFrameStyle.frame(graphics, x, y, w, h, accent);
        graphics.text(font, Component.literal(stars(pull.stars())), x + 18, y + 15, accent, true);
        graphics.text(font, Component.literal(name(pull.characterId())), x + 18, y + 35, TEXT, true);
        String result = pull.newlyOwned() ? "새로운 동료" : "별의 정수 +" + pull.essence();
        graphics.text(font, Component.literal(result), x + 18, y + 54, pull.newlyOwned() ? GREEN : PURPLE, false);
        if (pull.stars() >= 5) graphics.text(font, Component.literal("★5"), x + w - 48, y + 15, GOLD, true);
    }

    private void drawSingleSummary(GuiGraphicsExtractor graphics) {
        if (batch.pulls().isEmpty()) return;
        Pull pull = batch.pulls().getFirst();
        int w = Math.min(420, width - 44), h = 112;
        int x = (width - w) / 2, y = Math.max(66, height - h - 28);
        int accent = starColor(pull.stars());
        TurnboundFrameStyle.frame(graphics, x, y, w, h, accent);
        int portrait = Math.min(86, h - 14);
        TurnboundPortraitRenderer.extract(graphics, pull.characterId(), x + 8, y + 7, x + 8 + portrait, y + 7 + portrait, false);
        int tx = x + portrait + 18;
        graphics.text(font, Component.literal("소환 결과"), tx, y + 14, TEXT, true);
        graphics.text(font, Component.literal(stars(pull.stars()) + " · " + name(pull.characterId())), tx, y + 38, accent, true);
        graphics.text(font, Component.literal(pull.newlyOwned() ? "새로운 동료" : "별의 정수 +" + pull.essence()),
                tx, y + 62, pull.newlyOwned() ? GREEN : PURPLE, false);
        graphics.text(font, Component.literal("★5 천장 " + pull.pityAfter() + " / " + GachaCatalog.HARD_PITY),
                tx, y + 84, SECONDARY, false);
    }

    private void drawTenSummary(GuiGraphicsExtractor graphics) {
        int gap = 6;
        int totalW = Math.min(760, width - 36);
        int cardW = (totalW - gap * 4) / 5;
        int cardH = Math.min(108, Math.max(74, (height - 116 - gap) / 2));
        int startX = (width - totalW) / 2;
        int startY = Math.max(62, (height - (cardH * 2 + gap)) / 2 + 14);
        int summaryTicks = Math.max(0, ticks - Math.max(1, revealPulls.size()) * 30);
        int visible = Math.min(batch.pulls().size(), Math.max(1, summaryTicks / 4 + 1));

        for (int i = 0; i < batch.pulls().size() && i < 10; i++) {
            int x = startX + (i % 5) * (cardW + gap);
            int y = startY + (i / 5) * (cardH + gap);
            if (i >= visible) {
                TurnboundFrameStyle.inset(graphics, x, y, cardW, cardH);
                continue;
            }
            Pull pull = batch.pulls().get(i);
            int accent = starColor(pull.stars());
            TurnboundFrameStyle.frame(graphics, x, y, cardW, cardH, accent);
            graphics.text(font, Component.literal(stars(pull.stars())), x + 7, y + 6, accent, true);
            int portraitTop = y + 15;
            int portraitBottom = Math.max(portraitTop + 24, y + cardH - 35);
            TurnboundPortraitRenderer.extract(graphics, pull.characterId(), x + 4, portraitTop, x + cardW - 4, portraitBottom, false);
            graphics.text(font, Component.literal(shorten(name(pull.characterId()), 15)), x + 7, y + cardH - 31, TEXT, true);
            String result = pull.newlyOwned() ? "신규" : "정수 +" + pull.essence();
            graphics.text(font, Component.literal(result), x + 7, y + cardH - 18,
                    pull.newlyOwned() ? GREEN : PURPLE, false);
            graphics.text(font, Component.literal("천장 " + pull.pityAfter()), x + cardW - font.width("천장 " + pull.pityAfter()) - 6,
                    y + cardH - 18, SECONDARY, false);
        }
    }

    private static String name(String id) {
        try { return CanonicalData.definition(id).name(); }
        catch (RuntimeException ignored) { return "알 수 없는 동료"; }
    }

    private static String stars(int count) { return "★".repeat(Math.max(1, Math.min(5, count))); }
    private static int starColor(int stars) {
        return switch (stars) { case 5 -> GOLD; case 4 -> PURPLE; case 3 -> BLUE; default -> SECONDARY; };
    }
    private static String shorten(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER
                || event.key() == GLFW.GLFW_KEY_KP_ENTER || event.key() == GLFW.GLFW_KEY_SPACE) {
            finish();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void onClose() { finish(); }
}
