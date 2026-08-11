package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Persistent village status HUD. Never occupies the vanilla action-bar/hotbar zone.
 * Minecraft 26.2 moved the legacy minecraft.screen != null check to gui.screen().
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageMainHudOverlay {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(
            VillageGuardians.MOD_ID, "village_main_hud");
    private static final int TEXT = 0xFFF4F7F8;
    private static final int MUTED = 0xFFB9C3C8;
    private static final int CYAN = 0xFF52D9C2;
    private static final int GOLD = 0xFFFFC65C;
    private static final int BACK = 0x76081218;
    private static final int EDGE = 0xB3436975;
    private static String text = "";
    private static long lastUpdate;

    private VillageMainHudOverlay() {}

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.OVERLAY_MESSAGE, LAYER_ID, VillageMainHudOverlay::render);
    }

    public static void accept(VillageNetwork.MainHudPayload payload) {
        text = payload == null || payload.text() == null ? ""
                : VillageClientKeys.resolveTokens(payload.text());
        lastUpdate = System.currentTimeMillis();
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null || text.isBlank()) return;
        if (System.currentTimeMillis() - lastUpdate > 3_000L) return;

        Font font = minecraft.font;
        String[] sections = text.split(" §8│ ", -1);
        String day = sections.length > 0 ? plain(sections[0]) : plain(text);
        String level = sections.length > 1 ? plain(sections[1]) : "";
        String role = sections.length > 2 ? plain(sections[2]) : "";
        String economy = sections.length > 3 ? plain(sections[3]) : "";

        int maxWidth = Math.min(330, Math.max(150, graphics.guiWidth() - 34));
        String first = fit(font, join(day, level, "  ·  "), maxWidth - 24);
        String second = fit(font, join(role, economy, "  ·  "), maxWidth - 24);
        int contentWidth = Math.max(font.width(first), font.width(second));
        int panelWidth = Math.min(maxWidth, Math.max(154, contentWidth + 28));
        int left = 10;
        int top = 10;
        int bottom = top + 35;

        graphics.fill(left, top, left + panelWidth, bottom, BACK);
        graphics.fill(left, top, left + 3, bottom, CYAN);
        graphics.fill(left + 3, bottom - 1, left + panelWidth, bottom, EDGE);
        VillageQuickChatSafeScreen.drawDiamond(graphics, left + 12, top + 11, 4, 0xE018323A);
        VillageQuickChatSafeScreen.drawDiamondOutline(graphics, left + 12, top + 11, 4, CYAN);
        graphics.text(font, first, left + 22, top + 5, TEXT, true);
        graphics.text(font, second, left + 22, top + 19, MUTED, true);
        int accent = Math.min(panelWidth - 8, Math.max(34, panelWidth * 38 / 100));
        graphics.fill(left + 4, bottom - 2, left + accent, bottom, GOLD);
    }

    private static String join(String a, String b, String separator) {
        if (a == null || a.isBlank()) return b == null ? "" : b;
        if (b == null || b.isBlank()) return a;
        return a + separator + b;
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static String fit(Font font, String value, int maximumWidth) {
        if (maximumWidth <= 0 || value == null) return "";
        if (font.width(value) <= maximumWidth) return value;
        String suffix = "…";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + suffix) > maximumWidth) end--;
        return value.substring(0, end) + suffix;
    }
}
