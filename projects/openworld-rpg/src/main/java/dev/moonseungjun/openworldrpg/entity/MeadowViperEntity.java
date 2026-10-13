package dev.moonseungjun.openworldrpg.entity;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01MeadowViperPoisonRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerIncomingDamageRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerPoisePressureRuntime;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Project-owned Meadow Viper runtime.
 *
 * <p>The server owns warning/engage/commit/contact/recovery/spacing and only publishes compact
 * presentation state to clients. Natural R01 spawn admission remains separately gated.</p>
 */
public final class MeadowViperEntity extends PathfinderMob {
    public static final double WARNING_RADIUS = 3.5;
    public static final double BITE_RANGE = 2.2;
    public static final int AMBIENT_WARNING_TICKS = 9;
    public static final int BITE_WINDUP_TICKS = 9;
    public static final int BITE_ACTIVE_TICKS = 3;
    public static final int BITE_RECOVERY_TICKS = 11;
    public static final double SPACING_DISTANCE = 2.0;
    public static final double PLAYER_POISE_PRESSURE = 16.0;

    /** Source-derived review scale: 0.35 * ~2.265 source-unit peak mouth reach. */
    public static final double REVIEW_ATTACK_PEAK_FORWARD_REACH = 0.793;

    /**
     * Runtime navigation seed only. Exact world movement speed is deliberately not accepted as
     * production tuning until the pinned model is reviewed in-client, per R01 canon.
     */
    private static final double REVIEW_MOVEMENT_SPEED = 0.18;

    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(MeadowViperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> PHASE_START_TICK =
            SynchedEntityData.defineId(MeadowViperEntity.class, EntityDataSerializers.LONG);

    private UUID targetId;
    private boolean provoked;
    private boolean activeImpactResolved;

    public MeadowViperEntity(EntityType<? extends MeadowViperEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 75.0)
                .add(Attributes.MOVEMENT_SPEED, REVIEW_MOVEMENT_SPEED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, Phase.NEUTRAL.id());
        builder.define(PHASE_START_TICK, 0L);
    }

    public Phase phase() {
        return Phase.byId(entityData.get(PHASE));
    }

    public long phaseStartTick() {
        return entityData.get(PHASE_START_TICK);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean applied = super.hurtServer(level, source, damage);
        if (applied && source.getEntity() instanceof ServerPlayer player && player.isAlive()) {
            targetId = player.getUUID();
            provoked = true;
            beginImmediateEngage(level, player);
        }
        return applied;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel) || !isAlive()) {
            return;
        }
        tickServer(serverLevel);
    }

    private void tickServer(ServerLevel level) {
        long nowTick = level.getGameTime();
        ServerPlayer target = resolveTarget(level).orElse(null);

        if (target == null) {
            ServerPlayer ambient = nearestAmbientPlayer(level).orElse(null);
            if (ambient == null) {
                resetNeutral(nowTick);
                return;
            }
            target = ambient;
            targetId = target.getUUID();
            provoked = false;
            transition(Phase.WARNING, nowTick);
        }

        if (!validTarget(target)) {
            resetNeutral(nowTick);
            return;
        }

        face(target);
        double distance = horizontalDistanceTo(target);
        long phaseAge = Math.max(0L, nowTick - phaseStartTick());

        switch (phase()) {
            case NEUTRAL -> {
                if (provoked) {
                    beginImmediateEngage(level, target);
                } else if (distance <= WARNING_RADIUS) {
                    transition(Phase.WARNING, nowTick);
                }
            }
            case WARNING -> tickWarning(level, target, distance, phaseAge, nowTick);
            case BITE_WINDUP -> tickBiteWindup(target, distance, phaseAge, nowTick);
            case BITE_ACTIVE -> tickBiteActive(target, phaseAge, nowTick);
            case RECOVERY -> {
                getNavigation().stop();
                if (phaseAge >= BITE_RECOVERY_TICKS) {
                    transition(Phase.SPACING, nowTick);
                }
            }
            case SPACING -> tickSpacing(target, distance, nowTick);
        }
    }

    private void tickWarning(
            ServerLevel level,
            ServerPlayer target,
            double distance,
            long phaseAge,
            long nowTick
    ) {
        if (!provoked && distance > WARNING_RADIUS) {
            resetNeutral(nowTick);
            return;
        }
        if (distance > BITE_RANGE) {
            moveOnlyEnoughToCreateBiteReach(target);
            return;
        }
        getNavigation().stop();
        if (phaseAge >= AMBIENT_WARNING_TICKS) {
            activeImpactResolved = false;
            transition(Phase.BITE_ACTIVE, nowTick);
        }
    }

    private void tickBiteWindup(
            ServerPlayer target,
            double distance,
            long phaseAge,
            long nowTick
    ) {
        getNavigation().stop();
        if (!provoked && distance > WARNING_RADIUS) {
            resetNeutral(nowTick);
            return;
        }
        if (phaseAge >= BITE_WINDUP_TICKS) {
            activeImpactResolved = false;
            transition(Phase.BITE_ACTIVE, nowTick);
        }
    }

    private void tickBiteActive(
            ServerPlayer target,
            long phaseAge,
            long nowTick
    ) {
        getNavigation().stop();
        applyCollisionAwareLunge(target, phaseAge);

        // Resolve around the source clip's lunge/contact peak, not at active-frame start.
        if (!activeImpactResolved && phaseAge >= 2L) {
            activeImpactResolved = true;
            double contactDistance = horizontalDistanceTo(target);
            double visibleContactDistance = REVIEW_ATTACK_PEAK_FORWARD_REACH
                    + target.getBbWidth() * 0.5;
            if (contactDistance <= BITE_RANGE
                    && contactDistance <= visibleContactDistance + 1.0e-3
                    && getSensing().hasLineOfSight(target)) {
                applyCoilBite(target);
            }
        }
        if (phaseAge >= BITE_ACTIVE_TICKS) {
            transition(Phase.RECOVERY, nowTick);
        }
    }

    private void tickSpacing(ServerPlayer target, double distance, long nowTick) {
        if (distance >= SPACING_DISTANCE) {
            provoked = false;
            if (distance <= WARNING_RADIUS) {
                transition(Phase.WARNING, nowTick);
            } else {
                resetNeutral(nowTick);
            }
            return;
        }

        Vec3 away = position().subtract(target.position());
        Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
        if (horizontal.lengthSqr() < 1.0e-8) {
            horizontal = new Vec3(0.0, 0.0, 1.0);
        }
        horizontal = horizontal.normalize();
        Vec3 destination = position().add(horizontal.scale(SPACING_DISTANCE));
        getNavigation().moveTo(destination.x, getY(), destination.z, 1.0);
    }

    private void beginImmediateEngage(ServerLevel level, ServerPlayer target) {
        long nowTick = level.getGameTime();
        face(target);
        if (horizontalDistanceTo(target) <= BITE_RANGE) {
            transition(Phase.BITE_WINDUP, nowTick);
        } else {
            transition(Phase.WARNING, nowTick);
        }
    }

    private void moveOnlyEnoughToCreateBiteReach(ServerPlayer target) {
        double distance = horizontalDistanceTo(target);
        if (distance <= BITE_RANGE) {
            getNavigation().stop();
            return;
        }
        Vec3 toward = target.position().subtract(position());
        Vec3 horizontal = new Vec3(toward.x, 0.0, toward.z);
        if (horizontal.lengthSqr() < 1.0e-8) {
            getNavigation().stop();
            return;
        }
        Vec3 desired = target.position().subtract(horizontal.normalize().scale(BITE_RANGE));
        getNavigation().moveTo(desired.x, target.getY(), desired.z, 1.0);
    }

    private void applyCollisionAwareLunge(ServerPlayer target, long activeAge) {
        if (activeAge >= BITE_ACTIVE_TICKS) {
            return;
        }
        Vec3 toward = target.position().subtract(position());
        Vec3 horizontal = new Vec3(toward.x, 0.0, toward.z);
        double distance = horizontal.length();
        if (distance < 1.0e-6) {
            return;
        }
        double visibleContactDistance = REVIEW_ATTACK_PEAK_FORWARD_REACH
                + target.getBbWidth() * 0.5;
        double needed = Math.max(0.0, distance - visibleContactDistance);
        // Reach visible contact by active tick 2, where the source attack clip is near peak extension.
        int remainingContactSteps = Math.max(1, 3 - (int) activeAge);
        double thisTick = needed / remainingContactSteps;
        if (thisTick > 1.0e-4) {
            move(MoverType.SELF, horizontal.normalize().scale(thisTick));
        }
    }

    private void applyCoilBite(ServerPlayer target) {
        double rawDamage = ProjectCombatRules.rawEnemyPhysicalDamageFromBenchmarkShare(2, 0.09);
        PlayerDefenseAuthority.IncomingHit hit = PlayerDefenseAuthority.IncomingHit.baseline(
                rawDamage,
                ProjectImpactTransaction.DamageSchool.PHYSICAL,
                2,
                PlayerDefenseAuthority.GuardPressureBand.LIGHT,
                true,
                true,
                true
        );
        ProjectPlayerIncomingDamageRuntime.IncomingApplication incoming =
                ProjectPlayerIncomingDamageRuntime.applyProjectOwnedActorHit(this, target, hit);
        if (!incoming.accepted()) {
            return;
        }

        ProjectPlayerPoisePressureRuntime.applyAuthoredPressure(target, PLAYER_POISE_PRESSURE);
        double statusMultiplier = incoming.resolution()
                .map(result -> poisonBuildupMultiplier(target, result))
                .orElse(0.0);
        R01MeadowViperPoisonRuntime.applyBuildup(
                target,
                R01MeadowViperPoisonRuntime.BITE_BUILDUP,
                statusMultiplier
        );
    }

    private static double poisonBuildupMultiplier(
            ServerPlayer target,
            dev.moonseungjun.openworldrpg.combat.state.PlayerDefenseRuntimeState.IncomingDefenseResult result
    ) {
        if (result.dodged() || result.perfectGuarded()) {
            return 0.0;
        }
        if (!result.guarded()) {
            return 1.0;
        }
        var defense = CombatStateServices.defenseSnapshots()
                .snapshot(target.getUUID())
                .orElse(null);
        if (defense == null || defense.guardType().isEmpty()) {
            return 1.0;
        }
        double absorption = PlayerDefenseAuthority.guardAbsorption(
                defense.guardType().orElseThrow(),
                ProjectImpactTransaction.DamageSchool.PHYSICAL
        );
        if (result.guardBroken()) {
            absorption *= 0.5;
        }
        return Math.max(0.0, 1.0 - absorption);
    }

    private Optional<ServerPlayer> resolveTarget(ServerLevel level) {
        if (targetId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(level.getPlayerByUUID(targetId))
                .filter(this::validTarget);
    }

    private Optional<ServerPlayer> nearestAmbientPlayer(ServerLevel level) {
        return level.players().stream()
                .filter(this::validTarget)
                .filter(player -> horizontalDistanceTo(player) <= WARNING_RADIUS)
                .min(Comparator.comparingDouble(player -> distanceToSqr(player)));
    }

    private boolean validTarget(ServerPlayer player) {
        return player != null
                && player.isAlive()
                && !player.isSpectator()
                && !player.isCreative()
                && player.level() == level();
    }

    private double horizontalDistanceTo(ServerPlayer player) {
        double dx = player.getX() - getX();
        double dz = player.getZ() - getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void face(ServerPlayer target) {
        getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void transition(Phase next, long nowTick) {
        if (phase() == next) {
            return;
        }
        entityData.set(PHASE, next.id());
        entityData.set(PHASE_START_TICK, nowTick);
    }

    private void resetNeutral(long nowTick) {
        targetId = null;
        provoked = false;
        activeImpactResolved = false;
        getNavigation().stop();
        transition(Phase.NEUTRAL, nowTick);
    }

    public enum Phase {
        NEUTRAL(0),
        WARNING(1),
        BITE_WINDUP(2),
        BITE_ACTIVE(3),
        RECOVERY(4),
        SPACING(5);

        private final int id;

        Phase(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static Phase byId(int id) {
            for (Phase phase : values()) {
                if (phase.id == id) {
                    return phase;
                }
            }
            return NEUTRAL;
        }
    }
}
