package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.jspecify.annotations.Nullable;

public final class CampfireMusicClient {
    private static @Nullable SoundInstance current;

    private CampfireMusicClient() {}

    public static void playSelected() {
        stop();
        SoundInstance next = SimpleSoundInstance.forMusic(ModSounds.ETIRWER.get());
        Minecraft.getInstance().getSoundManager().play(next);
        current = next;
    }

    public static void stop() {
        if (current == null) return;
        Minecraft.getInstance().getSoundManager().stop(current);
        current = null;
    }

    public static boolean isPlaying() {
        return current != null;
    }
}
