package dev.moonseungjun.openworldrpg.equipment;

import com.google.gson.annotations.SerializedName;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Data-owned parameterized ordinary-affix templates such as weapon-family power. */
public record OrdinaryEquipmentParameterizedAffixData(
        @SerializedName("schema_version") int schemaVersion,
        String id,
        @SerializedName("weapon_family_power") WeaponFamilyPowerTemplate weaponFamilyPower
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String CANONICAL_ID =
            "openworld_rpg:equipment/parameterized_affix_templates";

    public OrdinaryEquipmentParameterizedAffixData {
        weaponFamilyPower = Objects.requireNonNull(
                weaponFamilyPower,
                "weaponFamilyPower"
        );
    }

    public OrdinaryEquipmentAffixRoller.AffixDefinition weaponFamilyPower(
            ProjectWeaponFamily family
    ) {
        Objects.requireNonNull(family, "family");
        if (!weaponFamilyPower.allowedFamilies().contains(
                family.name().toLowerCase(Locale.ROOT)
        )) {
            throw new IllegalArgumentException(
                    "Weapon family is not admitted by parameterized affix data: "
                            + family
            );
        }
        String slug = family.name().toLowerCase(Locale.ROOT);
        return OrdinaryEquipmentAffixRoller.AffixDefinition.percent(
                "openworld_rpg:affix/weapon_family/" + slug + "_power",
                OrdinaryEquipmentAffixRoller.AffixCategory.OFFENSE,
                weaponFamilyPower.min(),
                weaponFamilyPower.max(),
                false,
                "openworld_rpg:runtime_affix/weapon_family_power/" + slug
        );
    }

    public boolean matchesCanonical(
            OrdinaryEquipmentAffixRoller.AffixDefinition definition
    ) {
        Objects.requireNonNull(definition, "definition");
        String prefix = "openworld_rpg:affix/weapon_family/";
        String suffix = "_power";
        if (!definition.id().startsWith(prefix)
                || !definition.id().endsWith(suffix)) {
            return false;
        }
        String slug = definition.id().substring(
                prefix.length(),
                definition.id().length() - suffix.length()
        );
        ProjectWeaponFamily family;
        try {
            family = ProjectWeaponFamily.valueOf(
                    slug.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return weaponFamilyPower(family).equals(definition);
    }

    public static void validate(OrdinaryEquipmentParameterizedAffixData data) {
        Objects.requireNonNull(data, "data");
        if (data.schemaVersion() != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported parameterized-affix schema: "
                            + data.schemaVersion()
            );
        }
        if (!CANONICAL_ID.equals(data.id())) {
            throw new IllegalArgumentException(
                    "Unexpected parameterized-affix id: " + data.id()
            );
        }
        data.weaponFamilyPower().validate();
    }

    public record WeaponFamilyPowerTemplate(
            double min,
            double max,
            @SerializedName("runtime_state") String runtimeState,
            @SerializedName("allowed_families") List<String> allowedFamilies
    ) {
        public WeaponFamilyPowerTemplate {
            runtimeState = Objects.requireNonNull(runtimeState, "runtimeState");
            allowedFamilies = List.copyOf(
                    Objects.requireNonNull(allowedFamilies, "allowedFamilies")
            );
        }

        void validate() {
            if (!Double.isFinite(min)
                    || !Double.isFinite(max)
                    || min < 0.0
                    || max <= min) {
                throw new IllegalArgumentException(
                        "Invalid weapon-family power range."
                );
            }
            if (!"implemented".equals(runtimeState)) {
                throw new IllegalArgumentException(
                        "Weapon-family power template is not runtime-ready."
                );
            }
            Set<String> unique = Set.copyOf(allowedFamilies);
            if (unique.size() != allowedFamilies.size()
                    || unique.isEmpty()) {
                throw new IllegalArgumentException(
                        "Weapon-family template requires unique allowed families."
                );
            }
            for (String slug : unique) {
                ProjectWeaponFamily.valueOf(
                        slug.toUpperCase(Locale.ROOT)
                );
            }
        }
    }
}
