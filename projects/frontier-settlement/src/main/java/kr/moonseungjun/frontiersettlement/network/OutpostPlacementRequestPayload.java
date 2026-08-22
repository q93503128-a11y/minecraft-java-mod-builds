package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OutpostPlacementRequestPayload(int nonce, int targetX, int targetY, int targetZ, boolean confirm)
        implements CustomPacketPayload {
    public static final Type<OutpostPlacementRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "outpost_placement_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OutpostPlacementRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.nonce());
                buf.writeInt(payload.targetX());
                buf.writeInt(payload.targetY());
                buf.writeInt(payload.targetZ());
                buf.writeBoolean(payload.confirm());
            },
            buf -> new OutpostPlacementRequestPayload(
                    buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
