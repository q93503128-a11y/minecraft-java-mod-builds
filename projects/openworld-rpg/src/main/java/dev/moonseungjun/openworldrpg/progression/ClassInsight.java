package dev.moonseungjun.openworldrpg.progression;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public enum ClassInsight {
    WARRIOR_R01_BREAK_THE_CHARGE(RootClass.WARRIOR, "r01_break_the_charge"),
    WARRIOR_R03_COLLAPSED_MINE_BREAK(RootClass.WARRIOR, "r03_collapsed_mine_break"),
    WARRIOR_R03_GRIFFIN_HIGHLAND_DUEL(RootClass.WARRIOR, "r03_griffin_highland_duel"),
    WARRIOR_R07_ARMOR_OF_DESERT_GUARD_BREAK(RootClass.WARRIOR, "r07_armor_of_desert_guard_break"),
    WARRIOR_R09_FLAMEHORN_IMPACT_HUNT(RootClass.WARRIOR, "r09_flamehorn_impact_hunt"),
    WARRIOR_R09_FORTRESS_EXECUTIONER(RootClass.WARRIOR, "r09_fortress_executioner"),
    WARRIOR_R10_SCORCH_GOLEM_POISE(RootClass.WARRIOR, "r10_scorch_golem_poise"),
    WARRIOR_R12_TERRADRAGON_MASTERY(RootClass.WARRIOR, "r12_terradragon_mastery"),

    HUNTER_R01_CROWNED_MARK(RootClass.HUNTER, "r01_crowned_mark"),
    HUNTER_R02_FOREST_GUARDIAN_TRACKING(RootClass.HUNTER, "r02_forest_guardian_tracking"),
    HUNTER_R04_ICEWORM_HUNT(RootClass.HUNTER, "r04_iceworm_hunt"),
    HUNTER_R05_JUNGLE_PREDATOR_TRACKING(RootClass.HUNTER, "r05_jungle_predator_tracking"),
    HUNTER_R07_DEATHWORM_OBSERVATION(RootClass.HUNTER, "r07_deathworm_observation"),
    HUNTER_R08_TITAN_RABBIT_PRECISION(RootClass.HUNTER, "r08_titan_rabbit_precision"),
    HUNTER_R11_ABYSSAL_HUNT(RootClass.HUNTER, "r11_abyssal_hunt"),
    HUNTER_R12_FARSEER_ANOMALY_TARGET(RootClass.HUNTER, "r12_farseer_anomaly_target"),

    CLERIC_R01_BALANCED_GRACE(RootClass.CLERIC, "r01_balanced_grace"),
    CLERIC_R02_LICH_CLEANSE_SANCTUM(RootClass.CLERIC, "r02_lich_cleanse_sanctum"),
    CLERIC_R04_EXPEDITION_RESCUE(RootClass.CLERIC, "r04_expedition_rescue"),
    CLERIC_R06_HYDRA_FIELD_SUPPORT(RootClass.CLERIC, "r06_hydra_field_support"),
    CLERIC_R08_MOONPRIEST(RootClass.CLERIC, "r08_moonpriest"),
    CLERIC_R10_VOLCANIC_PROTECTION(RootClass.CLERIC, "r10_volcanic_protection"),
    CLERIC_R11_MARITIME_RESCUE(RootClass.CLERIC, "r11_maritime_rescue"),
    CLERIC_R12_ANOMALY_PURIFICATION(RootClass.CLERIC, "r12_anomaly_purification"),

    MAGE_R01_COMPLETE_THE_WEAVE(RootClass.MAGE, "r01_complete_the_weave"),
    MAGE_R02_RUINED_SANCTUM_ARCANE_DISCOVERY(RootClass.MAGE, "r02_ruined_sanctum_arcane_discovery"),
    MAGE_R04_FROST_PHENOMENON_RESEARCH(RootClass.MAGE, "r04_frost_phenomenon_research"),
    MAGE_R05_MATURE_EARTHLOONG_ELEMENTAL_INTERACTION(RootClass.MAGE, "r05_mature_earthloong_elemental_interaction"),
    MAGE_R06_HYDRA_MULTI_ELEMENT(RootClass.MAGE, "r06_hydra_multi_element"),
    MAGE_R08_MOONPRIEST_MAGICAL_ARCHIVE(RootClass.MAGE, "r08_moonpriest_magical_archive"),
    MAGE_R10_INFERNO_SPELL_CHALLENGE(RootClass.MAGE, "r10_inferno_spell_challenge"),
    MAGE_R12_FARSEER_ANOMALY_RESEARCH(RootClass.MAGE, "r12_farseer_anomaly_research"),

    GUARDIAN_R01_HOLD_THE_STORM(RootClass.GUARDIAN, "r01_hold_the_storm"),
    GUARDIAN_R03_HIGHLAND_CONVOY_DEFENSE(RootClass.GUARDIAN, "r03_highland_convoy_defense"),
    GUARDIAN_R04_EXPEDITION_RESCUE_HOLDOUT(RootClass.GUARDIAN, "r04_expedition_rescue_holdout"),
    GUARDIAN_R06_FLOODED_SHRINE_DEFENSE(RootClass.GUARDIAN, "r06_flooded_shrine_defense"),
    GUARDIAN_R07_CARAVAN_DEFENSE(RootClass.GUARDIAN, "r07_caravan_defense"),
    GUARDIAN_R09_FORTRESS_DEFENSE(RootClass.GUARDIAN, "r09_fortress_defense"),
    GUARDIAN_R10_SCORCH_GOLEM_HOLDOUT(RootClass.GUARDIAN, "r10_scorch_golem_holdout"),
    GUARDIAN_R11_HARBOR_SHIP_DEFENSE(RootClass.GUARDIAN, "r11_harbor_ship_defense");

    private final RootClass rootClass;
    private final String path;

    ClassInsight(RootClass rootClass, String path) {
        this.rootClass = Objects.requireNonNull(rootClass, "rootClass");
        this.path = Objects.requireNonNull(path, "path");
    }

    public RootClass rootClass() {
        return rootClass;
    }

    public String id() {
        return "openworld_rpg:insight/"
                + rootClass.name().toLowerCase(Locale.ROOT)
                + "/" + path;
    }

    public String rewardTransactionId() {
        return id() + "/reward";
    }

    public static Optional<ClassInsight> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(insight -> insight.id().equals(id)).findFirst();
    }

    public static java.util.List<ClassInsight> forRoot(RootClass rootClass) {
        Objects.requireNonNull(rootClass, "rootClass");
        return Arrays.stream(values()).filter(insight -> insight.rootClass == rootClass).toList();
    }
}
