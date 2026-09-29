package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import java.util.Optional;

public record ProjectSpellSpec(
        String id,
        double manaCost,
        int cooldownTicks,
        double actionCoefficient,
        double poiseCoefficient,
        int reentryWindowTicks
) {
    public static final String ARC_BOLT_ID = "openworld_rpg:arc_bolt";
    public static final String MEND_ID = "openworld_rpg:mend";
    public static final double MEND_HEAL_COEFFICIENT = 0.30;
    public static final double MEND_EMPOWERED_HEAL_COEFFICIENT = 0.40;

    public ProjectSpellSpec {
        Objects.requireNonNull(id, "id");
        if (!id.startsWith("openworld_rpg:")) {
            throw new IllegalArgumentException("Project spell must use openworld_rpg namespace: " + id);
        }
        if (!Double.isFinite(manaCost) || manaCost < 0) {
            throw new IllegalArgumentException("Mana cost must be finite and non-negative.");
        }
        if (cooldownTicks < 0 || !Double.isFinite(actionCoefficient) || actionCoefficient < 0
                || !Double.isFinite(poiseCoefficient) || poiseCoefficient < 0 || reentryWindowTicks < 0) {
            throw new IllegalArgumentException("Invalid project spell numeric contract: " + id);
        }
    }

    public static ProjectSpellSpec arcBolt() {
        return new ProjectSpellSpec(
                ARC_BOLT_ID,
                12.0,
                60,
                1.20,
                0.50,
                1
        );
    }

    public static ProjectSpellSpec mend() {
        return new ProjectSpellSpec(
                MEND_ID,
                22.0,
                160,
                0.0,
                0.0,
                1
        );
    }

    public static Optional<RootClass> requiredRootClass(String spellId) {
        Objects.requireNonNull(spellId, "spellId");
        return switch (spellId) {
            case ARC_BOLT_ID -> Optional.of(RootClass.MAGE);
            case MEND_ID -> Optional.of(RootClass.CLERIC);
            default -> Optional.empty();
        };
    }
}
