package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class MusicPlayerScreen extends Screen {
    private static final int ROW_HEIGHT = 30;
    private static final int VISIBLE_ROWS = 3;

    private int panelX, panelY, panelWidth, panelHeight;
    private int listX, listY, listWidth, listHeight;
    private int nowX, nowWidth;
    private int scrollRow;

    public MusicPlayerScreen() {
        super(Component.translatable("screen.campfiresessions.music.title"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(388, Math.max(320, width - 48));
        panelHeight = Math.min(224, Math.max(196, height - 44));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int gap = 12;
        nowWidth = Math.max(108, panelWidth / 3);
        listX = panelX + 16;
        listY = panelY + 54;
        nowX = panelX + panelWidth - nowWidth - 16;
        listWidth = nowX - gap - listX;
        listHeight = ROW_HEIGHT * VISIBLE_ROWS;

        int controlsY = panelY + panelHeight - 34;
        int controlW = 46;
        int controlGap = 6;
        int groupW = controlW * 3 + controlGap * 2;
        int controlsX = listX + Math.max(0, (listWidth - groupW) / 2);

        addRenderableWidget(new MusicTextureButton(
                controlsX, controlsY, controlW, 22, Component.literal("◀"),
                MusicTextureButton.Style.SECONDARY, b -> CampfireMusicClient.previous()));
        addRenderableWidget(new MusicTextureButton(
                controlsX + controlW + controlGap, controlsY, controlW, 22, Component.literal("▶"),
                MusicTextureButton.Style.PRIMARY, b -> CampfireMusicClient.toggleSelected()));
        addRenderableWidget(new MusicTextureButton(
                controlsX + (controlW + controlGap) * 2, controlsY, controlW, 22, Component.literal("▶▶"),
                MusicTextureButton.Style.SECONDARY, b -> CampfireMusicClient.next()));
        addRenderableWidget(new MusicTextureButton(
                panelX + panelWidth - 31, panelY + 11, 18, 18, Component.literal("×"),
                MusicTextureButton.Style.DANGER, b -> onClose()));
        keepSelectedVisible();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside(mouseX, mouseY, listX, listY, listWidth, listHeight) && scrollY != 0.0) {
            int max = Math.max(0, MusicCatalog.TRACKS.size() - VISIBLE_ROWS);
            scrollRow = Math.max(0, Math.min(max, scrollRow + (scrollY < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && inside(event.x(), event.y(), listX, listY, listWidth, listHeight)) {
            int visible = ((int) event.y() - listY) / ROW_HEIGHT;
            int index = scrollRow + visible;
            if (visible >= 0 && visible < VISIBLE_ROWS && index < MusicCatalog.TRACKS.size()) {
                CampfireMusicClient.select(index);
                keepSelectedVisible();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private void keepSelectedVisible() {
        int selected = CampfireMusicClient.selectedIndex();
        if (selected < scrollRow) scrollRow = selected;
        if (selected >= scrollRow + VISIBLE_ROWS) scrollRow = selected - VISIBLE_ROWS + 1;
        int max = Math.max(0, MusicCatalog.TRACKS.size() - VISIBLE_ROWS);
        scrollRow = Math.max(0, Math.min(max, scrollRow));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blurBeforeThisStratum();
        graphics.fill(0, 0, width, height, 0x72000000);
        MusicTheme theme = CampfireMusicClient.selectedTrack().theme();
        graphics.fill(0, 0, width, height, theme.background() & 0x33FFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        MusicTrack selected = CampfireMusicClient.selectedTrack();
        MusicTheme theme = selected.theme();

        drawPanel(graphics, panelX, panelY, panelWidth, panelHeight, 0xF00D1014, 0xFF343B43);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 3, theme.accent());

        graphics.text(font, "Campfire Sessions", panelX + 16, panelY + 13, theme.text(), true);
        graphics.text(font, Component.translatable("screen.campfiresessions.music.subtitle_v3"),
                panelX + 16, panelY + 29, theme.muted(), false);

        String mood = selected.theme().displayName().toUpperCase();
        int moodW = font.width(mood) + 12;
        int moodX = panelX + panelWidth - 43 - moodW;
        graphics.fill(moodX, panelY + 12, moodX + moodW, panelY + 27, 0xAA171C21);
        graphics.fill(moodX, panelY + 26, moodX + moodW, panelY + 27, theme.accent());
        graphics.centeredText(font, mood, moodX + moodW / 2, panelY + 16, theme.accent());

        graphics.text(font, "PLAYLIST", listX, listY - 13, theme.accent(), true);
        drawPlaylist(graphics, mouseX, mouseY, theme);
        drawNowPlaying(graphics, selected, theme);

        graphics.text(font, (CampfireMusicClient.selectedIndex() + 1) + " / " + MusicCatalog.TRACKS.size(),
                listX, panelY + panelHeight - 27, 0xFF737B83, false);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPlaylist(GuiGraphicsExtractor graphics, int mouseX, int mouseY, MusicTheme activeTheme) {
        graphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        for (int visible = 0; visible < VISIBLE_ROWS; visible++) {
            int index = scrollRow + visible;
            if (index >= MusicCatalog.TRACKS.size()) break;

            MusicTrack track = MusicCatalog.TRACKS.get(index);
            MusicTheme rowTheme = track.theme();
            int rowY = listY + visible * ROW_HEIGHT;
            boolean selected = index == CampfireMusicClient.selectedIndex();
            boolean hovered = inside(mouseX, mouseY, listX, rowY, listWidth, ROW_HEIGHT - 4);

            int bg = selected ? activeTheme.surface() : (hovered ? 0xC51D2329 : 0xA914191E);
            graphics.fill(listX + 1, rowY, listX + listWidth - 1, rowY + ROW_HEIGHT - 4, bg);
            graphics.fill(listX, rowY + 1, listX + 1, rowY + ROW_HEIGHT - 5,
                    selected ? activeTheme.accent() : 0xFF343B42);

            int dot = rowTheme.accent();
            graphics.fill(listX + 8, rowY + 9, listX + 12, rowY + 13, dot);
            if (CampfireMusicClient.playingIndex() == index) {
                graphics.text(font, "♪", listX + 7, rowY + 5, dot, true);
            }

            graphics.text(font, trim(track.title(), listWidth - 35), listX + 18, rowY + 5,
                    selected ? activeTheme.text() : 0xFFE1E5E8, selected);
            graphics.text(font, trim(track.artist() + " · " + track.theme().displayName(), listWidth - 35),
                    listX + 18, rowY + 17, selected ? activeTheme.muted() : 0xFF7F8991, false);
        }
        graphics.disableScissor();

        if (MusicCatalog.TRACKS.size() > VISIBLE_ROWS) {
            int railX = listX + listWidth + 4;
            int max = MusicCatalog.TRACKS.size() - VISIBLE_ROWS;
            int thumbH = Math.max(18, listHeight * VISIBLE_ROWS / MusicCatalog.TRACKS.size());
            int travel = listHeight - thumbH;
            int thumbY = listY + (max == 0 ? 0 : travel * scrollRow / max);
            graphics.fill(railX, listY, railX + 1, listY + listHeight, 0xFF303840);
            graphics.fill(railX, thumbY, railX + 2, thumbY + thumbH, activeTheme.accent());
        }
    }

    private void drawNowPlaying(GuiGraphicsExtractor graphics, MusicTrack selected, MusicTheme theme) {
        int top = listY;
        drawPanel(graphics, nowX, top, nowWidth, listHeight, theme.surface(), 0xFF38434A);
        graphics.fill(nowX + 1, top + 1, nowX + nowWidth - 1, top + 3, theme.accent());

        int cover = Math.min(52, nowWidth - 28);
        int coverX = nowX + (nowWidth - cover) / 2;
        int coverY = top + 10;
        graphics.fill(coverX, coverY, coverX + cover, coverY + cover, theme.background());
        graphics.fill(coverX + 3, coverY + 3, coverX + cover - 3, coverY + cover - 3, theme.surface());
        graphics.fill(coverX + 3, coverY + 3, coverX + cover - 3, coverY + 5, theme.accent());
        graphics.item(ModItems.ACOUSTIC_GUITAR.get().getDefaultInstance(),
                coverX + cover / 2 - 8, coverY + cover / 2 - 8);

        int textX = nowX + 9;
        int textY = coverY + cover + 7;
        graphics.text(font, trim(selected.title(), nowWidth - 18), textX, textY, theme.text(), true);
        graphics.text(font, trim(selected.artist(), nowWidth - 18), textX, textY + 12, theme.muted(), false);

        int barsY = top + listHeight - 10;
        long t = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        for (int i = 0; i < 7; i++) {
            int h = CampfireMusicClient.isPlayingSelected() ? 2 + Math.abs((int)((t + i * 3) % 9L) - 4) : 2;
            int x = nowX + 9 + i * 5;
            graphics.fill(x, barsY - h, x + 2, barsY, theme.accent());
        }
        String state = CampfireMusicClient.isPlayingSelected() ? "PLAYING" : "READY";
        graphics.text(font, state, nowX + nowWidth - 9 - font.width(state), barsY - 8,
                CampfireMusicClient.isPlayingSelected() ? theme.accent() : theme.muted(), false);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fill, int border) {
        graphics.fill(x + 1, y, x + w - 1, y + h, border);
        graphics.fill(x, y + 1, x + w, y + h - 1, border);
        graphics.fill(x + 2, y + 1, x + w - 2, y + h - 1, fill);
        graphics.fill(x + 1, y + 2, x + w - 1, y + h - 2, fill);
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

    @Override
    public boolean isPauseScreen() { return false; }
}
