package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.combat.state.ChilledRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.RebukedRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.SnaredRuntimeState;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Server-owned negative combat states applied to project-owned hostiles. */
public final class ProjectHostileStatusRuntime {
    public static final double REBUKED_STANDARD_MULTIPLIER = 0.85;
    public static final double REBUKED_BOSS_MULTIPLIER = 0.92;
    public static final long REBUKED_STANDARD_TICKS = 80L;
    public static final long REBUKED_BOSS_TICKS = 60L;
    public static final long REBUKED_EMPOWERED_BONUS_TICKS = 20L;

    public static final double SNARED_STANDARD_MOVEMENT_MULTIPLIER = 0.65;
    public static final double SNARED_MINIBOSS_MOVEMENT_MULTIPLIER = 0.80;
    public static final double SNARED_BOSS_MOVEMENT_MULTIPLIER = 0.88;
    public static final long SNARED_STANDARD_TICKS = 60L;
    public static final long SNARED_MINIBOSS_TICKS = 50L;
    public static final long SNARED_BOSS_TICKS = 40L;
    public static final long SNARED_EMPOWERED_NON_BOSS_BONUS_TICKS = 30L;
    public static final long SNARED_EMPOWERED_BOSS_BONUS_TICKS = 10L;

    public static final double SKYFALL_STANDARD_MOVEMENT_MULTIPLIER = 0.75;
    public static final double SKYFALL_BOSS_LIKE_MOVEMENT_MULTIPLIER = 0.90;
    public static final long SKYFALL_REFRESH_TICKS = 2L;

    public static final double PHASE_FIELD_STANDARD_MOVEMENT_MULTIPLIER = 0.75;
    public static final double PHASE_FIELD_BOSS_MOVEMENT_MULTIPLIER = 0.90;
    public static final long PHASE_FIELD_REFRESH_TICKS = 2L;

    public static final double CHILLED_STANDARD_MOVEMENT_MULTIPLIER = 0.65;
    public static final double CHILLED_MINIBOSS_MOVEMENT_MULTIPLIER = 0.80;
    public static final double CHILLED_BOSS_MOVEMENT_MULTIPLIER = 0.85;
    public static final long CHILLED_STANDARD_TICKS = 70L;
    public static final long CHILLED_MINIBOSS_TICKS = 50L;
    public static final long CHILLED_BOSS_TICKS = 40L;

    private static final Identifier SNARED_MOVEMENT_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "hostile_snared"
            );
    private static final Identifier SKYFALL_MOVEMENT_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "hostile_skyfall_slow"
            );
    private static final Identifier CHILLED_MOVEMENT_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "hostile_chilled"
            );
    private static final Identifier PHASE_FIELD_MOVEMENT_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "hostile_phase_field_slow"
            );

    private static final ConcurrentHashMap<UUID, RebukedRuntimeState>
            REBUKED = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, SnaredEntry>
            SNARED = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, SkyfallSlowEntry>
            SKYFALL_SLOW = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PhaseFieldSlowEntry>
            PHASE_FIELD_SLOW = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ChilledEntry>
            CHILLED = new ConcurrentHashMap<>();

    private ProjectHostileStatusRuntime() {
    }

    public static Optional<Application> applyRebuked(
            LivingEntity target,
            boolean empowered,
            long nowTick
    ) {
        var profile = ExternalActorBindingRuntime
                .combatProfile(target)
                .orElse(null);
        if (profile == null) {
            return Optional.empty();
        }

        boolean bossLike = profile.combatRank()
                != ExternalActorCombatProfile.CombatRank.NORMAL_ELITE;
        double multiplier = bossLike
                ? REBUKED_BOSS_MULTIPLIER
                : REBUKED_STANDARD_MULTIPLIER;
        long baseDuration = bossLike
                ? REBUKED_BOSS_TICKS
                : REBUKED_STANDARD_TICKS;
        long duration = baseDuration
                + (empowered
                        ? REBUKED_EMPOWERED_BONUS_TICKS
                        : 0L);

        var state = REBUKED.computeIfAbsent(
                target.getUUID(),
                ignored -> new RebukedRuntimeState()
        );
        var applied = state.apply(
                multiplier,
                duration,
                nowTick
        );
        return Optional.of(
                new Application(
                        multiplier,
                        duration,
                        applied.expiresAtTick(),
                        bossLike
                )
        );
    }

    public static boolean canApplySnared(LivingEntity target) {
        return !target.level().isClientSide()
                && ExternalActorBindingRuntime.combatProfile(target).isPresent()
                && target.getAttribute(Attributes.MOVEMENT_SPEED) != null
                && target.level() instanceof ServerLevel;
    }

    public static Optional<SnaredApplication> applySnared(
            LivingEntity target,
            boolean empowered,
            long nowTick
    ) {
        if (!canApplySnared(target)) {
            return Optional.empty();
        }
        var profile = ExternalActorBindingRuntime
                .combatProfile(target)
                .orElseThrow();

        double multiplier;
        long baseDuration;
        long empoweredBonus;
        switch (profile.combatRank()) {
            case NORMAL_ELITE -> {
                multiplier = SNARED_STANDARD_MOVEMENT_MULTIPLIER;
                baseDuration = SNARED_STANDARD_TICKS;
                empoweredBonus =
                        SNARED_EMPOWERED_NON_BOSS_BONUS_TICKS;
            }
            case MINIBOSS -> {
                multiplier = SNARED_MINIBOSS_MOVEMENT_MULTIPLIER;
                baseDuration = SNARED_MINIBOSS_TICKS;
                empoweredBonus =
                        SNARED_EMPOWERED_NON_BOSS_BONUS_TICKS;
            }
            case BOSS -> {
                multiplier = SNARED_BOSS_MOVEMENT_MULTIPLIER;
                baseDuration = SNARED_BOSS_TICKS;
                empoweredBonus =
                        SNARED_EMPOWERED_BOSS_BONUS_TICKS;
            }
            default -> throw new IllegalStateException(
                    "Unhandled hostile combat rank."
            );
        }

        long duration = baseDuration
                + (empowered ? empoweredBonus : 0L);
        UUID targetId = target.getUUID();
        SnaredEntry entry = SNARED.compute(
                targetId,
                (ignored, current) -> {
                    if (current == null
                            || current.level() != target.level()) {
                        return new SnaredEntry(
                                (ServerLevel) target.level(),
                                new SnaredRuntimeState()
                        );
                    }
                    return current;
                }
        );
        var applied = entry.state().apply(
                multiplier,
                duration,
                nowTick
        );
        synchronizeMovementControl(target, nowTick);

        return Optional.of(
                new SnaredApplication(
                        applied.movementMultiplier(),
                        duration,
                        applied.expiresAtTick(),
                        profile.combatRank()
                )
        );
    }

    public static Optional<SkyfallSlowApplication> applySkyfallSlow(
            LivingEntity target,
            long nowTick
    ) {
        if (target.level().isClientSide()
                || ExternalActorBindingRuntime.combatProfile(target).isEmpty()
                || target.getAttribute(Attributes.MOVEMENT_SPEED) == null
                || !(target.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }

        var profile = ExternalActorBindingRuntime
                .combatProfile(target)
                .orElseThrow();
        double multiplier = profile.combatRank()
                == ExternalActorCombatProfile.CombatRank.NORMAL_ELITE
                ? SKYFALL_STANDARD_MOVEMENT_MULTIPLIER
                : SKYFALL_BOSS_LIKE_MOVEMENT_MULTIPLIER;

        long expiresAtTick = Math.addExact(
                nowTick,
                SKYFALL_REFRESH_TICKS
        );
        SKYFALL_SLOW.put(
                target.getUUID(),
                new SkyfallSlowEntry(
                        level,
                        multiplier,
                        expiresAtTick
                )
        );

        boolean strongerControlActive =
                strongestMovementMultiplier(target, nowTick)
                        < multiplier - 1.0e-9;
        synchronizeMovementControl(target, nowTick);
        return Optional.of(
                new SkyfallSlowApplication(
                        multiplier,
                        expiresAtTick,
                        strongerControlActive,
                        profile.combatRank()
                )
        );
    }

    public static Optional<ChilledApplication> applyChilled(
            LivingEntity target,
            long nowTick
    ) {
        if (target.level().isClientSide()
                || ExternalActorBindingRuntime.combatProfile(target).isEmpty()
                || target.getAttribute(Attributes.MOVEMENT_SPEED) == null
                || !(target.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }

        var profile = ExternalActorBindingRuntime
                .combatProfile(target)
                .orElseThrow();
        double multiplier;
        long duration;
        switch (profile.combatRank()) {
            case NORMAL_ELITE -> {
                multiplier = CHILLED_STANDARD_MOVEMENT_MULTIPLIER;
                duration = CHILLED_STANDARD_TICKS;
            }
            case MINIBOSS -> {
                multiplier = CHILLED_MINIBOSS_MOVEMENT_MULTIPLIER;
                duration = CHILLED_MINIBOSS_TICKS;
            }
            case BOSS -> {
                multiplier = CHILLED_BOSS_MOVEMENT_MULTIPLIER;
                duration = CHILLED_BOSS_TICKS;
            }
            default -> throw new IllegalStateException(
                    "Unhandled hostile combat rank."
            );
        }

        ChilledEntry entry = CHILLED.compute(
                target.getUUID(),
                (ignored, current) -> {
                    if (current == null
                            || current.level() != target.level()) {
                        return new ChilledEntry(
                                level,
                                new ChilledRuntimeState()
                        );
                    }
                    return current;
                }
        );
        var applied = entry.state().apply(
                multiplier,
                duration,
                nowTick
        );
        synchronizeMovementControl(target, nowTick);
        return Optional.of(
                new ChilledApplication(
                        applied.movementMultiplier(),
                        applied.expiresAtTick(),
                        profile.combatRank()
                )
        );
    }

    public static Optional<ChilledRuntimeState.Snapshot> chilledSnapshot(
            LivingEntity target,
            long nowTick
    ) {
        var entry = CHILLED.get(target.getUUID());
        if (entry == null) {
            return Optional.empty();
        }
        var snapshot = entry.state().snapshot(nowTick);
        if (!snapshot.active()) {
            CHILLED.remove(target.getUUID(), entry);
            removeChilledModifier(target);
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    public static Optional<PhaseFieldSlowApplication>
            applyPhaseFieldSlow(
                    LivingEntity target,
                    double utilityMagnitudeMultiplier,
                    long nowTick
            ) {
        if (target.level().isClientSide()
                || !Double.isFinite(utilityMagnitudeMultiplier)
                || utilityMagnitudeMultiplier < 1.0
                || ExternalActorBindingRuntime.combatProfile(target).isEmpty()
                || target.getAttribute(Attributes.MOVEMENT_SPEED) == null
                || !(target.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }

        var profile = ExternalActorBindingRuntime
                .combatProfile(target)
                .orElseThrow();
        double multiplier = phaseFieldMovementMultiplier(
                profile.combatRank(),
                utilityMagnitudeMultiplier
        );
        long expiresAtTick = Math.addExact(
                nowTick,
                PHASE_FIELD_REFRESH_TICKS
        );
        PHASE_FIELD_SLOW.put(
                target.getUUID(),
                new PhaseFieldSlowEntry(
                        level,
                        multiplier,
                        expiresAtTick
                )
        );
        boolean strongerControlActive =
                strongestMovementMultiplier(target, nowTick)
                        < multiplier - 1.0e-9;
        synchronizeMovementControl(target, nowTick);
        return Optional.of(
                new PhaseFieldSlowApplication(
                        multiplier,
                        expiresAtTick,
                        strongerControlActive,
                        profile.combatRank()
                )
        );
    }

    public static double phaseFieldMovementMultiplier(
            ExternalActorCombatProfile.CombatRank rank,
            double utilityMagnitudeMultiplier
    ) {
        if (rank == null
                || !Double.isFinite(utilityMagnitudeMultiplier)
                || utilityMagnitudeMultiplier < 1.0) {
            throw new IllegalArgumentException(
                    "Invalid Phase Step field slow input."
            );
        }
        double baseline =
                rank == ExternalActorCombatProfile.CombatRank.BOSS
                        ? PHASE_FIELD_BOSS_MOVEMENT_MULTIPLIER
                        : PHASE_FIELD_STANDARD_MOVEMENT_MULTIPLIER;
        double slowFraction =
                (1.0 - baseline) * utilityMagnitudeMultiplier;
        return Math.max(0.0, 1.0 - slowFraction);
    }

    public static double outgoingDirectDamageMultiplier(
            LivingEntity attacker,
            long nowTick
    ) {
        var state = REBUKED.get(attacker.getUUID());
        if (state == null) {
            return 1.0;
        }
        double multiplier =
                state.outgoingDirectDamageMultiplier(nowTick);
        if (multiplier == 1.0) {
            REBUKED.remove(attacker.getUUID(), state);
        }
        return multiplier;
    }

    public static Optional<RebukedRuntimeState.Snapshot> rebukedSnapshot(
            LivingEntity target,
            long nowTick
    ) {
        var state = REBUKED.get(target.getUUID());
        if (state == null) {
            return Optional.empty();
        }
        var snapshot = state.snapshot(nowTick);
        if (!snapshot.active()) {
            REBUKED.remove(target.getUUID(), state);
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    public static Optional<SnaredRuntimeState.Snapshot> snaredSnapshot(
            LivingEntity target,
            long nowTick
    ) {
        var entry = SNARED.get(target.getUUID());
        if (entry == null) {
            return Optional.empty();
        }
        var snapshot = entry.state().snapshot(nowTick);
        if (!snapshot.active()) {
            removeSnared(target, entry);
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    public static void tick(MinecraftServer server) {
        SNARED.entrySet().removeIf(entry -> {
            SnaredEntry snared = entry.getValue();
            var entity = snared.level().getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) {
                return true;
            }

            long nowTick = snared.level().getGameTime();
            var snapshot = snared.state().snapshot(nowTick);
            if (!snapshot.active()) {
                removeSnaredModifier(living);
                return true;
            }
            synchronizeMovementControl(living, nowTick);
            return false;
        });

        SKYFALL_SLOW.entrySet().removeIf(entry -> {
            SkyfallSlowEntry slow = entry.getValue();
            var entity = slow.level().getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) {
                return true;
            }
            long nowTick = slow.level().getGameTime();
            if (nowTick >= slow.expiresAtTick()) {
                removeSkyfallModifier(living);
                return true;
            }
            synchronizeMovementControl(living, nowTick);
            return false;
        });

        CHILLED.entrySet().removeIf(entry -> {
            ChilledEntry chilled = entry.getValue();
            var entity = chilled.level().getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) {
                return true;
            }
            long nowTick = chilled.level().getGameTime();
            var snapshot = chilled.state().snapshot(nowTick);
            if (!snapshot.active()) {
                removeChilledModifier(living);
                return true;
            }
            synchronizeMovementControl(living, nowTick);
            return false;
        });

        PHASE_FIELD_SLOW.entrySet().removeIf(entry -> {
            PhaseFieldSlowEntry slow = entry.getValue();
            var entity = slow.level().getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) {
                return true;
            }
            long nowTick = slow.level().getGameTime();
            if (nowTick >= slow.expiresAtTick()) {
                removePhaseFieldModifier(living);
                return true;
            }
            synchronizeMovementControl(living, nowTick);
            return false;
        });
    }

    public static void clear(UUID entityId) {
        REBUKED.remove(entityId);
        SnaredEntry entry = SNARED.remove(entityId);
        SkyfallSlowEntry skyfall = SKYFALL_SLOW.remove(entityId);
        PhaseFieldSlowEntry phase = PHASE_FIELD_SLOW.remove(entityId);
        ChilledEntry chilled = CHILLED.remove(entityId);
        ServerLevel level = entry != null
                ? entry.level()
                : skyfall != null
                        ? skyfall.level()
                        : phase != null
                                ? phase.level()
                                : chilled != null
                                        ? chilled.level()
                                        : null;
        if (level != null) {
            var entity = level.getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                removeSnaredModifier(living);
                removeSkyfallModifier(living);
                removePhaseFieldModifier(living);
                removeChilledModifier(living);
            }
        }
    }

    private static void removeSnared(
            LivingEntity target,
            SnaredEntry expected
    ) {
        SNARED.remove(target.getUUID(), expected);
        removeSnaredModifier(target);
    }

    private static void synchronizeMovementControl(
            LivingEntity target,
            long nowTick
    ) {
        double strongest = 1.0;
        MovementControlSource source = MovementControlSource.NONE;

        var snared = snaredSnapshot(target, nowTick).orElse(null);
        if (snared != null
                && snared.movementMultiplier() < strongest) {
            strongest = snared.movementMultiplier();
            source = MovementControlSource.SNARED;
        }

        SkyfallSlowEntry skyfall = SKYFALL_SLOW.get(
                target.getUUID()
        );
        if (skyfall != null
                && skyfall.level() == target.level()
                && nowTick < skyfall.expiresAtTick()
                && skyfall.movementMultiplier() < strongest) {
            strongest = skyfall.movementMultiplier();
            source = MovementControlSource.SKYFALL;
        }

        var chilled = chilledSnapshot(target, nowTick).orElse(null);
        if (chilled != null
                && chilled.movementMultiplier() < strongest) {
            strongest = chilled.movementMultiplier();
            source = MovementControlSource.CHILLED;
        }

        PhaseFieldSlowEntry phase = PHASE_FIELD_SLOW.get(
                target.getUUID()
        );
        if (phase != null
                && phase.level() == target.level()
                && nowTick < phase.expiresAtTick()
                && phase.movementMultiplier() < strongest) {
            strongest = phase.movementMultiplier();
            source = MovementControlSource.PHASE_FIELD;
        }

        removeSnaredModifier(target);
        removeSkyfallModifier(target);
        removePhaseFieldModifier(target);
        removeChilledModifier(target);
        switch (source) {
            case SNARED -> synchronizeSnaredModifier(
                    target,
                    strongest
            );
            case SKYFALL -> synchronizeSkyfallModifier(
                    target,
                    strongest
            );
            case PHASE_FIELD -> synchronizePhaseFieldModifier(
                    target,
                    strongest
            );
            case CHILLED -> synchronizeChilledModifier(
                    target,
                    strongest
            );
            case NONE -> {
            }
        }
    }

    private static double strongestMovementMultiplier(
            LivingEntity target,
            long nowTick
    ) {
        double strongest = 1.0;
        var snared = snaredSnapshot(target, nowTick).orElse(null);
        if (snared != null) {
            strongest = Math.min(
                    strongest,
                    snared.movementMultiplier()
            );
        }
        SkyfallSlowEntry skyfall = SKYFALL_SLOW.get(
                target.getUUID()
        );
        if (skyfall != null
                && skyfall.level() == target.level()
                && nowTick < skyfall.expiresAtTick()) {
            strongest = Math.min(
                    strongest,
                    skyfall.movementMultiplier()
            );
        }
        var chilled = chilledSnapshot(target, nowTick).orElse(null);
        if (chilled != null) {
            strongest = Math.min(
                    strongest,
                    chilled.movementMultiplier()
            );
        }
        PhaseFieldSlowEntry phase = PHASE_FIELD_SLOW.get(
                target.getUUID()
        );
        if (phase != null
                && phase.level() == target.level()
                && nowTick < phase.expiresAtTick()) {
            strongest = Math.min(
                    strongest,
                    phase.movementMultiplier()
            );
        }
        return strongest;
    }

    private static void synchronizeSnaredModifier(
            LivingEntity target,
            double multiplier
    ) {
        var movement = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            throw new IllegalStateException(
                    "Snared target lost MOVEMENT_SPEED attribute."
            );
        }
        movement.removeModifier(SNARED_MOVEMENT_MODIFIER_ID);
        movement.addOrUpdateTransientModifier(
                new AttributeModifier(
                        SNARED_MOVEMENT_MODIFIER_ID,
                        multiplier - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );
    }

    private static void removeSnaredModifier(
            LivingEntity target
    ) {
        var movement = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(SNARED_MOVEMENT_MODIFIER_ID);
        }
    }

    private static void synchronizeSkyfallModifier(
            LivingEntity target,
            double multiplier
    ) {
        var movement = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            throw new IllegalStateException(
                    "Skyfall target lost MOVEMENT_SPEED attribute."
            );
        }
        movement.removeModifier(SKYFALL_MOVEMENT_MODIFIER_ID);
        movement.addOrUpdateTransientModifier(
                new AttributeModifier(
                        SKYFALL_MOVEMENT_MODIFIER_ID,
                        multiplier - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );
    }

    private static void removeSkyfallModifier(
            LivingEntity target
    ) {
        var movement = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(SKYFALL_MOVEMENT_MODIFIER_ID);
        }
    }

    private static void synchronizeChilledModifier(
            LivingEntity target,
            double multiplier
    ) {
        var movement = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            throw new IllegalStateException(
                    "Chilled target lost MOVEMENT_SPEED attribute."
            );
        }
        movement.removeModifier(CHILLED_MOVEMENT_MODIFIER_ID);
        movement.addOrUpdateTransientModifier(
                new AttributeModifier(
                        CHILLED_MOVEMENT_MODIFIER_ID,
                        multiplier - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );
    }

    private static void removeChilledModifier(
            LivingEntity target
    ) {
        var movement = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(CHILLED_MOVEMENT_MODIFIER_ID);
        }
    }

    private static void synchronizePhaseFieldModifier(
            LivingEntity target,
            double multiplier
    ) {
        var movement = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            throw new IllegalStateException(
                    "Phase-field target lost MOVEMENT_SPEED attribute."
            );
        }
        movement.removeModifier(PHASE_FIELD_MOVEMENT_MODIFIER_ID);
        movement.addOrUpdateTransientModifier(
                new AttributeModifier(
                        PHASE_FIELD_MOVEMENT_MODIFIER_ID,
                        multiplier - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );
    }

    private static void removePhaseFieldModifier(
            LivingEntity target
    ) {
        var movement = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(PHASE_FIELD_MOVEMENT_MODIFIER_ID);
        }
    }

    private enum MovementControlSource {
        NONE,
        SNARED,
        SKYFALL,
        PHASE_FIELD,
        CHILLED
    }

    public record Application(
            double multiplier,
            long durationTicks,
            long expiresAtTick,
            boolean bossRank
    ) {
    }

    public record SnaredApplication(
            double movementMultiplier,
            long durationTicks,
            long expiresAtTick,
            ExternalActorCombatProfile.CombatRank combatRank
    ) {
    }

    public record SkyfallSlowApplication(
            double movementMultiplier,
            long expiresAtTick,
            boolean suppressedByStrongerControl,
            ExternalActorCombatProfile.CombatRank combatRank
    ) {
    }

    public record ChilledApplication(
            double movementMultiplier,
            long expiresAtTick,
            ExternalActorCombatProfile.CombatRank combatRank
    ) {
    }

    public record PhaseFieldSlowApplication(
            double movementMultiplier,
            long expiresAtTick,
            boolean suppressedByStrongerControl,
            ExternalActorCombatProfile.CombatRank combatRank
    ) {
    }

    private record SnaredEntry(
            ServerLevel level,
            SnaredRuntimeState state
    ) {
    }

    private record SkyfallSlowEntry(
            ServerLevel level,
            double movementMultiplier,
            long expiresAtTick
    ) {
    }

    private record ChilledEntry(
            ServerLevel level,
            ChilledRuntimeState state
    ) {
    }

    private record PhaseFieldSlowEntry(
            ServerLevel level,
            double movementMultiplier,
            long expiresAtTick
    ) {
    }
}
