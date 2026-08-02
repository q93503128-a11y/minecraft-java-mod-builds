package kr.moonseungjun.villageguardians;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageClientUi {
    private VillageClientUi() {}

    @SubscribeEvent
    public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(VillageNetwork.OpenVillageUiPayload.TYPE,
                (payload, context) -> Minecraft.getInstance().gui.setScreen(
                        switch (payload.screenId()) {
                            case "skill_tree" -> new VillageSkillTreeScreen(payload);
                            case "town_hall" -> new VillageTownHallScreen(payload);
                            case "role_progress", "role_skills" -> new VillageRoleProgressScreen(payload);
                            case "quick_chat" -> new VillageQuickChatScreen(payload);
                            case "status", "wave_intel" -> new VillageStatusScreen(payload);
                            case "equipment_shop" -> new VillageShopScreen(payload);
                            case "equipment_fusion" -> new VillageFusionScreen(payload);
                            case "result" -> new VillageResultScreen(payload);
                            case "building", "management", "funding", "tower_control", "tower_detail", "caller", "relic_choice" ->
                                    new VillageFacilityScreen(payload);
                            default -> new VillageUiScreen(payload);
                        }));
        event.register(VillageNetwork.PlayerStatusPayload.TYPE,
                (payload, context) -> VillageInventoryPanel.updateStatus(payload));
    }
}
