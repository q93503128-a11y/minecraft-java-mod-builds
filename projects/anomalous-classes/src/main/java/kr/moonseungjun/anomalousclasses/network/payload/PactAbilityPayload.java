package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Only a slot intent. Player identity, contract, cooldown and target are server-resolved. */
public record PactAbilityPayload(int action) implements CustomPacketPayload {
    public static final Type<PactAbilityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID,"pact_ability"));
    public static final StreamCodec<ByteBuf,PactAbilityPayload> STREAM_CODEC = StreamCodec.of(
            (buffer,value)->ByteBufCodecs.VAR_INT.encode(buffer,value.action),
            buffer->new PactAbilityPayload(ByteBufCodecs.VAR_INT.decode(buffer)));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
