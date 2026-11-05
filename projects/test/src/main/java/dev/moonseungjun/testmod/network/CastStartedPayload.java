package dev.moonseungjun.testmod.network;

import dev.moonseungjun.testmod.TestMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record CastStartedPayload(UUID casterId) implements CustomPacketPayload {
    public static final Type<CastStartedPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TestMod.MOD_ID, "cast_started"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastStartedPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUUID(payload.casterId),
            buffer -> new CastStartedPayload(buffer.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
