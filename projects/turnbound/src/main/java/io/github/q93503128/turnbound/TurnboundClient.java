package io.github.q93503128.turnbound;

import io.github.q93503128.turnbound.client.BattleCameraController;
import io.github.q93503128.turnbound.client.BattlePlayerRenderPolicy;
import io.github.q93503128.turnbound.client.BattleStatusLayer;
import io.github.q93503128.turnbound.client.ClientAudioNetwork;
import io.github.q93503128.turnbound.client.ClientAudioPlayback;
import io.github.q93503128.turnbound.client.ClientBattleNetwork;
import io.github.q93503128.turnbound.client.ClientFieldNetwork;
import io.github.q93503128.turnbound.client.ClientMetaNetwork;
import io.github.q93503128.turnbound.client.ClientUiFeedbackLayer;
import io.github.q93503128.turnbound.client.ClientWorldLoadingBootstrap;
import io.github.q93503128.turnbound.client.FieldInteractionPromptLayer;
import io.github.q93503128.turnbound.client.FieldLocationBannerLayer;
import io.github.q93503128.turnbound.client.DrehmalMinimapLayer;
import io.github.q93503128.turnbound.client.MetaMenuKeyHandler;
import io.github.q93503128.turnbound.client.QuestGuideLayer;
import io.github.q93503128.turnbound.client.VanillaHudPolicy;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Turnbound.MOD_ID, dist = Dist.CLIENT)
public final class TurnboundClient {
    public TurnboundClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientBattleNetwork::register);
        modEventBus.addListener(ClientFieldNetwork::register);
        modEventBus.addListener(ClientMetaNetwork::register);
        modEventBus.addListener(ClientAudioNetwork::register);
        modEventBus.addListener((RegisterGuiLayersEvent event) -> {
            event.registerAboveAll(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "exploration_minimap"), new DrehmalMinimapLayer());
            event.registerAboveAll(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "location_banner"), new FieldLocationBannerLayer());
            event.registerAboveAll(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "interaction_prompt"), new FieldInteractionPromptLayer());
            event.registerAboveAll(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "quest_guide"), new QuestGuideLayer());
            event.registerAboveAll(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "battle_status"), new BattleStatusLayer());
            event.registerAboveAll(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "ui_feedback"), new ClientUiFeedbackLayer());
        });
        NeoForge.EVENT_BUS.addListener(VanillaHudPolicy::onGuiLayer);
        NeoForge.EVENT_BUS.addListener(BattleCameraController::onDetachedCameraDistance);
        NeoForge.EVENT_BUS.addListener(BattleCameraController::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(BattleCameraController::onFov);
        NeoForge.EVENT_BUS.addListener(BattlePlayerRenderPolicy::onRenderPlayer);
        NeoForge.EVENT_BUS.addListener(MetaMenuKeyHandler::onKey);
        NeoForge.EVENT_BUS.addListener(ClientWorldLoadingBootstrap::onTick);
        NeoForge.EVENT_BUS.addListener(ClientAudioPlayback::onTick);
    }
}
