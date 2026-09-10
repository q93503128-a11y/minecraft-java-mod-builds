package kr.moonseungjun.turnboundre.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.ui.ExpeditionJournalScreen;
import kr.moonseungjun.turnboundre.client.ui.PartyFormationScreen;
import kr.moonseungjun.turnboundre.client.ui.TurnboundMenuScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Single player-facing entry point for TURNBOUND management screens. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class TurnboundMenuInput {
    private static KeyMapping openMenu;

    private TurnboundMenuInput() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (openMenu == null) return;
        while (openMenu.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) continue;

            Screen current = minecraft.gui.screen();
            if (isTurnboundManagementScreen(current)) {
                minecraft.gui.setScreen(null);
                continue;
            }
            if (current != null) continue;
            if (BattleClientState.latestSnapshot().isPresent()) continue;
            minecraft.gui.setScreen(new TurnboundMenuScreen());
        }
    }

    static boolean isTurnboundManagementScreen(Screen screen) {
        return screen instanceof TurnboundMenuScreen
                || screen instanceof ExpeditionJournalScreen
                || screen instanceof PartyFormationScreen;
    }

    /** RegisterKeyMappingsEvent implements the mod-bus marker and is routed automatically by current FML. */
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = new KeyMapping.Category(
                Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "menu"));
        event.registerCategory(category);
        openMenu = new KeyMapping(
                "key.turnbound_re.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                category);
        event.register(openMenu);
    }
}
