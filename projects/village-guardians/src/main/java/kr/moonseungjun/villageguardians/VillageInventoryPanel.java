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
    private static final int DESIRED_WIDTH = 142;
    private static final int MIN_WIDTH = 104;
    private static final int PANEL_HEIGHT = 148;
    private static final int BACKGROUND = 0xFFE5DAC2;
    private static final int SURFACE = 0xFFF2EBD9;
    private static final int SURFACE_HOVER = 0xFFE2D1A9;
    private static final int BORDER = 0xFF6B5D49;
    private static final int ACCENT = 0xFF367D75;
    private static final int GOLD = 0xFFA7792E;
    private static final int TEXT = 0xFF2A241D;
    private static final int MUTED = 0xFF675F55;
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
        graphics.fill(layout.left(), layout.top(), layout.left() + 3, layout.bottom(), ACCENT);

        graphics.text(minecraft.font, "수호자", layout.left() + 9, layout.top() + 7, TEXT, false);
        String level = "Lv." + progress.level();
        graphics.text(minecraft.font, level,
                layout.right() - 8 - minecraft.font.width(level), layout.top() + 7, GOLD, false);

        int barLeft = layout.left() + 9;
        int barRight = layout.right() - 9;
        int barTop = layout.top() + 22;
        graphics.fill(barLeft, barTop, barRight, barTop + 4, 0xFFB8AB93);
        graphics.fill(barLeft + 1, barTop + 1,
                barLeft + 1 + Math.round((barRight - barLeft - 2) * progress.ratio()),
                barTop + 3, ACCENT);
        graphics.text(minecraft.font, fit(minecraft, progress.current() + "/" + progress.required() + " XP",
                        barRight - barLeft), barLeft, barTop + 7, MUTED, false);

        drawRow(graphics, minecraft, layout, layout.top() + 41, "직업", status.role(), ACCENT);
        drawRow(graphics, minecraft, layout, layout.top() + 55, "주화", economyValue(status.economy()), GOLD);
        graphics.text(minecraft.font, fit(minecraft, VillageClientKeys.compactSummary(), layout.width() - 18),
                layout.left() + 9, layout.top() + 70, MUTED, false);

        var player = minecraft.player;
        int wall = player == null ? 0 : VillageEquipmentSetSystem.countEquipped(player,
                VillageEquipmentSetSystem.EquipmentSet.WALL_GUARDIAN);
        int hunter = player == null ? 0 : VillageEquipmentSetSystem.countEquipped(player,
                VillageEquipmentSetSystem.EquipmentSet.NIGHT_HUNTER);
        graphics.text(minecraft.font, fit(minecraft, setLine("성벽 수호자", wall), layout.width() - 18),
                layout.left() + 9, layout.top() + 82, wall >= 2 ? GOLD : MUTED, false);
        graphics.text(minecraft.font, fit(minecraft, setLine("밤사냥꾼", hunter), layout.width() - 18),
                layout.left() + 9, layout.top() + 93, hunter >= 2 ? ACCENT : MUTED, false);

        int gap = 5;
        int buttonWidth = (layout.width() - 18 - gap) / 2;
        int firstY = layout.top() + 108;
        drawButton(graphics, minecraft, event.getMouseX(), event.getMouseY(),
                layout.left() + 9, firstY, buttonWidth, VillageClientKeys.statusKeyName() + " 상태", ACCENT);
        drawButton(graphics, minecraft, event.getMouseX(), event.getMouseY(),
                layout.left() + 9 + buttonWidth + gap, firstY, buttonWidth, VillageClientKeys.growthKeyName() + " 성장", GOLD);
        drawButton(graphics, minecraft, event.getMouseX(), event.getMouseY(),
                layout.left() + 9, firstY + 21, buttonWidth, VillageClientKeys.roleProgressKeyName() + " 직업 성장", ACCENT);
        drawButton(graphics, minecraft, event.getMouseX(), event.getMouseY(),
                layout.left() + 9 + buttonWidth + gap, firstY + 21, buttonWidth, VillageClientKeys.quickCommunicationKeyName() + " 통신", GOLD);
    }

    private static String setLine(String name, int count) {
        return name + " " + count + "/3  " + (count >= 2 ? "◆2" : "◇2") + " " + (count >= 3 ? "◆3" : "◇3");
    }

    private static void drawRow(GuiGraphicsExtractor graphics, Minecraft minecraft, Layout layout,
                                int y, String label, String value, int color) {
        graphics.fill(layout.left() + 9, y + 2, layout.left() + 12, y + 9, color);
        graphics.text(minecraft.font, label, layout.left() + 16, y, MUTED, false);
        int valueX = layout.left() + Math.max(43, layout.width() / 2 - 4);
        graphics.text(minecraft.font, fit(minecraft, value, layout.right() - valueX - 7),
                valueX, y, TEXT, false);
    }

    private static void drawButton(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                   int mouseX, int mouseY, int x, int y, int width,
                                   String label, int accent) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, 16);
        graphics.fill(x - 1, y - 1, x + width + 1, y + 17, hovered ? accent : BORDER);
        graphics.fill(x, y, x + width, y + 16, hovered ? SURFACE_HOVER : SURFACE);
        graphics.centeredText(minecraft.font, fit(minecraft, label, width - 6),
                x + width / 2, y + 4, hovered ? TEXT : MUTED);
    }

    @SubscribeEvent
    public static void onInventoryClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen) || event.getButton() != 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        Layout layout = layout(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        int gap = 5;
        int buttonWidth = (layout.width() - 18 - gap) / 2;
        int firstY = layout.top() + 108;
        String action = null;
        if (inside(event.getMouseX(), event.getMouseY(), layout.left() + 9, firstY, buttonWidth, 16)) {
            action = "open_status";
        } else if (inside(event.getMouseX(), event.getMouseY(), layout.left() + 9 + buttonWidth + gap,
                firstY, buttonWidth, 16)) {
            action = "open_skill_tree";
        } else if (inside(event.getMouseX(), event.getMouseY(), layout.left() + 9,
                firstY + 21, buttonWidth, 16)) {
            action = "open_role_progress_current";
        } else if (inside(event.getMouseX(), event.getMouseY(), layout.left() + 9 + buttonWidth + gap,
                firstY + 21, buttonWidth, 16)) {
            action = "open_quick_chat";
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
        if (bottom - top < 146) top = Math.max(2, bottom - 146);
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
        return matcher.matches() ? matcher.group(1) : text;
    }

    private static String fit(Minecraft minecraft, String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (maxWidth <= 0 || minecraft.font.width(normalized) <= maxWidth) return normalized;
        String suffix = "…";
        int end = normalized.length();
        while (end > 1 && minecraft.font.width(normalized.substring(0, end) + suffix) > maxWidth) end--;
        return normalized.substring(0, Math.max(1, end)) + suffix;
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
