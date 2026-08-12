package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        int roleHealth = role == VillageRole.VANGUARD ? 8 : role == VillageRole.WARDEN ? 6 : 0;
        int bonus = bonusHealthPoints(VillageCouncilState.levelOf(player.getUUID())) + roleHealth;
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
            if (event.getEntity() instanceof Monster monster) {
                value *= VillageSkillTreeSystem.executionMultiplier(attacker, monster.getHealth(), monster.getMaxHealth());
                value *= VillageRelicSystem.executionMultiplier(attacker, monster.getHealth(), monster.getMaxHealth());
                if (projectile) {
                    value *= VillageSkillTreeSystem.projectileExecutionMultiplier(
                            attacker, monster.getHealth(), monster.getMaxHealth());
                }
            }
            event.setAmount(event.getAmount() * value);
        }
        if (event.getEntity() instanceof ServerPlayer defender) {
            float value = incomingDamageMultiplier(VillageCouncilState.levelOf(defender.getUUID()));
            value *= roleIncomingMultiplier(defender);
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
        int base = Math.min(90, 7 + Math.round(defeated.getMaxHealth() * 0.48f));
        int reward = VillageCouncilState.isInsideVillage(killer) ? Math.round(base * 1.18f) : base;
        VillageCouncilState.ExperienceResult result = VillageCouncilState.grantExperience(killer, reward);
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
        killer.sendSystemMessage(Component.literal("§d+" + result.awardedExperience() + " XP"));
        if (result.levelsGained() > 0) {
            refreshPlayerPassive(killer);
            killer.heal(Math.min(6.0f, 2.0f + result.levelsGained()));
        }
    }

    public static String useRoleSkill(ServerPlayer player) { return useRoleSkill(player, 0); }

    public static String useRoleSkill(ServerPlayer player, int slot) {
        return VillageRoleSkillSystem.useEquippedSkill(player, slot);
    }

    public static String testRoleSkill(ServerPlayer player, String skillId) {
        return VillageRoleSkillSystem.useTestSkill(player, skillId);
    }

    public static String roleLoadout(ServerPlayer player) {
        return VillageRoleSkillSystem.loadoutSummary(player);
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

    public static float outgoingDamageMultiplier(int level) {
        int value = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        return 1.0f + (value - 1) * 0.035f + ((value - 1) / 5) * 0.08f;
    }

    public static float incomingDamageMultiplier(int level) {
        int value = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        return Math.max(0.58f, 1.0f - (value - 1) * 0.009f - ((value - 1) / 5) * 0.025f);
    }

    public static int bonusHealthPoints(int level) {
        int value = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        return ((value - 1) / 5) * 4;
    }
}
