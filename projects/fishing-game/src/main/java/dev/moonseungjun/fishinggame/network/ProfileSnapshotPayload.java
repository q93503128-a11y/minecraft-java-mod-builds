package dev.moonseungjun.fishinggame.network;

import java.util.ArrayList;
import java.util.List;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.profile.CatchEntry;
import dev.moonseungjun.fishinggame.profile.FishRecord;
import dev.moonseungjun.fishinggame.profile.PlayerFishingProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProfileSnapshotPayload(
        int coins,
        int rodTier,
        List<CatchEntry> catches,
        List<FishRecord> records,
        int rebirths
) implements CustomPacketPayload {
    public static final Type<ProfileSnapshotPayload> TYPE = new Type<>(FishingGameMod.id("profile_snapshot"));
    public static final StreamCodec<FriendlyByteBuf, ProfileSnapshotPayload> CODEC = StreamCodec.of(
            ProfileSnapshotPayload::encode,
            ProfileSnapshotPayload::decode
    );

    public ProfileSnapshotPayload(PlayerFishingProfile profile) {
        this(profile.coins(), profile.rodTier(), profile.catches(), profile.records(), profile.rebirths());
    }

    public ProfileSnapshotPayload(int coins, int rodTier, List<CatchEntry> catches, List<FishRecord> records) {
        this(coins, rodTier, catches, records, 0);
    }

    public ProfileSnapshotPayload {
        catches = List.copyOf(catches);
        records = List.copyOf(records);
        rebirths = Math.max(0, rebirths);
    }

    private static void encode(FriendlyByteBuf buf, ProfileSnapshotPayload payload) {
        buf.writeVarInt(payload.coins);
        buf.writeVarInt(payload.rodTier);
        buf.writeVarInt(payload.catches.size());
        for (CatchEntry entry : payload.catches) {
            buf.writeUtf(entry.speciesId());
            buf.writeVarInt(entry.weightGrams());
            buf.writeVarInt(entry.lengthMm());
            buf.writeVarInt(entry.value());
        }

        buf.writeVarInt(payload.records.size());
        for (FishRecord record : payload.records) {
            buf.writeUtf(record.speciesId());
            buf.writeVarInt(record.caughtCount());
            buf.writeVarInt(record.bestWeightGrams());
            buf.writeVarInt(record.bestLengthMm());
        }
        buf.writeVarInt(payload.rebirths);
    }

    private static ProfileSnapshotPayload decode(FriendlyByteBuf buf) {
        int coins = buf.readVarInt();
        int rodTier = buf.readVarInt();

        int catchSize = Math.min(PlayerFishingProfile.BAG_CAPACITY, Math.max(0, buf.readVarInt()));
        ArrayList<CatchEntry> catches = new ArrayList<>(catchSize);
        for (int i = 0; i < catchSize; i++) {
            catches.add(new CatchEntry(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }

        int recordSize = Math.min(128, Math.max(0, buf.readVarInt()));
        ArrayList<FishRecord> records = new ArrayList<>(recordSize);
        for (int i = 0; i < recordSize; i++) {
            records.add(new FishRecord(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        int rebirths = Math.max(0, buf.readVarInt());
        return new ProfileSnapshotPayload(coins, rodTier, catches, records, rebirths);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
