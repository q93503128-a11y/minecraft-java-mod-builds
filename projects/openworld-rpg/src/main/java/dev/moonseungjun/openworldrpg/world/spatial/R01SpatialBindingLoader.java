package dev.moonseungjun.openworldrpg.world.spatial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class R01SpatialBindingLoader {
    public static final String BUNDLED_RESOURCE =
            "/data/openworld_rpg/world/r01_spatial_candidates.json";
    private static final Gson GSON = new GsonBuilder().create();

    private R01SpatialBindingLoader() {
    }

    public static R01SpatialBindingData loadBundled() {
        try (InputStream stream =
                     R01SpatialBindingLoader.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled R01 spatial candidate data: " + BUNDLED_RESOURCE
                );
            }
            return parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "Could not load bundled R01 spatial candidate data.",
                    exception
            );
        }
    }

    public static R01SpatialBindingData parse(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        R01SpatialBindingData data =
                GSON.fromJson(reader, R01SpatialBindingData.class);
        R01SpatialBindingData.validate(data);
        return data;
    }
}
