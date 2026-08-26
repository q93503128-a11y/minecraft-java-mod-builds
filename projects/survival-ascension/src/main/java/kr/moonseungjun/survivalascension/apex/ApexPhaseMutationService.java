package kr.moonseungjun.survivalascension.apex;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Comparator;
import java.util.List;

/**
 * Adds two persistent combat phases to every Survival Ascension Apex archetype.
 *
 * ApexHuntSystem already owns the encounter, timeout and cleanup. This service only reacts to the
 * boss marker/health modifier that the hunt places on its boss, so escorts can never promote
 * themselves into an Apex phase. Phase flags live on the mob and therefore cannot repeat when a
 * hit, relog or another player re-enters the fight.
 */
public final class ApexPhaseMutationService {
    private static final String APEX_TYPE_KEY = "survivalascension_apex_type";
    private static final String PHASE_ONE_KEY = "survivalascension_apex_phase_one";
    private static final String PHASE_TWO_KEY = "survivalascension_apex_phase_two";
    private static final Identifier APEX_HEALTH_ID = id("apex_health");
    private static final Identifier PHASE_ONE_SPEED_ID = id("apex_phase_one_speed");
    private static final Identifier PHASE_ONE_ARMOR_ID = id("apex_phase_one_armor");
    private static final Identifier PHASE_TWO_SPEED_ID = id("apex_phase_two_speed");
    private static final Identifier PHASE_TWO_ATTACK_ID = id("apex_phase_two_attack");

    private ApexPhaseMutationService() {}

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0F
                || !(event.getEntity() instanceof Mob boss)
                || !(boss.level() instanceof ServerLevel level)
                || !isApexBoss(boss)) {
            return;
        }

        ApexArchetype archetype = archetypeOf(boss);
        if (archetype == null) return;
        double projectedHealth = Math.max(0.0D, boss.getHealth() - event.getAmount());
        double healthRatio = projectedHealth / Math.max(1.0F, boss.getMaxHealth());

        // Evaluate the post-hit health projection while the incoming hit is still mutable. This
        // makes a threshold-crossing hit trigger its phase immediately instead of one hit late.
        // A very large hit can cross both thresholds at once, so both mutations are installed.
        if (healthRatio <= 0.32D && !boss.getPersistentData().getBooleanOr(PHASE_TWO_KEY, false)) {
            if (!boss.getPersistentData().getBooleanOr(PHASE_ONE_KEY, false)) triggerPhaseOne(level, boss, archetype);
            triggerPhaseTwo(level, boss, archetype);
        } else if (healthRatio <= 0.62D && !boss.getPersistentData().getBooleanOr(PHASE_ONE_KEY, false)) {
            triggerPhaseOne(level, boss, archetype);
        }
    }

    private static void triggerPhaseOne(ServerLevel level, Mob boss, ApexArchetype archetype) {
        boss.getPersistentData().putBoolean(PHASE_ONE_KEY, true);
        addPermanent(boss.getAttribute(Attributes.MOVEMENT_SPEED), PHASE_ONE_SPEED_ID, 0.12D,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(boss.getAttribute(Attributes.ARMOR), PHASE_ONE_ARMOR_ID, 2.0D,
                AttributeModifier.Operation.ADD_VALUE);
        boss.setGlowingTag(true);

        List<ServerPlayer> players = nearbyPlayers(level, boss, 16.0D);
        switch (archetype) {
            case WOODLAND_BREAKER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, 0, false, true, true));
                radialPush(boss, players, 0.55D, 0.18D);
                burst(level, boss, ParticleTypes.CRIT, 28);
            }
            case ARID_COMMANDER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 180, 0, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0)));
                burst(level, boss, ParticleTypes.SMOKE, 26);
            }
            case WETLAND_PLAGUEHEART -> {
                boss.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 1, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0)));
                burst(level, boss, ParticleTypes.SPORE_BLOSSOM_AIR, 30);
            }
            case HIGHLAND_HUNTER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.SPEED, 180, 1, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0)));
                burst(level, boss, ParticleTypes.CLOUD, 24);
            }
            case OCEAN_TYRANT -> {
                boss.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 180, 0, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 0)));
                pullPlayers(boss, players, 0.32D);
                burst(level, boss, ParticleTypes.BUBBLE, 34);
            }
            case DEEP_STALKER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.SPEED, 180, 1, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0)));
                leapTowardNearest(boss, players, 0.72D, 0.34D);
                burst(level, boss, ParticleTypes.SQUID_INK, 24);
            }
            case FROZEN_WARDEN -> {
                boss.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 180, 0, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1)));
                burst(level, boss, ParticleTypes.SNOWFLAKE, 32);
            }
            case NETHER_REAVER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, false, true));
                boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, 0, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0)));
                burst(level, boss, ParticleTypes.FLAME, 32);
            }
            case END_HARBINGER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 160, 0, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 24, 0)));
                burst(level, boss, ParticleTypes.PORTAL, 36);
            }
        }
        announce(players, "§6[정점 2단계] §f" + archetype.koreanName() + "§f이 전투 양상을 바꿉니다.");
    }

    private static void triggerPhaseTwo(ServerLevel level, Mob boss, ApexArchetype archetype) {
        boss.getPersistentData().putBoolean(PHASE_TWO_KEY, true);
        addPermanent(boss.getAttribute(Attributes.MOVEMENT_SPEED), PHASE_TWO_SPEED_ID, 0.10D,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(boss.getAttribute(Attributes.ATTACK_DAMAGE), PHASE_TWO_ATTACK_ID, 0.18D,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        List<ServerPlayer> players = nearbyPlayers(level, boss, 18.0D);
        switch (archetype) {
            case WOODLAND_BREAKER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 240, 1, false, true, true));
                radialPush(boss, players, 1.05D, 0.30D);
                burst(level, boss, ParticleTypes.CRIT, 42);
            }
            case ARID_COMMANDER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.SPEED, 220, 1, false, true, true));
                boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 220, 0, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1)));
                burst(level, boss, ParticleTypes.ANGRY_VILLAGER, 28);
            }
            case WETLAND_PLAGUEHEART -> {
                boss.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 180, 1, false, true, true));
                players.forEach(player -> {
                    player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 0));
                });
                burst(level, boss, ParticleTypes.WITCH, 40);
            }
            case HIGHLAND_HUNTER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.SPEED, 260, 2, false, true, true));
                radialPush(boss, players, 0.35D, 0.12D);
                burst(level, boss, ParticleTypes.SWEEP_ATTACK, 22);
            }
            case OCEAN_TYRANT -> {
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 110, 1)));
                pullPlayers(boss, players, 0.62D);
                burst(level, boss, ParticleTypes.BUBBLE_COLUMN_UP, 42);
            }
            case DEEP_STALKER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0)));
                leapTowardNearest(boss, players, 1.05D, 0.44D);
                burst(level, boss, ParticleTypes.REVERSE_PORTAL, 36);
            }
            case FROZEN_WARDEN -> {
                boss.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 2)));
                burst(level, boss, ParticleTypes.SNOWFLAKE, 48);
            }
            case NETHER_REAVER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 240, 1, false, true, true));
                players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.WITHER, 90, 0)));
                radialPush(boss, players, 0.75D, 0.20D);
                burst(level, boss, ParticleTypes.SOUL_FIRE_FLAME, 44);
            }
            case END_HARBINGER -> {
                boss.addEffect(new MobEffectInstance(MobEffects.SPEED, 220, 1, false, true, true));
                players.forEach(player -> {
                    player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 38, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                });
                burst(level, boss, ParticleTypes.DRAGON_BREATH, 44);
            }
        }
        announce(players, "§4[정점 최종단계] §f" + archetype.koreanName() + "§f이 최종 전투 양상에 돌입합니다.");
    }

    private static List<ServerPlayer> nearbyPlayers(ServerLevel level, Mob boss, double radius) {
        return level.getEntitiesOfClass(
                ServerPlayer.class,
                boss.getBoundingBox().inflate(radius),
                player -> player.isAlive() && !player.isSpectator());
    }

    private static void announce(List<ServerPlayer> players, String text) {
        Component message = Component.literal(text);
        players.forEach(player -> player.sendSystemMessage(message));
    }

    private static void burst(ServerLevel level, Mob boss, ParticleOptions particle, int count) {
        level.sendParticles(particle,
                boss.getX(), boss.getY() + boss.getBbHeight() * 0.5D, boss.getZ(),
                count, 1.2D, Math.max(0.3D, boss.getBbHeight() * 0.35D), 1.2D, 0.04D);
    }

    private static void radialPush(Mob boss, List<ServerPlayer> players, double horizontal, double vertical) {
        for (ServerPlayer player : players) {
            Vec3 away = player.position().subtract(boss.position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() <= 1.0E-5D) continue;
            away = away.normalize();
            player.setDeltaMovement(player.getDeltaMovement().add(away.x * horizontal, vertical, away.z * horizontal));
            player.hurtMarked = true;
        }
    }

    private static void pullPlayers(Mob boss, List<ServerPlayer> players, double strength) {
        for (ServerPlayer player : players) {
            Vec3 toward = boss.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
            if (toward.lengthSqr() <= 1.0E-5D) continue;
            toward = toward.normalize();
            player.setDeltaMovement(player.getDeltaMovement().add(toward.x * strength, 0.08D, toward.z * strength));
            player.hurtMarked = true;
        }
    }

    private static void leapTowardNearest(Mob boss, List<ServerPlayer> players, double horizontal, double vertical) {
        ServerPlayer nearest = players.stream().min(Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
        if (nearest == null) return;
        Vec3 toward = nearest.position().subtract(boss.position()).multiply(1.0D, 0.0D, 1.0D);
        if (toward.lengthSqr() <= 1.0E-5D) return;
        toward = toward.normalize();
        boss.setDeltaMovement(toward.x * horizontal, vertical, toward.z * horizontal);
        boss.hurtMarked = true;
    }

    private static ApexArchetype archetypeOf(Mob boss) {
        String value = boss.getPersistentData().getStringOr(APEX_TYPE_KEY, "");
        if (value.isEmpty()) return null;
        try {
            return ApexArchetype.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isApexBoss(Mob mob) {
        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        return maxHealth != null && maxHealth.hasModifier(APEX_HEALTH_ID);
    }

    private static void addPermanent(AttributeInstance attribute, Identifier id, double amount,
                                     AttributeModifier.Operation operation) {
        if (attribute == null || attribute.hasModifier(id)) return;
        attribute.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, path);
    }
}
