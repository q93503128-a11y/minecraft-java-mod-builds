package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.world.GachaPresentationTimeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Full-screen 2D summon presentation. RNG and ownership remain server-authoritative. */
public final class GachaPresentationScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int BLUE = 0xFF6DC6FF;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFFFC857;
    private static final int PURPLE = 0xFFC794FF;
    private static final int BACKDROP = 0xFF080A0E;
    private static final int INTRO_TICKS = 44;
    private static final int SKIP_UNLOCK_TICKS = 10;

    public record Pull(
            String characterId, int stars, boolean newlyOwned, int essence, int pityAfter,
            int bonusLevelGranted, int bonusLevelAfter, boolean spotlight) {
        public Pull(String characterId, int stars, boolean newlyOwned, int essence, int pityAfter) {
            this(characterId, stars, newlyOwned, essence, pityAfter, 0, 0, false);
        }
    }

    /** Stage fields are retained only so old payloads decode safely; the 2D presentation never consumes them. */
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
    }

    private final Batch batch;
    private final List<Pull> revealPulls;
    private int ticks;
    private int lastAudioRevealIndex = -1;
    private GachaPresentationTimeline.Phase lastAudioPhase;
    private boolean finishing;
    private boolean summaryOnly;
    private boolean singleHold;
    private boolean presentationDone;
    private BattleHudButton skipButton;

    public GachaPresentationScreen(Batch batch) {
        super(Component.literal("정령의 기록"));
        this.batch = batch == null ? new Batch("", 0, List.of()) : batch;
        // Every committed pull is shown in order. Rarity affects pacing, never visibility.
        this.revealPulls = this.batch.pulls();
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
        skipButton = addRenderableWidget(new BattleHudButton(width - 118, 16, 100, 22,
                Component.literal("건너뛰기"), MUTED, ignored -> skipOrFinish()));
        skipButton.visible = ticks >= SKIP_UNLOCK_TICKS || summaryOnly || singleHold;
    }

    @Override
    public void tick() {
        super.tick();
        if (summaryOnly || singleHold) return;
        if (revealPulls.isEmpty()) {
            showSummary();
            return;
        }

        ticks++;
        if (skipButton != null) skipButton.visible = ticks >= SKIP_UNLOCK_TICKS;
        queuePhaseAudio();

        int end = totalSequenceTicks();
        if (ticks < end) return;

        if (batch.pulls().size() <= 1) {
            // A single pull stays on the completed reveal until the player confirms. It never auto-jumps to a result card.
            ticks = Math.max(INTRO_TICKS, end - 1);
            singleHold = true;
            if (skipButton != null) {
                skipButton.visible = true;
                skipButton.setMessage(Component.literal("확인"));
            }
        } else {
            // Multi-pulls may enter the summary only after every pull has actually traversed its reveal timeline.
            showSummary();
        }
    }

    private int revealTicks() {
        int total = 0;
        for (Pull pull : revealPulls) total += GachaPresentationTimeline.slotTicks(pull.stars(), pull.newlyOwned());
        return total;
    }

    private int totalSequenceTicks() {
        return INTRO_TICKS + revealTicks();
    }

    private void skipOrFinish() {
        if (singleHold || summaryOnly) {
            finish();
            return;
        }
        showSummary();
    }

    private void showSummary() {
        if (summaryOnly) return;
        summaryOnly = true;
        singleHold = false;
        if (skipButton != null) {
            skipButton.visible = true;
            skipButton.setMessage(Component.literal("닫기"));
        }
    }

    private void finish() {
        if (finishing) return;
        finishing = true;
        if (!presentationDone) {
            presentationDone = true;
            ClientPacketDistributor.sendToServer(new MetaCommandPayload("GACHA_DONE"));
        }
        if (minecraft != null) minecraft.gui.setScreen(new MetaMenuScreen(MetaMenuScreen.Tab.ARCHIVE));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            skipOrFinish();
            return true;
        }
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_SPACE)
                && (singleHold || summaryOnly)) {
            finish();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Fully opaque: no minimap, NPC prompt, world geometry or previous UI should bleed through the summon.
        graphics.fill(0, 0, width, height, BACKDROP);

        if (summaryOnly) {
            drawHeader(graphics, "소환 결과");
            if (batch.pulls().size() <= 1) drawSingleSummary(graphics);
            else drawMultiSummary(graphics);
        } else if (ticks < INTRO_TICKS) {
            drawPrelude(graphics);
        } else {
            Pull focus = currentReveal();
            if (focus != null) drawSpotlight(graphics, focus, currentPhase());
            else if (batch.pulls().size() <= 1) drawSingleSummary(graphics);
            else drawMultiSummary(graphics);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, String title) {
        graphics.text(font, Component.literal(title), 18, 18, TEXT, true);
        String spend = batch.pulls().size() + "회 · 크리스탈 -" + batch.crystalSpent();
        graphics.text(font, Component.literal(spend), 18, 34, SECONDARY, false);
    }

    private void drawPrelude(GuiGraphicsExtractor graphics) {
        int highest = highestStars();
        int accent = starColor(highest);
        int cx = width / 2;
        int cy = height / 2;
        int pulse = 34 + (int)Math.round(8.0 * Math.sin(ticks * 0.22));

        graphics.fill(0, height / 2 - 1, width, height / 2 + 1, withAlpha(accent, 48));
        graphics.fill(width / 2 - 1, 0, width / 2 + 1, height, withAlpha(accent, 32));
        for (int i = 0; i < 4; i++) {
            int r = pulse + i * 18;
            outline(graphics, cx - r, cy - r, r * 2, r * 2, withAlpha(accent, Math.max(42, 150 - i * 28)));
        }

        String stars = stars(highest);
        graphics.text(font, Component.literal(stars), cx - font.width(stars) / 2, cy - 12, accent, true);
        String cue = highest >= 5 ? "황금빛 공명이 정령의 기록을 가릅니다"
                : highest >= 4 ? "강한 공명이 기록에 응답합니다"
                : "정령의 기록이 열립니다";
        graphics.text(font, Component.literal(cue), cx - font.width(cue) / 2, cy + 12, TEXT, true);
        String count = batch.pulls().size() + "회 소환";
        graphics.text(font, Component.literal(count), cx - font.width(count) / 2, cy + 29, SECONDARY, false);
    }

    private Pull currentReveal() {
        if (revealPulls.isEmpty() || ticks < INTRO_TICKS) return null;
        int index = revealIndex();
        return index >= 0 && index < revealPulls.size() ? revealPulls.get(index) : null;
    }

    private int revealIndex() {
        int cursor = Math.max(0, ticks - INTRO_TICKS);
        for (int i = 0; i < revealPulls.size(); i++) {
            Pull pull = revealPulls.get(i);
            int duration = GachaPresentationTimeline.slotTicks(pull.stars(), pull.newlyOwned());
            if (cursor < duration) return i;
            cursor -= duration;
        }
        return revealPulls.size();
    }

    private int slotTick() {
        int cursor = Math.max(0, ticks - INTRO_TICKS);
        for (Pull pull : revealPulls) {
            int duration = GachaPresentationTimeline.slotTicks(pull.stars(), pull.newlyOwned());
            if (cursor < duration) return cursor;
            cursor -= duration;
        }
        return 0;
    }

    private GachaPresentationTimeline.Phase currentPhase() {
        Pull pull = currentReveal();
        if (pull == null) return GachaPresentationTimeline.Phase.COMPLETE;
        return GachaPresentationTimeline.phase(slotTick(), pull.stars(), pull.newlyOwned());
    }

    private void drawSpotlight(GuiGraphicsExtractor graphics, Pull pull, GachaPresentationTimeline.Phase phase) {
        int accent = starColor(pull.stars());
        int cx = width / 2;
        int panelW = Math.min(470, Math.max(310, width - 72));
        int panelH = Math.min(300, Math.max(226, height - 78));
        int x = (width - panelW) / 2;
        int y = Math.max(45, (height - panelH) / 2);

        graphics.fill(0, 0, 5, height, withAlpha(accent, 220));
        graphics.fill(width - 5, 0, width, height, withAlpha(accent, 220));
        TurnboundFrameStyle.frame(graphics, x, y, panelW, panelH, accent);

        String rarity = stars(pull.stars());
        graphics.text(font, Component.literal(rarity), x + 18, y + 15, accent, true);
        String count = (Math.min(revealIndex() + 1, revealPulls.size())) + " / " + revealPulls.size();
        graphics.text(font, Component.literal(count), x + panelW - 18 - font.width(count), y + 15, MUTED, false);

        if (phase == GachaPresentationTimeline.Phase.SIGNAL) {
            int visibleStars = Math.max(1, Math.min(pull.stars(), 1 + slotTick() / 4));
            String signal = "★".repeat(visibleStars);
            graphics.text(font, Component.literal(signal), cx - font.width(signal) / 2, y + panelH / 2 - 13, accent, true);
            String cue = pull.stars() >= 5 ? "최상위 공명 반응"
                    : pull.stars() >= 4 ? "강한 공명 반응"
                    : "공명 반응";
            graphics.text(font, Component.literal(cue), cx - font.width(cue) / 2, y + panelH / 2 + 9, SECONDARY, false);
            return;
        }

        int portraitSize = Math.min(190, panelH - 86);
        int portraitX = cx - portraitSize / 2;
        int portraitY = y + 37;
        if (phase == GachaPresentationTimeline.Phase.SILHOUETTE) {
            TurnboundPortraitRenderer.extractSilhouette(graphics, pull.characterId(),
                    portraitX, portraitY, portraitX + portraitSize, portraitY + portraitSize);
            String cue = pull.stars() >= 4 ? "강한 공명 속에서 형상이 드러납니다" : "형상이 드러납니다";
            graphics.text(font, Component.literal(cue), cx - font.width(cue) / 2,
                    y + panelH - 31, SECONDARY, false);
            return;
        }

        TurnboundPortraitRenderer.extractBust(graphics, pull.characterId(),
                portraitX, portraitY, portraitX + portraitSize, portraitY + portraitSize, false);

        if (phase == GachaPresentationTimeline.Phase.REVEAL) {
            String flash = stars(pull.stars());
            graphics.text(font, Component.literal(flash), cx - font.width(flash) / 2,
                    y + panelH - 29, accent, true);
            return;
        }

        String name = name(pull.characterId());
        int textY = y + panelH - 52;
        graphics.text(font, Component.literal(name), cx - font.width(name) / 2, textY, TEXT, true);
        String result = pull.newlyOwned() ? "새로운 동료" : duplicateResult(pull);
        graphics.text(font, Component.literal(result), cx - font.width(result) / 2,
                textY + 17, pull.newlyOwned() ? GREEN : PURPLE, false);
    }

    private void drawSingleSummary(GuiGraphicsExtractor graphics) {
        if (batch.pulls().isEmpty()) return;
        Pull pull = batch.pulls().getFirst();
        int w = Math.min(430, width - 48), h = Math.min(190, height - 84);
        int x = (width - w) / 2, y = Math.max(58, (height - h) / 2);
        int accent = starColor(pull.stars());
        TurnboundFrameStyle.frame(graphics, x, y, w, h, accent);
        int portrait = Math.min(132, h - 30);
        TurnboundPortraitRenderer.extractBust(graphics, pull.characterId(),
                x + 14, y + 15, x + 14 + portrait, y + 15 + portrait, false);
        int tx = x + portrait + 30;
        graphics.text(font, Component.literal("소환 결과"), tx, y + 24, SECONDARY, false);
        graphics.text(font, Component.literal(stars(pull.stars())), tx, y + 47, accent, true);
        graphics.text(font, Component.literal(name(pull.characterId())), tx, y + 70, TEXT, true);
        graphics.text(font, Component.literal(pull.newlyOwned() ? "새로운 동료" : duplicateResult(pull)),
                tx, y + 94, pull.newlyOwned() ? GREEN : PURPLE, false);
        graphics.text(font, Component.literal("확인하면 소환 기록으로 이동"), tx, y + 122, MUTED, false);
    }

    private void drawMultiSummary(GuiGraphicsExtractor graphics) {
        int count = batch.pulls().size();
        if (count <= 0) return;
        int cols = width >= 680 ? 5 : width >= 480 ? 4 : 2;
        int rows = (count + cols - 1) / cols;
        int gap = 7;
        int panelW = Math.min(width - 32, cols * 112 + (cols - 1) * gap + 24);
        int panelH = Math.min(height - 76, rows * 76 + (rows - 1) * gap + 42);
        int x = (width - panelW) / 2;
        int y = Math.max(52, (height - panelH) / 2);
        TurnboundFrameStyle.frame(graphics, x, y, panelW, panelH, starColor(highestStars()));
        graphics.text(font, Component.literal(count + "회 소환 결과"), x + 14, y + 13, TEXT, true);

        int cardW = Math.max(78, (panelW - 24 - (cols - 1) * gap) / cols);
        int cardH = Math.max(60, (panelH - 42 - (rows - 1) * gap) / rows);
        for (int i = 0; i < count; i++) {
            Pull pull = batch.pulls().get(i);
            int col = i % cols, row = i / cols;
            int cx = x + 12 + col * (cardW + gap);
            int cy = y + 34 + row * (cardH + gap);
            int accent = starColor(pull.stars());
            TurnboundUiSkin.inset(graphics, cx, cy, cardW, cardH);
            graphics.fill(cx, cy, cx + 3, cy + cardH, accent);
            int portrait = Math.min(42, cardH - 12);
            TurnboundPortraitRenderer.extract(graphics, pull.characterId(),
                    cx + 6, cy + 6, cx + 6 + portrait, cy + 6 + portrait, false);
            int tx = cx + portrait + 12;
            int textW = Math.max(24, cardW - portrait - 18);
            graphics.text(font, Component.literal(UiTextLayout.fit(name(pull.characterId()), textW)),
                    tx, cy + 8, TEXT, true);
            graphics.text(font, Component.literal(stars(pull.stars())), tx, cy + 23, accent, false);
            String status = pull.newlyOwned() ? "신규" : compactDuplicate(pull);
            graphics.text(font, Component.literal(UiTextLayout.fit(status, textW)), tx, cy + 38,
                    pull.newlyOwned() ? GREEN : PURPLE, false);
        }
    }

    private int highestStars() {
        int highest = 1;
        for (Pull pull : batch.pulls()) highest = Math.max(highest, pull.stars());
        return Math.max(1, Math.min(5, highest));
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

    private static String name(String id) {
        try { return CanonicalData.definition(id).name(); }
        catch (RuntimeException ignored) { return id == null ? "" : id; }
    }

    private static String stars(int stars) {
        int count = Math.max(1, Math.min(5, stars));
        return "★".repeat(count);
    }

    private static int starColor(int stars) {
        return switch (Math.max(1, Math.min(5, stars))) {
            case 5 -> GOLD;
            case 4 -> PURPLE;
            case 3 -> BLUE;
            case 2 -> GREEN;
            default -> SECONDARY;
        };
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static void outline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static String duplicateResult(Pull pull) {
        if (pull.bonusLevelGranted() > 0) return "+레벨 +" + pull.bonusLevelGranted() + " · 누적 +" + pull.bonusLevelAfter();
        if (pull.essence() > 0) return "별의 정수 +" + pull.essence();
        return "보유 중";
    }

    private static String compactDuplicate(Pull pull) {
        if (pull.bonusLevelGranted() > 0) return "+레벨";
        if (pull.essence() > 0) return "정수 +" + pull.essence();
        return "중복";
    }

    @Override
    public void onClose() {
        // Closing is an explicit player skip. No tick/state transition calls this path automatically.
        skipOrFinish();
    }

    @Override
    public void removed() {
        super.removed();
        // World actors/cameras are no longer part of summoning. Unexpected screen replacement must not silently
        // complete or skip the presentation; only finish() sends GACHA_DONE.
    }

    @Override public boolean isPauseScreen() { return false; }
}
