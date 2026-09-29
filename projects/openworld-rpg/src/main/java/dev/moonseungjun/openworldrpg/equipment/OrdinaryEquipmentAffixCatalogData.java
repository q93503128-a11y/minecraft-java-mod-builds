package dev.moonseungjun.openworldrpg.equipment;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Data-owned canonical ordinary affix values plus current runtime-adapter readiness. */
public record OrdinaryEquipmentAffixCatalogData(
        @SerializedName("schema_version") int schemaVersion,
        String id,
        @SerializedName("primary_curve") PrimaryCurveData primaryCurve,
        List<AffixEntry> affixes
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String CANONICAL_ID =
            "openworld_rpg:equipment/ordinary_affix_catalog";

    private static final Set<String> ALLOWED_RUNTIME_STATES = Set.of(
            "implemented",
            "stored_only"
    );
    private static final Set<String> LIVE_RESOURCE_AFFIX_IDS = Set.of(
            "openworld_rpg:affix/max_hp",
            "openworld_rpg:affix/max_mana",
            "openworld_rpg:affix/max_stamina",
            "openworld_rpg:affix/mana_recovery",
            "openworld_rpg:affix/stamina_recovery",
            "openworld_rpg:affix/skill_mana_cost_reduction",
            "openworld_rpg:affix/dodge_sprint_stamina_cost_reduction"
    );
    private static final Set<String> LIVE_CRITICAL_AFFIX_IDS = Set.of(
            "openworld_rpg:affix/critical_chance",
            "openworld_rpg:affix/critical_damage"
    );
    private static final Set<String> LIVE_ATTACK_SPEED_AFFIX_IDS = Set.of(
            "openworld_rpg:affix/attack_speed"
    );
    private static final Set<String> LIVE_MOVEMENT_AFFIX_IDS = Set.of(
            "openworld_rpg:affix/movement_speed"
    );
    private static final Set<String> LIVE_HEALING_RECEIVED_AFFIX_IDS = Set.of(
            "openworld_rpg:affix/healing_received"
    );

    public OrdinaryEquipmentAffixCatalogData {
        primaryCurve = Objects.requireNonNull(primaryCurve, "primaryCurve");
        affixes = List.copyOf(Objects.requireNonNull(affixes, "affixes"));
    }

    public List<OrdinaryEquipmentAffixRoller.AffixDefinition> definitions() {
        List<OrdinaryEquipmentAffixRoller.AffixDefinition> result =
                new ArrayList<>(affixes.size());
        for (AffixEntry entry : affixes) {
            result.add(entry.definition(primaryCurve.toRuntimeCurve()));
        }
        return List.copyOf(result);
    }

    public List<OrdinaryEquipmentAffixRoller.AffixDefinition>
    implementedDefinitions() {
        List<OrdinaryEquipmentAffixRoller.AffixDefinition> result =
                new ArrayList<>();
        for (AffixEntry entry : affixes) {
            if (entry.runtimeImplemented()) {
                result.add(entry.definition(primaryCurve.toRuntimeCurve()));
            }
        }
        return List.copyOf(result);
    }

    public OrdinaryEquipmentAffixRoller.AffixDefinition definition(
            String affixId
    ) {
        Objects.requireNonNull(affixId, "affixId");
        AffixEntry entry = affixes.stream()
                .filter(value -> value.id().equals(affixId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown ordinary affix id: " + affixId
                ));
        return entry.definition(primaryCurve.toRuntimeCurve());
    }

    public boolean runtimeImplemented(String affixId) {
        Objects.requireNonNull(affixId, "affixId");
        return affixes.stream()
                .filter(value -> value.id().equals(affixId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown ordinary affix id: " + affixId
                ))
                .runtimeImplemented();
    }


    public boolean resourceAuthorityReady() {
        return LIVE_RESOURCE_AFFIX_IDS.stream().allMatch(
                this::runtimeImplemented
        );
    }


    public boolean criticalAuthorityReady() {
        return LIVE_CRITICAL_AFFIX_IDS.stream().allMatch(
                this::runtimeImplemented
        );
    }

    public boolean attackSpeedAuthorityReady() {
        return LIVE_ATTACK_SPEED_AFFIX_IDS.stream().allMatch(
                this::runtimeImplemented
        );
    }

    public boolean movementAuthorityReady() {
        return LIVE_MOVEMENT_AFFIX_IDS.stream().allMatch(
                this::runtimeImplemented
        );
    }

    public boolean healingReceivedAuthorityReady() {
        return LIVE_HEALING_RECEIVED_AFFIX_IDS.stream().allMatch(
                this::runtimeImplemented
        );
    }

    public static void validate(
            OrdinaryEquipmentAffixCatalogData data
    ) {
        Objects.requireNonNull(data, "data");
        if (data.schemaVersion() != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported ordinary-affix catalog schema: "
                            + data.schemaVersion()
            );
        }
        if (!CANONICAL_ID.equals(data.id())) {
            throw new IllegalArgumentException(
                    "Unexpected ordinary-affix catalog id: " + data.id()
            );
        }
        data.primaryCurve().validate();

        Set<String> ids = new HashSet<>();
        for (AffixEntry entry : data.affixes()) {
            entry.validate(data.primaryCurve());
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException(
                        "Duplicate ordinary-affix id: " + entry.id()
                );
            }
        }
        if (data.affixes().isEmpty()) {
            throw new IllegalArgumentException(
                    "Ordinary-affix catalog cannot be empty."
            );
        }
    }

    public record PrimaryCurveData(
            @SerializedName("base_scale") double baseScale,
            @SerializedName("per_level_scale") double perLevelScale,
            @SerializedName("min_multiplier") double minMultiplier,
            @SerializedName("max_multiplier") double maxMultiplier
    ) {
        void validate() {
            toRuntimeCurve();
        }

        OrdinaryEquipmentAffixRoller.PrimaryCurve toRuntimeCurve() {
            return new OrdinaryEquipmentAffixRoller.PrimaryCurve(
                    baseScale,
                    perLevelScale,
                    minMultiplier,
                    maxMultiplier
            );
        }
    }

    public record AffixEntry(
            String id,
            String category,
            @SerializedName("value_rule") String valueRule,
            double min,
            double max,
            @SerializedName("runtime_payload") String runtimePayload,
            @SerializedName("runtime_state") String runtimeState
    ) {
        public AffixEntry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(valueRule, "valueRule");
            Objects.requireNonNull(runtimePayload, "runtimePayload");
            Objects.requireNonNull(runtimeState, "runtimeState");
        }

        void validate(PrimaryCurveData primaryCurve) {
            requireStableId(id);
            requireStableId(runtimePayload);
            if (!ALLOWED_RUNTIME_STATES.contains(runtimeState)) {
                throw new IllegalArgumentException(
                        "Unknown ordinary-affix runtime state: " + runtimeState
                );
            }
            OrdinaryEquipmentAffixRoller.AffixCategory.valueOf(
                    category.trim().toUpperCase(java.util.Locale.ROOT)
            );
            OrdinaryEquipmentAffixRoller.ValueRule rule =
                    OrdinaryEquipmentAffixRoller.ValueRule.valueOf(
                            valueRule.trim().toUpperCase(
                                    java.util.Locale.ROOT
                            )
                    );
            if (rule
                    == OrdinaryEquipmentAffixRoller.ValueRule.PRIMARY_FLAT) {
                primaryCurve.validate();
                if (min != 0.0 || max != 0.0) {
                    throw new IllegalArgumentException(
                            "Primary affix catalog entry must use 0/0 range sentinel: "
                                    + id
                    );
                }
            } else if (!Double.isFinite(min)
                    || !Double.isFinite(max)
                    || min < 0.0
                    || max <= min) {
                throw new IllegalArgumentException(
                        "Invalid percentage affix range: " + id
                );
            }
        }

        public boolean runtimeImplemented() {
            return "implemented".equals(runtimeState);
        }

        OrdinaryEquipmentAffixRoller.AffixDefinition definition(
                OrdinaryEquipmentAffixRoller.PrimaryCurve curve
        ) {
            var categoryValue =
                    OrdinaryEquipmentAffixRoller.AffixCategory.valueOf(
                            category.trim().toUpperCase(
                                    java.util.Locale.ROOT
                            )
                    );
            var rule =
                    OrdinaryEquipmentAffixRoller.ValueRule.valueOf(
                            valueRule.trim().toUpperCase(
                                    java.util.Locale.ROOT
                            )
                    );
            if (rule
                    == OrdinaryEquipmentAffixRoller.ValueRule.PRIMARY_FLAT) {
                return OrdinaryEquipmentAffixRoller.AffixDefinition.primary(
                        id,
                        runtimePayload,
                        curve
                );
            }
            return OrdinaryEquipmentAffixRoller.AffixDefinition.percent(
                    id,
                    categoryValue,
                    min,
                    max,
                    rule == OrdinaryEquipmentAffixRoller.ValueRule.PERCENT_TENTH,
                    runtimePayload
            );
        }
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected stable namespaced ordinary-affix id."
            );
        }
    }
}
