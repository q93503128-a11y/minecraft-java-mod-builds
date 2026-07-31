package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

final class VillageVanillaTemplateBuilder {
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

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(spec.rotation())
                .setIgnoreEntities(true)
                .setKnownShape(true)
                .addProcessor(new BlockIgnoreProcessor(List.of(
                        Blocks.STRUCTURE_BLOCK,
                        Blocks.STRUCTURE_VOID,
                        Blocks.JIGSAW)));

        BoundingBox localBounds = template.getBoundingBox(settings, BlockPos.ZERO);
        int rotatedWidth = localBounds.maxX() - localBounds.minX() + 1;
        int rotatedDepth = localBounds.maxZ() - localBounds.minZ() + 1;
        int offsetX = Math.max(0, (spec.width() - rotatedWidth) / 2) - localBounds.minX();
        int offsetZ = Math.max(0, (spec.depth() - rotatedDepth) / 2) - localBounds.minZ();
        BlockPos placement = new BlockPos(
                footprintOrigin.getX() + offsetX,
                groundY + 1,
                footprintOrigin.getZ() + offsetZ);
        return template.placeInWorld(level, placement, placement, settings, level.getRandom(), 2);
    }
}
