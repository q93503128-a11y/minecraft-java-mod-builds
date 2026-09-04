package kr.moonseungjun.livingkingdoms.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

/** Persisted proof of the actual authored room assigned to a player. */
public record ResidenceAssignment(
        int revision,
        int x,
        int y,
        int z,
        float yaw,
        int buildingX,
        int buildingZ
) {
    public static final int CURRENT_REVISION = 1;

    public static final Codec<ResidenceAssignment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(ResidenceAssignment::revision),
            Codec.INT.fieldOf("x").forGetter(ResidenceAssignment::x),
            Codec.INT.fieldOf("y").forGetter(ResidenceAssignment::y),
            Codec.INT.fieldOf("z").forGetter(ResidenceAssignment::z),
            Codec.FLOAT.optionalFieldOf("yaw", 180.0F).forGetter(ResidenceAssignment::yaw),
            Codec.INT.optionalFieldOf("building_x", 0).forGetter(ResidenceAssignment::buildingX),
            Codec.INT.optionalFieldOf("building_z", 0).forGetter(ResidenceAssignment::buildingZ)
    ).apply(instance, ResidenceAssignment::new));

    public ResidenceAssignment {
        if (revision < 0) throw new IllegalArgumentException("Negative residence assignment revision");
        if (y < -64 || y > 512) throw new IllegalArgumentException("Unsafe residence assignment Y: " + y);
        if (!Float.isFinite(yaw)) yaw = 180.0F;
    }

    public BlockPos position() {
        return new BlockPos(x, y, z);
    }

    public boolean current() {
        return revision >= CURRENT_REVISION;
    }
}
