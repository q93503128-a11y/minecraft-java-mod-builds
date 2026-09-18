package dev.moonseungjun.openworldrpg.combat.authority;

import java.util.Objects;

public record ProjectSpellSpec(
        String id,
        double manaCost,
        int cooldownTicks,
        double actionCoefficient,
        double poiseCoefficient,
        int reentryWindowTicks
) {
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
                "openworld_rpg:arc_bolt",
                12.0,
                60,
                1.20,
                0.50,
                1
        );
    }
}
