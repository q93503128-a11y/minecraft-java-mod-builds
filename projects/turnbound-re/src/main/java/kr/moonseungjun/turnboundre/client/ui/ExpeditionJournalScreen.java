package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleClientState;
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

/** Expedition selection reached from the unified menu, using the adopted Kenney-backed visual language. */
public final class ExpeditionJournalScreen extends Screen {
    private final Screen parent;
    private ExpeditionNetworkPayloads.JournalView view;
    private long seenGeneration = -1L;
    private String feedback = "";
    private boolean startPending;

    public ExpeditionJournalScreen() {
        this(null);
    }

    public ExpeditionJournalScreen(Screen parent) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.turnbound_re.expedition.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        syncState();
    }

    @Override
    public void tick() {
        super.tick();
        if (ExpeditionMenuPolicy.shouldEnterBattle(startPending, BattleClientState.latestSnapshot().isPresent())) {
            this.minecraft.gui.setScreen(null);
            return;
        }
        if (ExpeditionJournalClientState.generation() != seenGeneration) syncState();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep world context visible behind the compact route selector.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        UiLayoutMetrics.Rect root = root();
        UiVisualLanguage.titleBand(
                graphics, this.font,
                root.x(), root.y(), root.width(), 22,
                Component.translatable("screen.turnbound_re.expedition.title"),
                UiVisualLanguage.TEXT_FOCUS, true);

        String partyLine = view == null
                ? Component.translatable("screen.turnbound_re.expedition.loading").getString()
                : Component.translatable("screen.turnbound_re.expedition.party_count", view.party().size(), 4).getString();
        graphics.text(this.font, Component.literal(partyLine),
                root.x() + UiLayoutMetrics.SPACE_8, root.y() + 30,
                view != null && view.party().isEmpty() ? UiVisualLanguage.TEXT_WARNING : UiVisualLanguage.TEXT_PRIMARY,
                true);

        if (view != null) {
            for (int i = 0; i < view.encounters().size(); i++) {
                ExpeditionNetworkPayloads.EncounterView encounter = view.encounters().get(i);
                UiLayoutMetrics.Rect row = encounterRow(i);
                boolean enabled = ExpeditionMenuPolicy.canSelectEncounter(view, startPending);
                UiVisualLanguage.FrameState state = !enabled
                        ? UiVisualLanguage.FrameState.DISABLED
                        : TurnboundMenuScreen.contains(row, mouseX, mouseY)
                                ? UiVisualLanguage.FrameState.FOCUS
                                : UiVisualLanguage.FrameState.IDLE;
                UiVisualLanguage.frame(graphics, row.x(), row.y(), row.width(), row.height(), state);
                Component label = Component.translatable(
                        "screen.turnbound_re.expedition.encounter_row",
                        encounterName(encounter.id()), encounter.difficulty(), encounter.enemyCount());
                centered(graphics, row, label, UiVisualLanguage.textColor(state));
            }
        }

        UiLayoutMetrics.Rect party = partyButton();
        UiVisualLanguage.FrameState partyState = startPending
                ? UiVisualLanguage.FrameState.DISABLED
                : TurnboundMenuScreen.contains(party, mouseX, mouseY)
                        ? UiVisualLanguage.FrameState.FOCUS : UiVisualLanguage.FrameState.IDLE;
        UiVisualLanguage.frame(graphics, party.x(), party.y(), party.width(), party.height(), partyState);
        centered(graphics, party, Component.translatable("screen.turnbound_re.expedition.party"),
                UiVisualLanguage.textColor(partyState));

        UiLayoutMetrics.Rect back = backButton();
        UiVisualLanguage.FrameState backState = startPending
                ? UiVisualLanguage.FrameState.DISABLED
                : TurnboundMenuScreen.contains(back, mouseX, mouseY)
                        ? UiVisualLanguage.FrameState.FOCUS : UiVisualLanguage.FrameState.IDLE;
        UiVisualLanguage.frame(graphics, back.x(), back.y(), back.width(), back.height(), backState);
        centered(graphics, back, Component.translatable(parent == null ? "gui.done" : "gui.back"),
                UiVisualLanguage.textColor(backState));

        String visibleFeedback = feedback;
        if (view != null && view.party().isEmpty()) {
            visibleFeedback = Component.translatable("screen.turnbound_re.expedition.empty_party").getString();
        }
        if (!visibleFeedback.isBlank()) {
            graphics.text(this.font, Component.literal(fit(visibleFeedback, root.width() - UiLayoutMetrics.SPACE_16)),
                    root.x() + UiLayoutMetrics.SPACE_8,
                    Math.min(this.height - this.font.lineHeight - UiLayoutMetrics.SPACE_4, back.bottom() + UiLayoutMetrics.SPACE_4),
                    UiVisualLanguage.TEXT_WARNING, true);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (startPending) return true;
        if (event.button() == 0) {
            int mouseX = (int) Math.floor(event.x());
            int mouseY = (int) Math.floor(event.y());
            if (ExpeditionMenuPolicy.canSelectEncounter(view, false)) {
                for (int i = 0; i < view.encounters().size(); i++) {
                    if (TurnboundMenuScreen.contains(encounterRow(i), mouseX, mouseY)) {
                        start(view.encounters().get(i).id());
                        return true;
                    }
                }
            }
            if (TurnboundMenuScreen.contains(partyButton(), mouseX, mouseY)) {
                openParty();
                return true;
            }
            if (TurnboundMenuScreen.contains(backButton(), mouseX, mouseY)) {
                closeScreen();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void syncState() {
        seenGeneration = ExpeditionJournalClientState.generation();
        view = ExpeditionJournalClientState.view().orElse(null);
        feedback = view == null ? "" : feedbackFor(view.resultCode());
        if (view != null && view.resultCode() != null && !view.resultCode().isBlank()) startPending = false;
    }

    private void start(String encounterId) {
        if (!ExpeditionMenuPolicy.canSelectEncounter(view, startPending)) return;
        startPending = true;
        feedback = "";
        ClientPacketDistributor.sendToServer(ExpeditionNetworkPayloads.StartEncounterC2S.of(encounterId));
    }

    private void openParty() {
        ProgressionClientState.clear();
        this.minecraft.gui.setScreen(new PartyFormationScreen());
        ClientPacketDistributor.sendToServer(new ProgressionNetworkPayloads.RequestProgressC2S());
    }

    private UiLayoutMetrics.Rect root() {
        int panelWidth = Math.min(420, Math.max(240, this.width - UiLayoutMetrics.SPACE_16 * 2));
        int x = Math.max(0, (this.width - panelWidth) / 2);
        int y = Math.max(UiLayoutMetrics.SPACE_8, this.height / 2 - 110);
        return new UiLayoutMetrics.Rect(x, y, panelWidth, 206);
    }

    private UiLayoutMetrics.Rect encounterRow(int index) {
        UiLayoutMetrics.Rect root = root();
        return new UiLayoutMetrics.Rect(
                root.x() + UiLayoutMetrics.SPACE_8,
                root.y() + 48 + index * 34,
                root.width() - UiLayoutMetrics.SPACE_16,
                28);
    }

    private UiLayoutMetrics.Rect partyButton() {
        UiLayoutMetrics.Rect root = root();
        int gap = UiLayoutMetrics.SPACE_4;
        int width = (root.width() - UiLayoutMetrics.SPACE_16 - gap) / 2;
        return new UiLayoutMetrics.Rect(root.x() + UiLayoutMetrics.SPACE_8, root.y() + 150, width, 24);
    }

    private UiLayoutMetrics.Rect backButton() {
        UiLayoutMetrics.Rect party = partyButton();
        UiLayoutMetrics.Rect root = root();
        int x = party.right() + UiLayoutMetrics.SPACE_4;
        return new UiLayoutMetrics.Rect(x, party.y(), root.right() - UiLayoutMetrics.SPACE_8 - x, party.height());
    }

    private void centered(GuiGraphicsExtractor graphics, UiLayoutMetrics.Rect bounds, Component label, int color) {
        int x = bounds.x() + Math.max(UiLayoutMetrics.SPACE_4, (bounds.width() - this.font.width(label)) / 2);
        int y = bounds.y() + Math.max(UiLayoutMetrics.SPACE_2, (bounds.height() - this.font.lineHeight) / 2);
        graphics.text(this.font, label, x, y, color, true);
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

    private void closeScreen() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
