package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.network.SkillNetwork;
import kr.moonseungjun.survivalascension.progress.SkillClientBridge;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@Mod(value = SurvivalAscension.MOD_ID, dist = Dist.CLIENT)
public final class SurvivalAscensionClient {
    private static final Identifier SKILL_XP_LAYER = Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "skill_xp");
    public SurvivalAscensionClient(IEventBus modBus) {
        SkillNetwork.installClientReceivers(ClientSkillState::onUpdate, ClientSkillState::onSnapshot);
        SkillClientBridge.install(ClientSkillState::level);
        modBus.addListener(RegisterGuiLayersEvent.class, SurvivalAscensionClient::onRegisterGuiLayers);
    }
    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, SKILL_XP_LAYER, SkillHudOverlay::render);
    }
}
