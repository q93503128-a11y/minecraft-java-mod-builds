package kr.moonseungjun.villageguardians;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageInventoryPanel {
    private static final int PANEL_WIDTH = 164;
    private static final int PANEL_HEIGHT = 124;
    private static final int SHADOW = 0x88000000;
    private static final int BACKGROUND = 0xF2161A21;
    private static final int SURFACE = 0xFF20262F;
    private static final int SURFACE_HOVER = 0xFF2A333E;
    private static final int BORDER = 0xFF3B4653;
    private static final int ACCENT = 0xFF43C6AC;
    private static final int ACCENT_DARK = 0xFF237A70;
    private static final int GOLD = 0xFFE6B65A;
    private static final int TEXT = 0xFFF3F6F8;
    private static final int MUTED = 0xFF9BA7B4;
    private static final Pattern PROGRESS_PATTERN = Pattern.compile(".*?(\\d+).*?(\\d+)\\s*/\\s*(\\d+).*?");

    private static VillageNetwork.PlayerStatusPayload status = new VillageNetwork.PlayerStatusPayload(
            "레벨 동기화 중",
            "역할 확인 중",
            "주화 확인 중",
            "마을 확인 중");

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
        int buttonLeft = left + 10;
        int buttonTop = top + PANEL_HEIGHT - 31;
        boolean hovered = inside(
                event.getMouseX(), event.getMouseY(),
                buttonLeft, buttonTop, PANEL_WIDTH - 20, 22);

        graphics.fill(left + 4, top + 5, left + PANEL_WIDTH + 4, top + PANEL_HEIGHT + 5, SHADOW);
        graphics.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, BORDER);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, BACKGROUND);
        graphics.fill(left, top, left + 4, top + PANEL_HEIGHT, ACCENT);
        graphics.fill(left + 4, top, left + PANEL_WIDTH, top + 2, 0xFF647181);

        Minecraft minecraft = Minecraft.getInstance();
        ProgressData progress = parseProgress(status.progress());

        graphics.text(minecraft.font, "VILLAGE GUARDIANS", left + 12, top + 10, MUTED, false);
        graphics.text(minecraft.font, "마을 수호단", left + 12, top + 22, TEXT, false);
        String levelText = "LV " + progress.level();
        int badgeWidth = minecraft.font.width(levelText) + 10;
        graphics.fill(left + PANEL_WIDTH - badgeWidth - 9, top + 9,
                left + PANEL_WIDTH - 9, top + 25, SURFACE);
        graphics.centeredText(minecraft.font, levelText,
                left + PANEL_WIDTH - badgeWidth / 2 - 9, top + 13, GOLD);

        int barLeft = left + 12;
        int barTop = top + 39;
        int barWidth = PANEL_WIDTH - 24;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 6, 0xFF0C0F13);
        graphics.fill(barLeft + 1, barTop + 1,
                barLeft + 1 + Math.round((barWidth - 2) * progress.ratio()),
                barTop + 5, ACCENT);
        String xpText = progress.current() + " / " + progress.required() + " XP";
        graphics.text(minecraft.font, xpText, barLeft, barTop + 9, MUTED, false);

        drawInfoRow(graphics, minecraft, left, top + 62, "역할", status.role(), ACCENT);
        drawInfoRow(graphics, minecraft, left, top + 75, "자산", status.economy(), GOLD);
        drawInfoRow(graphics, minecraft, left, top + 88, "마을", status.village(), 0xFF8FA7FF);

        graphics.fill(buttonLeft - 1, buttonTop - 1,
                buttonLeft + PANEL_WIDTH - 19, buttonTop + 23, ACCENT_DARK);
        graphics.fill(buttonLeft, buttonTop,
                buttonLeft + PANEL_WIDTH - 20, buttonTop + 22,
                hovered ? SURFACE_HOVER : SURFACE);
        graphics.fill(buttonLeft, buttonTop,
                buttonLeft + 3, buttonTop + 22, hovered ? GOLD : ACCENT);
        graphics.centeredText(
                minecraft.font,
                "상태 및 역할 관리  ›",
                left + PANEL_WIDTH / 2 + 2,
                buttonTop + 7,
                TEXT);
    }

    private static void drawInfoRow(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            int left,
            int y,
            String label,
            String value,
            int markerColor) {
        graphics.fill(left + 12, y + 2, left + 15, y + 8, markerColor);
        graphics.text(minecraft.font, label, left + 20, y, MUTED, false);
        int valueX = left + 50;
        graphics.text(minecraft.font, compact(value, 21), valueX, y, TEXT, false);
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
                event.getMouseX(), event.getMouseY(),
                left + 10, top + PANEL_HEIGHT - 31, PANEL_WIDTH - 20, 22)) {
            return;
        }
        ClientPacketDistributor.sendToServer(
                new VillageNetwork.VillageUiActionPayload("open_status"));
        event.setCanceled(true);
    }

    private static ProgressData parseProgress(String text) {
        Matcher matcher = PROGRESS_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return new ProgressData(1, 0, 100);
        }
        try {
            int level = Integer.parseInt(matcher.group(1));
            int current = Integer.parseInt(matcher.group(2));
            int required = Math.max(1, Integer.parseInt(matcher.group(3)));
            return new ProgressData(level, current, required);
        } catch (NumberFormatException ignored) {
            return new ProgressData(1, 0, 100);
        }
    }

    private static String compact(String value, int maxCharacters) {
        if (value.length() <= maxCharacters) {
            return value;
        }
        return value.substring(0, Math.max(1, maxCharacters - 1)) + "…";
    }

    private static int[] panelPosition(int screenWidth, int screenHeight) {
        int inventoryRight = screenWidth / 2 + 90;
        int left = inventoryRight + 10;
        int top = Math.max(8, screenHeight / 2 - 83);
        if (left + PANEL_WIDTH > screenWidth - 8) {
            left = Math.max(8, screenWidth / 2 - 90 - PANEL_WIDTH - 10);
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

    private record ProgressData(int level, int current, int required) {
        float ratio() {
            return Math.max(0.0f, Math.min(1.0f, current / (float) required));
        }
    }
}
