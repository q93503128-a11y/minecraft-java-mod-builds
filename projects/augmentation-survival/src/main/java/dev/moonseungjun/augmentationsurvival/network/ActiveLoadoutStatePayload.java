package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Clientbound snapshot for the ten active slots and their cooldowns. */
public record ActiveLoadoutStatePayload(
        List<String> slotIds,
        List<Integer> cooldownTicks,
        List<String> availableIds
) implements CustomPacketPayload {
    public static final Type<ActiveLoadoutStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "active_loadout_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ActiveLoadoutStatePayload> CODEC = new StreamCodec<>() {
        @Override public ActiveLoadoutStatePayload decode(RegistryFriendlyByteBuf buffer) {
            int slots = Math.min(10, Math.max(0, buffer.readVarInt()));
            List<String> ids = new ArrayList<>(slots);
            List<Integer> cooldowns = new ArrayList<>(slots);
            for (int i = 0; i < slots; i++) {
                ids.add(buffer.readUtf(128));
                cooldowns.add(Math.max(0, buffer.readVarInt()));
            }
            int availableCount = Math.min(32, Math.max(0, buffer.readVarInt()));
            List<String> available = new ArrayList<>(availableCount);
            for (int i = 0; i < availableCount; i++) available.add(buffer.readUtf(128));
            return new ActiveLoadoutStatePayload(ids, cooldowns, available);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ActiveLoadoutStatePayload value) {
            int slots = Math.min(10, value.slotIds().size());
            buffer.writeVarInt(slots);
            for (int i = 0; i < slots; i++) {
                buffer.writeUtf(value.slotIds().get(i), 128);
                buffer.writeVarInt(i < value.cooldownTicks().size() ? Math.max(0, value.cooldownTicks().get(i)) : 0);
            }
            int availableCount = Math.min(32, value.availableIds().size());
            buffer.writeVarInt(availableCount);
            for (int i = 0; i < availableCount; i++) buffer.writeUtf(value.availableIds().get(i), 128);
        }
    };

    public ActiveLoadoutStatePayload {
        slotIds = List.copyOf(slotIds);
        cooldownTicks = List.copyOf(cooldownTicks);
        availableIds = List.copyOf(availableIds);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
