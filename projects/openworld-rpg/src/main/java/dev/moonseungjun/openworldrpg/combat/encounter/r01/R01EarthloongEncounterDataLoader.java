package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class R01EarthloongEncounterDataLoader {
    public static final String BUNDLED_RESOURCE =
            "/data/openworld_rpg/combat/bosses/r01_earthloong.json";
    private static final Gson GSON = new GsonBuilder().create();

    private R01EarthloongEncounterDataLoader() {
    }

    public static R01EarthloongEncounterData loadBundled() {
        try (InputStream stream =
                     R01EarthloongEncounterDataLoader.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled R01 Earthloong encounter data: " + BUNDLED_RESOURCE
                );
            }
            return parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "Could not load bundled R01 Earthloong encounter data.",
                    exception
            );
        }
    }

    public static R01EarthloongEncounterData parse(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        R01EarthloongEncounterData data =
                GSON.fromJson(reader, R01EarthloongEncounterData.class);
        validate(data);
        return data;
    }

    public static void validate(R01EarthloongEncounterData data) {
        if (data == null) {
            throw new IllegalArgumentException("Earthloong encounter data parsed to null.");
        }
        if (!"openworld_rpg:r01/earthloong".equals(data.id())) {
            throw new IllegalArgumentException("Unexpected Earthloong encounter id: " + data.id());
        }
        if (data.contentLevel() != 8) {
            throw new IllegalArgumentException(
                    "R01 Earthloong content level must remain 8: " + data.contentLevel()
            );
        }
        if (data.decisionDelayTicks() != 2) {
            throw new IllegalArgumentException(
                    "R01 Earthloong decision delay must remain 2 ticks: "
                            + data.decisionDelayTicks()
            );
        }
        if (Double.compare(data.phaseTwoHealthThreshold(), 0.55) != 0) {
            throw new IllegalArgumentException(
                    "R01 Earthloong phase-two threshold must remain 0.55."
            );
        }

        var furrow = data.lightningFurrow();
        if (furrow.phaseOneLaneCount() != 3
                || furrow.phaseTwoFirstLaneCount() != 4
                || !furrow.phaseTwoAlternatingLaneCounts().equals(java.util.List.of(3, 4))) {
            throw new IllegalArgumentException(
                    "R01 Earthloong Lightning Furrow lane contract drifted."
            );
        }

        Set<R01EarthloongEncounterData.ActionId> seen =
                EnumSet.noneOf(R01EarthloongEncounterData.ActionId.class);
        for (var rule : data.actions()) {
            if (rule.id() == null
                    || rule.minimumPhase() < 1
                    || rule.minimumPhase() > 2
                    || rule.weight() <= 0
                    || rule.cooldownTicks() < 0
                    || !seen.add(rule.id())) {
                throw new IllegalArgumentException(
                        "Invalid/duplicate R01 Earthloong action rule: " + rule
                );
            }
        }

        Set<R01EarthloongEncounterData.ActionId> expected =
                EnumSet.allOf(R01EarthloongEncounterData.ActionId.class);
        if (!seen.equals(expected)) {
            Set<R01EarthloongEncounterData.ActionId> missing = new HashSet<>(expected);
            missing.removeAll(seen);
            throw new IllegalArgumentException(
                    "R01 Earthloong action data is incomplete; missing=" + missing
            );
        }

        var rules = data.rulesById();
        requireRule(rules, R01EarthloongEncounterData.ActionId.CLAW_SWEEP, 1, 50, 0, false);
        requireRule(rules, R01EarthloongEncounterData.ActionId.TAIL_SCYTHE, 1, 55, 0, false);
        requireRule(rules, R01EarthloongEncounterData.ActionId.QUARRY_RUSH, 1, 45, 140, false);
        requireRule(rules, R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW, 1, 35, 160, true);
        requireRule(rules, R01EarthloongEncounterData.ActionId.ROOT_BREAKER, 1, 45, 180, true);
        requireRule(rules, R01EarthloongEncounterData.ActionId.FORKED_HEAVEN, 2, 40, 200, true);
        requireRule(rules, R01EarthloongEncounterData.ActionId.EARTHLINE_SURGE, 2, 45, 140, true);

        var impacts = data.impactRulesById();
        Set<R01EarthloongEncounterData.ActionId> expectedImpacts = EnumSet.of(
                R01EarthloongEncounterData.ActionId.CLAW_SWEEP,
                R01EarthloongEncounterData.ActionId.TAIL_SCYTHE,
                R01EarthloongEncounterData.ActionId.QUARRY_RUSH
        );
        if (!impacts.keySet().equals(expectedImpacts)) {
            throw new IllegalArgumentException(
                    "R01 Earthloong canon-closed impact set drifted: " + impacts.keySet()
            );
        }

        requireImpact(
                impacts,
                R01EarthloongEncounterData.ActionId.CLAW_SWEEP,
                0.10,
                dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority
                        .GuardPressureBand.MEDIUM,
                true,
                true
        );
        requireImpact(
                impacts,
                R01EarthloongEncounterData.ActionId.TAIL_SCYTHE,
                0.20,
                dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority
                        .GuardPressureBand.HEAVY,
                true,
                true
        );
        requireImpact(
                impacts,
                R01EarthloongEncounterData.ActionId.QUARRY_RUSH,
                0.24,
                dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority
                        .GuardPressureBand.HEAVY,
                false,
                true
        );
    }

    private static void requireImpact(
            java.util.Map<R01EarthloongEncounterData.ActionId,
                    R01EarthloongEncounterData.ImpactRule> impacts,
            R01EarthloongEncounterData.ActionId action,
            double benchmarkDamageShare,
            dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority
                    .GuardPressureBand guardPressure,
            boolean guardable,
            boolean perfectGuardable
    ) {
        var actual = impacts.get(action);
        var expected = new R01EarthloongEncounterData.ImpactRule(
                action,
                benchmarkDamageShare,
                dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction
                        .DamageSchool.PHYSICAL,
                guardPressure,
                guardable,
                perfectGuardable
        );
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                    "R01 Earthloong impact rule drifted for " + action
                            + ": expected=" + expected + ", actual=" + actual
            );
        }

        var hit = actual.toIncomingHit(8);
        if (!Double.isFinite(hit.rawDamage()) || hit.rawDamage() <= 0.0) {
            throw new IllegalArgumentException(
                    "R01 Earthloong impact rule produced invalid raw damage: " + action
            );
        }
    }

    private static void requireRule(
            java.util.Map<R01EarthloongEncounterData.ActionId,
                    R01EarthloongEncounterData.ActionRule> rules,
            R01EarthloongEncounterData.ActionId id,
            int minimumPhase,
            int weight,
            int cooldownTicks,
            boolean spaceControl
    ) {
        var actual = rules.get(id);
        var expected = new R01EarthloongEncounterData.ActionRule(
                id,
                minimumPhase,
                weight,
                cooldownTicks,
                spaceControl
        );
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                    "R01 Earthloong action rule drifted for " + id
                            + ": expected=" + expected + ", actual=" + actual
            );
        }
    }
}
