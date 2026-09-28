package dev.moonseungjun.openworldrpg.world.structure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class R01AlderfordRuntimeBindingLoader {
    public static final String BUNDLED_RESOURCE =
            "/data/openworld_rpg/world/r01_alderford_runtime_bindings.json";
    private static final Gson GSON = new GsonBuilder().create();

    private R01AlderfordRuntimeBindingLoader() {
    }

    public static R01AlderfordRuntimeBindingData loadBundled() {
        R01StructureBindingData structures =
                R01StructureBindingLoader.loadBundled();
        R01AlderfordRuntimeBindingData data;
        try (InputStream stream =
                     R01AlderfordRuntimeBindingLoader.class
                             .getResourceAsStream(BUNDLED_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled Alderford runtime bindings: "
                                + BUNDLED_RESOURCE
                );
            }
            data = parse(
                    new InputStreamReader(
                            stream,
                            StandardCharsets.UTF_8
                    ),
                    structures
            );
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "Could not load bundled Alderford runtime bindings.",
                    exception
            );
        }
        return data;
    }

    public static R01AlderfordRuntimeBindingData parse(
            Reader reader,
            R01StructureBindingData structures
    ) {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(structures, "structures");
        R01AlderfordRuntimeBindingData data =
                GSON.fromJson(
                        reader,
                        R01AlderfordRuntimeBindingData.class
                );
        R01AlderfordRuntimeBindingData.validate(data, structures);
        return data;
    }
}
