package kr.countrysidedays.network;

import kr.countrysidedays.CountrysideDays;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authoritative private landmark coordinates for one connected player. */
public record EstateHudPayload(
        int homeX,
        int homeY,
        int homeZ,
        int restaurantX,
        int restaurantY,
        int restaurantZ
) implements CustomPacketPayload {
    public static final Type<EstateHudPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(CountrysideDays.MOD_ID, "estate_hud")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EstateHudPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EstateHudPayload::homeX,
            ByteBufCodecs.VAR_INT, EstateHudPayload::homeY,
            ByteBufCodecs.VAR_INT, EstateHudPayload::homeZ,
            ByteBufCodecs.VAR_INT, EstateHudPayload::restaurantX,
            ByteBufCodecs.VAR_INT, EstateHudPayload::restaurantY,
            ByteBufCodecs.VAR_INT, EstateHudPayload::restaurantZ,
            EstateHudPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
