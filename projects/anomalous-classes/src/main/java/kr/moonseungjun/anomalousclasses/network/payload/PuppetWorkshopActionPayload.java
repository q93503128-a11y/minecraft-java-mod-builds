package kr.moonseungjun.anomalousclasses.network.payload;

import java.util.UUID;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Untrusted request: the server re-reads inventory slot, ID and revision. */
public record PuppetWorkshopActionPayload(BlockPos station, int operation, int inventorySlot,
                                          UUID puppetId, int expectedRevision, int craftDevice)
        implements CustomPacketPayload {
    public static final int CRAFT = 0, REPAIR = 1, EXPAND = 2, DISMANTLE = 3;
    public static final Type<PuppetWorkshopActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "puppet_workbench_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PuppetWorkshopActionPayload> STREAM_CODEC =
            StreamCodec.of((buffer, value) -> {
                buffer.writeBlockPos(value.station);
                buffer.writeVarInt(value.operation);
                buffer.writeVarInt(value.inventorySlot);
                buffer.writeUUID(value.puppetId);
                buffer.writeVarInt(value.expectedRevision);
                buffer.writeVarInt(value.craftDevice);
            }, buffer -> new PuppetWorkshopActionPayload(buffer.readBlockPos(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
