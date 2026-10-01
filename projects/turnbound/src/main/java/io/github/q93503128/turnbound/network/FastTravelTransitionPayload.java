package io.github.q93503128.turnbound.network;

import io.github.q93503128.turnbound.Turnbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-bound departure/arrival presentation state for physical waystation travel. */
public record FastTravelTransitionPayload(String transition) implements CustomPacketPayload {
    public static final Type<FastTravelTransitionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "fast_travel_transition"));
    public static final StreamCodec<ByteBuf, FastTravelTransitionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FastTravelTransitionPayload::transition, FastTravelTransitionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
