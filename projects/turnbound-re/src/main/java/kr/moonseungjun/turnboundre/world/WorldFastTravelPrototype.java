package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Adds the first two physical discovery/travel points to the production-facing HUB_01 -> REGION_01 prototype. */
public final class WorldFastTravelPrototype {
    public static final String HUB_LOCATOR = "turnbound_re:hub_01/waypoint";
    public static final String REGION_LOCATOR = "turnbound_re:region_01/waypoint";
    public static final int HUB_OFFSET_X = -8;
    public static final int HUB_OFFSET_Z = 0;
    public static final int REGION_OFFSET_X = 62;
    public static final int REGION_OFFSET_Z = -6;
    private static final int UPDATE_FLAGS = 3;

    public record Result(List<UUID> anchorEntityIds) {
        public Result {
            if (anchorEntityIds == null) throw new IllegalArgumentException("anchorEntityIds required");
            anchorEntityIds = List.copyOf(anchorEntityIds);
        }
    }

    private WorldFastTravelPrototype() {}

    public static List<String> validate(DefinitionRegistry definitions) {
        if (definitions == null) return List.of("definitions missing");
        List<String> errors = new ArrayList<>();
        var hub = WorldFastTravelResolver.resolve(definitions, HUB_LOCATOR, FunctionalWorldSliceLayout.DIMENSION).orElse(null);
        var region = WorldFastTravelResolver.resolve(definitions, REGION_LOCATOR, FunctionalWorldSliceLayout.DIMENSION).orElse(null);
        if (hub == null) errors.add("missing authored Hub fast-travel locator " + HUB_LOCATOR);
        if (region == null) errors.add("missing authored Region fast-travel locator " + REGION_LOCATOR);
        if (hub != null && !hub.anchor().destinations().equals(List.of(REGION_LOCATOR))) {
            errors.add("Hub fast-travel point must link exactly to REGION_01 during the two-point slice");
        }
        if (region != null && !region.anchor().destinations().equals(List.of(HUB_LOCATOR))) {
            errors.add("REGION_01 fast-travel point must link exactly to HUB_01 during the two-point slice");
        }

        ProductionWorldSlicePlan.Footprint waypoint = new ProductionWorldSlicePlan.Footprint(
                "region_waypoint",
                REGION_OFFSET_X - 1, REGION_OFFSET_X + 1,
                REGION_OFFSET_Z - 1, REGION_OFFSET_Z + 1);
        for (ProductionWorldSlicePlan.Footprint landmark : ProductionWorldSlicePlan.LANDMARK_FOOTPRINTS) {
            if (waypoint.overlaps(landmark)) errors.add("region fast-travel waypoint overlaps " + landmark.id());
        }
        if (REGION_OFFSET_Z >= -ProductionWorldSlicePlan.ROUTE_HALF_WIDTH - 1) {
            errors.add("region fast-travel waypoint must sit off the main road, not inside it");
        }
        return List.copyOf(errors);
    }

    public static Result install(ServerPlayer player, DefinitionRegistry definitions, BlockPos origin) {
        if (player == null || definitions == null || origin == null) throw new IllegalArgumentException("player/definitions/origin required");
        List<String> errors = validate(definitions);
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("; ", errors));
        String dimension = player.level().dimension().identifier().toString();
        if (!FunctionalWorldSliceLayout.DIMENSION.equals(dimension)) {
            throw new IllegalStateException("fast travel prototype requires " + FunctionalWorldSliceLayout.DIMENSION);
        }

        ServerLevel level = player.level();
        MinecraftServer server = level.getServer();
        if (server == null) throw new IllegalStateException("server unavailable");
        FastTravelSavedData saved = FastTravelSavedData.get(server);

        BlockPos hubMarker = origin.offset(HUB_OFFSET_X, 0, HUB_OFFSET_Z);
        BlockPos regionMarker = origin.offset(REGION_OFFSET_X, 0, REGION_OFFSET_Z);
        BlockPos hubArrival = buildWaypoint(level, hubMarker, Blocks.POLISHED_TUFF.defaultBlockState());
        BlockPos regionArrival = buildWaypoint(level, regionMarker, Blocks.STONE_BRICKS.defaultBlockState());

        saved.registerAnchor(HUB_LOCATOR, dimension, hubArrival);
        saved.registerAnchor(REGION_LOCATOR, dimension, regionArrival);

        UUID hubEntity = spawnAnchor(level, hubMarker, HUB_LOCATOR);
        UUID regionEntity = spawnAnchor(level, regionMarker, REGION_LOCATOR);
        return new Result(List.of(hubEntity, regionEntity));
    }

    private static BlockPos buildWaypoint(ServerLevel level, BlockPos marker, BlockState floor) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(marker.offset(dx, 0, dz), floor, UPDATE_FLAGS);
                level.setBlock(marker.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                level.setBlock(marker.offset(dx, 2, dz), Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            }
        }
        level.setBlock(marker.above(), Blocks.LODESTONE.defaultBlockState(), UPDATE_FLAGS);
        level.setBlock(marker.offset(-1, 1, -1), Blocks.STONE_BRICK_WALL.defaultBlockState(), UPDATE_FLAGS);
        level.setBlock(marker.offset(1, 1, -1), Blocks.STONE_BRICK_WALL.defaultBlockState(), UPDATE_FLAGS);
        level.setBlock(marker.offset(-1, 2, -1), Blocks.LANTERN.defaultBlockState(), UPDATE_FLAGS);
        level.setBlock(marker.offset(1, 2, -1), Blocks.LANTERN.defaultBlockState(), UPDATE_FLAGS);
        return marker.offset(0, 1, 1).immutable();
    }

    private static UUID spawnAnchor(ServerLevel level, BlockPos marker, String locator) {
        Interaction anchor = new Interaction(EntityTypes.INTERACTION, level);
        anchor.setPos(marker.getX() + 0.5D, marker.getY() + 1.75D, marker.getZ() + 0.5D);
        anchor.entityTags().add(WorldFastTravelResolver.tagFor(locator));
        anchor.entityTags().add(ProductionWorldSlicePrototypeBuilder.PROTOTYPE_ENTITY_TAG);
        level.addFreshEntity(anchor);
        return anchor.getUUID();
    }
}
