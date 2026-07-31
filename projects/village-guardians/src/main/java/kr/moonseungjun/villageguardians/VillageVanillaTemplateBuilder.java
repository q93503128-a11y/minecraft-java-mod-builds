package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

final class VillageVanillaTemplateBuilder {
    private static final List<Block> DOOR_BLOCKS = BuiltInRegistries.BLOCK.stream()
            .filter(block -> block.defaultBlockState().is(BlockTags.DOORS))
            .toList();

    private VillageVanillaTemplateBuilder() {
    }

    static boolean build(
            ServerLevel level,
            BlockPos footprintOrigin,
            int groundY,
            VillageBuildingCatalog.Spec spec) {
        if (place(level, footprintOrigin, groundY, spec, spec.templateId())) {
            return true;
        }
        return place(level, footprintOrigin, groundY, spec, spec.fallbackTemplateId());
    }

    private static boolean place(
            ServerLevel level,
            BlockPos footprintOrigin,
            int groundY,
            VillageBuildingCatalog.Spec spec,
            String templateId) {
        if (templateId.isBlank()) {
            return false;
        }
        Identifier id = Identifier.tryParse(templateId);
        if (id == null) {
            return false;
        }
        StructureTemplate template = level.getStructureManager().get(id).orElse(null);
        if (template == null) {
            return false;
        }

        Rotation rotation = chooseRotation(template, spec);
        StructurePlaceSettings settings = settings(rotation);
        BoundingBox localBounds = template.getBoundingBox(settings, BlockPos.ZERO);
        int rotatedWidth = localBounds.maxX() - localBounds.minX() + 1;
        int rotatedHeight = localBounds.maxY() - localBounds.minY() + 1;
        int rotatedDepth = localBounds.maxZ() - localBounds.minZ() + 1;
        if (rotatedWidth > spec.width()
                || rotatedDepth > spec.depth()
                || rotatedHeight > spec.height()) {
            return false;
        }

        int offsetX = Math.max(0, (spec.width() - rotatedWidth) / 2) - localBounds.minX();
        int offsetZ = Math.max(0, (spec.depth() - rotatedDepth) / 2) - localBounds.minZ();
        BlockPos placement = new BlockPos(
                footprintOrigin.getX() + offsetX,
                groundY + 1,
                footprintOrigin.getZ() + offsetZ);
        return template.placeInWorld(level, placement, placement, settings, level.getRandom(), 2);
    }

    private static Rotation chooseRotation(
            StructureTemplate template,
            VillageBuildingCatalog.Spec spec) {
        Rotation selected = spec.rotation();
        int selectedScore = Integer.MIN_VALUE;
        for (Rotation rotation : Rotation.values()) {
            StructurePlaceSettings settings = settings(rotation);
            BoundingBox bounds = template.getBoundingBox(settings, BlockPos.ZERO);
            int width = bounds.maxX() - bounds.minX() + 1;
            int height = bounds.maxY() - bounds.minY() + 1;
            int depth = bounds.maxZ() - bounds.minZ() + 1;
            if (width > spec.width() || depth > spec.depth() || height > spec.height()) {
                continue;
            }

            int score = rotation == spec.rotation() ? 1 : 0;
            int centerX2 = bounds.minX() + bounds.maxX();
            int centerZ2 = bounds.minZ() + bounds.maxZ();
            Direction front = spec.entranceFacing();
            Direction sideways = front.getClockWise();
            boolean foundDoor = false;
            for (Block door : DOOR_BLOCKS) {
                for (StructureTemplate.StructureBlockInfo info
                        : template.filterBlocks(BlockPos.ZERO, settings, door)) {
                    BlockPos pos = info.pos();
                    int relativeX2 = pos.getX() * 2 - centerX2;
                    int relativeZ2 = pos.getZ() * 2 - centerZ2;
                    int projection = relativeX2 * front.getStepX()
                            + relativeZ2 * front.getStepZ();
                    int lateral = Math.abs(relativeX2 * sideways.getStepX()
                            + relativeZ2 * sideways.getStepZ());
                    score = Math.max(score, 10_000 + projection * 100 - lateral * 3);
                    foundDoor = true;
                }
            }
            if (!foundDoor) {
                score -= 1_000;
            }
            if (score > selectedScore) {
                selectedScore = score;
                selected = rotation;
            }
        }
        return selected;
    }

    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setIgnoreEntities(true)
                .setKnownShape(true)
                .addProcessor(new BlockIgnoreProcessor(List.of(
                        Blocks.STRUCTURE_BLOCK,
                        Blocks.STRUCTURE_VOID,
                        Blocks.JIGSAW)));
    }
}
