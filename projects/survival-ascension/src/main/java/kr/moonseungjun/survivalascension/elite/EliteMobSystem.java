package kr.moonseungjun.survivalascension.elite;

/*
 * Rank-driven permanent attribute construction is adapted from Mob Champions.
 * Copyright (c) 2024 Wendall Cada, MIT License.
 * Survival Ascension uses its own rank probabilities, traits, progression coupling and rewards.
 */

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;

public final class EliteMobSystem {
    private static final String RANK_KEY = "survivalascension_elite_rank";
    private static final String TRAIT_KEY = "survivalascension_elite_trait";

    private static final Identifier HEALTH_ID = id("elite_health");
    private static final Identifier ARMOR_ID = id("elite_armor");
    private static final Identifier SPEED_ID = id("elite_speed");
    private static final Identifier ATTACK_ID = id("elite_attack");
    private static final Identifier KNOCKBACK_ID = id("elite_knockback");
    private static final Identifier TRAIT_SPEED_ID = id("elite_trait_speed");
    private static final Identifier TRAIT_ARMOR_ID = id("elite_trait_armor");
    private static final Identifier TRAIT_ATTACK_ID = id("elite_trait_attack");
    private static final Identifier TRAIT_KNOCKBACK_ID = id("elite_trait_knockback");

    private EliteMobSystem() {}

    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy) || mob instanceof EnderDragon || mob instanceof WitherBoss || mob.isBaby()) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (isElite(mob)) return;

        String spawnReason = event.getSpawnType().name();
        if (spawnReason.contains("SPAWNER")) return;

        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(96.0D),
                player -> !player.isSpectator());
        if (nearby.isEmpty()) return;

        double power = nearby.stream().mapToDouble(EliteMobSystem::averageSkillLevel).average().orElse(0.0D);
        RandomSource random = level.getRandom();
        double eliteChance = Math.min(0.16D, 0.025D + power * 0.00135D);
        if (random.nextDouble() >= eliteChance) return;

        Rank rank = chooseRank(random, power);
        Trait trait = Trait.values()[random.nextInt(Trait.values().length)];
        applyElite(mob, rank, trait);

        if (rank == Rank.MYTHIC_III) {
            Component alert = Component.literal("§6[신화 출현] §f" + mob.getName().getString() + " §7· §e" + trait.koreanName);
            for (ServerPlayer player : nearby) player.sendSystemMessage(alert, true);
        }
    }

    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Mob attacker) || !isElite(attacker)) return;
        if (trait(attacker) != Trait.BERSERKER || attacker.getHealth() > attacker.getMaxHealth() * 0.5F) return;
        Rank rank = rank(attacker);
        float multiplier = switch (rank) {
            case ELITE_I -> 1.25F;
            case ASCENDED_II -> 1.40F;
            case MYTHIC_III -> 1.60F;
            default -> 1.0F;
        };
        event.setNewDamage(event.getNewDamage() * multiplier);
    }

    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        if (!(event.getSource().getEntity() instanceof Mob attacker) || !isElite(attacker)) return;
        if (trait(attacker) != Trait.VAMPIRIC || event.getHealthDamage() <= 0.0F || !attacker.isAlive()) return;
        float fraction = switch (rank(attacker)) {
            case ELITE_I -> 0.18F;
            case ASCENDED_II -> 0.28F;
            case MYTHIC_III -> 0.40F;
            default -> 0.0F;
        };
        float heal = Math.min(attacker.getMaxHealth() * 0.08F, event.getHealthDamage() * fraction);
        if (heal > 0.0F) attacker.heal(heal);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !isElite(event.getEntity())) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        Rank rank = rank(event.getEntity());
        int vanillaXp = switch (rank) {
            case ELITE_I -> 8;
            case ASCENDED_II -> 24;
            case MYTHIC_III -> 60;
            default -> 0;
        };
        if (vanillaXp > 0) player.giveExperiencePoints(vanillaXp);
        if (rank == Rank.MYTHIC_III) {
            player.sendSystemMessage(Component.literal("§6[신화 처치] §f추가 경험치 §e+" + vanillaXp));
        }
    }

    public static boolean isElite(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getPersistentData().getIntOr(RANK_KEY, 0) > 0;
    }

    public static int rankId(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getPersistentData().getIntOr(RANK_KEY, 0);
    }

    private static double averageSkillLevel(ServerPlayer player) {
        SkillProgressData data = SkillProgressData.get(player);
        int total = 0;
        for (SkillType skill : SkillType.values()) total += data.level(player, skill);
        return total / (double) SkillType.values().length;
    }

    private static Rank chooseRank(RandomSource random, double power) {
        double roll = random.nextDouble();
        double mythicChance = Math.min(0.13D, 0.015D + power * 0.00115D);
        double ascendedChance = Math.min(0.42D, 0.10D + power * 0.0032D);
        if (roll < mythicChance) return Rank.MYTHIC_III;
        if (roll < mythicChance + ascendedChance) return Rank.ASCENDED_II;
        return Rank.ELITE_I;
    }

    private static void applyElite(Mob mob, Rank rank, Trait trait) {
        CompoundTag data = mob.getPersistentData();
        data.putInt(RANK_KEY, rank.id);
        data.putString(TRAIT_KEY, trait.id);

        addPermanent(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, rank.healthBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(mob.getAttribute(Attributes.ARMOR), ARMOR_ID, rank.armorBonus, AttributeModifier.Operation.ADD_VALUE);
        addPermanent(mob.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, rank.speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(mob.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, rank.attackBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_ID, rank.knockbackBonus, AttributeModifier.Operation.ADD_VALUE);

        switch (trait) {
            case SWIFT -> addPermanent(mob.getAttribute(Attributes.MOVEMENT_SPEED), TRAIT_SPEED_ID,
                    switch (rank) { case ELITE_I -> 0.12D; case ASCENDED_II -> 0.18D; case MYTHIC_III -> 0.25D; default -> 0.0D; },
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case BULWARK -> {
                addPermanent(mob.getAttribute(Attributes.ARMOR), TRAIT_ARMOR_ID,
                        switch (rank) { case ELITE_I -> 2.0D; case ASCENDED_II -> 4.0D; case MYTHIC_III -> 7.0D; default -> 0.0D; },
                        AttributeModifier.Operation.ADD_VALUE);
                addPermanent(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), TRAIT_KNOCKBACK_ID,
                        switch (rank) { case ELITE_I -> 0.10D; case ASCENDED_II -> 0.20D; case MYTHIC_III -> 0.35D; default -> 0.0D; },
                        AttributeModifier.Operation.ADD_VALUE);
            }
            case BERSERKER -> addPermanent(mob.getAttribute(Attributes.ATTACK_DAMAGE), TRAIT_ATTACK_ID,
                    switch (rank) { case ELITE_I -> 0.08D; case ASCENDED_II -> 0.15D; case MYTHIC_III -> 0.25D; default -> 0.0D; },
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case VAMPIRIC -> { }
        }

        mob.setHealth(mob.getMaxHealth());
        if (mob.getCustomName() == null) {
            String prefix = switch (rank) {
                case ELITE_I -> "§b[정예 I] ";
                case ASCENDED_II -> "§d[승천 II] ";
                case MYTHIC_III -> "§6[신화 III] ";
                default -> "";
            };
            mob.setCustomName(Component.literal(prefix + trait.koreanName + " " + mob.getName().getString()));
            mob.setCustomNameVisible(rank == Rank.MYTHIC_III);
        }
    }

    private static void addPermanent(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null || amount == 0.0D || attribute.hasModifier(id)) return;
        attribute.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    private static Rank rank(net.minecraft.world.entity.LivingEntity entity) {
        return Rank.fromId(entity.getPersistentData().getIntOr(RANK_KEY, 0));
    }

    private static Trait trait(net.minecraft.world.entity.LivingEntity entity) {
        return Trait.fromId(entity.getPersistentData().getStringOr(TRAIT_KEY, ""));
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, path); }

    private enum Rank {
        COMMON(0, 0, 0, 0, 0, 0),
        ELITE_I(1, 0.35D, 2.0D, 0.05D, 0.15D, 0.05D),
        ASCENDED_II(2, 0.85D, 5.0D, 0.10D, 0.35D, 0.15D),
        MYTHIC_III(3, 1.70D, 9.0D, 0.16D, 0.60D, 0.35D);

        final int id;
        final double healthBonus, armorBonus, speedBonus, attackBonus, knockbackBonus;
        Rank(int id, double healthBonus, double armorBonus, double speedBonus, double attackBonus, double knockbackBonus) {
            this.id = id;
            this.healthBonus = healthBonus;
            this.armorBonus = armorBonus;
            this.speedBonus = speedBonus;
            this.attackBonus = attackBonus;
            this.knockbackBonus = knockbackBonus;
        }
        static Rank fromId(int id) {
            for (Rank rank : values()) if (rank.id == id) return rank;
            return COMMON;
        }
    }

    private enum Trait {
        SWIFT("swift", "신속"), BULWARK("bulwark", "철벽"), VAMPIRIC("vampiric", "흡혈"), BERSERKER("berserker", "광전사");
        final String id, koreanName;
        Trait(String id, String koreanName) { this.id = id; this.koreanName = koreanName; }
        static Trait fromId(String id) {
            for (Trait trait : values()) if (trait.id.equals(id)) return trait;
            return SWIFT;
        }
    }
}
