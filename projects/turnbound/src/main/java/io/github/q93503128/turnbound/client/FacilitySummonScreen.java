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

/** Physical summon shrine. Summon/economy actions are intentionally absent from the global management menu. */
final class FacilitySummonScreen extends FacilityScreen {
    FacilitySummonScreen() {
        super("정령의 흔적", 0xFFC794FF, 460, 242, 350, 218);
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
        var state = ClientMetaState.snapshot();
        int x = left + 12;
        int y = bodyTop() + 5;
        int gap = 3;
        int w = (panelWidth - 24 - gap * 2) / 3;

        var one = new BattleHudButton(x, y, w, 18, Component.literal("1회 · 300"), 0xFF6DC6FF,
                ignored -> send("SUMMON1"));
        one.active = state.crystal() >= GachaCatalog.SINGLE_COST;
        addRenderableWidget(one);

        var ten = new BattleHudButton(x + w + gap, y, w, 18, Component.literal("10회 · 3000"), 0xFFFFC857,
                ignored -> send("SUMMON10"));
        ten.active = state.crystal() >= GachaCatalog.TEN_COST;
        addRenderableWidget(ten);

        var starter = new BattleHudButton(x + (w + gap) * 2, y, w, 18, Component.literal("초기 10회"), 0xFF62D39A,
                ignored -> send("STARTER"));
        starter.active = state.starterArchiveAvailable() && state.crystal() >= GachaCatalog.TEN_COST;
        addRenderableWidget(starter);

        int exchangeY = y + 23;
        var crystal = new BattleHudButton(
                x, exchangeY, panelWidth - 24, 18,
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
        int gridY = exchangeY + 23;
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
        inset(g, left + 8, bodyTop() + 1, panelWidth - 16, panelHeight - 62);
    }

    private void send(String raw) {
        ClientPacketDistributor.sendToServer(new MetaCommandPayload(raw));
    }
}
