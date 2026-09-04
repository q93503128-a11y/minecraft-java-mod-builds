package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ExpeditionSnapshotRequestPayload() implements CustomPacketPayload {
    public static final Type<ExpeditionSnapshotRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_snapshot_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExpeditionSnapshotRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> new ExpeditionSnapshotRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
