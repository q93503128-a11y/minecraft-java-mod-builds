package kr.moonseungjun.villageguardians;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageClientKeys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "controls"));
    private static final KeyMapping ROLE_SKILL = new KeyMapping(
            "key.villageguardians.role_skill",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);
    private static boolean tickListenerRegistered;

    private VillageClientKeys() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ROLE_SKILL);
        if (!tickListenerRegistered) {
            tickListenerRegistered = true;
            NeoForge.EVENT_BUS.addListener(VillageClientKeys::onClientTick);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ROLE_SKILL.consumeClick()) {
            if (minecraft.player == null) {
                continue;
            }
            ClientPacketDistributor.sendToServer(
                    new VillageNetwork.VillageUiActionPayload("use_skill"));
        }
    }
}
