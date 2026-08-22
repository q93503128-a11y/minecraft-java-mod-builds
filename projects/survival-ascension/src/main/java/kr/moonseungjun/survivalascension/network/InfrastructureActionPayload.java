package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record InfrastructureActionPayload(String projectId, String action) implements CustomPacketPayload {
    public static final Type<InfrastructureActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "infrastructure_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfrastructureActionPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.projectId());
                buf.writeUtf(payload.action());
            },
            buf -> new InfrastructureActionPayload(buf.readUtf(), buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
