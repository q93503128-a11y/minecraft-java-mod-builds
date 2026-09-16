package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.ExternalWorldProfileDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Binds TURNBOUND gameplay anchors onto an independently installed Drehmal: APOTHEOSIS world.
 *
 * <p>No Drehmal blocks, structures, datapack files or resource-pack assets are copied by this class. Coordinates
 * and semantic bindings live in reloadable TURNBOUND definition data, while this adapter only installs server
 * metadata and invisible Interaction anchors.</p>
 */
public final class DrehmalExternalWorldBinding {
    public static final String PROFILE_ID = "turnbound_re:drehmal_apotheosis_2_2_2f";
    public static final String PACK_PROFILE_MARKER_FILE = ".turnbound_re_profile";
    public static final String EXTERNAL_WORLD_ENTITY_TAG = "turnbound_re:external_world_anchor";
    private static final double SETUP_CONFIRM_DISTANCE_SQR = 192.0D * 192.0D;
    private static final float HUB_FACING_YAW = 180.0F;

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
        ExternalWorldProfileDefinition profile = definitions.externalWorldProfiles().get(PROFILE_ID);
        if (profile == null) return List.of("missing external world profile " + PROFILE_ID);
        if (!FunctionalWorldSliceLayout.DIMENSION.equals(profile.dimension())) {
            errors.add("Drehmal profile dimension mismatch: " + profile.dimension());
        }

        ExternalWorldProfileDefinition.Anchor hub = enabledAnchor(
                profile, ExternalWorldProfileDefinition.FAST_TRAVEL, WorldFastTravelPrototype.HUB_LOCATOR);
        ExternalWorldProfileDefinition.Anchor region = enabledAnchor(
                profile, ExternalWorldProfileDefinition.FAST_TRAVEL, WorldFastTravelPrototype.REGION_LOCATOR);
        if (hub == null) errors.add("missing enabled Hub fast-travel binding " + WorldFastTravelPrototype.HUB_LOCATOR);
        if (region == null) errors.add("missing enabled Region fast-travel binding " + WorldFastTravelPrototype.REGION_LOCATOR);
        if (hub != null && region != null && position(hub).equals(position(region))) {
            errors.add("external Hub and Region arrivals must differ");
        }
        return List.copyOf(errors);
    }

    /**
     * Operator-confirmed binding for manually installed worlds. The operator must stand near the configured
     * New Drabyel landmark so an unrelated save cannot be bound by accident.
     */
    public static Result install(ServerPlayer operator, DefinitionRegistry definitions) {
        if (operator == null || definitions == null) throw new IllegalArgumentException("operator/definitions required");
        List<String> errors = validate(definitions);
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("; ", errors));

        ExternalWorldProfileDefinition profile = definitions.externalWorldProfiles().get(PROFILE_ID);
        String dimension = operator.level().dimension().identifier().toString();
        if (!profile.dimension().equals(dimension)) {
            throw new IllegalStateException("Drehmal binding must be run from " + profile.dimension());
        }

        BlockPos hubArrival = position(enabledAnchor(
                profile, ExternalWorldProfileDefinition.FAST_TRAVEL, WorldFastTravelPrototype.HUB_LOCATOR));
        if (operator.distanceToSqr(
                hubArrival.getX() + 0.5D,
                hubArrival.getY() + 0.5D,
                hubArrival.getZ() + 0.5D) > SETUP_CONFIRM_DISTANCE_SQR) {
            throw new IllegalStateException(
                    "stand near the configured New Drabyel Hub landmark before binding the external world");
        }

        MinecraftServer server = operator.level().getServer();
        if (server == null) throw new IllegalStateException("server unavailable");
        return installOnServer(server, definitions);
    }

    /**
     * Zero-command binding for the TURNBOUND Prism distribution. The bootstrapper writes a tiny profile marker
     * only after the pinned official Drehmal world passes its directory hash. Arbitrary saves remain fail-closed.
     */
    public static Result installTrustedPackWorld(MinecraftServer server, DefinitionRegistry definitions) {
        if (server == null || definitions == null) throw new IllegalArgumentException("server/definitions required");
        if (!hasTrustedPackMarker(server)) {
            throw new IllegalStateException("TURNBOUND pack world marker is missing or does not match " + PROFILE_ID);
        }
        return installOnServer(server, definitions);
    }

    public static boolean hasTrustedPackMarker(MinecraftServer server) {
        if (server == null) return false;
        Path marker = server.getWorldPath(LevelResource.ROOT).resolve(PACK_PROFILE_MARKER_FILE);
        try {
            return Files.isRegularFile(marker)
                    && markerMatches(Files.readString(marker, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            return false;
        }
    }

    static boolean markerMatches(String markerText) {
        return markerText != null && PROFILE_ID.equals(markerText.trim());
    }

    private static Result installOnServer(MinecraftServer server, DefinitionRegistry definitions) {
        List<String> errors = validate(definitions);
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("; ", errors));

        ExternalWorldProfileDefinition profile = definitions.externalWorldProfiles().get(PROFILE_ID);
        ExternalWorldProfileDefinition.Anchor hubBinding = enabledAnchor(
                profile, ExternalWorldProfileDefinition.FAST_TRAVEL, WorldFastTravelPrototype.HUB_LOCATOR);
        ExternalWorldProfileDefinition.Anchor regionBinding = enabledAnchor(
                profile, ExternalWorldProfileDefinition.FAST_TRAVEL, WorldFastTravelPrototype.REGION_LOCATOR);
        BlockPos hubArrival = position(hubBinding);
        BlockPos regionArrival = position(regionBinding);

        ServerLevel overworld = server.overworld();
        FastTravelSavedData saved = FastTravelSavedData.get(server);
        List<UUID> entityIds = new ArrayList<>();

        for (ExternalWorldProfileDefinition.Anchor anchor : profile.anchors()) {
            if (anchor == null || !anchor.enabled()) continue;
            BlockPos pos = position(anchor);
            overworld.getChunkAt(pos);

            String tag;
            switch (anchor.kind()) {
                case ExternalWorldProfileDefinition.FAST_TRAVEL -> {
                    saved.registerAnchor(anchor.locator(), profile.dimension(), pos);
                    tag = WorldFastTravelResolver.tagFor(anchor.locator());
                }
                case ExternalWorldProfileDefinition.RESOURCE -> tag = WorldResourceAnchorResolver.tagFor(anchor.locator());
                case ExternalWorldProfileDefinition.ENCOUNTER -> tag = WorldEncounterAnchorResolver.tagFor(anchor.locator());
                default -> throw new IllegalStateException("unsupported external-world anchor kind " + anchor.kind());
            }
            UUID id = deterministicAnchorId(profile.id(), anchor);
            spawnOrRetagAnchor(overworld, id, pos, tag);
            entityIds.add(id);
        }

        // Binding is server setup, not player progression. Do not mark any player as having discovered the Hub.
        server.setRespawnData(LevelData.RespawnData.of(
                overworld.dimension(), hubArrival, HUB_FACING_YAW, 0.0F));

        return new Result(hubArrival.immutable(), regionArrival.immutable(), entityIds);
    }

    private static ExternalWorldProfileDefinition.Anchor enabledAnchor(
            ExternalWorldProfileDefinition profile,
            String kind,
            String locator
    ) {
        if (profile == null || kind == null || locator == null) return null;
        for (ExternalWorldProfileDefinition.Anchor anchor : profile.anchors()) {
            if (anchor != null && anchor.enabled() && kind.equals(anchor.kind()) && locator.equals(anchor.locator())) {
                return anchor;
            }
        }
        return null;
    }

    private static BlockPos position(ExternalWorldProfileDefinition.Anchor anchor) {
        if (anchor == null) throw new IllegalArgumentException("anchor required");
        return new BlockPos(anchor.x(), anchor.y(), anchor.z());
    }

    private static UUID deterministicAnchorId(String profileId, ExternalWorldProfileDefinition.Anchor anchor) {
        String key = profileId + "|" + anchor.kind() + "|" + anchor.locator();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static void spawnOrRetagAnchor(ServerLevel level, UUID id, BlockPos pos, String semanticTag) {
        var existing = level.getEntity(id);
        if (existing != null) {
            existing.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            existing.entityTags().add(semanticTag);
            existing.entityTags().add(EXTERNAL_WORLD_ENTITY_TAG);
            return;
        }

        Interaction anchor = new Interaction(EntityTypes.INTERACTION, level);
        anchor.setUUID(id);
        anchor.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        anchor.entityTags().add(semanticTag);
        anchor.entityTags().add(EXTERNAL_WORLD_ENTITY_TAG);
        level.addFreshEntity(anchor);
    }
}
