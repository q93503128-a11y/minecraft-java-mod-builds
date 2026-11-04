package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server validates all ownership, lifetime and release requests. */
public record DominationManageRequestPayload(int action, String entityId) implements CustomPacketPayload {
    public static final int OPEN = 0;
    public static final int RELEASE = 1;
    public static final int RELEASE_ALL = 2;
    public static final Type<DominationManageRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "domination_manage")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DominationManageRequestPayload> CODEC = new StreamCodec<>() {
        @Override public DominationManageRequestPayload decode(RegistryFriendlyByteBuf buffer) {
            return new DominationManageRequestPayload(buffer.readVarInt(), buffer.readUtf(72));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, DominationManageRequestPayload packet) {
            buffer.writeVarInt(packet.action());
            buffer.writeUtf(packet.entityId() == null ? "" : packet.entityId(), 72);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
