package kr.moonseungjun.anomalousclasses.network.payload;

import java.util.UUID;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** User intention only; no client claim of current N, role or ownership. */
public record SwarmTacticActionPayload(int view, int action, int expectedRevision,
        UUID id, String name, int attackPercent, int destination) implements CustomPacketPayload {
    public static final int SELECT = 0, CREATE = 1, UPDATE = 2, DELETE = 3,
            MOVE = 4, TEMPORARY = 5, RESTORE = 6;
    public static final UUID NONE = new UUID(0L, 0L);
    public static final Type<SwarmTacticActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "swarm_tactic_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwarmTacticActionPayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> {
                buf.writeVarInt(value.view);
                buf.writeVarInt(value.action);
                buf.writeVarInt(value.expectedRevision);
                buf.writeUUID(value.id);
                buf.writeUtf(value.name, 64);
                buf.writeVarInt(value.attackPercent);
                buf.writeVarInt(value.destination);
            }, buf -> new SwarmTacticActionPayload(buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readUUID(), buf.readUtf(64),
                    buf.readVarInt(), buf.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
