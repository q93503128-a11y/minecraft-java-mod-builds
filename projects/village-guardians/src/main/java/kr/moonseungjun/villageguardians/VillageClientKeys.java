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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageClientKeys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "controls"));

    private static final KeyMapping ROLE_SKILL_ONE = key("role_skill_one", GLFW.GLFW_KEY_Z);
    private static final KeyMapping ROLE_SKILL_TWO = key("role_skill_two", GLFW.GLFW_KEY_X);
    private static final KeyMapping QUICK_COMMUNICATION = key("quick_communication", GLFW.GLFW_KEY_V);
    private static final KeyMapping STATUS = key("status", GLFW.GLFW_KEY_H);
    private static final KeyMapping GROWTH = key("personal_progress", GLFW.GLFW_KEY_J);
    private static final KeyMapping ROLE_PROGRESS = key("role_progress", GLFW.GLFW_KEY_K);

    // X is deliberately omitted: vanilla's toolbar save/restore needs X + number,
    // while this mod consumes the standalone X click for skill slot 2.
    private static final Set<Integer> VANILLA_RESERVED = Set.of(
            GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D,
            GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_LEFT_CONTROL,
            GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_T,
            GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_C,
            GLFW.GLFW_KEY_SLASH, GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_ENTER,
            GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F2,
            GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_F4, GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_F11,
            GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3,
            GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7,
            GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9);

    private static boolean tickListenerRegistered;
    private static boolean bindingsChecked;
    private static boolean skillTwoPending;
    private static boolean skillTwoToolbarChord;

    private VillageClientKeys() {}

    private static KeyMapping key(String id, int key) {
        return new KeyMapping("key.villageguardians." + id, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping mapping : mappings()) event.register(mapping);
        if (!tickListenerRegistered) {
            tickListenerRegistered = true;
            NeoForge.EVENT_BUS.addListener(VillageClientKeys::onClientTick);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) migrateBindings(minecraft);
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.gui.screen() != null) {
            for (KeyMapping mapping : mappings()) drain(mapping);
            skillTwoPending = false;
            skillTwoToolbarChord = false;
            return;
        }
        consume(ROLE_SKILL_ONE, "use_skill:0");
        consumeSkillTwo(minecraft);
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

    public static String resolveTokens(String value) {
        if (value == null || value.isEmpty()) return value == null ? "" : value;
        return value
                .replace("{SKILL1}", skillOneKeyName())
                .replace("{SKILL2}", skillTwoKeyName())
                .replace("{QUICK}", quickCommunicationKeyName())
                .replace("{STATUS}", statusKeyName())
                .replace("{GROWTH}", growthKeyName())
                .replace("{ROLE}", roleProgressKeyName());
    }

    private static List<KeyMapping> mappings() {
        return List.of(ROLE_SKILL_ONE, ROLE_SKILL_TWO, QUICK_COMMUNICATION,
                STATUS, GROWTH, ROLE_PROGRESS);
    }

    private static String keyName(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    private static void migrateBindings(Minecraft minecraft) {
        if (bindingsChecked) return;
        bindingsChecked = true;

        boolean oldDefaults = keyValue(ROLE_SKILL_ONE) == GLFW.GLFW_KEY_Z
                && keyValue(ROLE_SKILL_TWO) == GLFW.GLFW_KEY_V
                && keyValue(QUICK_COMMUNICATION) == GLFW.GLFW_KEY_B
                && keyValue(STATUS) == GLFW.GLFW_KEY_H
                && keyValue(GROWTH) == GLFW.GLFW_KEY_J
                && keyValue(ROLE_PROGRESS) == GLFW.GLFW_KEY_K;

        Set<Integer> used = new HashSet<>();
        boolean unsafe = false;
        for (KeyMapping mapping : mappings()) {
            int value = keyValue(mapping);
            if (value <= 0 || VANILLA_RESERVED.contains(value) || !used.add(value)) unsafe = true;
        }
        if (!oldDefaults && !unsafe) return;

        set(ROLE_SKILL_ONE, GLFW.GLFW_KEY_Z);
        set(ROLE_SKILL_TWO, GLFW.GLFW_KEY_X);
        set(QUICK_COMMUNICATION, GLFW.GLFW_KEY_V);
        set(STATUS, GLFW.GLFW_KEY_H);
        set(GROWTH, GLFW.GLFW_KEY_J);
        set(ROLE_PROGRESS, GLFW.GLFW_KEY_K);
        KeyMapping.resetMapping();
        minecraft.options.save();
    }

    private static int keyValue(KeyMapping mapping) { return mapping.getKey().getValue(); }

    private static void set(KeyMapping mapping, int key) {
        mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
    }

    private static void consumeSkillTwo(Minecraft minecraft) {
        while (ROLE_SKILL_TWO.consumeClick()) {
            skillTwoPending = true;
            skillTwoToolbarChord = false;
        }
        if (!skillTwoPending) return;
        for (KeyMapping hotbar : minecraft.options.keyHotbarSlots) {
            if (hotbar.isDown()) {
                skillTwoToolbarChord = true;
                break;
            }
        }
        if (ROLE_SKILL_TWO.isDown()) return;
        if (!skillTwoToolbarChord) {
            ClientPacketDistributor.sendToServer(
                    new VillageNetwork.VillageUiActionPayload("use_skill:1"));
        }
        skillTwoPending = false;
        skillTwoToolbarChord = false;
    }

    private static void drain(KeyMapping mapping) {
        while (mapping.consumeClick()) { }
    }

    private static void consume(KeyMapping mapping, String action) {
        while (mapping.consumeClick()) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        }
    }
}
