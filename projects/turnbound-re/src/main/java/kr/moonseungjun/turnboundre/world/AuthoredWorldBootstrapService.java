package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.Set;

/**
 * Production entry point for the first authored TURNBOUND: RE world slice.
 *
 * A fresh world no longer requires an operator command. The first joining player causes the authored Hub/Region
 * slice to be installed once near the vanilla Overworld spawn. Per-player Hub discovery doubles as the onboarding
 * marker, so every new player begins in the same server-owned Hub without a second quest/save flag.
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
            TurnboundRe.LOGGER.error("TURNBOUND authored world bootstrap skipped because region definitions are unavailable");
            return;
        }

        FastTravelSavedData saved = FastTravelSavedData.get(server);
        if (needsWorldInstall(saved.anchorsSnapshot())) {
            if (!installFreshWorld(player, server, registry)) return;
            saved = FastTravelSavedData.get(server);
        }

        if (needsInitialHubArrival(saved.discovered(player.getUUID()))) {
            placeNewPlayerAtHub(player, server, saved);
        }
    }

    static boolean needsWorldInstall(Map<String, FastTravelSavedData.AnchorLocation> anchors) {
        if (anchors == null) return true;
        return !anchors.containsKey(WorldFastTravelPrototype.HUB_LOCATOR)
                || !anchors.containsKey(WorldFastTravelPrototype.REGION_LOCATOR);
    }

    static boolean needsInitialHubArrival(Set<String> discoveredLocators) {
        return discoveredLocators == null || !discoveredLocators.contains(WorldFastTravelPrototype.HUB_LOCATOR);
    }

    private boolean installFreshWorld(
            ServerPlayer player,
            MinecraftServer server,
            DefinitionRegistry registry
    ) {
        ServerLevel overworld = server.overworld();
        BlockPos vanillaSpawn = overworld.getRespawnData().pos();
        overworld.getChunkAt(vanillaSpawn);

        try {
            if (player.level() != overworld) {
                player.stopRiding();
                player.teleportTo(
                        overworld,
                        vanillaSpawn.getX() + 0.5D,
                        vanillaSpawn.getY(),
                        vanillaSpawn.getZ() + 0.5D,
                        Set.of(),
                        HUB_FACING_YAW,
                        0.0F,
                        false);
            }

            AuthoredFirstRegionBuilder.Result world =
                    AuthoredFirstRegionBuilder.build(player, registry, vanillaSpawn);

            // 26.2 world spawn is server RespawnData. Death/no-bed respawn now returns to the authored Hub,
            // rather than the discarded vanilla spawn used only as a search seed.
            server.setRespawnData(LevelData.RespawnData.of(
                    overworld.dimension(), world.hubArrival(), HUB_FACING_YAW, 0.0F));

            TurnboundRe.LOGGER.info(
                    "Installed TURNBOUND authored first region at {} {} {} (terrain relief={}, wet samples={}, search distance²={})",
                    world.origin().getX(), world.origin().getY(), world.origin().getZ(),
                    world.terrain().relief(), world.terrain().wetSamples(), world.terrain().distanceSquared());
            return true;
        } catch (RuntimeException failure) {
            TurnboundRe.LOGGER.error("TURNBOUND authored world bootstrap failed", failure);
            return false;
        }
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
