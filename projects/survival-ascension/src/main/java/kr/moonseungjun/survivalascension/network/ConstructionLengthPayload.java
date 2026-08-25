package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConstructionLengthPayload() implements CustomPacketPayload {
    public static final Type<ConstructionLengthPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "construction_length"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConstructionLengthPayload> CODEC = StreamCodec.unit(new ConstructionLengthPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
