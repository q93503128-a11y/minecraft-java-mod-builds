package kr.moonseungjun.frontiersettlement.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Compact spatial context used only for client presentation/Jade. Never resource authority. */
public record SettlementContextTarget(String key, String kind,
                                      int minX, int minY, int minZ,
                                      int maxX, int maxY, int maxZ,
                                      int markerX, int markerY, int markerZ,
                                      String title, String detail, int progress) {
    public static void encode(RegistryFriendlyByteBuf buf, SettlementContextTarget target) {
        buf.writeUtf(target.key());
        buf.writeUtf(target.kind());
        buf.writeVarInt(target.minX()); buf.writeVarInt(target.minY()); buf.writeVarInt(target.minZ());
        buf.writeVarInt(target.maxX()); buf.writeVarInt(target.maxY()); buf.writeVarInt(target.maxZ());
        buf.writeVarInt(target.markerX()); buf.writeVarInt(target.markerY()); buf.writeVarInt(target.markerZ());
        buf.writeUtf(target.title());
        buf.writeUtf(target.detail());
        buf.writeVarInt(target.progress() + 1);
    }

    public static SettlementContextTarget decode(RegistryFriendlyByteBuf buf) {
        return new SettlementContextTarget(
                buf.readUtf(), buf.readUtf(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readUtf(), buf.readUtf(), buf.readVarInt() - 1);
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public BlockPos markerPos() { return new BlockPos(markerX, markerY, markerZ); }
}
