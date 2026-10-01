package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.progression.GachaCatalog;
import io.github.q93503128.turnbound.world.GachaPresentationTimeline;
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

    public record Pull(
            String characterId, int stars, boolean newlyOwned, int essence, int pityAfter,
            int bonusLevelGranted, int bonusLevelAfter, boolean spotlight) {
        public Pull(String characterId, int stars, boolean newlyOwned, int essence, int pityAfter) {
            this(characterId, stars, newlyOwned, essence, pityAfter, 0, 0, false);
        }
    }
    public record Batch(
            String action, int crystalSpent, List<Pull> pulls,
            double stageX, double stageY, double stageZ, float cameraYaw
    ) {
        public Batch {
            pulls = List.copyOf(pulls == null ? List.of() : pulls);
        }
        public Batch(String action, int crystalSpent, List<Pull> pulls) {
            this(action, crystalSpent, pulls, Double.NaN, Double.NaN, Double.NaN, Float.NaN);
        }
        boolean hasStage() {
            return Double.isFinite(stageX) && Double.isFinite(stageY) && Double.isFinite(stageZ) && Float.isFinite(cameraYaw);
        }
    }

    private final Batch batch;
    private final List<Pull> revealPulls;
    private int ticks;
    private int lastAudioRevealIndex = -1;
    private GachaPresentationTimeline.Phase lastAudioPhase;
    private boolean finishing;

    public GachaPresentationScreen(Batch batch) {
        super(Component.literal("정령의 기록"));
        this.batch = batch == null ? new Batch("", 0, List.of()) : batch;
        List<Pull> authored = this.batch.pulls().stream().filter(Pull::spotlight).toList();
        if (!authored.isEmpty()) {
            this.revealPulls = authored;
        } else if (this.batch.pulls().isEmpty()) {
            this.revealPulls = List.of();
        } else {
            // Backward-compatible fallback for an older server payload.
            List<Pull> highlights = this.batch.pulls().stream()
                    .filter(pull -> pull.newlyOwned() || pull.stars() >= 4)
                    .toList();
            if (!highlights.isEmpty()) this.revealPulls = highlights;
            else {
                Pull best = this.batch.pulls().getFirst();
                for (Pull pull : this.batch.pulls()) if (pull.stars() > best.stars()) best = pull;
                this.revealPulls = List.of(best);
            }
        }
    }

    public static Batch decode(String raw) {
        String action = "";
        int spent = 0;
        List<Pull> pulls = new ArrayList<>();
        double stageX = Double.NaN, stageY = Double.NaN, stageZ = Double.NaN;
        float cameraYaw = Float.NaN;
        if (raw == null) raw = "";
        for (String line : raw.split("\n")) {
            if (line.isBlank()) continue;
            String[] p = line.split("\\|", -1);
            try {
                switch (p[0]) {
                    case "H" -> {
                        action = p[1];
                        spent = Integer.parseInt(p[3]);
                        if (p.length > 7) {
                            stageX = Double.parseDouble(p[4]);
                            stageY = Double.parseDouble(p[5]);
                            stageZ = Double.parseDouble(p[6]);
                            cameraYaw = Float.parseFloat(p[7]);
                        }
                    }
                    case "P" -> pulls.add(new Pull(p[1], Integer.parseInt(p[2]), "1".equals(p[3]),
                            Integer.parseInt(p[4]), Integer.parseInt(p[5]),
                            p.length > 6 ? Integer.parseInt(p[6]) : 0,
                            p.length > 7 ? Integer.parseInt(p[7]) : 0,
                            p.length > 8 && "1".equals(p[8])));
                    default -> { }
                }
            } catch (RuntimeException ignored) { }
        }
        return new Batch(action, spent, pulls, stageX, stageY, stageZ, cameraYaw);
    }

    @Override
    protected void init() {
        super.init();
        if (batch.hasStage()) {
            SummonCameraController.enter(batch.stageX(), batch.stageY(), batch.stageZ(), batch.cameraYaw());
        }
        addRenderableWidget(new BattleHudButton(width - 98, 16, 80, 22,
                Component.literal("건너뛰기"), MUTED, ignored -> finish()));
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        Pull focus = currentReveal();
        if (focus != null && batch.hasStage()) {
            SummonCameraController.update(slotTick(), currentPhase(), focus.stars());
        } else if (SummonCameraController.active()) {
            SummonCameraController.exit();
        }
        queuePhaseAudio();
        if (ticks >= totalDurationTicks()) finish();
    }

    private int totalDurationTicks() {
        return GachaPresentationTimeline.totalTicks(revealPulls.size(), batch.pulls().size());
    }

    private void finish() {
        if (finishing) return;
        finishing = true;
        SummonCameraController.exit();
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
        if (focus != null) drawWorldRevealOverlay(graphics, focus, currentPhase());
        else if (batch.pulls().size() <= 1) drawSingleSummary(graphics);
        else drawTenSummary(graphics);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private Pull currentReveal() {
        if (revealPulls.isEmpty()) return null;
        int index = revealIndex();
        return index >= 0 && index < revealPulls.size() ? revealPulls.get(index) : null;
    }

    private int revealIndex() {
        return ticks / GachaPresentationTimeline.SLOT_TICKS;
    }

    private int slotTick() {
        return ticks % GachaPresentationTimeline.SLOT_TICKS;
    }

    private GachaPresentationTimeline.Phase currentPhase() {
        return GachaPresentationTimeline.phase(slotTick());
    }

    private void drawWorldRevealOverlay(
            GuiGraphicsExtractor graphics, Pull pull, GachaPresentationTimeline.Phase phase) {
        int accent = starColor(pull.stars());
        int intensity = GachaPresentationTimeline.intensity(pull.stars());
        drawRaritySignal(graphics, accent, intensity, phase);

        if (!batch.hasStage()) {
            int modelSize = Math.min(230, Math.max(132, Math.min(width / 3, height / 2)));
            int modelCx = width / 2;
            int modelTop = Math.max(28, height / 2 - modelSize / 2 - 30);
            if (phase == GachaPresentationTimeline.Phase.SILHOUETTE) {
                TurnboundPortraitRenderer.extractSilhouette(graphics, pull.characterId(),
                        modelCx - modelSize / 2, modelTop, modelCx + modelSize / 2, modelTop + modelSize);
                return;
            }
            if (phase == GachaPresentationTimeline.Phase.REVEAL || phase == GachaPresentationTimeline.Phase.NAME) {
                TurnboundPortraitRenderer.extractBust(graphics, pull.characterId(),
                        modelCx - modelSize / 2, modelTop, modelCx + modelSize / 2, modelTop + modelSize, false);
            }
        }

        if (phase == GachaPresentationTimeline.Phase.SILHOUETTE) {
            String cue = "형상이 소환장에 응집됩니다";
            graphics.text(font, Component.literal(cue),
                    (width - font.width(cue)) / 2, Math.max(50, height - 52), SECONDARY, false);
            return;
        }

        // The real in-world BattleActorEntity is the reveal. Do not cover it with a flat portrait.
        if (phase != GachaPresentationTimeline.Phase.NAME) return;

        int w = Math.min(410, width - 44);
        int h = 76;
        int x = (width - w) / 2;
        int y = Math.max(62, height - h - 28);
        TurnboundFrameStyle.frame(graphics, x, y, w, h, accent);
        graphics.text(font, Component.literal(stars(pull.stars())), x + 18, y + 15, accent, true);
        graphics.text(font, Component.literal(name(pull.characterId())), x + 18, y + 35, TEXT, true);
        String result = pull.newlyOwned() ? "새로운 동료" : duplicateResult(pull);
        graphics.text(font, Component.literal(result), x + 18, y + 54, pull.newlyOwned() ? GREEN : PURPLE, false);
        if (pull.stars() >= 5) graphics.text(font, Component.literal("★5"), x + w - 48, y + 15, GOLD, true);
    }

    private void drawRaritySignal(
            GuiGraphicsExtractor graphics, int accent, int intensity, GachaPresentationTimeline.Phase phase) {
        int thickness = 2 + Math.max(0, intensity);
        int alpha = phase == GachaPresentationTimeline.Phase.SIGNAL ? 0xAA : 0x76;
        int signal = withAlpha(accent, alpha);
        graphics.fill(0, 0, width, thickness, signal);
        graphics.fill(0, height - thickness, width, height, signal);

        int inset = 12 + intensity * 7;
        graphics.fill(inset, 10 + intensity * 2, Math.max(inset + 1, width - inset), 12 + intensity * 2, signal);
        if (phase == GachaPresentationTimeline.Phase.SIGNAL) {
            String cue = intensity >= 4 ? "강한 공명이 느껴집니다"
                    : intensity >= 3 ? "선명한 공명이 이어집니다"
                    : "정령의 기록이 반응합니다";
            int tx = (width - font.width(cue)) / 2;
            graphics.text(font, Component.literal(cue), tx, Math.max(46, height / 2 - 10), accent, true);
        }
    }

    private void queuePhaseAudio() {
        Pull pull = currentReveal();
        if (pull == null) return;
        int index = revealIndex();
        GachaPresentationTimeline.Phase phase = currentPhase();
        if (index == lastAudioRevealIndex && phase == lastAudioPhase) return;
        lastAudioRevealIndex = index;
        lastAudioPhase = phase;

        int priority = pull.stars() >= 5 ? 3 : pull.stars() >= 4 ? 2 : 1;
        if (phase == GachaPresentationTimeline.Phase.SIGNAL) {
            ClientAudioDirector.acceptBatch("spawn|SYSTEM|" + priority + "||||" + pull.stars());
        } else if (phase == GachaPresentationTimeline.Phase.REVEAL) {
            ClientAudioDirector.acceptBatch(revealCue(pull) + "|SKILL|" + priority
                    + "|" + pull.characterId() + "||summon|" + pull.stars());
        }
    }

    private static String revealCue(Pull pull) {
        return switch (pull.characterId()) {
            case "P01" -> "hero_kyren";
            case "P02" -> "hero_lumea";
            case "P03" -> "hero_bram";
            case "P04" -> "hero_elysia";
            case "P05" -> "hero_lynette";
            case "P06" -> "hero_morwen";
            case "P07" -> "hero_marion";
            case "P08" -> "hero_raze";
            default -> "skill";
        };
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
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
        graphics.text(font, Component.literal(pull.newlyOwned() ? "새로운 동료" : duplicateResult(pull)),
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
        int summaryTicks = Math.max(0, ticks - Math.max(1, revealPulls.size()) * GachaPresentationTimeline.SLOT_TICKS);
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
            String result = pull.newlyOwned() ? "신규" : compactDuplicateResult(pull);
            graphics.text(font, Component.literal(result), x + 7, y + cardH - 18,
                    pull.newlyOwned() ? GREEN : PURPLE, false);
            graphics.text(font, Component.literal("천장 " + pull.pityAfter()), x + cardW - font.width("천장 " + pull.pityAfter()) - 6,
                    y + cardH - 18, SECONDARY, false);
        }
    }

    private static String duplicateResult(Pull pull) {
        String level = pull.bonusLevelGranted() > 0
                ? " · +레벨 +1 (+" + pull.bonusLevelAfter() + ")"
                : " · +레벨 MAX";
        return "별의 정수 +" + pull.essence() + level;
    }

    private static String compactDuplicateResult(Pull pull) {
        if (pull.bonusLevelGranted() > 0) return "정수 +" + pull.essence() + " · +" + pull.bonusLevelAfter();
        return "정수 +" + pull.essence() + " · MAX";
    }

    private static String name(String id) {
        try { return CanonicalData.definition(id).name(); }
        catch (RuntimeException ignored) { return "알 수 없는 동료"; }
    }

    private static String stars(int count) { return "★".repeat(Math.max(1, Math.min(5, count))); }
    private static int starColor(int stars) {
        return switch (stars) {
            case 5 -> GOLD;
            case 4 -> PURPLE;
            case 3 -> BLUE;
            case 2 -> GREEN;
            default -> SECONDARY;
        };
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

    @Override
    public void removed() {
        SummonCameraController.exit();
        super.removed();
    }
}
