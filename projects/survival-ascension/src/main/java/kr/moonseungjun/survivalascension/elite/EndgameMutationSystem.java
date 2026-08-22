package kr.moonseungjun.survivalascension.elite;

/*
 * Endgame mutation vocabulary is adapted from Hostiles Are Too Easy (CC0 1.0):
 * Withered skeletons and Phase / Plague zombies. Survival Ascension uses its own
 * NeoForge 26.2 event implementation, probabilities, cooldowns and rewards.
 */

import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class EndgameMutationSystem {
    private static final String MUTATION_KEY = "survivalascension_endgame_mutation";
    private static final String REACTION_READY_KEY = "survivalascension_endgame_mutation_ready";
    private static final double MUTATION_CHANCE = 0.18D;

    private EndgameMutationSystem() {}

    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy) || mob.isBaby()) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (WorldAscensionData.get(level.getServer()).stage() < 2) return;
        if (event.getSpawnType().name().contains("SPAWNER")) return;
        if (mutation(mob) != Mutation.NONE) return;
        if (level.getRandom().nextDouble() >= MUTATION_CHANCE) return;

        Mutation mutation;
        if (mob instanceof AbstractSkeleton) {
            mutation = Mutation.WITHERED;
        } else if (mob instanceof Zombie) {
            mutation = level.getRandom().nextBoolean() ? Mutation.PHASE : Mutation.PLAGUE;
        } else {
            return;
        }

        mob.getPersistentData().putString(MUTATION_KEY, mutation.id);
        String existing = mob.getName().getString();
        mob.setCustomName(Component.literal(mutation.prefix + existing));
    }

    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || event.getHealthDamage() <= 0.0F) return;

        if (event.getSource().getEntity() instanceof Mob attacker
                && event.getEntity() instanceof ServerPlayer player) {
            switch (mutation(attacker)) {
                case WITHERED -> player.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                case PLAGUE -> player.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
                default -> { }
            }
        }

        if (event.getEntity() instanceof Zombie zombie
                && mutation(zombie) == Mutation.PHASE
                && event.getSource().getEntity() instanceof ServerPlayer player
                && zombie.isAlive()) {
            reactPhase(level, zombie, player);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob)) return;
        Mutation mutation = mutation(mob);
        if (mutation == Mutation.NONE || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        player.giveExperiencePoints(10);
        if (mob.level() instanceof ServerLevel level && level.getRandom().nextFloat() < 0.35F) {
            level.addFreshEntity(new ItemEntity(level, mob.getX(), mob.getY() + 0.5D, mob.getZ(), new ItemStack(Items.ECHO_SHARD)));
        }
    }

    public static boolean isMutated(net.minecraft.world.entity.LivingEntity entity) {
        return mutation(entity) != Mutation.NONE;
    }

    private static void reactPhase(ServerLevel level, Zombie zombie, ServerPlayer player) {
        CompoundTag data = zombie.getPersistentData();
        long now = level.getGameTime();
        if (now < data.getLongOr(REACTION_READY_KEY, 0L)) return;
        if (level.getRandom().nextFloat() >= 0.55F) return;

        Vec3 away = zombie.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 1.0E-5D) return;
        away = away.normalize();
        double sideSign = level.getRandom().nextBoolean() ? 1.0D : -1.0D;
        Vec3 side = new Vec3(-away.z, 0.0D, away.x).scale(sideSign);
        Vec3 impulse = away.scale(0.35D).add(side.scale(0.62D));
        zombie.setDeltaMovement(impulse.x, Math.max(0.10D, zombie.getDeltaMovement().y), impulse.z);
        zombie.hurtMarked = true;
        data.putLong(REACTION_READY_KEY, now + 45L);
    }

    private static Mutation mutation(net.minecraft.world.entity.LivingEntity entity) {
        return Mutation.fromId(entity.getPersistentData().getStringOr(MUTATION_KEY, ""));
    }

    private enum Mutation {
        NONE("", ""),
        WITHERED("withered", "§8[종말·위더] §f"),
        PHASE("phase", "§5[종말·위상] §f"),
        PLAGUE("plague", "§2[종말·역병] §f");

        final String id;
        final String prefix;
        Mutation(String id, String prefix) { this.id = id; this.prefix = prefix; }

        static Mutation fromId(String id) {
            for (Mutation mutation : values()) if (mutation.id.equals(id)) return mutation;
            return NONE;
        }
    }
}
