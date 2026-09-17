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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Single player-facing hub for TURNBOUND management screens.
 * Visual surfaces reuse the adopted Kenney-backed UiVisualLanguage and Mojang runtime item visuals.
 */
public final class TurnboundMenuScreen extends Screen {
    private boolean progressRequested;

    public TurnboundMenuScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.turnbound_re.menu.title"));
    }

    @Override
    protected void init() {
        if (!progressRequested) {
            ProgressionClientState.clear();
            ClientPacketDistributor.sendToServer(new ProgressionNetworkPayloads.RequestProgressC2S());
            progressRequested = true;
        }
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

        renderProgressSummary(graphics, root);

        renderChoice(
                graphics,
                expeditions(),
                Component.translatable("screen.turnbound_re.menu.expeditions"),
                new ItemStack(Items.COMPASS),
                mouseX, mouseY);

        String partyLabel = Component.translatable("screen.turnbound_re.menu.party").getString()
                + " · " + Component.translatable("screen.turnbound_re.tab.equipment").getString();
        renderChoice(
                graphics,
                party(),
                Component.literal(partyLabel),
                new ItemStack(Items.PLAYER_HEAD),
                mouseX, mouseY);

        renderChoice(
                graphics,
                guide(),
                Component.translatable("screen.turnbound_re.menu.guide"),
                new ItemStack(Items.BOOK),
                mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderProgressSummary(GuiGraphicsExtractor graphics, UiLayoutMetrics.Rect root) {
        ProgressionNetworkPayloads.Snapshot snapshot = ProgressionClientState.snapshot().orElse(null);
        if (snapshot == null) {
            graphics.text(
                    this.font,
                    Component.translatable("screen.turnbound_re.party.loading"),
                    root.x() + UiLayoutMetrics.SPACE_8,
                    root.y() + 48,
                    UiVisualLanguage.TEXT_SECONDARY,
                    true);
            return;
        }

        Component partySummary = Component.translatable(
                "screen.turnbound_re.expedition.party_count",
                snapshot.party().size(), 4);
        graphics.text(
                this.font,
                partySummary,
                root.x() + UiLayoutMetrics.SPACE_8,
                root.y() + 48,
                UiVisualLanguage.TEXT_PRIMARY,
                true);

        int totalShards = snapshot.characters().stream()
                .mapToInt(character -> character.growth().shardBalance())
                .sum();
        Component wallet = Component.translatable(
                "screen.turnbound_re.growth.wallet",
                snapshot.coin(), snapshot.essence(), totalShards);
        graphics.text(
                this.font,
                wallet,
                root.x() + UiLayoutMetrics.SPACE_8,
                root.y() + 60,
                UiVisualLanguage.TEXT_SECONDARY,
                true);
    }

    private void renderChoice(
            GuiGraphicsExtractor graphics,
            UiLayoutMetrics.Rect bounds,
            Component label,
            ItemStack icon,
            int mouseX,
            int mouseY
    ) {
        UiVisualLanguage.FrameState state = contains(bounds, mouseX, mouseY)
                ? UiVisualLanguage.FrameState.FOCUS
                : UiVisualLanguage.FrameState.IDLE;
        UiVisualLanguage.frame(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), state);

        int iconX = bounds.x() + UiLayoutMetrics.SPACE_8;
        int iconY = bounds.y() + Math.max(0, (bounds.height() - 16) / 2);
        if (icon != null && !icon.isEmpty()) graphics.item(icon, iconX, iconY);

        int contentX = bounds.x() + 32;
        int contentWidth = Math.max(1, bounds.right() - UiLayoutMetrics.SPACE_8 - contentX);
        int textX = contentX + Math.max(0, (contentWidth - this.font.width(label)) / 2);
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
            if (contains(guide(), mouseX, mouseY)) {
                this.minecraft.gui.setScreen(new TurnboundGuideScreen(this));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void openExpeditions() {
        ExpeditionJournalClientState.clearView();
        this.minecraft.gui.setScreen(new ExpeditionJournalScreen(this));
        ClientPacketDistributor.sendToServer(new ExpeditionNetworkPayloads.RequestJournalC2S());
    }

    private void openParty() {
        this.minecraft.gui.setScreen(new PartyFormationScreen(this));
        ClientPacketDistributor.sendToServer(new ProgressionNetworkPayloads.RequestProgressC2S());
    }

    private UiLayoutMetrics.Rect root() {
        int width = Math.min(420, Math.max(260, this.width - UiLayoutMetrics.SPACE_16 * 2));
        int height = 210;
        int x = Math.max(0, (this.width - width) / 2);
        int y = Math.max(UiLayoutMetrics.SPACE_8, this.height / 2 - height / 2);
        return new UiLayoutMetrics.Rect(x, y, width, height);
    }

    private UiLayoutMetrics.Rect expeditions() {
        UiLayoutMetrics.Rect root = root();
        return new UiLayoutMetrics.Rect(root.x() + UiLayoutMetrics.SPACE_8, root.y() + 80,
                root.width() - UiLayoutMetrics.SPACE_16, 34);
    }

    private UiLayoutMetrics.Rect party() {
        UiLayoutMetrics.Rect first = expeditions();
        return new UiLayoutMetrics.Rect(first.x(), first.y() + first.height() + UiLayoutMetrics.SPACE_8,
                first.width(), first.height());
    }

    private UiLayoutMetrics.Rect guide() {
        UiLayoutMetrics.Rect second = party();
        return new UiLayoutMetrics.Rect(second.x(), second.y() + second.height() + UiLayoutMetrics.SPACE_8,
                second.width(), second.height());
    }

    static boolean contains(UiLayoutMetrics.Rect rect, int x, int y) {
        return x >= rect.x() && x < rect.right() && y >= rect.y() && y < rect.bottom();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
