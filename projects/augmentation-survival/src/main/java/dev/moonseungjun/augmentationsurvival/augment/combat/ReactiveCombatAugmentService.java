package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeCarrier;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeState;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared runtime for reactive combat, removable harmful effects and path-echo mechanics. */
public final class ReactiveCombatAugmentService {
    private static final String RETURN_TO_SENDER_ID = id("return_to_sender");
    private static final String STATUS_REFLECT_ID = id("status_reflect");
    private static final String STRONG_MIND_ID = id("strong_mind");
    private static final String SHIELD_BURST_ID = id("shield_burst");
    private static final String RETRACE_ID = id("retrace");
    private static final String INVULNERABILITY_ID = id("invincibility");

    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();
    private static final Set<Projectile> REFLECTED_PROJECTILES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final ThreadLocal<Boolean> EFFECT_REENTRY =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ReactiveCombatAugmentService() {
    }

    public static void initialize() {
        for (String augmentId : new String[]{
                RETURN_TO_SENDER_ID, STATUS_REFLECT_ID, STRONG_MIND_ID,
                SHIELD_BURST_ID, RETRACE_ID, INVULNERABILITY_ID
        }) {
            AugmentCatalog.require(augmentId);
        }
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(ReactiveCombatAugmentService::allowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(ReactiveCombatAugmentService::afterDamage);
        ServerPlayerEvents.JOIN.register(player -> reconcile(
                player,
                AugmentationPlayerStateService.state(player)
        ));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            RUNTIME.remove(newPlayer.getUUID());
            reconcile(newPlayer, AugmentationPlayerStateService.state(newPlayer));
        });
        ServerTickEvents.END_SERVER_TICK.register(ReactiveCombatAugmentService::tickRuntime);
    }

    public static void reconcile(ServerPlayer player, AugmentationPlayerState state) {
        boolean needsRuntime = boundedLevel(state, RETRACE_ID) > 0
                || boundedLevel(state, INVULNERABILITY_ID) > 0;
        if (!needsRuntime) {
            RUNTIME.remove(player.getUUID());
            return;
        }
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        if (boundedLevel(state, INVULNERABILITY_ID) > 0) {
            if (runtime.nextInvulnerabilityTick <= 0L) {
                runtime.nextInvulnerabilityTick = player.level().getGameTime()
                        + ReactiveCombatTuning.current().invulnerabilityIntervalTicks();
            }
        } else {
            runtime.nextInvulnerabilityTick = 0L;
            runtime.invulnerableUntilTick = 0L;
        }
        if (boundedLevel(state, RETRACE_ID) <= 0) {
            runtime.positions.clear();
        }
    }

    public static boolean isInvulnerable(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        return runtime != null
                && runtime.invulnerableUntilTick > player.level().getGameTime()
                && boundedLevel(AugmentationPlayerStateService.state(player), INVULNERABILITY_ID) > 0;
    }

    public static boolean isReflectedProjectile(Entity entity) {
        return entity instanceof Projectile projectile && REFLECTED_PROJECTILES.contains(projectile);
    }

    /**
     * Intercepts finite removable-style harmful effects. Null means vanilla should continue.
     * A non-null result means this method already handled or rejected the incoming application.
     */
    public static Boolean interceptIncomingEffect(
            ServerPlayer victim,
            MobEffectInstance incoming,
            Entity source
    ) {
        if (Boolean.TRUE.equals(EFFECT_REENTRY.get()) || !isFiniteHarmful(incoming)) {
            return null;
        }
        if (isInvulnerable(victim)) {
            return Boolean.FALSE;
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(victim);
        int reflectLevel = boundedLevel(state, STATUS_REFLECT_ID);
        if (reflectLevel > 0 && isDirectEnemySource(victim, source)) {
            reflectEffect(victim, (LivingEntity) source, incoming);
        }

        int mindLevel = boundedLevel(state, STRONG_MIND_ID);
        if (mindLevel <= 0 || isBossSource(source)) {
            return null;
        }
        double reduction = ReactiveCombatTuning.current().strongMindDurationReduction(mindLevel);
        int duration = Math.max(1, (int) Math.ceil(incoming.getDuration() * (1.0D - reduction)));
        if (duration >= incoming.getDuration()) {
            return null;
        }
        MobEffectInstance reduced = copyWithDuration(incoming, duration);
        EFFECT_REENTRY.set(Boolean.TRUE);
        try {
            return victim.addEffect(reduced, source);
        } finally {
            EFFECT_REENTRY.remove();
        }
    }

    /** Called by source-owned protection pools exactly once when a pool expires or fully depletes. */
    public static void onProtectionPoolEnded(ServerPlayer player, float effectivePoolAmount) {
        if (!(effectivePoolAmount > 0.0F) || !Float.isFinite(effectivePoolAmount)) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, SHIELD_BURST_ID);
        if (level <= 0) {
            return;
        }
        ReactiveCombatTuning.Snapshot tuning = ReactiveCombatTuning.current();
        float damage = (float) Math.min(
                tuning.shieldBurstDamageCap(level),
                effectivePoolAmount * tuning.shieldBurstDamageMultiplier(level)
        );
        double radius = tuning.shieldBurstRadius();
        double radiusSquared = radius * radius;
        for (LivingEntity target : player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> CompanionAugmentService.isHostileTarget(player, candidate)
                        && player.distanceToSqr(candidate) <= radiusSquared
        )) {
            applyDerivedDamage(player, target, damage, CombatProvenance.DERIVED_AOE);
        }
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player) || !isInvulnerable(player)) {
            return true;
        }
        return source.typeHolder().is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    private static void afterDamage(
            LivingEntity entity,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked
    ) {
        if (entity instanceof ServerPlayer player && blocked) {
            tryReturnProjectile(player, source);
        }
        if (blocked || !(damageTaken > 0.0F)
                || !(source.getEntity() instanceof ServerPlayer attacker)
                || !CompanionAugmentService.isHostileTarget(attacker, entity)
                || CombatProvenanceContext.resolve(source) != CombatProvenance.ROOT_MELEE
                || attacker.getAttackStrengthScale(0.5F) < 0.90F) {
            return;
        }
        triggerRetrace(attacker, baseDamageTaken);
    }

    private static void tryReturnProjectile(ServerPlayer player, DamageSource source) {
        if (!(source.getDirectEntity() instanceof Projectile projectile)
                || projectile.getDeltaMovement().lengthSqr() <= 1.0E-7D) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, RETURN_TO_SENDER_ID);
        if (level <= 0) {
            return;
        }
        double speed = projectile.getDeltaMovement().length()
                * ReactiveCombatTuning.current().returnSpeedScale(level);
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() <= 1.0E-7D) {
            return;
        }
        projectile.setOwner(player);
        projectile.setDeltaMovement(direction.normalize().scale(speed));
        REFLECTED_PROJECTILES.add(projectile);

        if (projectile instanceof AbstractArrow arrow && projectile instanceof ProjectileRuntimeCarrier carrier) {
            ProjectileRuntimeState runtime = carrier.augmentationSurvival$getProjectileRuntime();
            if (runtime == null) {
                runtime = new ProjectileRuntimeState();
                carrier.augmentationSurvival$setProjectileRuntime(runtime);
            }
            if (!runtime.initialized()) {
                runtime.initialize(player.getUUID(), arrow.getPierceLevel(), 0, 0, 0, 0);
            }
            runtime.markReflected();
        }
    }

    private static void triggerRetrace(ServerPlayer attacker, float baseDamageTaken) {
        if (!(baseDamageTaken > 0.0F) || !Float.isFinite(baseDamageTaken)) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(attacker);
        int level = boundedLevel(state, RETRACE_ID);
        long now = attacker.level().getGameTime();
        if (level <= 0 || now < state.cooldownReadyTick(RETRACE_ID)) {
            return;
        }
        RuntimeState runtime = RUNTIME.get(attacker.getUUID());
        if (runtime == null || runtime.positions.size() < 2) {
            return;
        }

        AugmentationPlayerStateService.replace(
                attacker,
                state.withCooldownReadyTick(
                        RETRACE_ID,
                        now + ReactiveCombatTuning.current().retraceCooldownTicks()
                )
        );
        float damage = (float) (baseDamageTaken
                * ReactiveCombatTuning.current().retraceDamageFraction(level));
        Set<Integer> hitIds = new HashSet<>();
        PositionSample newer = null;
        Iterator<PositionSample> iterator = runtime.positions.descendingIterator();
        while (iterator.hasNext()) {
            PositionSample sample = iterator.next();
            if (newer != null) {
                damagePathSegment(attacker, newer.position, sample.position, damage, hitIds);
            }
            newer = sample;
        }
    }

    private static void damagePathSegment(
            ServerPlayer owner,
            Vec3 start,
            Vec3 end,
            float damage,
            Set<Integer> hitIds
    ) {
        AABB search = new AABB(start, end).inflate(1.5D);
        for (LivingEntity candidate : owner.level().getEntitiesOfClass(
                LivingEntity.class,
                search,
                target -> CompanionAugmentService.isHostileTarget(owner, target)
                        && !hitIds.contains(target.getId())
        )) {
            double collisionRadius = Math.max(
                    0.5D,
                    (owner.getBbWidth() + candidate.getBbWidth()) * 0.5D
            );
            if (distanceToSegmentSquared(candidate.getBoundingBox().getCenter(), start, end)
                    > collisionRadius * collisionRadius) {
                continue;
            }
            hitIds.add(candidate.getId());
            applyDerivedDamage(owner, candidate, damage, CombatProvenance.PATH_ECHO);
        }
    }

    private static double distanceToSegmentSquared(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared <= 1.0E-9D) {
            return point.distanceToSqr(start);
        }
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(segment) / lengthSquared));
        return point.distanceToSqr(start.add(segment.scale(t)));
    }

    private static void tickRuntime(MinecraftServer server) {
        Iterator<Map.Entry<UUID, RuntimeState>> iterator = RUNTIME.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RuntimeState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isDeadOrDying()) {
                iterator.remove();
                continue;
            }
            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            int retraceLevel = boundedLevel(state, RETRACE_ID);
            int invulnerabilityLevel = boundedLevel(state, INVULNERABILITY_ID);
            if (retraceLevel <= 0 && invulnerabilityLevel <= 0) {
                iterator.remove();
                continue;
            }

            RuntimeState runtime = entry.getValue();
            long now = player.level().getGameTime();
            if (retraceLevel > 0) {
                runtime.positions.addLast(new PositionSample(now, player.position()));
                int historyTicks = ReactiveCombatTuning.current().retraceHistoryTicks();
                while (!runtime.positions.isEmpty()
                        && runtime.positions.peekFirst().tick < now - historyTicks) {
                    runtime.positions.removeFirst();
                }
            } else {
                runtime.positions.clear();
            }

            if (invulnerabilityLevel > 0) {
                if (runtime.nextInvulnerabilityTick <= 0L) {
                    runtime.nextInvulnerabilityTick = now
                            + ReactiveCombatTuning.current().invulnerabilityIntervalTicks();
                }
                if (now >= runtime.nextInvulnerabilityTick) {
                    runtime.invulnerableUntilTick = now
                            + ReactiveCombatTuning.current().invulnerabilityDurationTicks();
                    int interval = ReactiveCombatTuning.current().invulnerabilityIntervalTicks();
                    do {
                        runtime.nextInvulnerabilityTick += interval;
                    } while (runtime.nextInvulnerabilityTick <= now);
                }
            } else {
                runtime.nextInvulnerabilityTick = 0L;
                runtime.invulnerableUntilTick = 0L;
            }
        }
    }

    private static void reflectEffect(
            ServerPlayer owner,
            LivingEntity target,
            MobEffectInstance original
    ) {
        int duration = Math.max(
                1,
                (int) Math.floor(original.getDuration()
                        * ReactiveCombatTuning.current().statusReflectDurationFraction())
        );
        target.addEffect(copyWithDuration(original, duration), owner);
    }

    private static MobEffectInstance copyWithDuration(MobEffectInstance original, int duration) {
        return new MobEffectInstance(
                original.getEffect(),
                duration,
                original.getAmplifier(),
                original.isAmbient(),
                original.isVisible(),
                original.showIcon()
        );
    }

    private static boolean isFiniteHarmful(MobEffectInstance effect) {
        return effect != null
                && !effect.isInfiniteDuration()
                && effect.getDuration() > 0
                && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
    }

    private static boolean isDirectEnemySource(ServerPlayer victim, Entity source) {
        if (!(source instanceof LivingEntity living) || source == victim || victim.isAlliedTo(source)) {
            return false;
        }
        if (source instanceof ServerPlayer) {
            return true;
        }
        return CompanionAugmentService.isHostileTarget(victim, living);
    }

    private static boolean isBossSource(Entity source) {
        return source instanceof LivingEntity living
                && EntityCompatibilityProfileService.isBoss(living);
    }

    private static void applyDerivedDamage(
            ServerPlayer owner,
            LivingEntity target,
            float damage,
            CombatProvenance provenance
    ) {
        if (!(damage > 0.0F) || !Float.isFinite(damage) || target.isDeadOrDying()
                || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = owner.damageSources().playerAttack(owner);
        CombatProvenanceContext.runAs(
                provenance,
                () -> target.hurtServer(level, source, damage)
        );
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static final class RuntimeState {
        private final Deque<PositionSample> positions = new ArrayDeque<>();
        private long nextInvulnerabilityTick;
        private long invulnerableUntilTick;
    }

    private record PositionSample(long tick, Vec3 position) {
    }
}
