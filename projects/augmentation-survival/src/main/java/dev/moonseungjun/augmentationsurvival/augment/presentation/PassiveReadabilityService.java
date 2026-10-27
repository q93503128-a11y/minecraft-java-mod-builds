package dev.moonseungjun.augmentationsurvival.augment.presentation;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementTuning;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;

/**
 * Adds world-space readability to passive augments whose state changes are mechanically real but easy to miss.
 * It intentionally avoids persistent HUD cards: feedback is emitted only on authoritative state edges or procs.
 */
public final class PassiveReadabilityService {
    private static final String FOCUSED_AIM_ID = id("focused_aim");
    private static final String CATCH_BREATH_ID = id("catch_breath");
    private static final String MOMENTUM_SHIFT_ID = id("momentum_shift");
    private static final String ELASTIC_STORAGE_ID = id("elastic_storage");
    private static final String LAST_STRENGTH_ID = id("last_strength");
    private static final String LIFE_LINK_ID = id("life_link");
    private static final double STATIONARY_EPSILON_SQUARED = 1.0E-8D;
    private static final long LIFE_LINK_CUE_DEBOUNCE_TICKS = 10L;
    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();
    private static final Map<UUID, LifeLinkCue> LIFE_LINK_CUES = new HashMap<>();
    private static boolean initialized;

    private PassiveReadabilityService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(PassiveReadabilityService::tick);
        ServerPlayerEvents.LEAVE.register(player -> {
            UUID playerId = player.getUUID();
            RUNTIME.remove(playerId);
            LIFE_LINK_CUES.remove(playerId);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            RUNTIME.remove(oldPlayer.getUUID());
            RUNTIME.remove(newPlayer.getUUID());
            LIFE_LINK_CUES.remove(oldPlayer.getUUID());
            LIFE_LINK_CUES.remove(newPlayer.getUUID());
        });
    }

    public static void presentPotionDiffusion(ServerPlayer player, List<LivingEntity> recipients) {
        if (player == null || recipients == null || recipients.isEmpty()) return;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 source = player.position().add(0.0D, 1.0D, 0.0D);
        for (LivingEntity recipient : recipients) {
            if (recipient == null || recipient.isDeadOrDying() || recipient.level() != level) continue;
            Vec3 target = focusPoint(recipient);
            line(level, ParticleTypes.ENCHANT, source, target, 9);
            ring(level, ParticleTypes.END_ROD, target, 0.38D, 8);
            level.sendParticles(ParticleTypes.HEART, target.x, target.y, target.z, 2, 0.18D, 0.22D, 0.18D, 0.010D);
        }
        sound(level, source, SoundEvents.AMETHYST_BLOCK_CHIME, 0.28F, 1.38F);
    }

    public static void presentBallisticIntercept(ServerPlayer player, Projectile projectile) {
        if (player == null || projectile == null || projectile.isRemoved()) return;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 impact = projectile.position();
        ring(level, ParticleTypes.END_ROD, impact, 0.42D, 8);
        level.sendParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z, 7, 0.18D, 0.18D, 0.18D, 0.080D);
        level.sendParticles(ParticleTypes.POOF, impact.x, impact.y, impact.z, 4, 0.10D, 0.10D, 0.10D, 0.025D);
    }

    public static void presentWallSlam(ServerPlayer owner, LivingEntity target) {
        if (owner == null || target == null || target.isDeadOrDying()) return;
        ServerLevel level = (ServerLevel) owner.level();
        Vec3 impact = target.position().add(0.0D, Math.max(0.25D, target.getBbHeight() * 0.45D), 0.0D);
        ring(level, ParticleTypes.POOF, impact, 0.78D, 14);
        level.sendParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z, 12, 0.28D, 0.34D, 0.28D, 0.110D);
        Vec3 motion = target.getDeltaMovement();
        Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
        if (horizontal.lengthSqr() > 1.0E-6D) {
            Vec3 from = impact.subtract(horizontal.normalize().scale(1.0D));
            line(level, ParticleTypes.POOF, from, impact, 6);
        }
        sound(level, impact, SoundEvents.SHIELD_BLOCK.value(), 0.58F, 0.62F);
    }

    public static void presentLifeLink(ServerPlayer owner, LivingEntity primary) {
        if (owner == null || primary == null || primary.isDeadOrDying()) return;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(owner);
        if (!owns(state, LIFE_LINK_ID)) return;

        FinalAugmentTuning.Snapshot tuning = FinalAugmentTuning.current();
        double radius = tuning.lifeLinkRadius();
        double radiusSquared = radius * radius;
        List<LivingEntity> candidates = owner.level().getEntitiesOfClass(
                LivingEntity.class,
                primary.getBoundingBox().inflate(radius),
                candidate -> candidate != primary
                        && !candidate.isDeadOrDying()
                        && CompanionAugmentService.isHostileTarget(owner, candidate)
                        && primary.distanceToSqr(candidate) <= radiusSquared
        );
        candidates.sort(Comparator.comparingDouble(primary::distanceToSqr));

        List<LivingEntity> linked = new ArrayList<>();
        linked.add(primary);
        int additional = Math.max(0, tuning.lifeLinkMaxTargets() - 1);
        for (int i = 0; i < Math.min(additional, candidates.size()); i++) {
            linked.add(candidates.get(i));
        }
        if (linked.size() < 2) return;

        long now = owner.level().getGameTime();
        List<UUID> signature = linked.stream().map(LivingEntity::getUUID).toList();
        LifeLinkCue previous = LIFE_LINK_CUES.get(owner.getUUID());
        if (previous != null
                && previous.signature.equals(signature)
                && now - previous.gameTick < LIFE_LINK_CUE_DEBOUNCE_TICKS) {
            return;
        }
        LIFE_LINK_CUES.put(owner.getUUID(), new LifeLinkCue(signature, now));

        ServerLevel level = (ServerLevel) owner.level();
        Vec3 primaryPoint = focusPoint(primary);
        ring(level, ParticleTypes.WITCH, primaryPoint, 0.62D, 12);
        for (int i = 1; i < linked.size(); i++) {
            Vec3 other = focusPoint(linked.get(i));
            line(level, ParticleTypes.ENCHANT, primaryPoint, other, 10);
            ring(level, ParticleTypes.END_ROD, other, 0.42D, 8);
        }
        sound(level, primaryPoint, SoundEvents.AMETHYST_BLOCK_CHIME, 0.38F, 0.78F);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isDeadOrDying() || player.isSpectator()) {
                RUNTIME.remove(player.getUUID());
                continue;
            }
            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            if (!needsTracking(state)) {
                RUNTIME.remove(player.getUUID());
                continue;
            }
            RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> RuntimeState.create(player));
            tickFocusedAim(player, state, runtime);
            tickCatchBreath(player, state, runtime);
            tickLandingCues(player, state, runtime);
            tickLastStrength(player, state, runtime);
        }
    }

    private static void tickFocusedAim(ServerPlayer player, AugmentationPlayerState state, RuntimeState runtime) {
        if (!owns(state, FOCUSED_AIM_ID)) {
            runtime.focusedAimStationaryTicks = 0;
            runtime.focusedAimReady = false;
            runtime.lastPosition = player.position();
            return;
        }

        UtilityAugmentTuning.Snapshot tuning = UtilityAugmentTuning.current();
        Vec3 position = player.position();
        boolean stationary = runtime.lastPosition != null
                && runtime.lastPosition.distanceToSqr(position) <= STATIONARY_EPSILON_SQUARED;
        boolean fullDraw = player.isUsingItem()
                && player.getUseItem().getItem() instanceof BowItem
                && player.getTicksUsingItem() >= tuning.focusedAimFullDrawTicks();
        if (fullDraw && stationary) {
            runtime.focusedAimStationaryTicks++;
        } else {
            runtime.focusedAimStationaryTicks = 0;
        }
        boolean ready = runtime.focusedAimStationaryTicks >= tuning.focusedAimStationaryTicks();
        if (ready && !runtime.focusedAimReady) {
            emitFocusedAimReady(player);
        }
        runtime.focusedAimReady = ready;
        runtime.lastPosition = position;
    }

    private static void tickCatchBreath(ServerPlayer player, AugmentationPlayerState state, RuntimeState runtime) {
        if (!owns(state, CATCH_BREATH_ID)) {
            runtime.nonSprintingTicks = 0;
            runtime.catchBreathActive = false;
            return;
        }
        MovementTuning.Snapshot tuning = MovementTuning.current();
        if (player.isSprinting()) {
            runtime.nonSprintingTicks = 0;
        } else {
            runtime.nonSprintingTicks = Math.min(
                    tuning.catchBreathIdleTicks(),
                    runtime.nonSprintingTicks + 1
            );
        }
        boolean active = runtime.nonSprintingTicks >= tuning.catchBreathIdleTicks();
        if (active && !runtime.catchBreathActive) {
            emitCatchBreath(player);
        }
        runtime.catchBreathActive = active;
    }

    private static void tickLandingCues(ServerPlayer player, AugmentationPlayerState state, RuntimeState runtime) {
        boolean momentumOwned = owns(state, MOMENTUM_SHIFT_ID);
        boolean elasticOwned = owns(state, ELASTIC_STORAGE_ID);
        if (!momentumOwned && !elasticOwned) {
            runtime.maxObservedFallDistance = 0.0D;
            runtime.wasGrounded = player.onGround();
            return;
        }

        boolean grounded = player.onGround();
        boolean resetContact = grounded || player.isInWater() || player.onClimbable();
        if (!resetContact) {
            runtime.maxObservedFallDistance = Math.max(runtime.maxObservedFallDistance, player.fallDistance);
        } else {
            if (grounded && !runtime.wasGrounded) {
                double observed = runtime.maxObservedFallDistance;
                boolean momentum = momentumOwned
                        && observed >= MovementTuning.current().momentumMinimumFallDistance();
                boolean elastic = elasticOwned
                        && observed >= FinalAugmentTuning.current().elasticMinimumFallDistance();
                if (momentum || elastic) {
                    emitLanding(player, momentum, elastic);
                }
            }
            runtime.maxObservedFallDistance = 0.0D;
        }
        runtime.wasGrounded = grounded;
    }

    private static void tickLastStrength(ServerPlayer player, AugmentationPlayerState state, RuntimeState runtime) {
        if (!owns(state, LAST_STRENGTH_ID)) {
            runtime.lastStrengthActive = false;
            return;
        }
        float maxHealth = player.getMaxHealth();
        boolean active = maxHealth > 0.0F
                && player.getHealth() / maxHealth <= CombatTuning.current().lastStrengthHealthThreshold();
        if (active && !runtime.lastStrengthActive) {
            emitLastStrength(player);
        }
        runtime.lastStrengthActive = active;
    }

    private static void emitFocusedAimReady(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 focus = eye.add(player.getLookAngle().scale(1.45D));
        level.sendParticles(ParticleTypes.END_ROD, focus.x, focus.y, focus.z, 5, 0.045D, 0.045D, 0.045D, 0.008D);
        level.sendParticles(ParticleTypes.ENCHANT, focus.x, focus.y, focus.z, 7, 0.10D, 0.10D, 0.10D, 0.020D);
        sound(level, eye, SoundEvents.AMETHYST_BLOCK_CHIME, 0.26F, 1.62F);
    }

    private static void emitCatchBreath(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 self = player.position();
        level.sendParticles(ParticleTypes.CLOUD, self.x, self.y + 0.12D, self.z, 7, 0.34D, 0.08D, 0.34D, 0.020D);
        ring(level, ParticleTypes.POOF, self.add(0.0D, 0.18D, 0.0D), 0.72D, 10);
        sound(level, self, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.18F, 1.55F);
    }

    private static void emitLanding(ServerPlayer player, boolean momentum, boolean elastic) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 self = player.position();
        if (momentum) {
            ring(level, ParticleTypes.CLOUD, self.add(0.0D, 0.08D, 0.0D), 1.05D, 14);
            Vec3 horizontalLook = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
            if (horizontalLook.lengthSqr() > 1.0E-6D) {
                Vec3 backward = self.subtract(horizontalLook.normalize().scale(1.1D));
                line(level, ParticleTypes.POOF, backward.add(0.0D, 0.12D, 0.0D), self.add(0.0D, 0.12D, 0.0D), 6);
            }
        }
        if (elastic) {
            ring(level, ParticleTypes.END_ROD, self.add(0.0D, 0.14D, 0.0D), 0.52D, 10);
            level.sendParticles(ParticleTypes.CRIT, self.x, self.y + 0.16D, self.z, 6, 0.22D, 0.08D, 0.22D, 0.025D);
        }
        sound(
                level,
                self,
                momentum ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.AMETHYST_BLOCK_CHIME,
                momentum ? 0.32F : 0.26F,
                elastic ? 0.92F : 1.28F
        );
    }

    private static void emitLastStrength(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 chest = player.position().add(0.0D, 1.0D, 0.0D);
        ring(level, ParticleTypes.CRIT, chest, 0.72D, 12);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, chest.x, chest.y, chest.z, 7, 0.24D, 0.32D, 0.24D, 0.035D);
        sound(level, chest, SoundEvents.PLAYER_ATTACK_CRIT, 0.34F, 0.72F);
    }

    private static boolean needsTracking(AugmentationPlayerState state) {
        return owns(state, FOCUSED_AIM_ID)
                || owns(state, CATCH_BREATH_ID)
                || owns(state, MOMENTUM_SHIFT_ID)
                || owns(state, ELASTIC_STORAGE_ID)
                || owns(state, LAST_STRENGTH_ID);
    }

    private static boolean owns(AugmentationPlayerState state, String augmentId) {
        return state.level(augmentId) > 0;
    }

    private static Vec3 focusPoint(LivingEntity entity) {
        return entity.position().add(0.0D, Math.max(0.35D, entity.getBbHeight() * 0.55D), 0.0D);
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            level.sendParticles(
                    particle,
                    center.x + Math.cos(angle) * radius,
                    center.y,
                    center.z + Math.sin(angle) * radius,
                    1,
                    0.0D, 0.0D, 0.0D,
                    0.0D
            );
        }
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int points) {
        Vec3 delta = to.subtract(from);
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            Vec3 point = from.add(delta.scale(t));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static final class RuntimeState {
        private Vec3 lastPosition;
        private int focusedAimStationaryTicks;
        private boolean focusedAimReady;
        private int nonSprintingTicks;
        private boolean catchBreathActive;
        private double maxObservedFallDistance;
        private boolean wasGrounded;
        private boolean lastStrengthActive;

        private static RuntimeState create(ServerPlayer player) {
            RuntimeState state = new RuntimeState();
            state.lastPosition = player.position();
            state.wasGrounded = player.onGround();
            return state;
        }
    }

    private record LifeLinkCue(List<UUID> signature, long gameTick) {
    }
}
