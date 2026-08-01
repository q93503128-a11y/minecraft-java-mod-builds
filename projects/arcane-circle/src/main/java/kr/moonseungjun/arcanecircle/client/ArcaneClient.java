package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.arcanecircle.network.CastSpellPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import kr.moonseungjun.arcanecircle.network.SelectSlotPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ArcaneClient {
    private static final KeyMapping GRIMOIRE_KEY = new KeyMapping(
            "key.arcanecircle.grimoire", InputConstants.KEY_C, KeyMapping.Category.MISC);
    private static final KeyMapping CAST_KEY = new KeyMapping(
            "key.arcanecircle.cast", InputConstants.KEY_R, KeyMapping.Category.MISC);
    private static final KeyMapping PREVIOUS_KEY = new KeyMapping(
            "key.arcanecircle.previous_spell", InputConstants.KEY_Z, KeyMapping.Category.MISC);
    private static final KeyMapping NEXT_KEY = new KeyMapping(
            "key.arcanecircle.next_spell", InputConstants.KEY_X, KeyMapping.Category.MISC);

    private ArcaneClient() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GRIMOIRE_KEY);
        event.register(CAST_KEY);
        event.register(PREVIOUS_KEY);
        event.register(NEXT_KEY);
    }

    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        while (GRIMOIRE_KEY.consumeClick()) ClientPacketDistributor.sendToServer(new RequestGrimoirePayload("spells"));
        while (CAST_KEY.consumeClick()) ClientPacketDistributor.sendToServer(new CastSpellPayload(ArcaneClientState.selected()));
        while (PREVIOUS_KEY.consumeClick()) select(minecraft, -1);
        while (NEXT_KEY.consumeClick()) select(minecraft, 1);
    }

    private static void select(Minecraft minecraft, int delta) {
        int selected = Math.floorMod(ArcaneClientState.selected() + delta, 5);
        ClientPacketDistributor.sendToServer(new SelectSlotPayload(selected));
        String spellId = ArcaneClientState.slots().get(selected);
        String name = kr.moonseungjun.arcanecircle.magic.SpellCatalog.spell(spellId)
                .map(kr.moonseungjun.arcanecircle.magic.SpellDefinition::name).orElse("빈 슬롯");
        minecraft.player.sendOverlayMessage(Component.literal("§5[" + (selected + 1) + "] §f" + name));
    }
}
