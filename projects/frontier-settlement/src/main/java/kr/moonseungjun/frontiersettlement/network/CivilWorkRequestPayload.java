package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CivilWorkRequestPayload(int nonce,
                                      int firstX, int firstY, int firstZ,
                                      int secondX, int secondY, int secondZ,
                                      boolean confirm) implements CustomPacketPayload {
    public static final Type<CivilWorkRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "civil_work_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CivilWorkRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.nonce());
                buf.writeInt(payload.firstX()); buf.writeInt(payload.firstY()); buf.writeInt(payload.firstZ());
                buf.writeInt(payload.secondX()); buf.writeInt(payload.secondY()); buf.writeInt(payload.secondZ());
                buf.writeBoolean(payload.confirm());
            },
            buf -> new CivilWorkRequestPayload(buf.readVarInt(),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readBoolean())
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
