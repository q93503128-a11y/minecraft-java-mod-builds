package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongPhysicalEncounterRuntime;
import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;

/**
 * Central resolver for live authored encounter context used by class support mechanics.
 *
 * <p>Only encounter adapters with real server-side engagement truth are admitted here. Formal
 * party membership, proximity, stale reward eligibility and recent-combat timers are not enough.</p>
 */
public final class ProjectActiveEncounterRuntime {
    private ProjectActiveEncounterRuntime() {
    }

    public static OptionalInt supportEncounterLevel(
            ServerPlayer caster,
            ServerPlayer recipient
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(recipient, "recipient");
        if (caster.level() != recipient.level()) {
            return OptionalInt.empty();
        }

        OptionalInt casterR01 =
                R01EarthloongPhysicalEncounterRuntime
                        .activeEncounterLevelFor(caster);
        OptionalInt recipientR01 =
                R01EarthloongPhysicalEncounterRuntime
                        .activeEncounterLevelFor(recipient);

        if (casterR01.isEmpty()) {
            return recipientR01;
        }
        if (recipientR01.isEmpty()) {
            return casterR01;
        }
        return OptionalInt.of(
                Math.max(
                        casterR01.getAsInt(),
                        recipientR01.getAsInt()
                )
        );
    }
}
