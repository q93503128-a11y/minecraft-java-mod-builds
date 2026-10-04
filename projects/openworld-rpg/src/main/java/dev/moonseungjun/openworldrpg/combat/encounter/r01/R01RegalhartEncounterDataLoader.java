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

public final class R01RegalhartEncounterDataLoader {
    public static final String RESOURCE =
            "/data/openworld_rpg/combat/r01/regalhart.json";
    private static final Gson GSON = new GsonBuilder().create();

    private R01RegalhartEncounterDataLoader() {
    }

    public static R01RegalhartEncounterData load() {
        try (InputStream stream =
                     R01RegalhartEncounterDataLoader.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled R01 Regalhart encounter data: " + RESOURCE
                );
            }
            var data = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    R01RegalhartEncounterData.class
            );
            validate(data);
            return data;
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "Could not load bundled R01 Regalhart encounter data: " + RESOURCE,
                    exception
            );
        }
    }

    public static void validate(R01RegalhartEncounterData data) {
        Objects.requireNonNull(data, "data");
        if (!"openworld_rpg:r01/regalhart".equals(data.id())
                || !R01ExternalActorCatalog.REGALHART.equals(data.entityId())
                || data.contentLevel() != 8
                || data.decisionDelayTicks() != 2
                || Math.abs(data.weakPointMultiplier() - 1.25) > 0.000001) {
            throw new IllegalArgumentException("R01 Regalhart identity contract drifted.");
        }

        Map<R01RegalhartEncounterData.ActionId,
                R01RegalhartEncounterData.AttackRule> rules = data.rulesById();
        if (!rules.keySet().equals(
                EnumSet.allOf(R01RegalhartEncounterData.ActionId.class)
        )) {
            throw new IllegalArgumentException(
                    "R01 Regalhart action set drifted: " + rules.keySet()
            );
        }

        requireRule(rules, new R01RegalhartEncounterData.AttackRule(
                R01RegalhartEncounterData.ActionId.ANTLER_SWEEP,
                0, 9, 8,
                4.5, 0.0, 0.0, 0.11,
                PlayerDefenseAuthority.GuardPressureBand.MEDIUM,
                Boolean.TRUE, Boolean.TRUE,
                null, null
        ));
        requireRule(rules, new R01RegalhartEncounterData.AttackRule(
                R01RegalhartEncounterData.ActionId.CROWN_CHARGE,
                140, 18, 24,
                16.0, 16.0, 0.0, 0.28,
                PlayerDefenseAuthority.GuardPressureBand.HEAVY,
                Boolean.FALSE, Boolean.TRUE,
                70.0, 1.25
        ));
        requireRule(rules, new R01RegalhartEncounterData.AttackRule(
                R01RegalhartEncounterData.ActionId.REAR_KICK,
                60, 8, 7,
                3.5, 0.0, 0.0, 0.12,
                PlayerDefenseAuthority.GuardPressureBand.MEDIUM,
                Boolean.TRUE, Boolean.TRUE,
                28.0, null
        ));
        requireRule(rules, new R01RegalhartEncounterData.AttackRule(
                R01RegalhartEncounterData.ActionId.ROYAL_BOUND,
                180, 22, 22,
                16.0, 0.0, 4.5, 0.30,
                null,
                Boolean.FALSE, Boolean.FALSE,
                null, null
        ));

        var expectedSovereign = new R01RegalhartEncounterData.SovereignRule(
                0.40,
                30,
                0.50,
                1.10,
                2,
                6.0,
                13,
                28,
                3,
                2,
                8,
                6
        );
        if (!expectedSovereign.equals(data.sovereign())) {
            throw new IllegalArgumentException(
                    "R01 Regalhart Sovereign contract drifted: " + data.sovereign()
            );
        }

        for (R01RegalhartEncounterData.ActionId action
                : R01RegalhartEncounterData.ActionId.values()) {
            if (!rules.get(action).impactContractClosed()) {
                throw new IllegalArgumentException(
                        "R01 Regalhart impact contract must be closed for " + action
                );
            }
        }
    }

    private static void requireRule(
            Map<R01RegalhartEncounterData.ActionId,
                    R01RegalhartEncounterData.AttackRule> rules,
            R01RegalhartEncounterData.AttackRule expected
    ) {
        var actual = rules.get(expected.id());
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                    "R01 Regalhart action drifted for " + expected.id()
                            + ": expected=" + expected + ", actual=" + actual
            );
        }
    }
}
