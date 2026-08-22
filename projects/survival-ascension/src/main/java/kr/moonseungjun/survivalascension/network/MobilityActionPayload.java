package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MobilityActionPayload() implements CustomPacketPayload {
    public static final Type<MobilityActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mobility_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MobilityActionPayload> CODEC = StreamCodec.unit(new MobilityActionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
