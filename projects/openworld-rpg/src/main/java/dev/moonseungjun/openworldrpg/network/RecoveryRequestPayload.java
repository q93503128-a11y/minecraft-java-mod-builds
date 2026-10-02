package dev.moonseungjun.openworldrpg.network;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client intent only; Recovery Belt state, timing, eligibility and effects remain server-owned. */
public record RecoveryRequestPayload(
        long sequence
) implements CustomPacketPayload {
    public static final Type<RecoveryRequestPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "recovery_request"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            RecoveryRequestPayload
            > CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    RecoveryRequestPayload::sequence,
                    RecoveryRequestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
