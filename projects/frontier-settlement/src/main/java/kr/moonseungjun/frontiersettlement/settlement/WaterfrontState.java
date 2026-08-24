package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

/** Persisted local waterfront-work anchor/progress for one fishing outpost. */
public record WaterfrontState(int outpostId,
                              int bankX, int bankY, int bankZ,
                              int directionX, int directionZ,
                              int buildStep) {
    public static final Codec<WaterfrontState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("outpost_id").forGetter(WaterfrontState::outpostId),
            Codec.INT.fieldOf("bank_x").forGetter(WaterfrontState::bankX),
            Codec.INT.fieldOf("bank_y").forGetter(WaterfrontState::bankY),
            Codec.INT.fieldOf("bank_z").forGetter(WaterfrontState::bankZ),
            Codec.INT.fieldOf("direction_x").forGetter(WaterfrontState::directionX),
            Codec.INT.fieldOf("direction_z").forGetter(WaterfrontState::directionZ),
            Codec.INT.optionalFieldOf("build_step", 0).forGetter(WaterfrontState::buildStep)
    ).apply(instance, WaterfrontState::new));

    public WaterfrontState {
        buildStep = Math.max(0, buildStep);
        if (Math.abs(directionX) + Math.abs(directionZ) != 1) {
            directionX = 0;
            directionZ = 0;
        }
    }

    public BlockPos bank() {
        return new BlockPos(bankX, bankY, bankZ);
    }

    public WaterfrontState withBuildStep(int next) {
        return new WaterfrontState(outpostId, bankX, bankY, bankZ, directionX, directionZ, next);
    }
}
