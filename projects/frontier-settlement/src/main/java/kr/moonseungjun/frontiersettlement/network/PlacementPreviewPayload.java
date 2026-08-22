package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlacementPreviewPayload(int nonce, String buildingType,
                                      boolean valid, boolean confirmed,
                                      int originX, int originY, int originZ,
                                      int rotation, String message)
        implements CustomPacketPayload {
    public static final Type<PlacementPreviewPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "placement_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacementPreviewPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.nonce());
                buf.writeUtf(payload.buildingType(), 64);
                buf.writeBoolean(payload.valid());
                buf.writeBoolean(payload.confirmed());
                buf.writeInt(payload.originX());
                buf.writeInt(payload.originY());
                buf.writeInt(payload.originZ());
                buf.writeVarInt(payload.rotation());
                buf.writeUtf(payload.message(), 256);
            },
            buf -> new PlacementPreviewPayload(
                    buf.readVarInt(), buf.readUtf(64),
                    buf.readBoolean(), buf.readBoolean(),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readVarInt(), buf.readUtf(256))
    );

    public BlockPos origin() {
        return new BlockPos(originX, originY, originZ);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
