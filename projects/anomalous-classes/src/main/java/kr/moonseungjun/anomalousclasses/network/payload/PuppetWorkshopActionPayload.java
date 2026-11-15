package kr.moonseungjun.anomalousclasses.network.payload;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server rechecks station, range, class, inventory, and uniqueness before crafting. */
public record PuppetWorkshopActionPayload(BlockPos station) implements CustomPacketPayload {
    public static final Type<PuppetWorkshopActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "puppet_workbench_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PuppetWorkshopActionPayload> STREAM_CODEC =
            StreamCodec.of((buffer, value) -> buffer.writeBlockPos(value.station),
                    buffer -> new PuppetWorkshopActionPayload(buffer.readBlockPos()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
