package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.Map;

public record SkillSnapshotPayload(Map<String, Long> xp) implements CustomPacketPayload {
    public SkillSnapshotPayload { xp = Map.copyOf(xp); }
    public static final Type<SkillSnapshotPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "skill_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillSnapshotPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.xp().size());
                payload.xp().forEach((id, value) -> { buf.writeUtf(id); buf.writeVarLong(value); });
            },
            buf -> {
                int size = buf.readVarInt();
                Map<String, Long> xp = new HashMap<>(size);
                for (int i = 0; i < size; i++) xp.put(buf.readUtf(), buf.readVarLong());
                return new SkillSnapshotPayload(xp);
            });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
