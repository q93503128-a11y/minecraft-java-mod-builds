package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CloneCombatSnapshot;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CloneEntityTypes;
import dev.moonseungjun.augmentationsurvival.augment.companion.CombatCloneEntity;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Dedicated Mythic authority channel. Supported boss identities are permanently stolen on the
 * first root-direct kill, then every stolen package is released in one server-authoritative burst.
 */
public final class AuthorityTheftService {
    public static final String AUTHORITY_THEFT_ID = id("authority_theft");
    private static final String COOLDOWN_KEY = AUTHORITY_THEFT_ID + "#release";
    private static final int COOLDOWN_TICKS = 120 * 20;

    private static final Set<String> CHRONO_DAWN_PACKAGES = Set.of(
            "chronodawn_time_guardian",
            "chronodawn_chronos_warden",
            "chronodawn_clockwork_colossus",
            "chronodawn_temporal_phantom",
            "chronodawn_entropy_keeper",
            "chronodawn_time_tyrant"
    );

    private AuthorityTheftService() { }

    public static void initialize() {
        AugmentCatalog.require(AUTHORITY_THEFT_ID);
        CloneEntityTypes.initialize();
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(AuthorityTheftService::afterKill);
    }

    private static void afterKill(ServerLevel level, net.minecraft.world.entity.Entity killer, LivingEntity killed, net.minecraft.world.damagesource.DamageSource source) {
        if (!(killer instanceof ServerPlayer player)) return;
        if (AugmentationPlayerStateService.state(player).level(AUTHORITY_THEFT_ID) <= 0) return;
        if (!CombatProvenanceContext.resolve(source).isRootDirect()) return;
        EntityCompatibilityProfileService.resolve(killed).ifPresent(profile -> {
            if (!profile.boss() || !supports(profile.id())) return;
            AugmentationPlayerState before = AugmentationPlayerStateService.state(player);
            AugmentationPlayerState next = before.withStolenAuthority(profile.id());
            if (next.equals(before)) return;
            AugmentationPlayerStateService.replace(player, next);
            player.sendSystemMessage(Component.translatable(
                    "message.augmentation_survival.authority_theft.stolen",
                    killed.getDisplayName()
            ));
        });
    }

    public static boolean tryRelease(ServerPlayer player) {
        if (player.isDeadOrDying()) return false;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (state.level(AUTHORITY_THEFT_ID) <= 0 || state.stolenAuthorityBossIds().isEmpty()) return false;
        long now = player.level().getGameTime();
        if (state.cooldownReadyTick(COOLDOWN_KEY) > now) return false;

        int released = 0;
        for (String bossId : state.stolenAuthorityBossIds()) {
            if (releasePackage(player, bossId)) released++;
        }
        if (released <= 0) return false;

        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        AugmentationPlayerStateService.replace(player, latest.withCooldownReadyTick(COOLDOWN_KEY, now + COOLDOWN_TICKS));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.65F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(),
                    90, 0.8D, 1.1D, 0.8D, 0.15D);
        }
        return true;
    }

    public static boolean supports(String bossId) {
        return CHRONO_DAWN_PACKAGES.contains(bossId);
    }

    private static boolean releasePackage(ServerPlayer player, String bossId) {
        return switch (bossId) {
            case "chronodawn_time_guardian" -> { releaseTimeGuardian(player); yield true; }
            case "chronodawn_chronos_warden" -> { releaseChronosWarden(player); yield true; }
            case "chronodawn_clockwork_colossus" -> { releaseClockworkColossus(player); yield true; }
            case "chronodawn_temporal_phantom" -> { releaseTemporalPhantom(player); yield true; }
            case "chronodawn_entropy_keeper" -> { releaseEntropyKeeper(player); yield true; }
            case "chronodawn_time_tyrant" -> { releaseTimeTyrant(player); yield true; }
            default -> false;
        };
    }

    /** Time Guardian: blink pressure + temporal distortion AoE + slowed primary target. */
    private static void releaseTimeGuardian(ServerPlayer player) {
        LivingEntity target = nearestHostile(player, 20.0D);
        if (target != null) {
            blinkBehind(player, target, 3.0D);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0));
        }
        float damage = scaledDamage(player, 0.75D, 2.0D);
        for (LivingEntity enemy : hostiles(player, 4.0D)) {
            hurt(player, enemy, damage);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
            enemy.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 80, 0));
        }
        particles(player, ParticleTypes.WITCH, 48, 4.0D);
    }

    /** Chronos Warden: five-second stone stance and the source boss's 4-block ground slam. */
    private static void releaseChronosWarden(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 3));
        for (LivingEntity enemy : hostiles(player, 4.0D)) {
            hurt(player, enemy, 2.0F);
            Vec3 away = enemy.position().subtract(player.position());
            if (away.horizontalDistanceSqr() > 1.0E-5D) {
                Vec3 normalized = new Vec3(away.x, 0.0D, away.z).normalize();
                enemy.push(normalized.x * 1.15D, 0.45D, normalized.z * 1.15D);
            }
        }
        LivingEntity primary = nearestHostile(player, 5.0D);
        if (primary != null) primary.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 1));
        particles(player, ParticleTypes.SOUL, 56, 4.0D);
    }

    /** Clockwork Colossus: overcharge, repair protocol, gear strike and linear ground shockwave. */
    private static void releaseClockworkColossus(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 1));
        player.heal(player.getMaxHealth() * 0.15F);
        LivingEntity primary = nearestHostile(player, 16.0D);
        if (primary != null) hurt(player, primary, 8.0F);

        Vec3 look = flatLook(player);
        for (LivingEntity enemy : hostiles(player, 11.0D)) {
            Vec3 offset = enemy.position().subtract(player.position());
            double forward = offset.dot(look);
            if (forward < 0.0D || forward > 10.0D) continue;
            double lateral = Math.abs(offset.x * -look.z + offset.z * look.x);
            if (lateral > 1.5D) continue;
            hurt(player, enemy, 6.0F);
            enemy.push(look.x * 0.8D, 0.25D, look.z * 0.8D);
        }
        lineParticles(player, look, ParticleTypes.ELECTRIC_SPARK, 10);
    }

    /** Temporal Phantom: phase shift, warp debuff, two 15-second combat echoes and blink strike. */
    private static void releaseTemporalPhantom(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 4));
        LivingEntity target = nearestHostile(player, 20.0D);
        if (target != null) {
            hurt(player, target, scaledDamage(player, 0.8D, 4.0D));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 0));
            if (blinkBehind(player, target, 6.0D)) hurt(player, target, 8.0F);
        }
        spawnPhantomEchoes(player);
        particles(player, ParticleTypes.PORTAL, 60, 3.0D);
    }

    /** Entropy Keeper: decay aura, temporal rot and the source boss's 6-block entropy burst. */
    private static void releaseEntropyKeeper(ServerPlayer player) {
        LivingEntity primary = nearestHostile(player, 16.0D);
        if (primary != null) {
            for (LivingEntity enemy : player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    primary.getBoundingBox().inflate(2.0D),
                    candidate -> CompanionAugmentService.isHostileTarget(player, candidate))) {
                enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
        }
        for (LivingEntity enemy : hostiles(player, 6.0D)) {
            if (player.distanceToSqr(enemy) <= 16.0D) {
                enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            }
            hurt(player, enemy, 10.0F);
            enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
        }
        particles(player, ParticleTypes.SCULK_SOUL, 70, 6.0D);
    }

    /** Time Tyrant: time stop, teleport, acceleration, AoE pressure and one-shot time reversal heal. */
    private static void releaseTimeTyrant(ServerPlayer player) {
        LivingEntity target = nearestHostile(player, 24.0D);
        if (target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 4));
            blinkBehind(player, target, 3.0D);
        }
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 1));
        for (LivingEntity enemy : hostiles(player, 5.0D)) {
            hurt(player, enemy, scaledDamage(player, 1.0D, 8.0D));
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
        }
        player.heal(player.getMaxHealth() * 0.10F);
        particles(player, ParticleTypes.PORTAL, 100, 5.0D);
    }

    private static void spawnPhantomEchoes(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        Vec3 side = new Vec3(-flatLook(owner).z, 0.0D, flatLook(owner).x);
        for (int i = 0; i < 2; i++) {
            CombatCloneEntity clone = new CombatCloneEntity(CloneEntityTypes.COMBAT_CLONE, level);
            clone.configure(owner, 20.0F, 4.0D, 15 * 20);
            Vec3 pos = owner.position().add(side.scale(i == 0 ? 2.5D : -2.5D));
            clone.snapTo(pos.x, pos.y, pos.z);
            if (level.noCollision(clone, clone.getBoundingBox())) level.addFreshEntity(clone);
        }
    }

    private static boolean blinkBehind(ServerPlayer player, LivingEntity target, double distance) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        Vec3 look = target.getLookAngle();
        Vec3 desired = target.position().subtract(new Vec3(look.x, 0.0D, look.z).normalize().scale(distance));
        BlockPos base = BlockPos.containing(desired);
        for (int offset = 0; offset <= 3; offset++) {
            for (int yOffset : offset == 0 ? new int[]{0} : new int[]{offset, -offset}) {
                BlockPos pos = base.offset(0, yOffset, 0);
                AABB moved = player.getBoundingBox().move(
                        pos.getX() + 0.5D - player.getX(),
                        pos.getY() - player.getY(),
                        pos.getZ() + 0.5D - player.getZ()
                );
                if (level.noCollision(player, moved)) {
                    return player.teleportTo(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                            Set.of(), target.getYRot(), player.getXRot(), true);
                }
            }
        }
        return false;
    }

    private static List<LivingEntity> hostiles(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> CompanionAugmentService.isHostileTarget(player, candidate)
                        && player.distanceToSqr(candidate) <= radius * radius
        );
    }

    private static LivingEntity nearestHostile(ServerPlayer player, double radius) {
        LivingEntity best = null;
        double bestDistance = radius * radius;
        for (LivingEntity candidate : hostiles(player, radius)) {
            double distance = player.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static void hurt(ServerPlayer player, LivingEntity target, float damage) {
        if (!(player.level() instanceof ServerLevel level) || damage <= 0.0F) return;
        target.hurtServer(level, player.damageSources().playerAttack(player), damage);
    }

    private static float scaledDamage(ServerPlayer player, double multiplier, double floor) {
        return (float) Math.max(floor, CloneCombatSnapshot.meleeDamage(player) * multiplier);
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        return flat.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : flat.normalize();
    }

    private static void particles(ServerPlayer player, net.minecraft.core.particles.ParticleOptions particle, int count, double radius) {
        if (!(player.level() instanceof ServerLevel level)) return;
        level.sendParticles(particle, player.getX(), player.getY() + 0.8D, player.getZ(), count,
                radius * 0.35D, 0.8D, radius * 0.35D, 0.08D);
    }

    private static void lineParticles(ServerPlayer player, Vec3 direction, net.minecraft.core.particles.ParticleOptions particle, int blocks) {
        if (!(player.level() instanceof ServerLevel level)) return;
        for (int i = 1; i <= blocks * 2; i++) {
            double distance = i * 0.5D;
            level.sendParticles(particle,
                    player.getX() + direction.x * distance,
                    player.getY() + 0.2D,
                    player.getZ() + direction.z * distance,
                    2, 0.25D, 0.08D, 0.25D, 0.01D);
        }
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
