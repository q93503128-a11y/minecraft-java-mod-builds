package kr.moonseungjun.turnboundre;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.turnboundre.debug.TurnboundDebugCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(TurnboundRe.MOD_ID)
public final class TurnboundRe {
    public static final String MOD_ID = "turnbound_re";
    public static final String VERSION = "0.1.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TurnboundRe(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        LOGGER.info("TURNBOUND: RE {} M0 bootstrap loaded", VERSION);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        TurnboundDebugCommands.register(event.getDispatcher());
    }
}
