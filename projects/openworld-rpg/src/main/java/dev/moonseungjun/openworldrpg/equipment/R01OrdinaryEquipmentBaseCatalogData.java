package dev.moonseungjun.openworldrpg.equipment;

import com.google.gson.annotations.SerializedName;
import dev.moonseungjun.openworldrpg.combat.state.ProjectArmorArchetype;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.combat.state.ProjectShieldFamily;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Exact R01 ordinary base identities and their canonical generic-affix eligibility. */
public record R01OrdinaryEquipmentBaseCatalogData(
        @SerializedName("schema_version") int schemaVersion,
        String id,
        List<BaseEntry> bases
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String CANONICAL_ID =
            "openworld_rpg:r01/ordinary_equipment_bases";

    private static final Set<String> REQUIRED_IDS = Set.of(
            "openworld_rpg:heartland_arming_sword",
            "openworld_rpg:wayfarer_daggers",
            "openworld_rpg:quarry_maul",
            "openworld_rpg:river_pike",
            "openworld_rpg:riverwood_bow",
            "openworld_rpg:initiate_staff",
            "openworld_rpg:initiate_wand",
            "openworld_rpg:watch_buckler",
            "openworld_rpg:apprentice_focus",
            "openworld_rpg:river_scholar_garb",
            "openworld_rpg:wayfarer_leathers",
            "openworld_rpg:ironbound_guard",
            "openworld_rpg:greenwater_pendant",
            "openworld_rpg:roadworn_band",
            "openworld_rpg:wayfarers_token",
            "openworld_rpg:quarry_seal"
    );

    public R01OrdinaryEquipmentBaseCatalogData {
        bases = List.copyOf(Objects.requireNonNull(bases, "bases"));
    }

    public BaseEntry base(String baseId) {
        Objects.requireNonNull(baseId, "baseId");
        return bases.stream()
                .filter(value -> value.id().equals(baseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown R01 ordinary equipment base: " + baseId
                ));
    }

    public List<OrdinaryEquipmentAffixRoller.AffixDefinition>
    eligibleAffixes(
            String baseId,
            OrdinaryEquipmentAffixCatalogData staticCatalog,
            OrdinaryEquipmentParameterizedAffixData parameterized
    ) {
        BaseEntry base = base(baseId);
        OrdinaryEquipmentAffixRoller.CategoryWeights weights =
                OrdinaryEquipmentAffixRoller.categoryWeights(
                        base.itemFamily()
                );

        List<OrdinaryEquipmentAffixRoller.AffixDefinition> result =
                new ArrayList<>();
        for (var definition : staticCatalog.definitions()) {
            if (weights.weight(definition.category()) > 0) {
                result.add(definition);
            }
        }
        base.weaponFamily().ifPresent(family ->
                result.add(parameterized.weaponFamilyPower(family))
        );
        return List.copyOf(result);
    }

    public Set<String> runtimeBlockers(
            String baseId,
            OrdinaryEquipmentAffixCatalogData staticCatalog,
            OrdinaryEquipmentParameterizedAffixData parameterized
    ) {
        Set<String> blockers = new HashSet<>();
        for (var definition : eligibleAffixes(
                baseId,
                staticCatalog,
                parameterized
        )) {
            try {
                if (!staticCatalog.runtimeImplemented(definition.id())) {
                    blockers.add(definition.id());
                }
            } catch (IllegalArgumentException ignored) {
                if (!parameterized.matchesCanonical(definition)) {
                    blockers.add(definition.id());
                }
            }
        }
        return Set.copyOf(blockers);
    }

    public OrdinaryEquipmentMaterializer.BaseProfile materializerProfile(
            String baseId,
            ProjectEquipmentSlot resolvedArmorSlot
    ) {
        BaseEntry base = base(baseId);
        return switch (base.kind()) {
            case WEAPON -> OrdinaryEquipmentMaterializer.BaseProfile.weapon(
                    base.id(),
                    base.weaponFamily().orElseThrow()
            );
            case SHIELD -> OrdinaryEquipmentMaterializer.BaseProfile.shield(
                    base.id(),
                    base.shieldFamily().orElseThrow()
            );
            case FOCUS -> OrdinaryEquipmentMaterializer.BaseProfile.focus(
                    base.id()
            );
            case ARMOR -> OrdinaryEquipmentMaterializer.BaseProfile.armor(
                    armorPieceId(base.id(), resolvedArmorSlot),
                    resolvedArmorSlot,
                    base.armorArchetype().orElseThrow()
            );
            case ACCESSORY ->
                    OrdinaryEquipmentMaterializer.BaseProfile.accessory(
                            base.id(),
                            base.fixedSlot().orElseThrow()
                    );
        };
    }

    private static String armorPieceId(
            String familyId,
            ProjectEquipmentSlot slot
    ) {
        if (!ProjectArmorArchetype.isArmorSlot(slot)) {
            throw new IllegalArgumentException(
                    "R01 armor family requires an armor equipment slot."
            );
        }
        return familyId + "/" + slot.name().toLowerCase(Locale.ROOT);
    }

    public static void validate(R01OrdinaryEquipmentBaseCatalogData data) {
        Objects.requireNonNull(data, "data");
        if (data.schemaVersion() != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported R01 equipment-base schema: "
                            + data.schemaVersion()
            );
        }
        if (!CANONICAL_ID.equals(data.id())) {
            throw new IllegalArgumentException(
                    "Unexpected R01 equipment-base catalog id: " + data.id()
            );
        }

        Set<String> seen = new HashSet<>();
        for (BaseEntry base : data.bases()) {
            base.validate();
            if (!seen.add(base.id())) {
                throw new IllegalArgumentException(
                        "Duplicate R01 equipment base: " + base.id()
                );
            }
        }
        if (!seen.equals(REQUIRED_IDS)) {
            throw new IllegalArgumentException(
                    "R01 equipment-base roster does not match closed canon."
            );
        }
    }

    public enum Kind {
        WEAPON,
        SHIELD,
        FOCUS,
        ARMOR,
        ACCESSORY
    }

    public record BaseEntry(
            String id,
            Kind kind,
            @SerializedName("weapon_family") String weaponFamilyName,
            @SerializedName("shield_family") String shieldFamilyName,
            @SerializedName("armor_archetype") String armorArchetypeName,
            @SerializedName("fixed_slot") String fixedSlotName
    ) {
        public BaseEntry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(kind, "kind");
            // Nullable identity fields are intentional JSON unions by equipment kind.
        }

        void validate() {
            requireStableId(id);
            switch (kind) {
                case WEAPON -> {
                    if (weaponFamily().isEmpty()
                            || shieldFamilyName != null
                            || armorArchetypeName != null
                            || fixedSlotName != null) {
                        throw new IllegalArgumentException(
                                "R01 weapon base shape mismatch: " + id
                        );
                    }
                }
                case SHIELD -> {
                    if (shieldFamily().isEmpty()
                            || weaponFamilyName != null
                            || armorArchetypeName != null
                            || fixedSlotName != null) {
                        throw new IllegalArgumentException(
                                "R01 shield base shape mismatch: " + id
                        );
                    }
                }
                case FOCUS -> {
                    if (weaponFamilyName != null
                            || shieldFamilyName != null
                            || armorArchetypeName != null
                            || fixedSlotName != null) {
                        throw new IllegalArgumentException(
                                "R01 focus base shape mismatch: " + id
                        );
                    }
                }
                case ARMOR -> {
                    if (armorArchetype().isEmpty()
                            || weaponFamilyName != null
                            || shieldFamilyName != null
                            || fixedSlotName != null) {
                        throw new IllegalArgumentException(
                                "R01 armor base shape mismatch: " + id
                        );
                    }
                }
                case ACCESSORY -> {
                    if (fixedSlot().isEmpty()
                            || weaponFamilyName != null
                            || shieldFamilyName != null
                            || armorArchetypeName != null) {
                        throw new IllegalArgumentException(
                                "R01 accessory base shape mismatch: " + id
                        );
                    }
                }
            }
        }

        public Optional<ProjectWeaponFamily> weaponFamily() {
            return Optional.ofNullable(weaponFamilyName).map(value ->
                    ProjectWeaponFamily.valueOf(
                            value.toUpperCase(Locale.ROOT)
                    )
            );
        }

        public Optional<ProjectShieldFamily> shieldFamily() {
            return Optional.ofNullable(shieldFamilyName).map(value ->
                    ProjectShieldFamily.valueOf(
                            value.toUpperCase(Locale.ROOT)
                    )
            );
        }

        public Optional<ProjectArmorArchetype> armorArchetype() {
            return Optional.ofNullable(armorArchetypeName).map(value ->
                    ProjectArmorArchetype.valueOf(
                            value.toUpperCase(Locale.ROOT)
                    )
            );
        }

        public Optional<ProjectEquipmentSlot> fixedSlot() {
            return Optional.ofNullable(fixedSlotName).map(value ->
                    ProjectEquipmentSlot.valueOf(
                            value.toUpperCase(Locale.ROOT)
                    )
            );
        }

        public OrdinaryEquipmentAffixRoller.ItemFamily itemFamily() {
            return switch (kind) {
                case WEAPON ->
                        OrdinaryEquipmentAffixRoller.ItemFamily.NORMAL_WEAPON;
                case SHIELD ->
                        OrdinaryEquipmentAffixRoller.ItemFamily
                                .SHIELD_DEFENSIVE_OFFHAND;
                case FOCUS ->
                        OrdinaryEquipmentAffixRoller.ItemFamily.MAGICAL_FOCUS;
                case ARMOR -> switch (armorArchetype().orElseThrow()) {
                    case LIGHT ->
                            OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_LIGHT;
                    case MEDIUM ->
                            OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_MEDIUM;
                    case HEAVY ->
                            OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_HEAVY;
                };
                case ACCESSORY ->
                        OrdinaryEquipmentAffixRoller.ItemFamily.ACCESSORY;
            };
        }
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected stable namespaced R01 equipment-base id."
            );
        }
    }
}
