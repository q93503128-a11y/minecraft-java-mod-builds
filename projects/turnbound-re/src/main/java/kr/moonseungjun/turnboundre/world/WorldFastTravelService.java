package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Server-authoritative physical fast travel. Clients never submit coordinates or unlock state. */
public final class WorldFastTravelService {
    public enum ResultCode {
        DISCOVERED,
        TRAVELED,
        LOCKED,
        SELECT_REQUIRED,
        UNAVAILABLE,
        TOO_FAR,
        BUSY
    }

    public record Result(ResultCode code, String sourceLocator, String destinationLocator) {}

    private static final double REGISTERED_ANCHOR_TOLERANCE_SQR = 25.0D;

    private final DefinitionRepository definitions;
    private final BattleManager battles;

    public WorldFastTravelService(DefinitionRepository definitions, BattleManager battles) {
        if (definitions == null || battles == null) throw new IllegalArgumentException("definitions/battles required");
        this.definitions = definitions;
        this.battles = battles;
    }

    public Result use(ServerPlayer player, Entity target) {
        if (player == null || target == null) throw new IllegalArgumentException("player/target required");
        String dimension = player.level().dimension().identifier().toString();
        DefinitionRegistry registry = definitions.snapshot().registry();
        WorldFastTravelResolver.Resolved source = WorldFastTravelResolver
                .resolveEntity(registry, target, dimension).orElse(null);
        if (source == null) return new Result(ResultCode.UNAVAILABLE, "", "");
        String sourceLocator = source.anchor().locator();

        if (!WorldFastTravelResolver.withinUseRange(player.distanceToSqr(target))) {
            notify(player, "message.turnbound_re.fast_travel.too_far");
            return new Result(ResultCode.TOO_FAR, sourceLocator, "");
        }
        if (battles.battleForController(player.getUUID()).isPresent()) {
            notify(player, "message.turnbound_re.fast_travel.busy");
            return new Result(ResultCode.BUSY, sourceLocator, "");
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            notify(player, "message.turnbound_re.fast_travel.unavailable");
            return new Result(ResultCode.UNAVAILABLE, sourceLocator, "");
        }
        FastTravelSavedData saved = FastTravelSavedData.get(server);
        FastTravelSavedData.AnchorLocation registeredSource = saved.anchor(sourceLocator).orElse(null);
        if (!matchesRegisteredAnchor(target, dimension, registeredSource)) {
            notify(player, "message.turnbound_re.fast_travel.unavailable");
            return new Result(ResultCode.UNAVAILABLE, sourceLocator, "");
        }

        if (saved.discover(player.getUUID(), sourceLocator)) {
            player.sendSystemMessage(Component.translatable(
                    "message.turnbound_re.fast_travel.discovered", locationName(source.region().id())), true);
            return new Result(ResultCode.DISCOVERED, sourceLocator, "");
        }

        List<String> availableDestinations = new ArrayList<>();
        for (String destination : source.anchor().destinations()) {
            if (saved.discovered(player.getUUID(), destination)) availableDestinations.add(destination);
        }
        if (availableDestinations.isEmpty()) {
            notify(player, "message.turnbound_re.fast_travel.locked");
            return new Result(ResultCode.LOCKED, sourceLocator, "");
        }
        if (availableDestinations.size() != 1) {
            notify(player, "message.turnbound_re.fast_travel.select_required");
            return new Result(ResultCode.SELECT_REQUIRED, sourceLocator, "");
        }

        String destinationLocator = availableDestinations.getFirst();
        FastTravelSavedData.AnchorLocation destination = saved.anchor(destinationLocator).orElse(null);
        if (destination == null || !dimension.equals(destination.dimension())) {
            notify(player, "message.turnbound_re.fast_travel.unavailable");
            return new Result(ResultCode.UNAVAILABLE, sourceLocator, destinationLocator);
        }
        WorldFastTravelResolver.Resolved destinationDefinition = WorldFastTravelResolver
                .resolve(registry, destinationLocator, destination.dimension()).orElse(null);
        if (destinationDefinition == null) {
            notify(player, "message.turnbound_re.fast_travel.unavailable");
            return new Result(ResultCode.UNAVAILABLE, sourceLocator, destinationLocator);
        }

        ServerLevel level = player.level();
        level.getChunkAt(destination.blockPos());
        player.stopRiding();
        player.teleportTo(
                level,
                destination.x() + 0.5D,
                destination.y(),
                destination.z() + 0.5D,
                Set.of(),
                player.getYRot(),
                player.getXRot(),
                false);
        player.sendSystemMessage(Component.translatable(
                "message.turnbound_re.fast_travel.arrived", locationName(destinationDefinition.region().id())), true);
        return new Result(ResultCode.TRAVELED, sourceLocator, destinationLocator);
    }

    private static boolean matchesRegisteredAnchor(
            Entity target,
            String dimension,
            FastTravelSavedData.AnchorLocation registered
    ) {
        return registered != null
                && dimension.equals(registered.dimension())
                && target.distanceToSqr(
                        registered.x() + 0.5D,
                        registered.y() + 0.5D,
                        registered.z() + 0.5D) <= REGISTERED_ANCHOR_TOLERANCE_SQR;
    }

    private static Component locationName(String regionId) {
        if (regionId == null || regionId.isBlank()) return Component.literal("?");
        int colon = regionId.indexOf(':');
        if (colon <= 0 || colon >= regionId.length() - 1) return Component.literal(regionId);
        String namespace = regionId.substring(0, colon);
        String path = regionId.substring(colon + 1).replace('/', '.');
        return Component.translatable("travel." + namespace + "." + path);
    }

    private static void notify(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key), true);
    }
}
