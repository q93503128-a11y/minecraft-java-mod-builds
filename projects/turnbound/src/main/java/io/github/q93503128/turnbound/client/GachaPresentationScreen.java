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
    private static final int INTRO_TICKS = 34;
    private static final int SKIP_UNLOCK_TICKS = 8;

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
    private int introTicks;
    private int activeRevealIndex;
    private int revealTick;
    private int lastAudioRevealIndex = -1;
    private GachaPresentationTimeline.Phase lastAudioPhase;
    private boolean waitingForAdvance;
    private boolean finishing;
    private boolean summaryOnly;
    private boolean presentationDone;
    private BattleHudButton skipButton;
    private BattleHudButton nextButton;

    public GachaPresentationScreen(Batch batch) {
        super(Component.literal("정령의 기록"));
        this.batch = batch == null ? new Batch("", 0, List.of()) : batch;
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
        skipButton = addRenderableWidget(new BattleHudButton(width - 116, 14, 98, 22,
                Component.literal("건너뛰기"), MUTED, ignored -> skipOrFinish()));
        nextButton = addRenderableWidget(new BattleHudButton(width - 122, height - 36, 104, 22,
                Component.literal("다음"), GOLD, ignored -> advanceReveal()));
        updateControls();
    }

    @Override
    public void tick() {
        super.tick();
        if (summaryOnly || waitingForAdvance) return;
        if (revealPulls.isEmpty()) {
            showSummary();
            return;
        }

        if (introTicks < INTRO_TICKS) {
            introTicks++;
            updateControls();
            return;
        }

        Pull pull = currentReveal();
        if (pull == null) {
            showSummary();
            return;
        }

        revealTick++;
        queuePhaseAudio();

        // Each character reveal holds on the readable result until the player explicitly advances it.
        int holdTick = GachaPresentationTimeline.nameTick(pull.stars(), pull.newlyOwned());
        if (revealTick >= holdTick) {
            revealTick = holdTick;
            waitingForAdvance = true;
            updateControls();
        }
    }

    private void updateControls() {
        if (skipButton != null) {
            skipButton.visible = summaryOnly || introTicks >= SKIP_UNLOCK_TICKS;
            skipButton.setMessage(Component.literal(summaryOnly ? "닫기" : "건너뛰기"));
        }
        if (nextButton != null) {
            nextButton.visible = waitingForAdvance && !summaryOnly;
            if (waitingForAdvance) {
                boolean last = activeRevealIndex >= revealPulls.size() - 1;
                if (last && revealPulls.size() <= 1) nextButton.setMessage(Component.literal("확인"));
                else if (last) nextButton.setMessage(Component.literal("결과 보기"));
                else nextButton.setMessage(Component.literal("다음"));
            }
        }
    }

    private void advanceReveal() {
        if (!waitingForAdvance || summaryOnly) return;
        if (activeRevealIndex >= revealPulls.size() - 1) {
            if (revealPulls.size() <= 1) finish();
            else showSummary();
            return;
        }

        activeRevealIndex++;
        revealTick = 0;
        waitingForAdvance = false;
        lastAudioPhase = null;
        updateControls();
    }

    private void skipOrFinish() {
        if (summaryOnly) {
            finish();
            return;
        }
        showSummary();
    }

    private void showSummary() {
        if (summaryOnly) return;
        summaryOnly = true;
        waitingForAdvance = false;
        updateControls();
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
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_SPACE) {
            if (summaryOnly) {
                finish();
                return true;
            }
            if (waitingForAdvance) {
                advanceReveal();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKDROP);

        if (summaryOnly) {
            drawHeader(graphics, "소환 결과");
            if (batch.pulls().size() <= 1) drawSingleSummary(graphics);
            else drawMultiSummary(graphics);
        } else if (introTicks < INTRO_TICKS) {
            drawHeader(graphics, "정령의 기록");
            drawPrelude(graphics);
        } else {
            drawHeader(graphics, "정령의 기록");
            Pull focus = currentReveal();
            if (focus != null) drawSpotlight(graphics, focus, currentPhase());
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, String title) {
        graphics.text(font, Component.literal(title), 18, 17, TEXT, true);
        String spend = batch.pulls().size() + "회 · 크리스탈 -" + batch.crystalSpent();
        graphics.text(font, Component.literal(spend), 18, 33, SECONDARY, false);
    }

    private void drawPrelude(GuiGraphicsExtractor graphics) {
        int highest = highestStars();
        int accent = starColor(highest);
        int orb = Math.min(210, Math.max(132, Math.min(width, height) - 250));
        int cx = width / 2;
        int cy = height / 2 + 4;
        int ox = cx - orb / 2;
        int oy = cy - orb / 2;

        TurnboundUiSkin.orbBase(graphics, ox, oy, orb);
        TurnboundUiSkin.orbOverlay(graphics, ox, oy, orb, true, false, highest >= 4);

        String rarity = stars(highest);
        graphics.text(font, Component.literal(rarity), cx - font.width(rarity) / 2, cy - 15, accent, true);

        String cue = highest >= 5 ? "강렬한 공명이 기록을 깨웁니다"
                : highest >= 4 ? "선명한 공명이 기록에 응답합니다"
                : "정령의 기록이 열립니다";
        graphics.text(font, Component.literal(cue), cx - font.width(cue) / 2, cy + 7, TEXT, true);
        String count = batch.pulls().size() + "회 소환";
        graphics.text(font, Component.literal(count), cx - font.width(count) / 2, cy + 25, SECONDARY, false);
    }

    private Pull currentReveal() {
        if (revealPulls.isEmpty() || activeRevealIndex < 0 || activeRevealIndex >= revealPulls.size()) return null;
        return revealPulls.get(activeRevealIndex);
    }

    private GachaPresentationTimeline.Phase currentPhase() {
        Pull pull = currentReveal();
        if (pull == null) return GachaPresentationTimeline.Phase.COMPLETE;
        return GachaPresentationTimeline.phase(revealTick, pull.stars(), pull.newlyOwned());
    }

    private void drawSpotlight(GuiGraphicsExtractor graphics, Pull pull, GachaPresentationTimeline.Phase phase) {
        int accent = starColor(pull.stars());
        int layoutW = Math.min(650, Math.max(240, width - 32));
        int layoutH = Math.min(286, Math.max(168, height - 92));
        int x = (width - layoutW) / 2;
        int y = Math.max(48, (height - layoutH) / 2 + 4);
        if (y + layoutH > height - 16) y = Math.max(44, height - 16 - layoutH);
        int gap = layoutW < 360 ? 7 : 12;
        int figureW = Math.min(244, Math.max(104, layoutW * 39 / 100));
        int infoX = x + figureW + gap;
        int infoW = layoutW - figureW - gap;

        TurnboundFrameStyle.frame(graphics, x, y, layoutW, layoutH, accent);

        String progress = (activeRevealIndex + 1) + " / " + revealPulls.size();
        graphics.text(font, Component.literal(progress), x + layoutW - 18 - font.width(progress), y + 15, MUTED, false);

        int orb = Math.max(72, Math.min(figureW - 16, layoutH - 38));
        int orbX = x + (figureW - orb) / 2;
        int orbY = y + 24 + Math.max(0, (layoutH - 34 - orb) / 2);
        TurnboundUiSkin.orbBase(graphics, orbX, orbY, orb);
        TurnboundUiSkin.orbOverlay(graphics, orbX, orbY, orb, true, false,
                phase == GachaPresentationTimeline.Phase.REVEAL || phase == GachaPresentationTimeline.Phase.NAME);

        int modelW = Math.min(124, Math.max(72, figureW - 72));
        int modelH = Math.min(188, Math.max(96, layoutH - 70));
        int modelX = x + figureW / 2 - modelW / 2;
        int modelY = y + layoutH / 2 - modelH / 2 + 8;

        if (phase == GachaPresentationTimeline.Phase.REVEAL || phase == GachaPresentationTimeline.Phase.NAME) {
            TurnboundPortraitRenderer.extract(graphics, pull.characterId(),
                    modelX, modelY, modelX + modelW, modelY + modelH, false);
        } else {
            String signal = phase == GachaPresentationTimeline.Phase.SIGNAL
                    ? stars(Math.max(1, Math.min(pull.stars(), 1 + revealTick / 4)))
                    : stars(pull.stars());
            graphics.text(font, Component.literal(signal),
                    x + figureW / 2 - font.width(signal) / 2,
                    y + layoutH / 2 - 6, accent, true);
        }

        int infoY = y + 36;
        int infoH = layoutH - 52;
        TurnboundUiSkin.inset(graphics, infoX, infoY, infoW, infoH);
        graphics.fill(infoX + 7, infoY + 7, infoX + 10, infoY + infoH - 7, accent);

        int tx = infoX + 20;
        int ty = infoY + 16;
        String rarity = stars(pull.stars());
        graphics.text(font, Component.literal(rarity), tx, ty, accent, true);

        if (phase == GachaPresentationTimeline.Phase.SIGNAL) {
            graphics.text(font, Component.literal("공명 감지"), tx, ty + 28, TEXT, true);
            graphics.text(font, Component.literal(rarityCue(pull.stars())), tx, ty + 48, SECONDARY, false);
        } else if (phase == GachaPresentationTimeline.Phase.SILHOUETTE) {
            graphics.text(font, Component.literal("형상이 응답합니다"), tx, ty + 28, TEXT, true);
            graphics.text(font, Component.literal("기록을 해독하는 중"), tx, ty + 48, SECONDARY, false);
        } else if (phase == GachaPresentationTimeline.Phase.REVEAL) {
            graphics.text(font, Component.literal("기록 해방"), tx, ty + 28, TEXT, true);
            graphics.text(font, Component.literal("이름이 드러납니다"), tx, ty + 48, SECONDARY, false);
        } else {
            String name = name(pull.characterId());
            graphics.text(font, Component.literal(name), tx, ty + 28, TEXT, true);
            String result = pull.newlyOwned() ? "신규 획득" : duplicateResult(pull);
            graphics.text(font, Component.literal(result), tx, ty + 50,
                    pull.newlyOwned() ? GREEN : PURPLE, false);
            if (waitingForAdvance) {
                String hint = activeRevealIndex >= revealPulls.size() - 1
                        ? (revealPulls.size() <= 1 ? "확인하여 기록으로 돌아갑니다" : "결과 목록을 확인합니다")
                        : "Enter / Space 또는 다음";
                graphics.text(font, Component.literal(hint), tx, infoY + infoH - 24, MUTED, false);
            }
        }
    }

    private void drawSingleSummary(GuiGraphicsExtractor graphics) {
        if (batch.pulls().isEmpty()) return;
        Pull pull = batch.pulls().getFirst();
        int w = Math.min(430, width - 48), h = Math.min(190, height - 84);
        int x = (width - w) / 2, y = Math.max(58, (height - h) / 2);
        int accent = starColor(pull.stars());
        TurnboundFrameStyle.frame(graphics, x, y, w, h, accent);
        int portrait = Math.min(112, h - 40);
        TurnboundPortraitRenderer.extract(graphics, pull.characterId(),
                x + 18, y + 20, x + 18 + portrait, y + 20 + portrait, false);
        int tx = x + portrait + 38;
        graphics.text(font, Component.literal("소환 결과"), tx, y + 24, SECONDARY, false);
        graphics.text(font, Component.literal(stars(pull.stars())), tx, y + 47, accent, true);
        graphics.text(font, Component.literal(name(pull.characterId())), tx, y + 70, TEXT, true);
        graphics.text(font, Component.literal(pull.newlyOwned() ? "신규 획득" : duplicateResult(pull)),
                tx, y + 94, pull.newlyOwned() ? GREEN : PURPLE, false);
        graphics.text(font, Component.literal("Enter / Space · 닫기"), tx, y + 122, MUTED, false);
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
            graphics.fill(cx + 5, cy + 5, cx + 8, cy + cardH - 5, accent);
            int portrait = Math.min(40, cardH - 14);
            TurnboundPortraitRenderer.extract(graphics, pull.characterId(),
                    cx + 10, cy + 7, cx + 10 + portrait, cy + 7 + portrait, false);
            int tx = cx + portrait + 16;
            int textW = Math.max(24, cardW - portrait - 22);
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
        int index = activeRevealIndex;
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

    private static String rarityCue(int stars) {
        return stars >= 5 ? "최상위 공명"
                : stars >= 4 ? "강한 공명"
                : stars >= 3 ? "안정된 공명"
                : "희미한 공명";
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
        skipOrFinish();
    }

    @Override
    public void removed() {
        super.removed();
        // Only finish() sends GACHA_DONE. Unexpected screen replacement must not silently consume the presentation.
    }

    @Override public boolean isPauseScreen() { return false; }
}
