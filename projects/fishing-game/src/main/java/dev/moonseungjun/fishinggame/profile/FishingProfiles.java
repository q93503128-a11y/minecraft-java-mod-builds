package dev.moonseungjun.fishinggame.profile;

import dev.moonseungjun.fishinggame.FishingGameMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;

public final class FishingProfiles {
    public static final AttachmentType<PlayerFishingProfile> PROFILE = AttachmentRegistry.create(
            FishingGameMod.id("profile"),
            builder -> builder
                    .initializer(PlayerFishingProfile::empty)
                    .persistent(PlayerFishingProfile.CODEC)
                    .copyOnDeath()
    );

    private FishingProfiles() {
    }

    public static PlayerFishingProfile get(ServerPlayer player) {
        return player.getAttachedOrCreate(PROFILE);
    }

    public static void set(ServerPlayer player, PlayerFishingProfile profile) {
        player.setAttached(PROFILE, profile);
    }
}
