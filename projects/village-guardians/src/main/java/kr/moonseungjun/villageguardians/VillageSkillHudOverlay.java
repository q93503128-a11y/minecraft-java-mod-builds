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

/** Low-profile combat skill HUD. It never renders over menus or the vanilla hotbar zone.
 * Minecraft 26.2 moved the legacy minecraft.screen != null check to gui.screen().
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageSkillHudOverlay {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(
            VillageGuardians.MOD_ID, "skill_status_hud");
    private static final int TEXT = 0xFFF4F7F8;
    private static final int MUTED = 0xFF9DAAB1;
    private static final int ACCENT = 0xFF52D9C2;
    private static final int LINE = 0xAA426775;
    private static String text = "";
    private static long expiresAt;

    private VillageSkillHudOverlay() {}

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.OVERLAY_MESSAGE, LAYER_ID, VillageSkillHudOverlay::render);
    }

    public static void accept(VillageNetwork.SkillHudPayload payload) {
        text = payload == null || payload.text() == null ? ""
                : VillageClientKeys.resolveTokens(payload.text());
        expiresAt = System.currentTimeMillis() + 1_500L;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null || text.isBlank()
                || System.currentTimeMillis() > expiresAt) return;

        Font font = minecraft.font;
        String[] sections = text.split(" §8│ ", -1);
        int primaryCount = Math.min(2, sections.length);
        int centerX = graphics.guiWidth() / 2;
        int y = Math.max(54, graphics.guiHeight() - 98);
        int gap = 16;
        int itemWidth = Math.min(128, Math.max(84, (graphics.guiWidth() - 78) / 3));
        int totalWidth = primaryCount * itemWidth + Math.max(0, primaryCount - 1) * gap;
        int startX = centerX - totalWidth / 2;

        for (int index = 0; index < primaryCount; index++) {
            int left = startX + index * (itemWidth + gap);
            String value = fit(font, plain(sections[index]), itemWidth - 18);
            int diamondX = left + 6;
            VillageQuickChatSafeScreen.drawDiamond(graphics, diamondX, y + 5, 5, 0xDD18323A);
            VillageQuickChatSafeScreen.drawDiamondOutline(graphics, diamondX, y + 5, 5, ACCENT);
            graphics.text(font, value, left + 16, y, TEXT, true);
            graphics.fill(left + 15, y + 12, left + itemWidth, y + 13, LINE);
            graphics.fill(left + 15, y + 12, left + 35, y + 14, ACCENT);
        }

        if (sections.length > 2) {
            StringBuilder active = new StringBuilder();
            for (int index = 2; index < sections.length; index++) {
                if (!active.isEmpty()) active.append(" · ");
                active.append(plain(sections[index]));
            }
            String fitted = fit(font, active.toString(), Math.max(80, graphics.guiWidth() - 60));
            graphics.centeredText(font, fitted, centerX, y - 17, ACCENT);
        }

        if (primaryCount == 0) {
            graphics.centeredText(font, fit(font, plain(text), Math.max(80, graphics.guiWidth() - 40)),
                    centerX, y, MUTED);
        }
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static String fit(Font font, String value, int maximumWidth) {
        if (maximumWidth <= 0) return "";
        if (font.width(value) <= maximumWidth) return value;
        String suffix = "…";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + suffix) > maximumWidth) end--;
        return value.substring(0, end) + suffix;
    }
}
