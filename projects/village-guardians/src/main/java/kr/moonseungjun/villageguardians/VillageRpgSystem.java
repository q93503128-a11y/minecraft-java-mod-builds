package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class VillageRpgSystem {
    private VillageRpgSystem() {}

    public static void resetTransientState() {
        VillageCombatTechniqueSystem.reset();
        VillageRoleSkillSystem.resetTransientState();
        VillagePersonalCombatSystem.reset();
    }

    public static void refreshPassives(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(VillageRpgSystem::refreshPlayerPassive);
    }

    public static void refreshPlayerPassive(ServerPlayer player) {
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(20.0D + VillageSkillTreeSystem.healthTrainingBonus(player));
        }
        var attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            // Repeat attack training is a common combat stat, not a melee-only base-attribute patch.
            // Keeping the vanilla base at 1.0 prevents melee from receiving the same training twice.
            attackDamage.setBaseValue(1.0D);
        }

        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        int roleHealth = role == VillageRole.VANGUARD ? 8 : role == VillageRole.WARDEN ? 6 : 0;
        int bonus = bonusHealthPoints(VillageCouncilState.levelOf(player.getUUID())) + roleHealth
                + VillageRolePromotionSystem.bonusHealthPoints(player, role);
        if (bonus > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 80, Math.max(0, bonus / 4 - 1)));
        }
        if (!VillageRespawnSystem.isDowned(player)) {
            boolean daytime = VillageCouncilState.currentPhase() == VillageTimePhase.DAY;
            int speedAmplifier = VillageSkillTreeSystem.passiveSpeedAmplifier(player, daytime);
            if (speedAmplifier >= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 50, speedAmplifier, false, false, true));
            }
        }
        if (role == VillageRole.WARDEN) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
            if (player.getOffhandItem().is(Items.SHIELD)) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0, false, false, true));
            }
        }
        VillagePersonalCombatSystem.applyLowHealthPassive(player);
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        boolean preScaledRicochet = event.getSource().getEntity() instanceof ServerPlayer ricochetOwner
                && VillageRoleAbilitySystem.isPreScaledRicochetDamage(ricochetOwner, event.getEntity());
        if (!preScaledRicochet
                && event.getSource().getEntity() instanceof ServerPlayer attacker
                && !(event.getEntity() instanceof ServerPlayer)) {
            boolean projectile = event.getSource().getDirectEntity() instanceof AbstractArrow;
            float value = outgoingDamageMultiplier(VillageCouncilState.levelOf(attacker.getUUID()));
            value *= roleOutgoingMultiplier(attacker, projectile);
            value *= VillageRolePromotionSystem.outgoingMultiplier(attacker, projectile);
            value *= VillageProgressionSystem.smithyDamageMultiplier(attacker);
            value *= VillageProgressionSystem.learnedSkillDamageMultiplier(attacker);
            value *= VillageSkillTreeSystem.outgoingDamageMultiplier(attacker);
            value *= VillageSkillTreeSystem.movingDamageMultiplier(attacker);
            value *= projectile ? VillageSkillTreeSystem.projectileDamageMultiplier(attacker) : 1.0f;
            value *= VillageEquipmentShop.outgoingMultiplier(attacker, projectile);
            value *= VillageWeaponStyleSystem.outgoingMultiplier(attacker, projectile);
            value *= VillageEquipmentSetSystem.outgoingMultiplier(attacker, projectile);
            value *= projectile
                    ? VillageRelicSystem.projectileMultiplier(attacker)
                    : VillageRelicSystem.meleeMultiplier(attacker);
            if (event.getEntity() instanceof Mob target) {
                if (projectile) value *= VillageRelicSystem.projectileTargetMultiplier(attacker, target);
                value *= VillageRolePromotionSystem.targetMultiplier(attacker, target, projectile);
            }
            if (event.getEntity() instanceof Monster monster) {
                value *= VillageSkillTreeSystem.executionMultiplier(attacker, monster.getHealth(), monster.getMaxHealth());
                value *= VillageRelicSystem.executionMultiplier(attacker, monster.getHealth(), monster.getMaxHealth());
                if (projectile) {
                    value *= VillageSkillTreeSystem.projectileExecutionMultiplier(
                            attacker, monster.getHealth(), monster.getMaxHealth());
                }
            }
            float flatWeaponPower = VillageEquipmentRaritySystem.flatAttackBonus(attacker, projectile);
            float attackTrainingPower = (float) VillageSkillTreeSystem.attackTrainingBonus(attacker);
            float trainingCoefficient = projectile ? 0.90f : 1.0f;
            if (projectile && event.getSource().getDirectEntity() instanceof AbstractArrow arrow) {
                trainingCoefficient *= VillageRoleAbilitySystem.projectileAttackTrainingCoefficient(attacker, arrow);
            }
            trainingCoefficient *= VillageCombatTechniqueSystem.attackTrainingPrimaryCoefficient(
                    attacker, event.getEntity(), projectile);
            event.setAmount((event.getAmount() + flatWeaponPower + attackTrainingPower * trainingCoefficient) * value);
            if (!projectile) {
                float lifeSteal = VillageRelicSystem.meleeLifeStealBonus(attacker)
                        + VillageRolePromotionSystem.meleeLifeStealBonus(attacker);
                if (lifeSteal > 0.0f) attacker.heal(Math.min(6.0f, event.getAmount() * lifeSteal));
            }
        }
        if (event.getEntity() instanceof ServerPlayer defender) {
            float value = incomingDamageMultiplier(VillageCouncilState.levelOf(defender.getUUID()));
            value *= roleIncomingMultiplier(defender);
            value *= VillageRolePromotionSystem.incomingMultiplier(defender);
            value *= VillageSkillTreeSystem.incomingDamageMultiplier(defender);
            value *= VillageSkillTreeSystem.lowHealthIncomingMultiplier(defender);
            value *= VillageSkillTreeSystem.sprintIncomingMultiplier(defender);
            value *= VillageEquipmentShop.incomingMultiplier(defender);
            value *= VillageEquipmentSetSystem.incomingMultiplier(defender);
            value *= VillageRelicSystem.incomingMultiplier(defender);
            if (VillageCouncilState.isInsideVillage(defender)) value *= VillageProgressionSystem.wallDamageMultiplier();

            Entity sourceEntity = event.getSource().getEntity();
            if (VillageRaidSystem.isRaidEnemy(sourceEntity)) {
                value *= VillageDifficultyTuning.playerDamageMultiplier(VillageCouncilState.currentDay());
            }
            event.setAmount(event.getAmount() * value);
        }
        VillagePersonalCombatSystem.handleIncomingDamage(event);
        if (!preScaledRicochet) VillageCombatTechniqueSystem.handleIncomingDamage(event);
    }

    public static void handleDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Monster defeated)
                || VillageSkillTestSystem.isTestDummy(defeated)
                || !(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        boolean raidEnemy = VillageRaidSystem.isRaidEnemy(defeated);
        VillageCouncilState.ExperienceResult result = null;
        if (!raidEnemy) {
            int base = Math.min(90, 7 + Math.round(defeated.getMaxHealth() * 0.48f));
            int reward = VillageCouncilState.isInsideVillage(killer) ? Math.round(base * 1.18f) : base;
            result = VillageCouncilState.grantExperience(killer, reward);
        }
        int baseCoins = Math.max(1, Math.round(defeated.getMaxHealth() / 12.0f));
        int coins = Math.max(1, Math.round(baseCoins * VillageSkillTreeSystem.coinRewardMultiplier(killer)));
        VillageProgressionSystem.addCoins(killer, coins, "적 처치");
        float heal = VillageSkillTreeSystem.killHealAmount(killer);
        if (heal > 0.0f) killer.heal(heal);
        VillagePersonalCombatSystem.applyKillMomentum(killer);
        int speedSeconds = VillageSkillTreeSystem.killSpeedSeconds(killer);
        if (speedSeconds > 0) {
            killer.addEffect(new MobEffectInstance(MobEffects.SPEED, speedSeconds * 20, 2, false, false, true));
        }
        VillagePersonalCombatSystem.healNearbyAlliesOnKill(killer);
        MinecraftServer server = killer.level().getServer();
        float supplyChance = VillageSkillTreeSystem.sharedSupplyChance(killer);
        if (server != null && supplyChance > 0.0f && killer.getRandom().nextFloat() < supplyChance) {
            VillageProgressionSystem.addSupplies(server, 1, "공동 회수");
        }
        if (result != null) {
            killer.sendSystemMessage(Component.literal("§d+" + result.awardedExperience() + " XP"));
            if (result.levelsGained() > 0) {
                refreshPlayerPassive(killer);
                killer.heal(Math.min(6.0f, 2.0f + result.levelsGained()));
            }
        }
    }

    public static String useRoleSkill(ServerPlayer player) { return useRoleSkill(player, 0); }

    public static String useRoleSkill(ServerPlayer player, int slot) {
        return VillageRoleSkillSystem.useEquippedSkill(player, slot);
    }

    private static float roleOutgoingMultiplier(ServerPlayer player, boolean projectile) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) return 1.0f;
        return switch (role) {
            case VANGUARD -> !projectile && player.getMainHandItem().is(ItemTags.SWORDS) ? 1.28f : 1.08f;
            case RANGER -> projectile ? (isOnWallTop(player) ? 1.58f : 1.30f) : 0.92f;
            case ARCANIST -> projectile ? 1.04f : 0.98f;
            case LUMINAR -> 0.94f;
            case WARDEN -> 0.96f;
        };
    }

    private static float roleIncomingMultiplier(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) return 1.0f;
        return switch (role) {
            case VANGUARD -> 0.94f;
            case RANGER -> 1.02f;
            case ARCANIST -> 1.00f;
            case LUMINAR -> 0.97f;
            case WARDEN -> player.getOffhandItem().is(Items.SHIELD) ? 0.72f : 0.82f;
        };
    }

    private static boolean isOnWallTop(ServerPlayer player) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || !VillageCouncilState.isInsideVillage(player)) return false;
        BlockPos pos = player.blockPosition();
        int dx = Math.abs(pos.getX() - center.getX());
        int dz = Math.abs(pos.getZ() - center.getZ());
        int relativeY = pos.getY() - center.getY();
        boolean nearWallLine = Math.abs(dx - VillageWorldSystem.FORTRESS_RADIUS) <= 7
                || Math.abs(dz - VillageWorldSystem.FORTRESS_RADIUS) <= 7;
        return nearWallLine && relativeY >= 7 && relativeY <= 17;
    }

    public enum SkillAttackProfile {
        SINGLE_TARGET(0.90f),
        BURST_AREA(0.55f),
        MULTI_HIT(0.30f),
        PERSISTENT(0.16f);

        private final float trainingCoefficient;

        SkillAttackProfile(float trainingCoefficient) {
            this.trainingCoefficient = trainingCoefficient;
        }

        float trainingCoefficient() {
            return trainingCoefficient;
        }
    }

    /**
     * Converts the common flat attack-training stat into one bounded skill multiplier.
     * Skills already own their level/role/equipment/relic scaling, so this factor is applied once
     * to final custom-skill damage instead of adding the flat stat to every area or periodic hit.
     */
    public static float skillAttackTrainingMultiplier(ServerPlayer player, SkillAttackProfile profile) {
        if (player == null || profile == null) return 1.0f;
        float bonus = (float) VillageSkillTreeSystem.attackTrainingBonus(player);
        if (bonus <= 0.0f) return 1.0f;
        int combatLevel = RpgProgress.combatScalingLevel(VillageCouncilState.levelOf(player.getUUID()));
        float referenceAttack = 16.0f + combatLevel * 0.36f;
        return 1.0f + bonus / referenceAttack * profile.trainingCoefficient();
    }

    public static float applySkillAttackTraining(
            ServerPlayer player, float damage, SkillAttackProfile profile) {
        return damage * skillAttackTrainingMultiplier(player, profile);
    }

    public static float outgoingDamageMultiplier(int level) {
        int value = RpgProgress.combatScalingLevel(level);
        int foundation = Math.min(29, value - 1);
        int mastery = Math.max(0, value - 30);
        return 1.0f
                + foundation * 0.035f + (foundation / 5) * 0.08f
                + mastery * 0.008f + (mastery / 10) * 0.025f;
    }

    public static float incomingDamageMultiplier(int level) {
        int value = RpgProgress.combatScalingLevel(level);
        int foundation = Math.min(29, value - 1);
        int mastery = Math.max(0, value - 30);
        float result = 1.0f - foundation * 0.009f - (foundation / 5) * 0.025f
                - mastery * 0.0009f - (mastery / 10) * 0.004f;
        return Math.max(0.50f, result);
    }

    public static int bonusHealthPoints(int level) {
        int value = RpgProgress.combatScalingLevel(level);
        int foundation = (Math.min(30, value) - 1) / 5 * 4;
        int mastery = Math.max(0, value - 30) / 10 * 2;
        return foundation + mastery;
    }
}
