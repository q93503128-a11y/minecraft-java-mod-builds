package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MythicTargetPayload(double x, double z) implements CustomPacketPayload {
    public static final Type<MythicTargetPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mythic_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MythicTargetPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeDouble(payload.x());
                buf.writeDouble(payload.z());
            },
            buf -> new MythicTargetPayload(buf.readDouble(), buf.readDouble()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
