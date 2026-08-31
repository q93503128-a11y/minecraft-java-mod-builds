package kr.moonseungjun.titanbreak.network;

import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CombatAssistIntentPayload(boolean active) implements CustomPacketPayload {
    public static final Type<CombatAssistIntentPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "combat_assist_intent"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CombatAssistIntentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CombatAssistIntentPayload::active,
            CombatAssistIntentPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
