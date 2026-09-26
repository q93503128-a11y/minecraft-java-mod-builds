package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import kr.moonseungjun.campfiresessions.registry.ModItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MusicPlayerScreen extends Screen {
    private static final Identifier PANEL = id("music/panel");
    private static final Identifier BUTTON_BROWN = id("music/button_brown");
    private static final Identifier BUTTON_GREY = id("music/button_grey");
    private static final Identifier BUTTON_RED = id("music/button_red");

    public MusicPlayerScreen() {
        super(Component.translatable("screen.campfiresessions.music.title"));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, path);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(430, Math.max(300, width - 36));
        int panelHeight = Math.min(252, Math.max(214, height - 36));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int buttonY = top + panelHeight - 42;
        int buttonWidth = Math.max(76, Math.min(108, (panelWidth - 64) / 3));
        int gap = 8;
        int groupWidth = buttonWidth * 3 + gap * 2;
        int buttonX = left + (panelWidth - groupWidth) / 2;

        addRenderableWidget(new MusicTextureButton(
                buttonX, buttonY, buttonWidth, 24,
                Component.translatable("screen.campfiresessions.music.play"),
                BUTTON_BROWN,
                button -> CampfireMusicClient.playSelected()
        ));
        addRenderableWidget(new MusicTextureButton(
                buttonX + buttonWidth + gap, buttonY, buttonWidth, 24,
                Component.translatable("screen.campfiresessions.music.stop"),
                BUTTON_GREY,
                button -> CampfireMusicClient.stop()
        ));
        addRenderableWidget(new MusicTextureButton(
                buttonX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 24,
                Component.translatable("screen.campfiresessions.music.close"),
                BUTTON_RED,
                button -> onClose()
        ));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB20B0A08);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(430, Math.max(300, width - 36));
        int panelHeight = Math.min(252, Math.max(214, height - 36));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, left, top, panelWidth, panelHeight);
        graphics.text(font, Component.translatable("screen.campfiresessions.music.title"), left + 26, top + 18, 0xFFFFE7B0, true);
        graphics.text(font, Component.translatable("screen.campfiresessions.music.subtitle"), left + 26, top + 35, 0xFFD7C7A8, false);

        int cardLeft = left + 22;
        int cardRight = left + panelWidth - 22;
        graphics.fill(cardLeft, top + 59, cardRight, top + 131, 0x74302118);
        graphics.fill(cardLeft, top + 59, cardLeft + 4, top + 131, 0xFFE2AA62);
        graphics.item(ModItems.ACOUSTIC_GUITAR.get().getDefaultInstance(), cardLeft + 14, top + 84);
        graphics.text(font, "Etirwer", cardLeft + 44, top + 73, 0xFFFFFFFF, true);
        graphics.text(font, "Kistol · CC0", cardLeft + 44, top + 90, 0xFFC8B99D, false);
        graphics.text(font, Component.translatable("screen.campfiresessions.music.track_hint"), cardLeft + 44, top + 107, 0xFFA9987A, false);

        Component state = Component.translatable(CampfireMusicClient.isPlaying()
                ? "screen.campfiresessions.music.playing"
                : "screen.campfiresessions.music.ready");
        graphics.text(font, state, left + 26, top + 149,
                CampfireMusicClient.isPlaying() ? 0xFFB9E69D : 0xFFD6CDBD, true);
        graphics.text(font, Component.translatable("screen.campfiresessions.music.tip"), left + 26, top + 168, 0xFFB9AA8C, false);
        graphics.text(font, "CC0 prototype · 1 / 1", left + 26, top + 184, 0xFF8F816C, false);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
