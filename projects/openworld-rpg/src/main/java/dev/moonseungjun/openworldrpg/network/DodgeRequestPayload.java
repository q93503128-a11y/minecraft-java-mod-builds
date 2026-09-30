package dev.moonseungjun.openworldrpg.network;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client directional intent only; the server owns distance, timing, cost and acceptance. */
public record DodgeRequestPayload(
        float forwardIntent,
        float strafeIntent,
        long sequence
) implements CustomPacketPayload {
    public static final Type<DodgeRequestPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "dodge_request"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            DodgeRequestPayload
            > CODEC = StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    DodgeRequestPayload::forwardIntent,
                    ByteBufCodecs.FLOAT,
                    DodgeRequestPayload::strafeIntent,
                    ByteBufCodecs.VAR_LONG,
                    DodgeRequestPayload::sequence,
                    DodgeRequestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
