package io.github.q93503128.turnbound;

import com.mojang.logging.LogUtils;
import io.github.q93503128.turnbound.command.TurnboundCommands;
import io.github.q93503128.turnbound.combat.P0Scenario;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Turnbound.MOD_ID)
public final class Turnbound {
    public static final String MOD_ID = "turnbound";
    public static final String VERSION = "0.1.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Turnbound(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(TurnboundCommands::register);
        LOGGER.info("TURNBOUND {} loaded; P0 core diagnostic: {}", VERSION, P0Scenario.runAutoDiagnostic(80));
    }
}
