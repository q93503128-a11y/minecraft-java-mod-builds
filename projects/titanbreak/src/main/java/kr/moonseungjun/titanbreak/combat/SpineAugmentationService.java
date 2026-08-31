package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spine augmentation mechanics that depend on transient combat/movement state.
 */
public final class SpineAugmentationService {
    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();

    private static final class RuntimeState {
        Vec3 lastPosition;
        double lastFallDistance;
        double kineticEnergy;
        double lastSpentEnergy;
        long lastSpentTick = Long.MIN_VALUE / 4L;
        long phaseIntentTick = Long.MIN_VALUE / 4L;
        long projectilePhaseUntil = Long.MIN_VALUE / 4L;
        long stabilizeUntil = Long.MIN_VALUE / 4L;
        long lastAttackTick = Long.MIN_VALUE / 4L;

        RuntimeState(ServerPlayer player) {
            lastPosition = player.position();
            lastFallDistance = player.fallDistance;
        }
    }

    private SpineAugmentationService() {}

    /** Marks a genuine Phase-Step request before the ability attempts to move the player. */
    public static void notePhaseIntent(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState(player));
        runtime.phaseIntentTick = player.level().getGameTime();
    }

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState(player));
        long now = player.level().getGameTime();
        Vec3 position = player.position();
        Vec3 displacement = position.subtract(runtime.lastPosition);
        double horizontalTravel = Math.sqrt(displacement.x * displacement.x + displacement.z * displacement.z);

        tickPhaseWindow(player, state, runtime, now, horizontalTravel);
        tickKineticRelay(player, state, runtime, horizontalTravel);
        tickGyro(player, state, runtime, now);

        runtime.lastPosition = position;
        runtime.lastFallDistance = player.fallDistance;
    }

    private static void tickPhaseWindow(ServerPlayer player, TitanPlayerData.State state, RuntimeState runtime,
                                        long now, double horizontalTravel) {
        TitanPlayerData.AugmentInstance phase = state.firstInstalledInstance("phase_step_spine");
        if (phase == null || phase.enhancement() < 10) return;
        if (now - runtime.phaseIntentTick > 3L) return;
        if (horizontalTravel < 2.5D || horizontalTravel > 12.0D) return;

        runtime.projectilePhaseUntil = now + 8L;
        runtime.phaseIntentTick = Long.MIN_VALUE / 4L;
    }

    private static void tickKineticRelay(ServerPlayer player, TitanPlayerData.State state, RuntimeState runtime,
                                         double horizontalTravel) {
        TitanPlayerData.AugmentInstance relay = state.firstInstalledInstance("kinetic_relay_spine");
        if (relay == null) {
            runtime.kineticEnergy = 0.0D;
            runtime.lastSpentEnergy = 0.0D;
            return;
        }

        double gain = 0.0D;
        if (horizontalTravel > 0.48D) {
            gain += (horizontalTravel - 0.48D) * 5.0D;
        }
        double fallDelta = player.fallDistance - runtime.lastFallDistance;
        if (fallDelta > 0.15D) gain += fallDelta * 1.75D;

        if (gain > 0.0D) {
            runtime.kineticEnergy = Math.min(kineticCapacity(relay), runtime.kineticEnergy + gain);
        }
    }

    private static double kineticCapacity(TitanPlayerData.AugmentInstance relay) {
        double base = relay.enhancement() >= 5 ? 55.0D : 35.0D;
        return base + Math.max(0, relay.mk() - 1) * 4.0D;
    }

    private static void tickGyro(ServerPlayer player, TitanPlayerData.State state, RuntimeState runtime, long now) {
        TitanPlayerData.AugmentInstance gyro = state.firstInstalledInstance("gyro_stabilized_spine");
        if (gyro == null) return;

        Vec3 motion = player.getDeltaMovement();
        if (gyro.enhancement() >= 5 && now - runtime.lastAttackTick <= 2L) {
            Vec3 facing = horizontal(player.getLookAngle());
            if (facing.lengthSqr() > 1.0E-6D) {
                facing = facing.normalize();
                Vec3 flatMotion = new Vec3(motion.x, 0.0D, motion.z);
                double backward = flatMotion.dot(facing);
                if (backward < 0.0D) {
                    Vec3 corrected = flatMotion.subtract(facing.scale(backward * 0.65D));
                    motion = new Vec3(corrected.x, motion.y, corrected.z);
                }
            }
        }

        if (gyro.enhancement() >= 7 && !player.onGround() && player.isSprinting()) {
            Vec3 flatMotion = new Vec3(motion.x, 0.0D, motion.z);
            double speed = flatMotion.length();
            Vec3 facing = horizontal(player.getLookAngle());
            if (speed > 0.10D && facing.lengthSqr() > 1.0E-6D) {
                Vec3 desired = facing.normalize().scale(speed);
                double steer = Math.min(0.32D, 0.18D + Math.max(0, gyro.mk() - 1) * 0.025D);
                Vec3 steered = flatMotion.scale(1.0D - steer).add(desired.scale(steer));
                if (steered.lengthSqr() > 1.0E-6D) steered = steered.normalize().scale(speed);
                motion = new Vec3(steered.x, motion.y, steered.z);
            }
        }

        if (gyro.enhancement() >= 10 && now <= runtime.stabilizeUntil) {
            motion = new Vec3(motion.x * 0.78D, Math.max(-0.42D, Math.min(0.42D, motion.y)), motion.z * 0.78D);
        }

        if (!motion.equals(player.getDeltaMovement())) {
            player.setDeltaMovement(motion);
            player.hurtMarked = true;
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        if (event.getEntity() instanceof ServerPlayer defender) {
            TitanPlayerData.State defenderState = TitanPlayerData.get(level.getServer()).state(defender);
            RuntimeState runtime = RUNTIME.computeIfAbsent(defender.getUUID(), ignored -> new RuntimeState(defender));
            long now = level.getGameTime();

            TitanPlayerData.AugmentInstance phase = defenderState.firstInstalledInstance("phase_step_spine");
            Entity direct = event.getSource().getDirectEntity();
            if (phase != null && phase.enhancement() >= 10 && now <= runtime.projectilePhaseUntil
                    && direct instanceof Projectile) {
                event.getContainer().setNewDamage(0.0F);
                event.getContainer().setShouldCauseSideEffects(false);
                return;
            }

            TitanPlayerData.AugmentInstance gyro = defenderState.firstInstalledInstance("gyro_stabilized_spine");
            if (gyro != null && gyro.enhancement() >= 10 && event.getAmount() >= CombatScale.toInternal(30.0D)) {
                event.getContainer().setNewDamage(event.getAmount() * 0.82F);
                runtime.stabilizeUntil = now + 4L;
            }
        }

        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof ServerPlayer attacker) || attacker == event.getEntity()) return;
        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(attacker);
        RuntimeState runtime = RUNTIME.computeIfAbsent(attacker.getUUID(), ignored -> new RuntimeState(attacker));
        runtime.lastAttackTick = level.getGameTime();

        TitanPlayerData.AugmentInstance relay = state.firstInstalledInstance("kinetic_relay_spine");
        if (relay == null || runtime.kineticEnergy < 2.0D) return;

        double spent = runtime.kineticEnergy;
        runtime.kineticEnergy = 0.0D;
        runtime.lastSpentEnergy = spent;
        runtime.lastSpentTick = level.getGameTime();

        double visibleBonus = Math.min(70.0D, spent * 0.72D);
        event.getContainer().setNewDamage(event.getAmount() + (float) CombatScale.toInternal(visibleBonus));
        data.addMasteryXp(attacker, "kinetic_relay_spine", spent >= 18.0D ? 3 : 2);

        if (relay.enhancement() >= 7 && spent >= 18.0D) {
            releaseShockwave(attacker, event.getEntity(), spent);
        }
    }

    private static void releaseShockwave(ServerPlayer attacker, LivingEntity primary, double spent) {
        if (!(attacker.level() instanceof ServerLevel level)) return;
        double radius = Math.min(5.0D, 2.6D + spent * 0.035D);
        double visibleDamage = Math.min(42.0D, spent * 0.42D);
        for (Entity entity : level.getEntities(attacker, primary.getBoundingBox().inflate(radius),
                candidate -> candidate instanceof LivingEntity && candidate.isAlive() && candidate != primary
                        && candidate != attacker && candidate.distanceToSqr(primary) <= radius * radius)) {
            entity.hurtServer(level, attacker.damageSources().playerAttack(attacker),
                    (float) CombatScale.toInternal(visibleDamage));
            Vec3 away = entity.position().subtract(primary.position());
            if (away.lengthSqr() > 1.0E-6D) {
                away = away.normalize();
                entity.push(away.x * 0.55D, 0.16D, away.z * 0.55D);
                entity.hurtMarked = true;
            }
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        TitanPlayerData.AugmentInstance relay = state.firstInstalledInstance("kinetic_relay_spine");
        if (relay == null || relay.enhancement() < 10) return;

        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null || runtime.lastSpentEnergy <= 0.0D) return;
        long now = level.getGameTime();
        if (now - runtime.lastSpentTick > 12L) return;

        double retained = runtime.lastSpentEnergy * 0.60D;
        runtime.kineticEnergy = Math.min(kineticCapacity(relay), runtime.kineticEnergy + retained);
        runtime.lastSpentEnergy = 0.0D;
        runtime.lastSpentTick = Long.MIN_VALUE / 4L;
    }

    private static Vec3 horizontal(Vec3 value) {
        return new Vec3(value.x, 0.0D, value.z);
    }

    public static void clear(UUID playerId) {
        RUNTIME.remove(playerId);
    }

    public static void clearAll() {
        RUNTIME.clear();
    }
}
