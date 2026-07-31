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
    private static final int PANEL_WIDTH = 116;
    private static final int PANEL_HEIGHT = 136;
    private static final int SHADOW = 0x77000000;
    private static final int BACKGROUND = 0xF50D1117;
    private static final int SURFACE = 0xFF171D25;
    private static final int SURFACE_HOVER = 0xFF222C37;
    private static final int BORDER = 0xFF35414E;
    private static final int ACCENT = 0xFF3ED0B4;
    private static final int ACCENT_DARK = 0xFF1E776B;
    private static final int GOLD = 0xFFF1BC57;
    private static final int TEXT = 0xFFF3F6F8;
    private static final int MUTED = 0xFF94A0AD;
    private static final int BLUE = 0xFF86A8E8;
    private static final Pattern LEVEL_PATTERN = Pattern.compile(".*?(\\d+).*");
    private static final Pattern PROGRESS_PATTERN = Pattern.compile(".*?(\\d+).*?(\\d+)\\s*/\\s*(\\d+).*?");

    private static VillageNetwork.PlayerStatusPayload status = new VillageNetwork.PlayerStatusPayload(
            "레벨 동기화 중",
            "확인 중",
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
        int buttonLeft = left + 8;
        int buttonTop = top + PANEL_HEIGHT - 27;
        boolean hovered = inside(
                event.getMouseX(), event.getMouseY(),
                buttonLeft, buttonTop, PANEL_WIDTH - 16, 19);

        graphics.fill(left + 3, top + 4, left + PANEL_WIDTH + 3, top + PANEL_HEIGHT + 4, SHADOW);
        graphics.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, BORDER);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, BACKGROUND);
        graphics.fill(left, top, left + 3, top + PANEL_HEIGHT, ACCENT);

        Minecraft minecraft = Minecraft.getInstance();
        ProgressData progress = parseProgress(status.progress());

        graphics.text(minecraft.font, "VG", left + 10, top + 8, GOLD, false);
        graphics.text(minecraft.font, "수호단", left + 28, top + 8, TEXT, false);
        String levelText = "LV." + progress.level();
        int badgeWidth = minecraft.font.width(levelText) + 8;
        int badgeLeft = left + PANEL_WIDTH - badgeWidth - 7;
        graphics.fill(badgeLeft, top + 5, left + PANEL_WIDTH - 6, top + 20, SURFACE);
        graphics.centeredText(minecraft.font, levelText,
                badgeLeft + badgeWidth / 2, top + 9, GOLD);

        int barLeft = left + 9;
        int barTop = top + 27;
        int barWidth = PANEL_WIDTH - 18;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 5, 0xFF05080C);
        graphics.fill(barLeft + 1, barTop + 1,
                barLeft + 1 + Math.round((barWidth - 2) * progress.ratio()),
                barTop + 4, ACCENT);
        String xpText = progress.maxLevel()
                ? "MAX LEVEL"
                : progress.current() + "/" + progress.required() + " XP";
        graphics.text(minecraft.font, xpText, barLeft, barTop + 8,
                progress.maxLevel() ? GOLD : MUTED, false);

        drawInfoRow(graphics, minecraft, left, top + 49, "역할", status.role(), ACCENT);
        drawInfoRow(graphics, minecraft, left, top + 64, "자산", status.economy(), GOLD);
        drawInfoRow(graphics, minecraft, left, top + 79, "마을", status.village(), BLUE);

        graphics.fill(buttonLeft - 1, buttonTop - 1,
                buttonLeft + PANEL_WIDTH - 15, buttonTop + 20, hovered ? ACCENT : ACCENT_DARK);
        graphics.fill(buttonLeft, buttonTop,
                buttonLeft + PANEL_WIDTH - 16, buttonTop + 19,
                hovered ? SURFACE_HOVER : SURFACE);
        graphics.fill(buttonLeft, buttonTop, buttonLeft + 3, buttonTop + 19,
                hovered ? GOLD : ACCENT);
        graphics.text(minecraft.font, "상태 / 역할", buttonLeft + 10, buttonTop + 6, TEXT, false);
        graphics.text(minecraft.font, ">", buttonLeft + PANEL_WIDTH - 27, buttonTop + 6,
                hovered ? GOLD : MUTED, false);
    }

    private static void drawInfoRow(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            int left,
            int y,
            String label,
            String value,
            int markerColor) {
        graphics.fill(left + 9, y + 2, left + 11, y + 8, markerColor);
        graphics.text(minecraft.font, label, left + 16, y, MUTED, false);
        graphics.text(minecraft.font, compact(value, 14), left + 43, y, TEXT, false);
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
                left + 8, top + PANEL_HEIGHT - 27, PANEL_WIDTH - 16, 19)) {
            return;
        }
        ClientPacketDistributor.sendToServer(
                new VillageNetwork.VillageUiActionPayload("open_status"));
        event.setCanceled(true);
    }

    private static ProgressData parseProgress(String text) {
        Matcher progressMatcher = PROGRESS_PATTERN.matcher(text);
        if (progressMatcher.matches()) {
            try {
                int level = Integer.parseInt(progressMatcher.group(1));
                int current = Integer.parseInt(progressMatcher.group(2));
                int required = Math.max(1, Integer.parseInt(progressMatcher.group(3)));
                return new ProgressData(level, current, required, false);
            } catch (NumberFormatException ignored) {
            }
        }

        Matcher levelMatcher = LEVEL_PATTERN.matcher(text);
        if (levelMatcher.matches()) {
            try {
                int level = Integer.parseInt(levelMatcher.group(1));
                boolean maxLevel = text.contains("최고 레벨");
                return new ProgressData(level, maxLevel ? 1 : 0, 1, maxLevel);
            } catch (NumberFormatException ignored) {
            }
        }
        return new ProgressData(1, 0, 100, false);
    }

    private static String compact(String value, int maxCharacters) {
        String normalized = value
                .replace("내 수호 주화 ", "주화 ")
                .replace("제 ", "")
                .replace("일 ", "일·");
        if (normalized.length() <= maxCharacters) {
            return normalized;
        }
        return normalized.substring(0, Math.max(1, maxCharacters - 1)) + "…";
    }

    private static int[] panelPosition(int screenWidth, int screenHeight) {
        int inventoryRight = screenWidth / 2 + 90;
        int inventoryLeft = screenWidth / 2 - 90;
        int preferredRight = inventoryRight + 7;
        int left = preferredRight + PANEL_WIDTH <= screenWidth - 5
                ? preferredRight
                : Math.max(5, inventoryLeft - PANEL_WIDTH - 7);
        int top = Math.max(5, screenHeight / 2 - 83);
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

    private record ProgressData(int level, int current, int required, boolean maxLevel) {
        float ratio() {
            if (maxLevel) {
                return 1.0f;
            }
            return Math.max(0.0f, Math.min(1.0f, current / (float) required));
        }
    }
}
