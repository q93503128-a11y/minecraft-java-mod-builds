package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import kr.moonseungjun.campfiresessions.registry.ModItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MusicPlayerScreen extends Screen {
    private static final Identifier GLASS_PANEL = id("music/glass_panel");
    private static final Identifier METAL_PANEL = id("music/metal_panel");
    private static final Identifier METAL_BLUE = id("music/metal_blue");
    private static final Identifier METAL_RED = id("music/metal_red");
    private static final int ROW_HEIGHT = 40;
    private static final int VISIBLE_ROWS = 3;

    private int panelX, panelY, panelWidth, panelHeight;
    private int listX, listY, listWidth, listHeight;
    private int scrollRow;

    public MusicPlayerScreen() {
        super(Component.translatable("screen.campfiresessions.music.title"));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, path);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(366, Math.max(300, width - 82));
        panelHeight = Math.min(238, Math.max(206, height - 58));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int infoWidth = Math.max(106, panelWidth / 3);
        listX = panelX + 16;
        listY = panelY + 53;
        listWidth = panelWidth - infoWidth - 44;
        listHeight = ROW_HEIGHT * VISIBLE_ROWS;

        int controlY = panelY + panelHeight - 36;
        int buttonWidth = 54;
        int gap = 5;
        int controlsWidth = buttonWidth * 4 + gap * 3;
        int controlX = panelX + (panelWidth - controlsWidth) / 2;

        addRenderableWidget(new MusicTextureButton(controlX, controlY, buttonWidth, 22,
                Component.literal("◀"), METAL_PANEL, b -> CampfireMusicClient.previous()));
        addRenderableWidget(new MusicTextureButton(controlX + buttonWidth + gap, controlY, buttonWidth, 22,
                Component.translatable("screen.campfiresessions.music.play_pause"), METAL_BLUE,
                b -> CampfireMusicClient.toggleSelected()));
        addRenderableWidget(new MusicTextureButton(controlX + (buttonWidth + gap) * 2, controlY, buttonWidth, 22,
                Component.literal("▶"), METAL_PANEL, b -> CampfireMusicClient.next()));
        addRenderableWidget(new MusicTextureButton(controlX + (buttonWidth + gap) * 3, controlY, buttonWidth, 22,
                Component.translatable("screen.campfiresessions.music.close"), METAL_RED, b -> onClose()));
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
            int visibleIndex = ((int)event.y() - listY) / ROW_HEIGHT;
            int index = scrollRow + visibleIndex;
            if (visibleIndex >= 0 && visibleIndex < VISIBLE_ROWS && index < MusicCatalog.TRACKS.size()) {
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
        MusicTheme theme = CampfireMusicClient.selectedTrack().theme();
        graphics.fill(0, 0, width, height, 0x76000000);
        graphics.fill(0, 0, width, height, theme.background() & 0x55FFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        MusicTrack selected = CampfireMusicClient.selectedTrack();
        MusicTheme theme = selected.theme();

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GLASS_PANEL, panelX, panelY, panelWidth, panelHeight);
        graphics.fill(panelX + 3, panelY + 3, panelX + panelWidth - 3, panelY + panelHeight - 3, theme.background());
        graphics.fill(panelX + 3, panelY + 3, panelX + 6, panelY + panelHeight - 3, theme.accent());

        graphics.text(font, "CAMPFIRE SESSIONS", panelX + 16, panelY + 13, theme.text(), true);
        graphics.text(font, Component.translatable("screen.campfiresessions.music.subtitle_v2"),
                panelX + 16, panelY + 28, theme.muted(), false);
        graphics.text(font, "PLAYLIST", listX, listY - 14, theme.accent(), true);

        drawPlaylist(graphics, mouseX, mouseY, theme);
        drawNowPlaying(graphics, selected, theme);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPlaylist(GuiGraphicsExtractor graphics, int mouseX, int mouseY, MusicTheme activeTheme) {
        graphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        for (int visible = 0; visible < VISIBLE_ROWS; visible++) {
            int index = scrollRow + visible;
            if (index >= MusicCatalog.TRACKS.size()) break;
            MusicTrack track = MusicCatalog.TRACKS.get(index);
            int rowY = listY + visible * ROW_HEIGHT;
            boolean selected = index == CampfireMusicClient.selectedIndex();
            boolean hovered = inside(mouseX, mouseY, listX, rowY, listWidth, ROW_HEIGHT - 3);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, track.theme().rowSprite(),
                    listX, rowY, listWidth, ROW_HEIGHT - 3);
            graphics.fill(listX, rowY, listX + listWidth, rowY + ROW_HEIGHT - 3,
                    selected ? 0x35FFFFFF : 0x1E000000);
            if (hovered) graphics.fill(listX, rowY, listX + listWidth, rowY + ROW_HEIGHT - 3, 0x1FFFFFFF);
            if (selected) graphics.fill(listX, rowY, listX + 3, rowY + ROW_HEIGHT - 3, activeTheme.accent());

            String marker = CampfireMusicClient.playingIndex() == index ? "♪ " : "";
            graphics.text(font, marker + track.title(), listX + 8, rowY + 7,
                    selected ? activeTheme.text() : 0xFFE5E8EA, true);
            graphics.text(font, track.artist() + " · " + track.theme().displayName(), listX + 8, rowY + 22,
                    selected ? activeTheme.muted() : 0xFFAEB5BB, false);
        }
        graphics.disableScissor();

        if (MusicCatalog.TRACKS.size() > VISIBLE_ROWS) {
            int trackX = listX + listWidth + 4;
            int max = MusicCatalog.TRACKS.size() - VISIBLE_ROWS;
            int thumbHeight = Math.max(24, listHeight * VISIBLE_ROWS / MusicCatalog.TRACKS.size());
            int travel = listHeight - thumbHeight;
            int thumbY = listY + (max == 0 ? 0 : travel * scrollRow / max);
            graphics.fill(trackX, listY, trackX + 2, listY + listHeight, 0x663F4A52);
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, activeTheme.accent());
        }
    }

    private void drawNowPlaying(GuiGraphicsExtractor graphics, MusicTrack selected, MusicTheme theme) {
        int infoX = listX + listWidth + 16;
        int infoWidth = panelX + panelWidth - 16 - infoX;
        int infoY = listY;

        graphics.fill(infoX, infoY, infoX + infoWidth, infoY + listHeight, theme.surface());
        graphics.fill(infoX, infoY, infoX + infoWidth, infoY + 2, theme.accent());
        graphics.text(font, "NOW", infoX + 9, infoY + 9, theme.accent(), true);
        graphics.item(ModItems.ACOUSTIC_GUITAR.get().getDefaultInstance(), infoX + 8, infoY + 27);
        graphics.text(font, trim(selected.title(), infoWidth - 18), infoX + 9, infoY + 52, theme.text(), true);
        graphics.text(font, trim(selected.artist(), infoWidth - 18), infoX + 9, infoY + 66, theme.muted(), false);
        graphics.text(font, selected.theme().displayName().toUpperCase(), infoX + 9, infoY + 84, theme.accent(), false);
        graphics.text(font, CampfireMusicClient.isPlayingSelected() ? "♪ PLAYING" : "READY",
                infoX + 9, infoY + 102,
                CampfireMusicClient.isPlayingSelected() ? theme.accent() : theme.muted(), true);
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
    public boolean isPauseScreen() {
        return false;
    }
}
