package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record OutpostRecord(int id,
                            int centerX, int centerY, int centerZ,
                            int stockX, int stockY, int stockZ,
                            int roadIndex) {
    public static final Codec<OutpostRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(OutpostRecord::id),
            Codec.INT.fieldOf("center_x").forGetter(OutpostRecord::centerX),
            Codec.INT.fieldOf("center_y").forGetter(OutpostRecord::centerY),
            Codec.INT.fieldOf("center_z").forGetter(OutpostRecord::centerZ),
            Codec.INT.fieldOf("stock_x").forGetter(OutpostRecord::stockX),
            Codec.INT.fieldOf("stock_y").forGetter(OutpostRecord::stockY),
            Codec.INT.fieldOf("stock_z").forGetter(OutpostRecord::stockZ),
            Codec.INT.fieldOf("road_index").forGetter(OutpostRecord::roadIndex)
    ).apply(instance, OutpostRecord::new));

    public BlockPos center() {
        return new BlockPos(centerX, centerY, centerZ);
    }

    public BlockPos stockpile() {
        return new BlockPos(stockX, stockY, stockZ);
    }

    public boolean protectsXZ(BlockPos pos, int padding) {
        return Math.abs(pos.getX() - centerX) <= 5 + padding
                && Math.abs(pos.getZ() - centerZ) <= 5 + padding;
    }
}
