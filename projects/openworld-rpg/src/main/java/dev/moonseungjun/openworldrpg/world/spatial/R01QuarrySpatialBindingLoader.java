package dev.moonseungjun.openworldrpg.world.spatial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class R01QuarrySpatialBindingLoader {
    public static final String BUNDLED_RESOURCE =
            "/data/openworld_rpg/world/r01_quarry_runtime_bindings.json";
    private static final Gson GSON = new GsonBuilder().create();

    private R01QuarrySpatialBindingLoader() {
    }

    public static R01QuarrySpatialBindingData loadBundled() {
        R01QuarrySpatialBindingData data;
        try (InputStream stream =
                     R01QuarrySpatialBindingLoader.class.getResourceAsStream(
                             BUNDLED_RESOURCE
                     )) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled R01 Quarry runtime binding data: "
                                + BUNDLED_RESOURCE
                );
            }
            data = parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "Could not load bundled R01 Quarry runtime binding data.",
                    exception
            );
        }

        R01QuarrySpatialBindingData.validateAgainstSpatial(
                data,
                R01SpatialBindingLoader.loadBundled()
        );
        return data;
    }

    public static R01QuarrySpatialBindingData parse(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        R01QuarrySpatialBindingData data =
                GSON.fromJson(reader, R01QuarrySpatialBindingData.class);
        R01QuarrySpatialBindingData.validate(data);
        return data;
    }
}
