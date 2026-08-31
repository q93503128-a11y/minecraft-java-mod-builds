package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.network.TitanbreakNetwork;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime movement mechanics for the five leg augmentation families.
 *
 * The catalog intentionally keeps the +7 jump-booster milestone as "conditional triple jump"
 * without naming the condition. For alpha.25 the third jump is only accepted once the player
 * has started descending (or is touching a wall), keeping the rule skill-based and isolated here
 * so later balance passes can replace it without changing save data.
 */
public final class LegAugmentationService {
    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();

    private static final class RuntimeState {
        boolean grounded = true;
        double peakY;
        int airborneTicks;
        int jumpUses;
        int propulsionUses;
        boolean recoveredAirJump;
        int wallRunTicks;
        Vec3 wallDirection = Vec3.ZERO;

        RuntimeState(ServerPlayer player) {
            grounded = player.onGround();
            peakY = player.getY();
        }

        void resetGround(ServerPlayer player) {
            grounded = true;
            peakY = player.getY();
            airborneTicks = 0;
            jumpUses = 0;
            propulsionUses = 0;
            recoveredAirJump = false;
            wallRunTicks = 0;
            wallDirection = Vec3.ZERO;
        }
    }

    private LegAugmentationService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState(player));
        boolean grounded = player.onGround();

        if (grounded) {
            if (!runtime.grounded) handleLanding(player, state, runtime);
            runtime.resetGround(player);
            return;
        }

        if (runtime.grounded) {
            runtime.grounded = false;
            runtime.peakY = player.getY();
            runtime.airborneTicks = 0;
            runtime.jumpUses = 0;
            runtime.propulsionUses = 0;
            runtime.recoveredAirJump = false;
            runtime.wallRunTicks = 0;
            runtime.wallDirection = Vec3.ZERO;
        }

        runtime.airborneTicks++;
        runtime.peakY = Math.max(runtime.peakY, player.getY());
        tickAirRecovery(state, runtime);
        tickWallRun(player, state, runtime);
    }

    /** Called from the client's normal jump key. Ground jumps remain vanilla unless an augment adds output. */
    public static void useMobilityJump(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState(player));

        TitanPlayerData.AugmentInstance propulsion = state.firstInstalledInstance("propulsion_legs");
        if (!player.onGround() && player.isSprinting() && propulsion != null) {
            if (usePropulsion(player, state, runtime, propulsion)) {
                data.addMasteryXp(player, "propulsion_legs", 3);
                TitanbreakNetwork.sync(player);
            }
            return;
        }

        TitanPlayerData.AugmentInstance booster = state.firstInstalledInstance("jump_booster_legs");
        if (booster != null) {
            if (useJumpBooster(player, state, runtime, booster)) {
                data.addMasteryXp(player, "jump_booster_legs", player.onGround() ? 1 : 2);
                TitanbreakNetwork.sync(player);
            }
            return;
        }

        TitanPlayerData.AugmentInstance reinforced = state.firstInstalledInstance("reinforced_legs");
        if (reinforced != null && player.onGround()) {
            Vec3 motion = player.getDeltaMovement();
            double jump = 0.49D + Math.max(0, reinforced.mk() - 1) * 0.012D;
            if (reinforced.enhancement() >= 7) jump += 0.08D;
            player.setDeltaMovement(motion.x, Math.max(motion.y, jump), motion.z);
            player.hurtMarked = true;
            data.addMasteryXp(player, "reinforced_legs", 1);
        }
    }

    private static boolean useJumpBooster(ServerPlayer player, TitanPlayerData.State state, RuntimeState runtime,
                                          TitanPlayerData.AugmentInstance booster) {
        if (player.onGround()) {
            if (!spend(player, state, "jump_booster_legs", 0.28D, 0.24D)) return false;
            Vec3 motion = player.getDeltaMovement();
            double jump = 0.66D + Math.max(0, booster.mk() - 1) * 0.025D;
            player.setDeltaMovement(motion.x, Math.max(motion.y, jump), motion.z);
            player.hurtMarked = true;
            return true;
        }

        int enhancement = booster.enhancement();
        int allowed = enhancement >= 7 ? 2 : enhancement >= 5 ? 1 : 0;
        if (runtime.jumpUses >= allowed) return false;

        if (runtime.jumpUses >= 1 && enhancement >= 7) {
            boolean thirdJumpCondition = player.getDeltaMovement().y <= 0.05D || player.horizontalCollision;
            if (!thirdJumpCondition) return false;
        }

        if (!spend(player, state, "jump_booster_legs", 0.32D, 0.28D)) return false;
        Vec3 motion = player.getDeltaMovement();
        double jump = 0.62D + Math.max(0, booster.mk() - 1) * 0.025D;
        player.setDeltaMovement(motion.x, jump, motion.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        runtime.jumpUses++;
        return true;
    }

    private static boolean usePropulsion(ServerPlayer player, TitanPlayerData.State state, RuntimeState runtime,
                                         TitanPlayerData.AugmentInstance propulsion) {
        int enhancement = propulsion.enhancement();
        int charges = enhancement >= 10 ? 3 : enhancement >= 5 ? 2 : 1;
        if (runtime.propulsionUses >= charges) return false;
        if (!spend(player, state, "propulsion_legs", 0.46D, 0.50D)) return false;

        Vec3 look = player.getLookAngle();
        Vec3 direction;
        if (enhancement >= 7) {
            direction = new Vec3(look.x, Math.max(-0.30D, Math.min(0.45D, look.y * 0.65D)), look.z);
        } else {
            direction = new Vec3(look.x, 0.0D, look.z);
        }
        if (direction.lengthSqr() <= 1.0E-6D) return false;
        direction = direction.normalize();

        double speed = 1.18D + Math.max(0, propulsion.mk() - 1) * 0.075D;
        if (enhancement >= 10) speed += 0.18D;
        Vec3 dash = direction.scale(speed);
        if (enhancement >= 7) {
            Vec3 current = player.getDeltaMovement();
            dash = dash.add(current.x * 0.25D, Math.max(0.0D, current.y) * 0.25D, current.z * 0.25D);
        }
        player.setDeltaMovement(dash);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        runtime.propulsionUses++;
        return true;
    }

    private static void tickAirRecovery(TitanPlayerData.State state, RuntimeState runtime) {
        TitanPlayerData.AugmentInstance booster = state.firstInstalledInstance("jump_booster_legs");
        if (booster == null || booster.enhancement() < 10 || runtime.recoveredAirJump) return;
        if (runtime.airborneTicks < 30 || runtime.jumpUses <= 0) return;
        runtime.jumpUses--;
        runtime.recoveredAirJump = true;
    }

    private static void tickWallRun(ServerPlayer player, TitanPlayerData.State state, RuntimeState runtime) {
        TitanPlayerData.AugmentInstance spurs = state.firstInstalledInstance("wall_run_spurs");
        if (spurs == null || !player.isSprinting()) {
            runtime.wallRunTicks = 0;
            runtime.wallDirection = Vec3.ZERO;
            return;
        }

        int enhancement = spurs.enhancement();
        boolean blockSurface = player.horizontalCollision;
        boolean bossSurface = enhancement >= 10 && touchesLargeSurface(player);
        if (!blockSurface && !bossSurface) {
            runtime.wallRunTicks = 0;
            runtime.wallDirection = Vec3.ZERO;
            return;
        }

        int maxTicks = enhancement >= 5
                ? 70 + Math.max(0, spurs.mk() - 1) * 5
                : 32 + Math.max(0, spurs.mk() - 1) * 3;
        if (runtime.wallRunTicks >= maxTicks) return;

        if (runtime.wallRunTicks == 0 || enhancement >= 7 || runtime.wallDirection.lengthSqr() <= 1.0E-6D) {
            Vec3 look = player.getLookAngle();
            runtime.wallDirection = new Vec3(look.x, 0.0D, look.z);
            if (runtime.wallDirection.lengthSqr() <= 1.0E-6D) return;
            runtime.wallDirection = runtime.wallDirection.normalize();
        }

        if (runtime.wallRunTicks % 10 == 0 && !spend(player, state, "wall_run_spurs", 0.14D, 0.10D)) {
            runtime.wallRunTicks = maxTicks;
            return;
        }

        double speed = 0.31D + Math.max(0, spurs.mk() - 1) * 0.022D;
        Vec3 motion = player.getDeltaMovement();
        Vec3 direction = runtime.wallDirection;
        double vertical = Math.max(motion.y, enhancement >= 5 ? -0.015D : -0.035D);
        player.setDeltaMovement(direction.x * speed, vertical, direction.z * speed);
        player.hurtMarked = true;
        player.fallDistance = Math.min(player.fallDistance, 1.0F);
        runtime.wallRunTicks++;

        if (runtime.wallRunTicks % 20 == 0 && player.level() instanceof ServerLevel level) {
            TitanPlayerData.get(level.getServer()).addMasteryXp(player, "wall_run_spurs", 2);
        }
    }

    private static boolean touchesLargeSurface(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        AABB search = player.getBoundingBox().inflate(1.15D, 0.75D, 1.15D);
        for (Entity entity : level.getEntities(player, search, target -> target instanceof LivingEntity && target.isAlive())) {
            if (entity.getBbWidth() >= 2.5F || entity.getBbHeight() >= 3.5F) return true;
        }
        return false;
    }

    private static void handleLanding(ServerPlayer player, TitanPlayerData.State state, RuntimeState runtime) {
        double drop = Math.max(0.0D, runtime.peakY - player.getY());
        if (drop < 3.0D) return;

        TitanPlayerData.AugmentInstance absorber = state.firstInstalledInstance("impact_absorber_legs");
        if (absorber == null) return;
        player.fallDistance = 0.0F;

        if (!spend(player, state, "impact_absorber_legs", 0.34D, 0.42D)) return;
        int enhancement = absorber.enhancement();
        double radius = enhancement >= 7
                ? Math.min(6.0D, 2.5D + drop * 0.18D)
                : Math.min(2.2D, 1.35D + drop * 0.07D);
        double visibleDamage = Math.min(enhancement >= 7 ? 90.0D : 42.0D,
                8.0D + drop * (enhancement >= 7 ? 3.2D : 1.8D));
        landingShockwave(player, radius, visibleDamage);

        if (enhancement >= 10) {
            int breachPower = drop >= 12.0D ? 3 : 2;
            BreachService.breachArea(player, player.blockPosition().below(), breachPower,
                    Math.min(3.25D, 1.5D + drop * 0.10D), drop >= 12.0D ? 24 : 12, false);
        }

        if (player.level() instanceof ServerLevel level) {
            TitanPlayerData data = TitanPlayerData.get(level.getServer());
            data.addMasteryXp(player, "impact_absorber_legs", enhancement >= 7 ? 3 : 2);
            TitanbreakNetwork.sync(player);
        }
    }

    private static void landingShockwave(ServerPlayer player, double radius, double visibleDamage) {
        if (!(player.level() instanceof ServerLevel level)) return;
        for (Entity target : level.getEntities(player, player.getBoundingBox().inflate(radius),
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity.distanceToSqr(player) <= radius * radius)) {
            target.hurtServer(level, player.damageSources().playerAttack(player),
                    (float) CombatScale.toInternal(visibleDamage));
            Vec3 away = target.position().subtract(player.position());
            if (away.lengthSqr() > 1.0E-6D) {
                away = away.normalize();
                target.push(away.x * 0.65D, 0.28D, away.z * 0.65D);
                target.hurtMarked = true;
            }
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        if (!event.getSource().is(DamageTypes.FALL)) return;

        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        float damage = event.getAmount();
        if (damage <= 0.0F) return;

        TitanPlayerData.AugmentInstance absorber = state.firstInstalledInstance("impact_absorber_legs");
        if (absorber != null) {
            RuntimeState runtime = RUNTIME.get(player.getUUID());
            double estimatedDrop = runtime == null ? player.fallDistance : Math.max(player.fallDistance, runtime.peakY - player.getY());
            if (absorber.enhancement() >= 5
                    && estimatedDrop <= 14.0D + Math.max(0, absorber.mk() - 1) * 1.5D) {
                damage = 0.0F;
            } else {
                damage *= absorber.enhancement() >= 5 ? 0.18F : 0.42F;
            }
        }

        TitanPlayerData.AugmentInstance reinforced = state.firstInstalledInstance("reinforced_legs");
        if (reinforced != null && reinforced.enhancement() >= 10) damage *= 0.70F;

        event.getContainer().setNewDamage(Math.max(0.0F, damage));
        if (damage <= 1.0E-4F) event.getContainer().setShouldCauseSideEffects(false);
    }

    private static boolean spend(ServerPlayer player, TitanPlayerData.State state, String augmentId,
                                 double powerFactor, double heatFactor) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(augmentId);
        if (definition == null || state.heat() >= 99.0D) return false;
        double power = Math.max(0.0D, definition.powerLoad()) * powerFactor * state.powerLoadMultiplier(augmentId);
        if (!AugmentationResourceService.trySpendBurstPower(player, state, power)) return false;
        if (definition.heatLoad() > 0 && heatFactor > 0.0D && player.level() instanceof ServerLevel level) {
            TitanPlayerData data = TitanPlayerData.get(level.getServer());
            double rawHeat = definition.heatLoad() * heatFactor * state.heatLoadMultiplier(augmentId);
            data.setHeat(player, state.heat() + AugmentationResourceService.normalizedHeatGain(state, rawHeat));
        }
        return true;
    }

    public static void clear(UUID playerId) {
        RUNTIME.remove(playerId);
    }

    public static void clearAll() {
        RUNTIME.clear();
    }
}
