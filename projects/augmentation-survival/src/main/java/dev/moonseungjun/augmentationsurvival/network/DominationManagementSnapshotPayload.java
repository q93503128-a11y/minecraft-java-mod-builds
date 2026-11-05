package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authoritative roster for display only. */
public record DominationManagementSnapshotPayload(
        int occupied, int capacity, List<CompanionView> companions, boolean openScreen
) implements CustomPacketPayload {
    public record CompanionView(String uuid, String name, String entityId, String dimension, boolean loaded) { }
    public static final Type<DominationManagementSnapshotPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "domination_management_snapshot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DominationManagementSnapshotPayload> CODEC =
            new StreamCodec<>() {
                @Override public DominationManagementSnapshotPayload decode(RegistryFriendlyByteBuf buf) {
                    int occupied = Math.max(0, buf.readVarInt());
                    int capacity = Math.max(0, buf.readVarInt());
                    int count = Math.min(64, Math.max(0, buf.readVarInt()));
                    List<CompanionView> views = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        views.add(new CompanionView(buf.readUtf(64), buf.readUtf(128),
                                buf.readUtf(128), buf.readUtf(128), buf.readBoolean()));
                    }
                    return new DominationManagementSnapshotPayload(occupied, capacity, views, buf.readBoolean());
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, DominationManagementSnapshotPayload value) {
                    buf.writeVarInt(value.occupied());
                    buf.writeVarInt(value.capacity());
                    buf.writeVarInt(Math.min(value.companions().size(), 64));
                    for (int i = 0; i < Math.min(value.companions().size(), 64); i++) {
                        CompanionView entry = value.companions().get(i);
                        buf.writeUtf(entry.uuid(), 64);
                        buf.writeUtf(entry.name(), 128);
                        buf.writeUtf(entry.entityId(), 128);
                        buf.writeUtf(entry.dimension(), 128);
                        buf.writeBoolean(entry.loaded());
                    }
                    buf.writeBoolean(value.openScreen());
                }
            };
    public DominationManagementSnapshotPayload { companions = List.copyOf(companions); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
