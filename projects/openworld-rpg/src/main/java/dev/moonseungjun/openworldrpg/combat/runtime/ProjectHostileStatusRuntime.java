package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
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

    private static final Identifier SNARED_MOVEMENT_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "hostile_snared"
            );

    private static final ConcurrentHashMap<UUID, RebukedRuntimeState>
            REBUKED = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, SnaredEntry>
            SNARED = new ConcurrentHashMap<>();

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
        synchronizeSnaredModifier(
                target,
                applied.movementMultiplier()
        );

        return Optional.of(
                new SnaredApplication(
                        applied.movementMultiplier(),
                        duration,
                        applied.expiresAtTick(),
                        profile.combatRank()
                )
        );
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
            synchronizeSnaredModifier(
                    living,
                    snapshot.movementMultiplier()
            );
            return false;
        });
    }

    public static void clear(UUID entityId) {
        REBUKED.remove(entityId);
        SnaredEntry entry = SNARED.remove(entityId);
        if (entry != null) {
            var entity = entry.level().getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                removeSnaredModifier(living);
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

    private record SnaredEntry(
            ServerLevel level,
            SnaredRuntimeState state
    ) {
    }
}
