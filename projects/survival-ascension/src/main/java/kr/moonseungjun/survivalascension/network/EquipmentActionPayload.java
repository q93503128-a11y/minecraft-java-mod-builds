package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EquipmentActionPayload(int action) implements CustomPacketPayload {
    public static final Type<EquipmentActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "equipment_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentActionPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.action()),
            buf -> new EquipmentActionPayload(buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
