package kr.moonseungjun.senbonzakura.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.senbonzakura.ability.ShowcaseAbility;
import kr.moonseungjun.senbonzakura.network.BankaiVisualPayload;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

/** Direct test keys for the eight cinematic showcase abilities. */
public final class AbilityKeyHandler {
    private static final Map<ShowcaseAbility, KeyMapping> KEYS = new LinkedHashMap<>();

    static {
        bind(ShowcaseAbility.SKYFALL, "skyfall", GLFW.GLFW_KEY_Z);
        bind(ShowcaseAbility.WORLD_DIVIDE, "world_divide", GLFW.GLFW_KEY_V);
        bind(ShowcaseAbility.BLACK_SUN, "black_sun", GLFW.GLFW_KEY_G);
        bind(ShowcaseAbility.SWORD_GRAVE, "sword_grave", GLFW.GLFW_KEY_R);
        bind(ShowcaseAbility.GRAVITY_REVERSAL, "gravity_reversal", GLFW.GLFW_KEY_H);
        bind(ShowcaseAbility.LAST_SECOND, "last_second", GLFW.GLFW_KEY_J);
        bind(ShowcaseAbility.HEAVEN_JUDGMENT, "heaven_judgment", GLFW.GLFW_KEY_K);
        bind(ShowcaseAbility.STELLAR_LANCE, "stellar_lance", GLFW.GLFW_KEY_O);
    }

    private AbilityKeyHandler() {}

    private static void bind(ShowcaseAbility ability, String suffix, int glfwKey) {
        KEYS.put(ability, new KeyMapping(
                "key.senbonzakura.ability_" + suffix,
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                glfwKey,
                KeyMapping.Category.MISC));
    }

    public static void register(RegisterKeyMappingsEvent event) {
        KEYS.values().forEach(event::register);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        for (Map.Entry<ShowcaseAbility, KeyMapping> entry : KEYS.entrySet()) {
            while (entry.getValue().consumeClick()) {
                ClientPacketDistributor.sendToServer(new BankaiVisualPayload(
                        "action=ability_request;ability=" + entry.getKey().id()));
            }
        }
    }
}
