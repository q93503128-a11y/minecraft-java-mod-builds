package kr.moonseungjun.villageguardians;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageSkillHudOverlay {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(
            VillageGuardians.MOD_ID, "skill_status_hud");
    private static String text = "";
    private static long expiresAt;

    private VillageSkillHudOverlay() {}

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.OVERLAY_MESSAGE,
                LAYER_ID,
                VillageSkillHudOverlay::render);
    }

    public static void accept(VillageNetwork.SkillHudPayload payload) {
        text = payload == null || payload.text() == null ? "" : payload.text();
        expiresAt = System.currentTimeMillis() + 1_500L;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || text.isBlank()
                || System.currentTimeMillis() > expiresAt) {
            return;
        }
        Font font = minecraft.font;
        int maximumWidth = Math.max(80, graphics.guiWidth() - 24);
        String fitted = font.plainSubstrByWidth(text, maximumWidth - 14);
        if (fitted.length() < text.length()) {
            fitted = font.plainSubstrByWidth(fitted, maximumWidth - 22) + "…";
        }
        int width = font.width(fitted);
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 92;
        graphics.fill(x - 5, y - 3, x + width + 5, y + font.lineHeight + 3, 0x78000000);
        graphics.centeredText(font, Component.literal(fitted), graphics.guiWidth() / 2, y, 0xFFFFFFFF);
    }
}
