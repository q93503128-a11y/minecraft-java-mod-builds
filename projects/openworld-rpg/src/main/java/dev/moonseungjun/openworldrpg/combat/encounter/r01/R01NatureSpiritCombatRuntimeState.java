package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.UUID;

/**
 * Server-owned per-encounter Nature Spirit combat state.
 *
 * <p>This state intentionally owns only canon-closed mechanics that do not depend on presentation:
 * the rolling hostile-damage window plus Living Shell decision/multiplier state. Attack movement,
 * VFX and hit geometry stay in a later presentation binder.</p>
 */
public final class R01NatureSpiritCombatRuntimeState {
    private final R01SecondaryCreatureEncounterData data;
    private final R01NatureSpiritActionController controller;
    private final Deque<DamageSample> recentHostileDamage = new ArrayDeque<>();

    public R01NatureSpiritCombatRuntimeState(
            String encounterInstanceId,
            UUID actorId
    ) {
        this(
                R01SecondaryCreatureEncounterDataLoader.loadNatureSpirit(),
                encounterInstanceId,
                actorId
        );
    }

    R01NatureSpiritCombatRuntimeState(
            R01SecondaryCreatureEncounterData data,
            String encounterInstanceId,
            UUID actorId
    ) {
        this.data = Objects.requireNonNull(data, "data");
        R01SecondaryCreatureEncounterDataLoader.validate(data);
        if (data.kind() != R01SecondaryCreatureEncounterData.CreatureKind.NATURE_SPIRIT) {
            throw new IllegalArgumentException("Expected Nature Spirit encounter data.");
        }
        this.controller = new R01NatureSpiritActionController(
                data,
                encounterInstanceId,
                Objects.requireNonNull(actorId, "actorId")
        );
    }

    public void recordPostMitigationHostileDamage(
            double appliedDamage,
            long gameTick
    ) {
        if (!Double.isFinite(appliedDamage) || appliedDamage < 0.0) {
            throw new IllegalArgumentException(
                    "Nature Spirit hostile damage must be finite and non-negative."
            );
        }
        requireTick(gameTick);
        prune(gameTick);
        if (appliedDamage > 0.0) {
            recentHostileDamage.addLast(
                    new DamageSample(gameTick, appliedDamage)
            );
        }
    }

    public double recentHostileDamageFraction(
            double maxHealth,
            long gameTick
    ) {
        if (!Double.isFinite(maxHealth) || maxHealth <= 0.0) {
            throw new IllegalArgumentException(
                    "Nature Spirit maxHealth must be finite and positive."
            );
        }
        requireTick(gameTick);
        prune(gameTick);
        double total = 0.0;
        for (DamageSample sample : recentHostileDamage) {
            total += sample.damage();
        }
        return total / maxHealth;
    }

    public R01NatureSpiritActionController.Decision selectAtDecision(
            boolean rootedSwipeLegal,
            boolean earthenRamLegal,
            boolean bloomQuakeLegal,
            double targetDistance,
            double currentPoise,
            double maxPoise,
            double maxHealth,
            long gameTick
    ) {
        if (!Double.isFinite(currentPoise)
                || currentPoise < 0.0
                || !Double.isFinite(maxPoise)
                || maxPoise <= 0.0
                || currentPoise > maxPoise) {
            throw new IllegalArgumentException(
                    "Invalid Nature Spirit poise snapshot."
            );
        }
        double poiseFraction = currentPoise / maxPoise;
        return controller.select(
                new R01NatureSpiritActionController.Context(
                        rootedSwipeLegal,
                        earthenRamLegal,
                        bloomQuakeLegal,
                        targetDistance,
                        recentHostileDamageFraction(maxHealth, gameTick),
                        poiseFraction
                ),
                gameTick
        );
    }

    public double directDamageTakenMultiplier(long gameTick) {
        requireTick(gameTick);
        return controller.directDamageTakenMultiplier(gameTick);
    }

    public double poiseDamageTakenMultiplier(long gameTick) {
        requireTick(gameTick);
        return controller.poiseDamageTakenMultiplier(gameTick);
    }

    public boolean onPoiseBroken(long gameTick) {
        requireTick(gameTick);
        return controller.onPoiseBroken(gameTick);
    }

    public long livingShellReuseRemainingTicks(long gameTick) {
        requireTick(gameTick);
        return controller.livingShellReuseRemainingTicks(gameTick);
    }

    private void prune(long gameTick) {
        long windowTicks = data.livingShell().recentDamageWindowTicks();
        while (!recentHostileDamage.isEmpty()) {
            DamageSample first = recentHostileDamage.peekFirst();
            if (gameTick - first.gameTick() < windowTicks) {
                break;
            }
            recentHostileDamage.removeFirst();
        }
    }

    private static void requireTick(long gameTick) {
        if (gameTick < 0L) {
            throw new IllegalArgumentException(
                    "Nature Spirit server time must be non-negative."
            );
        }
    }

    private record DamageSample(long gameTick, double damage) {
        private DamageSample {
            if (gameTick < 0L
                    || !Double.isFinite(damage)
                    || damage <= 0.0) {
                throw new IllegalArgumentException(
                        "Invalid Nature Spirit hostile-damage sample."
                );
            }
        }
    }
}
