package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative runtime for standalone return-home and mythic control/damage actives. */
public final class SpecialActiveAugmentService {
    private static final String RETURN_HOME_ID = id("return_home");
    private static final String TIME_STOP_ID = id("time_stop");
    private static final String DIVINE_PUNISHMENT_ID = id("divine_punishment");
    private static final ResourceKey<DamageType> DIVINE_PUNISHMENT_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "divine_punishment")
    );

    private static final Map<UUID, ReturnHomeChannel> RETURN_HOME_CHANNELS = new HashMap<>();
    private static final Map<LivingEntity, ControlState> CONTROLLED = new IdentityHashMap<>();
    private static final Map<Projectile, ProjectileFreezeState> FROZEN_PROJECTILES = new IdentityHashMap<>();

    private SpecialActiveAugmentService() {
    }

    public static void initialize() {
        AugmentCatalog.require(RETURN_HOME_ID);
        AugmentCatalog.require(TIME_STOP_ID);
        AugmentCatalog.require(DIVINE_PUNISHMENT_ID);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(SpecialActiveAugmentService::afterDamage);
        ServerPlayerEvents.JOIN.register(SpecialActiveAugmentService::clearPlayerRuntime);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> clearPlayerRuntime(newPlayer));
        ServerTickEvents.END_SERVER_TICK.register(SpecialActiveAugmentService::tickRuntime);
    }

    public static boolean tryUse(ServerPlayer player, String abilityId) {
        if (RETURN_HOME_ID.equals(abilityId)) {
            return tryStartReturnHome(player);
        }
        if (TIME_STOP_ID.equals(abilityId)) {
            return tryTimeStop(player);
        }
        if (DIVINE_PUNISHMENT_ID.equals(abilityId)) {
            return tryDivinePunishment(player);
        }
        return false;
    }

    public static void notifyDisruptingAction(ServerPlayer player) {
        if (RETURN_HOME_CHANNELS.remove(player.getUUID()) != null) {
            ActivePresentationService.notifyState(
                    player, RETURN_HOME_ID, ActivePresentationPayload.CANCEL, 0, 0.0F
            );
        }
    }

    public static boolean isTimeStopped(LivingEntity entity) {
        ControlState state = CONTROLLED.get(entity);
        return state != null && state.timeStopTicks > 0;
    }

    public static boolean isProjectileTimeStopped(Projectile projectile) {
        return FROZEN_PROJECTILES.containsKey(projectile);
    }

    private static boolean tryStartReturnHome(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, RETURN_HOME_ID) <= 0 || player.getRespawnConfig() == null) {
            return false;
        }
        TeleportTransition destination = player.findRespawnPositionAndUseSpawnBlock(
                false,
                TeleportTransition.DO_NOTHING
        );
        if (destination == null || destination.missingRespawnBlock()) {
            return false;
        }
        long now = player.level().getGameTime();
        RETURN_HOME_CHANNELS.put(
                player.getUUID(),
                new ReturnHomeChannel(
                        player.level().dimension().identifier().toString(),
                        player.position(),
                        now + SpecialActiveTuning.current().returnHomeFocusTicks()
                )
        );
        return true;
    }

    private static boolean tryTimeStop(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, TIME_STOP_ID) <= 0 || !cooldownReady(player, state, TIME_STOP_ID)) {
            return false;
        }
        SpecialActiveTuning.Snapshot tuning = SpecialActiveTuning.current();
        setCooldown(player, state, TIME_STOP_ID, tuning.timeStopCooldownTicks());

        ServerLevel level = player.level();
        double radius = tuning.timeStopRadius();
        double radiusSquared = radius * radius;
        AABB bounds = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                bounds,
                candidate -> CompanionAugmentService.isHostileTarget(player, candidate)
                        && player.distanceToSqr(candidate) <= radiusSquared
                        && EntityCompatibilityProfileService.allowTimeStop(candidate)
        )) {
            applyTimeStop(target, tuning.timeStopDurationTicks());
        }
        for (Projectile projectile : level.getEntitiesOfClass(
                Projectile.class,
                bounds,
                candidate -> !candidate.isRemoved()
                        && player.distanceToSqr(candidate) <= radiusSquared
                        && isHostileProjectile(player, candidate)
        )) {
            freezeProjectile(projectile, tuning.timeStopDurationTicks());
        }
        return true;
    }

    private static boolean tryDivinePunishment(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int augmentLevel = boundedLevel(state, DIVINE_PUNISHMENT_ID);
        if (augmentLevel <= 0 || !cooldownReady(player, state, DIVINE_PUNISHMENT_ID)) {
            return false;
        }
        SpecialActiveTuning.Snapshot tuning = SpecialActiveTuning.current();
        setCooldown(player, state, DIVINE_PUNISHMENT_ID, tuning.divinePunishmentCooldownTicks());

        ServerLevel level = player.level();
        DamageSource source = player.damageSources().source(DIVINE_PUNISHMENT_DAMAGE, player);
        double radius = tuning.divinePunishmentRadius();
        double radiusSquared = radius * radius;
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> CompanionAugmentService.isHostileTarget(player, candidate)
                        && player.distanceToSqr(candidate) <= radiusSquared
        )) {
            boolean boss = EntityCompatibilityProfileService.isBoss(target);
            if (EntityCompatibilityProfileService.allowPercentHpDamage(target)) {
                float damage = (float) Math.min(
                        Float.MAX_VALUE,
                        target.getMaxHealth() * tuning.divinePunishmentFraction(augmentLevel, boss)
                );
                if (damage > 0.0F && Float.isFinite(damage)) {
                    CombatProvenanceContext.runAs(
                            CombatProvenance.SPECIAL_PERCENT_HP,
                            () -> target.hurtServer(level, source, damage)
                    );
                }
            }
            if (augmentLevel >= 2 && EntityCompatibilityProfileService.allowStun(target)) {
                applyStun(target, tuning.divinePunishmentStunTicksLevel2());
            }
        }
        return true;
    }

    private static void tickRuntime(MinecraftServer server) {
        tickReturnHome(server);
        tickControlledEntities();
        tickFrozenProjectiles();
    }

    private static void tickReturnHome(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ReturnHomeChannel>> iterator = RETURN_HOME_CHANNELS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ReturnHomeChannel> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isDeadOrDying()) {
                if (player != null) {
                    ActivePresentationService.notifyState(
                            player, RETURN_HOME_ID, ActivePresentationPayload.CANCEL, 0, 0.0F
                    );
                }
                iterator.remove();
                continue;
            }
            ReturnHomeChannel channel = entry.getValue();
            if (!channel.dimensionId.equals(player.level().dimension().identifier().toString())
                    || channel.startPosition.distanceToSqr(player.position()) > 1.0E-6D
                    || player.isUsingItem()) {
                ActivePresentationService.notifyState(
                        player, RETURN_HOME_ID, ActivePresentationPayload.CANCEL, 0, 0.0F
                );
                iterator.remove();
                continue;
            }
            if (player.level().getGameTime() < channel.completeTick) {
                continue;
            }
            iterator.remove();
            if (player.getRespawnConfig() == null) {
                ActivePresentationService.notifyState(
                        player, RETURN_HOME_ID, ActivePresentationPayload.CANCEL, 0, 0.0F
                );
                continue;
            }
            TeleportTransition destination = player.findRespawnPositionAndUseSpawnBlock(
                    false,
                    TeleportTransition.DO_NOTHING
            );
            if (destination == null || destination.missingRespawnBlock()) {
                ActivePresentationService.notifyState(
                        player, RETURN_HOME_ID, ActivePresentationPayload.CANCEL, 0, 0.0F
                );
                continue;
            }
            ServerPlayer teleported = player.teleport(destination);
            if (teleported != null) {
                ActivePresentationService.notifyStateAndPulse(
                        teleported, RETURN_HOME_ID, ActivePresentationPayload.END, 0, 0.0F
                );
            }
        }
    }

    private static void tickControlledEntities() {
        Iterator<Map.Entry<LivingEntity, ControlState>> iterator = CONTROLLED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, ControlState> entry = iterator.next();
            LivingEntity entity = entry.getKey();
            ControlState state = entry.getValue();
            if (entity.isRemoved() || entity.isDeadOrDying()) {
                iterator.remove();
                continue;
            }
            boolean stoppedAtTickStart = state.timeStopTicks > 0;
            if (state.timeStopTicks > 0) {
                state.timeStopTicks--;
            }
            if (!stoppedAtTickStart && state.stunTicks > 0) {
                state.stunTicks--;
            }
            if (state.timeStopTicks > 0 || state.stunTicks > 0 || stoppedAtTickStart) {
                entity.setDeltaMovement(Vec3.ZERO);
                if (entity instanceof Mob mob) {
                    mob.setNoAi(true);
                }
            }
            if (state.timeStopTicks > 0 && state.timeStopTicks % 10 == 0 && entity.level() instanceof ServerLevel level) {
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                        1, entity.getBbWidth() * 0.35D, entity.getBbHeight() * 0.25D,
                        entity.getBbWidth() * 0.35D, 0.0D
                );
            }
            if (state.timeStopTicks <= 0 && state.stunTicks <= 0) {
                if (entity instanceof Mob mob) {
                    mob.setNoAi(state.originalNoAi);
                }
                iterator.remove();
            }
        }
    }

    private static void tickFrozenProjectiles() {
        Iterator<Map.Entry<Projectile, ProjectileFreezeState>> iterator = FROZEN_PROJECTILES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Projectile, ProjectileFreezeState> entry = iterator.next();
            Projectile projectile = entry.getKey();
            ProjectileFreezeState state = entry.getValue();
            if (projectile.isRemoved()) {
                iterator.remove();
                continue;
            }
            state.remainingTicks--;
            if (state.remainingTicks <= 0) {
                projectile.setNoGravity(state.originalNoGravity);
                projectile.setDeltaMovement(state.originalVelocity);
                iterator.remove();
                continue;
            }
            projectile.setNoGravity(true);
            projectile.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void applyTimeStop(LivingEntity entity, int ticks) {
        ControlState state = CONTROLLED.computeIfAbsent(entity, SpecialActiveAugmentService::newControlState);
        state.timeStopTicks = Math.max(state.timeStopTicks, ticks);
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }
    }

    private static void applyStun(LivingEntity entity, int ticks) {
        ControlState state = CONTROLLED.computeIfAbsent(entity, SpecialActiveAugmentService::newControlState);
        state.stunTicks = Math.max(state.stunTicks, ticks);
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }
    }

    private static ControlState newControlState(LivingEntity entity) {
        return new ControlState(entity instanceof Mob mob && mob.isNoAi());
    }

    private static void freezeProjectile(Projectile projectile, int ticks) {
        ProjectileFreezeState state = FROZEN_PROJECTILES.get(projectile);
        if (state == null) {
            state = new ProjectileFreezeState(
                    projectile.getDeltaMovement(),
                    projectile.isNoGravity(),
                    ticks
            );
            FROZEN_PROJECTILES.put(projectile, state);
        } else {
            state.remainingTicks = Math.max(state.remainingTicks, ticks);
        }
        projectile.setNoGravity(true);
        projectile.setDeltaMovement(Vec3.ZERO);
    }

    private static boolean isHostileProjectile(ServerPlayer player, Projectile projectile) {
        if (!(projectile.getOwner() instanceof LivingEntity owner)) {
            return false;
        }
        return CompanionAugmentService.isHostileTarget(player, owner)
                && EntityCompatibilityProfileService.allowTimeStop(owner);
    }

    private static boolean cooldownReady(
            ServerPlayer player,
            AugmentationPlayerState state,
            String augmentId
    ) {
        return state.cooldownReadyTick(augmentId) <= player.level().getGameTime();
    }

    private static void setCooldown(
            ServerPlayer player,
            AugmentationPlayerState state,
            String augmentId,
            int cooldownTicks
    ) {
        AugmentationPlayerStateService.replace(
                player,
                state.withCooldownReadyTick(augmentId, player.level().getGameTime() + cooldownTicks)
        );
    }

    private static void afterDamage(
            LivingEntity entity,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked
    ) {
        if (entity instanceof ServerPlayer player && !blocked && damageTaken > 0.0F) {
            notifyDisruptingAction(player);
        }
    }

    private static void clearPlayerRuntime(ServerPlayer player) {
        RETURN_HOME_CHANNELS.remove(player.getUUID());
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private record ReturnHomeChannel(String dimensionId, Vec3 startPosition, long completeTick) {
    }

    private static final class ControlState {
        private final boolean originalNoAi;
        private int timeStopTicks;
        private int stunTicks;

        private ControlState(boolean originalNoAi) {
            this.originalNoAi = originalNoAi;
        }
    }

    private static final class ProjectileFreezeState {
        private final Vec3 originalVelocity;
        private final boolean originalNoGravity;
        private int remainingTicks;

        private ProjectileFreezeState(Vec3 originalVelocity, boolean originalNoGravity, int remainingTicks) {
            this.originalVelocity = originalVelocity;
            this.originalNoGravity = originalNoGravity;
            this.remainingTicks = remainingTicks;
        }
    }
}
