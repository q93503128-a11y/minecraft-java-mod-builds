package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import kr.moonseungjun.campfiresessions.registry.ModItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MusicPlayerScreen extends Screen {
    private static final Identifier PANEL = Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, "music/panel");

    public MusicPlayerScreen() {
        super(Component.translatable("screen.campfiresessions.music.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(420, Math.max(286, width - 36));
        int panelHeight = Math.min(244, Math.max(208, height - 36));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int buttonY = top + panelHeight - 40;

        addRenderableWidget(Button.builder(
                Component.translatable("screen.campfiresessions.music.play"),
                button -> CampfireMusicClient.playSelected()).bounds(left + 22, buttonY, 104, 22).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.campfiresessions.music.stop"),
                button -> CampfireMusicClient.stop()).bounds(left + 136, buttonY, 104, 22).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.campfiresessions.music.close"),
                button -> onClose()).bounds(left + panelWidth - 126, buttonY, 104, 22).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB20B0A08);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(420, Math.max(286, width - 36));
        int panelHeight = Math.min(244, Math.max(208, height - 36));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, left, top, panelWidth, panelHeight);
        graphics.text(font, Component.translatable("screen.campfiresessions.music.title"), left + 24, top + 18, 0xFFFFE7B0, true);
        graphics.text(font, Component.translatable("screen.campfiresessions.music.subtitle"), left + 24, top + 34, 0xFFD7C7A8, false);

        graphics.fill(left + 20, top + 57, left + panelWidth - 20, top + 125, 0x70302118);
        graphics.fill(left + 20, top + 57, left + 24, top + 125, 0xFFE2AA62);
        graphics.item(ModItems.ACOUSTIC_GUITAR.get().getDefaultInstance(), left + 34, top + 78);
        graphics.text(font, "Etirwer", left + 62, top + 71, 0xFFFFFFFF, true);
        graphics.text(font, "Kistol · CC0", left + 62, top + 88, 0xFFC8B99D, false);
        graphics.text(font, Component.translatable("screen.campfiresessions.music.track_hint"), left + 62, top + 104, 0xFFA9987A, false);

        Component state = Component.translatable(CampfireMusicClient.isPlaying()
                ? "screen.campfiresessions.music.playing"
                : "screen.campfiresessions.music.ready");
        graphics.text(font, state, left + 24, top + 143,
                CampfireMusicClient.isPlaying() ? 0xFFB9E69D : 0xFFD6CDBD, false);
        graphics.text(font, Component.translatable("screen.campfiresessions.music.tip"), left + 24, top + 160, 0xFFB9AA8C, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
