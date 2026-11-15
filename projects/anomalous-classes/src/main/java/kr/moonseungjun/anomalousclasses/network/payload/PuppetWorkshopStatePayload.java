package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Read-only server view for the puppet workbench; never authorizes a transaction. */
public record PuppetWorkshopStatePayload(BlockPos station, boolean craftable, int result) implements CustomPacketPayload {
    public static final int OPEN = 0, CRAFTED = 1, REJECTED = 2;
    public static final Type<PuppetWorkshopStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "puppet_workbench_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PuppetWorkshopStatePayload> STREAM_CODEC =
            StreamCodec.of((buffer, value) -> {
                buffer.writeBlockPos(value.station);
                buffer.writeBoolean(value.craftable);
                buffer.writeVarInt(value.result);
            }, buffer -> new PuppetWorkshopStatePayload(buffer.readBlockPos(),
                    buffer.readBoolean(), buffer.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
