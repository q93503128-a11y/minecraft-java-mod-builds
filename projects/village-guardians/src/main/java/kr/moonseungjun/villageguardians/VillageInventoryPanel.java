package kr.moonseungjun.villageguardians;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageInventoryPanel {
    private static final int PANEL_WIDTH = 138;
    private static final int PANEL_HEIGHT = 108;
    private static final int GOLD = 0xFFC89C54;
    private static final int PANEL = 0xF0221714;
    private static final int HEADER = 0xFF44231F;
    private static final int TEXT = 0xFFF1E4CF;
    private static final int MUTED = 0xFFBFAE98;
    private static final int ACTION = 0xFF176B68;
    private static final int ACTION_HOVER = 0xFF258985;

    private static VillageNetwork.PlayerStatusPayload status = new VillageNetwork.PlayerStatusPayload(
            "상태 동기화 중...",
            "역할 확인 중...",
            "재화 확인 중...",
            "마을 확인 중...");

    private VillageInventoryPanel() {
    }

    public static void updateStatus(VillageNetwork.PlayerStatusPayload payload) {
        status = payload;
    }

    @SubscribeEvent
    public static void onInventoryInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen) {
            ClientPacketDistributor.sendToServer(
                    new VillageNetwork.RequestPlayerStatusPayload("inventory"));
        }
    }

    @SubscribeEvent
    public static void onInventoryRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int[] pos = panelPosition(graphics.guiWidth(), graphics.guiHeight());
        int left = pos[0];
        int top = pos[1];
        boolean hovered = inside(
                event.getMouseX(),
                event.getMouseY(),
                left + 8,
                top + PANEL_HEIGHT - 29,
                PANEL_WIDTH - 16,
                21);

        graphics.fill(left - 2, top - 2, left + PANEL_WIDTH + 2, top + PANEL_HEIGHT + 2, GOLD);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, PANEL);
        graphics.fill(left + 5, top + 5, left + PANEL_WIDTH - 5, top + 25, HEADER);

        Minecraft minecraft = Minecraft.getInstance();
        graphics.centeredText(minecraft.font, "마을 수호단", left + PANEL_WIDTH / 2, top + 11, 0xFFFFD98A);
        graphics.text(minecraft.font, status.progress(), left + 9, top + 33, TEXT, false);
        graphics.text(minecraft.font, status.role(), left + 9, top + 46, TEXT, false);
        graphics.text(minecraft.font, status.economy(), left + 9, top + 59, MUTED, false);
        graphics.text(minecraft.font, status.village(), left + 9, top + 72, MUTED, false);

        int buttonLeft = left + 8;
        int buttonTop = top + PANEL_HEIGHT - 29;
        int buttonRight = left + PANEL_WIDTH - 8;
        graphics.fill(buttonLeft - 1, buttonTop - 1, buttonRight + 1, buttonTop + 22, GOLD);
        graphics.fill(
                buttonLeft,
                buttonTop,
                buttonRight,
                buttonTop + 21,
                hovered ? ACTION_HOVER : ACTION);
        graphics.centeredText(
                minecraft.font,
                "상태·역할 관리",
                left + PANEL_WIDTH / 2,
                buttonTop + 7,
                0xFFFFFFFF);
    }

    @SubscribeEvent
    public static void onInventoryClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen) || event.getButton() != 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int[] pos = panelPosition(
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight());
        int left = pos[0];
        int top = pos[1];
        if (!inside(
                event.getMouseX(),
                event.getMouseY(),
                left + 8,
                top + PANEL_HEIGHT - 29,
                PANEL_WIDTH - 16,
                21)) {
            return;
        }
        ClientPacketDistributor.sendToServer(
                new VillageNetwork.VillageUiActionPayload("open_status"));
        event.setCanceled(true);
    }

    private static int[] panelPosition(int screenWidth, int screenHeight) {
        int inventoryRight = screenWidth / 2 + 90;
        int left = inventoryRight + 9;
        int top = Math.max(8, screenHeight / 2 - 82);
        if (left + PANEL_WIDTH > screenWidth - 8) {
            left = Math.max(8, screenWidth / 2 - 90 - PANEL_WIDTH - 9);
        }
        return new int[]{left, top};
    }

    private static boolean inside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }
}
