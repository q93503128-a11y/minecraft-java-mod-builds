package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.world.entity.Entity;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Player-owned interpretations of publicly described World Bosses 2.0.2 signature abilities.
 * World Bosses is ARR; no private code, model or texture is imported here.
 * Detailed original mechanics are not available, so these timings are our own balance.
 *
 * Thunder Cage: visible electric zone with a warning and repeated crowd control.
 * Flame Prison: fixed-position, short-lived fire boundary that pulls escapees inward.
 * Primordial Chains -> Cataclysm Nova: staggered restraint followed by a telegraphed blast.
 */
public final class WorldBossAuthorityPatternService {
    private static final int MAX_FIELDS = 48;
    private static final int MAX_HOSTILES_PER_PULSE = 48;
    private static final List<Pattern> ACTIVE = new ArrayList<>();
    private static final Map<UUID, Long> SHADOW_VEIL = new HashMap<>();

    private enum Kind {
        THUNDER_CAGE, FLAME_PRISON, CATACLYSM_NOVA,
        SHADOW_SOUL_BOLT, DRAGON_FIRE_RAIN, STORM_STRIKE, WEAVER_THREADS
    }

    private static final class Pattern {
        private final UUID casterId;
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final Kind kind;
        private final long startTick;
        private final long expiryTick;
        private long nextFxTick;
        private long nextPulseTick;
        private boolean burstDone;
        private int pulseCount;
        private final UUID targetId;
        private final Vec3 launchPoint;

        private Pattern(ServerPlayer player, Vec3 center, Kind kind,
                        long durationTicks, long firstPulseDelay) {
            this(player, center, kind, durationTicks, firstPulseDelay, null);
        }

        private Pattern(ServerPlayer player, Vec3 center, Kind kind,
                        long durationTicks, long firstPulseDelay, LivingEntity target) {
            this.targetId = target == null ? null : target.getUUID();
            this.launchPoint = player.getEyePosition();
            this.casterId = player.getUUID();
            this.dimension = player.level().dimension();
            this.center = center;
            this.kind = kind;
            this.startTick = player.level().getGameTime();
            this.expiryTick = startTick + durationTicks;
            this.nextFxTick = startTick;
            this.nextPulseTick = startTick + firstPulseDelay;
        }
    }

    private WorldBossAuthorityPatternService() { }

    /** A grounded 4-block thunder prison rather than instant generic lightning-themed damage. */
    public static void castThunderCage(ServerPlayer player, LivingEntity target) {
        if (!validTarget(player, target)) return;
        add(new Pattern(player, target.position(), Kind.THUNDER_CAGE, 100L, 20L));
        player.level().playSound(null, target.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.75F, 1.30F);
    }

    /** Prison does not grief terrain or ignite a player's base. */
    public static void castFlamePrison(ServerPlayer player, LivingEntity target) {
        if (!validTarget(player, target)) return;
        add(new Pattern(player, target.position(), Kind.FLAME_PRISON, 100L, 10L));
        player.level().playSound(null, target.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.00F, 0.70F);
    }

    /** The public 2.0.2 ability names are known, but these numbers are ours, not source exact. */
    public static void castPrimordialNova(ServerPlayer player) {
        Vec3 origin = player.position();
        add(new Pattern(player, origin, Kind.CATACLYSM_NOVA, 32L, 26L));
        for (LivingEntity enemy : hostiles(player, origin, 9.0D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 3));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
            chainParticles(player.level(), origin, enemy.getBoundingBox().getCenter());
        }
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.00F, 0.55F);
    }

    /** Traveling soul bolt, shadow step, then darkness nova. */
    public static void castShadowKing(ServerPlayer player, LivingEntity target) {
        if (!validTarget(player, target)) return;
        add(new Pattern(player, target.position(), Kind.SHADOW_SOUL_BOLT, 62L, 18L, target));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT,
                SoundSource.PLAYERS, 0.8F, 0.8F);
    }

    /** Three separately telegraphed aerial fire strikes. */
    public static void castDragonFireRain(ServerPlayer player, LivingEntity target) {
        if (!validTarget(player, target)) return;
        add(new Pattern(player, target.position(), Kind.DRAGON_FIRE_RAIN, 70L, 23L, target));
        player.level().playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 0.8F, 0.65F);
    }

    /** Lightning is anchored at marked ground, so enemies can leave its radius. */
    public static void castStormStrike(ServerPlayer player) {
        int count = 0;
        for (LivingEntity enemy : hostiles(player, player.position(), 18.0D)) {
            if (count >= 3) break;
            enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
            add(new Pattern(player, enemy.position(), Kind.STORM_STRIKE, 38L, 26L, enemy));
            count++;
        }
    }

    /** Darkness-dependent veil; puppet strike follows the visible thread bind. */
    public static void castShadowWeaver(ServerPlayer player, LivingEntity target) {
        SHADOW_VEIL.put(player.getUUID(), player.level().getGameTime() + 120L);
        if (!validTarget(player, target)) return;
        add(new Pattern(player, target.position(), Kind.WEAVER_THREADS, 70L, 12L, target));
        player.level().playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.8F, 0.75F);
    }

    public static float modifyShadowVeilDamage(ServerPlayer player, float incoming) {
        if (incoming <= 0.0F || SHADOW_VEIL.getOrDefault(player.getUUID(), 0L)
                <= player.level().getGameTime()) return incoming;
        return player.level().getMaxLocalRawBrightness(player.blockPosition()) >= 12
                ? incoming : incoming * 0.45F;
    }

    private static void add(Pattern pattern) {
        // Cap globally and per caster; never evict another player's active technique to spawn ours.
        if (ACTIVE.size() >= MAX_FIELDS) return;
        int own = 0;
        for (Pattern existing : ACTIVE) if (existing.casterId.equals(pattern.casterId)) own++;
        if (own >= 4) return;
        ACTIVE.add(pattern);
    }

    public static void reset() {
        ACTIVE.clear();
        SHADOW_VEIL.clear();
    }

    /** Called exactly once from the main authority server tick, no duplicate tick listener. */
    public static void tick(MinecraftServer server) {
        Iterator<Pattern> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Pattern pattern = it.next();
            ServerPlayer caster = server.getPlayerList().getPlayer(pattern.casterId);
            if (caster == null || caster.isDeadOrDying()
                    || !caster.level().dimension().equals(pattern.dimension)
                    || caster.level().getGameTime() >= pattern.expiryTick) {
                it.remove();
                continue;
            }
            ServerLevel level = caster.level();
            long now = level.getGameTime();
            if (now >= pattern.nextFxTick) {
                pattern.nextFxTick = now + 8L;
                renderPattern(level, pattern, now);
            }
            if (now < pattern.nextPulseTick) continue;
            if (isRealmPattern(pattern.kind)) {
                if (runRealmPattern(caster, pattern, it, now)) continue;
            }
            if (pattern.kind == Kind.CATACLYSM_NOVA) {
                if (!pattern.burstDone) {
                    pattern.burstDone = true;
                    novaImpact(caster, pattern.center);
                    it.remove();
                }
                continue;
            }
            pattern.nextPulseTick = now + (pattern.kind == Kind.THUNDER_CAGE ? 14L : 18L);
            if (pattern.kind == Kind.THUNDER_CAGE) thunderPulse(caster, pattern.center);
            else flamePulse(caster, pattern.center);
        }
        SHADOW_VEIL.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            return player == null || player.isDeadOrDying()
                    || player.level().getGameTime() >= entry.getValue();
        });
    }

    private static boolean isRealmPattern(Kind kind) {
        return kind == Kind.SHADOW_SOUL_BOLT || kind == Kind.DRAGON_FIRE_RAIN
                || kind == Kind.STORM_STRIKE || kind == Kind.WEAVER_THREADS;
    }

    /** Each scheduled hit passes through the existing authority damage boundary. */
    private static boolean runRealmPattern(ServerPlayer caster, Pattern pattern,
                                           Iterator<Pattern> iterator, long now) {
        ServerLevel level = caster.level();
        switch (pattern.kind) {
            case SHADOW_SOUL_BOLT -> {
                if (pattern.pulseCount++ == 0) {
                    LivingEntity enemy = liveTarget(caster, pattern, 24.0D);
                    if (enemy != null) {
                        AuthorityTheftService.hurt(caster, enemy, 8.0F);
                        AuthorityTheftService.blinkBehind(caster, enemy, 4.0D);
                        enemy.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
                    }
                    pattern.nextPulseTick = now + 22L;
                } else {
                    for (LivingEntity enemy : hostiles(caster, pattern.center, 7.0D)) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
                        AuthorityTheftService.hurt(caster, enemy, 4.0F);
                    }
                    iterator.remove();
                }
            }
            case DRAGON_FIRE_RAIN -> {
                int stage = ++pattern.pulseCount;
                Vec3 center = pattern.center.add(stage == 1 ? -1.2D : stage == 3 ? 1.2D : 0.0D, 0, 0);
                for (LivingEntity enemy : hostiles(caster, center, 2.7D)) {
                    enemy.igniteForSeconds(5.0F);
                    AuthorityTheftService.hurt(caster, enemy, 7.0F);
                    enemy.push(0.0D, 0.4D, 0.0D);
                }
                level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.4D,
                        center.z, 12, 0.8D, 0.4D, 0.8D, 0.02D);
                level.playSound(null, BlockPos.containing(center),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.65F, 1.2F);
                if (stage >= 3) iterator.remove();
                else pattern.nextPulseTick = now + 14L;
            }
            case STORM_STRIKE -> {
                for (LivingEntity enemy : hostiles(caster, pattern.center, 3.0D))
                    AuthorityTheftService.hurt(caster, enemy, 10.0F);
                for (int i = 0; i < 8; i++) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            pattern.center.x, pattern.center.y + 0.85D * i, pattern.center.z,
                            2, 0.2D, 0, 0.2D, 0);
                }
                level.playSound(null, BlockPos.containing(pattern.center),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.2F);
                iterator.remove();
            }
            case WEAVER_THREADS -> {
                LivingEntity enemy = liveTarget(caster, pattern, 22.0D);
                if (enemy != null) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 55, 4));
                    enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 55, 1));
                    chainParticles(level, caster.getEyePosition(), enemy.getBoundingBox().getCenter());
                }
                if (++pattern.pulseCount >= 4) {
                    for (LivingEntity target : hostiles(caster, pattern.center, 5.0D)) {
                        target.igniteForSeconds(4.0F);
                        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0));
                        AuthorityTheftService.hurt(caster, target, 9.0F);
                    }
                    iterator.remove();
                } else pattern.nextPulseTick = now + 12L;
            }
            default -> { return false; }
        }
        return true;
    }

    private static LivingEntity liveTarget(ServerPlayer player, Pattern pattern, double radius) {
        if (pattern.targetId == null) return null;
        Entity entity = player.level().getEntity(pattern.targetId);
        return entity instanceof LivingEntity enemy && !enemy.isDeadOrDying()
                && enemy.distanceToSqr(player) <= radius * radius
                && CompanionAugmentService.isHostileTarget(player, enemy) ? enemy : null;
    }

    private static boolean validTarget(ServerPlayer player, LivingEntity target) {
        return target != null && !target.isDeadOrDying()
                && target.level() == player.level()
                && CompanionAugmentService.isHostileTarget(player, target);
    }

    private static List<LivingEntity> hostiles(ServerPlayer caster, Vec3 center, double radius) {
        double squared = radius * radius;
        List<LivingEntity> found = caster.level().getEntitiesOfClass(LivingEntity.class,
                AABB.ofSize(center, radius * 2.0D, radius * 2.0D + 3.0D, radius * 2.0D),
                e -> CompanionAugmentService.isHostileTarget(caster, e)
                        && e.position().distanceToSqr(center) <= squared);
        return found.size() <= MAX_HOSTILES_PER_PULSE
                ? found : List.copyOf(found.subList(0, MAX_HOSTILES_PER_PULSE));
    }

    private static void thunderPulse(ServerPlayer caster, Vec3 center) {
        for (LivingEntity enemy : hostiles(caster, center, 4.0D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 42, 2));
            enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0));
            AuthorityTheftService.hurt(caster, enemy, 4.0F);
        }
        caster.level().playSound(null, BlockPos.containing(center),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.38F, 1.55F);
    }

    private static void flamePulse(ServerPlayer caster, Vec3 center) {
        for (LivingEntity enemy : hostiles(caster, center, 3.7D)) {
            enemy.igniteForSeconds(3.0F);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 44, 2));
            Vec3 away = enemy.position().subtract(center);
            // Escapees near the fiery boundary are dragged inward, but not teleported or frozen.
            if (away.horizontalDistanceSqr() > 2.7D * 2.7D) {
                Vec3 horizontal = new Vec3(away.x, 0.0D, away.z).normalize();
                enemy.push(-horizontal.x * 0.35D, 0.04D, -horizontal.z * 0.35D);
            }
            AuthorityTheftService.hurt(caster, enemy, 3.0F);
        }
        caster.level().playSound(null, BlockPos.containing(center),
                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.40F, 1.0F);
    }

    private static void novaImpact(ServerPlayer caster, Vec3 center) {
        for (LivingEntity enemy : hostiles(caster, center, 9.0D)) {
            AuthorityTheftService.hurt(caster, enemy, 11.0F);
            Vec3 away = enemy.position().subtract(center);
            if (away.lengthSqr() > 1.0E-6D) {
                Vec3 force = away.normalize().scale(1.1D);
                enemy.push(force.x, Math.max(0.3D, force.y), force.z);
            }
        }
        ServerLevel level = caster.level();
        level.sendParticles(ParticleTypes.EXPLOSION,
                center.x, center.y + 0.7D, center.z,
                24, 3.0D, 0.5D, 3.0D, 0.08D);
        drawRing(level, center, 9.0D, 0.3D, ParticleTypes.END_ROD, 48);
        level.playSound(null, BlockPos.containing(center),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.25F, 0.60F);
    }

    private static void renderPattern(ServerLevel level, Pattern pattern, long now) {
        double elapsed = Math.max(0.0D, now - pattern.startTick);
        switch (pattern.kind) {
            case THUNDER_CAGE -> {
                for (int y = 0; y < 3; y++) {
                    drawRing(level, pattern.center, 4.0D, 0.4D + y * 1.15D,
                            ParticleTypes.ELECTRIC_SPARK, 16);
                }
                level.sendParticles(ParticleTypes.END_ROD,
                        pattern.center.x, pattern.center.y + 1.4D, pattern.center.z,
                        6, 0.5D, 0.8D, 0.5D, 0.0D);
            }
            case FLAME_PRISON -> {
                drawRing(level, pattern.center, 3.7D, 0.2D, ParticleTypes.FLAME, 24);
                drawRing(level, pattern.center, 3.7D, 1.5D, ParticleTypes.SOUL_FIRE_FLAME, 16);
                drawRing(level, pattern.center, 3.7D, 2.8D, ParticleTypes.FLAME, 16);
            }
            case SHADOW_SOUL_BOLT -> {
                if (pattern.pulseCount == 0) {
                    Vec3 at = pattern.launchPoint.add(pattern.center.subtract(pattern.launchPoint)
                            .scale(Math.min(1.0D, elapsed / 18.0D)));
                    level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.2D, at.z,
                            5, 0.15D, 0.15D, 0.15D, 0.01D);
                } else drawRing(level, pattern.center, 7.0D, 0.2D,
                        ParticleTypes.SOUL, 24);
            }
            case DRAGON_FIRE_RAIN -> {
                int stage = pattern.pulseCount + 1;
                Vec3 at = pattern.center.add(stage == 1 ? -1.2D : stage == 3 ? 1.2D : 0, 0, 0);
                drawRing(level, at, 2.7D, 0.18D, ParticleTypes.FLAME, 24);
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 5.0D, at.z,
                        10, 0.1D, 2.0D, 0.1D, 0.0D);
            }
            case STORM_STRIKE -> {
                drawRing(level, pattern.center, 3.0D, 0.1D, ParticleTypes.ELECTRIC_SPARK, 24);
                level.sendParticles(ParticleTypes.END_ROD, pattern.center.x,
                        pattern.center.y + 4.0D, pattern.center.z, 10, 0.1D, 2.0D, 0.1D, 0.0D);
            }
            case WEAVER_THREADS -> {
                drawRing(level, pattern.center, 5.0D, 0.12D, ParticleTypes.END_ROD, 24);
                level.sendParticles(ParticleTypes.PORTAL, pattern.center.x, pattern.center.y + 1.0D,
                        pattern.center.z, 8, 0.6D, 0.6D, 0.6D, 0.0D);
            }
            case CATACLYSM_NOVA -> {
                double progress = Math.min(1.0D, elapsed / 26.0D);
                double radius = 2.0D + progress * 7.0D;
                drawRing(level, pattern.center, radius, 0.18D, ParticleTypes.END_ROD, 32);
                drawRing(level, pattern.center, 9.0D, 0.12D, ParticleTypes.PORTAL, 18);
            }
        }
    }

    private static void chainParticles(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 line = to.subtract(from);
        for (int i = 1; i <= 8; i++) {
            Vec3 p = from.add(line.scale(i / 9.0D));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.0D, p.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void drawRing(ServerLevel level, Vec3 center, double radius, double height,
                                 ParticleOptions particle, int steps) {
        for (int i = 0; i < steps; i++) {
            double angle = (Math.PI * 2.0D * i) / steps;
            level.sendParticles(particle,
                    center.x + radius * Math.cos(angle), center.y + height,
                    center.z + radius * Math.sin(angle),
                    1, 0.0D, 0.01D, 0.0D, 0.0D);
        }
    }
}
