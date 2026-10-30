package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Converts shield-blocked damage into a server-authoritative forward cone shockwave. */
public final class ShieldCompressionService {
    private static final String RETURN_TO_SENDER_ID =
            AugmentationSurvival.MOD_ID + ":return_to_sender";
    private static final ResourceKey<DamageType> SHIELD_COMPRESSION_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "shield_compression")
    );
    private static final Map<ServerPlayer, Double> STORED_BLOCKED_DAMAGE = new WeakHashMap<>();

    private ShieldCompressionService() {
    }

    public static void onBlockedDamage(ServerPlayer player, DamageSource source, float blockedDamage) {
        if (!(blockedDamage > 0.0F) || !Float.isFinite(blockedDamage)) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state);
        if (level <= 0) {
            STORED_BLOCKED_DAMAGE.remove(player);
            return;
        }

        ReactiveCombatTuning.Snapshot tuning = ReactiveCombatTuning.current();
        double threshold = tuning.returnBlockedDamageThreshold();
        double stored = STORED_BLOCKED_DAMAGE.getOrDefault(player, 0.0D) + blockedDamage;
        int pulses = (int) Math.floor(stored / threshold);
        if (pulses <= 0) {
            STORED_BLOCKED_DAMAGE.put(player, stored);
            return;
        }
        STORED_BLOCKED_DAMAGE.put(player, stored - pulses * threshold);

        Entity dealer = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        Vec3 origin = player.getEyePosition().add(0.0D, -0.2D, 0.0D);
        Vec3 direction = dealer == null
                ? player.getLookAngle()
                : dealer.getBoundingBox().getCenter().subtract(origin);
        if (direction.lengthSqr() <= 1.0E-7D) {
            direction = player.getLookAngle();
        }
        if (direction.lengthSqr() <= 1.0E-7D) {
            return;
        }
        direction = direction.normalize();

        float waveDamage = (float) (pulses * threshold * tuning.returnDamageFraction(level));
        emitWave(player, origin, direction, waveDamage, tuning);
    }

    private static void emitWave(
            ServerPlayer player,
            Vec3 origin,
            Vec3 direction,
            float damage,
            ReactiveCombatTuning.Snapshot tuning
    ) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double range = tuning.returnWaveRange();
        double rangeSquared = range * range;
        double minimumDot = Math.cos(Math.toRadians(tuning.returnWaveHalfAngleDegrees()));
        DamageSource waveSource = player.damageSources().source(SHIELD_COMPRESSION_DAMAGE, null, player);

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                candidate -> OffensiveTargetContext.isValid(player, candidate)
        )) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
            double distanceSquared = toTarget.lengthSqr();
            if (distanceSquared <= 1.0E-7D || distanceSquared > rangeSquared) {
                continue;
            }
            if (direction.dot(toTarget.normalize()) < minimumDot) {
                continue;
            }
            CombatProvenanceContext.runAs(
                    CombatProvenance.DERIVED_AOE,
                    () -> target.hurtServer(level, waveSource, damage)
            );
        }

        emitConeParticles(level, origin, direction, range, tuning.returnWaveHalfAngleDegrees());
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                0.9F,
                0.72F
        );
    }

    private static void emitConeParticles(
            ServerLevel level,
            Vec3 origin,
            Vec3 direction,
            double range,
            double halfAngleDegrees
    ) {
        Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() <= 1.0E-6D) {
            right = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        right = right.normalize();
        Vec3 up = right.cross(direction).normalize();
        double tangent = Math.tan(Math.toRadians(halfAngleDegrees));
        int steps = 7;
        for (int step = 1; step <= steps; step++) {
            double distance = range * step / steps;
            double width = tangent * distance;
            Vec3 center = origin.add(direction.scale(distance));
            send(level, center);
            send(level, center.add(right.scale(width)));
            send(level, center.subtract(right.scale(width)));
            if ((step & 1) == 0) {
                send(level, center.add(up.scale(width * 0.55D)));
                send(level, center.subtract(up.scale(width * 0.55D)));
            }
        }
    }

    private static void send(ServerLevel level, Vec3 position) {
        level.sendParticles(
                ParticleTypes.SONIC_BOOM,
                position.x, position.y, position.z,
                1,
                0.0D, 0.0D, 0.0D,
                0.0D
        );
    }

    private static int boundedLevel(AugmentationPlayerState state) {
        int level = state.level(RETURN_TO_SENDER_ID);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(RETURN_TO_SENDER_ID).maxLevel());
    }
}
