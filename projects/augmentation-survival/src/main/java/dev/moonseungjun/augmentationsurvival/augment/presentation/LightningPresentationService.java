package dev.moonseungjun.augmentationsurvival.augment.presentation;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Distinguishes the ordinary chain-lightning proc from the mythic Divine Punishment strike. */
public final class LightningPresentationService {
    private static final DustParticleOptions DIVINE_CRIMSON = new DustParticleOptions(0xE0182D, 1.45F);
    private static final DustParticleOptions DIVINE_DARK_CRIMSON = new DustParticleOptions(0x7A0014, 1.15F);

    private LightningPresentationService() {
    }

    public static void chainStrike(ServerPlayer attacker, Vec3 from, LivingEntity target) {
        if (attacker == null || target == null || !(attacker.level() instanceof ServerLevel level)) return;
        Vec3 targetCenter = target.position().add(0.0D, Math.max(0.5D, target.getBbHeight() * 0.55D), 0.0D);
        Vec3 start = from == null ? attacker.getEyePosition() : from;
        line(level, ParticleTypes.ELECTRIC_SPARK, start, targetCenter, 14);
        ring(level, ParticleTypes.ELECTRIC_SPARK, target.position().add(0.0D, 0.12D, 0.0D), 0.85D, 14);
        spawnVisualLightning(level, target.position());
    }

    public static void divineStrike(ServerLevel level, Vec3 position, int strikeIndex) {
        if (level == null || position == null) return;
        int stage = Math.floorMod(strikeIndex, 3);
        double offset = stage == 0 ? 0.0D : 0.62D;
        double angle = stage * (Math.PI * 2.0D / 3.0D) + 0.35D;
        Vec3 boltPosition = position.add(Math.cos(angle) * offset, 0.0D, Math.sin(angle) * offset);
        spawnVisualLightning(level, boltPosition);

        Vec3 core = position.add(0.0D, 1.0D, 0.0D);
        ring(level, DIVINE_CRIMSON, position.add(0.0D, 0.12D, 0.0D), 1.15D + stage * 0.20D, 24);
        ring(level, DIVINE_DARK_CRIMSON, position.add(0.0D, 0.18D, 0.0D), 1.70D + stage * 0.18D, 28);
        for (int branch = 0; branch < 6; branch++) {
            double branchAngle = branch * (Math.PI * 2.0D / 6.0D) + stage * 0.33D;
            Vec3 end = core.add(
                    Math.cos(branchAngle) * (1.30D + stage * 0.22D),
                    1.15D + ((branch & 1) == 0 ? 0.55D : -0.15D),
                    Math.sin(branchAngle) * (1.30D + stage * 0.22D)
            );
            jaggedLine(level, branch % 2 == 0 ? DIVINE_CRIMSON : DIVINE_DARK_CRIMSON, core, end, 7, branch * 0.41D);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                core.x, core.y, core.z,
                22, 0.75D, 1.15D, 0.75D, 0.16D);
        level.playSound(null, position.x, position.y, position.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.85F, 0.72F + stage * 0.08F);
    }

    private static void spawnVisualLightning(ServerLevel level, Vec3 position) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.setVisualOnly(true);
        bolt.snapTo(position);
        level.addFreshEntity(bolt);
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int points) {
        int count = Math.max(2, points);
        for (int i = 0; i <= count; i++) {
            double t = i / (double) count;
            Vec3 point = from.lerp(to, t);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void jaggedLine(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 from,
            Vec3 to,
            int points,
            double phase
    ) {
        int count = Math.max(3, points);
        Vec3 delta = to.subtract(from);
        Vec3 side = new Vec3(-delta.z, 0.0D, delta.x);
        if (side.lengthSqr() > 1.0E-6D) side = side.normalize();
        for (int i = 0; i <= count; i++) {
            double t = i / (double) count;
            double jag = Math.sin((i * 2.35D) + phase) * 0.18D * (1.0D - Math.abs(t * 2.0D - 1.0D));
            Vec3 point = from.lerp(to, t).add(side.scale(jag));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
        int count = Math.max(8, points);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            level.sendParticles(
                    particle,
                    center.x + Math.cos(angle) * radius,
                    center.y,
                    center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D
            );
        }
    }
}
