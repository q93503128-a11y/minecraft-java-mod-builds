package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** One attack intent; the server raycasts from the actual controlled host body. */
public record ParasiteHostAttackPayload() implements CustomPacketPayload {
    public static final ParasiteHostAttackPayload INSTANCE = new ParasiteHostAttackPayload();
    public static final Type<ParasiteHostAttackPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "parasite_host_attack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ParasiteHostAttackPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
