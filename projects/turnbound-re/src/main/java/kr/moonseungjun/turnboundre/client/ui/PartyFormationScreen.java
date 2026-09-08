package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.PartyFormationDraft;
import kr.moonseungjun.turnboundre.client.ProgressionClientState;
import kr.moonseungjun.turnboundre.network.ProgressionNetworkPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Production Party Formation + Character detail shell. Persistence and growth truth stay server-owned. */
public final class PartyFormationScreen extends Screen {
    private enum DetailTab { OVERVIEW, SKILLS, GROWTH }

    private static final Identifier TITLE_BOX = Identifier.withDefaultNamespace("advancements/title_box");
    private static final Identifier FRAME_IDLE = Identifier.withDefaultNamespace("advancements/task_frame_unobtained");
    private static final Identifier FRAME_ACTIVE = Identifier.withDefaultNamespace("advancements/task_frame_obtained");
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFAAAAAA;
    private static final int TEXT_WARNING = 0xFFFFCC66;
    private static final int TEXT_SUCCESS = 0xFFAAFFAA;

    private final List<Button> rosterButtons = new ArrayList<>();
    private final List<Button> partyButtons = new ArrayList<>();
    private final CharacterEntityPreview entityPreview = new CharacterEntityPreview();
    private ProgressionNetworkPayloads.Snapshot snapshot;
    private List<String> draftParty = List.of();
    private String selectedCharacterId = "";
    private DetailTab detailTab = DetailTab.OVERVIEW;
    private int selectedSlot;
    private int rosterPage;
    private int rosterPageSize = 1;
    private long seenGeneration = -1L;
    private String feedback = "";
    private boolean feedbackSuccess;
    private Button applyButton;
    private Button resetButton;
    private Button removeButton;
    private Button prevRosterButton;
    private Button nextRosterButton;
    private Button levelUpButton;
    private Button ascendButton;

    public PartyFormationScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.turnbound_re.party_formation"));
    }

    @Override
    protected void init() {
        rosterButtons.clear();
        partyButtons.clear();
        levelUpButton = null;
        ascendButton = null;
        if (!UiLayoutMetrics.supportsPartyScreen(this.width, this.height)) {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> closeScreen())
                    .bounds(Math.max(0, this.width / 2 - 40), Math.max(0, this.height - 28), 80, 20)
                    .build());
            return;
        }

        if (ProgressionClientState.generation() != seenGeneration) syncFromClient();
        UiLayoutMetrics.PartyFormationLayout layout = UiLayoutMetrics.partyFormation(this.width, this.height);
        buildTabs(layout);
        buildRoster(layout);
        buildActiveParty(layout);
        buildGrowthActions(layout);
        buildFooter(layout);
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (ProgressionClientState.generation() != seenGeneration) {
            syncFromClient();
            this.rebuildWidgets();
        }
    }

    @Override
    public void removed() {
        entityPreview.clear();
        super.removed();
    }

    private void syncFromClient() {
        seenGeneration = ProgressionClientState.generation();
        snapshot = ProgressionClientState.snapshot().orElse(null);
        if (snapshot == null) {
            draftParty = List.of();
            selectedCharacterId = "";
            feedback = Component.translatable("screen.turnbound_re.party.loading").getString();
            feedbackSuccess = false;
            return;
        }

        draftParty = snapshot.party();
        if (snapshot.character(selectedCharacterId).isEmpty()) {
            selectedCharacterId = snapshot.characters().stream()
                    .filter(ProgressionNetworkPayloads.CharacterView::owned)
                    .findFirst()
                    .or(() -> snapshot.characters().stream().findFirst())
                    .map(ProgressionNetworkPayloads.CharacterView::id)
                    .orElse("");
        }
        selectedSlot = Math.max(0, Math.min(selectedSlot, 3));
        rosterPage = 0;
        feedback = resultFeedback(snapshot.resultCode(), snapshot.resultDetail());
        feedbackSuccess = "ACCEPTED".equals(snapshot.resultCode()) || "GROWTH_ACCEPTED".equals(snapshot.resultCode());
    }

    private void buildTabs(UiLayoutMetrics.PartyFormationLayout layout) {
        UiLayoutMetrics.Rect tabs = layout.tabs();
        int gap = UiLayoutMetrics.SPACE_4;
        int width = Math.min(82, Math.max(64, (tabs.width() - gap * 2) / 5));
        int x = tabs.x();
        for (DetailTab tab : DetailTab.values()) {
            this.addRenderableWidget(Button.builder(tabLabel(tab), ignored -> selectTab(tab))
                    .bounds(x, tabs.y(), width, tabs.height()).build());
            x += width + gap;
        }
    }

    private void buildRoster(UiLayoutMetrics.PartyFormationLayout layout) {
        UiLayoutMetrics.Rect region = layout.roster();
        int titleHeight = 18;
        int navHeight = 20;
        int rowHeight = 20;
        int gap = UiLayoutMetrics.SPACE_2;
        rosterPageSize = Math.max(1, (region.height() - titleHeight - navHeight - UiLayoutMetrics.SPACE_4) / (rowHeight + gap));

        int y = region.y() + titleHeight;
        for (int i = 0; i < rosterPageSize; i++) {
            final int row = i;
            Button button = Button.builder(Component.empty(), ignored -> selectRosterRow(row))
                    .bounds(region.x(), y, region.width(), rowHeight).build();
            rosterButtons.add(button);
            this.addRenderableWidget(button);
            y += rowHeight + gap;
        }

        int navY = region.bottom() - navHeight;
        int navWidth = Math.max(24, (region.width() - gap) / 2);
        prevRosterButton = Button.builder(Component.literal("<"), ignored -> changeRosterPage(-1))
                .bounds(region.x(), navY, navWidth, navHeight).build();
        nextRosterButton = Button.builder(Component.literal(">"), ignored -> changeRosterPage(1))
                .bounds(region.x() + navWidth + gap, navY, region.width() - navWidth - gap, navHeight).build();
        this.addRenderableWidget(prevRosterButton);
        this.addRenderableWidget(nextRosterButton);
    }

    private void buildActiveParty(UiLayoutMetrics.PartyFormationLayout layout) {
        UiLayoutMetrics.Rect region = layout.activeParty();
        int y = region.y() + 18;
        for (int slot = 0; slot < 4; slot++) {
            final int index = slot;
            Button button = Button.builder(Component.empty(), ignored -> choosePartySlot(index))
                    .bounds(region.x(), y, region.width(), 24).build();
            partyButtons.add(button);
            this.addRenderableWidget(button);
            y += 28;
        }
    }

    private void buildGrowthActions(UiLayoutMetrics.PartyFormationLayout layout) {
        if (detailTab != DetailTab.GROWTH) return;
        UiLayoutMetrics.Rect region = layout.selectedDetail();
        int gap = UiLayoutMetrics.SPACE_4;
        int buttonWidth = Math.max(56, (region.width() - gap) / 2);
        int y = region.bottom() - 20;
        levelUpButton = Button.builder(Component.translatable("screen.turnbound_re.growth.level_up"), ignored -> submitGrowth("LEVEL_UP"))
                .bounds(region.x(), y, buttonWidth, 20).build();
        ascendButton = Button.builder(Component.translatable("screen.turnbound_re.growth.ascend"), ignored -> submitGrowth("ASCEND"))
                .bounds(region.x() + buttonWidth + gap, y, region.width() - buttonWidth - gap, 20).build();
        this.addRenderableWidget(levelUpButton);
        this.addRenderableWidget(ascendButton);
    }

    private void buildFooter(UiLayoutMetrics.PartyFormationLayout layout) {
        UiLayoutMetrics.Rect footer = layout.footer();
        int gap = UiLayoutMetrics.SPACE_4;
        int buttonWidth = Math.min(86, Math.max(54, (footer.width() - gap * 3) / 4));
        int y = footer.y() + Math.max(0, (footer.height() - 20) / 2);

        removeButton = Button.builder(Component.translatable("screen.turnbound_re.party.remove_slot"), ignored -> removeSelectedSlot())
                .bounds(footer.x(), y, buttonWidth, 20).build();
        resetButton = Button.builder(Component.translatable("screen.turnbound_re.party.reset"), ignored -> resetDraft())
                .bounds(removeButton.getRight() + gap, y, buttonWidth, 20).build();
        applyButton = Button.builder(Component.translatable("screen.turnbound_re.party.apply"), ignored -> submitDraft())
                .bounds(resetButton.getRight() + gap, y, buttonWidth, 20).build();
        int doneX = footer.right() - buttonWidth;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> closeScreen())
                .bounds(doneX, y, buttonWidth, 20).build());
        this.addRenderableWidget(removeButton);
        this.addRenderableWidget(resetButton);
        this.addRenderableWidget(applyButton);
    }

    private void selectTab(DetailTab tab) {
        if (detailTab == tab) return;
        detailTab = tab;
        feedback = "";
        feedbackSuccess = false;
        this.rebuildWidgets();
    }

    private void selectRosterRow(int row) {
        if (snapshot == null) return;
        int index = rosterPage * rosterPageSize + row;
        if (index < 0 || index >= snapshot.characters().size()) return;
        selectedCharacterId = snapshot.characters().get(index).id();
        feedback = "";
        feedbackSuccess = false;
        refreshButtons();
    }

    private void choosePartySlot(int slot) {
        selectedSlot = slot;
        if (snapshot == null || selectedCharacterId.isBlank()) {
            refreshButtons();
            return;
        }
        ProgressionNetworkPayloads.CharacterView selected = snapshot.character(selectedCharacterId).orElse(null);
        if (selected == null || !selected.owned()) {
            feedback = Component.translatable("screen.turnbound_re.party.locked_cannot_assign").getString();
            feedbackSuccess = false;
            refreshButtons();
            return;
        }
        draftParty = PartyFormationDraft.assign(draftParty, slot, selectedCharacterId);
        feedback = previewFeedback();
        feedbackSuccess = PartyFormationDraft.cost(draftParty, snapshot) <= snapshot.partyCapacity();
        refreshButtons();
    }

    private void removeSelectedSlot() {
        draftParty = PartyFormationDraft.remove(draftParty, selectedSlot);
        feedback = previewFeedback();
        feedbackSuccess = true;
        refreshButtons();
    }

    private void resetDraft() {
        if (snapshot == null) return;
        draftParty = snapshot.party();
        feedback = "";
        feedbackSuccess = false;
        refreshButtons();
    }

    private void submitDraft() {
        if (snapshot == null || !PartyFormationDraft.isSubmittable(draftParty, snapshot) || draftParty.equals(snapshot.party())) return;
        feedback = Component.translatable("screen.turnbound_re.party.saving").getString();
        feedbackSuccess = false;
        ClientPacketDistributor.sendToServer(ProgressionNetworkPayloads.SetPartyC2S.of(snapshot.party(), draftParty));
        refreshButtons();
    }

    private void submitGrowth(String operation) {
        if (snapshot == null) return;
        ProgressionNetworkPayloads.CharacterView character = snapshot.character(selectedCharacterId).orElse(null);
        if (character == null || !character.owned()) return;
        boolean allowed = "LEVEL_UP".equals(operation) ? character.growth().canLevelUp() : character.growth().canAscend();
        if (!allowed) return;
        feedback = Component.translatable("screen.turnbound_re.growth.saving").getString();
        feedbackSuccess = false;
        ClientPacketDistributor.sendToServer(ProgressionNetworkPayloads.GrowthC2S.of(
                operation, character.id(), character.currentStar(), character.level()));
        refreshButtons();
    }

    private void changeRosterPage(int delta) {
        if (snapshot == null) return;
        int pages = Math.max(1, (snapshot.characters().size() + rosterPageSize - 1) / rosterPageSize);
        rosterPage = Math.max(0, Math.min(pages - 1, rosterPage + delta));
        refreshButtons();
    }

    private void refreshButtons() {
        if (snapshot == null) {
            for (Button button : rosterButtons) button.active = false;
            for (Button button : partyButtons) button.active = false;
            if (applyButton != null) applyButton.active = false;
            if (resetButton != null) resetButton.active = false;
            if (removeButton != null) removeButton.active = false;
            if (prevRosterButton != null) prevRosterButton.active = false;
            if (nextRosterButton != null) nextRosterButton.active = false;
            if (levelUpButton != null) levelUpButton.active = false;
            if (ascendButton != null) ascendButton.active = false;
            return;
        }

        int from = rosterPage * rosterPageSize;
        for (int row = 0; row < rosterButtons.size(); row++) {
            Button button = rosterButtons.get(row);
            int index = from + row;
            if (index >= snapshot.characters().size()) {
                button.setMessage(Component.empty());
                button.active = false;
                continue;
            }
            ProgressionNetworkPayloads.CharacterView character = snapshot.characters().get(index);
            boolean selected = character.id().equals(selectedCharacterId);
            boolean active = draftParty.contains(character.id());
            String prefix = selected ? "> " : (active ? "• " : "");
            String state = character.owned()
                    ? stars(character.currentStar()) + " Lv" + character.level() + " C" + character.squadCost()
                    : Component.translatable("screen.turnbound_re.party.locked_short").getString();
            button.setMessage(Component.literal(fit(prefix + displayName(character.id()) + "  " + state, button.getWidth() - 8)));
            button.active = true;
        }

        for (int slot = 0; slot < partyButtons.size(); slot++) {
            Button button = partyButtons.get(slot);
            String label;
            if (slot < draftParty.size()) {
                String id = draftParty.get(slot);
                int cost = snapshot.character(id).map(ProgressionNetworkPayloads.CharacterView::squadCost).orElse(0);
                label = (selectedSlot == slot ? "> " : "") + (slot + 1) + " · " + displayName(id) + " · C" + cost;
            } else {
                label = (selectedSlot == slot ? "> " : "") + (slot + 1) + " · "
                        + Component.translatable("screen.turnbound_re.party.empty_slot").getString();
            }
            button.setMessage(Component.literal(fit(label, button.getWidth() - 8)));
            button.active = true;
        }

        int pages = Math.max(1, (snapshot.characters().size() + rosterPageSize - 1) / rosterPageSize);
        prevRosterButton.active = rosterPage > 0;
        nextRosterButton.active = rosterPage + 1 < pages;
        removeButton.active = selectedSlot < draftParty.size();
        resetButton.active = !draftParty.equals(snapshot.party());
        applyButton.active = !draftParty.equals(snapshot.party()) && PartyFormationDraft.isSubmittable(draftParty, snapshot);

        ProgressionNetworkPayloads.CharacterView selected = snapshot.character(selectedCharacterId).orElse(null);
        if (levelUpButton != null) levelUpButton.active = selected != null && selected.growth().canLevelUp();
        if (ascendButton != null) ascendButton.active = selected != null && selected.growth().canAscend();
    }

    private String previewFeedback() {
        if (snapshot == null) return "";
        int cost = PartyFormationDraft.cost(draftParty, snapshot);
        return Component.translatable(cost > snapshot.partyCapacity()
                ? "screen.turnbound_re.party.cost_over" : "screen.turnbound_re.party.cost_ok", cost, snapshot.partyCapacity()).getString();
    }

    private String resultFeedback(String resultCode, String detail) {
        if (resultCode == null || resultCode.isBlank()) return "";
        return switch (resultCode) {
            case "ACCEPTED" -> Component.translatable("screen.turnbound_re.party.saved").getString();
            case "STALE_PARTY" -> Component.translatable("screen.turnbound_re.party.stale").getString();
            case "PARTY_COST_EXCEEDED" -> Component.translatable("screen.turnbound_re.party.server_cost_rejected", detail).getString();
            case "NOT_OWNED" -> Component.translatable("screen.turnbound_re.party.server_not_owned").getString();
            case "INVALID_PARTY" -> Component.translatable("screen.turnbound_re.party.server_invalid").getString();
            case "GROWTH_ACCEPTED" -> Component.translatable("screen.turnbound_re.growth.saved").getString();
            case "GROWTH_STALE" -> Component.translatable("screen.turnbound_re.growth.stale").getString();
            case "GROWTH_INSUFFICIENT_COIN" -> Component.translatable("screen.turnbound_re.growth.insufficient_coin").getString();
            case "GROWTH_INSUFFICIENT_ESSENCE" -> Component.translatable("screen.turnbound_re.growth.insufficient_essence").getString();
            case "GROWTH_INSUFFICIENT_SHARDS" -> Component.translatable("screen.turnbound_re.growth.insufficient_shards").getString();
            case "GROWTH_LEVEL_CAP" -> Component.translatable("screen.turnbound_re.growth.level_cap").getString();
            case "GROWTH_NOT_AT_LEVEL_CAP" -> Component.translatable("screen.turnbound_re.growth.not_at_level_cap").getString();
            case "GROWTH_MAX_STAR" -> Component.translatable("screen.turnbound_re.growth.max_star").getString();
            case "GROWTH_NOT_OWNED" -> Component.translatable("screen.turnbound_re.growth.not_owned").getString();
            default -> Component.translatable(resultCode.startsWith("GROWTH_")
                    ? "screen.turnbound_re.growth.server_rejected" : "screen.turnbound_re.party.server_rejected").getString();
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!UiLayoutMetrics.supportsPartyScreen(this.width, this.height)) {
            graphics.text(this.font, Component.translatable("screen.turnbound_re.party.canvas_too_small"),
                    Math.max(8, this.width / 2 - 100), Math.max(8, this.height / 2 - 20), TEXT_WARNING, true);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        UiLayoutMetrics.PartyFormationLayout layout = UiLayoutMetrics.partyFormation(this.width, this.height);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX,
                layout.header().x(), layout.header().y(), layout.header().width(), layout.header().height());
        graphics.text(this.font, this.title, layout.header().x() + UiLayoutMetrics.SPACE_8,
                layout.header().y() + 7, TEXT_PRIMARY, true);

        if (snapshot == null) {
            graphics.text(this.font, Component.translatable("screen.turnbound_re.party.loading"),
                    layout.root().x() + UiLayoutMetrics.SPACE_8, layout.roster().y(), TEXT_SECONDARY, true);
        } else {
            int draftCost = PartyFormationDraft.cost(draftParty, snapshot);
            String cost = Component.translatable("screen.turnbound_re.party.squad_cost", draftCost, snapshot.partyCapacity()).getString();
            graphics.text(this.font, Component.literal(cost),
                    layout.header().right() - UiLayoutMetrics.SPACE_8 - this.font.width(cost),
                    layout.header().y() + 7, draftCost > snapshot.partyCapacity() ? TEXT_WARNING : TEXT_PRIMARY, true);

            graphics.text(this.font, Component.translatable("screen.turnbound_re.party.roster"),
                    layout.roster().x(), layout.roster().y() + 4, TEXT_PRIMARY, true);
            graphics.text(this.font, Component.translatable("screen.turnbound_re.party.active_party"),
                    layout.activeParty().x(), layout.activeParty().y() + 4, TEXT_PRIMARY, true);
            graphics.text(this.font, tabLabel(detailTab),
                    layout.selectedDetail().x(), layout.selectedDetail().y() + 4, TEXT_PRIMARY, true);
            renderSelectedDetail(graphics, layout.selectedDetail());
        }

        if (!feedback.isBlank()) {
            int feedbackX = layout.tabs().x() + Math.min(270, layout.tabs().width() / 2);
            graphics.text(this.font, Component.literal(fit(feedback, layout.tabs().right() - feedbackX)),
                    feedbackX, layout.tabs().y() + 6, feedbackSuccess ? TEXT_SUCCESS : TEXT_WARNING, true);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSelectedDetail(GuiGraphicsExtractor graphics, UiLayoutMetrics.Rect region) {
        if (snapshot == null || selectedCharacterId.isBlank()) return;
        ProgressionNetworkPayloads.CharacterView character = snapshot.character(selectedCharacterId).orElse(null);
        if (character == null) return;

        EntityPreviewLayout.PreviewSpec preview = detailTab == DetailTab.OVERVIEW
                ? entityPreview.layout(this.minecraft,
                        ProgressionClientState.sourceEntity(character.id()).orElse(""), region.width(), region.height())
                : EntityPreviewLayout.PreviewSpec.hidden(region.width());
        if (preview.visible()) entityPreview.extract(graphics, region, preview);
        int textWidth = preview.visible() ? preview.textWidth() : region.width();

        int y = renderCharacterHeader(graphics, region, character, textWidth);
        if (!character.owned()) {
            graphics.text(this.font, Component.translatable("screen.turnbound_re.party.locked_detail"),
                    region.x(), y, TEXT_SECONDARY, true);
            return;
        }
        switch (detailTab) {
            case OVERVIEW -> renderOverview(graphics, region, character, y, textWidth);
            case SKILLS -> renderSkills(graphics, region, character, y);
            case GROWTH -> renderGrowth(graphics, region, character, y);
        }
    }

    private int renderCharacterHeader(GuiGraphicsExtractor graphics, UiLayoutMetrics.Rect region,
                                      ProgressionNetworkPayloads.CharacterView character, int textWidth) {
        int x = region.x();
        int y = region.y() + 18;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, character.owned() ? FRAME_ACTIVE : FRAME_IDLE, x, y, 20, 20);
        graphics.text(this.font, Component.literal(fit(displayName(character.id()), Math.max(1, textWidth - 26))), x + 26, y + 2,
                character.owned() ? TEXT_PRIMARY : TEXT_SECONDARY, true);
        String progression = character.owned()
                ? stars(character.currentStar()) + "  Lv" + character.level() + "/" + character.levelCap()
                : stars(character.originStar()) + "  " + Component.translatable("screen.turnbound_re.party.locked_short").getString();
        graphics.text(this.font, Component.literal(fit(progression, Math.max(1, textWidth - 26))), x + 26, y + 12, TEXT_SECONDARY, true);
        return y + 30;
    }

    private void renderOverview(GuiGraphicsExtractor graphics, UiLayoutMetrics.Rect region,
                                ProgressionNetworkPayloads.CharacterView character, int y, int textWidth) {
        int x = region.x();
        String roles = character.roles().stream().map(PartyFormationScreen::roleName).reduce((a, b) -> a + " · " + b).orElse("-");
        line(graphics, x, y, roles + "  C" + character.squadCost(), TEXT_PRIMARY, textWidth); y += 12;
        line(graphics, x, y, "HP " + character.hp() + "   ATK " + character.atk(), TEXT_PRIMARY, textWidth); y += 11;
        line(graphics, x, y, "DEF " + character.def() + "   SPD " + character.spd() + "   P " + character.poise(), TEXT_PRIMARY, textWidth); y += 13;
        if (!character.affinities().isEmpty()) {
            String affinity = character.affinities().stream().limit(3).map(PartyFormationScreen::affinityName)
                    .reduce((a, b) -> a + " · " + b).orElse("");
            line(graphics, x, y, affinity, TEXT_SECONDARY, textWidth); y += 12;
        }
        line(graphics, x, y, Component.translatable("screen.turnbound_re.party.basic").getString() + "  " + displayName(character.basicAction()), TEXT_PRIMARY, textWidth); y += 11;
        String skills = character.skills().stream().map(PartyFormationScreen::displayName).reduce((a, b) -> a + " / " + b).orElse("-");
        line(graphics, x, y, Component.translatable("screen.turnbound_re.party.skills").getString() + "  " + skills, TEXT_PRIMARY, textWidth); y += 11;
        line(graphics, x, y, Component.translatable("screen.turnbound_re.party.burst").getString() + "  " + displayName(character.burst()), TEXT_PRIMARY, textWidth); y += 11;
        String passives = character.passives().stream().map(PartyFormationScreen::displayName).reduce((a, b) -> a + " / " + b).orElse("-");
        line(graphics, x, y, Component.translatable("screen.turnbound_re.party.passive").getString() + "  " + passives, TEXT_SECONDARY, textWidth);
    }

    private void renderSkills(GuiGraphicsExtractor graphics, UiLayoutMetrics.Rect region,
                              ProgressionNetworkPayloads.CharacterView character, int y) {
        int x = region.x();
        int maxY = region.bottom() - 2;
        for (ProgressionNetworkPayloads.ActionView action : character.actions()) {
            if (y + 18 > maxY) break;
            String energy = action.energyDelta() > 0 ? "+" + action.energyDelta() : Integer.toString(action.energyDelta());
            line(graphics, x, y, actionKind(action.kind()) + " · " + displayName(action.id()) + " · E " + energy,
                    TEXT_PRIMARY, region.width());
            y += 10;
            String facts = "HP " + action.hpPower() + " · P " + action.poisePower() + " · "
                    + damageTagName(action.damageTag()) + " · " + targetName(action);
            String special = firstSpecialEffect(action.effects());
            if (!special.isBlank()) facts += " · " + special;
            line(graphics, x, y, facts, TEXT_SECONDARY, region.width());
            y += 9;
        }
    }

    private void renderGrowth(GuiGraphicsExtractor graphics, UiLayoutMetrics.Rect region,
                              ProgressionNetworkPayloads.CharacterView character, int y) {
        int x = region.x();
        ProgressionNetworkPayloads.GrowthView growth = character.growth();
        line(graphics, x, y, Component.translatable("screen.turnbound_re.growth.wallet",
                snapshot.coin(), snapshot.essence(), growth.shardBalance()).getString(), TEXT_SECONDARY, region.width()); y += 12;

        line(graphics, x, y, Component.translatable("screen.turnbound_re.growth.level_preview",
                character.level(), growth.nextLevel()).getString(), TEXT_PRIMARY, region.width()); y += 10;
        line(graphics, x, y, statDelta(character, growth.nextLevelStats()), TEXT_SECONDARY, region.width()); y += 10;
        line(graphics, x, y, costText(growth.levelCost()), TEXT_SECONDARY, region.width()); y += 10;
        if (!growth.levelBlockCode().isBlank()) {
            line(graphics, x, y, growthBlock(growth.levelBlockCode()), TEXT_WARNING, region.width()); y += 12;
        } else y += 2;

        line(graphics, x, y, Component.translatable("screen.turnbound_re.growth.ascend_preview",
                character.currentStar(), growth.nextStar(), growth.nextLevelCap()).getString(), TEXT_PRIMARY, region.width()); y += 10;
        line(graphics, x, y, statDelta(character, growth.nextStarStats()), TEXT_SECONDARY, region.width()); y += 10;
        line(graphics, x, y, costText(growth.ascendCost()), TEXT_SECONDARY, region.width()); y += 10;
        if (!growth.ascendBlockCode().isBlank()) {
            line(graphics, x, y, growthBlock(growth.ascendBlockCode()), TEXT_WARNING, region.width());
        }
    }

    private String statDelta(ProgressionNetworkPayloads.CharacterView current, ProgressionNetworkPayloads.StatsView next) {
        return "HP " + current.hp() + "→" + next.hp() + "  ATK " + current.atk() + "→" + next.atk()
                + "  DEF " + current.def() + "→" + next.def() + "  SPD " + current.spd() + "→" + next.spd()
                + "  P " + current.poise() + "→" + next.poise();
    }

    private String costText(ProgressionNetworkPayloads.CostView cost) {
        return Component.translatable("screen.turnbound_re.growth.cost", cost.coin(), cost.essence(), cost.shards()).getString();
    }

    private static String growthBlock(String code) {
        String key = switch (code) {
            case "LEVEL_CAP" -> "screen.turnbound_re.growth.level_cap";
            case "NOT_AT_LEVEL_CAP" -> "screen.turnbound_re.growth.not_at_level_cap";
            case "MAX_STAR" -> "screen.turnbound_re.growth.max_star";
            case "INSUFFICIENT_COIN" -> "screen.turnbound_re.growth.insufficient_coin";
            case "INSUFFICIENT_ESSENCE" -> "screen.turnbound_re.growth.insufficient_essence";
            case "INSUFFICIENT_SHARDS" -> "screen.turnbound_re.growth.insufficient_shards";
            case "NOT_OWNED" -> "screen.turnbound_re.growth.not_owned";
            default -> "screen.turnbound_re.growth.server_rejected";
        };
        return Component.translatable(key).getString();
    }

    private static Component tabLabel(DetailTab tab) {
        return Component.translatable(switch (tab) {
            case OVERVIEW -> "screen.turnbound_re.tab.overview";
            case SKILLS -> "screen.turnbound_re.tab.skills";
            case GROWTH -> "screen.turnbound_re.tab.growth";
        });
    }

    private static String actionKind(String kind) {
        return Component.translatable("screen.turnbound_re.skill.kind." + kind.toLowerCase(Locale.ROOT)).getString();
    }

    private static String targetName(ProgressionNetworkPayloads.ActionView action) {
        String team = Component.translatable("screen.turnbound_re.target_team." + action.targetTeam().toLowerCase(Locale.ROOT)).getString();
        String shape = Component.translatable("screen.turnbound_re.target_shape." + action.targetShape().toLowerCase(Locale.ROOT)).getString();
        return team + " " + shape + " ×" + action.targetCount();
    }

    private static String firstSpecialEffect(List<ProgressionNetworkPayloads.EffectView> effects) {
        for (ProgressionNetworkPayloads.EffectView effect : effects) {
            if ("DAMAGE".equals(effect.type()) || "HEAL".equals(effect.type()) || "NONE".equals(effect.type())) continue;
            String name = Component.translatable("screen.turnbound_re.skill.effect." + effect.type().toLowerCase(Locale.ROOT)).getString();
            String status = effect.status().isBlank() ? "" : " " + displayName(effect.status());
            String duration = effect.duration() > 0 ? " " + effect.duration() + "T" : "";
            return name + status + duration;
        }
        return "";
    }

    private static String roleName(String role) {
        if (role == null || role.isBlank()) return "?";
        return Component.translatable("screen.turnbound_re.party.role." + role.toLowerCase(Locale.ROOT)).getString();
    }

    private static String affinityName(String packed) {
        if (packed == null || packed.isBlank()) return "";
        String[] parts = packed.split("=", 2);
        if (parts.length != 2) return displayName(packed);
        String tag = damageTagName(parts[0]);
        String grade = Component.translatable("screen.turnbound_re.party.affinity." + parts[1].toLowerCase(Locale.ROOT)).getString();
        return tag + " " + grade;
    }

    private static String damageTagName(String tag) {
        return Component.translatable("screen.turnbound_re.damage_tag." + tag.toLowerCase(Locale.ROOT)).getString();
    }

    private static String stars(int count) { return "★".repeat(Math.max(0, count)); }

    private static String displayName(String id) {
        if (id == null || id.isBlank()) return "?";
        int colon = id.indexOf(':');
        String raw = colon >= 0 ? id.substring(colon + 1) : id;
        String[] words = raw.replace('-', '_').split("_+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.isEmpty() ? "?" : out.toString();
    }

    private void line(GuiGraphicsExtractor graphics, int x, int y, String text, int color, int maxWidth) {
        graphics.text(this.font, Component.literal(fit(text, maxWidth)), x, y, color, true);
    }

    private String fit(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (this.font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int suffixWidth = this.font.width(suffix);
        if (suffixWidth >= maxWidth) return "";
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end)) + suffixWidth > maxWidth) end--;
        return end <= 0 ? "" : text.substring(0, end) + suffix;
    }

    private void closeScreen() {
        this.minecraft.gui.setScreen(null);
    }
}
