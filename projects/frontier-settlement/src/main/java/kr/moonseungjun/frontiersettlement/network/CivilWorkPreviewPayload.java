package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import kr.moonseungjun.frontiersettlement.settlement.SettlementCivilWorkService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CivilWorkPreviewPayload(int nonce, boolean valid, boolean confirmed,
                                      int minX, int maxX, int minZ, int maxZ, int gradeY,
                                      int cutBlocks, int fillBlocks, String message)
        implements CustomPacketPayload {
    public static final Type<CivilWorkPreviewPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "civil_work_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CivilWorkPreviewPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.nonce());
                buf.writeBoolean(payload.valid());
                buf.writeBoolean(payload.confirmed());
                buf.writeInt(payload.minX()); buf.writeInt(payload.maxX());
                buf.writeInt(payload.minZ()); buf.writeInt(payload.maxZ());
                buf.writeInt(payload.gradeY());
                buf.writeVarInt(payload.cutBlocks()); buf.writeVarInt(payload.fillBlocks());
                buf.writeUtf(payload.message(), 256);
            },
            buf -> new CivilWorkPreviewPayload(buf.readVarInt(), buf.readBoolean(), buf.readBoolean(),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readUtf(256))
    );

    public static CivilWorkPreviewPayload fromCheck(int nonce, SettlementCivilWorkService.Check check, boolean confirmed) {
        return new CivilWorkPreviewPayload(nonce, check.valid(), confirmed,
                check.minX(), check.maxX(), check.minZ(), check.maxZ(), check.gradeY(),
                check.cutBlocks(), check.fillBlocks(), check.message());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
