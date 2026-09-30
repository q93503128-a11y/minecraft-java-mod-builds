package dev.moonseungjun.openworldrpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public enum ClassSpecialization {
    WARRIOR_VANGUARD(RootClass.WARRIOR, "warrior_vanguard"),
    WARRIOR_ARMSMASTER(RootClass.WARRIOR, "warrior_armsmaster"),
    HUNTER_RANGER(RootClass.HUNTER, "hunter_ranger"),
    HUNTER_MARKSMAN(RootClass.HUNTER, "hunter_marksman"),
    CLERIC_SAINT(RootClass.CLERIC, "cleric_saint"),
    CLERIC_INQUISITOR(RootClass.CLERIC, "cleric_inquisitor"),
    MAGE_ELEMENTALIST(RootClass.MAGE, "mage_elementalist"),
    MAGE_ARCANIST(RootClass.MAGE, "mage_arcanist"),
    GUARDIAN_BASTION(RootClass.GUARDIAN, "guardian_bastion"),
    GUARDIAN_SENTINEL(RootClass.GUARDIAN, "guardian_sentinel");

    public static final Codec<ClassSpecialization> CODEC =
            Codec.STRING.comapFlatMap(
                    value -> {
                        String normalized = value.trim().toLowerCase(Locale.ROOT);
                        return Arrays.stream(values())
                                .filter(specialization ->
                                        specialization.id.equals(normalized))
                                .findFirst()
                                .<DataResult<ClassSpecialization>>map(DataResult::success)
                                .orElseGet(() -> DataResult.error(
                                        () -> "Unknown Openworld RPG class specialization: " + value
                                ));
                    },
                    ClassSpecialization::id
            );

    private final RootClass rootClass;
    private final String id;

    ClassSpecialization(RootClass rootClass, String id) {
        this.rootClass = Objects.requireNonNull(rootClass, "rootClass");
        this.id = Objects.requireNonNull(id, "id");
    }

    public RootClass rootClass() {
        return rootClass;
    }

    public String id() {
        return id;
    }

    public static java.util.Optional<ClassSpecialization> byId(String id) {
        if (id == null) {
            return java.util.Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(specialization -> specialization.id.equals(normalized))
                .findFirst();
    }

    public static List<ClassSpecialization> forRoot(RootClass rootClass) {
        Objects.requireNonNull(rootClass, "rootClass");
        return Arrays.stream(values())
                .filter(specialization -> specialization.rootClass == rootClass)
                .toList();
    }
}
