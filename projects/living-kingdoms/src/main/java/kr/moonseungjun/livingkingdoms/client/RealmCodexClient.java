package kr.moonseungjun.livingkingdoms.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.livingkingdoms.network.RequestCodexPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Kingdom notebook shortcuts and the inventory-side notebook panel. */
public final class RealmCodexClient {
    private static final KeyMapping MAP_KEY = new KeyMapping(
            "key.livingkingdoms.realm_map", InputConstants.KEY_M, KeyMapping.Category.MISC
    );
    private static final KeyMapping STATUS_KEY = new KeyMapping(
            "key.livingkingdoms.character_status", InputConstants.KEY_K, KeyMapping.Category.MISC
    );

    private RealmCodexClient() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(MAP_KEY);
        event.register(STATUS_KEY);
    }

    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        while (MAP_KEY.consumeClick()) request("map");
        while (STATUS_KEY.consumeClick()) request("overview");
    }

    /**
     * The notebook is drawn after the vanilla inventory. Handle its click before the inventory so
     * slot/recipe-book routing can never consume a visible notebook button first.
     */
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen) || event.getButton() != 0) return;
        String page = notebookActionAt(screen.width, screen.height, event.getMouseX(), event.getMouseY());
        if (page == null) return;
        request(page);
        event.setCanceled(true);
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        GuiGraphicsExtractor g = event.getGuiGraphics();
        Panel p = panel(g.guiWidth(), g.guiHeight());
        int mx = event.getMouseX();
        int my = event.getMouseY();

        g.fill(p.x() + 3, p.y() + 4, p.x() + p.w() + 3, p.y() + p.h() + 4, 0x88000000);
        g.fill(p.x(), p.y(), p.x() + p.w(), p.y() + p.h(), 0xFF322219);
        g.fill(p.x() + 2, p.y() + 2, p.x() + p.w() - 2, p.y() + p.h() - 2, 0xFFC59A55);
        g.fill(p.x() + 4, p.y() + 4, p.x() + p.w() - 4, p.y() + p.h() - 4, 0xFFEEE0C3);
        g.fill(p.x() + 4, p.y() + 4, p.x() + p.w() - 4, p.y() + 20, 0xFF483126);
        g.text(Minecraft.getInstance().font, Component.literal("왕국 수첩"), p.x() + 10, p.y() + 8, 0xFFFFE9B5);
        g.text(Minecraft.getInstance().font, Component.literal("K"), p.x() + p.w() - 17, p.y() + 8, 0xFFD6B66B);

        customButton(g, p.x() + 7, p.y() + 24, 96, 19, "인물·소속", inside(mx, my, p.x() + 7, p.y() + 24, 96, 19));
        customButton(g, p.x() + 7, p.y() + 46, 46, 19, "지도 M", inside(mx, my, p.x() + 7, p.y() + 46, 46, 19));
        customButton(g, p.x() + 57, p.y() + 46, 46, 19, "기술", inside(mx, my, p.x() + 57, p.y() + 46, 46, 19));
    }

    static String notebookActionAt(int screenWidth, int screenHeight, double mx, double my) {
        Panel p = panel(screenWidth, screenHeight);
        if (inside(mx, my, p.x() + 7, p.y() + 24, 96, 19)) return "overview";
        if (inside(mx, my, p.x() + 7, p.y() + 46, 46, 19)) return "map";
        if (inside(mx, my, p.x() + 57, p.y() + 46, 46, 19)) return "skills";
        return null;
    }

    /** Pure geometry regression used by the graphical CI; no packet or player is required. */
    static boolean diagnosticNotebookRouting(int screenWidth, int screenHeight) {
        Panel p = panel(screenWidth, screenHeight);
        boolean overview = "overview".equals(notebookActionAt(
                screenWidth, screenHeight, p.x() + 55, p.y() + 33));
        boolean map = "map".equals(notebookActionAt(
                screenWidth, screenHeight, p.x() + 30, p.y() + 55));
        boolean skills = "skills".equals(notebookActionAt(
                screenWidth, screenHeight, p.x() + 80, p.y() + 55));
        boolean gap = notebookActionAt(screenWidth, screenHeight, p.x() + 55, p.y() + 55) == null;
        return overview && map && skills && gap;
    }

    private static void customButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String text, boolean hover) {
        g.fill(x, y, x + w, y + h, hover ? 0xFF315848 : 0xFF624128);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, hover ? 0xFF6B8A69 : 0xFF8A6037);
        g.centeredText(Minecraft.getInstance().font, Component.literal(text), x + w / 2, y + 6, 0xFFFFE9B6);
    }

    private static Panel panel(int screenWidth, int screenHeight) {
        int inventoryLeft = screenWidth / 2 - 90;
        int x = inventoryLeft - 120;
        int y = Math.max(6, screenHeight / 2 - 82);
        if (x < 6) {
            x = 6;
            y = 6;
        }
        return new Panel(x, y, 110, 70);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private static void request(String page) {
        if (Minecraft.getInstance().player != null) {
            ClientPacketDistributor.sendToServer(new RequestCodexPayload(page));
        }
    }

    private record Panel(int x, int y, int w, int h) {
    }
}
