package dev.moonseungjun.openworldrpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public enum ClassDoctrine {
    VANGUARD_RELENTLESS_TEMPO(
            ClassSpecialization.WARRIOR_VANGUARD,
            "vanguard_relentless_tempo"
    ),
    VANGUARD_SIEGEBREAKER(
            ClassSpecialization.WARRIOR_VANGUARD,
            "vanguard_siegebreaker"
    ),
    ARMSMASTER_DUELISTS_MEASURE(
            ClassSpecialization.WARRIOR_ARMSMASTER,
            "armsmaster_duelists_measure"
    ),
    ARMSMASTER_WEAPON_SAVANT(
            ClassSpecialization.WARRIOR_ARMSMASTER,
            "armsmaster_weapon_savant"
    ),
    RANGER_SKIRMISHER(
            ClassSpecialization.HUNTER_RANGER,
            "ranger_skirmisher"
    ),
    RANGER_FIELD_CONTROLLER(
            ClassSpecialization.HUNTER_RANGER,
            "ranger_field_controller"
    ),
    MARKSMAN_PATIENT_AIM(
            ClassSpecialization.HUNTER_MARKSMAN,
            "marksman_patient_aim"
    ),
    MARKSMAN_EXECUTION_WINDOW(
            ClassSpecialization.HUNTER_MARKSMAN,
            "marksman_execution_window"
    ),
    SAINT_MERCY(
            ClassSpecialization.CLERIC_SAINT,
            "saint_mercy"
    ),
    SAINT_AEGIS(
            ClassSpecialization.CLERIC_SAINT,
            "saint_aegis"
    ),
    INQUISITOR_CONDEMNATION(
            ClassSpecialization.CLERIC_INQUISITOR,
            "inquisitor_condemnation"
    ),
    INQUISITOR_PENANCE(
            ClassSpecialization.CLERIC_INQUISITOR,
            "inquisitor_penance"
    ),
    ELEMENTALIST_CONFLUENCE(
            ClassSpecialization.MAGE_ELEMENTALIST,
            "elementalist_confluence"
    ),
    ELEMENTALIST_DOMINION(
            ClassSpecialization.MAGE_ELEMENTALIST,
            "elementalist_dominion"
    ),
    ARCANIST_RIFTWALKER(
            ClassSpecialization.MAGE_ARCANIST,
            "arcanist_riftwalker"
    ),
    ARCANIST_SPELLSHAPER(
            ClassSpecialization.MAGE_ARCANIST,
            "arcanist_spellshaper"
    ),
    BASTION_BODYGUARD(
            ClassSpecialization.GUARDIAN_BASTION,
            "bastion_bodyguard"
    ),
    BASTION_STRONGHOLD(
            ClassSpecialization.GUARDIAN_BASTION,
            "bastion_stronghold"
    ),
    SENTINEL_COUNTERGUARD(
            ClassSpecialization.GUARDIAN_SENTINEL,
            "sentinel_counterguard"
    ),
    SENTINEL_WARDEN(
            ClassSpecialization.GUARDIAN_SENTINEL,
            "sentinel_warden"
    );

    public static final Codec<ClassDoctrine> CODEC =
            Codec.STRING.comapFlatMap(
                    value -> byId(value)
                            .<DataResult<ClassDoctrine>>map(DataResult::success)
                            .orElseGet(() -> DataResult.error(
                                    () -> "Unknown Openworld RPG class doctrine: " + value
                            )),
                    ClassDoctrine::id
            );

    private final ClassSpecialization specialization;
    private final String id;

    ClassDoctrine(
            ClassSpecialization specialization,
            String id
    ) {
        this.specialization = Objects.requireNonNull(
                specialization,
                "specialization"
        );
        this.id = Objects.requireNonNull(id, "id");
    }

    public ClassSpecialization specialization() {
        return specialization;
    }

    public String id() {
        return id;
    }

    public static Optional<ClassDoctrine> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(doctrine -> doctrine.id.equals(normalized))
                .findFirst();
    }

    public static java.util.List<ClassDoctrine> forSpecialization(
            ClassSpecialization specialization
    ) {
        Objects.requireNonNull(specialization, "specialization");
        return Arrays.stream(values())
                .filter(doctrine ->
                        doctrine.specialization == specialization)
                .toList();
    }
}
