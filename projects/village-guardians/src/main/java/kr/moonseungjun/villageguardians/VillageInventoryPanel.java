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
    private static final int PANEL_WIDTH = 126;
    private static final int PANEL_HEIGHT = 137;
    private static final int BACKGROUND = 0xF70B1118;
    private static final int SURFACE = 0xFF15202A;
    private static final int SURFACE_HOVER = 0xFF20303D;
    private static final int BORDER = 0xFF405466;
    private static final int ACCENT = 0xFF43D6BC;
    private static final int GOLD = 0xFFF1C35D;
    private static final int BLUE = 0xFF7EA9EA;
    private static final int TEXT = 0xFFF3F7FA;
    private static final int MUTED = 0xFFA3B0BC;
    private static final Pattern LEVEL_PATTERN = Pattern.compile(".*?(\\d+).*");
    private static final Pattern PROGRESS_PATTERN =
            Pattern.compile(".*?(\\d+).*?(\\d+)\\s*/\\s*(\\d+).*?");

    private static VillageNetwork.PlayerStatusPayload status =
            new VillageNetwork.PlayerStatusPayload(
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
        int[] position = panelPosition(graphics.guiWidth(), graphics.guiHeight());
        int left = position[0];
        int top = position[1];
        Minecraft minecraft = Minecraft.getInstance();
        ProgressData progress = parseProgress(status.progress());

        graphics.fill(left - 1, top - 1,
                left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, BORDER);
        graphics.fill(left, top,
                left + PANEL_WIDTH, top + PANEL_HEIGHT, BACKGROUND);
        graphics.fill(left, top, left + 4, top + PANEL_HEIGHT, ACCENT);

        graphics.text(minecraft.font, "수호자", left + 11, top + 9, TEXT, false);
        String level = "LV." + progress.level();
        graphics.text(minecraft.font, level,
                left + PANEL_WIDTH - 9 - minecraft.font.width(level),
                top + 9, GOLD, false);

        int barLeft = left + 11;
        int barTop = top + 27;
        int barWidth = PANEL_WIDTH - 22;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 5, 0xFF05080C);
        graphics.fill(barLeft, barTop,
                barLeft + Math.round(barWidth * progress.ratio()),
                barTop + 5, ACCENT);
        String xp = progress.maxLevel()
                ? "최고 레벨"
                : progress.current() + " / " + progress.required() + " XP";
        graphics.text(minecraft.font, xp, barLeft, barTop + 9, MUTED, false);

        graphics.text(minecraft.font, "역할", barLeft, top + 54, MUTED, false);
        graphics.text(minecraft.font, compact(status.role(), 13),
                left + 40, top + 54, TEXT, false);
        graphics.text(minecraft.font, compact(status.economy(), 19),
                barLeft, top + 68, GOLD, false);

        drawAction(graphics, minecraft, event.getMouseX(), event.getMouseY(),
                left + 8, top + 84, PANEL_WIDTH - 16, 20,
                "전술 발전", ACCENT, true);
        drawAction(graphics, minecraft, event.getMouseX(), event.getMouseY(),
                left + 8, top + 108, PANEL_WIDTH - 16, 20,
                "마을 귀환", BLUE, false);
    }

    private static void drawAction(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height,
            String label,
            int color,
            boolean tree) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1,
                hovered ? color : BORDER);
        graphics.fill(x, y, x + width, y + height,
                hovered ? SURFACE_HOVER : SURFACE);

        int iconX = x + 7;
        int iconY = y + 5;
        if (tree) {
            graphics.fill(iconX + 4, iconY, iconX + 6, iconY + 10, color);
            graphics.fill(iconX, iconY + 3, iconX + 10, iconY + 5, color);
            graphics.fill(iconX, iconY, iconX + 3, iconY + 3, color);
            graphics.fill(iconX + 7, iconY + 7, iconX + 10, iconY + 10, color);
        } else {
            graphics.fill(iconX + 4, iconY, iconX + 6, iconY + 10, color);
            graphics.fill(iconX, iconY + 1, iconX + 6, iconY + 3, color);
            graphics.fill(iconX, iconY + 1, iconX + 2, iconY + 7, color);
        }
        graphics.text(minecraft.font, label, x + 24, y + 6, TEXT, false);
        graphics.text(minecraft.font, "›", x + width - 14, y + 6,
                hovered ? GOLD : MUTED, false);
    }

    @SubscribeEvent
    public static void onInventoryClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen) || event.getButton() != 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int[] position = panelPosition(
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight());
        int left = position[0];
        int top = position[1];

        if (inside(event.getMouseX(), event.getMouseY(),
                left + 8, top + 84, PANEL_WIDTH - 16, 20)) {
            ClientPacketDistributor.sendToServer(
                    new VillageNetwork.VillageUiActionPayload("open_skill_tree"));
            event.setCanceled(true);
            return;
        }
        if (inside(event.getMouseX(), event.getMouseY(),
                left + 8, top + 108, PANEL_WIDTH - 16, 20)) {
            ClientPacketDistributor.sendToServer(
                    new VillageNetwork.VillageUiActionPayload("return_village"));
            event.setCanceled(true);
        }
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
                .replace(" · 장비 ", " · 장비");
        if (normalized.length() <= maxCharacters) {
            return normalized;
        }
        return normalized.substring(0, Math.max(1, maxCharacters - 1)) + "…";
    }

    private static int[] panelPosition(int screenWidth, int screenHeight) {
        int inventoryRight = screenWidth / 2 + 90;
        int inventoryLeft = screenWidth / 2 - 90;
        int rightCandidate = inventoryRight + 8;
        int left = rightCandidate + PANEL_WIDTH <= screenWidth - 7
                ? rightCandidate
                : Math.max(7, inventoryLeft - PANEL_WIDTH - 8);
        int top = Math.max(7, screenHeight / 2 - 82);
        return new int[]{left, top};
    }

    private static boolean inside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
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
