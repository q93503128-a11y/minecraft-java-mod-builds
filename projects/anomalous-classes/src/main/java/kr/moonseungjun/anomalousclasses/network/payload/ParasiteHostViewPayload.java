package kr.moonseungjun.anomalousclasses.network.payload;

import java.util.UUID;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-issued camera target. -1 clears the previous host; no client ownership claims. */
public record ParasiteHostViewPayload(int entityId, UUID hostUuid) implements CustomPacketPayload {
    public static final UUID NONE = new UUID(0L, 0L);
    public static final Type<ParasiteHostViewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "parasite_host_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ParasiteHostViewPayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> {
                buf.writeVarInt(value.entityId());
                buf.writeUUID(value.hostUuid());
            }, buf -> new ParasiteHostViewPayload(buf.readVarInt(), buf.readUUID()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
