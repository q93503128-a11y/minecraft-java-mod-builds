package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record RoadSegment(int startX, int startY, int startZ,
                          int directionX, int directionZ, int length) {
    public static final Codec<RoadSegment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("start_x").forGetter(RoadSegment::startX),
            Codec.INT.fieldOf("start_y").forGetter(RoadSegment::startY),
            Codec.INT.fieldOf("start_z").forGetter(RoadSegment::startZ),
            Codec.INT.fieldOf("direction_x").forGetter(RoadSegment::directionX),
            Codec.INT.fieldOf("direction_z").forGetter(RoadSegment::directionZ),
            Codec.INT.fieldOf("length").forGetter(RoadSegment::length)
    ).apply(instance, RoadSegment::new));

    public BlockPos start() {
        return new BlockPos(startX, startY, startZ);
    }

    public BlockPos end() {
        int last = Math.max(0, length - 1);
        return new BlockPos(startX + directionX * last, startY, startZ + directionZ * last);
    }

    public boolean containsXZ(BlockPos pos) {
        int relX = pos.getX() - startX;
        int relZ = pos.getZ() - startZ;
        int along = relX * directionX + relZ * directionZ;
        int side = relX * -directionZ + relZ * directionX;
        return along >= 0 && along < length && Math.abs(side) <= 1;
    }
}
