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

/** A responsive HUD for the authored realm. Vanilla survival meters are deliberately removed. */
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
        int slot = width < 420 ? 20 : 22;
        int gap = 1;
        int hotbarWidth = slot * 9 + gap * 8;
        int hotbarX = (width - hotbarWidth) / 2;
        int hotbarY = height - slot - 5;
        int selected = minecraft.player.getInventory().getSelectedSlot();

        for (int index = 0; index < 9; index++) {
            int x = hotbarX + index * (slot + gap);
            ExternalRpgUi.hudButton(graphics, font, x, hotbarY, slot, slot, "", index == selected);
            ItemStack stack = minecraft.player.getInventory().getItem(index);
            if (!stack.isEmpty()) {
                int iconX = x + Math.max(2, (slot - 16) / 2);
                int iconY = hotbarY + Math.max(2, (slot - 16) / 2);
                ExternalRpgUi.itemStackIcon(graphics, stack, iconX, iconY);
                if (stack.getCount() > 1) {
                    String count = Integer.toString(stack.getCount());
                    graphics.text(font, Component.literal(count), x + slot - font.width(count) - 2,
                            hotbarY + slot - 9, 0xFFFFFFFF, true);
                }
            }
        }

        int leftWidth = Math.min(220, Math.max(150, (width - hotbarWidth) / 2 - 10));
        int leftX = 6;
        int meterY = Math.max(6, hotbarY - 72);
        float healthRatio = minecraft.player.getMaxHealth() <= 0.0F ? 0.0F
                : minecraft.player.getHealth() / minecraft.player.getMaxHealth();
        String health = decimal(minecraft.player.getHealth()) + " / " + decimal(minecraft.player.getMaxHealth());
        ExternalRpgUi.hudMeter(graphics, font, leftX, meterY, leftWidth,
                "생명", health, healthRatio, 0xFFB94F43);

        int armor = minecraft.player.getArmorValue();
        ExternalRpgUi.hudMeter(graphics, font, leftX, meterY + 24, leftWidth,
                "방호", Integer.toString(armor), Math.min(1.0F, armor / 20.0F), 0xFF66839A);
        ExternalRpgUi.hudPanel(graphics, leftX, meterY + 48, leftWidth, 22);
        graphics.text(font, Component.literal("은화 " + civic.silver()), leftX + 7, meterY + 54,
                0xFFF0D690, true);
        String legal = civic.wanted() > 0 ? "수배 " + civic.wanted() : "법적 상태 정상";
        graphics.text(font, Component.literal(legal), leftX + leftWidth - font.width(legal) - 7,
                meterY + 54, civic.wanted() > 0 ? 0xFFFF8A76 : 0xFFC8D8B8, false);

        ItemStack selectedStack = minecraft.player.getInventory().getSelectedItem();
        if (!selectedStack.isEmpty()) {
            String name = selectedStack.getHoverName().getString();
            int labelWidth = Math.min(hotbarWidth, Math.max(80, font.width(name) + 18));
            int labelX = (width - labelWidth) / 2;
            ExternalRpgUi.hudPanel(graphics, labelX, hotbarY - 21, labelWidth, 18);
            graphics.centeredText(font, Component.literal(shortText(name, 34)), width / 2,
                    hotbarY - 16, 0xFFF6E8C7);
        }

        drawChronicle(graphics, minecraft, civic, width, hotbarX + hotbarWidth + 7, hotbarY);
        drawCompass(graphics, minecraft, width);
    }

    private static void drawChronicle(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                      FantasyHudStatePayload civic, int screenWidth,
                                      int requestedX, int hotbarY) {
        if (screenWidth < 430) return;
        Font font = minecraft.font;
        int panelWidth = Math.min(210, Math.max(150, screenWidth - requestedX - 6));
        int panelHeight = 61;
        int x = Math.max(requestedX, screenWidth - panelWidth - 6);
        int y = Math.max(6, hotbarY - panelHeight - 2);
        ExternalRpgUi.hudPanel(graphics, x, y, panelWidth, panelHeight);

        long dayTime = minecraft.level.getLevelData().getDayTime();
        long day = Math.floorDiv(dayTime, 24_000L);
        long seasonDay = Math.floorMod(day, 112L);
        String season = switch ((int) (seasonDay / 28L)) {
            case 0 -> "새봄";
            case 1 -> "높은여름";
            case 2 -> "수확철";
            default -> "긴겨울";
        };
        int hour = (int) Math.floorMod(dayTime / 1_000L + 6L, 24L);
        int minute = (int) Math.floorMod(dayTime, 1_000L) * 60 / 1_000;
        String clock = String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute);

        graphics.text(font, Component.literal("왕국력 " + (day + 1) + "일 · " + season),
                x + 8, y + 7, 0xFFF3E0B8, true);
        graphics.text(font, Component.literal(clock + " · " + weatherName(minecraft)),
                x + 8, y + 20, 0xFFD8C59D, false);
        graphics.text(font, Component.literal(shortText(civic.profession(), 28) + " · 명망 " + civic.renown()),
                x + 8, y + 33, 0xFFD8C59D, false);
        graphics.text(font, Component.literal("시세 곡" + civic.grainIndex() + " 금" + civic.metalIndex()
                        + " 약" + civic.herbIndex() + " 노" + civic.laborIndex()),
                x + 8, y + 46, 0xFFC7B38E, false);
    }

    private static void drawCompass(GuiGraphicsExtractor graphics, Minecraft minecraft, int screenWidth) {
        if (screenWidth < 300) return;
        int size = screenWidth < 500 ? 56 : 68;
        int x = screenWidth - size - 7;
        int y = 7;
        ExternalRpgUi.minimapFrame(graphics, x, y, size);
        int arrowSize = Math.max(10, size / 4);
        ExternalRpgUi.minimapArrow(graphics, x + (size - arrowSize) / 2, y + (size - arrowSize) / 2, arrowSize);
        String direction = directionName(minecraft.player.getYRot());
        graphics.centeredText(minecraft.font, Component.literal(direction), x + size / 2,
                y + size - 13, 0xFFF3E2B8);
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

    private static String weatherName(Minecraft minecraft) {
        if (minecraft.level.isThundering()) return "폭풍";
        if (minecraft.level.isRaining()) return "비";
        return "맑음";
    }

    private static String decimal(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String shortText(String text, int max) {
        if (text == null || text.length() <= max) return text == null ? "" : text;
        return text.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static boolean insideRealm(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null
                && minecraft.level.dimension().equals(StarterRealmManager.REALM_KEY);
    }
}
