package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.storage.LevelData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Binds TURNBOUND gameplay anchors onto an independently installed Drehmal: APOTHEOSIS world.
 *
 * <p>No Drehmal blocks, structures, datapack files or resource-pack assets are copied by this class. It only
 * registers TURNBOUND server metadata and invisible Interaction anchors at public, documented world landmarks.</p>
 */
public final class DrehmalExternalWorldBinding {
    public static final String PROFILE_ID = "drehmal:apotheosis_v2_2_2f";
    public static final String EXTERNAL_WORLD_ENTITY_TAG = "turnbound_re:external_world_anchor";

    // Official wiki locations for the external world. HUB_01 uses New Drabyel, the first town reached from Stasis.
    public static final BlockPos HUB_ARRIVAL = new BlockPos(502, 67, 1801);
    public static final BlockPos REGION_ARRIVAL = new BlockPos(778, 31, 668);

    private static final float HUB_FACING_YAW = 180.0F;
    private static final UUID HUB_ANCHOR_UUID = UUID.fromString("14d4a0c6-4a8a-4eb6-bbfd-20d1d83d1001");
    private static final UUID REGION_ANCHOR_UUID = UUID.fromString("14d4a0c6-4a8a-4eb6-bbfd-20d1d83d1002");

    public record Result(BlockPos hubArrival, BlockPos regionArrival, List<UUID> anchorEntityIds) {
        public Result {
            if (hubArrival == null || regionArrival == null || anchorEntityIds == null) {
                throw new IllegalArgumentException("external world binding result fields required");
            }
            anchorEntityIds = List.copyOf(anchorEntityIds);
        }
    }

    private DrehmalExternalWorldBinding() {}

    public static List<String> validate(DefinitionRegistry definitions) {
        if (definitions == null) return List.of("definitions missing");
        List<String> errors = new ArrayList<>();
        String dimension = FunctionalWorldSliceLayout.DIMENSION;
        if (WorldFastTravelResolver.resolve(definitions, WorldFastTravelPrototype.HUB_LOCATOR, dimension).isEmpty()) {
            errors.add("missing Hub fast-travel locator " + WorldFastTravelPrototype.HUB_LOCATOR);
        }
        if (WorldFastTravelResolver.resolve(definitions, WorldFastTravelPrototype.REGION_LOCATOR, dimension).isEmpty()) {
            errors.add("missing Region fast-travel locator " + WorldFastTravelPrototype.REGION_LOCATOR);
        }
        if (HUB_ARRIVAL.equals(REGION_ARRIVAL)) errors.add("external Hub and Region arrivals must differ");
        return List.copyOf(errors);
    }

    /**
     * Operator-confirmed binding. This deliberately does not auto-detect worlds because a false positive would
     * mutate an unrelated save. The official Drehmal world must already be installed by the user.
     */
    public static Result install(ServerPlayer operator, DefinitionRegistry definitions) {
        if (operator == null || definitions == null) throw new IllegalArgumentException("operator/definitions required");
        List<String> errors = validate(definitions);
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("; ", errors));

        String dimension = operator.level().dimension().identifier().toString();
        if (!FunctionalWorldSliceLayout.DIMENSION.equals(dimension)) {
            throw new IllegalStateException("Drehmal binding must be run from " + FunctionalWorldSliceLayout.DIMENSION);
        }

        MinecraftServer server = operator.level().getServer();
        if (server == null) throw new IllegalStateException("server unavailable");
        ServerLevel overworld = server.overworld();

        // Loading the two known landmarks is enough for metadata/anchor registration and does not alter map blocks.
        overworld.getChunkAt(HUB_ARRIVAL);
        overworld.getChunkAt(REGION_ARRIVAL);

        FastTravelSavedData saved = FastTravelSavedData.get(server);
        saved.registerAnchor(WorldFastTravelPrototype.HUB_LOCATOR, dimension, HUB_ARRIVAL);
        saved.registerAnchor(WorldFastTravelPrototype.REGION_LOCATOR, dimension, REGION_ARRIVAL);

        spawnOrRetagAnchor(overworld, HUB_ANCHOR_UUID, HUB_ARRIVAL, WorldFastTravelPrototype.HUB_LOCATOR);
        spawnOrRetagAnchor(overworld, REGION_ANCHOR_UUID, REGION_ARRIVAL, WorldFastTravelPrototype.REGION_LOCATOR);

        // Binding is server setup, not player progression. Do not mark the operator as having discovered the Hub.
        // TURNBOUND's current loop begins at its Hub. This changes only respawn metadata, never Drehmal geometry.
        server.setRespawnData(LevelData.RespawnData.of(
                overworld.dimension(), HUB_ARRIVAL, HUB_FACING_YAW, 0.0F));

        return new Result(HUB_ARRIVAL.immutable(), REGION_ARRIVAL.immutable(),
                List.of(HUB_ANCHOR_UUID, REGION_ANCHOR_UUID));
    }

    private static void spawnOrRetagAnchor(ServerLevel level, UUID id, BlockPos pos, String locator) {
        var existing = level.getEntity(id);
        if (existing != null) {
            existing.entityTags().add(WorldFastTravelResolver.tagFor(locator));
            existing.entityTags().add(EXTERNAL_WORLD_ENTITY_TAG);
            return;
        }

        Interaction anchor = new Interaction(EntityTypes.INTERACTION, level);
        anchor.setUUID(id);
        anchor.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        anchor.entityTags().add(WorldFastTravelResolver.tagFor(locator));
        anchor.entityTags().add(EXTERNAL_WORLD_ENTITY_TAG);
        level.addFreshEntity(anchor);
    }
}
