package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConstructionModePayload(String modeId) implements CustomPacketPayload {
    public static final Type<ConstructionModePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "construction_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConstructionModePayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.modeId()),
            buf -> new ConstructionModePayload(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
