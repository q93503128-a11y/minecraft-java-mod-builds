package io.github.q93503128.turnbound;

import com.mojang.logging.LogUtils;
import io.github.q93503128.turnbound.content.RebuildContentCatalog;
import io.github.q93503128.turnbound.session.BattleNetwork;
import io.github.q93503128.turnbound.session.MultiplayerPartyNetwork;
import io.github.q93503128.turnbound.world.TurnboundAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Content-rebuild entrypoint. Registers the reusable protocol and stable entity/attachment identities,
 * but deliberately does not activate the retired world, quest, combat encounter or player-shell runtimes.
 * Old data remains available in Git history for reconciliation, not as playable content.
 */
@Mod(Turnbound.MOD_ID)
public final class Turnbound {
    public static final String MOD_ID = "turnbound";
    public static final String VERSION = "0.1.0-alpha.17";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Turnbound(IEventBus modEventBus) {
        RebuildContentCatalog.requireEmptyProductionCatalog();
        // Preserve only generic player-profile schema and data transport identifiers.
        TurnboundAttachments.register(modEventBus);

        // Keep generic battle and party protocols, but no retired actor/item/sound registries.
        modEventBus.addListener(BattleNetwork::register);
        modEventBus.addListener(MultiplayerPartyNetwork::register);
        LOGGER.info("TURNBOUND content-rebuild foundation initialized; retired authored gameplay remains inactive");
    }
}
