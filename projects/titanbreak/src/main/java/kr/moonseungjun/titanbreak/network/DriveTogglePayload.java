package kr.moonseungjun.titanbreak.network;

import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DriveTogglePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<DriveTogglePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "drive_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DriveTogglePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, DriveTogglePayload::enabled, DriveTogglePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
