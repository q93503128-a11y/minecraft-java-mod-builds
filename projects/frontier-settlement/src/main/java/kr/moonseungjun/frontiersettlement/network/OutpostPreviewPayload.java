package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import kr.moonseungjun.frontiersettlement.settlement.SettlementOutpostService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OutpostPreviewPayload(int nonce, boolean valid, boolean confirmed,
                                    int roadIndex,
                                    int gateX, int gateY, int gateZ,
                                    int directionX, int directionZ,
                                    String specialization, String message)
        implements CustomPacketPayload {
    public static final Type<OutpostPreviewPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "outpost_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OutpostPreviewPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.nonce());
                buf.writeBoolean(payload.valid());
                buf.writeBoolean(payload.confirmed());
                buf.writeVarInt(payload.roadIndex() + 1);
                buf.writeInt(payload.gateX());
                buf.writeInt(payload.gateY());
                buf.writeInt(payload.gateZ());
                buf.writeInt(payload.directionX());
                buf.writeInt(payload.directionZ());
                buf.writeUtf(payload.specialization(), 32);
                buf.writeUtf(payload.message(), 256);
            },
            buf -> new OutpostPreviewPayload(
                    buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt() - 1,
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readUtf(32), buf.readUtf(256))
    );

    public static OutpostPreviewPayload fromCheck(int nonce, SettlementOutpostService.PlacementCheck check, boolean confirmed) {
        BlockPos gate = check.gate();
        return new OutpostPreviewPayload(nonce, check.valid(), confirmed, check.roadIndex(),
                gate.getX(), gate.getY(), gate.getZ(), check.directionX(), check.directionZ(),
                check.specialization(), check.message());
    }

    public BlockPos gate() {
        return new BlockPos(gateX, gateY, gateZ);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
