package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public final class MusicPlayerScreen extends Screen {
    private static final double CARD_STEP = 43.0;
    private static final double MAX_VISIBLE_DISTANCE = 2.15;

    private int panelX, panelY, panelWidth, panelHeight;
    private int wheelX, wheelY, wheelWidth, wheelHeight;
    private int infoX, infoY, infoWidth, infoHeight;

    private double carouselPosition;
    private double scrollVelocity;
    private long lastFrameNanos;

    private MusicTextureButton playButton;
    private MusicTextureButton repeatButton;

    public MusicPlayerScreen() {
        super(Component.translatable("screen.campfiresessions.music.title"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(500, Math.max(320, width - 28));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 8));
        panelHeight = Math.min(270, Math.max(222, height - 30));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 8));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        infoWidth = Math.max(132, panelWidth / 3);
        wheelX = panelX + 18;
        wheelY = panelY + 55;
        infoX = panelX + panelWidth - infoWidth - 18;
        infoY = wheelY;
        wheelWidth = Math.max(138, infoX - wheelX - 15);
        wheelHeight = Math.max(122, panelHeight - 108);
        infoHeight = wheelHeight;

        carouselPosition = CampfireMusicClient.selectedIndex();
        scrollVelocity = 0.0;
        lastFrameNanos = System.nanoTime();

        int buttonY = panelY + panelHeight - 34;
        int gap = 6;
        int prevW = 58;
        int playW = 72;
        int nextW = 64;
        int repeatW = 58;
        int groupW = prevW + playW + nextW + repeatW + gap * 3;
        int startX = panelX + (panelWidth - groupW) / 2;

        addRenderableWidget(new MusicTextureButton(
                startX, buttonY, prevW, 22, Component.literal("◀ PREV"),
                MusicTextureButton.Style.SECONDARY, b -> nudge(-1)));

        playButton = addRenderableWidget(new MusicTextureButton(
                startX + prevW + gap, buttonY, playW, 22, Component.literal("▶ PLAY"),
                MusicTextureButton.Style.PRIMARY, b -> CampfireMusicClient.toggleSelected()));

        addRenderableWidget(new MusicTextureButton(
                startX + prevW + playW + gap * 2, buttonY, nextW, 22, Component.literal("NEXT ▶"),
                MusicTextureButton.Style.SECONDARY, b -> nudge(1)));

        repeatButton = addRenderableWidget(new MusicTextureButton(
                startX + prevW + playW + nextW + gap * 3, buttonY, repeatW, 22, Component.literal("↻ OFF"),
                MusicTextureButton.Style.SECONDARY, b -> CampfireMusicClient.toggleRepeatOne()));

        addRenderableWidget(new MusicTextureButton(
                panelX + panelWidth - 31, panelY + 12, 18, 18, Component.literal("×"),
                MusicTextureButton.Style.DANGER, b -> onClose()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside(mouseX, mouseY, wheelX, wheelY, wheelWidth, wheelHeight) && scrollY != 0.0) {
            scrollVelocity += -scrollY * 4.8;
            scrollVelocity = Math.max(-10.0, Math.min(10.0, scrollVelocity));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && inside(event.x(), event.y(), wheelX, wheelY, wheelWidth, wheelHeight)) {
            for (int i = 0; i < MusicCatalog.TRACKS.size(); i++) {
                double diff = wrappedDifference(i);
                if (Math.abs(diff) > MAX_VISIBLE_DISTANCE) continue;
                CardBounds bounds = cardBounds(diff);
                if (inside(event.x(), event.y(), bounds.x(), bounds.y(), bounds.width(), bounds.height())) {
                    carouselPosition += diff;
                    scrollVelocity = 0.0;
                    CampfireMusicClient.previewSelect(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void nudge(int direction) {
        scrollVelocity = 0.0;
        carouselPosition = Math.rint(carouselPosition) + direction;
        CampfireMusicClient.previewSelect((int) Math.round(carouselPosition));
    }

    private void advanceCarousel() {
        long now = System.nanoTime();
        double dt = Math.min(0.05, Math.max(0.0, (now - lastFrameNanos) / 1_000_000_000.0));
        lastFrameNanos = now;

        if (Math.abs(scrollVelocity) > 0.035) {
            carouselPosition += scrollVelocity * dt;
            scrollVelocity *= Math.exp(-7.2 * dt);
        } else {
            scrollVelocity = 0.0;
            double target = Math.rint(carouselPosition);
            carouselPosition += (target - carouselPosition) * Math.min(1.0, dt * 18.0);
            if (Math.abs(target - carouselPosition) < 0.002) carouselPosition = target;
        }

        CampfireMusicClient.previewSelect((int) Math.round(carouselPosition));
    }

    private double wrappedDifference(int index) {
        int count = MusicCatalog.TRACKS.size();
        double normalized = carouselPosition % count;
        if (normalized < 0.0) normalized += count;
        double diff = index - normalized;
        double half = count / 2.0;
        while (diff > half) diff -= count;
        while (diff < -half) diff += count;
        return diff;
    }

    private CardBounds cardBounds(double diff) {
        double distance = Math.abs(diff);
        double focus = Math.max(0.0, 1.0 - Math.min(1.0, distance));
        int cardWidth = wheelWidth - (int) Math.round(Math.min(1.8, distance) * 24.0);
        int cardHeight = 29 + (int) Math.round(focus * 10.0);
        int x = wheelX + (wheelWidth - cardWidth) / 2;
        int centerY = wheelY + wheelHeight / 2;
        int y = centerY + (int) Math.round(diff * CARD_STEP) - cardHeight / 2;
        return new CardBounds(x, y, cardWidth, cardHeight);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blurBeforeThisStratum();
        graphics.fill(0, 0, width, height, 0x65000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        advanceCarousel();

        MusicTrack selected = CampfireMusicClient.selectedTrack();
        MusicTheme theme = selected.theme();

        playButton.setMessage(Component.literal(CampfireMusicClient.isPlayingSelected() ? "■ STOP" : "▶ PLAY"));
        repeatButton.setMessage(Component.literal(CampfireMusicClient.isRepeatOne() ? "↻ ONE" : "↻ OFF"));

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, theme.panelSprite(),
                panelX, panelY, panelWidth, panelHeight);
        // Keep the external UI art visible instead of covering it with a nearly opaque hand-drawn panel.
        graphics.fill(panelX + 9, panelY + 9, panelX + panelWidth - 9, panelY + panelHeight - 9, 0x29000000);

        graphics.text(font, "CAMPFIRE SESSIONS", panelX + 18, panelY + 15, theme.text(), true);
        graphics.text(font, Component.translatable("screen.campfiresessions.music.subtitle_v5"),
                panelX + 18, panelY + 31, theme.muted(), false);

        String mood = selected.theme().displayName().toUpperCase();
        graphics.text(font, mood, panelX + panelWidth - 44 - font.width(mood), panelY + 16, theme.accent(), true);

        drawCarousel(graphics);
        drawInfo(graphics, selected, theme);

        graphics.text(font, "B PREV  ·  N NEXT  ·  R REPEAT",
                panelX + 18, panelY + panelHeight - 49, 0xFFA9ADB0, false);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCarousel(GuiGraphicsExtractor graphics) {
        int centerY = wheelY + wheelHeight / 2;
        MusicTheme selectedTheme = CampfireMusicClient.selectedTrack().theme();
        graphics.fill(wheelX + 4, centerY - 22, wheelX + wheelWidth - 4, centerY - 21, 0x58FFFFFF);
        graphics.fill(wheelX + 4, centerY + 21, wheelX + wheelWidth - 4, centerY + 22, selectedTheme.accent());

        for (int i = 0; i < MusicCatalog.TRACKS.size(); i++) {
            double diff = wrappedDifference(i);
            double distance = Math.abs(diff);
            if (distance > MAX_VISIBLE_DISTANCE) continue;

            MusicTrack track = MusicCatalog.TRACKS.get(i);
            MusicTheme cardTheme = track.theme();
            CardBounds b = cardBounds(diff);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, cardTheme.cardSprite(),
                    b.x(), b.y(), b.width(), b.height());

            if (distance > 0.55) {
                int alpha = Math.min(0x88, 0x28 + (int) (distance * 0x24));
                graphics.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), alpha << 24);
            } else {
                graphics.fill(b.x() + 4, b.y() + b.height() - 3, b.x() + b.width() - 4,
                        b.y() + b.height() - 2, cardTheme.accent());
            }

            int duration = MusicMetadata.durationSeconds(track.id());
            String time = formatTime(duration);
            int timeW = font.width(time);
            int textX = b.x() + 12;
            int titleY = b.y() + (distance < 0.55 ? 7 : 10);
            int titleColor = distance < 0.55 ? cardTheme.text() : 0xFFD4D7D9;
            graphics.text(font, trim(track.title(), b.width() - 48 - timeW), textX, titleY, titleColor, distance < 0.55);
            graphics.text(font, time, b.x() + b.width() - 10 - timeW, titleY, cardTheme.muted(), false);

            if (distance < 0.55) {
                graphics.text(font, trim(track.artist() + " · " + track.subtitle(), b.width() - 35),
                        textX, b.y() + 21, cardTheme.muted(), false);
            }

            if (CampfireMusicClient.playingIndex() == i) {
                graphics.text(font, "♪", b.x() + b.width() - 17, b.y() + b.height() - 14, cardTheme.accent(), true);
            }
        }
    }

    private void drawInfo(GuiGraphicsExtractor graphics, MusicTrack selected, MusicTheme theme) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, theme.cardSprite(),
                infoX, infoY, infoWidth, infoHeight);
        graphics.fill(infoX + 6, infoY + 6, infoX + infoWidth - 6, infoY + infoHeight - 6, 0x25000000);

        int iconY = infoY + 13;
        graphics.item(ModItems.ACOUSTIC_GUITAR.get().getDefaultInstance(), infoX + 11, iconY);
        graphics.text(font, "NOW SELECTED", infoX + 33, iconY + 3, theme.accent(), true);

        int y = infoY + 40;
        graphics.text(font, trim(selected.title(), infoWidth - 20), infoX + 10, y, theme.text(), true);
        graphics.text(font, trim(selected.artist(), infoWidth - 20), infoX + 10, y + 13, theme.muted(), false);
        graphics.text(font, selected.bpm() + " BPM  ·  " + selected.theme().displayName().toUpperCase(),
                infoX + 10, y + 29, theme.accent(), true);

        int duration = MusicMetadata.durationSeconds(selected.id());
        int elapsed = CampfireMusicClient.isPlayingSelected()
                ? Math.min(duration, CampfireMusicClient.elapsedSeconds()) : 0;

        int progressX = infoX + 10;
        int progressY = infoY + infoHeight - 42;
        int progressW = infoWidth - 20;
        graphics.fill(progressX, progressY, progressX + progressW, progressY + 3, 0x6A000000);
        if (duration > 0 && elapsed > 0) {
            int fill = Math.max(1, (int) Math.round(progressW * (elapsed / (double) duration)));
            graphics.fill(progressX, progressY, progressX + fill, progressY + 3, theme.accent());
        }

        String timeText = formatTime(elapsed) + " / " + formatTime(duration);
        graphics.text(font, timeText, progressX, progressY + 7, theme.text(), false);

        String repeatText = CampfireMusicClient.isRepeatOne() ? "REPEAT ONE" : "AUTO NEXT";
        graphics.text(font, repeatText, infoX + infoWidth - 10 - font.width(repeatText), progressY + 7,
                CampfireMusicClient.isRepeatOne() ? theme.accent() : theme.muted(), false);

        int barsY = infoY + infoHeight - 15;
        double beat = CampfireMusicClient.isPlayingSelected()
                ? CampfireMusicClient.elapsedSecondsExact() * selected.bpm() / 60.0 : 0.0;
        for (int i = 0; i < 8; i++) {
            int h = CampfireMusicClient.isPlayingSelected()
                    ? 3 + (int) Math.round(Math.abs(Math.sin(beat * Math.PI + i * 0.8)) * 7.0)
                    : 3;
            int x = infoX + 10 + i * 6;
            graphics.fill(x, barsY - h, x + 3, barsY, theme.accent());
        }
        String state = CampfireMusicClient.isPlayingSelected() ? "PLAYING" : "READY";
        graphics.text(font, state, infoX + infoWidth - 10 - font.width(state), barsY - 8,
                CampfireMusicClient.isPlayingSelected() ? theme.accent() : theme.muted(), false);
    }

    private String trim(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int limit = Math.max(1, maxWidth - font.width(suffix));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = out.toString() + text.charAt(i);
            if (font.width(next) > limit) break;
            out.append(text.charAt(i));
        }
        return out + suffix;
    }

    private static String formatTime(int seconds) {
        if (seconds <= 0) return "--:--";
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static boolean inside(double x, double y, int left, int top, int w, int h) {
        return x >= left && x < left + w && y >= top && y < top + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record CardBounds(int x, int y, int width, int height) {}
}
