package dev.moonseungjun.openworldrpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.entity.TrailStagEntity;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Project-owned frequent combat and traversal inputs. */
public final class OpenworldRpgClient implements ClientModInitializer {
    private static final KeyMapping DODGE_KEY =
            key(
                    "key.openworld_rpg.dodge",
                    GLFW.GLFW_KEY_Q
            );

    private static final KeyMapping MOUNT_KEY =
            key(
                    "key.openworld_rpg.mount_action",
                    GLFW.GLFW_KEY_U,
                    CategoryHolder.TRAVERSAL
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
        KeyMappingHelper.registerKeyMapping(MOUNT_KEY);
        ProjectDodgeClientBridge.initialize();

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.gui.screen() != null) {
                return;
            }
            suppressVanillaQuickActionConflict(
                    client.options.keyDrop,
                    DODGE_KEY
            );
            suppressVanillaQuickActionConflict(
                    client.options.keySwapOffhand,
                    SKILL_KEYS.get(0)
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.gui.screen() != null) {
                return;
            }

            boolean ridingTrailStag = client.player.getVehicle() instanceof TrailStagEntity;
            for (int slot = 0; slot < SKILL_KEYS.size(); slot++) {
                KeyMapping key = SKILL_KEYS.get(slot);
                while (key.consumeClick()) {
                    if (!ridingTrailStag) {
                        ProjectSpellClientBridge.castSlot(
                                client.player,
                                slot
                        );
                    }
                }
            }
            while (DODGE_KEY.consumeClick()) {
                if (!ridingTrailStag) {
                    ProjectDodgeClientBridge.request(
                            client.player
                    );
                }
            }
            while (MOUNT_KEY.consumeClick()) {
                TrailStagMountClientBridge.request(
                        client.player
                );
            }
        });
    }

    private static void suppressVanillaQuickActionConflict(
            KeyMapping vanilla,
            KeyMapping project
    ) {
        if (!vanilla.same(project)) {
            return;
        }
        while (vanilla.consumeClick()) {
            // Project combat action owns this physical key for this tick.
        }
    }

    private static KeyMapping key(
            String translationKey,
            int glfwKey
    ) {
        return key(
                translationKey,
                glfwKey,
                CategoryHolder.COMBAT
        );
    }

    private static KeyMapping key(
            String translationKey,
            int glfwKey,
            KeyMapping.Category category
    ) {
        return new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                glfwKey,
                category
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
        private static final KeyMapping.Category TRAVERSAL =
                KeyMapping.Category.register(
                        Identifier.fromNamespaceAndPath(
                                OpenworldRpgMod.MOD_ID,
                                "traversal"
                        )
                );
    }
}
