package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/** Physical Market Row shop. This screen is intentionally not reachable from the global E management menu. */
final class FacilityMarketScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int MUTED = 0xFF87909E;
    private static final int GOLD = 0xFFFFC857;
    private static final int GREEN = 0xFF62D39A;

    private int left, top, panelWidth, panelHeight, page;

    FacilityMarketScreen() {
        super(Component.literal("Market Row"));
    }

    void refreshSnapshot() {
        clearWidgets();
        init();
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(720, Math.max(330, width - 36));
        panelHeight = Math.min(520, Math.max(280, height - 36));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;

        var rows = ClientMetaState.snapshot().shopItems();
        int perPage = Math.max(4, Math.min(10, (panelHeight - 150) / 34));
        int pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        int start = page * perPage;
        int end = Math.min(rows.size(), start + perPage);
        int y = top + 82;
        int buttonW = panelWidth - 36;
        for (int i = start; i < end; i++) {
            var row = rows.get(i);
            String text = row.tier() + " · " + row.name() + " · " + slotLabel(row.slot()) + " · " + row.price() + "G";
            var button = new BattleHudButton(left + 18, y, buttonW, 27, Component.literal(text), row.unlocked() ? GOLD : MUTED,
                    ignored -> buy(row.itemId()));
            button.active = row.unlocked() && ClientMetaState.snapshot().gold() >= row.price();
            addRenderableWidget(button);
            y += 33;
        }

        int pagerY = top + panelHeight - 40;
        if (page > 0) addRenderableWidget(new BattleHudButton(left + 18, pagerY, 82, 22, Component.literal("< 이전"), MUTED, ignored -> movePage(-1)));
        if (page + 1 < pages) addRenderableWidget(new BattleHudButton(left + 106, pagerY, 82, 22, Component.literal("다음 >"), MUTED, ignored -> movePage(1)));
    }

    private void buy(String itemId) {
        ClientPacketDistributor.sendToServer(new MetaCommandPayload("BUY|" + itemId));
    }

    private void movePage(int delta) {
        page = Math.max(0, page + delta);
        clearWidgets();
        init();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_E || event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        FacilityUiAccess.clear();
        super.onClose();
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TurnboundFrameStyle.frame(graphics, left, top, panelWidth, panelHeight, GOLD);
        graphics.text(font, Component.literal("Market Row · 장비 상점"), left + 18, top + 18, TEXT, true);
        graphics.text(font, Component.literal("보유 골드 " + ClientMetaState.snapshot().gold() + " · 장비는 목록에서 직접 구매"),
                left + 18, top + 42, GOLD, false);
        graphics.text(font, Component.literal("상점 기능은 라디아 Market Row에서만 이용할 수 있습니다."),
                left + 18, top + 59, GREEN, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static String slotLabel(String slot) {
        return switch (slot) {
            case "WEAPON" -> "무기";
            case "ARMOR" -> "방어구";
            case "ACCESSORY" -> "장신구";
            case "SIGNATURE" -> "전용 장비";
            default -> slot;
        };
    }
}
