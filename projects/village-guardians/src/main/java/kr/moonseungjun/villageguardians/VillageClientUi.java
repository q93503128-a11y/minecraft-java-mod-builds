package kr.moonseungjun.villageguardians;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageClientUi {
    private VillageClientUi() {
    }

    @SubscribeEvent
    public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(VillageNetwork.OpenVillageUiPayload.TYPE,
                (payload, context) -> Minecraft.getInstance().gui.setScreen(
                        payload.screenId().equals("skill_tree")
                                ? new VillageSkillTreeScreen(payload)
                                : new VillageUiScreen(payload)));
        event.register(VillageNetwork.PlayerStatusPayload.TYPE,
                (payload, context) -> VillageInventoryPanel.updateStatus(payload));
    }
}
