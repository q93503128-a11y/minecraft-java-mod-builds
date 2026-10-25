package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Local prediction state for the collision-changing Phase Shift active. */
public record PhaseStatePayload(boolean phaseShiftActive) implements CustomPacketPayload {
    public static final Type<PhaseStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "phase_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PhaseStatePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    PhaseStatePayload::phaseShiftActive,
                    PhaseStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
