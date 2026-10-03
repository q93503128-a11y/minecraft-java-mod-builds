package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

public final class R01SteelboarEncounterDataLoader {
    public static final String RESOURCE =
            "/data/openworld_rpg/combat/r01/steelboar.json";
    private static final Gson GSON = new GsonBuilder().create();

    private R01SteelboarEncounterDataLoader() {
    }

    public static R01SteelboarEncounterData load() {
        try (InputStream stream =
                     R01SteelboarEncounterDataLoader.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled R01 Steelboar encounter data: " + RESOURCE
                );
            }
            var data = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    R01SteelboarEncounterData.class
            );
            validate(data);
            return data;
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "Could not load bundled R01 Steelboar encounter data: " + RESOURCE,
                    exception
            );
        }
    }

    public static void validate(R01SteelboarEncounterData data) {
        Objects.requireNonNull(data, "data");
        if (!"openworld_rpg:r01/steelboar".equals(data.id())
                || !R01ExternalActorCatalog.STEELBOAR.equals(data.entityId())
                || data.contentLevel() != 6
                || data.decisionDelayTicks() != 2) {
            throw new IllegalArgumentException("R01 Steelboar identity contract drifted.");
        }

        Map<R01SteelboarEncounterData.ActionId,
                R01SteelboarEncounterData.AttackRule> rules = data.rulesById();
        if (!rules.keySet().equals(EnumSet.allOf(R01SteelboarEncounterData.ActionId.class))) {
            throw new IllegalArgumentException(
                    "R01 Steelboar action set drifted: " + rules.keySet()
            );
        }

        requireRule(rules, new R01SteelboarEncounterData.AttackRule(
                R01SteelboarEncounterData.ActionId.IRON_TUSK,
                60, 0, 8, 7,
                0.0, 3.0, 0.0,
                0.13,
                PlayerDefenseAuthority.GuardPressureBand.MEDIUM,
                Boolean.TRUE, true,
                28.0,
                null,
                null
        ));
        requireRule(rules, new R01SteelboarEncounterData.AttackRule(
                R01SteelboarEncounterData.ActionId.SHOULDER_HOOK,
                40, 60, 12, 14,
                0.0, 3.0, 0.0,
                0.20,
                PlayerDefenseAuthority.GuardPressureBand.HEAVY,
                Boolean.TRUE, true,
                45.0,
                null,
                null
        ));
        requireRule(rules, new R01SteelboarEncounterData.AttackRule(
                R01SteelboarEncounterData.ActionId.IRON_RUSH,
                100, 140, 17, 24,
                5.5, 12.0, 12.0,
                0.28,
                PlayerDefenseAuthority.GuardPressureBand.HEAVY,
                null, true,
                70.0,
                1.40,
                18.0
        ));

        var expectedFurious = new R01SteelboarEncounterData.FuriousRouteRule(
                0.35,
                280,
                6.0,
                12.0,
                12,
                28
        );
        if (!expectedFurious.equals(data.furiousRoute())) {
            throw new IllegalArgumentException(
                    "R01 Steelboar Furious Route contract drifted: " + data.furiousRoute()
            );
        }

        if (rules.get(R01SteelboarEncounterData.ActionId.IRON_RUSH)
                .impactContractClosed()) {
            throw new IllegalArgumentException(
                    "Iron Rush ordinary guardability must remain unresolved until canon states it."
            );
        }
    }

    private static void requireRule(
            Map<R01SteelboarEncounterData.ActionId,
                    R01SteelboarEncounterData.AttackRule> rules,
            R01SteelboarEncounterData.AttackRule expected
    ) {
        var actual = rules.get(expected.id());
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                    "R01 Steelboar action drifted for " + expected.id()
                            + ": expected=" + expected + ", actual=" + actual
            );
        }
    }
}
