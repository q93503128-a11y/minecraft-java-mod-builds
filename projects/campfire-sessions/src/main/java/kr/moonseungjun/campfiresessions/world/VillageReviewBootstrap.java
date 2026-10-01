package kr.moonseungjun.campfiresessions.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import kr.moonseungjun.campfiresessions.CampfireSessions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Development-only physical village slice on the verified canonical island.
 *
 * <p>The bootstrap refuses arbitrary worlds, preflights every real external
 * structure, applies a bounded authored landscape pass, then places the selected
 * Kogtyv Greece shells plus the MIT Currents of Trade dock.</p>
 */
public final class VillageReviewBootstrap {
    private static final String PROPERTY = "campfiresessions.villageReview";
    private static final String SOURCE_MARKER_NAME = ".campfiresessions-canonical-world-source";
    private static final String MARKER_NAME = ".campfiresessions-village-review-v3";
    private static final String CANONICAL_WORLD_SHA256 =
            "7a3d98ff75feb26913e2d4c32ca7339448d3c660c14f986ce9f5e4ff340d3d3b";
    private static final int MAX_GRADE_DELTA = 8;

    private static final BlockIgnoreProcessor REVIEW_MARKER_PROCESSOR = new BlockIgnoreProcessor(List.of(
            Blocks.JIGSAW,
            Blocks.BARRIER,
            Blocks.STRUCTURE_BLOCK,
            Blocks.STRUCTURE_VOID
    ));

    private static final List<BuildingSpec> BUILDINGS = List.of(
            kogtyv("resident_services", "house/shop_triple_2", -312, 71, -19, 13, 16, 8),
            kogtyv("general_store", "house/shop_triple_1", -303, 72, -46, 13, 16, 8),
            kogtyv("clinic", "house/shop_medium_3", -321, 67, -12, 7, 16, 8),
            kogtyv("cafe", "house/shop_medium_1", -287, 73, -46, 7, 16, 8),
            kogtyv("clothing_shop", "house/shop_medium_2", -291, 74, -11, 7, 16, 8),
            kogtyv("museum", "center/ratush_1", -294, 75, -32, 21, 8, 16),

            // The source dock's street connector is local (5,2,0). CLOCKWISE_90
            // maps its long +Z pier axis westward into the canonical ocean.
            external(
                    "harbor_dock",
                    "external/currents_of_trade/dock",
                    -329, 62, -53,
                    Rotation.CLOCKWISE_90,
                    11, 10, 15,
                    false
            ),

            // First housing-scale pass: real external shells, not placeholders.
            kogtyv("player_house_stage_1", "house/small_1", -322, 66, -72, 5, 16, 5),
            kogtyv("resident_house_south_1", "house/small_2", -312, 67, -72, 5, 16, 5),
            kogtyv("resident_house_south_2", "house/medium_1", -322, 68, -60, 7, 16, 6),
            kogtyv("resident_house_east_1", "house/small_3", -283, 71, -62, 5, 16, 5),
            kogtyv("resident_house_north_1", "house/medium_2", -300, 71, 6, 7, 16, 6)
    );

    private VillageReviewBootstrap() {}

    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        if (!enabled()) {
            return;
        }

        verifyCanonicalSource(event.getServer().getWorldPath(LevelResource.ROOT));

        ServerLevel level = event.getServer().overworld();
        Path marker = event.getServer().getWorldPath(LevelResource.ROOT).resolve(MARKER_NAME);
        if (Files.exists(marker)) {
            CampfireSessions.LOGGER.info("Campfire village review layout already applied: {}", marker);
            return;
        }

        List<PreparedBuilding> prepared = prepareBuildings(level);
        preflight(level, prepared);

        List<BoundingBox> protectedBounds = prepared.stream().map(PreparedBuilding::bounds).toList();
        VillageReviewLandscape.preflight(level, protectedBounds);
        VillageReviewLandscape.place(level, protectedBounds);

        for (PreparedBuilding building : prepared) {
            place(level, building);
        }

        try {
            Files.writeString(
                    marker,
                    "Campfire Sessions village review v3\n"
                            + "canonical archive sha256: " + CANONICAL_WORLD_SHA256 + "\n"
                            + "canonical terrain probe: run 36806856318\n"
                            + "external structures: " + BUILDINGS.size() + "\n"
                            + "landscape: plaza + connected village paths\n"
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write Campfire village review marker " + marker, exception);
        }

        CampfireSessions.LOGGER.info(
                "Campfire village review layout applied: {} real external structures",
                BUILDINGS.size()
        );
    }

    private static void verifyCanonicalSource(Path worldRoot) {
        Path sourceMarker = worldRoot.resolve(SOURCE_MARKER_NAME);
        if (!Files.isRegularFile(sourceMarker)) {
            throw new IllegalStateException(
                    "Campfire village review refused unverified world: missing " + sourceMarker
            );
        }
        try {
            String text = Files.readString(sourceMarker);
            if (!text.contains(CANONICAL_WORLD_SHA256)) {
                throw new IllegalStateException(
                        "Campfire village review canonical marker does not contain the pinned archive SHA-256"
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read Campfire canonical world marker " + sourceMarker, exception);
        }
    }

    private static List<PreparedBuilding> prepareBuildings(ServerLevel level) {
        return BUILDINGS.stream().map(spec -> {
            StructureTemplate template = template(level, spec);
            StructurePlaceSettings settings = settings(spec);
            BoundingBox bounds = template.getBoundingBox(settings, spec.origin());
            return new PreparedBuilding(spec, template, settings, bounds);
        }).toList();
    }

    private static void preflight(ServerLevel level, List<PreparedBuilding> buildings) {
        List<String> terrainFailures = new ArrayList<>();
        List<PreparedBuilding> failedBuildings = new ArrayList<>();
        for (PreparedBuilding building : buildings) {
            BuildingSpec spec = building.spec();
            Vec3i actual = building.template().getSize();
            if (!actual.equals(spec.expectedSize())) {
                throw new IllegalStateException(
                        "Campfire village review structure size changed for " + spec.role()
                                + ": expected=" + spec.expectedSize() + " actual=" + actual
                );
            }

            if (spec.gradeFootprint()) {
                String failure = landPreflightFailure(level, building);
                if (failure != null) {
                    terrainFailures.add(failure);
                    failedBuildings.add(building);
                }
            }
        }

        if (!terrainFailures.isEmpty()) {
            List<String> suggestions = failedBuildings.stream()
                    .map(building -> building.spec().role() + "="
                            + relocationSuggestions(level, building, buildings, failedBuildings))
                    .toList();
            throw new IllegalStateException(
                    "Campfire village review terrain preflight failed: "
                            + String.join("; ", terrainFailures)
                            + " | relocation candidates: "
                            + String.join("; ", suggestions)
            );
        }

        for (PreparedBuilding building : buildings) {
            if (!building.spec().gradeFootprint() && "harbor_dock".equals(building.spec().role())) {
                preflightDock(level, building);
            }
        }
    }

    private static String landPreflightFailure(ServerLevel level, PreparedBuilding building) {
        int targetGroundY = building.spec().origin().getY() - 1;
        BoundingBox box = building.bounds();
        int worstDelta = -1;
        int worstX = box.minX();
        int worstZ = box.minZ();
        int worstSurface = targetGroundY;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                level.getChunkAt(new BlockPos(x, targetGroundY, z));
                int currentSurface = terrainSurfaceY(level, x, z);
                int delta = Math.abs(currentSurface - targetGroundY);
                if (delta > worstDelta) {
                    worstDelta = delta;
                    worstX = x;
                    worstZ = z;
                    worstSurface = currentSurface;
                }
            }
        }

        if (worstDelta > MAX_GRADE_DELTA) {
            return building.spec().role()
                    + " worst=(" + worstX + "," + worstZ + ")"
                    + " surface=" + worstSurface
                    + " target=" + targetGroundY
                    + " delta=" + worstDelta;
        }
        return null;
    }

    private static String relocationSuggestions(
            ServerLevel level,
            PreparedBuilding building,
            List<PreparedBuilding> buildings,
            List<PreparedBuilding> failedBuildings
    ) {
        BuildingSpec spec = building.spec();
        int desiredGroundY = spec.origin().getY() - 1;
        List<RelocationCandidate> candidates = new ArrayList<>();
        int radius = "museum".equals(spec.role()) ? 48 : 36;

        for (int originX = spec.origin().getX() - radius; originX <= spec.origin().getX() + radius; originX += 2) {
            for (int originZ = spec.origin().getZ() - radius; originZ <= spec.origin().getZ() + radius; originZ += 2) {
                BlockPos probeOrigin = new BlockPos(originX, spec.origin().getY(), originZ);
                BoundingBox box = building.template().getBoundingBox(building.settings(), probeOrigin);
                if (intersectsPlaza(box) || intersectsFixedBuilding(box, building, buildings, failedBuildings)) {
                    continue;
                }

                int minSurface = Integer.MAX_VALUE;
                int maxSurface = Integer.MIN_VALUE;
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    for (int z = box.minZ(); z <= box.maxZ(); z++) {
                        level.getChunkAt(new BlockPos(x, desiredGroundY, z));
                        int surface = terrainSurfaceY(level, x, z);
                        minSurface = Math.min(minSurface, surface);
                        maxSurface = Math.max(maxSurface, surface);
                    }
                }

                if (maxSurface - minSurface > MAX_GRADE_DELTA * 2) {
                    continue;
                }
                int targetMin = maxSurface - MAX_GRADE_DELTA;
                int targetMax = minSurface + MAX_GRADE_DELTA;
                int targetGroundY = Math.max(targetMin, Math.min(desiredGroundY, targetMax));
                if (targetGroundY < desiredGroundY - 6 || targetGroundY > desiredGroundY + 4) {
                    continue;
                }
                int worstDelta = Math.max(maxSurface - targetGroundY, targetGroundY - minSurface);
                int movement = Math.abs(originX - spec.origin().getX()) + Math.abs(originZ - spec.origin().getZ());
                int score = worstDelta * 10000
                        + Math.abs(targetGroundY - desiredGroundY) * 250
                        + movement;
                candidates.add(new RelocationCandidate(
                        new BlockPos(originX, targetGroundY + 1, originZ),
                        worstDelta,
                        minSurface,
                        maxSurface,
                        movement,
                        score
                ));
            }
        }

        candidates.sort((a, b) -> Integer.compare(a.score(), b.score()));
        if (candidates.isEmpty()) {
            return "none within scan radius";
        }
        return candidates.stream().limit(6).map(RelocationCandidate::summary).toList().toString();
    }

    private static boolean intersectsFixedBuilding(
            BoundingBox candidate,
            PreparedBuilding subject,
            List<PreparedBuilding> buildings,
            List<PreparedBuilding> failedBuildings
    ) {
        for (PreparedBuilding other : buildings) {
            if (other == subject || failedBuildings.contains(other)) {
                continue;
            }
            if (intersectsXZ(candidate, other.bounds())) {
                return true;
            }
        }
        return false;
    }

    private static boolean intersectsPlaza(BoundingBox box) {
        return box.maxX() >= -311 && box.minX() <= -299
                && box.maxZ() >= -37 && box.minZ() <= -27;
    }

    private static boolean intersectsXZ(BoundingBox a, BoundingBox b) {
        return a.maxX() >= b.minX() && a.minX() <= b.maxX()
                && a.maxZ() >= b.minZ() && a.minZ() <= b.maxZ();
    }

    private record RelocationCandidate(
            BlockPos origin,
            int worstDelta,
            int minSurface,
            int maxSurface,
            int movement,
            int score
    ) {
        private String summary() {
            return origin + " range=" + minSurface + ".." + maxSurface
                    + " worst=" + worstDelta + " move=" + movement;
        }
    }
    private static void preflightDock(ServerLevel level, PreparedBuilding building) {
        BuildingSpec spec = building.spec();
        BlockPos streetConnector = StructureTemplate.transform(
                new BlockPos(5, 2, 0),
                Mirror.NONE,
                spec.rotation(),
                BlockPos.ZERO
        ).offset(spec.origin());
        level.getChunkAt(streetConnector);
        int streetSurface = terrainSurfaceY(level, streetConnector.getX(), streetConnector.getZ());
        if (Math.abs(streetSurface - streetConnector.getY()) > 3) {
            throw new IllegalStateException(
                    "Campfire harbor dock street connector misses canonical shoreline: connector="
                            + streetConnector + " surface=" + streetSurface
            );
        }

        BoundingBox box = building.bounds();
        int water = 0;
        int samples = 0;
        int westSampleMaxX = Math.min(box.maxX(), box.minX() + 4);
        for (int x = box.minX(); x <= westSampleMaxX; x += 2) {
            for (int z = box.minZ(); z <= box.maxZ(); z += 2) {
                level.getChunkAt(new BlockPos(x, 62, z));
                samples++;
                boolean hasFluid =
                        !level.getFluidState(new BlockPos(x, 62, z)).isEmpty()
                                || !level.getFluidState(new BlockPos(x, 63, z)).isEmpty();
                if (hasFluid) {
                    water++;
                }
            }
        }
        if (samples == 0 || water * 4 < samples) {
            throw new IllegalStateException(
                    "Campfire harbor dock outer pier is not sufficiently over water: waterSamples="
                            + water + "/" + samples + " bounds=" + box
            );
        }

        CampfireSessions.LOGGER.info(
                "Campfire harbor dock preflight: street={} surface={} outer-water={}/{} bounds={}",
                streetConnector,
                streetSurface,
                water,
                samples,
                box
        );
    }

    private static void place(ServerLevel level, PreparedBuilding building) {
        BuildingSpec spec = building.spec();
        if (spec.gradeFootprint()) {
            gradeFootprint(level, building.bounds(), spec.origin().getY());
        }

        boolean placed = building.template().placeInWorld(
                level,
                spec.origin(),
                spec.origin(),
                building.settings(),
                level.getRandom(),
                Block.UPDATE_ALL
        );
        if (!placed) {
            throw new IllegalStateException("Failed to place Campfire village review structure " + spec.role());
        }

        CampfireSessions.LOGGER.info(
                "Campfire village review placed {} at {} structure={} rotation={} bounds={}",
                spec.role(),
                spec.origin(),
                spec.structureId(),
                spec.rotation(),
                building.bounds()
        );
    }

    private static void gradeFootprint(ServerLevel level, BoundingBox box, int buildingY) {
        int targetGroundY = buildingY - 1;
        int clearTop = box.maxY() + 2;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int surface = terrainSurfaceY(level, x, z);

                if (surface < targetGroundY) {
                    for (int y = surface + 1; y < targetGroundY; y++) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }

                level.setBlock(
                        new BlockPos(x, targetGroundY, z),
                        Blocks.GRASS_BLOCK.defaultBlockState(),
                        Block.UPDATE_ALL
                );

                for (int y = buildingY; y <= Math.max(clearTop, surface + 1); y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static int terrainSurfaceY(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        int floor = Math.max(level.getMinY(), y - 32);
        while (y > floor) {
            var state = level.getBlockState(new BlockPos(x, y, z));
            if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
                return y;
            }
            y--;
        }
        return y;
    }

    private static StructureTemplate template(ServerLevel level, BuildingSpec spec) {
        return level.getStructureManager().get(spec.structureId())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing packaged Campfire village structure " + spec.structureId()
                ));
    }

    private static StructurePlaceSettings settings(BuildingSpec spec) {
        return new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(spec.rotation())
                .setIgnoreEntities(true)
                .addProcessor(REVIEW_MARKER_PROCESSOR);
    }

    private static BuildingSpec kogtyv(
            String role,
            String suffix,
            int x,
            int y,
            int z,
            int sizeX,
            int sizeY,
            int sizeZ
    ) {
        return external(
                role,
                "external/kogtyv_greece/" + suffix,
                x, y, z,
                Rotation.NONE,
                sizeX, sizeY, sizeZ,
                true
        );
    }

    private static BuildingSpec external(
            String role,
            String structurePath,
            int x,
            int y,
            int z,
            Rotation rotation,
            int sizeX,
            int sizeY,
            int sizeZ,
            boolean gradeFootprint
    ) {
        return new BuildingSpec(
                role,
                Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, structurePath),
                new BlockPos(x, y, z),
                rotation,
                new Vec3i(sizeX, sizeY, sizeZ),
                gradeFootprint
        );
    }

    private record BuildingSpec(
            String role,
            Identifier structureId,
            BlockPos origin,
            Rotation rotation,
            Vec3i expectedSize,
            boolean gradeFootprint
    ) {}

    private record PreparedBuilding(
            BuildingSpec spec,
            StructureTemplate template,
            StructurePlaceSettings settings,
            BoundingBox bounds
    ) {}
}
