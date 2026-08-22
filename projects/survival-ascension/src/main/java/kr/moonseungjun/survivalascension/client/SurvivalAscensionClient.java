package kr.moonseungjun.survivalascension.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.network.SkillNetwork;
import kr.moonseungjun.survivalascension.progress.SkillClientBridge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SurvivalAscension.MOD_ID, dist = Dist.CLIENT)
public final class SurvivalAscensionClient {
    private static final Identifier SKILL_XP_LAYER = Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "skill_xp");
    private static final KeyMapping OPEN_SKILLS = new KeyMapping(
            "key.survivalascension.skills", InputConstants.KEY_K, KeyMapping.Category.MISC);

    public SurvivalAscensionClient(IEventBus modBus) {
        SkillNetwork.installClientReceivers(ClientSkillState::onUpdate, ClientSkillState::onSnapshot);
        SkillClientBridge.install(ClientSkillState::level);
        modBus.addListener(RegisterGuiLayersEvent.class, SurvivalAscensionClient::onRegisterGuiLayers);
        modBus.addListener(RegisterKeyMappingsEvent.class, SurvivalAscensionClient::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, SurvivalAscensionClient::onClientTick);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, SKILL_XP_LAYER, SkillHudOverlay::render);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SKILLS);
    }

    private static void onClientTick(ClientTickEvent.Pre event) {
        while (OPEN_SKILLS.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.level != null) {
                minecraft.gui.setScreen(new SkillsScreen());
            }
        }
    }
}
