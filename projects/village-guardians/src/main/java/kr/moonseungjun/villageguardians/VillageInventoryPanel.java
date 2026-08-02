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
    private static final int DESIRED_WIDTH = 152;
    private static final int MIN_WIDTH = 108;
    private static final int PANEL_HEIGHT = 154;
    private static final int BACKGROUND = 0xFFF0E5CC;
    private static final int SURFACE = 0xFFFFF8E8;
    private static final int SURFACE_HOVER = 0xFFFFE2A8;
    private static final int BORDER = 0xFF75634C;
    private static final int ACCENT = 0xFF2E8E80;
    private static final int GOLD = 0xFFC78B2D;
    private static final int TEXT = 0xFF241D17;
    private static final int MUTED = 0xFF6D6256;
    private static final Pattern PROGRESS_PATTERN = Pattern.compile(".*?(\\d+).*?(\\d+)\\s*/\\s*(\\d+).*");

    private static VillageNetwork.PlayerStatusPayload status = new VillageNetwork.PlayerStatusPayload(
            "레벨 동기화 중", "미선택", "주화 확인 중", "마을 확인 중");

    private VillageInventoryPanel() {}

    public static void updateStatus(VillageNetwork.PlayerStatusPayload payload) { status = payload; }

    @SubscribeEvent
    public static void onInventoryInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.RequestPlayerStatusPayload("inventory"));
        }
    }

    @SubscribeEvent
    public static void onInventoryRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Layout layout = layout(graphics.guiWidth(), graphics.guiHeight());
        Minecraft minecraft = Minecraft.getInstance();
        Progress progress = parseProgress(status.progress());

        graphics.fill(layout.left() - 1, layout.top() - 1, layout.right() + 1, layout.bottom() + 1, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), BACKGROUND);
        graphics.fill(layout.left(), layout.top(), layout.left() + 4, layout.bottom(), ACCENT);

        graphics.text(minecraft.font, "수호자", layout.left() + 11, layout.top() + 9, TEXT, false);
        String level = "LV." + progress.level();
        graphics.text(minecraft.font, level,
                layout.right() - 10 - minecraft.font.width(level), layout.top() + 9, GOLD, false);

        int barLeft = layout.left() + 11;
        int barRight = layout.right() - 11;
        int barTop = layout.top() + 27;
        graphics.fill(barLeft, barTop, barRight, barTop + 5, 0xFFC1B39B);
        graphics.fill(barLeft + 1, barTop + 1,
                barLeft + 1 + Math.round((barRight - barLeft - 2) * progress.ratio()),
                barTop + 4, ACCENT);
        graphics.text(minecraft.font, progress.current() + " / " + progress.required() + " XP",
                barLeft, barTop + 9, MUTED, false);

        drawRow(graphics, minecraft, layout, layout.top() + 48, "직업", compact(status.role(), 13), ACCENT);
        drawRow(graphics, minecraft, layout, layout.top() + 64, "재화", economyValue(status.economy()), GOLD);
        graphics.text(minecraft.font, "C 통신 · R/G 기술", layout.left() + 11, layout.top() + 82, MUTED, false);

        int gap = 6;
        int buttonWidth = (layout.width() - 22 - gap) / 2;
        int firstY = layout.top() + 98;
        drawButton(graphics, minecraft, event.getMouseX(), event.getMouseY(),
                layout.left() + 11, firstY, buttonWidth, "상태 I", ACCENT);
        drawButton(graphics, minecraft, event.getMouseX(), event.getMouseY(),
                layout.left() + 11 + buttonWidth + gap, firstY, buttonWidth, "개인 P", GOLD);
        drawButton(graphics, minecraft, event.getMouseX(), event.getMouseY(),
                layout.left() + 11, firstY + 25, buttonWidth, "직업 O", ACCENT);
        drawButton(graphics, minecraft, event.getMouseX(), event.getMouseY(),
                layout.left() + 11 + buttonWidth + gap, firstY + 25, buttonWidth, "호출 V", GOLD);
    }

    private static void drawRow(GuiGraphicsExtractor graphics, Minecraft minecraft, Layout layout,
                                int y, String label, String value, int color) {
        graphics.fill(layout.left() + 11, y + 2, layout.left() + 14, y + 10, color);
        graphics.text(minecraft.font, label, layout.left() + 19, y, MUTED, false);
        int valueX = layout.left() + Math.max(50, layout.width() / 2 - 3);
        graphics.text(minecraft.font, compact(value, Math.max(7, (layout.right() - valueX - 8) / 6)),
                valueX, y, TEXT, false);
    }

    private static void drawButton(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                   int mouseX, int mouseY, int x, int y, int width,
                                   String label, int accent) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, 19);
        graphics.fill(x - 1, y - 1, x + width + 1, y + 20, hovered ? accent : BORDER);
        graphics.fill(x, y, x + width, y + 19, hovered ? SURFACE_HOVER : SURFACE);
        graphics.centeredText(minecraft.font, label, x + width / 2, y + 6, hovered ? TEXT : MUTED);
    }

    @SubscribeEvent
    public static void onInventoryClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen) || event.getButton() != 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        Layout layout = layout(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        int gap = 6;
        int buttonWidth = (layout.width() - 22 - gap) / 2;
        int firstY = layout.top() + 98;
        String action = null;
        if (inside(event.getMouseX(), event.getMouseY(), layout.left() + 11, firstY, buttonWidth, 19)) {
            action = "open_status";
        } else if (inside(event.getMouseX(), event.getMouseY(), layout.left() + 11 + buttonWidth + gap,
                firstY, buttonWidth, 19)) {
            action = "open_personal_progress";
        } else if (inside(event.getMouseX(), event.getMouseY(), layout.left() + 11,
                firstY + 25, buttonWidth, 19)) {
            action = "open_role_progress_current";
        } else if (inside(event.getMouseX(), event.getMouseY(), layout.left() + 11 + buttonWidth + gap,
                firstY + 25, buttonWidth, 19)) {
            action = "open_caller_menu";
        }
        if (action != null) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
            event.setCanceled(true);
        }
    }

    private static Layout layout(int screenWidth, int screenHeight) {
        int inventoryLeft = (screenWidth - 176) / 2;
        int inventoryRight = inventoryLeft + 176;
        int leftSpace = Math.max(0, inventoryLeft - 6);
        int rightSpace = Math.max(0, screenWidth - inventoryRight - 6);
        boolean useLeft = leftSpace >= MIN_WIDTH || rightSpace < MIN_WIDTH || leftSpace >= rightSpace;
        int available = useLeft ? leftSpace : rightSpace;
        int width = Math.max(MIN_WIDTH, Math.min(DESIRED_WIDTH, Math.max(MIN_WIDTH, available - 4)));
        int left = useLeft ? Math.max(2, inventoryLeft - width - 4)
                : Math.min(screenWidth - width - 2, inventoryRight + 4);
        int top = Math.max(3, screenHeight / 2 - PANEL_HEIGHT / 2);
        int bottom = Math.min(screenHeight - 3, top + PANEL_HEIGHT);
        if (bottom - top < 142) top = Math.max(2, bottom - 142);
        return new Layout(left, top, width, bottom - top);
    }

    private static Progress parseProgress(String text) {
        Matcher matcher = PROGRESS_PATTERN.matcher(text);
        if (matcher.matches()) {
            try {
                int level = Integer.parseInt(matcher.group(1));
                int current = Integer.parseInt(matcher.group(2));
                int required = Math.max(1, Integer.parseInt(matcher.group(3)));
                return new Progress(level, current, required);
            } catch (NumberFormatException ignored) { }
        }
        return new Progress(1, 0, 100);
    }

    private static String economyValue(String text) {
        Matcher matcher = Pattern.compile(".*?(\\d+).*").matcher(text);
        return matcher.matches() ? matcher.group(1) : compact(text, 8);
    }

    private static String compact(String value, int max) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        return normalized.length() <= max ? normalized
                : normalized.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Layout(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }
    }

    private record Progress(int level, int current, int required) {
        float ratio() { return Math.max(0.0f, Math.min(1.0f, current / (float) required)); }
    }
}
