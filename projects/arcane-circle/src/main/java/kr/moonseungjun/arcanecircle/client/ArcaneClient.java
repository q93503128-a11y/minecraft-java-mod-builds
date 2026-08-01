package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.arcanecircle.network.CastSpellPayload;
import kr.moonseungjun.arcanecircle.network.CommitFusionPayload;
import kr.moonseungjun.arcanecircle.network.QueueFusionPayload;
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

    private static boolean fusionWasDown;
    private static int stableHotbarSlot = -1;

    private ArcaneClient() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GRIMOIRE_KEY);
        event.register(FUSION_MODIFIER_KEY);
        for (KeyMapping key : SLOT_KEYS) event.register(key);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            fusionWasDown = false;
            stableHotbarSlot = -1;
            ArcaneClientState.reset();
            drainClicks();
            return;
        }

        int currentHotbar = minecraft.player.getInventory().getSelectedSlot();
        if (stableHotbarSlot < 0 || stableHotbarSlot >= 9) stableHotbarSlot = currentHotbar;

        if (minecraft.gui.screen() != null) {
            while (GRIMOIRE_KEY.consumeClick()) {}
            drainSlotClicks();
            if (fusionWasDown || FUSION_MODIFIER_KEY.isDown()) {
                ClientPacketDistributor.sendToServer(new CommitFusionPayload(1));
            }
            fusionWasDown = false;
            stableHotbarSlot = currentHotbar;
            return;
        }

        while (GRIMOIRE_KEY.consumeClick()) {
            ClientPacketDistributor.sendToServer(new RequestGrimoirePayload("atlas"));
        }

        boolean fusionDown = FUSION_MODIFIER_KEY.isDown();
        boolean usedSpellKey = false;
        for (int slot = 0; slot < SLOT_KEYS.length; slot++) {
            while (SLOT_KEYS[slot].consumeClick()) {
                usedSpellKey = true;
                if (fusionDown) ClientPacketDistributor.sendToServer(new QueueFusionPayload(slot));
                else ClientPacketDistributor.sendToServer(new CastSpellPayload(slot));
            }
        }

        if (usedSpellKey) {
            minecraft.player.getInventory().setSelectedSlot(stableHotbarSlot);
        } else {
            stableHotbarSlot = currentHotbar;
        }

        if (fusionWasDown && !fusionDown) {
            ClientPacketDistributor.sendToServer(new CommitFusionPayload(0));
        }
        fusionWasDown = fusionDown;
    }

    private static void drainClicks() {
        while (GRIMOIRE_KEY.consumeClick()) {}
        drainSlotClicks();
    }

    private static void drainSlotClicks() {
        for (KeyMapping key : SLOT_KEYS) while (key.consumeClick()) {}
    }
}
