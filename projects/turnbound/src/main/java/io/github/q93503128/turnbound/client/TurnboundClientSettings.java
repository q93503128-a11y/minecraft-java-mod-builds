package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Small persistent TURNBOUND-only client preferences. Vanilla video/input settings stay in Minecraft options. */
final class TurnboundClientSettings {
    private static final int[] VOLUME_STEPS = {100, 75, 50, 25, 0};
    private static final String FILE_NAME = "turnbound-client.properties";

    private static boolean loaded;
    private static boolean musicEnabled = true;
    private static int musicVolume = 100;
    private static boolean sfxEnabled = true;
    private static int sfxVolume = 100;
    private static boolean impactCameraEnabled = true;
    private static boolean minimapEnabled = true;

    private TurnboundClientSettings() {}

    static boolean musicEnabled() { load(); return musicEnabled; }
    static float musicGain() { load(); return musicVolume / 100.0F; }
    static int musicPercent() { load(); return musicVolume; }
    static boolean sfxEnabled() { load(); return sfxEnabled; }
    static float sfxGain() { load(); return sfxVolume / 100.0F; }
    static int sfxPercent() { load(); return sfxVolume; }
    static boolean impactCameraEnabled() { load(); return impactCameraEnabled; }
    static boolean minimapEnabled() { load(); return minimapEnabled; }

    static void toggleMusic() { load(); musicEnabled = !musicEnabled; save(); }
    static void cycleMusicVolume() { load(); musicVolume = nextVolume(musicVolume); save(); }
    static void toggleSfx() { load(); sfxEnabled = !sfxEnabled; save(); }
    static void cycleSfxVolume() { load(); sfxVolume = nextVolume(sfxVolume); save(); }
    static void toggleImpactCamera() { load(); impactCameraEnabled = !impactCameraEnabled; save(); }
    static void toggleMinimap() { load(); minimapEnabled = !minimapEnabled; save(); }

    private static int nextVolume(int current) {
        for (int i = 0; i < VOLUME_STEPS.length; i++) {
            if (VOLUME_STEPS[i] == current) return VOLUME_STEPS[(i + 1) % VOLUME_STEPS.length];
        }
        return 100;
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        Path path = path();
        if (!Files.isRegularFile(path)) return;
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
            musicEnabled = bool(properties, "musicEnabled", true);
            musicVolume = volume(properties, "musicVolume", 100);
            sfxEnabled = bool(properties, "sfxEnabled", true);
            sfxVolume = volume(properties, "sfxVolume", 100);
            impactCameraEnabled = bool(properties, "impactCameraEnabled", true);
            minimapEnabled = bool(properties, "minimapEnabled", true);
        } catch (IOException ignored) {
            // Preferences are optional. A read failure falls back to safe defaults without affecting gameplay.
        }
    }

    private static void save() {
        Path path = path();
        Properties properties = new Properties();
        properties.setProperty("musicEnabled", Boolean.toString(musicEnabled));
        properties.setProperty("musicVolume", Integer.toString(musicVolume));
        properties.setProperty("sfxEnabled", Boolean.toString(sfxEnabled));
        properties.setProperty("sfxVolume", Integer.toString(sfxVolume));
        properties.setProperty("impactCameraEnabled", Boolean.toString(impactCameraEnabled));
        properties.setProperty("minimapEnabled", Boolean.toString(minimapEnabled));
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out, "TURNBOUND client preferences");
            }
        } catch (IOException ignored) {
            // Never let a preference write failure interrupt a client session.
        }
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static int volume(Properties properties, String key, int fallback) {
        try {
            int value = Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
            for (int step : VOLUME_STEPS) if (step == value) return step;
        } catch (NumberFormatException ignored) {}
        return fallback;
    }
}
