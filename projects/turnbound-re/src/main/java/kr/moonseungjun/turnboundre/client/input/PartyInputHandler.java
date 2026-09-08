package kr.moonseungjun.turnboundre.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.ProgressionClientState;
import kr.moonseungjun.turnboundre.client.ui.PartyFormationScreen;
import kr.moonseungjun.turnboundre.network.ProgressionNetworkPayloads;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Configurable entry point for the standalone Party Formation screen. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class PartyInputHandler {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "menu"));

    private static final KeyMapping OPEN_PARTY = new KeyMapping(
            "key.turnbound_re.open_party_formation",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY);

    private PartyInputHandler() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_PARTY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_PARTY.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null || minecraft.gui.getScreen() != null) continue;
            ProgressionClientState.clear();
            minecraft.gui.setScreen(new PartyFormationScreen());
            ClientPacketDistributor.sendToServer(new ProgressionNetworkPayloads.RequestProgressC2S());
        }
    }
}
