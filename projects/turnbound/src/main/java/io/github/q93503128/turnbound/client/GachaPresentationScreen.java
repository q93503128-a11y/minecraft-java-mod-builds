package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.world.GachaPresentationFlow;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen summon presentation for already committed server results.
 *
 * <p>The cinematic has one batch omen, then one silhouette/reveal/HOLD cycle per pull. HOLD is deliberately
 * player-paced: no tick path can select the next pull. Skip is explicit and goes to the committed summary rather
 * than consuming or rerolling anything.</p>
 */
public final class GachaPresentationScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int BLUE = 0xFF6DC6FF;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFFFC857;
    private static final int PURPLE = 0xFFC794FF;
    private static final int SILVER = 0xFFD7DEE8;
    private static final int BACKDROP = 0xFF080A0E;

    public record Pull(
            String characterId, int stars, boolean newlyOwned, int essence, int pityAfter,
            int bonusLevelGranted, int bonusLevelAfter, boolean spotlight) {
        public Pull(String characterId, int stars, boolean newlyOwned, int essence, int pityAfter) {
            this(characterId, stars, newlyOwned, essence, pityAfter, 0, 0, false);
        }
    }

    /** Stage coordinates remain only for backward-compatible payload decoding; 2D summon never consumes them. */
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
    private final GachaPresentationFlow flow;
    private boolean finishing;
    private boolean presentationDone;
    private boolean audioStarted;
    private boolean fiveStarUpgradeCuePlayed;
    private GachaPresentationFlow.Stage lastAudioStage;
    private int lastAudioIndex = -1;
    private BattleHudButton skipButton;
    private BattleHudButton primaryButton;

    public GachaPresentationScreen(Batch batch) {
        super(Component.literal("정령의 기록"));
        this.batch = batch == null ? new Batch("", 0, List.of()) : batch;
        this.flow = new GachaPresentationFlow(this.batch.pulls().stream()
                .map(pull -> new GachaPresentationFlow.Pull(pull.stars(), pull.newlyOwned()))
                .toList());
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
        skipButton = addRenderableWidget(new BattleHudButton(width - 112, 14, 94, 22,
                Component.literal("건너뛰기"), MUTED, ignored -> skipOrFinish()));
        primaryButton = addRenderableWidget(new BattleHudButton(width - 128, height - 38, 110, 22,
                Component.literal("다음"), GOLD, ignored -> primaryAction()));
        updateControls();
    }

    @Override
    public void tick() {
        super.tick();
        if (!audioStarted) {
            audioStarted = true;
            playStageEntry(flow.stage());
        }

        GachaPresentationFlow.Stage beforeStage = flow.stage();
        int beforeIndex = flow.currentIndex();
        boolean beforeUpgrade = flow.fiveStarUpgrade();
        flow.tick();

        if (flow.stage() != beforeStage || flow.currentIndex() != beforeIndex) {
            playStageEntry(flow.stage());
        }
        if (!beforeUpgrade && flow.fiveStarUpgrade() && !fiveStarUpgradeCuePlayed) {
            fiveStarUpgradeCuePlayed = true;
            ClientAudioDirector.acceptBatch("spawn|SYSTEM|3||||5");
        }
        updateControls();
    }

    private void primaryAction() {
        if (flow.holding()) {
            GachaPresentationFlow.Stage before = flow.stage();
            int beforeIndex = flow.currentIndex();
            flow.advance();
            if (flow.stage() != before || flow.currentIndex() != beforeIndex) playStageEntry(flow.stage());
            updateControls();
        } else if (flow.summary()) {
            finish();
        }
    }

    private void skipOrFinish() {
        if (flow.summary()) {
            finish();
            return;
        }
        flow.skip();
        updateControls();
    }

    private void updateControls() {
        if (skipButton != null) {
            skipButton.visible = true;
            skipButton.setMessage(Component.literal(flow.summary() ? "닫기" : "건너뛰기"));
        }
        if (primaryButton != null) {
            primaryButton.visible = flow.holding();
            if (flow.holding()) {
                primaryButton.setMessage(Component.literal(flow.lastPull()
                        ? (flow.pullCount() <= 1 ? "확인" : "결과 보기")
                        : "다음"));
            }
        }
    }

    private void finish() {
        if (finishing) return;
        finishing = true;
        if (!presentationDone) {
            presentationDone = true;
            ClientPacketDistributor.sendToServer(new MetaCommandPayload("GACHA_DONE"));
        }
        if (minecraft != null) minecraft.gui.setScreen(new FacilitySummonScreen());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            skipOrFinish();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_SPACE) {
            if (flow.holding() || flow.summary()) {
                primaryAction();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKDROP);
        drawCinematicHeader(graphics);

        switch (flow.stage()) {
            case RITUAL -> drawRitual(graphics);
            case OMEN -> drawOmen(graphics);
            case BREAK -> drawBreak(graphics);
            case SILHOUETTE -> drawSilhouette(graphics);
            case REVEAL -> drawReveal(graphics);
            case HOLD -> drawHold(graphics);
            case SUMMARY -> drawSummary(graphics);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCinematicHeader(GuiGraphicsExtractor graphics) {
        if (flow.summary()) return;
        graphics.text(font, Component.literal("정령의 기록"), 18, 17, TEXT, true);
        if (flow.stage().ordinal() >= GachaPresentationFlow.Stage.SILHOUETTE.ordinal()) {
            String progress = (flow.currentIndex() + 1) + " / " + Math.max(1, flow.pullCount());
            graphics.text(font, Component.literal(progress), width - 126 - font.width(progress), 20, MUTED, false);
        } else {
            String count = flow.pullCount() + "회 소환";
            graphics.text(font, Component.literal(count), 18, 33, SECONDARY, false);
        }
    }

    private void drawRitual(GuiGraphicsExtractor g) {
        int cx = width / 2;
        int cy = height / 2 + 2;
        int orb = ritualOrbSize(142);
        int x = cx - orb / 2;
        int y = cy - orb / 2;
        int travel = Math.max(20, Math.min(92, (int) (flow.progress() * 92)));

        g.fill(Math.max(0, cx - travel - 84), cy - 1, cx - 38, cy + 1, withAlpha(PURPLE, 96));
        g.fill(cx + 38, cy - 1, Math.min(width, cx + travel + 84), cy + 1, withAlpha(PURPLE, 96));
        TurnboundUiSkin.orbBase(g, x, y, orb);
        TurnboundUiSkin.orbOverlay(g, x, y, orb, true, false, false);
        centerText(g, "공명이 모입니다", cy + orb / 2 + 16, SECONDARY, false);
    }

    private void drawOmen(GuiGraphicsExtractor g) {
        int omenStars = flow.omenStars();
        int accent = starColor(omenStars);
        int cx = width / 2;
        int cy = height / 2 + 2;
        int orb = ritualOrbSize(156);
        int x = cx - orb / 2;
        int y = cy - orb / 2;

        rarityEdges(g, accent, 2 + (int) (flow.progress() * 3));
        TurnboundUiSkin.orbBase(g, x, y, orb);
        TurnboundUiSkin.orbOverlay(g, x, y, orb, true, false, omenStars >= 4);
        centerText(g, stars(omenStars), cy - 8, accent, true);
        centerText(g, omenStars >= 4 ? "강한 공명이 접근합니다" : "기록이 응답합니다",
                cy + 16, TEXT, true);
    }

    private void drawBreak(GuiGraphicsExtractor g) {
        int displayStars = flow.breakDisplayStars();
        int accent = starColor(displayStars);
        int cx = width / 2;
        int cy = height / 2;
        rarityEdges(g, accent, 5);
        int reach = Math.max(36, (int) (Math.min(width, height) * (0.18F + flow.progress() * 0.34F)));
        g.fill(cx - reach, cy - 2, cx + reach, cy + 2, withAlpha(accent, 180));
        g.fill(cx - 2, cy - reach / 2, cx + 2, cy + reach / 2, withAlpha(accent, 150));

        if (flow.highestStars() >= 5 && flow.fiveStarUpgrade()) {
            int local = Math.max(0, flow.stageTick() - 5);
            if (local <= 2) g.fill(0, 0, width, height, 0x82FFF6D6);
            centerText(g, "★★★★★", cy - 10, GOLD, true);
            centerText(g, "공명이 한계를 넘습니다", cy + 14, TEXT, true);
        } else {
            centerText(g, stars(displayStars), cy - 10, accent, true);
            centerText(g, "기록의 봉인이 열립니다", cy + 14, SECONDARY, false);
        }
    }

    private void drawSilhouette(GuiGraphicsExtractor g) {
        Pull pull = currentPull();
        if (pull == null) return;
        int accent = starColor(pull.stars());
        CharacterLayout layout = characterLayout();
        drawCharacterBackdrop(g, layout, accent, false);
        TurnboundPortraitRenderer.extractSilhouette(g, pull.characterId(),
                layout.figureX(), layout.figureY(), layout.figureX() + layout.figureW(), layout.figureY() + layout.figureH());
        drawCharacterSideText(g, layout, stars(pull.stars()), accent,
                "형상이 응답합니다", "기록을 해독하는 중", false);
    }

    private void drawReveal(GuiGraphicsExtractor g) {
        Pull pull = currentPull();
        if (pull == null) return;
        int accent = starColor(pull.stars());
        CharacterLayout layout = characterLayout();
        drawCharacterBackdrop(g, layout, accent, true);
        TurnboundPortraitRenderer.extract(g, pull.characterId(),
                layout.figureX(), layout.figureY(), layout.figureX() + layout.figureW(), layout.figureY() + layout.figureH(), false);
        drawCharacterSideText(g, layout, stars(pull.stars()), accent,
                "기록 해방", pull.stars() >= 5 ? "최상위 공명이 모습을 드러냅니다" : "공명이 모습을 드러냅니다", false);
    }

    private void drawHold(GuiGraphicsExtractor g) {
        Pull pull = currentPull();
        if (pull == null) return;
        int accent = starColor(pull.stars());
        CharacterLayout layout = characterLayout();
        drawCharacterBackdrop(g, layout, accent, true);
        TurnboundPortraitRenderer.extract(g, pull.characterId(),
                layout.figureX(), layout.figureY(), layout.figureX() + layout.figureW(), layout.figureY() + layout.figureH(), false);

        int tx = layout.infoX();
        int ty = layout.infoY();
        String rarity = stars(pull.stars());
        g.text(font, Component.literal(rarity), tx, ty, accent, true);
        g.fill(tx, ty + 18, Math.min(width - 20, tx + Math.max(52, layout.infoW() - 8)), ty + 20, withAlpha(accent, 170));
        g.text(font, Component.literal(name(pull.characterId())), tx, ty + 31, TEXT, true);
        String result = pull.newlyOwned() ? "새로운 동료" : duplicateResult(pull);
        g.text(font, Component.literal(result), tx, ty + 52, pull.newlyOwned() ? GREEN : PURPLE, false);
        String hint = flow.lastPull()
                ? (flow.pullCount() <= 1 ? "Enter / Space · 확인" : "Enter / Space · 결과 보기")
                : "Enter / Space · 다음";
        g.text(font, Component.literal(UiTextLayout.fit(hint, Math.max(54, layout.infoW()))),
                tx, Math.min(height - 54, ty + 82), MUTED, false);
    }

    private void drawSummary(GuiGraphicsExtractor g) {
        drawSummaryHeader(g);
        if (batch.pulls().size() <= 1) drawSingleSummary(g);
        else drawMultiSummary(g);
    }

    private void drawSummaryHeader(GuiGraphicsExtractor g) {
        g.text(font, Component.literal("소환 결과"), 18, 17, TEXT, true);
        String spend = batch.pulls().size() + "회 · 크리스탈 -" + batch.crystalSpent();
        g.text(font, Component.literal(spend), 18, 33, SECONDARY, false);
    }

    private void drawSingleSummary(GuiGraphicsExtractor g) {
        if (batch.pulls().isEmpty()) return;
        Pull pull = batch.pulls().getFirst();
        int accent = starColor(pull.stars());
        int w = Math.min(420, width - 36);
        int h = Math.min(178, height - 78);
        int x = (width - w) / 2;
        int y = Math.max(52, (height - h) / 2 + 6);
        TurnboundFrameStyle.frame(g, x, y, w, h, accent);
        int portrait = Math.min(108, h - 34);
        TurnboundPortraitRenderer.extract(g, pull.characterId(),
                x + 16, y + 17, x + 16 + portrait, y + 17 + portrait, false);
        int tx = x + portrait + 34;
        g.text(font, Component.literal(stars(pull.stars())), tx, y + 31, accent, true);
        g.text(font, Component.literal(name(pull.characterId())), tx, y + 55, TEXT, true);
        g.text(font, Component.literal(pull.newlyOwned() ? "새로운 동료" : duplicateResult(pull)),
                tx, y + 78, pull.newlyOwned() ? GREEN : PURPLE, false);
        g.text(font, Component.literal("Enter / Space · 닫기"), tx, y + 112, MUTED, false);
    }

    private void drawMultiSummary(GuiGraphicsExtractor g) {
        int count = batch.pulls().size();
        if (count <= 0) return;
        int cols = count >= 8 ? 5 : Math.min(count, width >= 420 ? 5 : 4);
        cols = Math.max(1, cols);
        int rows = (count + cols - 1) / cols;
        int gap = 5;
        int panelW = Math.min(width - 24, 560);
        int availableH = Math.max(120, height - 66);
        int panelH = Math.min(availableH, rows * 78 + (rows - 1) * gap + 38);
        int x = (width - panelW) / 2;
        int y = Math.max(46, (height - panelH) / 2 + 7);
        TurnboundFrameStyle.frame(g, x, y, panelW, panelH, starColor(highestStars()));

        int cardW = Math.max(42, (panelW - 22 - (cols - 1) * gap) / cols);
        int cardH = Math.max(54, (panelH - 36 - (rows - 1) * gap) / rows);
        for (int i = 0; i < count; i++) {
            Pull pull = batch.pulls().get(i);
            int col = i % cols;
            int row = i / cols;
            int cx = x + 11 + col * (cardW + gap);
            int cy = y + 27 + row * (cardH + gap);
            int accent = starColor(pull.stars());
            TurnboundUiSkin.inset(g, cx, cy, cardW, cardH);
            g.fill(cx + 4, cy + 5, cx + 7, cy + cardH - 5, accent);

            int portrait = Math.min(34, Math.min(cardW - 14, cardH - 36));
            int portraitX = cx + (cardW - portrait) / 2;
            TurnboundPortraitRenderer.extract(g, pull.characterId(),
                    portraitX, cy + 5, portraitX + portrait, cy + 5 + portrait, false);
            int textW = Math.max(24, cardW - 10);
            int nameY = cy + Math.max(31, portrait + 7);
            String characterName = UiTextLayout.fit(name(pull.characterId()), textW);
            g.text(font, Component.literal(characterName), cx + (cardW - font.width(characterName)) / 2,
                    nameY, TEXT, true);
            String rarity = stars(pull.stars());
            g.text(font, Component.literal(rarity), cx + (cardW - font.width(rarity)) / 2,
                    nameY + 12, accent, false);
            String status = pull.newlyOwned() ? "신규" : compactDuplicate(pull);
            status = UiTextLayout.fit(status, textW);
            if (nameY + 26 < cy + cardH - 2) {
                g.text(font, Component.literal(status), cx + (cardW - font.width(status)) / 2,
                        nameY + 24, pull.newlyOwned() ? GREEN : PURPLE, false);
            }
        }
    }

    private void drawCharacterSideText(GuiGraphicsExtractor g, CharacterLayout layout, String rarity, int accent,
                                       String title, String subtitle, boolean showName) {
        int tx = layout.infoX();
        int ty = layout.infoY();
        g.text(font, Component.literal(rarity), tx, ty, accent, true);
        g.fill(tx, ty + 18, Math.min(width - 20, tx + Math.max(48, layout.infoW() - 8)), ty + 20,
                withAlpha(accent, 150));
        g.text(font, Component.literal(title), tx, ty + 31, TEXT, true);
        g.text(font, Component.literal(UiTextLayout.fit(subtitle, Math.max(52, layout.infoW()))),
                tx, ty + 52, SECONDARY, false);
        if (showName) {
            Pull pull = currentPull();
            if (pull != null) g.text(font, Component.literal(name(pull.characterId())), tx, ty + 73, TEXT, true);
        }
    }

    private CharacterLayout characterLayout() {
        int top = 48;
        int bottom = Math.max(top + 112, height - 46);
        int usableH = Math.max(112, bottom - top);
        int margin = width < 380 ? 12 : 20;
        int usableW = Math.max(220, width - margin * 2);
        int figureW = Math.max(104, Math.min(250, usableW * 48 / 100));
        int infoGap = width < 380 ? 9 : 18;
        int infoW = Math.max(82, usableW - figureW - infoGap);
        int figureX = margin;
        int figureY = top;
        int figureH = usableH;
        int infoX = figureX + figureW + infoGap;
        int infoY = top + Math.max(20, usableH / 2 - 48);
        return new CharacterLayout(figureX, figureY, figureW, figureH, infoX, infoY, infoW);
    }

    private void drawCharacterBackdrop(GuiGraphicsExtractor g, CharacterLayout layout, int accent, boolean active) {
        int orb = Math.min(layout.figureW() - 12, Math.min(layout.figureH() - 16, 168));
        orb = Math.max(74, orb);
        int ox = layout.figureX() + (layout.figureW() - orb) / 2;
        int oy = layout.figureY() + (layout.figureH() - orb) / 2;
        TurnboundUiSkin.orbBase(g, ox, oy, orb);
        TurnboundUiSkin.orbOverlay(g, ox, oy, orb, true, false, active);
        g.fill(layout.infoX() - 7, layout.figureY() + 12,
                layout.infoX() - 5, layout.figureY() + layout.figureH() - 12, withAlpha(accent, 170));
    }

    private int ritualOrbSize(int preferred) {
        return Math.max(92, Math.min(preferred, Math.min(width - 80, height - 88)));
    }

    private void rarityEdges(GuiGraphicsExtractor g, int accent, int thickness) {
        int t = Math.max(1, thickness);
        g.fill(0, 0, t, height, withAlpha(accent, 190));
        g.fill(width - t, 0, width, height, withAlpha(accent, 190));
        g.fill(0, 0, width, Math.max(1, t / 2), withAlpha(accent, 90));
        g.fill(0, height - Math.max(1, t / 2), width, height, withAlpha(accent, 90));
    }

    private void centerText(GuiGraphicsExtractor g, String text, int y, int color, boolean shadow) {
        g.text(font, Component.literal(text), width / 2 - font.width(text) / 2, y, color, shadow);
    }

    private Pull currentPull() {
        int index = flow.currentIndex();
        return index >= 0 && index < batch.pulls().size() ? batch.pulls().get(index) : null;
    }

    private int highestStars() {
        int highest = 1;
        for (Pull pull : batch.pulls()) highest = Math.max(highest, pull.stars());
        return Math.max(1, Math.min(5, highest));
    }

    private void playStageEntry(GachaPresentationFlow.Stage stage) {
        if (lastAudioStage == stage && lastAudioIndex == flow.currentIndex()) return;
        lastAudioStage = stage;
        lastAudioIndex = flow.currentIndex();
        Pull pull = currentPull();

        switch (stage) {
            case RITUAL -> ClientAudioDirector.acceptBatch("spawn|SYSTEM|1||||1");
            case OMEN -> {
                int stars = flow.omenStars();
                int priority = stars >= 4 ? 2 : stars >= 3 ? 1 : 0;
                ClientAudioDirector.acceptBatch("spawn|SYSTEM|" + priority + "||||" + stars);
            }
            case BREAK -> {
                if (flow.highestStars() < 5) {
                    int stars = flow.breakDisplayStars();
                    ClientAudioDirector.acceptBatch("spawn|SYSTEM|" + (stars >= 4 ? 2 : 1) + "||||" + stars);
                }
            }
            case REVEAL -> {
                if (pull != null) {
                    int priority = pull.stars() >= 5 ? 3 : pull.stars() >= 4 ? 2 : 1;
                    ClientAudioDirector.acceptBatch(revealCue(pull) + "|SKILL|" + priority
                            + "|" + pull.characterId() + "||summon|" + pull.stars());
                }
            }
            default -> { }
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
        return "★".repeat(Math.max(1, Math.min(5, stars)));
    }

    private static int starColor(int stars) {
        return switch (Math.max(1, Math.min(5, stars))) {
            case 5 -> GOLD;
            case 4 -> PURPLE;
            case 3 -> BLUE;
            case 2 -> GREEN;
            default -> SILVER;
        };
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
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
        // Only finish() sends GACHA_DONE. Unexpected replacement cannot silently consume a summon presentation.
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record CharacterLayout(int figureX, int figureY, int figureW, int figureH,
                                   int infoX, int infoY, int infoW) { }
}
