package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SettlementContextPayload(int buildingCount, int outpostCount,
                                       String projectLabel, int projectProgress,
                                       List<SettlementContextTarget> targets)
        implements CustomPacketPayload {
    private static final int MAX_TARGETS = 256;
    public static final SettlementContextPayload EMPTY = new SettlementContextPayload(0, 0, "", -1, List.of());
    public static final Type<SettlementContextPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "settlement_context"));

    public SettlementContextPayload {
        targets = List.copyOf(targets);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SettlementContextPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.buildingCount());
                buf.writeVarInt(payload.outpostCount());
                buf.writeUtf(payload.projectLabel());
                buf.writeVarInt(payload.projectProgress() + 1);
                int size = Math.min(MAX_TARGETS, payload.targets().size());
                buf.writeVarInt(size);
                for (int i = 0; i < size; i++) SettlementContextTarget.encode(buf, payload.targets().get(i));
            },
            buf -> {
                int buildingCount = buf.readVarInt();
                int outpostCount = buf.readVarInt();
                String projectLabel = buf.readUtf();
                int projectProgress = buf.readVarInt() - 1;
                int size = Math.max(0, Math.min(MAX_TARGETS, buf.readVarInt()));
                List<SettlementContextTarget> targets = new ArrayList<>(size);
                for (int i = 0; i < size; i++) targets.add(SettlementContextTarget.decode(buf));
                return new SettlementContextPayload(buildingCount, outpostCount, projectLabel, projectProgress, targets);
            }
    );

    public SettlementContextTarget targetAt(net.minecraft.core.BlockPos pos) {
        for (SettlementContextTarget target : targets) if (target.contains(pos)) return target;
        return null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
