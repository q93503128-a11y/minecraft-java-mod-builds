package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import org.jspecify.annotations.Nullable;

public final class CampfireMusicClient {
    private static int selectedIndex;
    private static int playingIndex = -1;
    private static @Nullable SoundInstance current;

    private CampfireMusicClient() {}

    public static MusicTrack selectedTrack() { return MusicCatalog.get(selectedIndex); }
    public static int selectedIndex() { return selectedIndex; }

    public static void previewSelect(int index) {
        selectedIndex = Math.floorMod(index, MusicCatalog.TRACKS.size());
    }

    public static void select(int index) {
        int next = Math.floorMod(index, MusicCatalog.TRACKS.size());
        boolean resume = isPlaying();
        selectedIndex = next;
        if (resume) playSelected();
    }

    public static void previous() { select(selectedIndex - 1); }
    public static void next() { select(selectedIndex + 1); }

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

    public static boolean isPlaying() { return current != null; }
    public static boolean isPlayingSelected() { return current != null && playingIndex == selectedIndex; }
    public static int playingIndex() { return playingIndex; }

    public static void clientTick() {
        if (current == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            stop();
            return;
        }

        boolean holdingGuitar = player.getMainHandItem().is(ModItems.ACOUSTIC_GUITAR.get())
                || player.getOffhandItem().is(ModItems.ACOUSTIC_GUITAR.get());
        if (!holdingGuitar) {
            stop();
            return;
        }

        long time = level.getGameTime();
        if (time % 5L != 0L) return;

        double phase = (time % 80L) / 80.0 * Math.PI * 2.0;
        double radius = 0.42;
        double x = player.getX() + Math.cos(phase) * radius;
        double y = player.getY() + 1.45 + Math.sin(phase * 2.0) * 0.14;
        double z = player.getZ() + Math.sin(phase) * radius;
        double note = (time % 24L) / 24.0;
        level.addParticle(ParticleTypes.NOTE, x, y, z, note, 0.0, 0.0);

        if (time % 15L == 0L) {
            level.addParticle(ParticleTypes.NOTE,
                    player.getX() - Math.cos(phase) * 0.28,
                    player.getY() + 1.72,
                    player.getZ() - Math.sin(phase) * 0.28,
                    (note + 0.35) % 1.0, 0.0, 0.0);
        }
    }
}
