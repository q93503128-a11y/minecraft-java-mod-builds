package dev.moonseungjun.testmod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class GachaCinematicScreen extends Screen {
    private static final long LOCKS_START = 550L;
    private static final long HINT_START = 1550L;
    private static final long OPEN_START = 2700L;
    private static final long LEGENDARY_BREAK = 3400L;
    private static final long TRANSITION_START = 3900L;
    private static final long REVEAL_START = 4700L;
    private static final long FINAL_START = 5900L;

    private final Rarity rarity;
    private final long startedNanos = System.nanoTime();
    private boolean lockSound;
    private boolean hintSound;
    private boolean gateSound;
    private boolean breakSound;
    private boolean revealSound;

    public GachaCinematicScreen(Rarity rarity) {
        super(Component.translatable("screen.moon_test.gacha"));
        this.rarity = rarity;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        long elapsed = elapsedMs();
        playTimelineSounds(elapsed);

        int cx = width / 2;
        int cy = height / 2;
        int min = Math.min(width, height);

        int accent = accentAt(elapsed);
        drawBackdrop(graphics, cx, cy, min, elapsed, accent);

        if (elapsed < TRANSITION_START + 550L) {
            drawVault(graphics, cx, cy, min, elapsed, accent);
        }

        if (elapsed >= TRANSITION_START - 250L) {
            drawTransition(graphics, cx, cy, min, elapsed, accent);
        }

        if (elapsed >= REVEAL_START - 150L) {
            drawReveal(graphics, cx, cy, min, elapsed, accent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawBackdrop(GuiGraphicsExtractor graphics, int cx, int cy, int min, long elapsed, int accent) {
        graphics.fillGradient(0, 0, width, height, 0xFF04060D, 0xFF090D18);

        double pulse = 0.5 + 0.5 * Math.sin(elapsed / 420.0);
        for (int i = 0; i < 14; i++) {
            float angle = (float) (Math.PI * 2.0 * i / 14.0 + elapsed * 0.00012);
            int alpha = 8 + (int) (pulse * 10);
            drawBeam(graphics, cx, cy, angle, min, 2 + (i % 3), withAlpha(accent, alpha));
        }

        int depthCount = 7;
        double travel = (elapsed % 1700L) / 1700.0;
        for (int i = 0; i < depthCount; i++) {
            double p = (i + travel) / depthCount;
            int size = (int) (min * (0.20 + p * 0.72));
            int alpha = Math.max(5, (int) (26 * (1.0 - p)));
            graphics.outline(cx - size / 2, cy - size / 2, size, size, withAlpha(accent, alpha));
        }

        int vignette = Math.max(18, min / 8);
        graphics.fillGradient(0, 0, width, vignette, 0xF0000000, 0x00000000);
        graphics.fillGradient(0, height - vignette, width, height, 0x00000000, 0xF0000000);
        graphics.fill(0, 0, Math.max(10, width / 14), height, 0xA8000000);
        graphics.fill(width - Math.max(10, width / 14), 0, width, height, 0xA8000000);
    }

    private void drawVault(GuiGraphicsExtractor graphics, int cx, int cy, int min, long elapsed, int accent) {
        int door = Math.max(118, (int) (min * 0.54));
        int frame = door + Math.max(22, min / 14);

        double entrance = easeOut(clamp(elapsed / 780.0));
        float scale = (float) (0.72 + entrance * 0.28);

        float shakeX = 0.0F;
        float shakeY = 0.0F;
        if (rarity == Rarity.LEGENDARY && elapsed >= OPEN_START && elapsed < LEGENDARY_BREAK) {
            double pressure = clamp((elapsed - OPEN_START) / (double) (LEGENDARY_BREAK - OPEN_START));
            shakeX = (float) (Math.sin(elapsed * 0.085) * 4.0 * pressure);
            shakeY = (float) (Math.cos(elapsed * 0.071) * 2.0 * pressure);
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx + shakeX, cy + shakeY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-cx, -cy);

        drawOuterFrame(graphics, cx, cy, frame, elapsed, accent);
        drawPortalCore(graphics, cx, cy, door - 12, elapsed, accent);
        drawDoorPanels(graphics, cx, cy, door, elapsed, accent);
        drawLocks(graphics, cx, cy, frame, elapsed, accent);

        graphics.pose().popMatrix();
    }

    private void drawOuterFrame(GuiGraphicsExtractor graphics, int cx, int cy, int frame, long elapsed, int accent) {
        int x = cx - frame / 2;
        int y = cy - frame / 2;
        graphics.fill(x, y, x + frame, y + frame, 0xFF0B0E16);
        graphics.outline(x, y, frame, frame, 0xFF31394A);
        graphics.outline(x + 5, y + 5, frame - 10, frame - 10, withAlpha(accent, 90));
        graphics.outline(x + 12, y + 12, frame - 24, frame - 24, 0xFF151B28);

        float spin = (float) (elapsed * 0.00055);
        for (int i = 0; i < 4; i++) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(cx, cy);
            graphics.pose().rotate(spin + (float) (Math.PI * 0.5 * i));
            int arm = frame / 2 + 9;
            graphics.fill(-3, -arm, 3, -frame / 2 + 7, withAlpha(accent, 64));
            graphics.fill(-7, -arm + 3, 7, -arm + 8, 0xFF283044);
            graphics.pose().popMatrix();
        }
    }

    private void drawPortalCore(GuiGraphicsExtractor graphics, int cx, int cy, int size, long elapsed, int accent) {
        int half = size / 2;
        int portalAlpha = elapsed < OPEN_START ? 25 : 90 + (int) (100 * clamp((elapsed - OPEN_START) / 900.0));
        graphics.fillGradient(cx - half, cy - half, cx + half, cy + half,
                withAlpha(accent, Math.min(210, portalAlpha)), 0xFF02040A);

        if (elapsed > HINT_START) {
            double pulse = 0.5 + 0.5 * Math.sin(elapsed / 115.0);
            int slit = 2 + (int) (pulse * 3);
            graphics.fill(cx - slit, cy - half + 8, cx + slit, cy + half - 8, withAlpha(accent, 160));
            graphics.fill(cx - 1, cy - half + 8, cx + 1, cy + half - 8, 0xFFF7FCFF);
        }
    }

    private void drawDoorPanels(GuiGraphicsExtractor graphics, int cx, int cy, int door, long elapsed, int accent) {
        int half = door / 2;
        int panelW = half - 2;
        int panelH = door;

        double normalOpen = easeInOut(clamp((elapsed - OPEN_START) / 720.0));
        boolean legendary = rarity == Rarity.LEGENDARY;
        double blast = legendary ? easeOut(clamp((elapsed - LEGENDARY_BREAK) / 520.0)) : 0.0;
        double open = legendary ? 0.0 : normalOpen;

        float leftCenterX = (float) (cx - panelW / 2.0 - open * door * 0.54 - blast * door * 0.90);
        float rightCenterX = (float) (cx + panelW / 2.0 + open * door * 0.54 + blast * door * 0.90);
        float leftY = (float) (cy - blast * door * 0.22);
        float rightY = (float) (cy + blast * door * 0.18);
        float rotation = (float) (blast * 0.48);

        drawDoorPanel(graphics, leftCenterX, leftY, panelW, panelH, -rotation, accent, false);
        drawDoorPanel(graphics, rightCenterX, rightY, panelW, panelH, rotation, accent, true);

        if (legendary && blast > 0.0) {
            for (int i = 0; i < 10; i++) {
                float angle = (float) (Math.PI * 2.0 * i / 10.0 + 0.22);
                float distance = (float) (door * (0.18 + blast * (0.75 + (i % 3) * 0.08)));
                float sx = cx + (float) Math.cos(angle) * distance;
                float sy = cy + (float) Math.sin(angle) * distance;
                graphics.pose().pushMatrix();
                graphics.pose().translate(sx, sy);
                graphics.pose().rotate(angle + (float) blast * 1.2F);
                int len = 10 + (i % 4) * 4;
                graphics.fill(-len, -2, len, 2, withAlpha(accent, (int) (210 * (1.0 - blast * 0.45))));
                graphics.pose().popMatrix();
            }
        }
    }

    private void drawDoorPanel(GuiGraphicsExtractor graphics, float centerX, float centerY, int panelW, int panelH,
                               float rotation, int accent, boolean mirror) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().rotate(rotation);

        int x1 = -panelW / 2;
        int y1 = -panelH / 2;
        int x2 = panelW / 2;
        int y2 = panelH / 2;
        graphics.fillGradient(x1, y1, x2, y2, mirror ? 0xFF171C28 : 0xFF252C3A, 0xFF080B12);
        graphics.outline(x1, y1, panelW, panelH, 0xFF424D61);
        graphics.outline(x1 + 5, y1 + 5, panelW - 10, panelH - 10, withAlpha(accent, 48));

        int ribStep = Math.max(14, panelW / 4);
        for (int x = x1 + ribStep; x < x2; x += ribStep) {
            graphics.verticalLine(x, y1 + 8, y2 - 8, 0xFF343D50);
        }
        for (int y = y1 + 18; y < y2 - 10; y += 20) {
            graphics.horizontalLine(x1 + 8, x2 - 8, y, 0xFF111722);
        }

        graphics.fill(mirror ? x1 + 7 : x2 - 12, y1 + 10,
                mirror ? x1 + 11 : x2 - 8, y2 - 10, withAlpha(accent, 80));
        graphics.pose().popMatrix();
    }

    private void drawLocks(GuiGraphicsExtractor graphics, int cx, int cy, int frame, long elapsed, int accent) {
        int radius = frame / 2 + 18;
        for (int i = 0; i < 4; i++) {
            long unlockAt = LOCKS_START + i * 260L;
            boolean unlocked = elapsed >= unlockAt;
            double angle = Math.PI * 0.5 * i - Math.PI * 0.5;
            int x = cx + (int) (Math.cos(angle) * radius);
            int y = cy + (int) (Math.sin(angle) * radius);
            int size = unlocked ? 12 : 16;
            int color = unlocked ? withAlpha(accent, 220) : 0xFF273041;
            graphics.fill(x - size / 2, y - size / 2, x + size / 2, y + size / 2, 0xFF080B11);
            graphics.outline(x - size / 2, y - size / 2, size, size, color);
            if (unlocked) {
                graphics.fill(x - 2, y - 2, x + 2, y + 2, color);
            }
        }
    }

    private void drawTransition(GuiGraphicsExtractor graphics, int cx, int cy, int min, long elapsed, int accent) {
        double t = clamp((elapsed - (TRANSITION_START - 250L)) / 850.0);
        if (t <= 0.0) {
            return;
        }

        int flashAlpha = (int) (220 * Math.sin(Math.PI * Math.min(1.0, t)));
        int radius = (int) (min * (0.04 + easeOut(t) * 0.72));
        graphics.fill(cx - radius, cy - radius, cx + radius, cy + radius, withAlpha(accent, flashAlpha));
        graphics.outline(cx - radius, cy - radius, radius * 2, radius * 2, withAlpha(0xFFFFFF, flashAlpha));

        for (int i = 0; i < 18; i++) {
            float angle = (float) (Math.PI * 2.0 * i / 18.0);
            int alpha = (int) (130 * (1.0 - t));
            drawBeam(graphics, cx, cy, angle, (int) (min * (0.3 + t)), 2 + i % 2, withAlpha(accent, alpha));
        }

        if (t > 0.62) {
            int black = (int) (255 * clamp((t - 0.62) / 0.38));
            graphics.fill(0, 0, width, height, black << 24);
        }
    }

    private void drawReveal(GuiGraphicsExtractor graphics, int cx, int cy, int min, long elapsed, int accent) {
        double appear = easeOut(clamp((elapsed - (REVEAL_START - 150L)) / 700.0));
        int silhouetteHeight = Math.max(120, (int) (min * 0.52));
        int baseY = cy + silhouetteHeight / 2;

        int glowW = (int) (silhouetteHeight * (0.24 + 0.18 * appear));
        graphics.fillGradient(cx - glowW, cy - silhouetteHeight / 2,
                cx + glowW, baseY, withAlpha(accent, (int) (80 * appear)), 0x00000000);

        drawHumanoid(graphics, cx, baseY, silhouetteHeight, 0xFF030408, 0xFF11141C);

        double scan = clamp((elapsed - REVEAL_START) / 900.0);
        int scanY = baseY - (int) (silhouetteHeight * scan);
        if (scan > 0.0) {
            graphics.enableScissor(cx - silhouetteHeight / 2, scanY, cx + silhouetteHeight / 2, baseY + 4);
            drawHumanoidOutline(graphics, cx, baseY, silhouetteHeight, withAlpha(accent, 235));
            graphics.disableScissor();
            graphics.fill(cx - silhouetteHeight / 3, scanY - 1, cx + silhouetteHeight / 3, scanY + 1,
                    withAlpha(0xFFFFFF, 210));
        }

        if (elapsed >= FINAL_START) {
            double finalT = easeOut(clamp((elapsed - FINAL_START) / 500.0));
            int alpha = (int) (255 * finalT);
            int titleY = baseY + 22;
            String stars = rarity.stars;
            centeredText(graphics, stars, titleY, withAlpha(accent, alpha), true);
            centeredText(graphics, rarity.label, titleY + 15, withAlpha(0xFFFFFF, alpha), true);

            int lineW = (int) (min * 0.18 * finalT);
            graphics.fill(cx - lineW, titleY - 8, cx + lineW, titleY - 7, withAlpha(accent, alpha / 2));
        }
    }

    private void drawHumanoid(GuiGraphicsExtractor graphics, int cx, int baseY, int h, int bodyColor, int edgeColor) {
        int head = Math.max(18, h / 7);
        int torsoW = Math.max(28, h / 5);
        int torsoH = Math.max(42, h / 3);
        int legH = Math.max(35, h / 4);
        int headY = baseY - legH - torsoH - head - 8;
        int torsoY = headY + head + 6;

        graphics.fill(cx - head / 2, headY, cx + head / 2, headY + head, bodyColor);
        graphics.outline(cx - head / 2, headY, head, head, edgeColor);
        graphics.fill(cx - torsoW / 2, torsoY, cx + torsoW / 2, torsoY + torsoH, bodyColor);
        graphics.outline(cx - torsoW / 2, torsoY, torsoW, torsoH, edgeColor);

        drawLimb(graphics, cx - torsoW / 2 - 5, torsoY + 8, 7, torsoH - 3, -0.13F, bodyColor);
        drawLimb(graphics, cx + torsoW / 2 + 5, torsoY + 8, 7, torsoH - 3, 0.13F, bodyColor);
        drawLimb(graphics, cx - torsoW / 4, torsoY + torsoH - 1, 9, legH, 0.05F, bodyColor);
        drawLimb(graphics, cx + torsoW / 4, torsoY + torsoH - 1, 9, legH, -0.05F, bodyColor);
    }

    private void drawHumanoidOutline(GuiGraphicsExtractor graphics, int cx, int baseY, int h, int color) {
        int head = Math.max(18, h / 7);
        int torsoW = Math.max(28, h / 5);
        int torsoH = Math.max(42, h / 3);
        int legH = Math.max(35, h / 4);
        int headY = baseY - legH - torsoH - head - 8;
        int torsoY = headY + head + 6;
        graphics.outline(cx - head / 2 - 2, headY - 2, head + 4, head + 4, color);
        graphics.outline(cx - torsoW / 2 - 2, torsoY - 2, torsoW + 4, torsoH + 4, color);
        graphics.fill(cx - 1, headY - 10, cx + 1, baseY + 3, withAlpha(color & 0xFFFFFF, 70));
    }

    private void drawLimb(GuiGraphicsExtractor graphics, float x, float y, int w, int h, float rotation, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().rotate(rotation);
        graphics.fill(-w / 2, 0, w / 2, h, color);
        graphics.pose().popMatrix();
    }

    private void drawBeam(GuiGraphicsExtractor graphics, int cx, int cy, float angle, int length, int thickness, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().rotate(angle);
        graphics.fill(18, -thickness / 2, length, Math.max(1, thickness / 2), color);
        graphics.pose().popMatrix();
    }

    private void centeredText(GuiGraphicsExtractor graphics, String text, int y, int color, boolean shadow) {
        int x = width / 2 - minecraft.font.width(text) / 2;
        graphics.text(minecraft.font, text, x, y, color, shadow);
    }

    private void playTimelineSounds(long elapsed) {
        if (minecraft == null) {
            return;
        }
        if (!lockSound && elapsed >= LOCKS_START) {
            lockSound = true;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.68F));
        }
        if (!hintSound && elapsed >= HINT_START) {
            hintSound = true;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.35F));
        }
        if (!gateSound && elapsed >= OPEN_START) {
            gateSound = true;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.72F));
        }
        if (rarity == Rarity.LEGENDARY && !breakSound && elapsed >= LEGENDARY_BREAK) {
            breakSound = true;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_SONIC_BOOM, 0.92F));
        }
        if (!revealSound && elapsed >= REVEAL_START) {
            revealSound = true;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.TOTEM_USE, rarity == Rarity.LEGENDARY ? 1.12F : 0.92F));
        }
    }

    private int accentAt(long elapsed) {
        if (rarity != Rarity.LEGENDARY) {
            return rarity.color;
        }
        if (elapsed < LEGENDARY_BREAK) {
            return 0xA98BFF;
        }
        double t = easeOut(clamp((elapsed - LEGENDARY_BREAK) / 420.0));
        return mixRgb(0xA98BFF, 0xFFD86A, t);
    }

    private long elapsedMs() {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private static int mixRgb(int a, int b, double t) {
        t = clamp(t);
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = (int) Math.round(ar + (br - ar) * t);
        int g = (int) Math.round(ag + (bg - ag) * t);
        int bl = (int) Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double easeOut(double t) {
        t = clamp(t);
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    private static double easeInOut(double t) {
        t = clamp(t);
        return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
    }

    public enum Rarity {
        NORMAL(0x67D8FF, "★★★", "일반 공명"),
        RARE(0xB889FF, "★★★★", "희귀 공명"),
        LEGENDARY(0xFFD86A, "★★★★★", "최고 등급 공명");

        private final int color;
        private final String stars;
        private final String label;

        Rarity(int color, String stars, String label) {
            this.color = color;
            this.stars = stars;
            this.label = label;
        }
    }
}
