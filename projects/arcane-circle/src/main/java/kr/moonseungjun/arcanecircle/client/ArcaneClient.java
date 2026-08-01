package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.arcanecircle.network.CastSpellPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import kr.moonseungjun.arcanecircle.network.SelectSlotPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ArcaneClient {
    private static final KeyMapping GRIMOIRE_KEY = new KeyMapping(
            "key.arcanecircle.grimoire", InputConstants.KEY_C, KeyMapping.Category.MISC);
    private static final KeyMapping CAST_KEY = new KeyMapping(
            "key.arcanecircle.cast", InputConstants.KEY_R, KeyMapping.Category.MISC);
    private static final KeyMapping FUSION_KEY = new KeyMapping(
            "key.arcanecircle.fusion_cast", InputConstants.KEY_G, KeyMapping.Category.MISC);
    private static final KeyMapping PREVIOUS_FOCUS_KEY = new KeyMapping(
            "key.arcanecircle.previous_focus", InputConstants.KEY_Z, KeyMapping.Category.MISC);
    private static final KeyMapping NEXT_FOCUS_KEY = new KeyMapping(
            "key.arcanecircle.next_focus", InputConstants.KEY_X, KeyMapping.Category.MISC);
    private static final KeyMapping PREVIOUS_WEAVE_KEY = new KeyMapping(
            "key.arcanecircle.previous_weave", InputConstants.KEY_V, KeyMapping.Category.MISC);
    private static final KeyMapping NEXT_WEAVE_KEY = new KeyMapping(
            "key.arcanecircle.next_weave", InputConstants.KEY_B, KeyMapping.Category.MISC);

    private ArcaneClient() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GRIMOIRE_KEY);
        event.register(CAST_KEY);
        event.register(FUSION_KEY);
        event.register(PREVIOUS_FOCUS_KEY);
        event.register(NEXT_FOCUS_KEY);
        event.register(PREVIOUS_WEAVE_KEY);
        event.register(NEXT_WEAVE_KEY);
    }

    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        while (GRIMOIRE_KEY.consumeClick()) ClientPacketDistributor.sendToServer(new RequestGrimoirePayload("atlas"));
        while (CAST_KEY.consumeClick()) ClientPacketDistributor.sendToServer(new CastSpellPayload(0));
        while (FUSION_KEY.consumeClick()) ClientPacketDistributor.sendToServer(new CastSpellPayload(1));
        while (PREVIOUS_FOCUS_KEY.consumeClick()) ClientPacketDistributor.sendToServer(new SelectSlotPayload(0));
        while (NEXT_FOCUS_KEY.consumeClick()) ClientPacketDistributor.sendToServer(new SelectSlotPayload(1));
        while (PREVIOUS_WEAVE_KEY.consumeClick()) ClientPacketDistributor.sendToServer(new SelectSlotPayload(2));
        while (NEXT_WEAVE_KEY.consumeClick()) ClientPacketDistributor.sendToServer(new SelectSlotPayload(3));
    }
}
