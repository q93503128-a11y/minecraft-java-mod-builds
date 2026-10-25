package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Clientbound projection of gates visible to one player. */
public record SpatialLinkSnapshotPayload(List<GateView> gates) implements CustomPacketPayload {
    public static final Type<SpatialLinkSnapshotPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "spatial_link_snapshot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SpatialLinkSnapshotPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public SpatialLinkSnapshotPayload decode(RegistryFriendlyByteBuf buf) {
                    int count = Math.max(0, Math.min(4096, buf.readVarInt()));
                    ArrayList<GateView> gates = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        gates.add(new GateView(
                                buf.readUtf(64),
                                buf.readUtf(64),
                                buf.readUtf(128),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readBoolean(),
                                buf.readBoolean(),
                                buf.readBoolean()
                        ));
                    }
                    return new SpatialLinkSnapshotPayload(List.copyOf(gates));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SpatialLinkSnapshotPayload payload) {
                    buf.writeVarInt(payload.gates().size());
                    for (GateView gate : payload.gates()) {
                        buf.writeUtf(gate.id(), 64);
                        buf.writeUtf(gate.name(), 64);
                        buf.writeUtf(gate.dimension(), 128);
                        buf.writeDouble(gate.x());
                        buf.writeDouble(gate.y());
                        buf.writeDouble(gate.z());
                        buf.writeBoolean(gate.owner());
                        buf.writeBoolean(gate.favorite());
                        buf.writeBoolean(gate.defaultGate());
                    }
                }
            };

    public SpatialLinkSnapshotPayload {
        gates = List.copyOf(gates);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record GateView(
            String id,
            String name,
            String dimension,
            double x,
            double y,
            double z,
            boolean owner,
            boolean favorite,
            boolean defaultGate
    ) { }
}
