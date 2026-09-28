package dev.moonseungjun.openworldrpg.world.structure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingLoader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class R01StructureBindingLoader {
    public static final String BUNDLED_RESOURCE =
            "/data/openworld_rpg/world/r01_alderford_structure_bindings.json";
    private static final Gson GSON = new GsonBuilder().create();

    private R01StructureBindingLoader() {
    }

    public static R01StructureBindingData loadBundled() {
        R01StructureBindingData data;
        try (InputStream stream =
                     R01StructureBindingLoader.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled R01 Alderford structure binding data: "
                                + BUNDLED_RESOURCE
                );
            }
            data = parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "Could not load bundled R01 Alderford structure binding data.",
                    exception
            );
        }

        R01StructureBindingData.validateAgainstSpatial(
                data,
                R01SpatialBindingLoader.loadBundled()
        );
        return data;
    }

    public static R01StructureBindingData parse(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        R01StructureBindingData data =
                GSON.fromJson(reader, R01StructureBindingData.class);
        R01StructureBindingData.validate(data);
        return data;
    }
}
