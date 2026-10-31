package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Delayed, server-owned second hit for Echo Strike with a neon afterimage presentation. */
public final class EchoStrikeAfterimageService {
    private static final String ECHO_STRIKE_ID = AugmentationSurvival.MOD_ID + ":echo_strike";
    private static final int AFTERIMAGE_DELAY_TICKS = 10;
    private static final int COUNTER_STALE_TICKS = 20 * 10;
    private static final DustParticleOptions CYAN = new DustParticleOptions(0x39E7FF, 1.05F);
    private static final DustParticleOptions MAGENTA = new DustParticleOptions(0xF04BFF, 0.95F);
    private static final DustParticleOptions PALE = new DustParticleOptions(0xC8F7FF, 0.78F);
    private static final Map<HitKey, HitCounter> COUNTERS = new HashMap<>();
    private static final List<PendingEcho> PENDING = new ArrayList<>();
    private static boolean initialized;

    private EchoStrikeAfterimageService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(EchoStrikeAfterimageService::tick);
        ServerPlayerEvents.JOIN.register(EchoStrikeAfterimageService::clearPlayer);
        ServerPlayerEvents.LEAVE.register(EchoStrikeAfterimageService::clearPlayer);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> clearPlayer(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            COUNTERS.clear();
            PENDING.clear();
        });
    }

    /** Called only from the existing fully-charged ROOT_MELEE branch. */
    public static void onChargedMelee(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            float baseDamageTaken,
            long now
    ) {
        int level = Math.max(0, Math.min(3, state.level(ECHO_STRIKE_ID)));
        if (level <= 0 || target == null || target.isDeadOrDying()
                || !(baseDamageTaken > 0.0F) || !Float.isFinite(baseDamageTaken)) {
            return;
        }

        HitKey key = new HitKey(attacker.getUUID(), target.getUUID());
        HitCounter counter = COUNTERS.computeIfAbsent(key, ignored -> new HitCounter());
        if (counter.lastTick > 0L && now - counter.lastTick > COUNTER_STALE_TICKS) {
            counter.count = 0;
        }
        counter.count++;
        counter.lastTick = now;

        int trigger = ExpansionTuning.current().echoStrikeTriggerHits(level);
        if (counter.count < trigger) return;
        counter.count = 0;

        float echoDamage = (float) (baseDamageTaken * ExpansionTuning.current().echoStrikeDamageFraction(level));
        Vec3 attackerOrigin = attacker.getBoundingBox().getCenter();
        Vec3 targetOrigin = target.getBoundingBox().getCenter();
        PENDING.add(new PendingEcho(
                attacker.getUUID(),
                target.getUUID(),
                (ServerLevel) attacker.level(),
                now + AFTERIMAGE_DELAY_TICKS,
                echoDamage,
                attackerOrigin,
                targetOrigin
        ));
        cueDeparture((ServerLevel) attacker.level(), attackerOrigin, targetOrigin);
    }

    private static void tick(MinecraftServer server) {
        long overworldNow = server.overworld().getGameTime();
        COUNTERS.entrySet().removeIf(entry -> overworldNow - entry.getValue().lastTick > COUNTER_STALE_TICKS);

        Iterator<PendingEcho> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingEcho pending = iterator.next();
            long now = pending.level().getGameTime();
            if (now < pending.dueTick()) continue;
            iterator.remove();

            ServerPlayer attacker = server.getPlayerList().getPlayer(pending.attackerId());
            Entity found = pending.level().getEntity(pending.targetId());
            if (attacker == null || attacker.isDeadOrDying() || attacker.level() != pending.level()
                    || !(found instanceof LivingEntity target) || target.isDeadOrDying()) {
                continue;
            }

            boolean applied = CombatProvenanceContext.callAs(
                    CombatProvenance.ECHO,
                    () -> target.hurtServer(
                            pending.level(),
                            attacker.damageSources().playerAttack(attacker),
                            pending.damage()
                    )
            );
            if (applied) {
                cueImpact(pending.level(), pending.attackerOrigin(), pending.targetOrigin(), target.getBoundingBox().getCenter());
            }
        }
    }

    private static void cueDeparture(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from);
        Vec3 side = perpendicular(direction).scale(0.16D);
        line(level, PALE, from.add(side), to.add(side), 8);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                from.x, from.y, from.z,
                5, 0.18D, 0.24D, 0.18D, 0.025D);
    }

    private static void cueImpact(ServerLevel level, Vec3 from, Vec3 oldTarget, Vec3 currentTarget) {
        Vec3 start = from.lerp(oldTarget, 0.18D);
        Vec3 direction = currentTarget.subtract(start);
        Vec3 side = perpendicular(direction).scale(0.22D);
        line(level, CYAN, start.add(side), currentTarget.add(side), 15);
        line(level, MAGENTA, start.subtract(side), currentTarget.subtract(side), 15);
        line(level, PALE, start, currentTarget, 11);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                currentTarget.x, currentTarget.y, currentTarget.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                currentTarget.x, currentTarget.y, currentTarget.z,
                13, 0.34D, 0.42D, 0.34D, 0.08D);
        level.playSound(null,
                currentTarget.x, currentTarget.y, currentTarget.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.72F, 1.48F);
        level.playSound(null,
                currentTarget.x, currentTarget.y, currentTarget.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.36F, 1.78F);
    }

    private static Vec3 perpendicular(Vec3 direction) {
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        return side.lengthSqr() < 1.0E-8D ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int points) {
        int count = Math.max(2, points);
        for (int i = 0; i <= count; i++) {
            double t = i / (double) count;
            Vec3 point = from.lerp(to, t);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void clearPlayer(ServerPlayer player) {
        UUID id = player.getUUID();
        COUNTERS.entrySet().removeIf(entry -> entry.getKey().attackerId().equals(id));
        PENDING.removeIf(entry -> entry.attackerId().equals(id));
    }

    private record HitKey(UUID attackerId, UUID targetId) {
    }

    private static final class HitCounter {
        private int count;
        private long lastTick;
    }

    private record PendingEcho(
            UUID attackerId,
            UUID targetId,
            ServerLevel level,
            long dueTick,
            float damage,
            Vec3 attackerOrigin,
            Vec3 targetOrigin
    ) {
    }
}
