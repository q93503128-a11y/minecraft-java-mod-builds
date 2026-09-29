package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/** Physical equipment merchant. Buying and selling are not exposed by the global management menu. */
final class FacilityMarketScreen extends FacilityScreen {
    FacilityMarketScreen() {
        super("장비 상점", 0xFFFFC857, 480, 246, 360, 218);
    }

    @Override
    protected String subtitle() {
        return "보유 골드 " + ClientMetaState.snapshot().gold();
    }

    @Override
    protected void buildFacility() {
        int gap = 8;
        int colW = (panelWidth - 28 - gap) / 2;
        int leftX = left + 10;
        int rightX = leftX + colW + gap;
        int y = bodyTop() + 20;
        int rowH = 18;
        int step = 20;
        int maxRows = Math.max(4, (bodyBottom() - y - 4) / step);

        int i = 0;
        for (var row : ClientMetaState.snapshot().shopItems()) {
            if (i >= maxRows) break;
            String label = row.tier() + " · " + row.name() + " · " + row.price() + "G";
            var button = new BattleHudButton(
                    leftX + 4, y + i * step, colW - 8, rowH, Component.literal(label),
                    row.unlocked() ? 0xFFFFC857 : 0xFF707987,
                    ignored -> send("BUY|" + row.itemId()));
            button.active = row.unlocked() && ClientMetaState.snapshot().gold() >= row.price();
            addRenderableWidget(button);
            i++;
        }

        i = 0;
        var sell = ClientMetaState.snapshot().equipment().stream()
                .filter(ClientMetaState.EquipmentRow::sellable)
                .sorted(Comparator.comparingInt(ClientMetaState.EquipmentRow::salePrice).reversed())
                .toList();
        for (var row : sell) {
            if (i >= maxRows) break;
            addRenderableWidget(new BattleHudButton(
                    rightX + 4, y + i * step, colW - 8, rowH,
                    Component.literal(row.name() + " · " + row.salePrice() + "G"),
                    0xFF62D39A,
                    ignored -> send("SELL|" + row.instanceId())));
            i++;
        }
    }

    @Override
    protected void renderFacility(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int gap = 8;
        int colW = (panelWidth - 28 - gap) / 2;
        int leftX = left + 10;
        int rightX = leftX + colW + gap;
        int h = panelHeight - 62;
        inset(g, leftX, bodyTop() + 1, colW, h);
        inset(g, rightX, bodyTop() + 1, colW, h);
        g.text(font, Component.literal("구매"), leftX + 7, bodyTop() + 7, 0xFFAEB7C6, true);
        g.text(font, Component.literal("판매"), rightX + 7, bodyTop() + 7, 0xFFAEB7C6, true);
    }

    private void send(String raw) {
        ClientPacketDistributor.sendToServer(new MetaCommandPayload(raw));
    }
}
