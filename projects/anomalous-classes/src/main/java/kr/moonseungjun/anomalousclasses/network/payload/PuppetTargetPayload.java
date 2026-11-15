package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** R is an order, not an attack skill; it does not consume a cooldown. */
public record PuppetTargetPayload() implements CustomPacketPayload {
    public static final PuppetTargetPayload INSTANCE = new PuppetTargetPayload();
    public static final Type<PuppetTargetPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "puppet_target"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PuppetTargetPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
