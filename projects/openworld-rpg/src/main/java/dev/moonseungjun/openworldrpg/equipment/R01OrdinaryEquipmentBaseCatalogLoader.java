package dev.moonseungjun.openworldrpg.equipment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class R01OrdinaryEquipmentBaseCatalogLoader {
    public static final String BUNDLED_RESOURCE =
            "/data/openworld_rpg/equipment/r01_ordinary_equipment_bases.json";
    private static final Gson GSON = new GsonBuilder().create();

    private R01OrdinaryEquipmentBaseCatalogLoader() {
    }

    public static R01OrdinaryEquipmentBaseCatalogData loadBundled() {
        try (InputStream stream =
                     R01OrdinaryEquipmentBaseCatalogLoader.class
                             .getResourceAsStream(BUNDLED_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled R01 equipment-base catalog: "
                                + BUNDLED_RESOURCE
                );
            }
            return parse(new InputStreamReader(
                    stream,
                    StandardCharsets.UTF_8
            ));
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "Could not load R01 equipment-base catalog.",
                    exception
            );
        }
    }

    public static R01OrdinaryEquipmentBaseCatalogData parse(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        R01OrdinaryEquipmentBaseCatalogData data = GSON.fromJson(
                reader,
                R01OrdinaryEquipmentBaseCatalogData.class
        );
        R01OrdinaryEquipmentBaseCatalogData.validate(data);
        return data;
    }
}
