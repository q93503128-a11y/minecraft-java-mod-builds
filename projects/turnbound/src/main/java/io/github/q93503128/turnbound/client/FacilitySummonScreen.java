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

/** Physical Standard Archive service. Summon and Essence exchange stay separated instead of competing in one grid. */
final class FacilitySummonScreen extends FacilityScreen {
    private enum Page { SUMMON, EXCHANGE }

    private Page page = Page.SUMMON;

    FacilitySummonScreen() {
        super("정령의 기록", 0xFFC794FF, 520, 286, 296, 216);
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
        int tabsY = bodyTop() + 3;
        var summonTab = new BattleHudButton(x, tabsY, 82, 18, Component.literal("소환"), 0xFFC794FF,
                ignored -> switchPage(Page.SUMMON));
        summonTab.setSelected(page == Page.SUMMON);
        addRenderableWidget(summonTab);

        var exchangeTab = new BattleHudButton(x + 86, tabsY, 96, 18, Component.literal("별의 정수"), 0xFFC794FF,
                ignored -> switchPage(Page.EXCHANGE));
        exchangeTab.setSelected(page == Page.EXCHANGE);
        addRenderableWidget(exchangeTab);

        if (page == Page.SUMMON) buildSummonControls();
        else buildExchangeControls();
    }

    private void buildSummonControls() {
        var state = ClientMetaState.snapshot();
        int x = left + 12;
        int gap = 4;
        int y = bodyBottom() - 22;
        int w = (panelWidth - 24 - gap * 2) / 3;

        var one = new BattleHudButton(x, y, w, 18,
                Component.literal("1회 · " + GachaCatalog.SINGLE_COST), 0xFF6DC6FF,
                ignored -> send("SUMMON1"));
        one.active = state.crystal() >= GachaCatalog.SINGLE_COST;
        addRenderableWidget(one);

        var ten = new BattleHudButton(x + w + gap, y, w, 18,
                Component.literal("10회 · " + GachaCatalog.TEN_COST), 0xFFFFC857,
                ignored -> send("SUMMON10"));
        ten.active = state.crystal() >= GachaCatalog.TEN_COST;
        addRenderableWidget(ten);

        var starter = new BattleHudButton(x + (w + gap) * 2, y, w, 18,
                Component.literal("첫 10회 · ★4+"), 0xFF62D39A,
                ignored -> send("STARTER"));
        starter.active = state.starterArchiveAvailable() && state.crystal() >= GachaCatalog.TEN_COST;
        addRenderableWidget(starter);
    }

    private void buildExchangeControls() {
        var state = ClientMetaState.snapshot();
        int x = left + 12;
        int y = bodyTop() + 28;
        var crystal = new BattleHudButton(
                x, y, panelWidth - 24, 18,
                Component.literal("별의 정수 150 → 크리스탈 300"),
                0xFFC794FF,
                ignored -> send("ESSENCE_CRYSTAL"));
        crystal.active = state.essence() >= StarEssenceExchangeRules.CRYSTAL_COST;
        addRenderableWidget(crystal);

        var choices = state.characters().stream()
                .filter(ClientMetaState.CharacterRow::owned)
                .filter(row -> row.nativeStar() == 4 || row.nativeStar() == 5)
                .sorted(Comparator.comparingInt(ClientMetaState.CharacterRow::nativeStar).reversed()
                        .thenComparing(ClientMetaState.CharacterRow::name))
                .toList();

        int gridY = y + 24;
        int cols = panelWidth < 390 ? 2 : 3;
        int cardGap = 4;
        int cardW = (panelWidth - 24 - cardGap * (cols - 1)) / cols;
        int maxRows = Math.max(1, (bodyBottom() - gridY - 3) / 21);
        int limit = Math.min(choices.size(), maxRows * cols);
        for (int i = 0; i < limit; i++) {
            var row = choices.get(i);
            int cost = StarEssenceExchangeRules.choiceCost(row.nativeStar());
            int xx = x + (i % cols) * (cardW + cardGap);
            int yy = gridY + (i / cols) * 21;
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

    @Override
    protected void renderFacility(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (page == Page.SUMMON) renderSummon(g);
        else renderExchange(g);
    }

    private void renderSummon(GuiGraphicsExtractor g) {
        int x = left + 8;
        int y = bodyTop() + 25;
        int w = panelWidth - 16;
        int h = Math.max(86, bodyBottom() - y - 27);
        inset(g, x, y, w, h);

        int split = x + Math.max(132, w * 58 / 100);
        split = Math.min(x + w - 104, split);
        int heroX = x + 10;
        int heroW = Math.max(112, split - heroX - 8);
        int heroTop = y + 12;
        int heroBottom = y + h - 12;

        g.text(font, Component.literal("STANDARD ARCHIVE"), heroX, heroTop, TurnboundUiTokens.TEXT_PRIMARY, true);
        g.text(font, Component.literal("정령의 기록에 응답한 동료"), heroX, heroTop + 15,
                TurnboundUiTokens.TEXT_SECONDARY, false);

        int modelsTop = heroTop + 28;
        int modelsH = Math.max(46, heroBottom - modelsTop);
        int modelGap = 2;
        int modelW = Math.max(32, (heroW - modelGap * 2) / 3);
        String[] featured = {"P02", "P05", "P06"};
        for (int i = 0; i < featured.length; i++) {
            int xx = heroX + i * (modelW + modelGap);
            TurnboundPortraitRenderer.extract(g, featured[i], xx, modelsTop, xx + modelW, modelsTop + modelsH, false);
        }

        g.fill(split, y + 10, split + 2, y + h - 10, 0x88C794FF);
        int infoX = split + 12;
        int infoW = Math.max(76, x + w - infoX - 10);
        g.text(font, Component.literal("상시 소환"), infoX, y + 14, TurnboundUiTokens.TEXT_PRIMARY, true);
        g.text(font, Component.literal("★5  3%"), infoX, y + 34, 0xFFFFC857, true);
        g.text(font, Component.literal("★4  12%"), infoX, y + 48, 0xFFC794FF, false);
        g.text(font, Component.literal("★3  35%"), infoX, y + 62, 0xFF6DC6FF, false);
        if (h >= 118) {
            g.text(font, Component.literal("★2  30% · ★1  20%"), infoX, y + 76,
                    TurnboundUiTokens.TEXT_SECONDARY, false);
            String pity = "★5 확정까지 " + Math.max(0, GachaCatalog.HARD_PITY - ClientMetaState.snapshot().fiveStarPity()) + "회";
            g.text(font, Component.literal(UiTextLayout.fit(pity, infoW)), infoX, y + 96,
                    TurnboundUiTokens.TEXT_SECONDARY, false);
        }
    }

    private void renderExchange(GuiGraphicsExtractor g) {
        int x = left + 8;
        int y = bodyTop() + 24;
        int w = panelWidth - 16;
        int h = Math.max(72, bodyBottom() - y);
        inset(g, x, y, w, h);
        g.text(font, Component.literal("중복 획득으로 모은 별의 정수를 영구 보상으로 교환합니다."),
                x + 10, y + 8, TurnboundUiTokens.TEXT_SECONDARY, false);
        if (ClientMetaState.snapshot().characters().stream()
                .noneMatch(row -> row.owned() && (row.nativeStar() == 4 || row.nativeStar() == 5))) {
            g.text(font, Component.literal("선택 가능한 ★4·★5 동료를 먼저 획득해야 합니다."),
                    x + 10, y + 54, TurnboundUiTokens.TEXT_SECONDARY, false);
        }
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
