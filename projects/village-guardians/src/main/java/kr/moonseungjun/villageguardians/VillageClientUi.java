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
                (rawPayload, context) -> {
                    VillageNetwork.OpenVillageUiPayload payload = resolve(rawPayload);
                    Minecraft.getInstance().gui.setScreen(
                            switch (payload.screenId()) {
                                case "skill_tree" -> new VillageSkillTreeScreen(payload);
                                case "role_progress", "role_skills" -> new VillageRoleProgressScreen(payload);
                                case "quick_chat" -> new VillageQuickChatSafeScreen(payload);
                                case "town_hall" -> new VillageTownHallGridScreen(payload);
                                case "equipment_shop" -> new VillageShopCatalogScreen(payload);
                                case "status", "caller" -> new VillageCommandCenterScreen(payload);
                                case "building", "management", "funding", "tower_control", "tower_detail", "skill_test" ->
                                        new VillageActionDetailScreen(payload);
                                case "relic_collection" -> new VillageRelicScreen(payload);
                                case "relic_choice" -> new VillageRelicChoiceConfirmScreen(payload);
                                case "wave_intel" -> new VillageWaveIntelDossierScreen(payload);
                                case "victory" -> new VillageVictoryScreen(payload);
                                case "game_over" -> new VillageGameOverScreen(payload);
                                case "skill_test_role", "skill_test_skill" -> new VillageSkillTestScreen(payload);
                                case "skill_test_password" -> new VillageSkillTestPasswordScreen(payload);
                                case "equipment_fusion" -> new VillageFusionSafeScreen(payload);
                                case "result" -> new VillageResultScreen(payload);
                                default -> new VillageActionDetailScreen(payload);
                            });
                });
        event.register(VillageNetwork.SkillMotionPayload.TYPE,
                (payload, context) -> VillageSkillEffectClient.acceptMotion(payload));
        event.register(VillageNetwork.SkillHudPayload.TYPE,
                (payload, context) -> VillageSkillHudOverlay.accept(payload));
        event.register(VillageNetwork.MainHudPayload.TYPE,
                (payload, context) -> VillageMainHudOverlay.accept(payload));
        event.register(VillageNetwork.PlayerStatusPayload.TYPE,
                (payload, context) -> VillageInventoryPanel.updateStatus(payload));
    }

    private static VillageNetwork.OpenVillageUiPayload resolve(
            VillageNetwork.OpenVillageUiPayload payload) {
        return new VillageNetwork.OpenVillageUiPayload(
                payload.screenId(),
                VillageClientKeys.resolveTokens(payload.title()),
                VillageClientKeys.resolveTokens(payload.body()),
                payload.actions(),
                VillageClientKeys.resolveTokens(payload.labels()));
    }
}
