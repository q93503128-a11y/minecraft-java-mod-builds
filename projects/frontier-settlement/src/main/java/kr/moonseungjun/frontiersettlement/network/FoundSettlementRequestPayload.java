package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request to found the one shared settlement at the player's current server-authoritative position. */
public record FoundSettlementRequestPayload(boolean confirm) implements CustomPacketPayload {
    public static final Type<FoundSettlementRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "found_settlement_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FoundSettlementRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.confirm()),
            buf -> new FoundSettlementRequestPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
