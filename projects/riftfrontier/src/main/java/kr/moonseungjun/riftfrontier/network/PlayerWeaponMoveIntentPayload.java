package kr.moonseungjun.riftfrontier.network;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Client-to-server intent for one authored weapon move.
 *
 * <p>No damage, target, hit result, phase, duration, cooldown or client clock is accepted on this
 * wire contract. The sending player and current loadout are resolved again on the server.</p>
 */
public record PlayerWeaponMoveIntentPayload(ContentId moveId) implements CustomPacketPayload {
    public static final Type<PlayerWeaponMoveIntentPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(Riftfrontier.MOD_ID, "player_weapon_move_intent")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerWeaponMoveIntentPayload> STREAM_CODEC =
        StreamCodec.ofMember(PlayerWeaponMoveIntentPayload::encode, PlayerWeaponMoveIntentPayload::decode);

    public PlayerWeaponMoveIntentPayload {
        moveId = Objects.requireNonNull(moveId, "moveId");
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(moveId.toString());
    }

    private static PlayerWeaponMoveIntentPayload decode(RegistryFriendlyByteBuf buf) {
        return new PlayerWeaponMoveIntentPayload(ContentId.parse(buf.readUtf(160)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
