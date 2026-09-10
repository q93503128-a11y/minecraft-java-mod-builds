package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.ExpeditionJournalClientState;
import kr.moonseungjun.turnboundre.client.ProgressionClientState;
import kr.moonseungjun.turnboundre.network.ExpeditionNetworkPayloads;
import kr.moonseungjun.turnboundre.network.ProgressionNetworkPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Single player-facing hub for TURNBOUND management screens.
 * Visual surfaces reuse the adopted Kenney-backed UiVisualLanguage instead of vanilla button skins.
 */
public final class TurnboundMenuScreen extends Screen {
    public TurnboundMenuScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.turnbound_re.menu.title"));
    }

    @Override
    protected void init() {
        // Interaction is handled by the same Kenney-backed frame regions that are rendered below.
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the world visible. This menu is a management overlay, not a replacement scene.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        UiLayoutMetrics.Rect root = root();
        UiVisualLanguage.titleBand(
                graphics, this.font,
                root.x(), root.y(), root.width(), 22,
                Component.translatable("screen.turnbound_re.menu.title"),
                UiVisualLanguage.TEXT_FOCUS, true);

        graphics.text(
                this.font,
                Component.translatable("screen.turnbound_re.menu.subtitle"),
                root.x() + UiLayoutMetrics.SPACE_8,
                root.y() + 30,
                UiVisualLanguage.TEXT_SECONDARY,
                true);

        renderChoice(
                graphics,
                expeditions(),
                Component.translatable("screen.turnbound_re.menu.expeditions"),
                mouseX, mouseY);
        renderChoice(
                graphics,
                party(),
                Component.translatable("screen.turnbound_re.menu.party"),
                mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderChoice(
            GuiGraphicsExtractor graphics,
            UiLayoutMetrics.Rect bounds,
            Component label,
            int mouseX,
            int mouseY
    ) {
        UiVisualLanguage.FrameState state = contains(bounds, mouseX, mouseY)
                ? UiVisualLanguage.FrameState.FOCUS
                : UiVisualLanguage.FrameState.IDLE;
        UiVisualLanguage.frame(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), state);
        int textX = bounds.x() + Math.max(UiLayoutMetrics.SPACE_8, (bounds.width() - this.font.width(label)) / 2);
        int textY = bounds.y() + Math.max(UiLayoutMetrics.SPACE_4, (bounds.height() - this.font.lineHeight) / 2);
        graphics.text(this.font, label, textX, textY, UiVisualLanguage.textColor(state), true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mouseX = (int) Math.floor(event.x());
            int mouseY = (int) Math.floor(event.y());
            if (contains(expeditions(), mouseX, mouseY)) {
                openExpeditions();
                return true;
            }
            if (contains(party(), mouseX, mouseY)) {
                openParty();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void openExpeditions() {
        ExpeditionJournalClientState.clear();
        this.minecraft.gui.setScreen(new ExpeditionJournalScreen(this));
        ClientPacketDistributor.sendToServer(new ExpeditionNetworkPayloads.RequestJournalC2S());
    }

    private void openParty() {
        ProgressionClientState.clear();
        this.minecraft.gui.setScreen(new PartyFormationScreen(this));
        ClientPacketDistributor.sendToServer(new ProgressionNetworkPayloads.RequestProgressC2S());
    }

    private UiLayoutMetrics.Rect root() {
        int width = Math.min(380, Math.max(236, this.width - UiLayoutMetrics.SPACE_16 * 2));
        int height = 150;
        int x = Math.max(0, (this.width - width) / 2);
        int y = Math.max(UiLayoutMetrics.SPACE_8, this.height / 2 - height / 2);
        return new UiLayoutMetrics.Rect(x, y, width, height);
    }

    private UiLayoutMetrics.Rect expeditions() {
        UiLayoutMetrics.Rect root = root();
        return new UiLayoutMetrics.Rect(
                root.x() + UiLayoutMetrics.SPACE_8,
                root.y() + 52,
                root.width() - UiLayoutMetrics.SPACE_16,
                34);
    }

    private UiLayoutMetrics.Rect party() {
        UiLayoutMetrics.Rect first = expeditions();
        return new UiLayoutMetrics.Rect(first.x(), first.y() + first.height() + UiLayoutMetrics.SPACE_8,
                first.width(), first.height());
    }

    static boolean contains(UiLayoutMetrics.Rect rect, int x, int y) {
        return x >= rect.x() && x < rect.right() && y >= rect.y() && y < rect.bottom();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
