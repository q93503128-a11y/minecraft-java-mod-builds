package kr.moonseungjun.campfiresessions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.jspecify.annotations.Nullable;

public final class CampfireMusicClient {
    private static int selectedIndex;
    private static int playingIndex = -1;
    private static @Nullable SoundInstance current;

    private CampfireMusicClient() {}

    public static MusicTrack selectedTrack() {
        return MusicCatalog.get(selectedIndex);
    }

    public static int selectedIndex() {
        return selectedIndex;
    }

    public static void select(int index) {
        int next = Math.floorMod(index, MusicCatalog.TRACKS.size());
        boolean resume = isPlaying();
        selectedIndex = next;
        if (resume) playSelected();
    }

    public static void previous() {
        select(selectedIndex - 1);
    }

    public static void next() {
        select(selectedIndex + 1);
    }

    public static void toggleSelected() {
        if (isPlayingSelected()) stop();
        else playSelected();
    }

    public static void playSelected() {
        stop();
        MusicTrack track = selectedTrack();
        SoundInstance next = SimpleSoundInstance.forMusic(track.sound().get());
        Minecraft.getInstance().getSoundManager().play(next);
        current = next;
        playingIndex = selectedIndex;
    }

    public static void stop() {
        if (current != null) Minecraft.getInstance().getSoundManager().stop(current);
        current = null;
        playingIndex = -1;
    }

    public static boolean isPlaying() {
        return current != null;
    }

    public static boolean isPlayingSelected() {
        return current != null && playingIndex == selectedIndex;
    }

    public static int playingIndex() {
        return playingIndex;
    }
}
