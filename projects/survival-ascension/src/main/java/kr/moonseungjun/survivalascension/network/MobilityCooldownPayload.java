package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MobilityCooldownPayload(int cooldownTicks) implements CustomPacketPayload {
    public static final Type<MobilityCooldownPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mobility_cooldown"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobilityCooldownPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.cooldownTicks()),
            buf -> new MobilityCooldownPayload(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
