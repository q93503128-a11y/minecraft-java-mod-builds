package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record BuildingRecord(String type, int originX, int originY, int originZ) {
    public static final Codec<BuildingRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("type").forGetter(BuildingRecord::type),
            Codec.INT.fieldOf("origin_x").forGetter(BuildingRecord::originX),
            Codec.INT.fieldOf("origin_y").forGetter(BuildingRecord::originY),
            Codec.INT.fieldOf("origin_z").forGetter(BuildingRecord::originZ)
    ).apply(instance, BuildingRecord::new));

    public BlockPos origin() {
        return new BlockPos(originX, originY, originZ);
    }

    public BuildingType buildingType() {
        return BuildingType.fromId(type);
    }

    public BlockPos workCenter() {
        BuildingType resolved = buildingType();
        if (resolved == null) return origin();
        return origin().offset(resolved.width() / 2, 1, resolved.depth() / 2);
    }

    public boolean protectsXZ(BlockPos pos, int padding) {
        BuildingType resolved = buildingType();
        if (resolved == null) return false;
        return pos.getX() >= originX - padding
                && pos.getX() < originX + resolved.width() + padding
                && pos.getZ() >= originZ - padding
                && pos.getZ() < originZ + resolved.depth() + padding;
    }
}
