package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.progression.GachaCatalog;
import io.github.q93503128.turnbound.progression.GrowthRulesV1;
import io.github.q93503128.turnbound.progression.StarEssenceExchangeRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/** Physical summon archive. Pull presentation is a separate full-screen 2D cinematic. */
final class FacilitySummonScreen extends FacilityScreen {
    private enum Page { SUMMON, EXCHANGE, DUPLICATES }

    private Page page = Page.SUMMON;
    private int duplicatePage;

    FacilitySummonScreen() {
        super("정령의 기록", 0xFFC794FF, 500, 270, 376, 232);
    }

    @Override
    protected boolean immersivePresentation() {
        return true;
    }

    @Override
    protected String subtitle() {
        return "";
    }

    @Override
    protected void buildFacility() {
        int tabGap = 5;
        int tabW = Math.min(92, Math.max(70, (width - 34 - tabGap * 2) / 3));
        int tabY = 13;
        int tabX = 16;
        var summonTab = new BattleHudButton(tabX, tabY, tabW, 18,
                Component.literal("소환"), 0xFF6DC6FF, ignored -> switchPage(Page.SUMMON));
        summonTab.active = page != Page.SUMMON;
        addRenderableWidget(summonTab);
        var exchangeTab = new BattleHudButton(tabX + tabW + tabGap, tabY, tabW, 18,
                Component.literal("별의 정수"), 0xFFC794FF, ignored -> switchPage(Page.EXCHANGE));
        exchangeTab.active = page != Page.EXCHANGE;
        addRenderableWidget(exchangeTab);
        int pendingCount = ClientMetaState.snapshot().pendingDuplicates().stream()
                .mapToInt(ClientMetaState.PendingDuplicateRow::count).sum();
        var duplicateTab = new BattleHudButton(tabX + (tabW + tabGap) * 2, tabY, tabW, 18,
                Component.literal("중복 보상" + (pendingCount > 0 ? " " + pendingCount : "")), 0xFFFFC857,
                ignored -> switchPage(Page.DUPLICATES));
        duplicateTab.active = page != Page.DUPLICATES;
        addRenderableWidget(duplicateTab);

        if (page == Page.SUMMON) buildSummonControls();
        else if (page == Page.EXCHANGE) buildExchangeControls();
        else buildDuplicateControls();
    }

    private void buildSummonControls() {
        var state = ClientMetaState.snapshot();
        int gap = 8;
        int maxButton = 162;
        int totalW = Math.min(width - 28, maxButton * 2 + gap);
        int buttonW = (totalW - gap) / 2;
        int x = (width - totalW) / 2;
        int y = height - 34;

        var one = new BattleHudButton(x, y, buttonW, 22,
                Component.literal("1회 소환 · 300"), 0xFF6DC6FF, ignored -> send("SUMMON1"));
        one.active = state.crystal() >= GachaCatalog.SINGLE_COST;
        addRenderableWidget(one);

        boolean starter = state.starterArchiveAvailable();
        String tenLabel = starter ? "첫 10회 · ★4+" : "10회 소환 · 3000";
        String action = starter ? "STARTER" : "SUMMON10";
        var ten = new BattleHudButton(x + buttonW + gap, y, buttonW, 22,
                Component.literal(tenLabel), 0xFFFFC857, ignored -> send(action));
        ten.active = state.crystal() >= GachaCatalog.TEN_COST;
        addRenderableWidget(ten);
    }

    private void buildExchangeControls() {
        var state = ClientMetaState.snapshot();
        int panelW = Math.min(520, width - 30);
        int panelX = (width - panelW) / 2;
        int topY = 58;
        var crystal = new BattleHudButton(
                panelX + 12, topY + 28, panelW - 24, 19,
                Component.literal("별의 정수 150 → 크리스탈 300"), 0xFFC794FF,
                ignored -> send("ESSENCE_CRYSTAL"));
        crystal.active = state.essence() >= StarEssenceExchangeRules.CRYSTAL_COST;
        addRenderableWidget(crystal);

        var choices = state.characters().stream()
                .filter(ClientMetaState.CharacterRow::owned)
                .filter(row -> row.nativeStar() == 4 || row.nativeStar() == 5)
                .sorted(Comparator.comparingInt(ClientMetaState.CharacterRow::nativeStar).reversed()
                        .thenComparing(ClientMetaState.CharacterRow::name))
                .toList();
        int gridY = topY + 57;
        int cols = 3;
        int cardGap = 4;
        int cardW = (panelW - 24 - cardGap * (cols - 1)) / cols;
        int maxRows = Math.max(1, (height - gridY - 20) / 22);
        int limit = Math.min(choices.size(), maxRows * cols);
        for (int i = 0; i < limit; i++) {
            var row = choices.get(i);
            int cost = StarEssenceExchangeRules.choiceCost(row.nativeStar());
            int xx = panelX + 12 + (i % cols) * (cardW + cardGap);
            int yy = gridY + (i / cols) * 22;
            String bonus = row.bonusLevel() >= GrowthRulesV1.duplicateBonusMax() ? "MAX" : "+" + row.bonusLevel();
            var button = new BattleHudButton(
                    xx, yy, cardW, 18,
                    Component.literal("★" + row.nativeStar() + " " + row.name() + " " + bonus + " · " + cost),
                    row.nativeStar() == 5 ? 0xFFFFC857 : 0xFFC794FF,
                    ignored -> send("ESSENCE_PICK" + row.nativeStar() + "|" + row.id()));
            button.active = state.essence() >= cost;
            addRenderableWidget(button);
        }
    }

    private void buildDuplicateControls() {
        var state = ClientMetaState.snapshot();
        var pending = state.pendingDuplicates();
        int rowCount = Math.max(1, (height - 110) / 38);
        int pages = Math.max(1, (pending.size() + rowCount - 1) / rowCount);
        duplicatePage = Math.min(duplicatePage, pages - 1);
        int widthAvailable = Math.min(520, width - 30);
        int panelX = (width - widthAvailable) / 2;
        int buttonGap = 6;
        int twoButtonsW = widthAvailable - 24;
        int choiceW = (twoButtonsW - buttonGap) / 2;
        for (int index = duplicatePage * rowCount; index < Math.min(pending.size(), (duplicatePage + 1) * rowCount); index++) {
            var row = pending.get(index);
            int y = 92 + (index - duplicatePage * rowCount) * 38;
            var level = new BattleHudButton(panelX + 12, y, choiceW, 18,
                    Component.literal("+레벨 1 (현재 +" + row.bonusLevel() + ")"), 0xFF6DC6FF,
                    ignored -> send("DUP_LEVEL|" + row.characterId()));
            level.active = row.bonusLevel() < GrowthRulesV1.duplicateBonusMax();
            addRenderableWidget(level);
            var essence = new BattleHudButton(panelX + 12 + choiceW + buttonGap, y, choiceW, 18,
                    Component.literal("별의 정수 +" + GachaCatalog.duplicateEssence(row.nativeStars())), 0xFFC794FF,
                    ignored -> send("DUP_ESSENCE|" + row.characterId()));
            addRenderableWidget(essence);
        }
        if (pages > 1) {
            int y = height - 28;
            var previous = new BattleHudButton(panelX + 12, y, 78, 18,
                    Component.literal("이전"), 0xFFC794FF, ignored -> { duplicatePage--; refreshSnapshot(); });
            previous.active = duplicatePage > 0;
            addRenderableWidget(previous);
            var next = new BattleHudButton(panelX + widthAvailable - 90, y, 78, 18,
                    Component.literal("다음"), 0xFFC794FF, ignored -> { duplicatePage++; refreshSnapshot(); });
            next.active = duplicatePage + 1 < pages;
            addRenderableWidget(next);
        }
    }

    private void renderDuplicatePage(GuiGraphicsExtractor g) {
        int panelW = Math.min(520, width - 30);
        int x = (width - panelW) / 2;
        g.text(font, Component.literal("중복 동료 보상"), x + 12, 49, GachaPresentationScreen.TEXT, true);
        g.text(font, Component.literal("각 중복마다 +레벨 또는 별의 정수 중 하나를 선택합니다."),
                x + 12, 64, GachaPresentationScreen.SECONDARY, false);
        var pending = ClientMetaState.snapshot().pendingDuplicates();
        int rows = Math.max(1, (height - 110) / 38);
        if (pending.isEmpty()) {
            g.text(font, Component.literal("선택할 중복 보상이 없습니다."), x + 12, 92,
                    GachaPresentationScreen.SECONDARY, false);
        }
        for (int i = duplicatePage * rows; i < Math.min(pending.size(), (duplicatePage + 1) * rows); i++) {
            var row = pending.get(i);
            String name = GachaPresentationScreen.characterName(row.characterId());
            String label = "★" + row.nativeStars() + " " + name + " · 선택 " + row.count() + "회";
            g.text(font, Component.literal(UiTextLayout.fit(label, panelW - 24)),
                    x + 12, 81 + (i - duplicatePage * rows) * 38, GachaPresentationScreen.TEXT, false);
        }
    }

    @Override
    protected void renderFacility(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF050815);
        SummonLayerRenderer.fullScreen(g, SummonAssets.BG_ARCHIVE, width, height);
        if (page == Page.SUMMON) renderSummonPage(g);
        else if (page == Page.EXCHANGE) renderExchangePage(g);
        else renderDuplicatePage(g);
    }

    private void renderSummonPage(GuiGraphicsExtractor g) {
        var state = ClientMetaState.snapshot();
        g.text(font, Component.literal("정령의 기록"), 18, 42, GachaPresentationScreen.TEXT, true);
        g.text(font, Component.literal("기록에 응답한 동료와 새로운 인연을 맺습니다."),
                18, 60, GachaPresentationScreen.SECONDARY, false);
        g.text(font, Component.literal("★5 3%   ·   ★4 12%"), 18, 80, GachaPresentationScreen.GOLD, true);

        String crystal = "크리스탈 " + state.crystal();
        String pity = "★5 천장 " + state.fiveStarPity() + "/" + GachaCatalog.HARD_PITY;
        int hudW = Math.max(font.width(crystal), font.width(pity));
        int hx = Math.max(18, width - hudW - 18);
        g.text(font, Component.literal(crystal), hx, 42, GachaPresentationScreen.TEXT, true);
        g.text(font, Component.literal(pity), hx, 59, GachaPresentationScreen.SECONDARY, false);

        int portraitBottom = height - (width < 620 ? 70 : 82);
        int portraitHeight = Math.max(84, Math.min(Math.round(height * 0.66F), Math.round(width * 0.32F)));
        if (width < 620) {
            int singleHeight = Math.min(portraitHeight, Math.round(width * 0.50F));
            drawPortrait(g, "P02", width * 3 / 4, portraitBottom, singleHeight);
        } else {
            drawPortrait(g, "P01", width / 2 - portraitHeight * 43 / 100,
                    portraitBottom, portraitHeight * 3 / 4);
            drawPortrait(g, "P02", width / 2, portraitBottom, portraitHeight);
            drawPortrait(g, "P05", width / 2 + portraitHeight * 43 / 100,
                    portraitBottom, portraitHeight * 3 / 4);
        }

        if (state.starterArchiveAvailable()) {
            String starter = "첫 10회는 ★4 이상 동료가 반드시 응답합니다";
            String fit = UiTextLayout.fit(starter, Math.max(1, width - 28));
            g.text(font, Component.literal(fit), Math.max(14, width / 2 - font.width(fit) / 2),
                    height - 54, GachaPresentationScreen.PURPLE, true);
        }
    }

    private static void drawPortrait(GuiGraphicsExtractor graphics, String characterId,
                                     int centerX, int bottomY, int portraitHeight) {
        int safeHeight = Math.max(1, portraitHeight);
        int portraitWidth = safeHeight * 2 / 3;
        SummonLayerRenderer.portrait(graphics, characterId, false,
                centerX - portraitWidth / 2, bottomY - safeHeight, portraitWidth, safeHeight);
    }

    private void renderExchangePage(GuiGraphicsExtractor g) {
        int panelW = Math.min(520, width - 30);
        int panelH = Math.min(250, height - 72);
        int x = (width - panelW) / 2;
        int y = 50;
        TurnboundUiSkin.panel(g, x, y, panelW, panelH);
        g.text(font, Component.literal("별의 정수"), x + 14, y + 12, TurnboundUiTokens.TEXT_PRIMARY, true);
        g.text(font, Component.literal("보유한 ★4/★5 동료의 기록을 강화하거나 크리스탈로 교환합니다."),
                x + 14, y + 28, TurnboundUiTokens.TEXT_SECONDARY, false);
    }

    private void switchPage(Page next) {
        if (page == next) return;
        page = next;
        refreshSnapshot();
    }

    private void send(String raw) {
        ClientPacketDistributor.sendToServer(new MetaCommandPayload(raw));
    }
}
