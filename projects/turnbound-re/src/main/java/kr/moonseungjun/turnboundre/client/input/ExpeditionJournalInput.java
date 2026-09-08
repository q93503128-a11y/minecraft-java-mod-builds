package kr.moonseungjun.turnboundre.client.input;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.ExpeditionJournalClientState;
import kr.moonseungjun.turnboundre.client.ui.ExpeditionJournalScreen;
import kr.moonseungjun.turnboundre.network.ExpeditionNetworkPayloads;
import kr.moonseungjun.turnboundre.world.ExpeditionJournalAccess;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Opens the Expedition Journal by using the physical journal item; no TURNBOUND key binding is registered. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class ExpeditionJournalInput {
    private ExpeditionJournalInput() {}

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (!ExpeditionJournalAccess.isJournal(minecraft.player.getItemInHand(event.getHand()))) return;

        event.setCanceled(true);
        event.setSwingHand(false);
        ExpeditionJournalClientState.clear();
        minecraft.gui.setScreen(new ExpeditionJournalScreen());
        ClientPacketDistributor.sendToServer(new ExpeditionNetworkPayloads.RequestJournalC2S());
    }
}
