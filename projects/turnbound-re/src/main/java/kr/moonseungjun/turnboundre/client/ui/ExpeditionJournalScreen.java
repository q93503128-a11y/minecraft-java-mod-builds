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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only authored expedition reference reached from the unified menu.
 * Actual combat entry is world-first and happens only through validated encounter anchors.
 */
public final class ExpeditionJournalScreen extends Screen {
    private static final int ENCOUNTER_ROW_HEIGHT = 38;
    private static final int ENCOUNTER_ROW_STEP = 42;
    private static final int ENEMY_PREVIEW_SLOT = 26;
    private static final int ENEMY_PREVIEW_GAP = 2;
    private static final int MAX_ENEMY_PREVIEWS = 4;

    private final Screen parent;
    private final Map<String, CharacterEntityPreview> enemyPreviews = new HashMap<>();
    private ExpeditionNetworkPayloads.JournalView view;
    private long seenGeneration = -1L;

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
        boolean returningFromChild = seenGeneration >= 0;
        if (returningFromChild) {
            ExpeditionJournalClientState.clear();
            view = null;
            ClientPacketDistributor.sendToServer(new ExpeditionNetworkPayloads.RequestJournalC2S());
        }
        syncState();
    }

    @Override
    public void tick() {
        super.tick();
        if (ExpeditionJournalClientState.generation() != seenGeneration) syncState();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep world context visible behind the compact authored-route reference.
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
                renderEncounterRow(graphics, view.encounters().get(i), encounterRow(i));
            }
        }

        UiLayoutMetrics.Rect party = partyButton();
        UiVisualLanguage.FrameState partyState = TurnboundMenuScreen.contains(party, mouseX, mouseY)
                ? UiVisualLanguage.FrameState.FOCUS : UiVisualLanguage.FrameState.IDLE;
        UiVisualLanguage.frame(graphics, party.x(), party.y(), party.width(), party.height(), partyState);
        centered(graphics, party, Component.translatable("screen.turnbound_re.expedition.party"),
                UiVisualLanguage.textColor(partyState));

        UiLayoutMetrics.Rect back = backButton();
        UiVisualLanguage.FrameState backState = TurnboundMenuScreen.contains(back, mouseX, mouseY)
                ? UiVisualLanguage.FrameState.FOCUS : UiVisualLanguage.FrameState.IDLE;
        UiVisualLanguage.frame(graphics, back.x(), back.y(), back.width(), back.height(), backState);
        centered(graphics, back, Component.translatable(parent == null ? "gui.done" : "gui.back"),
                UiVisualLanguage.textColor(backState));

        if (view != null && view.party().isEmpty()) {
            String feedback = Component.translatable("screen.turnbound_re.expedition.empty_party").getString();
            graphics.text(this.font, Component.literal(fit(feedback, root.width() - UiLayoutMetrics.SPACE_16)),
                    root.x() + UiLayoutMetrics.SPACE_8,
                    Math.min(this.height - this.font.lineHeight - UiLayoutMetrics.SPACE_4, back.bottom() + UiLayoutMetrics.SPACE_4),
                    UiVisualLanguage.TEXT_WARNING, true);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderEncounterRow(
            GuiGraphicsExtractor graphics,
            ExpeditionNetworkPayloads.EncounterView encounter,
            UiLayoutMetrics.Rect row
    ) {
        UiVisualLanguage.frame(graphics, row.x(), row.y(), row.width(), row.height(), UiVisualLanguage.FrameState.IDLE);

        List<String> sources = encounter.enemySourceEntities();
        int previewCount = Math.min(MAX_ENEMY_PREVIEWS, sources.size());
        int previewStartX = row.x() + UiLayoutMetrics.SPACE_4;
        int previewY = row.y() + Math.max(1, (row.height() - ENEMY_PREVIEW_SLOT) / 2);
        int previewBlockWidth = previewCount == 0
                ? 0
                : previewCount * ENEMY_PREVIEW_SLOT + (previewCount - 1) * ENEMY_PREVIEW_GAP;

        graphics.enableScissor(row.x() + 1, row.y() + 1, row.right() - 1, row.bottom() - 1);
        for (int i = 0; i < previewCount; i++) {
            String sourceEntity = sources.get(i);
            CharacterEntityPreview preview = enemyPreviews.computeIfAbsent(sourceEntity, ignored -> new CharacterEntityPreview());
            UiLayoutMetrics.Rect slot = new UiLayoutMetrics.Rect(
                    previewStartX + i * (ENEMY_PREVIEW_SLOT + ENEMY_PREVIEW_GAP),
                    previewY,
                    ENEMY_PREVIEW_SLOT,
                    ENEMY_PREVIEW_SLOT);
            EntityPreviewLayout.PreviewSpec spec = preview.layout(
                    this.minecraft, sourceEntity, slot.width(), slot.height());
            preview.extract(graphics, slot, spec);
        }

        int textX = previewCount == 0
                ? row.x() + UiLayoutMetrics.SPACE_8
                : previewStartX + previewBlockWidth + UiLayoutMetrics.SPACE_4;
        int textWidth = Math.max(1, row.right() - UiLayoutMetrics.SPACE_8 - textX);
        String title = encounterName(encounter.id()).getString();
        graphics.text(this.font, Component.literal(fit(title, textWidth)),
                textX, row.y() + 5, UiVisualLanguage.TEXT_PRIMARY, true);

        String danger = Component.translatable("screen.turnbound_re.anchor.danger", encounter.difficulty()).getString();
        String enemies = Component.translatable("screen.turnbound_re.anchor.enemies", encounter.enemyCount()).getString();
        String meta = danger + " · " + enemies;
        graphics.text(this.font, Component.literal(fit(meta, textWidth)),
                textX, row.bottom() - this.font.lineHeight - 5, UiVisualLanguage.TEXT_SECONDARY, true);
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mouseX = (int) Math.floor(event.x());
            int mouseY = (int) Math.floor(event.y());
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
    }

    private void openParty() {
        ProgressionClientState.clear();
        this.minecraft.gui.setScreen(new PartyFormationScreen(this));
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
                root.y() + 48 + index * ENCOUNTER_ROW_STEP,
                root.width() - UiLayoutMetrics.SPACE_16,
                ENCOUNTER_ROW_HEIGHT);
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

    @Override
    public void removed() {
        enemyPreviews.values().forEach(CharacterEntityPreview::clear);
        enemyPreviews.clear();
        super.removed();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
