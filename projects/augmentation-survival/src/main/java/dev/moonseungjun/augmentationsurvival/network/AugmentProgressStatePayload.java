package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Clientbound snapshot of all acquired augment levels for player-facing collection UI. */
public record AugmentProgressStatePayload(List<String> ids, List<Integer> levels) implements CustomPacketPayload {
    /** Defensive protocol ceiling with headroom above the current 149-entry canonical roster. */
    private static final int MAX_ENTRIES = 512;

    public static final Type<AugmentProgressStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "augment_progress_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AugmentProgressStatePayload> CODEC = new StreamCodec<>() {
        @Override public AugmentProgressStatePayload decode(RegistryFriendlyByteBuf buffer) {
            int count = Math.min(MAX_ENTRIES, Math.max(0, buffer.readVarInt()));
            List<String> ids = new ArrayList<>(count);
            List<Integer> levels = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ids.add(buffer.readUtf(128));
                levels.add(Math.max(1, buffer.readVarInt()));
            }
            return new AugmentProgressStatePayload(ids, levels);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, AugmentProgressStatePayload value) {
            int count = Math.min(MAX_ENTRIES, Math.min(value.ids().size(), value.levels().size()));
            buffer.writeVarInt(count);
            for (int i = 0; i < count; i++) {
                buffer.writeUtf(value.ids().get(i), 128);
                buffer.writeVarInt(Math.max(1, value.levels().get(i)));
            }
        }
    };

    public AugmentProgressStatePayload {
        ids = List.copyOf(ids);
        levels = List.copyOf(levels);
    }

    public static int maxEntries() {
        return MAX_ENTRIES;
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
