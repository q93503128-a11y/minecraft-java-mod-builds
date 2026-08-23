package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SettlementSnapshotPayload(boolean founded, long wood, long stone, long metal, long food,
                                        int population, String tier, int buildingUnlockMask)
        implements CustomPacketPayload {
    public static final Type<SettlementSnapshotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "settlement_snapshot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SettlementSnapshotPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.founded());
                buf.writeVarLong(payload.wood());
                buf.writeVarLong(payload.stone());
                buf.writeVarLong(payload.metal());
                buf.writeVarLong(payload.food());
                buf.writeVarInt(payload.population());
                buf.writeUtf(payload.tier());
                buf.writeVarInt(payload.buildingUnlockMask());
            },
            buf -> new SettlementSnapshotPayload(
                    buf.readBoolean(),
                    buf.readVarLong(),
                    buf.readVarLong(),
                    buf.readVarLong(),
                    buf.readVarLong(),
                    buf.readVarInt(),
                    buf.readUtf(),
                    buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
