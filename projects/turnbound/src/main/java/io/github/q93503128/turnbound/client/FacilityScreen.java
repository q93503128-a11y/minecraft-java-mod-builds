package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/**
 * Shared shell for physical NPC services.
 *
 * <p>Visible chrome is always the adopted Foozle RPG UI skin through {@link TurnboundUiSkin}; concrete facilities
 * own only their task-specific controls/data. Keeping this shell shared prevents each new NPC from inventing another
 * fullscreen layout or duplicating close/refresh/frame behavior.</p>
 */
abstract class FacilityScreen extends Screen {
    protected int left;
    protected int top;
    protected int panelWidth;
    protected int panelHeight;

    private final int preferredWidth;
    private final int preferredHeight;
    private final int minimumWidth;
    private final int minimumHeight;
    private final int accent;

    protected FacilityScreen(
            String title,
            int accent,
            int preferredWidth,
            int preferredHeight,
            int minimumWidth,
            int minimumHeight
    ) {
        super(Component.literal(title));
        this.accent = accent;
        this.preferredWidth = preferredWidth;
        this.preferredHeight = preferredHeight;
        this.minimumWidth = minimumWidth;
        this.minimumHeight = minimumHeight;
    }

    @Override
    protected final void init() {
        super.init();
        panelWidth = Math.min(preferredWidth, Math.max(minimumWidth, width - 24));
        panelHeight = Math.min(preferredHeight, Math.max(minimumHeight, height - 24));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        buildFacility();
    }

    protected abstract void buildFacility();

    protected abstract void renderFacility(
            @NotNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    );

    protected String subtitle() {
        return "";
    }

    protected final int bodyTop() {
        return top + 49;
    }

    protected final int bodyBottom() {
        return top + panelHeight - 13;
    }

    protected final void inset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        TurnboundUiSkin.inset(graphics, x, y, width, height);
    }

    final void refreshSnapshot() {
        clearWidgets();
        init();
    }

    @Override
    public final void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the 3D world visible behind compact physical-NPC service surfaces.
    }

    @Override
    public final void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TurnboundFrameStyle.frame(graphics, left, top, panelWidth, panelHeight, accent);
        graphics.text(font, title, left + 13, top + 12, TurnboundUiTokens.TEXT_PRIMARY, true);
        String subtitle = subtitle();
        if (!subtitle.isBlank()) {
            graphics.text(font, Component.literal(UiTextLayout.fit(subtitle, panelWidth - 26)),
                    left + 13, top + 28, TurnboundUiTokens.TEXT_SECONDARY, false);
        }
        TurnboundFrameStyle.divider(graphics, left + 12, top + 42, panelWidth - 24);
        renderFacility(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public final boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_E || event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        FacilityUiAccess.clear();
        super.onClose();
    }

    @Override
    public final boolean isPauseScreen() {
        return false;
    }
}
