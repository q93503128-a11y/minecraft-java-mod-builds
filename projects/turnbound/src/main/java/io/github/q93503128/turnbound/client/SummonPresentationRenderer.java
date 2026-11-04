package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.GachaPresentationFlow;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Composes authored page and character artwork; state ownership stays in GachaPresentationFlow. */
final class SummonPresentationRenderer {
    private static final long TICK_NANOS = 50_000_000L;
    private static GachaPresentationScreen clockScreen;
    private static GachaPresentationFlow.Stage clockStage;
    private static int clockIndex = -1;
    private static int clockTick = -1;
    private static long clockTickStartedNanos;

    private SummonPresentationRenderer() {}

    static void render(GachaPresentationScreen screen, GuiGraphicsExtractor graphics) {
        GachaPresentationFlow flow = screen.flowView();
        if (flow.summary()) {
            drawSummary(screen, graphics);
            return;
        }

        float progress = visualProgress(screen, flow);
        switch (flow.stage()) {
            case RITUAL -> drawRitual(screen, graphics, progress);
            case OMEN -> drawOmen(screen, graphics, progress);
            case BREAK -> drawBreak(screen, graphics, progress);
            case SILHOUETTE -> drawSilhouette(screen, graphics, progress);
            case REVEAL -> drawCharacter(screen, graphics, true, progress);
            case HOLD -> drawCharacter(screen, graphics, false, 1.0F);
            case SUMMARY -> { }
        }
        drawCinematicHud(screen, graphics);
    }

    private static void drawRitual(GachaPresentationScreen screen, GuiGraphicsExtractor graphics, float progress) {
        screen.text(graphics, "정령의 기록", 18, 17, GachaPresentationScreen.TEXT, true);
        centerText(screen, graphics, "페이지가 열립니다", screen.screenHeight() / 2,
                withAlpha(GachaPresentationScreen.SECONDARY, fadeIn(progress, 0.08F, 0.78F)), false);
    }

    private static void drawOmen(GachaPresentationScreen screen, GuiGraphicsExtractor graphics, float progress) {
        GachaPresentationFlow flow = screen.flowView();
        int omenStars = flow.omenStars();
        drawRarityWash(graphics, omenStars, screen.screenWidth(), screen.screenHeight());

        String copy = omenStars >= 4 ? "잉크가 색을 기억합니다" : "기록이 응답합니다";
        centerText(screen, graphics, copy, screen.screenHeight() / 2,
                withAlpha(GachaPresentationScreen.TEXT, fadeIn(progress, 0.10F, 0.62F)), true);
    }

    private static void drawBreak(GachaPresentationScreen screen, GuiGraphicsExtractor graphics, float progress) {
        GachaPresentationFlow flow = screen.flowView();
        boolean goldUpgrade = flow.fiveStarUpgrade();
        int stars = flow.breakDisplayStars();
        drawRarityWash(graphics, stars, screen.screenWidth(), screen.screenHeight());

        SummonVisualSpec.Spec visual = SummonVisualSpec.forStars(stars);
        float flashProgress = Math.max(0.0F, 1.0F - progress / 0.36F);
        int flashAlpha = Math.round(visual.breakFlashAlpha() * flashProgress);
        if (flashAlpha > 0) {
            graphics.fill(0, 0, screen.screenWidth(), screen.screenHeight(),
                    withAlpha(visual.accent(), flashAlpha));
        }

        if (goldUpgrade) {
            centerText(screen, graphics, "★★★★★", screen.screenHeight() / 2 + 118,
                    withAlpha(GachaPresentationScreen.GOLD, fadeIn(progress, 0.42F, 0.78F)), true);
        }
    }

    private static void drawSilhouette(GachaPresentationScreen screen, GuiGraphicsExtractor graphics, float progress) {
        GachaPresentationScreen.Pull pull = screen.currentPull();
        if (pull == null) return;

        CharacterLayout layout = characterLayout(screen);
        drawRarityWash(graphics, pull.stars(), screen.screenWidth(), screen.screenHeight());

        float settle = SummonEasing.easeOutCubic(progress);
        float scale = 0.90F + 0.10F * settle;
        int portraitW = Math.round(layout.portraitW() * scale);
        int portraitH = Math.round(layout.portraitH() * scale);
        int portraitX = layout.portraitX() + (layout.portraitW() - portraitW) / 2
                + Math.round(18.0F * (1.0F - settle));
        int portraitY = layout.portraitY() + (layout.portraitH() - portraitH) / 2;
        float rarityScale = SummonVisualSpec.forStars(pull.stars()).portraitScale();
        SummonLayerRenderer.portrait(graphics, pull.characterId(), true,
                portraitX, portraitY, portraitW, portraitH, rarityScale);

        screen.text(graphics, "페이지 너머의 인연", layout.textX(), layout.textY(),
                GachaPresentationScreen.SECONDARY, true);
    }

    private static void drawCharacter(GachaPresentationScreen screen, GuiGraphicsExtractor graphics,
                                      boolean revealMotion, float progress) {
        GachaPresentationScreen.Pull pull = screen.currentPull();
        if (pull == null) return;

        CharacterLayout layout = characterLayout(screen);
        drawRarityWash(graphics, pull.stars(), screen.screenWidth(), screen.screenHeight());

        float settle = revealMotion ? SummonEasing.easeOutCubic(progress) : 1.0F;
        float scale = revealMotion ? 1.045F - 0.045F * settle : 1.0F;
        int portraitW = Math.round(layout.portraitW() * scale);
        int portraitH = Math.round(layout.portraitH() * scale);
        int portraitX = layout.portraitX() + (layout.portraitW() - portraitW) / 2
                + (revealMotion ? Math.round(22.0F * (1.0F - settle)) : 0);
        int portraitY = layout.portraitY() + (layout.portraitH() - portraitH) / 2;
        float rarityScale = SummonVisualSpec.forStars(pull.stars()).portraitScale();
        SummonLayerRenderer.portrait(graphics, pull.characterId(), false,
                portraitX, portraitY, portraitW, portraitH, rarityScale);

        drawRevealCopy(screen, graphics, pull, layout, revealMotion, progress);
    }

    private static void drawRevealCopy(GachaPresentationScreen screen, GuiGraphicsExtractor graphics,
                                       GachaPresentationScreen.Pull pull, CharacterLayout layout,
                                       boolean revealMotion, float progress) {
        int x = layout.textX();
        int y = layout.textY();
        int color = GachaPresentationScreen.starColor(pull.stars());
        int labelAlpha = revealMotion ? fadeIn(progress, 0.08F, 0.38F) : 255;
        int nameAlpha = revealMotion ? fadeIn(progress, 0.24F, 0.58F) : 255;
        int resultAlpha = revealMotion ? fadeIn(progress, 0.46F, 0.82F) : 255;

        String headline = pull.newlyOwned() ? "새로운 동료" : "중복 기록";
        screen.text(graphics, headline, x, y,
                withAlpha(pull.newlyOwned() ? GachaPresentationScreen.GREEN : GachaPresentationScreen.PURPLE,
                        labelAlpha), true);
        screen.text(graphics, GachaPresentationScreen.stars(pull.stars()), x, y + 26,
                withAlpha(color, labelAlpha), true);
        screen.text(graphics, GachaPresentationScreen.characterName(pull.characterId()), x, y + 54,
                withAlpha(GachaPresentationScreen.TEXT, nameAlpha), true);

        String result = pull.newlyOwned() ? "새로운 인연이 기록되었습니다"
                : GachaPresentationScreen.duplicateResult(pull);
        String fittedResult = UiTextLayout.fit(result, layout.textW());
        screen.text(graphics, fittedResult, x, y + 86,
                withAlpha(pull.newlyOwned() ? GachaPresentationScreen.SECONDARY : GachaPresentationScreen.PURPLE,
                        resultAlpha), false);

        if (!revealMotion) {
            String hint = screen.flowView().lastPull()
                    ? (screen.flowView().pullCount() <= 1 ? "Enter / Space · 확인" : "Enter / Space · 결과 보기")
                    : "Enter / Space · 다음";
            screen.text(graphics, UiTextLayout.fit(hint, layout.textW()), x,
                    Math.min(screen.screenHeight() - 48, y + 118), GachaPresentationScreen.MUTED, false);
        }
    }

    private static void drawCinematicHud(GachaPresentationScreen screen, GuiGraphicsExtractor graphics) {
        if (screen.flowView().stage().ordinal() >= GachaPresentationFlow.Stage.SILHOUETTE.ordinal()) {
            String progress = (screen.flowView().currentIndex() + 1) + " / "
                    + Math.max(1, screen.flowView().pullCount());
            int x = Math.max(14, screen.screenWidth() - 118 - screen.textWidth(progress));
            screen.text(graphics, progress, x, 17, GachaPresentationScreen.SECONDARY, true);
        } else {
            screen.text(graphics, "정령의 기록", 18, 17, GachaPresentationScreen.TEXT, true);
        }
    }

    private static void drawSummary(GachaPresentationScreen screen, GuiGraphicsExtractor graphics) {
        GachaPresentationScreen.Batch batch = screen.batchView();
        int highest = 1;
        for (GachaPresentationScreen.Pull pull : batch.pulls()) highest = Math.max(highest, pull.stars());
        drawRarityWash(graphics, highest, screen.screenWidth(), screen.screenHeight());

        String title = batch.pulls().size() <= 1 ? "소환 결과" : batch.pulls().size() + "회 소환 결과";
        screen.text(graphics, title, 18, 16, GachaPresentationScreen.TEXT, true);
        screen.text(graphics, "크리스탈 -" + batch.crystalSpent(), 18, 33,
                GachaPresentationScreen.SECONDARY, false);
        if (batch.pulls().size() <= 1) drawSingleSummary(screen, graphics);
        else drawMultiSummary(screen, graphics);
    }

    private static void drawSingleSummary(GachaPresentationScreen screen, GuiGraphicsExtractor graphics) {
        if (screen.batchView().pulls().isEmpty()) return;
        GachaPresentationScreen.Pull pull = screen.batchView().pulls().getFirst();
        CharacterLayout layout = characterLayout(screen);
        float rarityScale = SummonVisualSpec.forStars(pull.stars()).portraitScale();
        SummonLayerRenderer.portrait(graphics, pull.characterId(), false,
                layout.portraitX(), layout.portraitY(), layout.portraitW(), layout.portraitH(), rarityScale);

        int x = layout.textX();
        int y = layout.textY();
        screen.text(graphics, GachaPresentationScreen.stars(pull.stars()), x, y,
                GachaPresentationScreen.starColor(pull.stars()), true);
        screen.text(graphics, GachaPresentationScreen.characterName(pull.characterId()), x, y + 30,
                GachaPresentationScreen.TEXT, true);
        screen.text(graphics, UiTextLayout.fit(pull.newlyOwned() ? "새로운 동료"
                        : GachaPresentationScreen.duplicateResult(pull), layout.textW()),
                x, y + 58, pull.newlyOwned() ? GachaPresentationScreen.GREEN : GachaPresentationScreen.PURPLE, false);
        screen.text(graphics, "Enter / Space · 닫기", x,
                Math.min(screen.screenHeight() - 48, y + 92), GachaPresentationScreen.MUTED, false);
    }

    private static void drawMultiSummary(GachaPresentationScreen screen, GuiGraphicsExtractor graphics) {
        int count = screen.batchView().pulls().size();
        if (count <= 0) return;

        int columns = count >= 5 ? 5 : count;
        int rows = (count + columns - 1) / columns;
        int gap = screen.screenWidth() < 520 ? 3 : 8;
        int side = screen.screenWidth() < 520 ? 7 : 24;
        int top = 50;
        int bottom = 18;
        int availableWidth = Math.max(1, screen.screenWidth() - side * 2);
        int availableHeight = Math.max(1, screen.screenHeight() - top - bottom);

        int cardWidth = Math.max(1, (availableWidth - gap * (columns - 1)) / columns);
        int cardHeight = Math.max(1, (availableHeight - gap * (rows - 1)) / rows);
        int gridWidth = columns * cardWidth + (columns - 1) * gap;
        int gridHeight = rows * cardHeight + (rows - 1) * gap;
        int startX = (screen.screenWidth() - gridWidth) / 2;
        int startY = top + Math.max(0, (availableHeight - gridHeight) / 2);

        for (int i = 0; i < count; i++) {
            GachaPresentationScreen.Pull pull = screen.batchView().pulls().get(i);
            int x = startX + (i % columns) * (cardWidth + gap);
            int y = startY + (i / columns) * (cardHeight + gap);
            int frameWidth = Math.min(cardWidth, cardHeight * 2 / 3);
            int frameX = x + (cardWidth - frameWidth) / 2;
            SummonLayerRenderer.sprite(graphics, SummonAssets.card(pull.stars()),
                    frameX, y, frameWidth, cardHeight);

            int labelBlock = Math.min(cardHeight - 1, Math.max(44, cardHeight / 3));
            int portraitMaxHeight = Math.max(1, cardHeight - labelBlock - 8);
            int portraitMaxWidth = Math.max(1, frameWidth - 8);
            int portraitWidth = Math.min(portraitMaxWidth, portraitMaxHeight * 2 / 3);
            int portraitHeight = portraitWidth * 3 / 2;
            if (portraitHeight > portraitMaxHeight) {
                portraitHeight = portraitMaxHeight;
                portraitWidth = portraitHeight * 2 / 3;
            }
            int portraitX = x + (cardWidth - portraitWidth) / 2;
            int portraitY = y + 3 + Math.max(0, (portraitMaxHeight - portraitHeight) / 2);
            float rarityScale = SummonVisualSpec.forStars(pull.stars()).portraitScale();
            SummonLayerRenderer.portrait(graphics, pull.characterId(), false,
                    portraitX, portraitY, portraitWidth, portraitHeight, rarityScale);

            int nameY = y + cardHeight - 45;
            String name = UiTextLayout.fit(GachaPresentationScreen.characterName(pull.characterId()),
                    Math.max(1, frameWidth - 8));
            screen.text(graphics, name, x + (cardWidth - screen.textWidth(name)) / 2, nameY,
                    GachaPresentationScreen.TEXT, true);

            String rarity = frameWidth < 72 ? "★" + pull.stars() : GachaPresentationScreen.stars(pull.stars());
            rarity = UiTextLayout.fit(rarity, Math.max(1, frameWidth - 8));
            screen.text(graphics, rarity, x + (cardWidth - screen.textWidth(rarity)) / 2, nameY + 12,
                    GachaPresentationScreen.starColor(pull.stars()), false);

            String status = pull.newlyOwned() ? "NEW" : "중복";
            status = UiTextLayout.fit(status, Math.max(1, frameWidth - 8));
            int statusColor = pull.newlyOwned() ? GachaPresentationScreen.GREEN : GachaPresentationScreen.PURPLE;
            screen.text(graphics, status, x + (cardWidth - screen.textWidth(status)) / 2, nameY + 24,
                    statusColor, false);

            String reward = pull.newlyOwned() ? "신규 영입"
                    : pull.bonusLevelGranted() > 0 ? "+레벨 " + pull.bonusLevelGranted()
                    : pull.essence() > 0 ? "정수 +" + pull.essence() : "레벨 MAX";
            reward = UiTextLayout.fit(reward, Math.max(1, frameWidth - 8));
            screen.text(graphics, reward, x + (cardWidth - screen.textWidth(reward)) / 2, nameY + 35,
                    statusColor, false);
        }
    }

    private static CharacterLayout characterLayout(GachaPresentationScreen screen) {
        int width = screen.screenWidth();
        int height = screen.screenHeight();
        int margin = width < 520 ? 14 : 32;
        int top = 36;
        int bottom = 42;
        int usableHeight = Math.max(96, height - top - bottom);
        int maxPortraitWidth = Math.max(1, width * (width < 520 ? 42 : 44) / 100);
        int portraitWidth = Math.min(maxPortraitWidth, usableHeight * 2 / 3);
        int portraitHeight = Math.max(1, portraitWidth * 3 / 2);
        if (portraitHeight > usableHeight) {
            portraitHeight = usableHeight;
            portraitWidth = Math.max(1, portraitHeight * 2 / 3);
        }

        int portraitX = Math.max(0, width - margin - portraitWidth);
        int portraitY = top + Math.max(0, (usableHeight - portraitHeight) / 2);
        int textX = margin + 2;
        int textWidth = Math.max(1, Math.min(460, portraitX - textX - 20));
        int textY = top + Math.max(14, usableHeight / 2 - 58);
        return new CharacterLayout(portraitX, portraitY, portraitWidth, portraitHeight,
                textX, textY, textWidth);
    }

    private static void drawRarityWash(GuiGraphicsExtractor graphics, int stars, int width, int height) {
        Identifier wash = SummonAssets.rarityWash(stars);
        if (wash != null) SummonLayerRenderer.fullScreen(graphics, wash, width, height);
    }

    private static float visualProgress(GachaPresentationScreen screen, GachaPresentationFlow flow) {
        int duration = flow.durationTicks();
        if (duration <= 0) return 1.0F;

        long now = System.nanoTime();
        if (clockScreen != screen || clockStage != flow.stage()
                || clockIndex != flow.currentIndex() || clockTick != flow.stageTick()) {
            clockScreen = screen;
            clockStage = flow.stage();
            clockIndex = flow.currentIndex();
            clockTick = flow.stageTick();
            clockTickStartedNanos = now;
        }
        float subTick = Math.max(0.0F, Math.min(1.0F,
                (now - clockTickStartedNanos) / (float) TICK_NANOS));
        return Math.max(0.0F, Math.min(1.0F, (flow.stageTick() + subTick) / duration));
    }

    private static int fadeIn(float progress, float start, float end) {
        if (progress <= start) return 0;
        if (progress >= end) return 255;
        float t = (progress - start) / Math.max(0.001F, end - start);
        return Math.round(255.0F * SummonEasing.easeOutCubic(t));
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static void centerText(GachaPresentationScreen screen, GuiGraphicsExtractor graphics,
                                   String text, int y, int color, boolean shadow) {
        screen.text(graphics, text, screen.screenWidth() / 2 - screen.textWidth(text) / 2, y, color, shadow);
    }

    private record CharacterLayout(int portraitX, int portraitY, int portraitW, int portraitH,
                                   int textX, int textY, int textW) { }
}
