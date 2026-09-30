package dev.moonseungjun.openworldrpg.network;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HunterQuickstepIntentPayload(
        float forwardIntent,
        float strafeIntent,
        long sequence
) implements CustomPacketPayload {
    public static final Type<HunterQuickstepIntentPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "hunter_quickstep_intent"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, HunterQuickstepIntentPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    HunterQuickstepIntentPayload::forwardIntent,
                    ByteBufCodecs.FLOAT,
                    HunterQuickstepIntentPayload::strafeIntent,
                    ByteBufCodecs.VAR_LONG,
                    HunterQuickstepIntentPayload::sequence,
                    HunterQuickstepIntentPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
