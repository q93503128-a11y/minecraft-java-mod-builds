package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.arcanecircle.network.BeginCastPayload;
import kr.moonseungjun.arcanecircle.network.CommitFusionPayload;
import kr.moonseungjun.arcanecircle.network.QueueFusionPayload;
import kr.moonseungjun.arcanecircle.network.ReleaseCastPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import kr.moonseungjun.arcanecircle.network.UseArcaneAbilityPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Arrays;

public final class ArcaneClient {
    private static final KeyMapping GRIMOIRE_KEY = new KeyMapping(
            "key.arcanecircle.grimoire", InputConstants.KEY_C, KeyMapping.Category.MISC);
    /** Secondary authority of maintained high-circle spells (Control Weather first). */
    private static final KeyMapping ARCANE_ABILITY_KEY = new KeyMapping(
            "key.arcanecircle.arcane_ability", InputConstants.KEY_G, KeyMapping.Category.MISC);
    private static final KeyMapping[] SLOT_KEYS = {
            new KeyMapping("key.arcanecircle.slot_1", InputConstants.KEY_1, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_2", InputConstants.KEY_2, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_3", InputConstants.KEY_3, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_4", InputConstants.KEY_4, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_5", InputConstants.KEY_5, KeyMapping.Category.MISC)
    };
    private static final boolean[] SLOT_WAS_DOWN = new boolean[5];
    private static final boolean[] FUSION_QUEUED = new boolean[5];
    private static int primarySlot = -1;
    private static boolean fusionChord;
    private static int protectedSelectedSlot = -1;
    private static boolean numberInputActive;

    private ArcaneClient() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GRIMOIRE_KEY);
        event.register(ARCANE_ABILITY_KEY);
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
            while (ARCANE_ABILITY_KEY.consumeClick()) {}
            drainSlotClicks();
            if (primarySlot >= 0 || fusionChord) {
                ClientPacketDistributor.sendToServer(new CommitFusionPayload(1));
            }
            resetCastChord();
            return;
        }
        while (GRIMOIRE_KEY.consumeClick()) {
            ClientPacketDistributor.sendToServer(new RequestGrimoirePayload("atlas"));
        }
        while (ARCANE_ABILITY_KEY.consumeClick()) {
            ClientPacketDistributor.sendToServer(new UseArcaneAbilityPayload(0));
        }

        boolean[] down = new boolean[SLOT_KEYS.length];
        boolean[] pressed = new boolean[SLOT_KEYS.length];
        for (int slot = 0; slot < SLOT_KEYS.length; slot++) {
            down[slot] = SLOT_KEYS[slot].isDown();
            boolean clicked = false;
            while (SLOT_KEYS[slot].consumeClick()) clicked = true;
            pressed[slot] = clicked || (down[slot] && !SLOT_WAS_DOWN[slot]);
        }

        for (int slot = 0; slot < SLOT_KEYS.length; slot++) {
            if (!pressed[slot]) continue;
            if (primarySlot < 0) {
                primarySlot = slot;
                fusionChord = false;
                Arrays.fill(FUSION_QUEUED, false);
                ClientPacketDistributor.sendToServer(new BeginCastPayload(slot));
            } else if (slot != primarySlot) {
                if (!fusionChord) {
                    ClientPacketDistributor.sendToServer(new QueueFusionPayload(primarySlot));
                    FUSION_QUEUED[primarySlot] = true;
                    fusionChord = true;
                }
                if (!FUSION_QUEUED[slot]) {
                    ClientPacketDistributor.sendToServer(new QueueFusionPayload(slot));
                    FUSION_QUEUED[slot] = true;
                }
            }
        }

        boolean primaryReleased = primarySlot >= 0 && !down[primarySlot]
                && (SLOT_WAS_DOWN[primarySlot] || pressed[primarySlot]);
        if (primaryReleased) {
            if (fusionChord) ClientPacketDistributor.sendToServer(new CommitFusionPayload(0));
            else ClientPacketDistributor.sendToServer(new ReleaseCastPayload(primarySlot));
            resetCastChord();
        }

        for (int slot = 0; slot < SLOT_KEYS.length; slot++) SLOT_WAS_DOWN[slot] = down[slot];
    }

    private static void resetCastChord() {
        primarySlot = -1;
        fusionChord = false;
        Arrays.fill(FUSION_QUEUED, false);
    }

    private static void resetInput() {
        resetCastChord();
        protectedSelectedSlot = -1;
        numberInputActive = false;
        Arrays.fill(SLOT_WAS_DOWN, false);
    }

    private static void drainClicks() {
        while (GRIMOIRE_KEY.consumeClick()) {}
        while (ARCANE_ABILITY_KEY.consumeClick()) {}
        drainSlotClicks();
    }

    private static void drainSlotClicks() {
        for (KeyMapping key : SLOT_KEYS) while (key.consumeClick()) {}
    }
}
