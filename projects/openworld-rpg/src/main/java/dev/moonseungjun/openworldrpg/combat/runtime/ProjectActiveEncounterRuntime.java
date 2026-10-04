package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongPhysicalEncounterRuntime;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Central resolver for live authored encounter context used by class support mechanics.
 *
 * <p>Only encounter adapters with real server-side engagement truth are admitted here. Formal
 * party membership, proximity, stale reward eligibility and recent-combat timers are not enough.
 * Encounter identity is preserved so two simultaneous encounters at the same content level cannot
 * accidentally share support authority.</p>
 */
public final class ProjectActiveEncounterRuntime {
    private static final String R01_EARTHLOONG_KIND =
            "openworld_rpg:r01/earthloong";

    private ProjectActiveEncounterRuntime() {
    }

    public static Optional<ActiveEncounterRef> activeEncounterFor(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        return R01EarthloongPhysicalEncounterRuntime
                .activeEncounterFor(player)
                .map(ref -> new ActiveEncounterRef(
                        R01_EARTHLOONG_KIND,
                        ref.actorId(),
                        ref.contentLevel()
                ));
    }

    /**
     * Resolves an encounter level for a support action.
     *
     * <p>A support caster may legitimately join an encounter by helping one already-engaged
     * recipient, so one side may have no current encounter yet. If both sides are already engaged,
     * however, they must refer to the exact same live encounter instance. Same region/level is not
     * enough.</p>
     */
    public static OptionalInt supportEncounterLevel(
            ServerPlayer caster,
            ServerPlayer recipient
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(recipient, "recipient");
        if (caster.level() != recipient.level()) {
            return OptionalInt.empty();
        }

        return resolveSupportEncounterLevel(
                activeEncounterFor(caster),
                activeEncounterFor(recipient)
        );
    }

    public static boolean shareActiveEncounter(
            ServerPlayer first,
            ServerPlayer second
    ) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first.level() != second.level()) {
            return false;
        }
        Optional<ActiveEncounterRef> firstRef =
                activeEncounterFor(first);
        Optional<ActiveEncounterRef> secondRef =
                activeEncounterFor(second);
        return firstRef.isPresent()
                && secondRef.isPresent()
                && sameEncounter(
                        firstRef.orElseThrow(),
                        secondRef.orElseThrow()
                );
    }

    static OptionalInt resolveSupportEncounterLevel(
            Optional<ActiveEncounterRef> caster,
            Optional<ActiveEncounterRef> recipient
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(recipient, "recipient");

        if (caster.isEmpty()) {
            return recipient
                    .map(ref -> OptionalInt.of(ref.contentLevel()))
                    .orElseGet(OptionalInt::empty);
        }
        if (recipient.isEmpty()) {
            return OptionalInt.of(
                    caster.orElseThrow().contentLevel()
            );
        }

        ActiveEncounterRef casterRef = caster.orElseThrow();
        ActiveEncounterRef recipientRef = recipient.orElseThrow();
        if (!sameEncounter(casterRef, recipientRef)
                || casterRef.contentLevel()
                        != recipientRef.contentLevel()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(casterRef.contentLevel());
    }

    static boolean sameEncounter(
            ActiveEncounterRef first,
            ActiveEncounterRef second
    ) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return first.kind().equals(second.kind())
                && first.actorId().equals(second.actorId());
    }

    public record ActiveEncounterRef(
            String kind,
            UUID actorId,
            int contentLevel
    ) {
        public ActiveEncounterRef {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(actorId, "actorId");
            if (kind.isBlank()
                    || kind.indexOf(':') <= 0
                    || contentLevel <= 0) {
                throw new IllegalArgumentException(
                        "Invalid active encounter reference."
                );
            }
        }
    }
}
