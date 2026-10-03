package dev.moonseungjun.openworldrpg.world.spatial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class R01RegalhartSpatialBindingLoader {
    public static final String BUNDLED_RESOURCE =
            "/data/openworld_rpg/world/r01_regalhart_spatial_bindings.json";
    private static final Gson GSON = new GsonBuilder().create();

    private R01RegalhartSpatialBindingLoader() {
    }

    public static R01RegalhartSpatialBindingData loadBundled() {
        try (InputStream stream =
                     R01RegalhartSpatialBindingLoader.class
                             .getResourceAsStream(BUNDLED_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled Regalhart spatial binding: "
                                + BUNDLED_RESOURCE
                );
            }
            return parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "Could not load bundled Regalhart spatial binding.",
                    exception
            );
        }
    }

    public static R01RegalhartSpatialBindingData parse(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        R01RegalhartSpatialBindingData data =
                GSON.fromJson(reader, R01RegalhartSpatialBindingData.class);
        R01RegalhartSpatialBindingData.validate(data);
        return data;
    }
}
