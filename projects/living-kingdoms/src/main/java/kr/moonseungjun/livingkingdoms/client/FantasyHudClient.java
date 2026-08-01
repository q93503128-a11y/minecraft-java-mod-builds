package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.FantasyHudStatePayload;
import kr.moonseungjun.livingkingdoms.world.StarterRealmManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Set;

/** Compact realm HUD with a clear information hierarchy and no large opaque panels. */
public final class FantasyHudClient {
    private static final Set<Identifier> HIDDEN_LAYERS = Set.of(
            VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH,
            VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND,
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
            VanillaGuiLayers.SELECTED_ITEM_NAME
    );
    private static final int PANEL = 0xB8141820;
    private static final int PANEL_EDGE = 0xCC786443;
    private static final int TEXT = 0xFFF0E6CF;
    private static final int MUTED = 0xFFC6B99C;

    private FantasyHudClient() {
    }

    public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!insideRealm(minecraft) || minecraft.player.isSpectator()) return;
        Identifier layer = event.getName();
        if (VanillaGuiLayers.HOTBAR.equals(layer)) renderHud(event.getGuiGraphics(), minecraft);
        if (HIDDEN_LAYERS.contains(layer)) event.setCanceled(true);
    }

    private static void renderHud(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (width < 170 || height < 90) return;

        Font font = minecraft.font;
        FantasyHudStatePayload civic = ClientNetworkHandlers.hudState();
        int slot = width < 420 ? 19 : 21;
        int gap = 2;
        int hotbarWidth = slot * 9 + gap * 8;
        int hotbarX = (width - hotbarWidth) / 2;
        int hotbarY = height - slot - 5;
        int selected = minecraft.player.getInventory().getSelectedSlot();

        for (int index = 0; index < 9; index++) {
            int x = hotbarX + index * (slot + gap);
            slot(graphics, x, hotbarY, slot, index == selected);
            ItemStack stack = minecraft.player.getInventory().getItem(index);
            if (!stack.isEmpty()) {
                int iconX = x + Math.max(1, (slot - 16) / 2);
                int iconY = hotbarY + Math.max(1, (slot - 16) / 2);
                ExternalRpgUi.itemStackIcon(graphics, stack, iconX, iconY);
                if (stack.getCount() > 1) {
                    String count = Integer.toString(stack.getCount());
                    graphics.text(font, Component.literal(count), x + slot - font.width(count) - 1,
                            hotbarY + slot - 8, 0xFFFFFFFF, true);
                }
            }
        }

        drawVitals(graphics, minecraft, civic, hotbarY);
        drawWorldLine(graphics, minecraft, civic, width);
        drawCompass(graphics, minecraft, width);
        drawSelectedItem(graphics, minecraft, width, hotbarY, hotbarWidth);
    }

    private static void drawVitals(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                   FantasyHudStatePayload civic, int hotbarY) {
        Font font = minecraft.font;
        int width = 154;
        int x = 7;
        int y = Math.max(7, hotbarY - 52);
        panel(graphics, x, y, width, 45);

        float healthRatio = minecraft.player.getMaxHealth() <= 0.0F ? 0.0F
                : minecraft.player.getHealth() / minecraft.player.getMaxHealth();
        bar(graphics, x + 7, y + 7, width - 14, 8, healthRatio, 0xFFD25A50);
        graphics.text(font, Component.literal("생명 " + decimal(minecraft.player.getHealth())
                        + "/" + decimal(minecraft.player.getMaxHealth())),
                x + 8, y + 5, TEXT, true);

        int armor = minecraft.player.getArmorValue();
        bar(graphics, x + 7, y + 20, width - 14, 5, Math.min(1.0F, armor / 20.0F), 0xFF718CA5);
        String legal = civic.wanted() > 0 ? "수배 " + civic.wanted() : "법 상태 정상";
        graphics.text(font, Component.literal("은화 " + civic.silver()), x + 8, y + 30, 0xFFE6C873, false);
        graphics.text(font, Component.literal(legal), x + width - font.width(legal) - 8, y + 30,
                civic.wanted() > 0 ? 0xFFFF8172 : 0xFFAFC8A6, false);
    }

    private static void drawWorldLine(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                      FantasyHudStatePayload civic, int screenWidth) {
        if (screenWidth < 440) return;
        long time = minecraft.level.getGameTime();
        long day = Math.floorDiv(time, 24_000L) + 1L;
        int hour = (int) Math.floorMod(time / 1_000L + 6L, 24L);
        int minute = (int) Math.floorMod(time, 1_000L) * 60 / 1_000;
        String text = "왕국력 " + day + "일  "
                + String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute)
                + "  ·  " + shortText(civic.profession(), 18)
                + "  ·  명망 " + civic.renown();
        int textWidth = minecraft.font.width(text);
        int x = Math.max(170, (screenWidth - textWidth) / 2);
        int y = 7;
        graphics.fill(x - 7, y - 3, x + textWidth + 7, y + 12, 0x9810141B);
        graphics.fill(x - 7, y + 11, x + textWidth + 7, y + 12, 0xAA8C7448);
        graphics.text(minecraft.font, Component.literal(text), x, y, MUTED, false);
    }

    private static void drawCompass(GuiGraphicsExtractor graphics, Minecraft minecraft, int screenWidth) {
        if (screenWidth < 280) return;
        int size = screenWidth < 500 ? 48 : 54;
        int x = screenWidth - size - 7;
        int y = 7;
        ExternalRpgUi.minimapFrame(graphics, x, y, size);
        int arrowSize = Math.max(10, size / 4);
        ExternalRpgUi.minimapArrow(graphics, x + (size - arrowSize) / 2,
                y + (size - arrowSize) / 2 - 2, arrowSize);
        graphics.centeredText(minecraft.font, Component.literal(directionName(minecraft.player.getYRot())),
                x + size / 2, y + size - 12, TEXT);
    }

    private static void drawSelectedItem(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                         int screenWidth, int hotbarY, int hotbarWidth) {
        ItemStack selected = minecraft.player.getInventory().getSelectedItem();
        if (selected.isEmpty()) return;
        String name = shortText(selected.getHoverName().getString(), 30);
        int width = Math.min(hotbarWidth, minecraft.font.width(name) + 14);
        int x = (screenWidth - width) / 2;
        graphics.fill(x, hotbarY - 15, x + width, hotbarY - 3, 0xA810141B);
        graphics.centeredText(minecraft.font, Component.literal(name), screenWidth / 2,
                hotbarY - 13, TEXT);
    }

    private static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x55000000);
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.fill(x, y, x + width, y + 1, PANEL_EDGE);
        graphics.fill(x, y + height - 1, x + width, y + height, 0x88574731);
    }

    private static void slot(GuiGraphicsExtractor graphics, int x, int y, int size, boolean selected) {
        graphics.fill(x, y, x + size, y + size, selected ? 0xD22B313A : 0xB8171C23);
        int edge = selected ? 0xFFE3C474 : 0xAA7C694C;
        graphics.fill(x, y, x + size, y + 1, edge);
        graphics.fill(x, y + size - 1, x + size, y + size, edge);
        graphics.fill(x, y, x + 1, y + size, edge);
        graphics.fill(x + size - 1, y, x + size, y + size, edge);
    }

    private static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                            float ratio, int color) {
        graphics.fill(x, y, x + width, y + height, 0xB3090C10);
        int filled = Math.max(0, Math.min(width, Math.round(width * clamp(ratio))));
        if (filled > 0) graphics.fill(x, y, x + filled, y + height, color);
    }

    private static String directionName(float yaw) {
        int direction = Math.floorMod(Math.round(yaw / 45.0F), 8);
        return switch (direction) {
            case 0 -> "남";
            case 1 -> "남서";
            case 2 -> "서";
            case 3 -> "북서";
            case 4 -> "북";
            case 5 -> "북동";
            case 6 -> "동";
            default -> "남동";
        };
    }

    private static String decimal(float value) {
        return String.format(java.util.Locale.ROOT, "%.0f", value);
    }

    private static String shortText(String text, int max) {
        if (text == null || text.isBlank()) return "미등록";
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static boolean insideRealm(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null
                && minecraft.level.dimension().equals(StarterRealmManager.REALM_KEY);
    }
}
