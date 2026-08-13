package kr.moonseungjun.senbonzakura.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.senbonzakura.ability.ShowcaseAbility;
import kr.moonseungjun.senbonzakura.network.BankaiVisualPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed 10-slot ability controls.
 *
 * Abilities no longer consume one permanent keyboard key each. Any number of future abilities
 * can live in the library while combat input stays at ten Shift+number slots.
 */
public final class AbilityKeyHandler {
    public static final KeyMapping OPEN_SKILL_INVENTORY = new KeyMapping(
            "key.senbonzakura.skill_inventory",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            KeyMapping.Category.MISC);

    private static final List<KeyMapping> SLOT_KEYS = new ArrayList<>(SkillLoadout.SLOT_COUNT);

    static {
        slotKey(1, GLFW.GLFW_KEY_1);
        slotKey(2, GLFW.GLFW_KEY_2);
        slotKey(3, GLFW.GLFW_KEY_3);
        slotKey(4, GLFW.GLFW_KEY_4);
        slotKey(5, GLFW.GLFW_KEY_5);
        slotKey(6, GLFW.GLFW_KEY_6);
        slotKey(7, GLFW.GLFW_KEY_7);
        slotKey(8, GLFW.GLFW_KEY_8);
        slotKey(9, GLFW.GLFW_KEY_9);
        slotKey(10, GLFW.GLFW_KEY_0);
    }

    private AbilityKeyHandler() {}

    private static void slotKey(int humanSlot, int glfwKey) {
        SLOT_KEYS.add(new KeyMapping(
                "key.senbonzakura.skill_slot_" + humanSlot,
                KeyConflictContext.IN_GAME,
                KeyModifier.SHIFT,
                InputConstants.Type.KEYSYM,
                glfwKey,
                KeyMapping.Category.MISC));
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SKILL_INVENTORY);
        SLOT_KEYS.forEach(event::register);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        while (OPEN_SKILL_INVENTORY.consumeClick()) {
            if (minecraft.player != null) {
                minecraft.gui.setScreen(new SkillInventoryScreen());
            }
        }

        for (int slot = 0; slot < SLOT_KEYS.size(); slot++) {
            KeyMapping mapping = SLOT_KEYS.get(slot);
            while (mapping.consumeClick()) {
                ShowcaseAbility ability = SkillLoadout.get(slot);
                if (ability == null || minecraft.player == null) continue;
                ClientPacketDistributor.sendToServer(new BankaiVisualPayload(
                        "action=ability_request;ability=" + ability.id()));
            }
        }
    }
}
