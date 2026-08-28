package io.github.q93503128.turnbound;

import com.mojang.logging.LogUtils;
import io.github.q93503128.turnbound.command.TurnboundCommands;
import io.github.q93503128.turnbound.combat.P0Scenario;
import io.github.q93503128.turnbound.session.BattleNetwork;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(Turnbound.MOD_ID)
public final class Turnbound {
    public static final String MOD_ID="turnbound"; public static final String VERSION="0.1.0-alpha.2"; public static final Logger LOGGER= LogUtils.getLogger();
    public Turnbound(IEventBus modEventBus){ modEventBus.addListener(BattleNetwork::register); NeoForge.EVENT_BUS.addListener(TurnboundCommands::register); NeoForge.EVENT_BUS.addListener(this::tick); NeoForge.EVENT_BUS.addListener(this::logout); LOGGER.info("TURNBOUND {} loaded; {}",VERSION,P0Scenario.runAutoDiagnostic(160)); }
    private void tick(PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p)BattleSessionManager.tick(p);}
    private void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)BattleSessionManager.end(p);}
}
