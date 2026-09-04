package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.expedition.ExpeditionData;
import kr.moonseungjun.survivalascension.expedition.ExpeditionRegion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public record ExpeditionSnapshotPayload(int discoveredMask, int completedMask, Map<String, String> directives)
        implements CustomPacketPayload {
    public ExpeditionSnapshotPayload { directives = Map.copyOf(directives); }

    public static final Type<ExpeditionSnapshotPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_snapshot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExpeditionSnapshotPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.discoveredMask());
                buf.writeVarInt(payload.completedMask());
                buf.writeVarInt(payload.directives().size());
                payload.directives().forEach((region, summary) -> {
                    buf.writeUtf(region);
                    buf.writeUtf(summary);
                });
            },
            buf -> {
                int discovered = buf.readVarInt();
                int completed = buf.readVarInt();
                int size = buf.readVarInt();
                Map<String, String> directives = new HashMap<>(size);
                for (int i = 0; i < size; i++) directives.put(buf.readUtf(), buf.readUtf());
                return new ExpeditionSnapshotPayload(discovered, completed, directives);
            });

    public static ExpeditionSnapshotPayload from(ServerPlayer player) {
        ExpeditionData data = ExpeditionData.get(player);
        int discovered = 0;
        int completed = 0;
        Map<String, String> directives = new HashMap<>();
        for (ExpeditionRegion region : ExpeditionRegion.values()) {
            if (data.isDiscovered(player, region)) {
                discovered |= region.bit();
                directives.put(region.name(), data.directiveSummary(player, region));
            }
            if (data.isComplete(player, region)) completed |= region.bit();
        }
        return new ExpeditionSnapshotPayload(discovered, completed, directives);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
