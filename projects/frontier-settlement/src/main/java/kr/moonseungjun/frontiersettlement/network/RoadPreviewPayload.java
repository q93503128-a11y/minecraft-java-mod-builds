package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import kr.moonseungjun.frontiersettlement.settlement.SettlementRoadService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record RoadPreviewPayload(int nonce, boolean valid, boolean confirmed,
                                 int stoneCost, List<Integer> path, String message)
        implements CustomPacketPayload {
    public static final Type<RoadPreviewPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "road_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoadPreviewPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.nonce());
                buf.writeBoolean(payload.valid());
                buf.writeBoolean(payload.confirmed());
                buf.writeVarInt(payload.stoneCost());
                buf.writeVarInt(payload.path().size());
                for (int value : payload.path()) buf.writeInt(value);
                buf.writeUtf(payload.message(), 256);
            },
            buf -> {
                int nonce = buf.readVarInt();
                boolean valid = buf.readBoolean();
                boolean confirmed = buf.readBoolean();
                int stoneCost = buf.readVarInt();
                int size = buf.readVarInt();
                if (size < 0 || size > SettlementRoadService.MAX_ROUTE_LENGTH * 3) {
                    throw new IllegalArgumentException("invalid road preview coordinate count: " + size);
                }
                List<Integer> path = new ArrayList<>(size);
                for (int i = 0; i < size; i++) path.add(buf.readInt());
                String message = buf.readUtf(256);
                return new RoadPreviewPayload(nonce, valid, confirmed, stoneCost, List.copyOf(path), message);
            }
    );

    public static RoadPreviewPayload fromCheck(int nonce, SettlementRoadService.RouteCheck check, boolean confirmed) {
        List<Integer> encoded = new ArrayList<>(check.centers().size() * 3);
        for (BlockPos center : check.centers()) {
            encoded.add(center.getX());
            encoded.add(center.getY());
            encoded.add(center.getZ());
        }
        return new RoadPreviewPayload(nonce, check.valid(), confirmed, check.stoneCost(),
                List.copyOf(encoded), check.message());
    }

    public List<BlockPos> centers() {
        if (path == null || path.size() < 6 || path.size() % 3 != 0) return List.of();
        List<BlockPos> centers = new ArrayList<>(path.size() / 3);
        for (int i = 0; i + 2 < path.size(); i += 3) {
            centers.add(new BlockPos(path.get(i), path.get(i + 1), path.get(i + 2)));
        }
        return List.copyOf(centers);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
