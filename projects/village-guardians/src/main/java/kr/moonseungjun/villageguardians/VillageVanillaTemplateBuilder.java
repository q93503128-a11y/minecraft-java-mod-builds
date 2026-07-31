package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
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
        if (spec.templateId().isBlank()) {
            return false;
        }
        Identifier id = Identifier.tryParse(spec.templateId());
        if (id == null) {
            return false;
        }
        StructureTemplate template = level.getStructureManager().get(id).orElse(null);
        if (template == null) {
            return false;
        }

        Vec3i size = template.getSize();
        int offsetX = Math.max(0, (spec.width() - size.getX()) / 2);
        int offsetZ = Math.max(0, (spec.depth() - size.getZ()) / 2);
        BlockPos placement = new BlockPos(
                footprintOrigin.getX() + offsetX,
                groundY + 1,
                footprintOrigin.getZ() + offsetZ);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .setKnownShape(true)
                .addProcessor(new BlockIgnoreProcessor(List.of(
                        Blocks.STRUCTURE_BLOCK,
                        Blocks.STRUCTURE_VOID,
                        Blocks.JIGSAW)));
        return template.placeInWorld(level, placement, placement, settings, level.getRandom(), 2);
    }
}
