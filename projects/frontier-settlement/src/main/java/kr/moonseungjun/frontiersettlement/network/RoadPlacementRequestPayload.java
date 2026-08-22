package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RoadPlacementRequestPayload(int nonce,
                                          int startX, int startY, int startZ,
                                          int endX, int endY, int endZ,
                                          boolean confirm)
        implements CustomPacketPayload {
    public static final Type<RoadPlacementRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "road_placement_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoadPlacementRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.nonce());
                buf.writeInt(payload.startX());
                buf.writeInt(payload.startY());
                buf.writeInt(payload.startZ());
                buf.writeInt(payload.endX());
                buf.writeInt(payload.endY());
                buf.writeInt(payload.endZ());
                buf.writeBoolean(payload.confirm());
            },
            buf -> new RoadPlacementRequestPayload(
                    buf.readVarInt(),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
