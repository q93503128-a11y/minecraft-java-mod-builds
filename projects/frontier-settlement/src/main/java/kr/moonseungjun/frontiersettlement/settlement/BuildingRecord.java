package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record BuildingRecord(String type, int originX, int originY, int originZ, int rotation) {
    public static final Codec<BuildingRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("type").forGetter(BuildingRecord::type),
            Codec.INT.fieldOf("origin_x").forGetter(BuildingRecord::originX),
            Codec.INT.fieldOf("origin_y").forGetter(BuildingRecord::originY),
            Codec.INT.fieldOf("origin_z").forGetter(BuildingRecord::originZ),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(BuildingRecord::rotation)
    ).apply(instance, BuildingRecord::new));

    public BuildingRecord(String type, int originX, int originY, int originZ) {
        this(type, originX, originY, originZ, 0);
    }

    public BlockPos origin() {
        return new BlockPos(originX, originY, originZ);
    }

    public BuildingType buildingType() {
        return BuildingType.fromId(type);
    }

    public BuildingRotation buildingRotation() {
        return BuildingRotation.fromId(rotation);
    }

    public int rotatedWidth() {
        BuildingType resolved = buildingType();
        return resolved == null ? 1 : buildingRotation().rotatedWidth(resolved);
    }

    public int rotatedDepth() {
        BuildingType resolved = buildingType();
        return resolved == null ? 1 : buildingRotation().rotatedDepth(resolved);
    }

    public BlockPos localToWorld(int localX, int localY, int localZ) {
        BuildingType resolved = buildingType();
        if (resolved == null) return origin().offset(localX, localY, localZ);
        BlockPos localAbsolute = origin().offset(localX, localY, localZ);
        return buildingRotation().rotateLocal(origin(), localAbsolute, resolved.width(), resolved.depth());
    }

    public BlockPos workCenter() {
        BuildingType resolved = buildingType();
        if (resolved == null) return origin();
        return localToWorld(resolved.width() / 2, 1, resolved.depth() / 2);
    }

    public boolean protectsXZ(BlockPos pos, int padding) {
        int width = rotatedWidth();
        int depth = rotatedDepth();
        return pos.getX() >= originX - padding
                && pos.getX() < originX + width + padding
                && pos.getZ() >= originZ - padding
                && pos.getZ() < originZ + depth + padding;
    }
}
