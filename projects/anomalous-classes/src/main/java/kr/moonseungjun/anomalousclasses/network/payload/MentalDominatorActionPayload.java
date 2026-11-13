package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MentalDominatorActionPayload(int action, String entityId, int expectedRevision)
        implements CustomPacketPayload {
    public static final int FOLLOW = 0;
    public static final int STAY = 1;
    public static final int ALL_FOLLOW = 2;
    public static final int ALL_STAY = 3;
    public static final int TOGGLE_PLAYER_DAMAGE = 4;
    public static final int RELEASE = 5;
    private static final int MAX_ID_LENGTH = 36;

    public static final Type<MentalDominatorActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "mental_dominator_action"));
    public static final StreamCodec<ByteBuf, MentalDominatorActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                ByteBufCodecs.VAR_INT.encode(buffer, value.action);
                ByteBufCodecs.STRING_UTF8.encode(buffer, value.entityId);
                ByteBufCodecs.VAR_INT.encode(buffer, value.expectedRevision);
            }, buffer -> new MentalDominatorActionPayload(ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer)));

    public MentalDominatorActionPayload {
        if (action < FOLLOW || action > RELEASE) action = -1;
        entityId = entityId == null ? "" : entityId.substring(0, Math.min(MAX_ID_LENGTH, entityId.length()));
        if (expectedRevision < 0) expectedRevision = -1;
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
