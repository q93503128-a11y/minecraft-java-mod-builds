package dev.moonseungjun.openworldrpg.network;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-confirmed dodge presentation event for the dodging player and tracking clients. */
public record DodgeAcceptedPayload(
        int entityId,
        int directionCode
) implements CustomPacketPayload {
    public static final Type<DodgeAcceptedPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "dodge_accepted"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            DodgeAcceptedPayload
            > CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    DodgeAcceptedPayload::entityId,
                    ByteBufCodecs.VAR_INT,
                    DodgeAcceptedPayload::directionCode,
                    DodgeAcceptedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
