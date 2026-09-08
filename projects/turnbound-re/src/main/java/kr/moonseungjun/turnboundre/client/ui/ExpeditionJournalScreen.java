package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.ExpeditionJournalClientState;
import kr.moonseungjun.turnboundre.client.ProgressionClientState;
import kr.moonseungjun.turnboundre.network.ExpeditionNetworkPayloads;
import kr.moonseungjun.turnboundre.network.ProgressionNetworkPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Compact world-visible Expedition Journal using the established M5 visual language. */
public final class ExpeditionJournalScreen extends Screen {
    private ExpeditionNetworkPayloads.JournalView view;
    private long seenGeneration = -1L;
    private String feedback = "";

    public ExpeditionJournalScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.turnbound_re.expedition.title"));
    }

    @Override
    protected void init() {
        syncState();
        int panelWidth = Math.min(420, Math.max(240, this.width - UiLayoutMetrics.SPACE_8 * 2));
        int x = (this.width - panelWidth) / 2;
        int y = Math.max(UiLayoutMetrics.SPACE_8, this.height / 2 - 110);
        int buttonHeight = 22;
        int gap = UiLayoutMetrics.SPACE_4;

        if (view != null) {
            int encounterY = y + 48;
            for (ExpeditionNetworkPayloads.EncounterView encounter : view.encounters()) {
                Component label = Component.translatable(
                        "screen.turnbound_re.expedition.encounter_row",
                        encounterName(encounter.id()), encounter.difficulty(), encounter.enemyCount());
                Button button = Button.builder(label, ignored -> start(encounter.id()))
                        .bounds(x, encounterY, panelWidth, buttonHeight)
                        .build();
                button.active = !view.party().isEmpty();
                this.addRenderableWidget(button);
                encounterY += buttonHeight + gap;
            }
        }

        int footerY = Math.min(this.height - 28, y + 166);
        int half = Math.max(80, (panelWidth - gap) / 2);
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.turnbound_re.expedition.party"), ignored -> openParty())
                .bounds(x, footerY, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> closeScreen())
                .bounds(x + half + gap, footerY, panelWidth - half - gap, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (ExpeditionJournalClientState.generation() != seenGeneration) {
            syncState();
            this.rebuildWidgets();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Preserve the game world behind the journal instead of replacing it with a generic full-screen panel.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(420, Math.max(240, this.width - UiLayoutMetrics.SPACE_8 * 2));
        int x = (this.width - panelWidth) / 2;
        int y = Math.max(UiLayoutMetrics.SPACE_8, this.height / 2 - 110);
        UiVisualLanguage.titleBand(
                graphics, this.font, x, y, panelWidth, 20,
                Component.translatable("screen.turnbound_re.expedition.title"),
                UiVisualLanguage.TEXT_FOCUS, true);

        String partyLine = view == null
                ? Component.translatable("screen.turnbound_re.expedition.loading").getString()
                : Component.translatable("screen.turnbound_re.expedition.party_count", view.party().size(), 4).getString();
        graphics.text(this.font, Component.literal(partyLine), x, y + 28,
                view != null && view.party().isEmpty() ? UiVisualLanguage.TEXT_WARNING : UiVisualLanguage.TEXT_PRIMARY, true);

        String visibleFeedback = feedback;
        if (view != null && view.party().isEmpty()) {
            visibleFeedback = Component.translatable("screen.turnbound_re.expedition.empty_party").getString();
        }
        if (!visibleFeedback.isBlank()) {
            graphics.text(this.font, Component.literal(fit(visibleFeedback, panelWidth)), x, y + 198,
                    UiVisualLanguage.TEXT_WARNING, true);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void syncState() {
        seenGeneration = ExpeditionJournalClientState.generation();
        view = ExpeditionJournalClientState.view().orElse(null);
        feedback = view == null ? "" : feedbackFor(view.resultCode());
    }

    private void start(String encounterId) {
        if (view == null || view.party().isEmpty()) return;
        ClientPacketDistributor.sendToServer(ExpeditionNetworkPayloads.StartEncounterC2S.of(encounterId));
        this.minecraft.gui.setScreen(null);
    }

    private void openParty() {
        ProgressionClientState.clear();
        this.minecraft.gui.setScreen(new PartyFormationScreen());
        ClientPacketDistributor.sendToServer(new ProgressionNetworkPayloads.RequestProgressC2S());
    }

    private static Component encounterName(String id) {
        int colon = id == null ? -1 : id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return Component.translatable("encounter.turnbound_re." + path + ".name");
    }

    private String feedbackFor(String code) {
        if (code == null || code.isBlank()) return "";
        return switch (code) {
            case "ALREADY_IN_BATTLE" -> Component.translatable("screen.turnbound_re.expedition.already_in_battle").getString();
            case "EMPTY_PARTY", "INVALID_PARTY" -> Component.translatable("screen.turnbound_re.expedition.empty_party").getString();
            case "INVALID_ENCOUNTER" -> Component.translatable("screen.turnbound_re.expedition.invalid_encounter").getString();
            default -> Component.translatable("screen.turnbound_re.expedition.server_rejected").getString();
        };
    }

    private String fit(String text, int maxWidth) {
        if (text == null || text.isBlank() || maxWidth <= 0) return "";
        if (this.font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end) + suffix) > maxWidth) end--;
        return end == 0 ? "" : text.substring(0, end) + suffix;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
