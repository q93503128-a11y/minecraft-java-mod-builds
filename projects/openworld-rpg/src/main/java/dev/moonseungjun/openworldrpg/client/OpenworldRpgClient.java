package dev.moonseungjun.openworldrpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Project-owned frequent combat inputs. Spell Engine remains a casting backend, not the input UI.
 */
public final class OpenworldRpgClient implements ClientModInitializer {
    private static final KeyMapping DODGE_KEY =
            key(
                    "key.openworld_rpg.dodge",
                    GLFW.GLFW_KEY_LEFT_ALT
            );

    private static final List<KeyMapping> SKILL_KEYS =
            List.of(
                    key("key.openworld_rpg.active_skill_1", GLFW.GLFW_KEY_F),
                    key("key.openworld_rpg.active_skill_2", GLFW.GLFW_KEY_G),
                    key("key.openworld_rpg.active_skill_3", GLFW.GLFW_KEY_V),
                    key("key.openworld_rpg.active_skill_4", GLFW.GLFW_KEY_X),
                    key("key.openworld_rpg.ultimate", GLFW.GLFW_KEY_Y)
            );

    @Override
    public void onInitializeClient() {
        for (KeyMapping key : SKILL_KEYS) {
            KeyMappingHelper.registerKeyMapping(key);
        }
        KeyMappingHelper.registerKeyMapping(DODGE_KEY);
        ProjectDodgeClientBridge.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.gui.screen() != null) {
                return;
            }

            for (int slot = 0; slot < SKILL_KEYS.size(); slot++) {
                KeyMapping key = SKILL_KEYS.get(slot);
                while (key.consumeClick()) {
                    ProjectSpellClientBridge.castSlot(
                            client.player,
                            slot
                    );
                }
            }
            while (DODGE_KEY.consumeClick()) {
                ProjectDodgeClientBridge.request(
                        client.player
                );
            }
        });
    }

    private static KeyMapping key(
            String translationKey,
            int glfwKey
    ) {
        return new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                glfwKey,
                CategoryHolder.COMBAT
        );
    }

    private static final class CategoryHolder {
        private static final KeyMapping.Category COMBAT =
                KeyMapping.Category.register(
                        Identifier.fromNamespaceAndPath(
                                OpenworldRpgMod.MOD_ID,
                                "combat"
                        )
                );
    }
}
