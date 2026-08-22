package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlacementRequestPayload(int nonce, String buildingType,
                                      int centerX, int centerY, int centerZ,
                                      int rotation, boolean confirm)
        implements CustomPacketPayload {
    public static final Type<PlacementRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "placement_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacementRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.nonce());
                buf.writeUtf(payload.buildingType(), 64);
                buf.writeInt(payload.centerX());
                buf.writeInt(payload.centerY());
                buf.writeInt(payload.centerZ());
                buf.writeVarInt(payload.rotation());
                buf.writeBoolean(payload.confirm());
            },
            buf -> new PlacementRequestPayload(
                    buf.readVarInt(), buf.readUtf(64),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readVarInt(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
