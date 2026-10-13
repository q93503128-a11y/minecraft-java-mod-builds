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
    private enum Page { SUMMON, EXCHANGE }

    private Page page = Page.SUMMON;

    FacilitySummonScreen() {
        super("정령의 기록", 0xFFC794FF, 500, 270, 376, 232);
    }

    @Override
    protected String subtitle() {
        var state = ClientMetaState.snapshot();
        return "크리스탈 " + state.crystal()
                + " · 별의 정수 " + state.essence()
                + " · ★5 천장 " + state.fiveStarPity() + "/" + GachaCatalog.HARD_PITY;
    }

    @Override
    protected void buildFacility() {
        int x = left + 12;
        int y = bodyTop() + 3;
        int tabsW = panelWidth - 24;
        int tabGap = 4;
        int tabW = (tabsW - tabGap) / 2;
        addRenderableWidget(new BattleHudButton(x, y, tabW, 18, Component.literal("소환"), 0xFF6DC6FF,
                ignored -> switchPage(Page.SUMMON))).active = page != Page.SUMMON;
        addRenderableWidget(new BattleHudButton(x + tabW + tabGap, y, tabW, 18, Component.literal("별의 정수"), 0xFFC794FF,
                ignored -> switchPage(Page.EXCHANGE))).active = page != Page.EXCHANGE;

        if (page == Page.SUMMON) buildSummonControls();
        else buildExchangeControls();
    }

    private void buildSummonControls() {
        var state = ClientMetaState.snapshot();
        int x = left + 18;
        int bottom = bodyBottom() - 4;
        int gap = 6;
        int w = (panelWidth - 36 - gap) / 2;
        int y = bottom - 22;

        var one = new BattleHudButton(x, y, w, 20,
                Component.literal("1회 소환 · 300"), 0xFF6DC6FF, ignored -> send("SUMMON1"));
        one.active = state.crystal() >= GachaCatalog.SINGLE_COST;
        addRenderableWidget(one);

        boolean starter = state.starterArchiveAvailable();
        String tenLabel = starter ? "첫 10회 · ★4+ · 3000" : "10회 소환 · 3000";
        String action = starter ? "STARTER" : "SUMMON10";
        var ten = new BattleHudButton(x + w + gap, y, w, 20,
                Component.literal(tenLabel), 0xFFFFC857, ignored -> send(action));
        ten.active = state.crystal() >= GachaCatalog.TEN_COST;
        addRenderableWidget(ten);
    }

    private void buildExchangeControls() {
        var state = ClientMetaState.snapshot();
        int x = left + 12;
        int y = bodyTop() + 29;
        var crystal = new BattleHudButton(
                x, y, panelWidth - 24, 18,
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
        int gridY = y + 27;
        int cols = 3;
        int cardGap = 3;
        int cardW = (panelWidth - 24 - cardGap * (cols - 1)) / cols;
        int maxRows = Math.max(1, (bodyBottom() - gridY) / 20);
        int limit = Math.min(choices.size(), maxRows * cols);
        for (int i = 0; i < limit; i++) {
            var row = choices.get(i);
            int cost = StarEssenceExchangeRules.choiceCost(row.nativeStar());
            int xx = x + (i % cols) * (cardW + cardGap);
            int yy = gridY + (i / cols) * 20;
            String bonus = row.bonusLevel() >= GrowthRulesV1.duplicateBonusMax() ? "MAX" : "+" + row.bonusLevel();
            var button = new BattleHudButton(
                    xx, yy, cardW, 17,
                    Component.literal("★" + row.nativeStar() + " " + row.name() + " " + bonus + " · " + cost),
                    row.nativeStar() == 5 ? 0xFFFFC857 : 0xFFC794FF,
                    ignored -> send("ESSENCE_PICK" + row.nativeStar() + "|" + row.id()));
            button.active = state.essence() >= cost;
            addRenderableWidget(button);
        }
    }

    @Override
    protected void renderFacility(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (page == Page.SUMMON) renderSummonPage(g);
        else renderExchangePage(g);
    }

    private void renderSummonPage(GuiGraphicsExtractor g) {
        int x = left + 10;
        int y = bodyTop() + 25;
        int w = panelWidth - 20;
        int h = Math.max(82, bodyBottom() - y - 31);
        SummonLayerRenderer.sprite(g, SummonAssets.BG_ARCHIVE, x, y, w, h);

        int artW = Math.min(w * 54 / 100, 238);
        int artH = Math.max(76, h - 8);
        int cx = x + artW / 2 + 6;
        int cy = y + h / 2;
        int halo = Math.min(artW - 8, artH);
        SummonLayerRenderer.centered(g, SummonAssets.RADIAL_GLOW, cx, cy, halo, halo);
        SummonLayerRenderer.centered(g, SummonAssets.RUNE_CIRCLE, cx, cy, halo - 4, halo - 4);
        SummonLayerRenderer.centered(g, SummonAssets.ORBIT_B, cx, cy, halo, halo);
        int crystalH = Math.max(58, halo * 68 / 100);
        SummonLayerRenderer.centered(g, SummonAssets.CRYSTAL, cx, cy, crystalH * 7 / 10, crystalH);
        SummonLayerRenderer.centered(g, SummonAssets.ORBIT_A, cx, cy, halo + 5, halo + 5);

        int tx = x + artW + 16;
        int textW = Math.max(80, x + w - tx - 10);
        g.text(font, Component.literal("STANDARD ARCHIVE"), tx, y + 11, TurnboundUiTokens.TEXT_PRIMARY, true);
        g.text(font, Component.literal("정령의 기록"), tx, y + 29, 0xFF6DC6FF, true);
        g.text(font, Component.literal("★5  3%"), tx, y + 50, 0xFFFFC857, true);
        g.text(font, Component.literal("★4  12%"), tx, y + 64, 0xFFC794FF, false);
        if (h >= 102) {
            g.text(font, Component.literal(UiTextLayout.fit("★3 35% · ★2 30% · ★1 20%", textW)),
                    tx, y + 79, TurnboundUiTokens.TEXT_SECONDARY, false);
        }
        var state = ClientMetaState.snapshot();
        String pity = "다음 ★5까지 최대 " + Math.max(0, GachaCatalog.HARD_PITY - state.fiveStarPity()) + "회";
        if (h >= 118) {
            g.text(font, Component.literal(UiTextLayout.fit(pity, textW)),
                    tx, y + 95, TurnboundUiTokens.TEXT_SECONDARY, false);
        }
    }

    private void renderExchangePage(GuiGraphicsExtractor g) {
        inset(g, left + 8, bodyTop() + 25, panelWidth - 16, panelHeight - 66);
        g.text(font, Component.literal("보유한 ★4/★5 동료의 기록을 강화하거나 크리스탈로 교환합니다."),
                left + 14, bodyTop() + 51, TurnboundUiTokens.TEXT_SECONDARY, false);
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
