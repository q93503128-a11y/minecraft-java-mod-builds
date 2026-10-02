package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class R01SecondaryCreatureEncounterDataLoader {
    public static final String CAVE_CENTIPEDE_RESOURCE =
            "/data/openworld_rpg/combat/r01/cave_centipede.json";
    public static final String NATURE_SPIRIT_RESOURCE =
            "/data/openworld_rpg/combat/r01/nature_spirit.json";
    private static final Gson GSON = new GsonBuilder().create();

    private R01SecondaryCreatureEncounterDataLoader() {
    }

    public static R01SecondaryCreatureEncounterData loadCaveCentipede() {
        return load(CAVE_CENTIPEDE_RESOURCE);
    }

    public static R01SecondaryCreatureEncounterData loadNatureSpirit() {
        return load(NATURE_SPIRIT_RESOURCE);
    }

    private static R01SecondaryCreatureEncounterData load(String resource) {
        try (InputStream stream =
                     R01SecondaryCreatureEncounterDataLoader.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled R01 secondary-creature encounter data: " + resource
                );
            }
            var data = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    R01SecondaryCreatureEncounterData.class
            );
            validate(data);
            return data;
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "Could not load bundled R01 secondary-creature encounter data: " + resource,
                    exception
            );
        }
    }

    public static void validate(R01SecondaryCreatureEncounterData data) {
        Objects.requireNonNull(data, "data");
        switch (data.kind()) {
            case CAVE_CENTIPEDE -> validateCaveCentipede(data);
            case NATURE_SPIRIT -> validateNatureSpirit(data);
        }
    }

    private static void validateCaveCentipede(R01SecondaryCreatureEncounterData data) {
        if (!"openworld_rpg:r01/cave_centipede".equals(data.id())
                || !R01ExternalActorCatalog.CAVE_CENTIPEDE_HEAD.equals(data.entityId())
                || data.contentLevel() != 4
                || data.livingShell() != null) {
            throw new IllegalArgumentException("R01 Cave Centipede identity contract drifted.");
        }
        Map<R01SecondaryCreatureEncounterData.ActionId,
                R01SecondaryCreatureEncounterData.ActionRule> rules = data.rulesById();
        requireExactActionSet(
                rules.keySet(),
                EnumSet.of(
                        R01SecondaryCreatureEncounterData.ActionId.SCUTTLE_BITE,
                        R01SecondaryCreatureEncounterData.ActionId.BODY_RAKE,
                        R01SecondaryCreatureEncounterData.ActionId.CEILING_DROP
                ),
                "Cave Centipede"
        );

        requireRule(rules, new R01SecondaryCreatureEncounterData.ActionRule(
                R01SecondaryCreatureEncounterData.ActionId.SCUTTLE_BITE,
                60, 0, 7, 0, 8,
                0.0, 2.3, false, 0.0, 0.0,
                0.11, ProjectImpactTransaction.DamageSchool.PHYSICAL,
                PlayerDefenseAuthority.GuardPressureBand.LIGHT,
                true, true, 30.0, 0.0
        ));
        requireRule(rules, new R01SecondaryCreatureEncounterData.ActionRule(
                R01SecondaryCreatureEncounterData.ActionId.BODY_RAKE,
                40, 0, 13, 5, 13,
                0.0, 3.2, false, 0.0, 0.0,
                0.18, ProjectImpactTransaction.DamageSchool.PHYSICAL,
                PlayerDefenseAuthority.GuardPressureBand.MEDIUM,
                true, true, 0.0, 28.0
        ));
        requireRule(rules, new R01SecondaryCreatureEncounterData.ActionRule(
                R01SecondaryCreatureEncounterData.ActionId.CEILING_DROP,
                100, 160, 14, 0, 15,
                0.0, 2.3, true, 0.0, 2.3,
                0.16, ProjectImpactTransaction.DamageSchool.PHYSICAL,
                PlayerDefenseAuthority.GuardPressureBand.MEDIUM,
                true, true, 20.0, 0.0
        ));
    }

    private static void validateNatureSpirit(R01SecondaryCreatureEncounterData data) {
        if (!"openworld_rpg:r01/nature_spirit".equals(data.id())
                || !R01ExternalActorCatalog.NATURE_SPIRIT.equals(data.entityId())
                || data.contentLevel() != 7
                || data.livingShell() == null) {
            throw new IllegalArgumentException("R01 Nature Spirit identity contract drifted.");
        }
        Map<R01SecondaryCreatureEncounterData.ActionId,
                R01SecondaryCreatureEncounterData.ActionRule> rules = data.rulesById();
        requireExactActionSet(
                rules.keySet(),
                EnumSet.of(
                        R01SecondaryCreatureEncounterData.ActionId.ROOTED_SWIPE,
                        R01SecondaryCreatureEncounterData.ActionId.EARTHEN_RAM,
                        R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE
                ),
                "Nature Spirit"
        );

        requireRule(rules, new R01SecondaryCreatureEncounterData.ActionRule(
                R01SecondaryCreatureEncounterData.ActionId.ROOTED_SWIPE,
                60, 0, 9, 0, 0,
                0.0, 3.2, false, 0.0, 0.0,
                0.13, ProjectImpactTransaction.DamageSchool.PHYSICAL,
                PlayerDefenseAuthority.GuardPressureBand.MEDIUM,
                true, true, 0.0, 0.0
        ));
        requireRule(rules, new R01SecondaryCreatureEncounterData.ActionRule(
                R01SecondaryCreatureEncounterData.ActionId.EARTHEN_RAM,
                40, 80, 15, 0, 17,
                2.5, 5.0, false, 3.0, 0.0,
                0.22, ProjectImpactTransaction.DamageSchool.PHYSICAL,
                PlayerDefenseAuthority.GuardPressureBand.HEAVY,
                true, true, 0.0, 45.0
        ));
        requireRule(rules, new R01SecondaryCreatureEncounterData.ActionRule(
                R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE,
                20, 160, 20, 0, 18,
                0.0, 4.0, false, 0.0, 4.0,
                0.25, ProjectImpactTransaction.DamageSchool.PHYSICAL,
                null,
                false, false, 0.0, 45.0
        ));

        var shell = data.livingShell();
        var expected = new R01SecondaryCreatureEncounterData.LivingShellRule(
                50, 240, 0.65, 1.25, 0.20, 80, 0.40, 4.0
        );
        if (!expected.equals(shell)) {
            throw new IllegalArgumentException(
                    "R01 Nature Spirit Living Shell contract drifted: " + shell
            );
        }
    }

    private static void requireExactActionSet(
            Set<R01SecondaryCreatureEncounterData.ActionId> actual,
            Set<R01SecondaryCreatureEncounterData.ActionId> expected,
            String label
    ) {
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException(
                    "R01 " + label + " action set drifted: " + actual
            );
        }
    }

    private static void requireRule(
            Map<R01SecondaryCreatureEncounterData.ActionId,
                    R01SecondaryCreatureEncounterData.ActionRule> rules,
            R01SecondaryCreatureEncounterData.ActionRule expected
    ) {
        var actual = rules.get(expected.id());
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                    "R01 secondary-creature action drifted for " + expected.id()
                            + ": expected=" + expected + ", actual=" + actual
            );
        }
        var hit = actual.toIncomingHit(
                expected.id() == R01SecondaryCreatureEncounterData.ActionId.ROOTED_SWIPE
                        || expected.id() == R01SecondaryCreatureEncounterData.ActionId.EARTHEN_RAM
                        || expected.id() == R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE
                        ? 7
                        : 4
        );
        if (!Double.isFinite(hit.rawDamage()) || hit.rawDamage() <= 0.0) {
            throw new IllegalArgumentException(
                    "R01 secondary-creature impact produced invalid raw damage: " + expected.id()
            );
        }
    }
}
