package dev.moonseungjun.augmentationsurvival.augment.spatial;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Server-authoritative permanent gate registry and cross-dimension travel boundary. */
public final class SpatialLinkService {
    public static final String SPATIAL_LINK_ID = id("spatial_link");

    private SpatialLinkService() { }

    public static void initialize() {
        AugmentCatalog.require(SPATIAL_LINK_ID);
    }

    public static boolean hasAccess(ServerPlayer player) {
        return AugmentationPlayerStateService.state(player).level(SPATIAL_LINK_ID) > 0;
    }

    public static List<SpatialLinkWorldState.GateRecord> visibleGates(ServerPlayer player) {
        if (!hasAccess(player)) return List.of();
        return state(player.level().getServer()).visibleGates(player.getUUID());
    }

    public static SpatialLinkWorldState snapshot(ServerPlayer player) {
        return state(player.level().getServer());
    }

    public static boolean registerCurrentLocation(ServerPlayer player, String requestedName) {
        if (!hasAccess(player) || player.isDeadOrDying()) return false;
        String name = normalizeName(requestedName, "공간문 " + (ownedGateCount(player) + 1));
        String gateId = UUID.randomUUID().toString();
        SpatialLinkWorldState.GateRecord gate = new SpatialLinkWorldState.GateRecord(
                gateId,
                player.getUUID().toString(),
                name,
                player.level().dimension().identifier().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                List.of()
        );
        setState(player.level().getServer(), state(player.level().getServer()).withGate(gate));
        return true;
    }

    public static boolean rename(ServerPlayer player, String gateId, String requestedName) {
        if (!hasAccess(player)) return false;
        SpatialLinkWorldState current = state(player.level().getServer());
        SpatialLinkWorldState.GateRecord gate = current.gates().get(gateId);
        if (gate == null || !gate.owner().equals(player.getUUID().toString())) return false;
        String normalized = normalizeName(requestedName, gate.name());
        setState(player.level().getServer(), current.rename(player.getUUID(), gateId, normalized));
        return true;
    }

    public static boolean delete(ServerPlayer player, String gateId) {
        if (!hasAccess(player)) return false;
        SpatialLinkWorldState current = state(player.level().getServer());
        if (!current.gates().containsKey(gateId)) return false;
        SpatialLinkWorldState next = current.withoutGate(player.getUUID(), gateId);
        if (next.equals(current)) return false;
        setState(player.level().getServer(), next);
        return true;
    }

    public static boolean toggleFavorite(ServerPlayer player, String gateId) {
        if (!hasAccess(player)) return false;
        SpatialLinkWorldState current = state(player.level().getServer());
        if (!current.isVisible(player.getUUID(), gateId)) return false;
        setState(player.level().getServer(), current.toggleFavorite(player.getUUID(), gateId));
        return true;
    }

    public static boolean setDefault(ServerPlayer player, String gateId) {
        if (!hasAccess(player)) return false;
        SpatialLinkWorldState current = state(player.level().getServer());
        if (!current.isVisible(player.getUUID(), gateId)) return false;
        setState(player.level().getServer(), current.withDefault(player.getUUID(), gateId));
        return true;
    }

    public static boolean share(ServerPlayer player, String gateId, ServerPlayer target, boolean shared) {
        if (!hasAccess(player) || !hasAccess(target) || player == target) return false;
        SpatialLinkWorldState current = state(player.level().getServer());
        SpatialLinkWorldState.GateRecord gate = current.gates().get(gateId);
        if (gate == null || !gate.owner().equals(player.getUUID().toString())) return false;
        setState(player.level().getServer(), current.share(player.getUUID(), gateId, target.getUUID(), shared));
        return true;
    }

    public static boolean teleport(ServerPlayer player, String gateId) {
        if (!hasAccess(player) || player.isDeadOrDying()) return false;
        MinecraftServer server = player.level().getServer();
        SpatialLinkWorldState current = state(server);
        if (!current.isVisible(player.getUUID(), gateId)) return false;
        SpatialLinkWorldState.GateRecord gate = current.gates().get(gateId);
        if (gate == null) return false;

        Identifier dimensionId;
        try {
            dimensionId = Identifier.parse(gate.dimension());
        } catch (RuntimeException invalid) {
            return false;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel target = server.getLevel(dimension);
        if (target == null) return false;

        BlockPos registered = BlockPos.containing(gate.x(), gate.y(), gate.z());
        target.getChunkAt(registered);
        BlockPos safe = findSafePosition(target, player, registered);
        if (safe == null) {
            player.sendSystemMessage(Component.translatable("message.augmentation_survival.spatial_link.blocked"));
            return false;
        }

        double x = safe.getX() + 0.5D;
        double y = safe.getY();
        double z = safe.getZ() + 0.5D;
        player.stopRiding();
        return player.teleportTo(target, x, y, z, Set.of(), gate.yaw(), gate.pitch(), true);
    }

    public static boolean teleportDefault(ServerPlayer player) {
        String gateId = state(player.level().getServer()).defaultGateByOwner().get(player.getUUID().toString());
        return gateId != null && teleport(player, gateId);
    }

    public static boolean isFavorite(ServerPlayer player, String gateId) {
        return state(player.level().getServer()).isFavorite(player.getUUID(), gateId);
    }

    public static boolean isDefault(ServerPlayer player, String gateId) {
        return state(player.level().getServer()).isDefault(player.getUUID(), gateId);
    }

    public static boolean isOwner(ServerPlayer player, String gateId) {
        SpatialLinkWorldState.GateRecord gate = state(player.level().getServer()).gates().get(gateId);
        return gate != null && gate.owner().equals(player.getUUID().toString());
    }

    private static int ownedGateCount(ServerPlayer player) {
        String owner = player.getUUID().toString();
        int count = 0;
        for (SpatialLinkWorldState.GateRecord gate : state(player.level().getServer()).gates().values()) {
            if (gate.owner().equals(owner)) count++;
        }
        return count;
    }

    private static BlockPos findSafePosition(ServerLevel level, ServerPlayer player, BlockPos origin) {
        for (int offset = 0; offset <= 4; offset++) {
            for (int direction : offset == 0 ? new int[]{0} : new int[]{offset, -offset}) {
                BlockPos candidate = origin.offset(0, direction, 0);
                if (candidate.getY() < level.getMinY() || candidate.getY() >= level.getMaxY() - 2) continue;
                AABB moved = player.getBoundingBox().move(
                        candidate.getX() + 0.5D - player.getX(),
                        candidate.getY() - player.getY(),
                        candidate.getZ() + 0.5D - player.getZ()
                );
                if (level.noCollision(player, moved)) return candidate;
            }
        }
        return null;
    }

    private static String normalizeName(String requested, String fallback) {
        String value = requested == null ? "" : requested.strip();
        if (value.isEmpty()) value = fallback;
        if (value.length() > 32) value = value.substring(0, 32);
        return value;
    }

    private static SpatialLinkWorldState state(MinecraftServer server) {
        return server.overworld().getAttachedOrSet(SpatialLinkWorldAttachments.STATE, SpatialLinkWorldState.initial());
    }

    private static void setState(MinecraftServer server, SpatialLinkWorldState state) {
        server.overworld().setAttached(SpatialLinkWorldAttachments.STATE, state);
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
