package io.github.q93503128.turnbound;

import com.mojang.logging.LogUtils;
import io.github.q93503128.turnbound.command.TurnboundCommands;
import io.github.q93503128.turnbound.presentation.DrabyelServiceActors;
import io.github.q93503128.turnbound.presentation.SignatureBattleActors;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import io.github.q93503128.turnbound.presentation.TurnboundVisualItems;
import io.github.q93503128.turnbound.session.BattleInteractionGuard;
import io.github.q93503128.turnbound.session.BattleNetwork;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import io.github.q93503128.turnbound.world.CampaignPersistence;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import io.github.q93503128.turnbound.world.ExternalWorldBootstrap;
import io.github.q93503128.turnbound.world.DrehmalHubEntityPolicy;
import io.github.q93503128.turnbound.world.FieldInteractionGuard;
import io.github.q93503128.turnbound.world.FieldNetwork;
import io.github.q93503128.turnbound.world.GachaPresentationActorService;
import io.github.q93503128.turnbound.world.MetaNetwork;
import io.github.q93503128.turnbound.world.PlayerShellRules;
import io.github.q93503128.turnbound.world.TurnboundAttachments;
import io.github.q93503128.turnbound.world.WorldSessionRouter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(Turnbound.MOD_ID)
public final class Turnbound {
    public static final String MOD_ID = "turnbound";
    public static final String VERSION = "0.1.0-alpha.17";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Turnbound(IEventBus modEventBus) {
        TurnboundAttachments.register(modEventBus);
        TurnboundSounds.SOUND_EVENTS.register(modEventBus);
        TurnboundVisualItems.register(modEventBus);
        TurnboundBattleActors.register(modEventBus);
        SignatureBattleActors.register(modEventBus);
        DrabyelServiceActors.register(modEventBus);
        modEventBus.addListener(BattleNetwork::register);
        modEventBus.addListener(FieldNetwork::register);
        modEventBus.addListener(MetaNetwork::register);
        NeoForge.EVENT_BUS.addListener(TurnboundCommands::register);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::login);
        NeoForge.EVENT_BUS.addListener(this::logout);
        NeoForge.EVENT_BUS.addListener(this::serverStopping);
        NeoForge.EVENT_BUS.addListener(PlayerShellRules::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(DrehmalHubEntityPolicy::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(BattleInteractionGuard::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(BattleInteractionGuard::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(BattleInteractionGuard::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(BattleInteractionGuard::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(BattleInteractionGuard::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(FieldInteractionGuard::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(FieldInteractionGuard::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(FieldInteractionGuard::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(FieldInteractionGuard::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(FieldInteractionGuard::onAttackEntity);
    }

    private void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GachaPresentationActorService.tick(player);
        if (CampaignPersistence.blocked(player)) return;

        // Production gameplay is fail-closed onto the selected authored world. Installing TURNBOUND in an arbitrary
        // save must never rebuild Aster March, erase terrain, cancel native spawns or apply the RPG player shell.
        if (!ExternalWorldBootstrap.tick(player)) {
            CampaignPersistence.autosave(player);
            return;
        }

        PlayerShellRules.maintain(player);
        BattleSessionManager.tick(player);
        CampaignPersistence.autosave(player);
    }

    private void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (CampaignProgressStore.hasRuntime(player.getUUID())) {
            if (BattleSessionManager.resumeIfPresent(player)) {
                LOGGER.warn("TURNBOUND resumed retained in-memory battle state for {} after a failed lifecycle flush", player.getUUID());
            } else {
                LOGGER.warn("TURNBOUND resumed retained in-memory campaign state for {} after a failed lifecycle flush", player.getUUID());
                ExternalWorldBootstrap.initialize(player);
            }
            return;
        }
        CampaignPersistence.load(player);
        ExternalWorldBootstrap.initialize(player);
    }

    private void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GachaPresentationActorService.finish(player);
        boolean releaseRuntime = true;
        if (!CampaignPersistence.blocked(player)) {
            releaseRuntime = BattleSessionManager.endForLifecycle(player);
            if (releaseRuntime) releaseRuntime = CampaignPersistence.saveIfDirtyForLifecycle(player);
        }
        if (releaseRuntime || CampaignPersistence.blocked(player)) {
            CampaignProgressStore.removeRuntime(player.getUUID());
            ExternalWorldBootstrap.remove(player);
            WorldSessionRouter.remove(player);
        } else {
            LOGGER.error("TURNBOUND retained unsaved in-memory state for {} so a same-server reconnect can retry persistence", player.getUUID());
        }
        CampaignPersistence.forget(player);
    }

    private void serverStopping(ServerStoppingEvent event) {
        var players = event.getServer().getPlayerList().getPlayers();
        GachaPresentationActorService.clearAll();
        BattleSessionManager.clearAll(players);
        for (ServerPlayer player : players) {
            if (!CampaignPersistence.saveIfDirtyForLifecycle(player)) {
                LOGGER.error("TURNBOUND could not flush campaign state for {} before server shutdown", player.getUUID());
            }
        }
        ExternalWorldBootstrap.clear();
        WorldSessionRouter.clearAll(players);
        CampaignProgressStore.clearRuntime();
    }
}
