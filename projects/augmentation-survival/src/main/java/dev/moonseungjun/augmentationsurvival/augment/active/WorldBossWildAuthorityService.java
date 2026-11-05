package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import java.util.ArrayList;
import java.util.Iterator;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Independent player-use adaptations of World Bosses 2.0.2 public signatures. Proprietary logic is not bundled. */
public final class WorldBossWildAuthorityService {
    private static final int MAX_FIELDS = 40;
    private static final int MAX_TARGETS = 32;
    private static final List<Field> FIELDS = new ArrayList<>();
    private enum Kind { SPORE_ROOTS, SANDSTORM_BURROW, FEAR_SOUL_DRAIN, VOID_RIFT }
    private static final class Field {
        final UUID caster, target;
        final ResourceKey<Level> dimension;
        final Kind kind;
        final Vec3 center;
        final long start, end;
        long nextPulse, nextFx;
        boolean finished;
        Field(ServerPlayer player, Kind kind, Vec3 center, LivingEntity target, long duration, long firstPulse) {
            caster = player.getUUID();
            dimension = player.level().dimension();
            this.kind = kind;
            this.center = center;
            this.target = target == null ? null : target.getUUID();
            start = player.level().getGameTime();
            end = start + duration;
            nextPulse = start + firstPulse;
            nextFx = start;
        }
    }
    private WorldBossWildAuthorityService() { }
    public static void castSylvaron(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
        LivingEntity target = nearest(player, 16.0D);
        if (target != null) {
            add(new Field(player, Kind.SPORE_ROOTS, target.position(), target, 80L, 10L));
            player.level().playSound(null, target.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7F, 0.8F);
        }
    }
    public static void castNekhraz(ServerPlayer player) {
        LivingEntity target = nearest(player, 18.0D);
        if (target != null) {
            add(new Field(player, Kind.SANDSTORM_BURROW, target.position(), target, 70L, 10L));
            player.level().playSound(null, target.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }
    public static void castMorvath(ServerPlayer player) {
        add(new Field(player, Kind.FEAR_SOUL_DRAIN, player.position(), nearest(player, 16.0D), 88L, 8L));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.9F, 0.6F);
    }
    public static void castElyndra(ServerPlayer player) {
        LivingEntity target = nearest(player, 18.0D);
        if (target != null) {
            add(new Field(player, Kind.VOID_RIFT, target.position(), target, 65L, 8L));
            player.level().playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }
    private static void add(Field f) {
        if (FIELDS.size() >= MAX_FIELDS) return;
        int own = 0;
        for (Field a : FIELDS) if (a.caster.equals(f.caster)) own++;
        if (own < 4) FIELDS.add(f);
    }
    public static void reset() { FIELDS.clear(); }
    public static void tick(MinecraftServer server) {
        Iterator<Field> iter = FIELDS.iterator();
        while (iter.hasNext()) {
            Field f = iter.next();
            ServerPlayer player = server.getPlayerList().getPlayer(f.caster);
            if (player == null || player.isDeadOrDying() || !player.level().dimension().equals(f.dimension)
                    || player.level().getGameTime() >= f.end) {
                iter.remove(); continue;
            }
            long now = player.level().getGameTime();
            if (now >= f.nextFx) { f.nextFx = now + 10L; visual(player.level(), f); }
            if (now < f.nextPulse) continue;
            f.nextPulse = now + 14L;
            switch (f.kind) {
                case SPORE_ROOTS -> sporeRoots(player, f);
                case SANDSTORM_BURROW -> sandstormBurrow(player, f, now);
                case FEAR_SOUL_DRAIN -> fearDrain(player, f, now);
                case VOID_RIFT -> voidRift(player, f, now);
            }
        }
    }
    private static void sporeRoots(ServerPlayer player, Field f) {
        for (LivingEntity enemy : hostiles(player, f.center, 5.0D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 58, 3));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 58, 0));
            enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 58, 0));
            AuthorityTheftService.hurt(player, enemy, 2.0F);
        }
    }
    private static void sandstormBurrow(ServerPlayer player, Field f, long now) {
        for (LivingEntity enemy : hostiles(player, f.center, 5.5D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 55, 0));
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 55, 1));
        }
        if (f.finished || now < f.start + 25L) return;
        f.finished = true;
        LivingEntity target = liveTarget(player, f.target, 18.0D);
        if (target != null && AuthorityTheftService.blinkBehind(player, target, 2.5D)) {
            AuthorityTheftService.hurt(player, target, 9.0F);
            target.push(0.0D, 0.6D, 0.0D);
            player.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
    }
    private static void fearDrain(ServerPlayer player, Field f, long now) {
        if (!f.finished) {
            f.finished = true;
            for (LivingEntity enemy : hostiles(player, f.center, 8.0D)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 90, 0));
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 1));
                Vec3 away = enemy.position().subtract(f.center);
                if (away.lengthSqr() > 1.0E-6D) {
                    Vec3 force = away.normalize().scale(0.7D);
                    enemy.push(force.x, 0.22D, force.z);
                }
            }
        }
        LivingEntity target = liveTarget(player, f.target, 16.0D);
        if (target != null && now >= f.start + 20L) {
            AuthorityTheftService.drainLife(player, target, 4.0F, 0.5F);
            trace(player.level(), target.getBoundingBox().getCenter(), player.getEyePosition(), ParticleTypes.SOUL, 9);
        }
    }
    private static void voidRift(ServerPlayer player, Field f, long now) {
        if (!f.finished && now >= f.start + 40L) {
            f.finished = true;
            for (LivingEntity enemy : hostiles(player, f.center, 6.5D)) {
                AuthorityTheftService.hurt(player, enemy, 9.0F);
                enemy.push(0.0D, 0.65D, 0.0D);
            }
            player.level().playSound(null, BlockPos.containing(f.center), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.1F, 0.45F);
            ring(player.level(), f.center, 6.5D, 0.4D, ParticleTypes.END_ROD, 32);
            return;
        }
        if (f.finished) return;
        for (LivingEntity enemy : hostiles(player, f.center, 6.5D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 52, 2));
            Vec3 difference = f.center.subtract(enemy.position());
            Vec3 horizontal = new Vec3(difference.x, 0.0D, difference.z);
            if (horizontal.lengthSqr() > 0.25D) {
                Vec3 pull = horizontal.normalize().scale(0.55D);
                enemy.push(pull.x, 0.10D, pull.z);
            }
        }
    }
    private static void visual(ServerLevel level, Field f) {
        switch (f.kind) {
            case SPORE_ROOTS -> {
                ring(level, f.center, 5.0D, 0.2D, ParticleTypes.HAPPY_VILLAGER, 24);
                ring(level, f.center, 4.7D, 0.95D, ParticleTypes.WITCH, 16);
            }
            case SANDSTORM_BURROW -> {
                ring(level, f.center, 5.5D, 0.28D, ParticleTypes.CAMPFIRE_COSY_SMOKE, 24);
                ring(level, f.center, 4.8D, 1.7D, ParticleTypes.SMOKE, 16);
            }
            case FEAR_SOUL_DRAIN -> ring(level, f.center, 8.0D, 0.18D, ParticleTypes.SOUL, 28);
            case VOID_RIFT -> {
                ring(level, f.center, f.finished ? 3.5D : 6.5D, 0.18D, ParticleTypes.PORTAL, 30);
                ring(level, f.center, 2.0D, 1.35D, ParticleTypes.REVERSE_PORTAL, 16);
            }
        }
    }
    private static LivingEntity nearest(ServerPlayer player, double r) {
        LivingEntity best = null;
        double dist = r*r;
        for (LivingEntity candidate : hostiles(player, player.position(), r)) {
            double ds = candidate.distanceToSqr(player);
            if (ds < dist) { best = candidate; dist = ds; }
        }
        return best;
    }
    private static LivingEntity liveTarget(ServerPlayer player, UUID uuid, double radius) {
        if (uuid == null) return null;
        Entity obj = player.level().getEntity(uuid);
        return obj instanceof LivingEntity target
                && !target.isDeadOrDying() && target.distanceToSqr(player) <= radius * radius
                && CompanionAugmentService.isHostileTarget(player, target) ? target : null;
    }
    private static List<LivingEntity> hostiles(ServerPlayer caster, Vec3 center, double radius) {
        List<LivingEntity> entities = caster.level().getEntitiesOfClass(LivingEntity.class,
                AABB.ofSize(center, 2*radius, 2*radius+3.0D, 2*radius),
                e -> CompanionAugmentService.isHostileTarget(caster, e)
                    && e.position().distanceToSqr(center) <= radius*radius);
        return entities.size() <= MAX_TARGETS ? entities : List.copyOf(entities.subList(0, MAX_TARGETS));
    }
    private static void ring(ServerLevel level, Vec3 center, double radius, double height,
                             ParticleOptions particle, int points) {
        for (int i=0;i<points;i++) {
            double angle = i * Math.PI * 2.0D / points;
            level.sendParticles(particle, center.x + Math.cos(angle)*radius, center.y + height,
                    center.z + Math.sin(angle)*radius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
    private static void trace(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, int points) {
        Vec3 direction = to.subtract(from);
        for (int i=0;i<points;i++) {
            Vec3 at = from.add(direction.scale((i+1.0D)/(points+1.0D)));
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
