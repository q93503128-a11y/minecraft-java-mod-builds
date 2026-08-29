package io.github.q93503128.turnbound;

import com.mojang.logging.LogUtils;
import io.github.q93503128.turnbound.command.TurnboundCommands;
import io.github.q93503128.turnbound.combat.P0Scenario;
import io.github.q93503128.turnbound.session.BattleInteractionGuard;
import io.github.q93503128.turnbound.session.BattleNetwork;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import io.github.q93503128.turnbound.world.PlayerShellRules;
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
    public static final String VERSION = "0.1.0-alpha.7";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Turnbound(IEventBus modEventBus) {
        modEventBus.addListener(BattleNetwork::register);
        NeoForge.EVENT_BUS.addListener(TurnboundCommands::register);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::logout);
        NeoForge.EVENT_BUS.addListener(this::serverStopping);
        NeoForge.EVENT_BUS.addListener(PlayerShellRules::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(BattleInteractionGuard::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(BattleInteractionGuard::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(BattleInteractionGuard::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(BattleInteractionGuard::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(BattleInteractionGuard::onAttackEntity);
        LOGGER.info("TURNBOUND {} loaded; {}", VERSION, P0Scenario.runAutoDiagnostic(160));
    }

    private void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerShellRules.maintain(player);
            BattleSessionManager.tick(player);
        }
    }

    private void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BattleSessionManager.end(player);
    }

    private void serverStopping(ServerStoppingEvent event) {
        BattleSessionManager.clearAll(event.getServer().getPlayerList().getPlayers());
    }
}