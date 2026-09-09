package dev.moonseungjun.fishinggame.network;

import java.util.ArrayList;
import java.util.List;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.profile.CatchEntry;
import dev.moonseungjun.fishinggame.profile.PlayerFishingProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProfileSnapshotPayload(int coins, int rodTier, List<CatchEntry> catches) implements CustomPacketPayload {
    public static final Type<ProfileSnapshotPayload> TYPE = new Type<>(FishingGameMod.id("profile_snapshot"));
    public static final StreamCodec<FriendlyByteBuf, ProfileSnapshotPayload> CODEC = StreamCodec.of(
            ProfileSnapshotPayload::encode,
            ProfileSnapshotPayload::decode
    );

    public ProfileSnapshotPayload(PlayerFishingProfile profile) {
        this(profile.coins(), profile.rodTier(), profile.catches());
    }

    public ProfileSnapshotPayload {
        catches = List.copyOf(catches);
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
    }

    private static ProfileSnapshotPayload decode(FriendlyByteBuf buf) {
        int coins = buf.readVarInt();
        int rodTier = buf.readVarInt();
        int size = Math.min(PlayerFishingProfile.BAG_CAPACITY, Math.max(0, buf.readVarInt()));
        ArrayList<CatchEntry> catches = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            catches.add(new CatchEntry(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return new ProfileSnapshotPayload(coins, rodTier, catches);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
