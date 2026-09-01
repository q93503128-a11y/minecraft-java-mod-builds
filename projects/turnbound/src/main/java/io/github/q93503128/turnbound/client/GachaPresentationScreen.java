package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.CharacterMenuCatalog;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Canon §130 Echo Archive summon presentation. Skip is available from the first frame. */
public final class GachaPresentationScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int BLUE = 0xFF6DC6FF;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFFFC857;
    private static final int PURPLE = 0xFFC794FF;
    private static final int PANEL = 0xED090C12;

    public record Pull(String characterId, int stars, boolean newlyOwned, int essence, int pityAfter) {}
    public record Batch(String action, int crystalSpent, List<Pull> pulls) {
        public Batch { pulls = List.copyOf(pulls == null ? List.of() : pulls); }
    }

    private final Batch batch;
    private final List<Pull> newPulls;
    private int ticks;
    private boolean audioQueued;

    public GachaPresentationScreen(Batch batch) {
        super(Component.literal("Echo Archive"));
        this.batch = batch == null ? new Batch("", 0, List.of()) : batch;
        this.newPulls = this.batch.pulls().stream().filter(Pull::newlyOwned).toList();
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
        addRenderableWidget(new BattleHudButton(width - 98, 18, 80, 22,
                Component.literal("SKIP"), MUTED, ignored -> finish()));
        if (!audioQueued) {
            audioQueued = true;
            boolean five = batch.pulls().stream().anyMatch(p -> p.stars() >= 5);
            ClientAudioDirector.acceptBatch((five ? "gacha_five_star" : "gacha_reveal") + "|SYSTEM|3||||0");
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (ticks >= totalDurationTicks()) finish();
    }

    private int totalDurationTicks() {
        int base = batch.pulls().size() <= 1 ? 36 : 90;
        return Math.max(base, newPulls.size() * 30 + (batch.pulls().size() <= 1 ? 6 : 36));
    }

    private void finish() {
        if (minecraft != null) minecraft.gui.setScreen(new MetaMenuScreen(MetaMenuScreen.Tab.ARCHIVE));
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, PANEL);
        int rail = batch.pulls().stream().anyMatch(p -> p.stars() >= 5) ? GOLD : BLUE;
        graphics.fill(0, 0, width, 3, rail);
        graphics.fill(0, height - 3, width, height, rail);
        graphics.text(font, Component.literal("ECHO ARCHIVE"), 22, 20, TEXT, true);
        graphics.text(font, Component.literal(batch.pulls().size() + "회 소환 · Crystal -" + batch.crystalSpent()), 22, 37, SECONDARY, false);

        Pull newFocus = currentNewFocus();
        if (newFocus != null) drawNewReveal(graphics, newFocus);
        else if (batch.pulls().size() <= 1) drawSingle(graphics);
        else drawTen(graphics);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private Pull currentNewFocus() {
        if (newPulls.isEmpty()) return null;
        int index = ticks / 30;
        return index >= 0 && index < newPulls.size() ? newPulls.get(index) : null;
    }

    private void drawNewReveal(GuiGraphicsExtractor graphics, Pull pull) {
        int cardW = Math.min(470, width - 60), cardH = Math.min(286, height - 86);
        int x = (width - cardW) / 2, y = (height - cardH) / 2 + 8;
        int accent = starColor(pull.stars());
        graphics.fill(x, y, x + cardW, y + cardH, 0xE7121720);
        TurnboundFrameStyle.frame(graphics, x, y, cardW, cardH, accent);
        graphics.fill(x, y, x + 5, y + cardH, accent);
        graphics.fill(x + cardW - 5, y, x + cardW, y + cardH, accent);

        String name = name(pull.characterId());
        var profile = CharacterMenuCatalog.profile(pull.characterId());
        graphics.text(font, Component.literal("NEW"), x + 24, y + 24, GREEN, true);
        graphics.text(font, Component.literal(stars(pull.stars())), x + 24, y + 49, accent, true);
        graphics.text(font, Component.literal(name), x + 24, y + 74, TEXT, true);
        graphics.text(font, Component.literal(profile.role()), x + 24, y + 94, SECONDARY, false);
        graphics.text(font, Component.literal("Weapon · " + profile.weapon()), x + 24, y + 112, SECONDARY, false);

        int portraitX = x + cardW / 2 + 4;
        int portraitY = y + 35;
        int portraitW = cardW / 2 - 30;
        int portraitH = cardH - 70;
        graphics.fill(portraitX, portraitY, portraitX + portraitW, portraitY + portraitH, 0xAA151B25);
        graphics.fill(portraitX, portraitY, portraitX + 4, portraitY + portraitH, accent);
        graphics.fill(portraitX + portraitW - 4, portraitY, portraitX + portraitW, portraitY + portraitH, accent);
        graphics.text(font, Component.literal(pull.characterId()), portraitX + 14, portraitY + 16, MUTED, true);
        graphics.text(font, Component.literal(name), portraitX + 14, portraitY + portraitH - 27, accent, true);
    }

    private void drawSingle(GuiGraphicsExtractor graphics) {
        if (batch.pulls().isEmpty()) return;
        Pull pull = batch.pulls().getFirst();
        int w = Math.min(390, width - 50), h = Math.min(220, height - 80);
        int x = (width - w) / 2, y = (height - h) / 2 + 8;
        int accent = starColor(pull.stars());
        graphics.fill(x, y, x + w, y + h, 0xE7161B24);
        TurnboundFrameStyle.frame(graphics, x, y, w, h, accent);
        graphics.fill(x, y, x + w, y + 4, accent);
        graphics.text(font, Component.literal(stars(pull.stars())), x + 22, y + 26, accent, true);
        graphics.text(font, Component.literal(name(pull.characterId())), x + 22, y + 52, TEXT, true);
        graphics.text(font, Component.literal(pull.newlyOwned() ? "NEW" : "Star Essence +" + pull.essence()),
                x + 22, y + 78, pull.newlyOwned() ? GREEN : PURPLE, true);
        graphics.text(font, Component.literal("Pity · " + pull.pityAfter() + " / 80"), x + 22, y + 102, SECONDARY, false);
        if (pull.stars() >= 5) graphics.text(font, Component.literal("★5"), x + w - 54, y + h - 35, GOLD, true);
    }

    private void drawTen(GuiGraphicsExtractor graphics) {
        int gap = 8;
        int totalW = Math.min(760, width - 44);
        int cardW = (totalW - gap * 4) / 5;
        int cardH = Math.min(126, Math.max(82, (height - 110 - gap) / 2));
        int startX = (width - totalW) / 2;
        int startY = Math.max(72, (height - (cardH * 2 + gap)) / 2 + 14);
        int visible = Math.min(batch.pulls().size(), Math.max(1, (ticks - newPulls.size() * 30) / 4 + 1));
        for (int i = 0; i < batch.pulls().size() && i < 10; i++) {
            int x = startX + (i % 5) * (cardW + gap);
            int y = startY + (i / 5) * (cardH + gap);
            if (i >= visible) {
                graphics.fill(x, y, x + cardW, y + cardH, 0xA40E1218);
                TurnboundFrameStyle.frame(graphics, x, y, cardW, cardH, MUTED);
                continue;
            }
            Pull pull = batch.pulls().get(i);
            int accent = starColor(pull.stars());
            graphics.fill(x, y, x + cardW, y + cardH, 0xD9141922);
            TurnboundFrameStyle.frame(graphics, x, y, cardW, cardH, accent);
            graphics.fill(x, y, x + cardW, y + 3, accent);
            graphics.text(font, Component.literal(stars(pull.stars())), x + 8, y + 10, accent, true);
            graphics.text(font, Component.literal(shorten(name(pull.characterId()), 17)), x + 8, y + 31, TEXT, true);
            String result = pull.newlyOwned() ? "NEW" : "Essence +" + pull.essence();
            graphics.text(font, Component.literal(result), x + 8, y + cardH - 28, pull.newlyOwned() ? GREEN : PURPLE, false);
            graphics.text(font, Component.literal("Pity " + pull.pityAfter()), x + 8, y + cardH - 14, SECONDARY, false);
        }
    }

    private static String name(String id) {
        try { return CanonicalData.definition(id).name(); }
        catch (RuntimeException ignored) { return id; }
    }

    private static String stars(int count) { return "★".repeat(Math.max(1, Math.min(5, count))); }
    private static int starColor(int stars) {
        return switch (stars) { case 5 -> GOLD; case 4 -> PURPLE; case 3 -> BLUE; case 2 -> GREEN; default -> SECONDARY; };
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
