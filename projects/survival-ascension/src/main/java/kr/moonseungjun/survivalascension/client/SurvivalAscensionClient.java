package kr.moonseungjun.survivalascension.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.network.MobilityActionPayload;
import kr.moonseungjun.survivalascension.network.SkillNetwork;
import kr.moonseungjun.survivalascension.progress.SkillClientBridge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.List;

@Mod(value = SurvivalAscension.MOD_ID, dist = Dist.CLIENT)
public final class SurvivalAscensionClient {
    private static final Identifier SKILL_XP_LAYER = Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "skill_xp");
    private static final List<String> KOREAN_EXTERNAL_CONTENT_PACKS = List.of(
            "korean_tbos_1",
            "korean_tbos_2",
            "korean_tbos_3",
            "korean_tbos_4",
            "korean_tbos_5",
            "korean_tbos_6",
            "korean_external_misc");
    private static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.survivalascension.menu", InputConstants.KEY_M, KeyMapping.Category.MISC);
    private static final KeyMapping MOBILITY_ACTION = new KeyMapping(
            "key.survivalascension.mobility_action", InputConstants.KEY_R, KeyMapping.Category.MISC);

    public SurvivalAscensionClient(IEventBus modBus) {
        SkillNetwork.installClientReceivers(ClientSkillState::onUpdate, ClientSkillState::onSnapshot);
        SkillClientBridge.install(ClientSkillState::level);
        modBus.addListener(RegisterGuiLayersEvent.class, SurvivalAscensionClient::onRegisterGuiLayers);
        modBus.addListener(RegisterKeyMappingsEvent.class, SurvivalAscensionClient::onRegisterKeyMappings);
        modBus.addListener(AddPackFindersEvent.class, SurvivalAscensionClient::onAddPackFinders);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, SurvivalAscensionClient::onClientTick);
    }

    private static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        for (String packName : KOREAN_EXTERNAL_CONTENT_PACKS) {
            event.addPackFinders(
                    Identifier.fromNamespaceAndPath(
                            SurvivalAscension.MOD_ID, "resourcepacks/" + packName),
                    PackType.CLIENT_RESOURCES,
                    Component.literal("Survival Ascension 한국어 외부 콘텐츠"),
                    PackSource.BUILT_IN,
                    true,
                    Pack.Position.TOP);
        }
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, SKILL_XP_LAYER, SkillHudOverlay::render);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
        event.register(MOBILITY_ACTION);
    }

    private static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_MENU.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null) continue;
            Screen current = minecraft.gui.screen();
            if (current instanceof AscensionRadialMenuScreen
                    || current instanceof ConstructionRadialMenuScreen
                    || current instanceof MiningRadialMenuScreen
                    || current instanceof EquipmentRadialMenuScreen
                    || current instanceof InfrastructureRadialMenuScreen
                    || current instanceof ProductionRadialMenuScreen) {
                minecraft.gui.setScreen(null);
            } else if (current == null || current instanceof SkillsScreen || current instanceof GuideScreen) {
                minecraft.gui.setScreen(new AscensionRadialMenuScreen());
            }
        }
        while (MOBILITY_ACTION.consumeClick()) {
            if (minecraft.player != null && minecraft.level != null && minecraft.gui.screen() == null) {
                ClientPacketDistributor.sendToServer(new MobilityActionPayload());
            }
        }
    }
}
