package dev.moonseungjun.testmod.network;

import dev.moonseungjun.testmod.TestMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CastRequestPayload() implements CustomPacketPayload {
    public static final Type<CastRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TestMod.MOD_ID, "cast_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastRequestPayload> CODEC = StreamCodec.unit(new CastRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
