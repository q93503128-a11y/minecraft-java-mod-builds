package kr.moonseungjun.campfiresessions.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class MusicMetadata {
    private static final String RESOURCE = "/assets/campfiresessions/music/track_metadata.json";
    private static final Map<String, Integer> DURATIONS = loadDurations();

    private MusicMetadata() {}

    public static int durationSeconds(String id) {
        return DURATIONS.getOrDefault(id, 0);
    }

    private static Map<String, Integer> loadDurations() {
        Map<String, Integer> result = new HashMap<>();
        try (InputStream stream = MusicMetadata.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) return result;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : root.entrySet()) {
                JsonObject track = entry.getValue().getAsJsonObject();
                result.put(entry.getKey(), track.get("duration_seconds").getAsInt());
            }
        } catch (Exception ignored) {
            // UI falls back to "--:--" if generated metadata is unavailable.
        }
        return result;
    }
}
