package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Serverbound active loadout edit or state request. */
public record ActiveLoadoutEditPayload(int action, int firstSlot, int secondSlot, String augmentId)
        implements CustomPacketPayload {
    public static final int REQUEST = 0;
    public static final int ASSIGN = 1;
    public static final int SWAP = 2;
    public static final int CLEAR = 3;
    public static final Type<ActiveLoadoutEditPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "active_loadout_edit")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ActiveLoadoutEditPayload> CODEC = new StreamCodec<>() {
        @Override public ActiveLoadoutEditPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ActiveLoadoutEditPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(128));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ActiveLoadoutEditPayload value) {
            buffer.writeVarInt(value.action());
            buffer.writeVarInt(value.firstSlot());
            buffer.writeVarInt(value.secondSlot());
            buffer.writeUtf(value.augmentId() == null ? "" : value.augmentId(), 128);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
