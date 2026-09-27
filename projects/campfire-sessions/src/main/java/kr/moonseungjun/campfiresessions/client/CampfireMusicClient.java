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
    private static long playStartedNanos;
    private static long lastParticlePulse = Long.MIN_VALUE;
    private static boolean repeatOne;

    private CampfireMusicClient() {}

    public static MusicTrack selectedTrack() { return MusicCatalog.get(selectedIndex); }
    public static int selectedIndex() { return selectedIndex; }
    public static int playingIndex() { return playingIndex; }
    public static boolean isPlaying() { return current != null; }
    public static boolean isPlayingSelected() { return current != null && playingIndex == selectedIndex; }
    public static boolean isRepeatOne() { return repeatOne; }
    public static int playingBpm() { return current != null && playingIndex >= 0 ? MusicCatalog.get(playingIndex).bpm() : selectedTrack().bpm(); }

    static void catalogChanged() {
        int size = MusicCatalog.size();
        if (size <= 0) return;
        selectedIndex = Math.floorMod(selectedIndex, size);
        if (playingIndex >= size) stop();
    }

    public static void previewSelect(int index) {
        selectedIndex = Math.floorMod(index, MusicCatalog.size());
    }

    public static void select(int index) {
        int next = Math.floorMod(index, MusicCatalog.size());
        boolean resume = isPlaying();
        selectedIndex = next;
        if (resume) playSelected();
    }

    public static void previous() { select(selectedIndex - 1); }
    public static void next() { select(selectedIndex + 1); }

    public static void toggleRepeatOne() {
        repeatOne = !repeatOne;
    }

    public static void toggleSelected() {
        if (isPlayingSelected()) stop();
        else playSelected();
    }

    public static void playSelected() {
        playIndex(selectedIndex);
    }

    private static void playIndex(int index) {
        stop();
        selectedIndex = Math.floorMod(index, MusicCatalog.size());
        MusicTrack track = selectedTrack();
        SoundInstance next = SimpleSoundInstance.forMusic(track.sound().get());
        Minecraft.getInstance().getSoundManager().play(next);
        current = next;
        playingIndex = selectedIndex;
        playStartedNanos = System.nanoTime();
        lastParticlePulse = Long.MIN_VALUE;
    }

    public static void stop() {
        if (current != null) Minecraft.getInstance().getSoundManager().stop(current);
        current = null;
        playingIndex = -1;
        playStartedNanos = 0L;
        lastParticlePulse = Long.MIN_VALUE;
    }

    public static double elapsedSecondsExact() {
        if (current == null || playStartedNanos == 0L) return 0.0;
        return Math.max(0.0, (System.nanoTime() - playStartedNanos) / 1_000_000_000.0);
    }

    public static int elapsedSeconds() {
        return (int) Math.floor(elapsedSecondsExact());
    }

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

        MusicTrack playing = MusicCatalog.get(playingIndex);
        int duration = MusicMetadata.durationSeconds(playing.id());
        if (duration > 0 && elapsedSecondsExact() >= duration) {
            if (repeatOne) playIndex(playingIndex);
            else playIndex(playingIndex + 1);
            return;
        }

        int pulsesPerBeat = playing.bpm() >= 140 ? 2 : 1;
        double beat = elapsedSecondsExact() * playing.bpm() / 60.0;
        long pulse = (long) Math.floor(beat * pulsesPerBeat);
        if (pulse == lastParticlePulse) return;
        lastParticlePulse = pulse;

        double phase = pulse * 0.78;
        double energy = Math.min(1.0, Math.max(0.0, (playing.bpm() - 70.0) / 90.0));
        double radius = 0.30 + energy * 0.12 + 0.04 * Math.sin(pulse * 0.9);
        double x = player.getX() + Math.cos(phase) * radius;
        double y = player.getY() + 1.42 + ((pulse & 1L) == 0L ? 0.05 : 0.16 + energy * 0.05);
        double z = player.getZ() + Math.sin(phase) * radius;
        double noteColor = Math.floorMod(pulse * 5L + playing.bpm(), 24L) / 24.0;
        level.addParticle(ParticleTypes.NOTE, x, y, z, noteColor, 0.0, 0.0);

        long downbeatInterval = 4L * pulsesPerBeat;
        if (pulse % downbeatInterval == 0L) {
            level.addParticle(ParticleTypes.NOTE,
                    player.getX() - Math.cos(phase) * (0.20 + energy * 0.10),
                    player.getY() + 1.66 + energy * 0.04,
                    player.getZ() - Math.sin(phase) * (0.20 + energy * 0.10),
                    (noteColor + 0.34) % 1.0, 0.0, 0.0);
        }
    }
}
