package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-owned scanner snapshot. Ore level is sent for local visual scanning; target results are authoritative. */
public record PerceptionStatePayload(
        int oreSenseLevel,
        boolean enchantRefreshOwned,
        List<Long> chestPositions,
        List<Integer> dangerEntityIds,
        List<Long> reviveMarkerPositions
) implements CustomPacketPayload {
    public static final Type<PerceptionStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "perception_state")
    );
    private static final int MAX_BLOCK_MARKERS = 512;
    private static final int MAX_ENTITY_MARKERS = 256;

    public static final StreamCodec<RegistryFriendlyByteBuf, PerceptionStatePayload> CODEC = new StreamCodec<>() {
        @Override
        public PerceptionStatePayload decode(RegistryFriendlyByteBuf buffer) {
            int oreLevel = buffer.readVarInt();
            boolean enchantRefresh = buffer.readBoolean();
            int chestCount = Math.min(MAX_BLOCK_MARKERS, Math.max(0, buffer.readVarInt()));
            List<Long> chests = new ArrayList<>(chestCount);
            for (int index = 0; index < chestCount; index++) chests.add(buffer.readLong());
            int dangerCount = Math.min(MAX_ENTITY_MARKERS, Math.max(0, buffer.readVarInt()));
            List<Integer> danger = new ArrayList<>(dangerCount);
            for (int index = 0; index < dangerCount; index++) danger.add(buffer.readVarInt());
            int reviveCount = Math.min(MAX_BLOCK_MARKERS, Math.max(0, buffer.readVarInt()));
            List<Long> revive = new ArrayList<>(reviveCount);
            for (int index = 0; index < reviveCount; index++) revive.add(buffer.readLong());
            return new PerceptionStatePayload(oreLevel, enchantRefresh, chests, danger, revive);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PerceptionStatePayload value) {
            buffer.writeVarInt(value.oreSenseLevel());
            buffer.writeBoolean(value.enchantRefreshOwned());
            int chestCount = Math.min(MAX_BLOCK_MARKERS, value.chestPositions().size());
            buffer.writeVarInt(chestCount);
            for (int index = 0; index < chestCount; index++) buffer.writeLong(value.chestPositions().get(index));
            int dangerCount = Math.min(MAX_ENTITY_MARKERS, value.dangerEntityIds().size());
            buffer.writeVarInt(dangerCount);
            for (int index = 0; index < dangerCount; index++) buffer.writeVarInt(value.dangerEntityIds().get(index));
            int reviveCount = Math.min(MAX_BLOCK_MARKERS, value.reviveMarkerPositions().size());
            buffer.writeVarInt(reviveCount);
            for (int index = 0; index < reviveCount; index++) buffer.writeLong(value.reviveMarkerPositions().get(index));
        }
    };

    public PerceptionStatePayload {
        oreSenseLevel = Math.max(0, Math.min(3, oreSenseLevel));
        chestPositions = List.copyOf(chestPositions);
        dangerEntityIds = List.copyOf(dangerEntityIds);
        reviveMarkerPositions = List.copyOf(reviveMarkerPositions);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
