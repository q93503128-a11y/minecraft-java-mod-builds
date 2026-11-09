package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkillTreeActionPayload(int classIndex, int nodeIndex, int action, int expectedRevision)
        implements CustomPacketPayload {
    public static final Type<SkillTreeActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "skill_tree_action"));
    public static final StreamCodec<ByteBuf, SkillTreeActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                ByteBufCodecs.VAR_INT.encode(buffer, value.classIndex);
                ByteBufCodecs.VAR_INT.encode(buffer, value.nodeIndex);
                ByteBufCodecs.VAR_INT.encode(buffer, value.action);
                ByteBufCodecs.VAR_INT.encode(buffer, value.expectedRevision);
            }, buffer -> new SkillTreeActionPayload(ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer))));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public enum Action { INVEST_ONE, INVEST_TO_BREAKPOINT, INVEST_MAX, RESET }
}
