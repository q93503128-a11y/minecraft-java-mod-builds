package kr.moonseungjun.survivalascension.elite;

import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Gives ordinary Elite I / Ascended II traits different counterplay without turning them into fake bosses.
 *
 * External field bosses are deliberately excluded: their source mod owns their combat identity. This class
 * is fully event-driven and only reacts to entities that are already fighting, so it adds no world scans.
 */
public final class EliteTraitTacticsService {
    private static final String RANK_KEY = "survivalascension_elite_rank";
    private static final String TRAIT_KEY = "survivalascension_elite_trait";
    private static final String TACTIC_READY_KEY = "survivalascension_elite_tactic_ready";
    private static final String GUARD_FEEDBACK_READY_KEY = "survivalascension_elite_guard_feedback_ready";

    private EliteTraitTacticsService() {}

    /**
     * Bulwark is a positioning check instead of just another armor number. Frontal player attacks are
     * softened, while side/back attacks pass through normally. Environmental damage is unaffected.
     */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob defender) || event.getAmount() <= 0.0F) return;
        int rank = ambientRank(defender);
        if (rank <= 0 || !"bulwark".equals(trait(defender))) return;
        if (!(defender.level() instanceof ServerLevel level)) return;

        ServerPlayer attacker = contributingPlayer(event.getSource(), level);
        if (attacker == null || !isInFront(defender, attacker)) return;

        float multiplier = rank >= 2 ? 0.62F : 0.75F;
        event.setAmount(Math.max(0.0F, event.getAmount() * multiplier));

        CompoundTag data = defender.getPersistentData();
        long now = level.getGameTime();
        if (now >= data.getLongOr(GUARD_FEEDBACK_READY_KEY, 0L)) {
            data.putLong(GUARD_FEEDBACK_READY_KEY, now + 12L);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    defender.getX(), defender.getY() + defender.getBbHeight() * 0.55D, defender.getZ(),
                    rank >= 2 ? 14 : 9, 0.45D, 0.55D, 0.45D, 0.08D);
        }
    }

    /**
     * Swift and Berserker react when hit; Vampiric becomes harder to disengage from after landing a hit.
     * Existing EliteMobSystem trait stats/healing remain the baseline, while these tactics add movement and
     * defensive tempo rather than another large attack multiplier.
     */
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F) return;

        if (event.getEntity() instanceof Mob defender && defender.isAlive()
                && defender.level() instanceof ServerLevel level) {
            int rank = ambientRank(defender);
            ServerPlayer player = rank > 0 ? contributingPlayer(event.getSource(), level) : null;
            if (player != null) {
                String trait = trait(defender);
                if ("swift".equals(trait)) triggerSwiftFlank(level, defender, player, rank);
                else if ("berserker".equals(trait) && defender.getHealth() <= defender.getMaxHealth() * 0.50F) {
                    triggerBerserkerRush(level, defender, rank);
                }
            }
        }

        if (event.getSource().getEntity() instanceof Mob attacker
                && event.getEntity() instanceof ServerPlayer
                && attacker.isAlive()
                && attacker.level() instanceof ServerLevel level) {
            int rank = ambientRank(attacker);
            if (rank > 0 && "vampiric".equals(trait(attacker))) {
                triggerBloodHunt(level, attacker, rank);
            }
        }
    }

    private static void triggerSwiftFlank(ServerLevel level, Mob mob, ServerPlayer player, int rank) {
        if (!claimTactic(mob, level.getGameTime(), rank >= 2 ? 34 : 46)) return;

        Vec3 toward = player.position().subtract(mob.position()).multiply(1.0D, 0.0D, 1.0D);
        if (toward.lengthSqr() <= 1.0E-5D) return;
        toward = toward.normalize();
        double sideSign = level.getRandom().nextBoolean() ? 1.0D : -1.0D;
        Vec3 side = new Vec3(-toward.z, 0.0D, toward.x).scale(sideSign);
        double lateral = rank >= 2 ? 0.82D : 0.64D;
        double pressure = rank >= 2 ? 0.28D : 0.20D;
        Vec3 impulse = side.scale(lateral).add(toward.scale(pressure));
        mob.setDeltaMovement(impulse.x, Math.max(0.10D, mob.getDeltaMovement().y), impulse.z);
        mob.hurtMarked = true;
        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, rank >= 2 ? 34 : 26, rank >= 2 ? 1 : 0, true, false));
        level.sendParticles(ParticleTypes.CLOUD,
                mob.getX(), mob.getY() + 0.2D, mob.getZ(), rank >= 2 ? 12 : 8,
                0.35D, 0.12D, 0.35D, 0.03D);
    }

    private static void triggerBerserkerRush(ServerLevel level, Mob mob, int rank) {
        if (!claimTactic(mob, level.getGameTime(), rank >= 2 ? 48 : 64)) return;
        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, rank >= 2 ? 60 : 45, rank >= 2 ? 1 : 0, true, false));
        level.sendParticles(ParticleTypes.CRIT,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.55D, mob.getZ(),
                rank >= 2 ? 18 : 12, 0.5D, 0.65D, 0.5D, 0.12D);
    }

    private static void triggerBloodHunt(ServerLevel level, Mob mob, int rank) {
        if (!claimTactic(mob, level.getGameTime(), rank >= 2 ? 34 : 46)) return;

        mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, rank >= 2 ? 60 : 40, rank >= 2 ? 1 : 0, true, false));
        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, rank >= 2 ? 45 : 32, 0, true, false));
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.65D, mob.getZ(),
                rank >= 2 ? 12 : 8, 0.4D, 0.55D, 0.4D, 0.04D);
    }

    private static ServerPlayer contributingPlayer(DamageSource source, ServerLevel level) {
        if (source.getEntity() instanceof ServerPlayer direct) return direct;
        return AscensionAffixes.rangedProjectileOwner(source.getDirectEntity(), level);
    }

    private static boolean claimTactic(Mob mob, long now, int cooldownTicks) {
        CompoundTag data = mob.getPersistentData();
        if (now < data.getLongOr(TACTIC_READY_KEY, 0L)) return false;
        data.putLong(TACTIC_READY_KEY, now + cooldownTicks);
        return true;
    }

    private static int ambientRank(Mob mob) {
        if (MythicFieldBossService.isExternalFieldBoss(mob)) return 0;
        int rank = mob.getPersistentData().getIntOr(RANK_KEY, 0);
        return rank >= 1 && rank <= 2 ? rank : 0;
    }

    private static String trait(Mob mob) {
        return mob.getPersistentData().getStringOr(TRAIT_KEY, "");
    }

    private static boolean isInFront(Mob defender, Entity attacker) {
        Vec3 facing = defender.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        Vec3 toAttacker = attacker.position().subtract(defender.position()).multiply(1.0D, 0.0D, 1.0D);
        if (facing.lengthSqr() <= 1.0E-5D || toAttacker.lengthSqr() <= 1.0E-5D) return true;
        return facing.normalize().dot(toAttacker.normalize()) >= 0.20D;
    }
}
