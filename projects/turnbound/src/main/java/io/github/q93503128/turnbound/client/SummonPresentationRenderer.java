package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.GachaPresentationFlow;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Visual-only renderer for the 2D summon cinematic. State ownership stays in GachaPresentationScreen/Flow. */
final class SummonPresentationRenderer {
    private static final long TICK_NANOS = 50_000_000L;
    private static GachaPresentationScreen clockScreen;
    private static GachaPresentationFlow.Stage clockStage;
    private static int clockIndex = -1;
    private static int clockTick = -1;
    private static long clockTickStartedNanos;

    private SummonPresentationRenderer() {}

    static void render(GachaPresentationScreen screen, GuiGraphicsExtractor g) {
        GachaPresentationFlow flow = screen.flowView();
        if (flow.summary()) {
            drawSummary(screen, g);
            return;
        }
        float progress = visualProgress(screen, flow);
        switch (flow.stage()) {
            case RITUAL -> drawRitual(screen, g, progress);
            case OMEN -> drawOmen(screen, g, progress);
            case BREAK -> drawBreak(screen, g, progress);
            case SILHOUETTE -> drawCharacter(screen, g, true, false, progress);
            case REVEAL -> drawCharacter(screen, g, false, true, progress);
            case HOLD -> drawCharacter(screen, g, false, false, 1.0F);
            case SUMMARY -> { }
        }
        drawCinematicHud(screen, g);
    }

    private static void drawRitual(GachaPresentationScreen s, GuiGraphicsExtractor g, float progress) {
        float eased = SummonEasing.easeOutCubic(progress);
        int cx = s.screenWidth() / 2;
        int cy = s.screenHeight() / 2 - 4;
        int base = Math.max(132, Math.min(300, Math.min(s.screenWidth(), s.screenHeight()) * 58 / 100));
        int glow = Math.round(base * (1.02F + 0.18F * eased));
        SummonLayerRenderer.centered(g, SummonAssets.RADIAL_GLOW, cx, cy, glow + 96, glow + 96);

        int crystalH = Math.round(base * (0.72F + 0.05F * eased));
        int crystalW = Math.round(crystalH * 0.70F);
        int lift = Math.round(10.0F * (1.0F - eased));
        SummonLayerRenderer.centered(g, SummonAssets.CRYSTAL, cx, cy + lift, crystalW, crystalH);

        int sweepW = Math.max(120, base);
        int sweepX = cx - base + Math.round((base * 2.0F - sweepW) * SummonEasing.easeInOutCubic(progress));
        SummonLayerRenderer.sprite(g, SummonAssets.LIGHT_SWEEP, sweepX, cy - 24, sweepW, 48);
        centerText(s, g, "기록이 공명합니다",
                Math.min(s.screenHeight() - 38, cy + base / 2 + 15),
                withAlpha(GachaPresentationScreen.SECONDARY, fadeIn(progress, 0.12F, 0.58F)), true);
    }

    private static void drawOmen(GachaPresentationScreen s, GuiGraphicsExtractor g, float progress) {
        GachaPresentationFlow f = s.flowView();
        int cx = s.screenWidth() / 2;
        int cy = s.screenHeight() / 2 - 3;
        int base = Math.max(150, Math.min(320, Math.min(s.screenWidth(), s.screenHeight()) * 62 / 100));
        float pulse = 0.98F + 0.07F * SummonEasing.pulse(progress);
        int glow = Math.round((base + 116) * pulse);
        SummonLayerRenderer.centered(g, f.omenStars() >= 4 ? SummonAssets.OMEN_VIOLET : SummonAssets.RADIAL_GLOW,
                cx, cy, glow, glow);

        int crystalH = Math.round(base * 0.74F);
        SummonLayerRenderer.centered(g, SummonAssets.CRYSTAL, cx, cy,
                Math.round(crystalH * 0.70F), crystalH);

        int wash = Math.round(24.0F * SummonEasing.pulse(progress));
        g.fill(0, 0, s.screenWidth(), s.screenHeight(),
                f.omenStars() >= 4 ? (wash << 24) | 0x005A2879 : (wash << 24) | 0x001A5271);

        centerText(s, g, f.omenStars() >= 4 ? "강한 공명이 접근합니다" : "기록이 응답합니다",
                Math.min(s.screenHeight() - 38, cy + base / 2 + 14),
                withAlpha(GachaPresentationScreen.TEXT, fadeIn(progress, 0.08F, 0.38F)), true);
    }

    private static void drawBreak(GachaPresentationScreen s, GuiGraphicsExtractor g, float progress) {
        GachaPresentationFlow f = s.flowView();
        int cx = s.screenWidth() / 2;
        int cy = s.screenHeight() / 2;
        int base = Math.max(164, Math.min(340, Math.min(s.screenWidth(), s.screenHeight()) * 66 / 100));
        boolean gold = f.highestStars() >= 5 && f.fiveStarUpgrade();
        float burst = SummonEasing.easeOutCubic(progress);
        int burstSize = Math.round((base + 74) * (0.84F + 0.34F * burst));
        SummonLayerRenderer.centered(g, gold ? SummonAssets.BREAK_GOLD : SummonAssets.OMEN_VIOLET,
                cx, cy, burstSize, burstSize);

        int crystalH = Math.round(base * (0.78F - 0.18F * burst));
        SummonLayerRenderer.centered(g, SummonAssets.CRYSTAL, cx, cy,
                Math.round(crystalH * 0.70F), crystalH);

        float flash = 1.0F - Math.min(1.0F, progress / 0.28F);
        int flashAlpha = Math.round((gold ? 150 : 92) * flash);
        if (flashAlpha > 0) {
            g.fill(0, 0, s.screenWidth(), s.screenHeight(),
                    gold ? (flashAlpha << 24) | 0x00FFF0B0 : (flashAlpha << 24) | 0x00B57DFF);
        }
        if (gold) {
            centerText(s, g, "★★★★★",
                    Math.min(s.screenHeight() - 44, cy + base / 2 - 2),
                    withAlpha(GachaPresentationScreen.GOLD, fadeIn(progress, 0.14F, 0.42F)), true);
        }
    }

    private static void drawCharacter(GachaPresentationScreen s, GuiGraphicsExtractor g,
                                      boolean silhouette, boolean revealMotion, float progress) {
        GachaPresentationScreen.Pull pull = s.currentPull();
        if (pull == null) return;

        SummonVisualSpec.Spec visual = SummonVisualSpec.forStars(pull.stars());
        CharacterLayout layout = characterLayout(s);
        int accent = GachaPresentationScreen.starColor(pull.stars());
        int glowX = layout.portraitX() + layout.portraitW() / 2;
        int glowY = layout.portraitY() + layout.portraitH() / 2;
        int glowSize = Math.round(Math.max(layout.portraitW() * 1.75F, layout.portraitH() * 0.88F)
                * visual.glowScale());

        SummonLayerRenderer.centered(g, pull.stars() >= 5 ? SummonAssets.BREAK_GOLD
                        : pull.stars() >= 4 ? SummonAssets.OMEN_VIOLET : SummonAssets.RADIAL_GLOW,
                glowX, glowY, glowSize, glowSize);

        int portraitW = layout.portraitW();
        int portraitH = layout.portraitH();
        int portraitX = layout.portraitX();
        int portraitY = layout.portraitY();

        if (silhouette) {
            float settle = SummonEasing.easeOutCubic(progress);
            float scale = 0.94F + 0.06F * settle;
            int w = Math.round(portraitW * scale);
            int h = Math.round(portraitH * scale);
            portraitX += (portraitW - w) / 2 + Math.round(8.0F * (1.0F - settle));
            portraitY += (portraitH - h) / 2 + Math.round(7.0F * (1.0F - settle));
            portraitW = w;
            portraitH = h;
        } else if (revealMotion) {
            float settle = SummonEasing.easeOutCubic(progress);
            float scale = 1.045F - 0.045F * settle;
            int w = Math.round(portraitW * scale);
            int h = Math.round(portraitH * scale);
            portraitX += (portraitW - w) / 2 + Math.round(13.0F * (1.0F - settle));
            portraitY += (portraitH - h) / 2;
            portraitW = w;
            portraitH = h;
        }

        SummonLayerRenderer.portrait(g, pull.characterId(), silhouette,
                portraitX, portraitY, portraitW, portraitH);

        if (!silhouette && revealMotion) {
            int sweepW = Math.max(116, layout.portraitW());
            int sweepX = layout.portraitX() - sweepW
                    + Math.round((layout.portraitW() + sweepW * 2.0F) * SummonEasing.easeInOutCubic(progress));
            SummonLayerRenderer.sprite(g, SummonAssets.LIGHT_SWEEP, sweepX,
                    layout.portraitY() + layout.portraitH() / 3,
                    sweepW, Math.max(34, layout.portraitH() / 5));

            float flash = 1.0F - Math.min(1.0F, progress / 0.22F);
            int flashAlpha = Math.round((pull.stars() >= 5 ? 112 : pull.stars() >= 4 ? 78 : 48) * flash);
            if (flashAlpha > 0) {
                g.fill(0, 0, s.screenWidth(), s.screenHeight(), withAlpha(accent, flashAlpha));
            }
        }

        drawRevealCopy(s, g, pull, layout, silhouette, revealMotion, progress);
    }

    private static void drawRevealCopy(GachaPresentationScreen s, GuiGraphicsExtractor g,
                                       GachaPresentationScreen.Pull pull, CharacterLayout layout,
                                       boolean silhouette, boolean revealMotion, float progress) {
        int tx = layout.textX();
        int ty = layout.textY();
        int accent = GachaPresentationScreen.starColor(pull.stars());

        if (silhouette) {
            int alpha = fadeIn(progress, 0.18F, 0.64F);
            s.text(g, "기록 해독 중", tx, ty,
                    withAlpha(GachaPresentationScreen.SECONDARY, alpha), false);
            s.text(g, GachaPresentationScreen.stars(pull.stars()), tx, ty + 24,
                    withAlpha(accent, alpha), true);
            g.fill(tx, ty + 43, tx + Math.min(layout.textW(), 112), ty + 44,
                    withAlpha(accent, alpha / 2));
            return;
        }

        int headlineAlpha = revealMotion ? fadeIn(progress, 0.12F, 0.42F) : 255;
        int nameAlpha = revealMotion ? fadeIn(progress, 0.26F, 0.58F) : 255;
        int detailAlpha = revealMotion ? fadeIn(progress, 0.46F, 0.84F) : 255;
        String headline = pull.newlyOwned() ? "새로운 동료" : "기록 재현";

        s.text(g, headline, tx, ty,
                withAlpha(pull.newlyOwned() ? GachaPresentationScreen.GREEN : GachaPresentationScreen.SECONDARY,
                        headlineAlpha), true);
        s.text(g, GachaPresentationScreen.stars(pull.stars()), tx, ty + 23,
                withAlpha(accent, headlineAlpha), true);
        g.fill(tx, ty + 42, tx + Math.min(layout.textW(), 132), ty + 44,
                withAlpha(accent, Math.max(0, headlineAlpha * 2 / 3)));
        s.text(g, GachaPresentationScreen.characterName(pull.characterId()), tx, ty + 53,
                withAlpha(GachaPresentationScreen.TEXT, nameAlpha), true);

        String result = pull.newlyOwned() ? "새로운 인연이 기록되었습니다"
                : GachaPresentationScreen.duplicateResult(pull);
        s.text(g, UiTextLayout.fit(result, Math.max(96, layout.textW())), tx, ty + 76,
                withAlpha(pull.newlyOwned() ? GachaPresentationScreen.SECONDARY : GachaPresentationScreen.PURPLE,
                        detailAlpha), false);

        if (!revealMotion) {
            String hint = s.flowView().lastPull()
                    ? (s.flowView().pullCount() <= 1 ? "Enter / Space · 확인" : "Enter / Space · 결과 보기")
                    : "Enter / Space · 다음";
            s.text(g, hint, tx, Math.min(s.screenHeight() - 48, ty + 105),
                    GachaPresentationScreen.MUTED, false);
        }
    }

    private static void drawCinematicHud(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        if (s.flowView().stage().ordinal() >= GachaPresentationFlow.Stage.SILHOUETTE.ordinal()) {
            String progress = (s.flowView().currentIndex() + 1) + " / " + Math.max(1, s.flowView().pullCount());
            int x = Math.max(12, s.screenWidth() - 120 - s.textWidth(progress));
            s.text(g, progress, x, 18, GachaPresentationScreen.SECONDARY, true);
        } else {
            s.text(g, "정령의 기록", 18, 17, GachaPresentationScreen.TEXT, true);
        }
    }

    private static void drawSummary(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        GachaPresentationScreen.Batch batch = s.batchView();
        String title = batch.pulls().size() <= 1 ? "소환 결과" : batch.pulls().size() + "회 소환 결과";
        s.text(g, title, 18, 17, GachaPresentationScreen.TEXT, true);
        s.text(g, "크리스탈 -" + batch.crystalSpent(), 18, 33, GachaPresentationScreen.SECONDARY, false);
        if (batch.pulls().size() <= 1) drawSingleSummary(s, g);
        else drawMultiSummary(s, g);
    }

    private static void drawSingleSummary(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        if (s.batchView().pulls().isEmpty()) return;
        GachaPresentationScreen.Pull pull = s.batchView().pulls().getFirst();
        int w = Math.min(560, s.screenWidth() - 40);
        int h = Math.min(300, s.screenHeight() - 86);
        int x = (s.screenWidth() - w) / 2;
        int y = 48 + Math.max(0, (s.screenHeight() - 48 - h) / 2);
        TurnboundUiSkin.panel(g, x, y, w, h);

        int maxPortraitH = Math.max(90, h - 20);
        int portraitH = Math.min(maxPortraitH, Math.max(90, h - 20));
        int portraitW = portraitH * 2 / 3;
        portraitW = Math.min(portraitW, w * 46 / 100);
        portraitH = portraitW * 3 / 2;
        int px = x + w - portraitW - 12;
        int py = y + (h - portraitH) / 2;
        int glow = Math.max(portraitW + 60, portraitH * 4 / 5);
        SummonLayerRenderer.centered(g, pull.stars() >= 5 ? SummonAssets.BREAK_GOLD : SummonAssets.RADIAL_GLOW,
                px + portraitW / 2, py + portraitH / 2, glow, glow);
        SummonLayerRenderer.portrait(g, pull.characterId(), false, px, py, portraitW, portraitH);

        int tx = x + 18;
        int ty = y + Math.max(34, h / 2 - 52);
        int accent = GachaPresentationScreen.starColor(pull.stars());
        s.text(g, GachaPresentationScreen.stars(pull.stars()), tx, ty, accent, true);
        s.text(g, GachaPresentationScreen.characterName(pull.characterId()), tx, ty + 26,
                GachaPresentationScreen.TEXT, true);
        s.text(g, pull.newlyOwned() ? "새로운 동료" : GachaPresentationScreen.duplicateResult(pull),
                tx, ty + 50, pull.newlyOwned() ? GachaPresentationScreen.GREEN : GachaPresentationScreen.PURPLE, false);
        s.text(g, "Enter / Space · 닫기", tx, ty + 84, GachaPresentationScreen.MUTED, false);
    }

    private static void drawMultiSummary(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        int count = s.batchView().pulls().size();
        if (count <= 0) return;

        // Ten-pull is a result board, not a scrolling list. Keep the canonical 5x2 silhouette at every GUI scale.
        int cols = count >= 8 ? 5 : count >= 5 ? 5 : Math.max(1, count);
        int rows = (count + cols - 1) / cols;
        int gap = s.screenWidth() < 520 ? 4 : 6;
        int top = 48;
        int side = s.screenWidth() < 520 ? 10 : 18;
        int bottom = 24;
        int availableW = Math.max(120, s.screenWidth() - side * 2);
        int availableH = Math.max(90, s.screenHeight() - top - bottom);

        int cardW = Math.max(52, (availableW - gap * (cols - 1)) / cols);
        int cardH = Math.max(68, (availableH - gap * (rows - 1)) / rows);
        int gridW = cols * cardW + (cols - 1) * gap;
        int gridH = rows * cardH + (rows - 1) * gap;
        int startX = (s.screenWidth() - gridW) / 2;
        int startY = top + Math.max(0, (availableH - gridH) / 2);

        for (int i = 0; i < count; i++) {
            GachaPresentationScreen.Pull pull = s.batchView().pulls().get(i);
            int x = startX + (i % cols) * (cardW + gap);
            int y = startY + (i / cols) * (cardH + gap);
            TurnboundUiSkin.inset(g, x, y, cardW, cardH);

            int accent = GachaPresentationScreen.starColor(pull.stars());
            g.fill(x + 5, y + 5, x + cardW - 5, y + 7, withAlpha(accent, 210));

            int reservedText = 29;
            int maxPortraitH = Math.max(28, cardH - reservedText - 8);
            int maxPortraitW = Math.max(20, cardW - 12);
            int portraitW = Math.min(maxPortraitW, maxPortraitH * 2 / 3);
            int portraitH = portraitW * 3 / 2;
            if (portraitH > maxPortraitH) {
                portraitH = maxPortraitH;
                portraitW = portraitH * 2 / 3;
            }
            int px = x + (cardW - portraitW) / 2;
            int py = y + 7 + Math.max(0, (maxPortraitH - portraitH) / 2);
            SummonLayerRenderer.portrait(g, pull.characterId(), false, px, py, portraitW, portraitH);

            int textW = Math.max(28, cardW - 8);
            String characterName = UiTextLayout.fit(GachaPresentationScreen.characterName(pull.characterId()), textW);
            int nameY = y + cardH - 25;
            s.text(g, characterName, x + (cardW - s.textWidth(characterName)) / 2,
                    nameY, GachaPresentationScreen.TEXT, true);
            String rarity = GachaPresentationScreen.stars(pull.stars());
            s.text(g, rarity, x + (cardW - s.textWidth(rarity)) / 2,
                    nameY + 12, accent, false);

            if (pull.newlyOwned()) {
                s.text(g, "NEW", x + 6, y + 9, GachaPresentationScreen.GOLD, true);
            }
        }
    }

    private static CharacterLayout characterLayout(GachaPresentationScreen s) {
        int margin = s.screenWidth() < 520 ? 14 : 28;
        int top = 30;
        int bottom = 38;
        int usableH = Math.max(130, s.screenHeight() - top - bottom);
        int maxPortraitW = Math.max(96, s.screenWidth() * (s.screenWidth() < 520 ? 36 : 42) / 100);
        int portraitW = Math.min(maxPortraitW, usableH * 2 / 3);
        int portraitH = portraitW * 3 / 2;
        if (portraitH > usableH) {
            portraitH = usableH;
            portraitW = portraitH * 2 / 3;
        }

        int portraitX = s.screenWidth() - margin - portraitW;
        int portraitY = top + (usableH - portraitH) / 2;
        int textX = margin + 4;
        int textW = Math.max(96, portraitX - textX - 18);
        int textY = top + Math.max(20, usableH / 2 - 55);
        return new CharacterLayout(portraitX, portraitY, portraitW, portraitH, textX, textY, textW);
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

    private static void centerText(GachaPresentationScreen s, GuiGraphicsExtractor g,
                                   String text, int y, int color, boolean shadow) {
        s.text(g, text, s.screenWidth() / 2 - s.textWidth(text) / 2, y, color, shadow);
    }

    private record CharacterLayout(int portraitX, int portraitY, int portraitW, int portraitH,
                                   int textX, int textY, int textW) { }
}
