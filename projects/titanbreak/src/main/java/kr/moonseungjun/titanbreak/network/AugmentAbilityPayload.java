package kr.moonseungjun.titanbreak.network;

import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AugmentAbilityPayload(int ability) implements CustomPacketPayload {
    public static final int HOOK = 1;
    public static final int PHASE_STEP = 2;
    public static final int ARM_RIGHT = 3;
    public static final int ARM_LEFT = 4;
    public static final int LEG_JUMP = 5;
    public static final Type<AugmentAbilityPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "augment_ability"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AugmentAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AugmentAbilityPayload::ability, AugmentAbilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
