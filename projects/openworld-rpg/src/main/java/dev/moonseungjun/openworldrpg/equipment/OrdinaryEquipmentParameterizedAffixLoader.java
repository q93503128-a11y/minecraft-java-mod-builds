package dev.moonseungjun.openworldrpg.equipment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class OrdinaryEquipmentParameterizedAffixLoader {
    public static final String BUNDLED_RESOURCE =
            "/data/openworld_rpg/equipment/parameterized_affix_templates.json";
    private static final Gson GSON = new GsonBuilder().create();

    private OrdinaryEquipmentParameterizedAffixLoader() {
    }

    public static OrdinaryEquipmentParameterizedAffixData loadBundled() {
        try (InputStream stream =
                     OrdinaryEquipmentParameterizedAffixLoader.class
                             .getResourceAsStream(BUNDLED_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled parameterized-affix templates: "
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
                    "Could not load parameterized-affix templates.",
                    exception
            );
        }
    }

    public static OrdinaryEquipmentParameterizedAffixData parse(
            Reader reader
    ) {
        Objects.requireNonNull(reader, "reader");
        OrdinaryEquipmentParameterizedAffixData data = GSON.fromJson(
                reader,
                OrdinaryEquipmentParameterizedAffixData.class
        );
        OrdinaryEquipmentParameterizedAffixData.validate(data);
        return data;
    }
}
