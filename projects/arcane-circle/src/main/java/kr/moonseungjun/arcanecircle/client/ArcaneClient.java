package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.arcanecircle.network.BeginCastPayload;
import kr.moonseungjun.arcanecircle.network.CommitFusionPayload;
import kr.moonseungjun.arcanecircle.network.QueueFusionPayload;
import kr.moonseungjun.arcanecircle.network.ReleaseCastPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ArcaneClient {
    private static final KeyMapping GRIMOIRE_KEY = new KeyMapping(
            "key.arcanecircle.grimoire", InputConstants.KEY_C, KeyMapping.Category.MISC);
    private static final KeyMapping FUSION_MODIFIER_KEY = new KeyMapping(
            "key.arcanecircle.fusion_modifier", InputConstants.KEY_X, KeyMapping.Category.MISC);
    private static final KeyMapping[] SLOT_KEYS = {
            new KeyMapping("key.arcanecircle.slot_1", InputConstants.KEY_1, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_2", InputConstants.KEY_2, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_3", InputConstants.KEY_3, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_4", InputConstants.KEY_4, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_5", InputConstants.KEY_5, KeyMapping.Category.MISC)
    };
    private static final boolean[] SLOT_WAS_DOWN = new boolean[5];
    private static boolean fusionWasDown;
    private static int protectedSelectedSlot = -1;
    private static boolean numberInputActive;

    private ArcaneClient() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GRIMOIRE_KEY);
        event.register(FUSION_MODIFIER_KEY);
        for (KeyMapping key : SLOT_KEYS) event.register(key);
    }

    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) return;
        protectedSelectedSlot = minecraft.player.getInventory().getSelectedSlot();
        numberInputActive = false;
        for (KeyMapping vanilla : minecraft.options.keyHotbarSlots) {
            numberInputActive |= vanilla.isDown();
            vanilla.setDown(false);
            while (vanilla.consumeClick()) {}
        }
    }

    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            resetInput();
            ArcaneClientState.reset();
            drainClicks();
            return;
        }
        if (numberInputActive && protectedSelectedSlot >= 0
                && minecraft.player.getInventory().getSelectedSlot() != protectedSelectedSlot) {
            minecraft.player.getInventory().setSelectedSlot(protectedSelectedSlot);
        }
        if (minecraft.gui.screen() != null) {
            while (GRIMOIRE_KEY.consumeClick()) {}
            drainSlotClicks();
            boolean hadActiveInput = fusionWasDown;
            for (int slot = 0; slot < SLOT_WAS_DOWN.length; slot++) {
                hadActiveInput |= SLOT_WAS_DOWN[slot];
                SLOT_WAS_DOWN[slot] = false;
            }
            if (hadActiveInput || FUSION_MODIFIER_KEY.isDown()) {
                ClientPacketDistributor.sendToServer(new CommitFusionPayload(1));
            }
            fusionWasDown = false;
            return;
        }
        while (GRIMOIRE_KEY.consumeClick()) {
            ClientPacketDistributor.sendToServer(new RequestGrimoirePayload("atlas"));
        }
        boolean fusionDown = FUSION_MODIFIER_KEY.isDown();
        if (!fusionWasDown && fusionDown) {
            ClientPacketDistributor.sendToServer(new CommitFusionPayload(1));
            for (int slot = 0; slot < SLOT_WAS_DOWN.length; slot++) SLOT_WAS_DOWN[slot] = false;
        }
        for (int slot = 0; slot < SLOT_KEYS.length; slot++) {
            boolean down = SLOT_KEYS[slot].isDown();
            if (down && !SLOT_WAS_DOWN[slot]) {
                if (fusionDown) ClientPacketDistributor.sendToServer(new QueueFusionPayload(slot));
                else ClientPacketDistributor.sendToServer(new BeginCastPayload(slot));
            } else if (!down && SLOT_WAS_DOWN[slot] && !fusionWasDown) {
                ClientPacketDistributor.sendToServer(new ReleaseCastPayload(slot));
            }
            SLOT_WAS_DOWN[slot] = down;
            while (SLOT_KEYS[slot].consumeClick()) {}
        }
        if (fusionWasDown && !fusionDown) {
            ClientPacketDistributor.sendToServer(new CommitFusionPayload(0));
            for (int slot = 0; slot < SLOT_WAS_DOWN.length; slot++) SLOT_WAS_DOWN[slot] = false;
        }
        fusionWasDown = fusionDown;
    }

    private static void resetInput() {
        fusionWasDown = false;
        protectedSelectedSlot = -1;
        numberInputActive = false;
        for (int slot = 0; slot < SLOT_WAS_DOWN.length; slot++) SLOT_WAS_DOWN[slot] = false;
    }
    private static void drainClicks() {
        while (GRIMOIRE_KEY.consumeClick()) {}
        drainSlotClicks();
    }
    private static void drainSlotClicks() {
        for (KeyMapping key : SLOT_KEYS) while (key.consumeClick()) {}
    }
}
