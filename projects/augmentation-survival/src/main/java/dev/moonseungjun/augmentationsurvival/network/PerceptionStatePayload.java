package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-owned scanner snapshot plus small owner-only augment presentation state. */
public record PerceptionStatePayload(
        int oreSenseLevel,
        boolean enchantRefreshOwned,
        int sweepingEnhancementLevel,
        List<Long> chestPositions,
        List<Integer> dangerEntityIds,
        List<Long> reviveMarkerPositions
) implements CustomPacketPayload {
    public static final Type<PerceptionStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "perception_state"));
    private static final int MAX_BLOCK_MARKERS = 512;
    private static final int MAX_ENTITY_MARKERS = 256;

    public static final StreamCodec<RegistryFriendlyByteBuf, PerceptionStatePayload> CODEC = new StreamCodec<>() {
        @Override public PerceptionStatePayload decode(RegistryFriendlyByteBuf buffer) {
            int oreLevel = buffer.readVarInt();
            boolean enchantRefresh = buffer.readBoolean();
            int sweepingLevel = buffer.readVarInt();
            int chestCount = Math.min(MAX_BLOCK_MARKERS, Math.max(0, buffer.readVarInt()));
            List<Long> chests = new ArrayList<>(chestCount);
            for (int i = 0; i < chestCount; i++) chests.add(buffer.readLong());
            int dangerCount = Math.min(MAX_ENTITY_MARKERS, Math.max(0, buffer.readVarInt()));
            List<Integer> danger = new ArrayList<>(dangerCount);
            for (int i = 0; i < dangerCount; i++) danger.add(buffer.readVarInt());
            int reviveCount = Math.min(MAX_BLOCK_MARKERS, Math.max(0, buffer.readVarInt()));
            List<Long> revive = new ArrayList<>(reviveCount);
            for (int i = 0; i < reviveCount; i++) revive.add(buffer.readLong());
            return new PerceptionStatePayload(oreLevel, enchantRefresh, sweepingLevel, chests, danger, revive);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, PerceptionStatePayload value) {
            buffer.writeVarInt(value.oreSenseLevel());
            buffer.writeBoolean(value.enchantRefreshOwned());
            buffer.writeVarInt(value.sweepingEnhancementLevel());
            int chestCount = Math.min(MAX_BLOCK_MARKERS, value.chestPositions().size());
            buffer.writeVarInt(chestCount);
            for (int i = 0; i < chestCount; i++) buffer.writeLong(value.chestPositions().get(i));
            int dangerCount = Math.min(MAX_ENTITY_MARKERS, value.dangerEntityIds().size());
            buffer.writeVarInt(dangerCount);
            for (int i = 0; i < dangerCount; i++) buffer.writeVarInt(value.dangerEntityIds().get(i));
            int reviveCount = Math.min(MAX_BLOCK_MARKERS, value.reviveMarkerPositions().size());
            buffer.writeVarInt(reviveCount);
            for (int i = 0; i < reviveCount; i++) buffer.writeLong(value.reviveMarkerPositions().get(i));
        }
    };

    public PerceptionStatePayload {
        oreSenseLevel = Math.max(0, Math.min(3, oreSenseLevel));
        sweepingEnhancementLevel = Math.max(0, Math.min(3, sweepingEnhancementLevel));
        chestPositions = List.copyOf(chestPositions);
        dangerEntityIds = List.copyOf(dangerEntityIds);
        reviveMarkerPositions = List.copyOf(reviveMarkerPositions);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
