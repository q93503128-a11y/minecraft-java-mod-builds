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
 * <p>The recommended Modrinth distribution downloads the pinned official external world on first launch and writes a
 * verified profile marker. That trusted pack world self-binds on the first player login. Manual external-world
 * installs remain operator-confirmed and never cause arbitrary saves to be rewritten as TURNBOUND worlds.</p>
 */
public final class AuthoredWorldBootstrapService {
    private static final float HUB_FACING_YAW = -90.0F;
    private final DefinitionRepository definitions;
    private final FirstExpeditionQuestService firstExpedition;
    private MinecraftServer reconciledTrustedServer;

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
        boolean trustedPackWorld = DrehmalExternalWorldBinding.hasTrustedPackMarker(server);
        boolean missingWaypoints = needsWorldInstall(saved.anchorsSnapshot());

        if (trustedPackWorld && (reconciledTrustedServer != server || missingWaypoints)) {
            try {
                DrehmalExternalWorldBinding.Result result =
                        DrehmalExternalWorldBinding.installTrustedPackWorld(server, registry);
                reconciledTrustedServer = server;
                TurnboundRe.LOGGER.info(
                        "TURNBOUND verified Modrinth world reconciled: Hub {} / Region {} / semantic entities {}",
                        result.hubArrival(),
                        result.regionArrival(),
                        result.anchorEntityIds().size());
            } catch (RuntimeException exception) {
                TurnboundRe.LOGGER.error("TURNBOUND verified pack world could not be reconciled", exception);
                return;
            }
        } else if (missingWaypoints) {
            TurnboundRe.LOGGER.warn(
                    "TURNBOUND production world is not bound. Use the TURNBOUND Modrinth pack, or install "
                            + "the official external world manually and bind it near New Drabyel.");
            return;
        }

        if (needsWorldInstall(saved.anchorsSnapshot())) {
            TurnboundRe.LOGGER.error("TURNBOUND pack binding completed without both canonical fast-travel anchors");
            return;
        }

        if (needsInitialHubArrival(saved.discovered(player.getUUID()))) {
            placeNewPlayerAtHub(player, server, saved);
        }
    }

    /** True when the server still lacks one of the two canonical external-world waypoints. */
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
