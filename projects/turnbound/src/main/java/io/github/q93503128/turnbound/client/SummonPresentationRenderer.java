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
        float ambient = ambientWave(0.80);
        int cx = s.screenWidth() / 2;
        int cy = s.screenHeight() / 2 - 4;
        int base = Math.max(142, Math.min(310, Math.min(s.screenWidth(), s.screenHeight()) * 62 / 100));
        int sigil = Math.round(base * (0.92F + 0.08F * eased));

        SummonLayerRenderer.centered(g, SummonAssets.SIGIL_BACK, cx, cy, sigil, sigil);
        SummonLayerRenderer.centered(g, SummonAssets.FLARE_CYAN, cx, cy,
                Math.min(s.screenWidth() - 18, base * 2 + 80), Math.max(54, base / 3));
        SummonLayerRenderer.fullScreen(g, SummonAssets.SHARDS_CYAN, s.screenWidth(), s.screenHeight());

        int crystalH = Math.round(base * (0.66F + 0.08F * eased));
        int crystalW = crystalH * 72 / 100;
        int lift = Math.round(14.0F * (1.0F - eased) + (ambient - 0.5F) * 4.0F);
        SummonLayerRenderer.centered(g, SummonAssets.CRYSTAL, cx, cy + lift, crystalW, crystalH);

        int page = Math.max(15, base / 12);
        int spread = Math.max(64, base / 2);
        SummonLayerRenderer.sprite(g, SummonAssets.PAGE,
                cx - spread - page, cy - base / 5 + Math.round(6 * ambient), page * 4 / 3, page);
        SummonLayerRenderer.sprite(g, SummonAssets.PAGE,
                cx + spread, cy + base / 7 - Math.round(5 * ambient), page * 4 / 3, page);

        centerText(s, g, "기록이 공명합니다",
                Math.min(s.screenHeight() - 42, cy + base / 2 + 14),
                withAlpha(GachaPresentationScreen.SECONDARY, fadeIn(progress, 0.12F, 0.52F)), true);
    }

    private static void drawOmen(GachaPresentationScreen s, GuiGraphicsExtractor g, float progress) {
        GachaPresentationFlow f = s.flowView();
        int omenStars = f.omenStars();
        int cx = s.screenWidth() / 2;
        int cy = s.screenHeight() / 2 - 2;
        int base = Math.max(150, Math.min(330, Math.min(s.screenWidth(), s.screenHeight()) * 67 / 100));
        float pulse = 0.98F + 0.045F * SummonEasing.pulse(progress);
        int sigil = Math.round(base * pulse);

        SummonLayerRenderer.fullScreen(g, SummonAssets.shards(omenStars), s.screenWidth(), s.screenHeight());
        SummonLayerRenderer.centered(g, SummonAssets.SIGIL_BACK, cx, cy, sigil, sigil);
        SummonLayerRenderer.centered(g, SummonAssets.flare(omenStars), cx, cy,
                Math.min(s.screenWidth() - 12, base * 2 + 100), Math.max(58, base / 3));
        SummonLayerRenderer.centered(g, omenStars >= 4 ? SummonAssets.OMEN_VIOLET : SummonAssets.RADIAL_GLOW,
                cx, cy, base + 118, base + 118);

        int crystalH = Math.round(base * 0.70F);
        SummonLayerRenderer.centered(g, SummonAssets.CRYSTAL, cx, cy,
                crystalH * 72 / 100, crystalH);

        int wash = Math.round((omenStars >= 4 ? 32.0F : 20.0F) * SummonEasing.pulse(progress));
        g.fill(0, 0, s.screenWidth(), s.screenHeight(),
                omenStars >= 4 ? (wash << 24) | 0x005A2879 : (wash << 24) | 0x001A5271);

        centerText(s, g, omenStars >= 4 ? "공명이 변질됩니다" : "기록이 응답합니다",
                Math.min(s.screenHeight() - 42, cy + base / 2 + 14),
                withAlpha(GachaPresentationScreen.TEXT, fadeIn(progress, 0.08F, 0.34F)), true);
    }

    private static void drawBreak(GachaPresentationScreen s, GuiGraphicsExtractor g, float progress) {
        GachaPresentationFlow f = s.flowView();
        int cx = s.screenWidth() / 2;
        int cy = s.screenHeight() / 2;
        int base = Math.max(166, Math.min(350, Math.min(s.screenWidth(), s.screenHeight()) * 70 / 100));
        boolean gold = f.highestStars() >= 5 && f.fiveStarUpgrade();
        int stars = gold ? 5 : f.breakDisplayStars();
        float burst = SummonEasing.easeOutCubic(progress);

        SummonLayerRenderer.fullScreen(g, SummonAssets.shards(stars), s.screenWidth(), s.screenHeight());
        SummonLayerRenderer.centered(g, SummonAssets.SIGIL_BACK, cx, cy,
                Math.round(base * (0.86F + 0.20F * burst)), Math.round(base * (0.86F + 0.20F * burst)));
        SummonLayerRenderer.centered(g, SummonAssets.flare(stars), cx, cy,
                Math.min(s.screenWidth() + 80, base * 3), Math.max(72, base / 2));
        int burstSize = Math.round((base + 86) * (0.80F + 0.40F * burst));
        SummonLayerRenderer.centered(g, gold ? SummonAssets.BREAK_GOLD : SummonAssets.OMEN_VIOLET,
                cx, cy, burstSize, burstSize);

        int crystalH = Math.max(42, Math.round(base * (0.76F - 0.28F * burst)));
        SummonLayerRenderer.centered(g, SummonAssets.CRYSTAL, cx, cy,
                crystalH * 72 / 100, crystalH);

        float flash = 1.0F - Math.min(1.0F, progress / 0.30F);
        int flashAlpha = Math.round((gold ? 205 : stars >= 4 ? 124 : 80) * flash);
        if (flashAlpha > 0) {
            g.fill(0, 0, s.screenWidth(), s.screenHeight(),
                    gold ? (flashAlpha << 24) | 0x00FFF0B0 : (flashAlpha << 24) | 0x00B57DFF);
        }
        if (gold) {
            centerText(s, g, "★★★★★",
                    Math.min(s.screenHeight() - 46, cy + base / 2 - 1),
                    withAlpha(GachaPresentationScreen.GOLD, fadeIn(progress, 0.16F, 0.46F)), true);
        }
    }

    private static void drawCharacter(GachaPresentationScreen s, GuiGraphicsExtractor g,
                                      boolean silhouette, boolean revealMotion, float progress) {
        GachaPresentationScreen.Pull pull = s.currentPull();
        if (pull == null) return;

        CharacterLayout layout = characterLayout(s);
        int stars = pull.stars();
        int accent = GachaPresentationScreen.starColor(stars);
        float ambient = ambientWave(1.35);
        float settle = silhouette ? SummonEasing.easeOutCubic(progress)
                : revealMotion ? SummonEasing.easeOutCubic(progress) : 1.0F;
        int shakeX = revealMotion ? rarityShake(stars, progress, true) : 0;
        int shakeY = revealMotion ? rarityShake(stars, progress, false) : 0;

        int glowX = layout.portraitX() + layout.portraitW() / 2 + shakeX;
        int glowY = layout.portraitY() + layout.portraitH() / 2 + shakeY;
        int flareW = Math.min(s.screenWidth() + 90, Math.max(layout.portraitW() * 3, s.screenWidth() * 2 / 3));
        int flareH = Math.max(62, layout.portraitH() / 3);

        SummonLayerRenderer.fullScreen(g, SummonAssets.shards(stars), s.screenWidth(), s.screenHeight());
        int sigil = Math.max(layout.portraitW() + 68, Math.min(layout.portraitH(), layout.portraitW() * 2));
        SummonLayerRenderer.centered(g, SummonAssets.SIGIL_BACK, glowX, glowY, sigil, sigil);
        SummonLayerRenderer.centered(g, SummonAssets.flare(stars), glowX, glowY, flareW, flareH);

        int portraitW = layout.portraitW();
        int portraitH = layout.portraitH();
        int portraitX = layout.portraitX();
        int portraitY = layout.portraitY() + Math.round((ambient - 0.5F) * 3.0F);

        if (silhouette) {
            float scale = 0.90F + 0.10F * settle;
            int w = Math.round(portraitW * scale);
            int h = Math.round(portraitH * scale);
            portraitX += (portraitW - w) / 2 + Math.round(18.0F * (1.0F - settle));
            portraitY += (portraitH - h) / 2 + Math.round(7.0F * (1.0F - settle));
            portraitW = w;
            portraitH = h;
        } else if (revealMotion) {
            float scale = 1.075F - 0.075F * settle;
            int w = Math.round(portraitW * scale);
            int h = Math.round(portraitH * scale);
            portraitX += (portraitW - w) / 2 + Math.round(22.0F * (1.0F - settle)) + shakeX;
            portraitY += (portraitH - h) / 2 + shakeY;
            portraitW = w;
            portraitH = h;
        }

        SummonLayerRenderer.portrait(g, pull.characterId(), silhouette,
                portraitX, portraitY, portraitW, portraitH);

        if (!silhouette && revealMotion) {
            int sweepW = Math.max(126, layout.portraitW() * 5 / 4);
            int sweepX = layout.portraitX() - sweepW
                    + Math.round((layout.portraitW() + sweepW * 2.0F) * SummonEasing.easeInOutCubic(progress));
            SummonLayerRenderer.sprite(g, SummonAssets.LIGHT_SWEEP, sweepX,
                    layout.portraitY() + layout.portraitH() / 4,
                    sweepW, Math.max(42, layout.portraitH() / 3));

            float flash = 1.0F - Math.min(1.0F, progress / 0.18F);
            int flashAlpha = Math.round((stars >= 5 ? 178 : stars >= 4 ? 112 : 70) * flash);
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
            int alpha = fadeIn(progress, 0.20F, 0.62F);
            s.text(g, "기록 해독 중", tx, ty,
                    withAlpha(GachaPresentationScreen.SECONDARY, alpha), false);
            g.fill(tx, ty + 18, tx + Math.min(layout.textW(), 110), ty + 19,
                    withAlpha(accent, alpha / 2));
            return;
        }

        int rarityAlpha = revealMotion ? fadeIn(progress, 0.10F, 0.34F) : 255;
        int nameAlpha = revealMotion ? fadeIn(progress, 0.28F, 0.58F) : 255;
        int detailAlpha = revealMotion ? fadeIn(progress, 0.52F, 0.86F) : 255;
        String headline = pull.newlyOwned() ? "새로운 동료" : "기록 재현";

        s.text(g, headline, tx, ty,
                withAlpha(pull.newlyOwned() ? GachaPresentationScreen.GREEN : GachaPresentationScreen.SECONDARY,
                        rarityAlpha), true);
        s.text(g, GachaPresentationScreen.stars(pull.stars()), tx, ty + 22,
                withAlpha(accent, rarityAlpha), true);
        g.fill(tx, ty + 42, tx + Math.min(layout.textW(), 148), ty + 44,
                withAlpha(accent, Math.max(0, rarityAlpha * 2 / 3)));
        s.text(g, GachaPresentationScreen.characterName(pull.characterId()), tx, ty + 54,
                withAlpha(GachaPresentationScreen.TEXT, nameAlpha), true);

        String result = pull.newlyOwned() ? "새로운 인연이 기록되었습니다"
                : GachaPresentationScreen.duplicateResult(pull);
        s.text(g, UiTextLayout.fit(result, Math.max(96, layout.textW())), tx, ty + 78,
                withAlpha(pull.newlyOwned() ? GachaPresentationScreen.SECONDARY : GachaPresentationScreen.PURPLE,
                        detailAlpha), false);

        if (!revealMotion) {
            String hint = s.flowView().lastPull()
                    ? (s.flowView().pullCount() <= 1 ? "Enter / Space · 확인" : "Enter / Space · 결과 보기")
                    : "Enter / Space · 다음";
            s.text(g, hint, tx, Math.min(s.screenHeight() - 46, ty + 108),
                    GachaPresentationScreen.MUTED, false);
        }
    }

    private static void drawCinematicHud(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        if (s.flowView().stage().ordinal() >= GachaPresentationFlow.Stage.SILHOUETTE.ordinal()) {
            String progress = (s.flowView().currentIndex() + 1) + " / " + Math.max(1, s.flowView().pullCount());
            int x = Math.max(14, s.screenWidth() - 102 - s.textWidth(progress));
            s.text(g, progress, x, 17, GachaPresentationScreen.SECONDARY, true);
        } else {
            s.text(g, "정령의 기록", 18, 17, GachaPresentationScreen.TEXT, true);
        }
    }

    private static void drawSummary(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        GachaPresentationScreen.Batch batch = s.batchView();
        int highest = 1;
        for (GachaPresentationScreen.Pull pull : batch.pulls()) highest = Math.max(highest, pull.stars());
        SummonLayerRenderer.fullScreen(g, SummonAssets.shards(highest), s.screenWidth(), s.screenHeight());

        String title = batch.pulls().size() <= 1 ? "소환 결과" : batch.pulls().size() + "회 소환 결과";
        s.text(g, title, 18, 17, GachaPresentationScreen.TEXT, true);
        s.text(g, "크리스탈 -" + batch.crystalSpent(), 18, 33, GachaPresentationScreen.SECONDARY, false);
        if (batch.pulls().size() <= 1) drawSingleSummary(s, g);
        else drawMultiSummary(s, g);
    }

    private static void drawSingleSummary(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        if (s.batchView().pulls().isEmpty()) return;
        GachaPresentationScreen.Pull pull = s.batchView().pulls().getFirst();
        CharacterLayout layout = characterLayout(s);
        int cx = layout.portraitX() + layout.portraitW() / 2;
        int cy = layout.portraitY() + layout.portraitH() / 2;
        SummonLayerRenderer.centered(g, SummonAssets.flare(pull.stars()), cx, cy,
                Math.min(s.screenWidth(), layout.portraitW() * 3), Math.max(64, layout.portraitH() / 3));
        SummonLayerRenderer.portrait(g, pull.characterId(), false,
                layout.portraitX(), layout.portraitY(), layout.portraitW(), layout.portraitH());
        int tx = layout.textX();
        int ty = layout.textY();
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

        int cols = count >= 8 ? 5 : count >= 5 ? 5 : Math.max(1, count);
        int rows = (count + cols - 1) / cols;
        int gap = s.screenWidth() < 520 ? 4 : 7;
        int top = 48;
        int side = s.screenWidth() < 520 ? 10 : 20;
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

            if (pull.stars() >= 5) {
                SummonLayerRenderer.centered(g, SummonAssets.FLARE_GOLD,
                        x + cardW / 2, y + cardH / 2, cardW + 42, Math.max(32, cardH / 2));
            }
            SummonLayerRenderer.sprite(g, SummonAssets.card(pull.stars()), x, y, cardW, cardH);

            int reservedText = 28;
            int maxPortraitH = Math.max(28, cardH - reservedText - 7);
            int maxPortraitW = Math.max(20, cardW - 10);
            int portraitW = Math.min(maxPortraitW, maxPortraitH * 2 / 3);
            int portraitH = portraitW * 3 / 2;
            if (portraitH > maxPortraitH) {
                portraitH = maxPortraitH;
                portraitW = portraitH * 2 / 3;
            }
            int px = x + (cardW - portraitW) / 2;
            int py = y + 5 + Math.max(0, (maxPortraitH - portraitH) / 2);
            SummonLayerRenderer.portrait(g, pull.characterId(), false, px, py, portraitW, portraitH);

            int labelY = y + cardH - 27;
            g.fill(x + 6, labelY - 2, x + cardW - 6, y + cardH - 6, 0xB8060A12);
            String characterName = UiTextLayout.fit(GachaPresentationScreen.characterName(pull.characterId()),
                    Math.max(26, cardW - 10));
            s.text(g, characterName, x + (cardW - s.textWidth(characterName)) / 2,
                    labelY, GachaPresentationScreen.TEXT, true);
            String rarity = GachaPresentationScreen.stars(pull.stars());
            s.text(g, rarity, x + (cardW - s.textWidth(rarity)) / 2,
                    labelY + 11, GachaPresentationScreen.starColor(pull.stars()), false);

            if (pull.newlyOwned()) {
                s.text(g, "NEW", x + 6, y + 7, GachaPresentationScreen.GOLD, true);
            }
        }
    }

    private static CharacterLayout characterLayout(GachaPresentationScreen s) {
        int margin = s.screenWidth() < 520 ? 14 : 30;
        int top = 30;
        int bottom = 38;
        int usableH = Math.max(130, s.screenHeight() - top - bottom);
        int maxPortraitW = Math.max(96, s.screenWidth() * (s.screenWidth() < 520 ? 38 : 44) / 100);
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
        int textY = top + Math.max(20, usableH / 2 - 57);
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

    private static float ambientWave(double seconds) {
        return (float) ((Math.sin(System.nanoTime() / 1_000_000_000.0 / seconds * Math.PI * 2.0) + 1.0) * 0.5);
    }

    private static int rarityShake(int stars, float progress, boolean horizontal) {
        if (stars < 4 || progress > 0.24F) return 0;
        float strength = (stars >= 5 ? 4.0F : 2.0F) * (1.0F - progress / 0.24F);
        double phase = System.nanoTime() / 14_000_000.0 + (horizontal ? 0.0 : 1.7);
        return Math.round((float) Math.sin(phase) * strength);
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
