package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.Set;

/**
 * Production entry point for TURNBOUND: RE's external authored-world base.
 *
 * <p>Production no longer generates a TURNBOUND-authored replacement Hub/Region on first login. The selected
 * external world must be installed separately and bound through {@link DrehmalExternalWorldBinding}. Once its
 * server-owned anchors exist, per-player Hub discovery remains the onboarding marker.</p>
 *
 * <p>The old generated world builders remain operator-only functional harnesses. They must never silently replace
 * a missing external production world.</p>
 */
public final class AuthoredWorldBootstrapService {
    private static final float HUB_FACING_YAW = -90.0F;
    private final DefinitionRepository definitions;
    private final FirstExpeditionQuestService firstExpedition;

    public AuthoredWorldBootstrapService(
            DefinitionRepository definitions,
            FirstExpeditionQuestService firstExpedition
    ) {
        if (definitions == null || firstExpedition == null) {
            throw new IllegalArgumentException("definitions/firstExpedition required");
        }
        this.definitions = definitions;
        this.firstExpedition = firstExpedition;
    }

    public void register(IEventBus bus) {
        if (bus == null) throw new IllegalArgumentException("bus required");
        bus.addListener(this::onPlayerLoggedIn);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        initialize(player);
    }

    public void initialize(ServerPlayer player) {
        if (player == null) throw new IllegalArgumentException("player required");
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        DefinitionRegistry registry = definitions.snapshot().registry();
        if (registry.regions().get(FunctionalWorldSliceLayout.HUB_ID) == null
                || registry.regions().get(FunctionalWorldSliceLayout.REGION_ID) == null) {
            TurnboundRe.LOGGER.error("TURNBOUND external-world bootstrap skipped because region definitions are unavailable");
            return;
        }

        FastTravelSavedData saved = FastTravelSavedData.get(server);
        if (needsWorldInstall(saved.anchorsSnapshot())) {
            TurnboundRe.LOGGER.warn(
                    "TURNBOUND production world is not bound. Install the official external world and bind it; "
                            + "the mod will not generate replacement Hub/Region geometry automatically.");
            return;
        }

        if (needsInitialHubArrival(saved.discovered(player.getUUID()))) {
            placeNewPlayerAtHub(player, server, saved);
        }
    }

    /** Kept as the stable contract name: true now means external-world binding is missing, not that blocks should be built. */
    static boolean needsWorldInstall(Map<String, FastTravelSavedData.AnchorLocation> anchors) {
        if (anchors == null) return true;
        return !anchors.containsKey(WorldFastTravelPrototype.HUB_LOCATOR)
                || !anchors.containsKey(WorldFastTravelPrototype.REGION_LOCATOR);
    }

    static boolean needsInitialHubArrival(Set<String> discoveredLocators) {
        return discoveredLocators == null || !discoveredLocators.contains(WorldFastTravelPrototype.HUB_LOCATOR);
    }

    private void placeNewPlayerAtHub(
            ServerPlayer player,
            MinecraftServer server,
            FastTravelSavedData saved
    ) {
        FastTravelSavedData.AnchorLocation hub = saved.anchor(WorldFastTravelPrototype.HUB_LOCATOR).orElse(null);
        if (hub == null || !FunctionalWorldSliceLayout.DIMENSION.equals(hub.dimension())) return;

        ServerLevel overworld = server.overworld();
        overworld.getChunkAt(hub.blockPos());
        saved.discover(player.getUUID(), WorldFastTravelPrototype.HUB_LOCATOR);

        player.stopRiding();
        player.teleportTo(
                overworld,
                hub.x() + 0.5D,
                hub.y(),
                hub.z() + 0.5D,
                Set.of(),
                HUB_FACING_YAW,
                0.0F,
                false);
        firstExpedition.onInitialHubArrival(player);
    }
}
