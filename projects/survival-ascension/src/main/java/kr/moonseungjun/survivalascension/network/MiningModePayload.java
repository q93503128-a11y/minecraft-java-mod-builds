package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MiningModePayload(String modeId) implements CustomPacketPayload {
    public static final Type<MiningModePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mining_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MiningModePayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.modeId()),
            buf -> new MiningModePayload(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
