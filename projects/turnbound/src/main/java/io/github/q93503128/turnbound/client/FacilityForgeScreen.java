package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.progression.GrowthRulesV1;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

/** Physical New Drabyel blacksmith. Enhancement lives here, not in the global equipment-management menu. */
final class FacilityForgeScreen extends FacilityScreen {
    private int page;
    private String selected = "";

    FacilityForgeScreen() {
        super("대장간", 0xFFFFC857, 420, 232, 330, 210);
    }

    @Override
    protected String subtitle() {
        return "보유 골드 " + ClientMetaState.snapshot().gold() + " · 장비 강화";
    }

    @Override
    protected void buildFacility() {
        List<ClientMetaState.EquipmentRow> rows = rows();
        if (!selected.isBlank() && rows.stream().noneMatch(row -> row.instanceId().equals(selected))) selected = "";
        if (selected.isBlank() && !rows.isEmpty()) selected = rows.getFirst().instanceId();

        int listW = Math.min(205, panelWidth * 50 / 100);
        int rowH = 18;
        int listTop = bodyTop() + 5;
        int per = Math.max(4, (bodyBottom() - listTop - 22) / (rowH + 2));
        int pages = Math.max(1, (rows.size() + per - 1) / per);
        page = Math.max(0, Math.min(page, pages - 1));
        int start = page * per;
        int end = Math.min(rows.size(), start + per);

        for (int i = start; i < end; i++) {
            var row = rows.get(i);
            int yy = listTop + (i - start) * (rowH + 2);
            addRenderableWidget(new BattleHudButton(
                    left + 12, yy, listW - 16, rowH,
                    Component.literal(row.tier() + " · " + row.name() + " +" + row.enhancement()),
                    row.instanceId().equals(selected) ? 0xFF6DC6FF : 0xFFAEB7C6,
                    ignored -> {
                        selected = row.instanceId();
                        refreshSnapshot();
                    }));
        }

        var row = selectedRow(rows);
        if (row != null) {
            int rx = left + listW + 6;
            int rw = panelWidth - listW - 18;
            boolean maxed = row.enhancement() >= GrowthRulesV1.maxEnhancement();
            int cost = maxed ? 0 : GrowthRulesV1.enhancementCost(row.tier(), row.enhancement());
            var enhance = new BattleHudButton(
                    rx, bodyTop() + 66, rw, 19,
                    Component.literal(maxed ? "강화 완료" : "+ " + (row.enhancement() + 1) + " 강화 · " + cost + "G"),
                    maxed ? 0xFF707987 : 0xFFFFC857,
                    ignored -> ClientPacketDistributor.sendToServer(
                            new MetaCommandPayload("ENHANCE|" + row.instanceId())));
            enhance.active = !maxed && ClientMetaState.snapshot().gold() >= cost;
            addRenderableWidget(enhance);
        }

        int pagerY = top + panelHeight - 24;
        if (page > 0) {
            addRenderableWidget(new BattleHudButton(
                    left + 12, pagerY, 42, 15, Component.literal("<"), 0xFF707987,
                    ignored -> { page--; refreshSnapshot(); }));
        }
        if (page + 1 < pages) {
            addRenderableWidget(new BattleHudButton(
                    left + 58, pagerY, 42, 15, Component.literal(">"), 0xFF707987,
                    ignored -> { page++; refreshSnapshot(); }));
        }
    }

    @Override
    protected void renderFacility(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        List<ClientMetaState.EquipmentRow> rows = rows();
        var row = selectedRow(rows);
        int listW = Math.min(205, panelWidth * 50 / 100);
        int rx = left + listW + 6;
        int rw = panelWidth - listW - 18;
        int y = bodyTop() + 5;
        inset(g, left + 8, bodyTop() + 1, listW - 8, panelHeight - 62);
        inset(g, rx - 4, bodyTop() + 1, rw + 4, 91);
        if (row == null) {
            g.text(font, Component.literal("강화할 장비가 없습니다."), rx + 5, y + 8, TurnboundUiTokens.TEXT_SECONDARY, false);
            return;
        }
        g.text(font, Component.literal(UiTextLayout.fit(row.name(), rw - 12)), rx + 5, y + 7, TurnboundUiTokens.TEXT_PRIMARY, true);
        g.text(font, Component.literal(row.tier() + " · +" + row.enhancement()), rx + 5, y + 23, 0xFFAEB7C6, false);
        g.text(font, Component.literal(UiTextLayout.fit(row.mainType() + " " + row.mainValue(), rw - 12)),
                rx + 5, y + 39, 0xFF62D39A, false);
    }

    private static List<ClientMetaState.EquipmentRow> rows() {
        return ClientMetaState.snapshot().equipment().stream()
                .sorted(Comparator.comparingInt(ClientMetaState.EquipmentRow::enhancement).reversed()
                        .thenComparing(ClientMetaState.EquipmentRow::name))
                .toList();
    }

    private ClientMetaState.EquipmentRow selectedRow(List<ClientMetaState.EquipmentRow> rows) {
        return rows.stream().filter(row -> row.instanceId().equals(selected)).findFirst().orElse(null);
    }
}
