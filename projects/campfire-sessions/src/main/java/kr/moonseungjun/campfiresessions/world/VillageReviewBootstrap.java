package kr.moonseungjun.campfiresessions.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import kr.moonseungjun.campfiresessions.CampfireSessions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
 * Development-only first physical layout pass on the canonical Campfire island.
 *
 * <p>The review bootstrap is intentionally inert in ordinary gameplay. It exists
 * to place the real MIT Kogtyv Greece structures on the verified Geming400
 * terrain before the first user-facing map/building playtest.</p>
 */
public final class VillageReviewBootstrap {
    private static final String PROPERTY = "campfiresessions.villageReview";
    private static final String MARKER_NAME = ".campfiresessions-village-review-v2";
    private static final int MAX_GRADE_DELTA = 8;

    private static final BlockIgnoreProcessor REVIEW_MARKER_PROCESSOR = new BlockIgnoreProcessor(List.of(
            Blocks.JIGSAW,
            Blocks.BARRIER,
            Blocks.STRUCTURE_BLOCK,
            Blocks.STRUCTURE_VOID
    ));

    /**
     * Origins are placement origins, not conceptual building centers.
     * Y values come from the successful canonical-world terrain probe #15.
     * Rotation stays NONE in this first physical pass so entrance direction can
     * be judged from the real structures rather than guessed from filenames.
     */
    private static final List<BuildingSpec> BUILDINGS = List.of(
            spec("resident_services", "house/shop_triple_2", -312, 71, -19, 13, 16, 8),
            spec("general_store", "house/shop_triple_1", -290, 72, -46, 13, 16, 8),
            spec("clinic", "house/shop_medium_3", -321, 67, -12, 7, 16, 8),
            spec("cafe", "house/shop_medium_1", -296, 73, -28, 7, 16, 8),
            spec("clothing_shop", "house/shop_medium_2", -291, 74, -11, 7, 16, 8),
            spec("museum", "center/ratush_1", -284, 76, -32, 21, 8, 16),
            spec("harbor_service", "house/shop_small_1", -334, 64, -48, 5, 16, 7),

            // First housing-scale pass: real external shells, not placeholders.
            spec("player_house_stage_1", "house/small_1", -322, 66, -72, 5, 16, 5),
            spec("resident_house_south_1", "house/small_2", -312, 67, -72, 5, 16, 5),
            spec("resident_house_south_2", "house/medium_1", -322, 68, -60, 7, 16, 6),
            spec("resident_house_east_1", "house/small_3", -283, 71, -62, 5, 16, 5),
            spec("resident_house_north_1", "house/medium_2", -300, 71, 6, 7, 16, 6)
    );

    private VillageReviewBootstrap() {}

    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        if (!enabled()) {
            return;
        }

        ServerLevel level = event.getServer().overworld();
        Path marker = event.getServer().getWorldPath(LevelResource.ROOT).resolve(MARKER_NAME);
        if (Files.exists(marker)) {
            CampfireSessions.LOGGER.info("Campfire village review layout already applied: {}", marker);
            return;
        }

        preflight(level);

        for (BuildingSpec spec : BUILDINGS) {
            place(level, spec);
        }

        try {
            Files.writeString(
                    marker,
                    "Campfire Sessions village review v2\n"
                            + "canonical terrain probe: run 36806856318\n"
                            + "buildings: " + BUILDINGS.size() + "\n"
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write Campfire village review marker " + marker, exception);
        }

        CampfireSessions.LOGGER.info(
                "Campfire village review layout applied: {} real Kogtyv Greece structures",
                BUILDINGS.size()
        );
    }

    private static void preflight(ServerLevel level) {
        for (BuildingSpec spec : BUILDINGS) {
            StructureTemplate template = template(level, spec);
            Vec3i actual = template.getSize();
            if (!actual.equals(spec.expectedSize())) {
                throw new IllegalStateException(
                        "Campfire village review structure size changed for " + spec.role()
                                + ": expected=" + spec.expectedSize() + " actual=" + actual
                );
            }

            StructurePlaceSettings settings = settings(spec);
            BoundingBox box = template.getBoundingBox(settings, spec.origin());
            int targetGroundY = spec.origin().getY() - 1;
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    level.getChunkAt(new BlockPos(x, targetGroundY, z));
                    int currentSurface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                    if (Math.abs(currentSurface - targetGroundY) > MAX_GRADE_DELTA) {
                        throw new IllegalStateException(
                                "Campfire village review grading limit exceeded for " + spec.role()
                                        + " at " + x + "," + z
                                        + ": surface=" + currentSurface + " target=" + targetGroundY
                        );
                    }
                }
            }
        }
    }

    private static void place(ServerLevel level, BuildingSpec spec) {
        StructureTemplate template = template(level, spec);
        StructurePlaceSettings settings = settings(spec);
        BoundingBox box = template.getBoundingBox(settings, spec.origin());

        gradeFootprint(level, box, spec.origin().getY());
        boolean placed = template.placeInWorld(
                level,
                spec.origin(),
                spec.origin(),
                settings,
                level.getRandom(),
                Block.UPDATE_ALL
        );
        if (!placed) {
            throw new IllegalStateException("Failed to place Campfire village review structure " + spec.role());
        }

        CampfireSessions.LOGGER.info(
                "Campfire village review placed {} at {} structure={} bounds={}",
                spec.role(),
                spec.origin(),
                spec.structureId(),
                box
        );
    }

    private static void gradeFootprint(ServerLevel level, BoundingBox box, int buildingY) {
        int targetGroundY = buildingY - 1;
        int clearTop = box.maxY() + 2;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

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

    private static BuildingSpec spec(
            String role,
            String suffix,
            int x,
            int y,
            int z,
            int sizeX,
            int sizeY,
            int sizeZ
    ) {
        return new BuildingSpec(
                role,
                Identifier.fromNamespaceAndPath(
                        CampfireSessions.MOD_ID,
                        "external/kogtyv_greece/" + suffix
                ),
                new BlockPos(x, y, z),
                Rotation.NONE,
                new Vec3i(sizeX, sizeY, sizeZ)
        );
    }

    private record BuildingSpec(
            String role,
            Identifier structureId,
            BlockPos origin,
            Rotation rotation,
            Vec3i expectedSize
    ) {}
}
