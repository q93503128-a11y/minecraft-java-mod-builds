package kr.moonseungjun.anomalousclasses.network.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Approved server state. Quick and management screens share the same contract. */
public record SwarmTacticStatePayload(int view, int result, int revision,
        int alive, int maximum, int attack, int guard, int lift, int regroup,
        UUID selectedId, int appliedAttackPercent, boolean temporary,
        List<TacticEntry> tactics) implements CustomPacketPayload {
    public static final int OPEN = 0, UPDATED = 1, STALE = 2, REJECTED = 3;
    public record TacticEntry(UUID id, String name, int attackPercent) {}
    public SwarmTacticStatePayload {
        tactics = List.copyOf(tactics);
        if (tactics.size() > 8) throw new IllegalArgumentException("Too many tactic entries");
    }
    public static final Type<SwarmTacticStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "swarm_tactic_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwarmTacticStatePayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> {
                buf.writeVarInt(value.view);
                buf.writeVarInt(value.result);
                buf.writeVarInt(value.revision);
                buf.writeVarInt(value.alive);
                buf.writeVarInt(value.maximum);
                buf.writeVarInt(value.attack);
                buf.writeVarInt(value.guard);
                buf.writeVarInt(value.lift);
                buf.writeVarInt(value.regroup);
                buf.writeUUID(value.selectedId);
                buf.writeVarInt(value.appliedAttackPercent);
                buf.writeBoolean(value.temporary);
                buf.writeVarInt(value.tactics.size());
                for (TacticEntry entry : value.tactics) {
                    buf.writeUUID(entry.id());
                    buf.writeUtf(entry.name(), 24);
                    buf.writeVarInt(entry.attackPercent());
                }
            }, buf -> {
                int view = buf.readVarInt();
                int result = buf.readVarInt();
                int revision = buf.readVarInt();
                int alive = buf.readVarInt();
                int maximum = buf.readVarInt();
                int attack = buf.readVarInt();
                int guard = buf.readVarInt();
                int lift = buf.readVarInt();
                int regroup = buf.readVarInt();
                UUID active = buf.readUUID();
                int ratio = buf.readVarInt();
                boolean temporary = buf.readBoolean();
                int count = buf.readVarInt();
                if (count < 0 || count > 8) throw new IllegalArgumentException("Invalid swarm tactic count");
                List<TacticEntry> entries = new ArrayList<>(count);
                for (int i = 0; i < count; i++)
                    entries.add(new TacticEntry(buf.readUUID(), buf.readUtf(24), buf.readVarInt()));
                return new SwarmTacticStatePayload(view, result, revision, alive, maximum,
                        attack, guard, lift, regroup, active, ratio, temporary, entries);
            });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
