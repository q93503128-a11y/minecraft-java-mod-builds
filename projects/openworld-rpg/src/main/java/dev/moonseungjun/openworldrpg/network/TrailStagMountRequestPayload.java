package dev.moonseungjun.openworldrpg.network;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client mount-button intent only. The server owns all summon timing, placement, and gameplay state. */
public record TrailStagMountRequestPayload(
        long sequence,
        int protocolVersion
) implements CustomPacketPayload {
    public static final int CURRENT_PROTOCOL_VERSION = 1;

    public static final Type<TrailStagMountRequestPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "trail_stag_mount_request"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            TrailStagMountRequestPayload
            > CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    TrailStagMountRequestPayload::sequence,
                    ByteBufCodecs.VAR_INT,
                    TrailStagMountRequestPayload::protocolVersion,
                    TrailStagMountRequestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
