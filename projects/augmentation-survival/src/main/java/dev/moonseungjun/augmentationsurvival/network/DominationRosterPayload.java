package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded client display of server-owned live/reserved domination slots. */
public record DominationRosterPayload(int occupied, int maximum, List<Member> members)
        implements CustomPacketPayload {
    public static final Type<DominationRosterPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "domination_roster")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DominationRosterPayload> CODEC = new StreamCodec<>() {
        @Override public DominationRosterPayload decode(RegistryFriendlyByteBuf buffer) {
            int used = Math.max(0, buffer.readVarInt());
            int cap = Math.max(0, buffer.readVarInt());
            int size = Math.min(32, Math.max(0, buffer.readVarInt()));
            List<Member> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                entries.add(new Member(buffer.readUtf(72), buffer.readUtf(128),
                        buffer.readUtf(128), buffer.readBoolean(), buffer.readBoolean(),
                        Math.max(0, buffer.readVarInt())));
            }
            return new DominationRosterPayload(used, cap, entries);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, DominationRosterPayload packet) {
            buffer.writeVarInt(Math.max(0, packet.occupied()));
            buffer.writeVarInt(Math.max(0, packet.maximum()));
            int size = Math.min(32, packet.members().size());
            buffer.writeVarInt(size);
            for (int i = 0; i < size; i++) {
                Member entry = packet.members().get(i);
                buffer.writeUtf(entry.entityId(), 72);
                buffer.writeUtf(entry.typeId(), 128);
                buffer.writeUtf(entry.dimension(), 128);
                buffer.writeBoolean(entry.reserved());
                buffer.writeBoolean(entry.loaded());
                buffer.writeVarInt(Math.max(0, entry.reviveSeconds()));
            }
        }
    };
    public DominationRosterPayload { members = List.copyOf(members); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record Member(String entityId, String typeId, String dimension,
                         boolean reserved, boolean loaded, int reviveSeconds) { }
}
