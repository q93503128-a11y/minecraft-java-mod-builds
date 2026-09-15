package dev.moonseungjun.fishinggame.client;

import dev.moonseungjun.fishinggame.fishing.FishRarity;
import dev.moonseungjun.fishinggame.fishing.FishSizeGrade;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class FishingClientAudio {
    private static final long PULL_SOUND_COOLDOWN_MS = 480L;
    private static long lastPullSoundMs;

    private FishingClientAudio() {
    }

    public static void onCastFullyCharged() {
        play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.22f, 1.70f);
    }

    public static void onFishingState(int previousStage, float previousTension, int nextStage, float nextTension) {
        if (previousStage != 2 || nextStage != 2) return;
        if (nextTension - previousTension < 0.065f) return;

        long now = System.currentTimeMillis();
        if (now - lastPullSoundMs < PULL_SOUND_COOLDOWN_MS) return;
        lastPullSoundMs = now;
        play(SoundEvents.COD_FLOP, 0.18f, 0.78f + Math.min(0.30f, nextTension * 0.28f));
    }

    public static void onCatch(RecentCatchPresentation presentation, FishRarity rarity) {
        boolean trophy = presentation.sizeGrade() == FishSizeGrade.TROPHY
                || presentation.sizeGrade() == FishSizeGrade.MONSTER;
        boolean rare = rarity == FishRarity.EPIC || rarity == FishRarity.LEGENDARY;
        boolean major = presentation.firstDiscovery()
                || presentation.personalBest()
                || presentation.locationCompleted()
                || presentation.sizeGrade() == FishSizeGrade.MONSTER
                || rarity == FishRarity.LEGENDARY;

        if (trophy || rare) {
            float pitch = 1.03f + rarity.ordinal() * 0.07f;
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.48f, pitch);
        }
        if (major) {
            play(SoundEvents.PLAYER_LEVELUP, presentation.locationCompleted() ? 0.55f : 0.42f, 1.16f);
        }
    }

    public static void onSale() {
        play(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.48f, 1.12f);
    }

    public static void onRodUpgrade() {
        play(SoundEvents.PLAYER_LEVELUP, 0.58f, 1.03f);
    }

    public static void onRebirth() {
        play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.62f, 0.88f);
        play(SoundEvents.PLAYER_LEVELUP, 0.72f, 1.28f);
    }

    private static void play(SoundEvent sound, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.playSound(sound, volume, pitch);
        }
    }
}
