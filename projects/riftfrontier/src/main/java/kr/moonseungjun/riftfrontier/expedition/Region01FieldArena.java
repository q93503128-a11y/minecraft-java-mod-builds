package kr.moonseungjun.riftfrontier.expedition;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Minecraft materializer for the pure Region 01 field-layout contract.
 *
 * It uses only vanilla runtime blocks. No third-party asset bytes are added. Tuff-family material and staggered
 * cover are a Trial-Chambers-informed field-review baseline for navigation/readability, not final Riftfrontier art.
 */
public final class Region01FieldArena {
    private Region01FieldArena() {}

    public static void materialize(ServerLevel level, BlockPos center) {
        for (Region01FieldArenaPlan.FloorCell cell : Region01FieldArenaPlan.floorCells()) {
            var state = switch (cell.role()) {
                case COMBAT_FIELD -> Blocks.TUFF_BRICKS.defaultBlockState();
                case OPEN_LANE -> Blocks.POLISHED_TUFF.defaultBlockState();
            };
            level.setBlockAndUpdate(center.offset(cell.dx(), -1, cell.dz()), state);
        }

        for (Region01FieldArenaPlan.CoverPillar pillar : Region01FieldArenaPlan.coverPillars()) {
            for (int dy = 0; dy < pillar.height(); dy++) {
                level.setBlockAndUpdate(
                    center.offset(pillar.dx(), dy, pillar.dz()),
                    Blocks.TUFF_BRICKS.defaultBlockState()
                );
            }
        }
    }
}
