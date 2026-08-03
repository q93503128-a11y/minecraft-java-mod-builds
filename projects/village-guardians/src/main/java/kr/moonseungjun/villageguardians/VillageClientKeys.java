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
    private static final KeyMapping ROLE_SKILL_ONE = key("role_skill_one", GLFW.GLFW_KEY_Z);
    private static final KeyMapping ROLE_SKILL_TWO = key("role_skill_two", GLFW.GLFW_KEY_X);
    private static final KeyMapping QUICK_COMMUNICATION = key("quick_communication", GLFW.GLFW_KEY_B);
    private static final KeyMapping STATUS = key("status", GLFW.GLFW_KEY_H);
    private static final KeyMapping GROWTH = key("personal_progress", GLFW.GLFW_KEY_J);
    private static final KeyMapping ROLE_PROGRESS = key("role_progress", GLFW.GLFW_KEY_K);
    private static boolean tickListenerRegistered;
    private static boolean legacySkillBindingsChecked;

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
        event.register(GROWTH);
        event.register(ROLE_PROGRESS);
        if (!tickListenerRegistered) {
            tickListenerRegistered = true;
            NeoForge.EVENT_BUS.addListener(VillageClientKeys::onClientTick);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            migrateLegacySkillBindings(minecraft);
        }
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.gui.screen() != null) {
            drain(ROLE_SKILL_ONE);
            drain(ROLE_SKILL_TWO);
            drain(QUICK_COMMUNICATION);
            drain(STATUS);
            drain(GROWTH);
            drain(ROLE_PROGRESS);
            return;
        }
        consume(ROLE_SKILL_ONE, "use_skill:0");
        consume(ROLE_SKILL_TWO, "use_skill:1");
        consume(QUICK_COMMUNICATION, "open_quick_chat");
        consume(STATUS, "open_status");
        consume(GROWTH, "open_skill_tree");
        consume(ROLE_PROGRESS, "open_role_progress_current");
    }

    public static String skillOneKeyName() { return keyName(ROLE_SKILL_ONE); }
    public static String skillTwoKeyName() { return keyName(ROLE_SKILL_TWO); }
    public static String quickCommunicationKeyName() { return keyName(QUICK_COMMUNICATION); }
    public static String statusKeyName() { return keyName(STATUS); }
    public static String growthKeyName() { return keyName(GROWTH); }
    public static String roleProgressKeyName() { return keyName(ROLE_PROGRESS); }

    public static String compactSummary() {
        return quickCommunicationKeyName() + " 통신 · "
                + skillOneKeyName() + "/" + skillTwoKeyName() + " 기술";
    }

    private static String keyName(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    private static void migrateLegacySkillBindings(Minecraft minecraft) {
        if (legacySkillBindingsChecked) return;
        legacySkillBindingsChecked = true;
        int first = ROLE_SKILL_ONE.getKey().getValue();
        int second = ROLE_SKILL_TWO.getKey().getValue();
        boolean oldPair = (first == GLFW.GLFW_KEY_R && second == GLFW.GLFW_KEY_G)
                || (first == GLFW.GLFW_KEY_G && second == GLFW.GLFW_KEY_R);
        if (!oldPair) return;
        ROLE_SKILL_ONE.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Z));
        ROLE_SKILL_TWO.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_X));
        KeyMapping.resetMapping();
        minecraft.options.save();
    }

    private static void drain(KeyMapping mapping) {
        while (mapping.consumeClick()) {
            // Discard clicks captured while another screen owns keyboard input.
        }
    }

    private static void consume(KeyMapping mapping, String action) {
        while (mapping.consumeClick()) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        }
    }
}
