package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.GachaPresentationFlow;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Visual-only renderer for the 2D summon cinematic. State ownership stays in GachaPresentationScreen/Flow. */
final class SummonPresentationRenderer {
    private SummonPresentationRenderer() {}

    static void render(GachaPresentationScreen screen, GuiGraphicsExtractor g) {
        GachaPresentationFlow flow = screen.flowView();
        if (flow.summary()) {
            drawSummary(screen, g);
            return;
        }
        switch (flow.stage()) {
            case RITUAL -> drawRitual(screen, g);
            case OMEN -> drawOmen(screen, g);
            case BREAK -> drawBreak(screen, g);
            case SILHOUETTE -> drawCharacter(screen, g, true, false);
            case REVEAL -> drawCharacter(screen, g, false, true);
            case HOLD -> drawCharacter(screen, g, false, false);
            case SUMMARY -> { }
        }
        drawCinematicHud(screen, g);
    }

    private static void drawRitual(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        GachaPresentationFlow f = s.flowView();
        float eased = SummonEasing.easeOutCubic(f.progress());
        int cx = s.screenWidth() / 2;
        int cy = s.screenHeight() / 2 + Math.round(8 * (1.0F - eased));
        int base = Math.max(132, Math.min(330, Math.min(s.screenWidth(), s.screenHeight()) * 62 / 100));
        int halo = Math.round(base * (0.84F + 0.16F * eased));
        SummonLayerRenderer.centered(g, SummonAssets.RADIAL_GLOW, cx, cy, halo + 58, halo + 58);
        SummonLayerRenderer.centered(g, SummonAssets.RUNE_CIRCLE, cx, cy, halo, halo);
        SummonLayerRenderer.centered(g, SummonAssets.ORBIT_B, cx, cy, halo + 12, halo + 12);
        int crystalH = Math.round(halo * 0.72F);
        SummonLayerRenderer.centered(g, SummonAssets.CRYSTAL, cx, cy, Math.round(crystalH * 0.70F), crystalH);
        SummonLayerRenderer.centered(g, SummonAssets.ORBIT_A, cx, cy, halo + 26, halo + 26);
        centerText(s, g, "기록이 공명합니다", Math.min(s.screenHeight() - 44, cy + halo / 2 + 8),
                GachaPresentationScreen.SECONDARY, true);
    }

    private static void drawOmen(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        GachaPresentationFlow f = s.flowView();
        int cx = s.screenWidth() / 2;
        int cy = s.screenHeight() / 2;
        int base = Math.max(150, Math.min(360, Math.min(s.screenWidth(), s.screenHeight()) * 68 / 100));
        float pulse = 0.94F + 0.08F * SummonEasing.pulse(f.progress());
        int halo = Math.round(base * pulse);
        SummonLayerRenderer.centered(g, SummonAssets.OMEN_VIOLET, cx, cy, halo + 92, halo + 92);
        SummonLayerRenderer.centered(g, SummonAssets.RUNE_CIRCLE, cx, cy, halo, halo);
        SummonLayerRenderer.centered(g, SummonAssets.ORBIT_B, cx, cy, halo + 18, halo + 18);
        int crystalH = Math.round(halo * 0.74F);
        SummonLayerRenderer.centered(g, SummonAssets.CRYSTAL, cx, cy, Math.round(crystalH * 0.70F), crystalH);
        SummonLayerRenderer.centered(g, SummonAssets.ORBIT_A, cx, cy, halo + 32, halo + 32);
        centerText(s, g, f.omenStars() >= 4 ? "강한 공명이 접근합니다" : "기록이 응답합니다",
                Math.min(s.screenHeight() - 44, cy + halo / 2 + 10), GachaPresentationScreen.TEXT, true);
    }

    private static void drawBreak(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        GachaPresentationFlow f = s.flowView();
        int cx = s.screenWidth() / 2;
        int cy = s.screenHeight() / 2;
        int base = Math.max(162, Math.min(380, Math.min(s.screenWidth(), s.screenHeight()) * 70 / 100));
        boolean gold = f.highestStars() >= 5 && f.fiveStarUpgrade();
        SummonLayerRenderer.centered(g, gold ? SummonAssets.BREAK_GOLD : SummonAssets.OMEN_VIOLET,
                cx, cy, base + 116, base + 116);
        SummonLayerRenderer.centered(g, SummonAssets.RUNE_CIRCLE, cx, cy, base, base);
        SummonLayerRenderer.centered(g, SummonAssets.ORBIT_A, cx, cy, base + 34, base + 34);
        int crystalH = Math.round(base * 0.75F);
        SummonLayerRenderer.centered(g, SummonAssets.CRYSTAL, cx, cy, Math.round(crystalH * 0.70F), crystalH);
        if (gold) {
            int local = Math.max(0, f.stageTick() - 5);
            if (local <= 1) g.fill(0, 0, s.screenWidth(), s.screenHeight(), 0x74FFF1C2);
            centerText(s, g, "★★★★★", cy + base / 2 - 14, GachaPresentationScreen.GOLD, true);
        }
    }

    private static void drawCharacter(GachaPresentationScreen s, GuiGraphicsExtractor g,
                                      boolean silhouette, boolean revealMotion) {
        GachaPresentationScreen.Pull pull = s.currentPull();
        if (pull == null) return;
        GachaPresentationFlow f = s.flowView();
        SummonVisualSpec.Spec visual = SummonVisualSpec.forStars(pull.stars());
        CharacterLayout layout = characterLayout(s);
        int glowSize = Math.round(Math.min(layout.portraitH(), layout.portraitW()) * visual.glowScale() * 1.30F);
        int glowX = layout.portraitX() + layout.portraitW() / 2;
        int glowY = layout.portraitY() + layout.portraitH() / 2;
        SummonLayerRenderer.centered(g, pull.stars() >= 5 ? SummonAssets.BREAK_GOLD
                        : pull.stars() >= 4 ? SummonAssets.OMEN_VIOLET : SummonAssets.RADIAL_GLOW,
                glowX, glowY, glowSize, glowSize);
        int rune = Math.max(112, Math.min(layout.portraitH() - 8, layout.portraitW() + 58));
        SummonLayerRenderer.centered(g, SummonAssets.RUNE_CIRCLE, glowX, glowY, rune, rune);
        if (visual.rareFx()) SummonLayerRenderer.centered(g, SummonAssets.ORBIT_A, glowX, glowY, rune + 24, rune + 24);

        int portraitW = Math.round(layout.portraitW() * visual.portraitScale());
        int portraitH = Math.round(layout.portraitH() * visual.portraitScale());
        int portraitX = layout.portraitX() + (layout.portraitW() - portraitW) / 2;
        int portraitY = layout.portraitY() + (layout.portraitH() - portraitH) / 2;
        if (revealMotion) {
            float settle = SummonEasing.backOut(f.progress());
            float scale = 1.10F - 0.10F * settle;
            int w = Math.round(portraitW * scale);
            int h = Math.round(portraitH * scale);
            portraitX += (portraitW - w) / 2;
            portraitY += (portraitH - h) / 2;
            portraitW = w;
            portraitH = h;
        }
        SummonLayerRenderer.portrait(g, pull.characterId(), silhouette, portraitX, portraitY, portraitW, portraitH);
        if (!silhouette && revealMotion) {
            int sweepW = Math.max(96, layout.portraitW() * 3 / 4);
            int sweepX = layout.portraitX() - sweepW / 2
                    + Math.round((layout.portraitW() + sweepW) * SummonEasing.easeInOutCubic(f.progress()));
            SummonLayerRenderer.sprite(g, SummonAssets.LIGHT_SWEEP, sweepX,
                    layout.portraitY() + layout.portraitH() / 3, sweepW, Math.max(28, layout.portraitH() / 6));
        }
        drawRevealCopy(s, g, pull, layout, silhouette, revealMotion);
    }

    private static void drawRevealCopy(GachaPresentationScreen s, GuiGraphicsExtractor g,
                                       GachaPresentationScreen.Pull pull, CharacterLayout layout,
                                       boolean silhouette, boolean revealMotion) {
        int tx = layout.textX();
        int ty = layout.textY();
        int accent = GachaPresentationScreen.starColor(pull.stars());
        if (silhouette) {
            s.text(g, "기록 해독 중", tx, ty, GachaPresentationScreen.SECONDARY, false);
            s.text(g, GachaPresentationScreen.stars(pull.stars()), tx, ty + 22, accent, true);
            s.text(g, "형상이 기록에 응답합니다", tx, ty + 44, GachaPresentationScreen.TEXT, true);
            return;
        }
        String headline = pull.newlyOwned() ? "새로운 동료" : "기록 재현";
        s.text(g, headline, tx, ty, pull.newlyOwned() ? GachaPresentationScreen.GREEN : GachaPresentationScreen.SECONDARY, true);
        s.text(g, GachaPresentationScreen.stars(pull.stars()), tx, ty + 23, accent, true);
        if (!revealMotion || s.flowView().progress() > 0.28F) {
            s.text(g, GachaPresentationScreen.characterName(pull.characterId()), tx, ty + 46, GachaPresentationScreen.TEXT, true);
        }
        if (!revealMotion || s.flowView().progress() > 0.56F) {
            String result = pull.newlyOwned() ? "정령의 기록에 새로운 인연이 새겨졌습니다"
                    : GachaPresentationScreen.duplicateResult(pull);
            s.text(g, UiTextLayout.fit(result, Math.max(100, layout.textW())), tx, ty + 68,
                    pull.newlyOwned() ? GachaPresentationScreen.SECONDARY : GachaPresentationScreen.PURPLE, false);
        }
        if (!revealMotion) {
            String hint = s.flowView().lastPull()
                    ? (s.flowView().pullCount() <= 1 ? "Enter / Space · 확인" : "Enter / Space · 결과 보기")
                    : "Enter / Space · 다음";
            s.text(g, hint, tx, Math.min(s.screenHeight() - 54, ty + 98), GachaPresentationScreen.MUTED, false);
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
        s.text(g, "크리스탈 -" + batch.crystalSpent(), 18, 34, GachaPresentationScreen.SECONDARY, false);
        if (batch.pulls().size() <= 1) drawSingleSummary(s, g);
        else drawMultiSummary(s, g);
    }

    private static void drawSingleSummary(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        if (s.batchView().pulls().isEmpty()) return;
        GachaPresentationScreen.Pull pull = s.batchView().pulls().getFirst();
        int w = Math.min(620, s.screenWidth() - 48);
        int h = Math.min(350, s.screenHeight() - 94);
        int x = (s.screenWidth() - w) / 2;
        int y = 54 + Math.max(0, (s.screenHeight() - 54 - h) / 2);
        int portraitW = Math.min(w * 52 / 100, 260);
        int portraitH = h - 24;
        int px = x + w - portraitW - 10;
        SummonLayerRenderer.centered(g, pull.stars() >= 5 ? SummonAssets.BREAK_GOLD : SummonAssets.RADIAL_GLOW,
                px + portraitW / 2, y + h / 2, portraitW + 70, portraitW + 70);
        SummonLayerRenderer.portrait(g, pull.characterId(), false, px, y + 8, portraitW, portraitH);
        int tx = x + 14;
        int ty = y + 54;
        int accent = GachaPresentationScreen.starColor(pull.stars());
        s.text(g, GachaPresentationScreen.stars(pull.stars()), tx, ty, accent, true);
        s.text(g, GachaPresentationScreen.characterName(pull.characterId()), tx, ty + 26, GachaPresentationScreen.TEXT, true);
        s.text(g, pull.newlyOwned() ? "새로운 동료" : GachaPresentationScreen.duplicateResult(pull), tx, ty + 50,
                pull.newlyOwned() ? GachaPresentationScreen.GREEN : GachaPresentationScreen.PURPLE, false);
        s.text(g, "Enter / Space · 닫기", tx, ty + 88, GachaPresentationScreen.MUTED, false);
    }

    private static void drawMultiSummary(GachaPresentationScreen s, GuiGraphicsExtractor g) {
        int count = s.batchView().pulls().size();
        int cols = s.screenWidth() >= 700 ? 5 : s.screenWidth() >= 470 ? 4 : 2;
        cols = Math.min(cols, Math.max(1, count));
        int rows = (count + cols - 1) / cols;
        int gap = s.screenWidth() >= 700 ? 7 : 5;
        int top = 56;
        int availableW = s.screenWidth() - 28;
        int availableH = s.screenHeight() - top - 38;
        int cardW = Math.max(74, Math.min(154, (availableW - gap * (cols - 1)) / cols));
        int cardH = Math.max(82, Math.min(190, (availableH - gap * (rows - 1)) / rows));
        int gridW = cols * cardW + (cols - 1) * gap;
        int gridH = rows * cardH + (rows - 1) * gap;
        int startX = (s.screenWidth() - gridW) / 2;
        int startY = top + Math.max(0, (availableH - gridH) / 2);
        for (int i = 0; i < count; i++) {
            GachaPresentationScreen.Pull pull = s.batchView().pulls().get(i);
            int x = startX + (i % cols) * (cardW + gap);
            int y = startY + (i / cols) * (cardH + gap);
            SummonLayerRenderer.sprite(g, SummonAssets.card(pull.stars()), x, y, cardW, cardH);
            int portraitH = Math.max(38, cardH - 48);
            int portraitW = Math.max(34, Math.min(cardW - 14, portraitH * 2 / 3));
            int px = x + (cardW - portraitW) / 2;
            SummonLayerRenderer.portrait(g, pull.characterId(), false, px, y + 6, portraitW, portraitH);
            int textW = Math.max(32, cardW - 12);
            String characterName = UiTextLayout.fit(GachaPresentationScreen.characterName(pull.characterId()), textW);
            int nameY = y + cardH - 38;
            s.text(g, characterName, x + (cardW - s.textWidth(characterName)) / 2, nameY, GachaPresentationScreen.TEXT, true);
            String rarity = GachaPresentationScreen.stars(pull.stars());
            s.text(g, rarity, x + (cardW - s.textWidth(rarity)) / 2, nameY + 12,
                    GachaPresentationScreen.starColor(pull.stars()), false);
            if (pull.newlyOwned()) {
                SummonLayerRenderer.sprite(g, SummonAssets.NEW_TAG, x + cardW - 46, y + 5, 42, 15);
                s.text(g, "NEW", x + cardW - 42, y + 8, GachaPresentationScreen.GOLD, true);
            }
        }
    }

    private static CharacterLayout characterLayout(GachaPresentationScreen s) {
        int margin = s.screenWidth() < 460 ? 12 : 28;
        int top = 38;
        int usableH = Math.max(150, s.screenHeight() - 34 - top);
        boolean compact = s.screenWidth() < 520;
        int portraitW = compact ? Math.max(120, s.screenWidth() * 48 / 100)
                : Math.min(360, s.screenWidth() * 48 / 100);
        int portraitX = s.screenWidth() - margin - portraitW;
        int textX = margin;
        int textW = Math.max(96, portraitX - margin - textX - 12);
        int textY = top + Math.max(20, usableH / 2 - 64);
        if (compact) {
            portraitW = Math.max(118, s.screenWidth() * 52 / 100);
            portraitX = s.screenWidth() - margin - portraitW;
            textW = Math.max(88, portraitX - margin - textX - 8);
        }
        return new CharacterLayout(portraitX, top, portraitW, usableH, textX, textY, textW);
    }

    private static void centerText(GachaPresentationScreen s, GuiGraphicsExtractor g,
                                   String text, int y, int color, boolean shadow) {
        s.text(g, text, s.screenWidth() / 2 - s.textWidth(text) / 2, y, color, shadow);
    }

    private record CharacterLayout(int portraitX, int portraitY, int portraitW, int portraitH,
                                   int textX, int textY, int textW) { }
}
