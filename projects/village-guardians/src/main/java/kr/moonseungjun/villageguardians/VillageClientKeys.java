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
    private static final KeyMapping ROLE_SKILL_ONE = key("role_skill_one", GLFW.GLFW_KEY_R);
    private static final KeyMapping ROLE_SKILL_TWO = key("role_skill_two", GLFW.GLFW_KEY_G);
    private static final KeyMapping QUICK_COMMUNICATION = key("quick_communication", GLFW.GLFW_KEY_C);
    private static final KeyMapping STATUS = key("status", GLFW.GLFW_KEY_I);
    private static final KeyMapping PERSONAL_PROGRESS = key("personal_progress", GLFW.GLFW_KEY_P);
    private static final KeyMapping ROLE_PROGRESS = key("role_progress", GLFW.GLFW_KEY_O);
    private static final KeyMapping CALLER = key("caller", GLFW.GLFW_KEY_V);
    private static boolean tickListenerRegistered;

    private VillageClientKeys() {}

    private static KeyMapping key(String id, int key) {
        return new KeyMapping("key.villageguardians." + id, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ROLE_SKILL_ONE);
        event.register(ROLE_SKILL_TWO);
        event.register(QUICK_COMMUNICATION);
        event.register(STATUS);
        event.register(PERSONAL_PROGRESS);
        event.register(ROLE_PROGRESS);
        event.register(CALLER);
        if (!tickListenerRegistered) {
            tickListenerRegistered = true;
            NeoForge.EVENT_BUS.addListener(VillageClientKeys::onClientTick);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        consume(minecraft, ROLE_SKILL_ONE, "use_skill:0");
        consume(minecraft, ROLE_SKILL_TWO, "use_skill:1");
        consume(minecraft, QUICK_COMMUNICATION, "open_quick_chat");
        consume(minecraft, STATUS, "open_status");
        consume(minecraft, PERSONAL_PROGRESS, "open_personal_progress");
        consume(minecraft, ROLE_PROGRESS, "open_role_progress_current");
        consume(minecraft, CALLER, "open_caller_menu");
    }

    private static void consume(Minecraft minecraft, KeyMapping mapping, String action) {
        while (mapping.consumeClick()) {
            if (minecraft.player != null) {
                ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
            }
        }
    }
}
