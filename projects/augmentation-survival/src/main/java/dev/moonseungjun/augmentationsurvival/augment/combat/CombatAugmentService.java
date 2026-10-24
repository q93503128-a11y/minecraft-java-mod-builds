package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Shared server-owned combat runtime for root damage, reactive movement, kill effects and lethal
 * interception. Timed state only tracks players who currently have an active combat effect.
 */
public final class CombatAugmentService {
    private static final String KEEN_EDGE_ID = id("keen_edge");
    private static final String PRECISE_AIM_ID = id("precise_aim");
    private static final String TOUGH_SKIN_ID = id("tough_skin");
    private static final String LIGHT_LANDING_ID = id("light_landing");
    private static final String STRONG_LUNGS_ID = id("strong_lungs");
    private static final String LAST_STRENGTH_ID = id("last_strength");
    private static final String KILL_STREAK_ID = id("kill_streak");
    private static final String URGENT_STEPS_ID = id("urgent_steps");
    private static final String LIFESTEAL_ID = id("lifesteal");
    private static final String LAST_STAND_ID = id("last_stand");
    private static final String EMERGENCY_TOTEM_ID = id("emergency_totem");
    private static final String REINCARNATION_ID = id("reincarnation");

    private static final Identifier DYNAMIC_MOVEMENT_ID = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "augment_dynamic/movement_speed"
    );
    private static final Identifier DYNAMIC_ATTACK_SPEED_ID = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "augment_dynamic/attack_speed"
    );

    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

    private CombatAugmentService() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CombatAugmentService::allowDamage);
        ServerLivingEntityEvents.ALLOW_DEATH.register(CombatAugmentService::allowDeath);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(CombatAugmentService::afterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                clearRuntime(player);
            }
        });
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(CombatAugmentService::afterKill);
        ServerPlayerEvents.JOIN.register(CombatAugmentService::clearRuntime);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> clearRuntime(newPlayer)
        );
        ServerTickEvents.END_SERVER_TICK.register(CombatAugmentService::tickRuntime);
    }

    /** Applies root attacker bonuses before vanilla armor/protection mitigation. */
    public static float modifyOutgoingDamage(LivingEntity target, DamageSource source, float damage) {
        if (!(damage > 0.0F) || !Float.isFinite(damage)) {
            return damage;
        }
        if (CombatProvenanceContext.isDelayedDamage()) {
            return damage;
        }
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return damage;
        }

        CombatProvenance provenance = CombatProvenanceContext.resolve(source);
        if (!provenance.isRootDirect()) {
            return damage;
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(attacker);
        CombatTuning.Snapshot tuning = CombatTuning.current();
        double bonus = 0.0D;
        if (provenance == CombatProvenance.ROOT_MELEE) {
            bonus += tuning.keenEdgePerLevel() * boundedLevel(state, KEEN_EDGE_ID);
        } else if (provenance == CombatProvenance.ROOT_PROJECTILE) {
            bonus += tuning.preciseAimPerLevel() * boundedLevel(state, PRECISE_AIM_ID);
        }
        if (attacker.getMaxHealth() > 0.0F
                && attacker.getHealth() / attacker.getMaxHealth() <= tuning.lastStrengthHealthThreshold()) {
            bonus += tuning.lastStrengthPerLevel() * boundedLevel(state, LAST_STRENGTH_ID);
        }
        float scaled = safeScale(damage, 1.0D + bonus);
        return CombatExpansionService.modifyOutgoingDamage(target, source, scaled);
    }

    /** Applies augment damage layers after vanilla armor/Protection and before vanilla absorption. */
    public static float modifyPostMitigationDamage(LivingEntity victim, DamageSource source, float damage) {
        if (CombatProvenanceContext.isDelayedDamage()) {
            return damage;
        }
        if (!(victim instanceof ServerPlayer player) || !(damage > 0.0F) || !Float.isFinite(damage)) {
            return CombatExpansionService.modifyFinalTakenDamage(victim, source, damage);
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        CombatTuning.Snapshot tuning = CombatTuning.current();
        double multiplier = 1.0D;

        int toughSkin = boundedLevel(state, TOUGH_SKIN_ID);
        if (toughSkin > 0) {
            multiplier *= 1.0D - tuning.toughSkinPerLevel() * toughSkin;
        }
        if (source.typeHolder().is(DamageTypeTags.IS_FALL)) {
            multiplier *= 1.0D - tuning.lightLandingPerLevel() * boundedLevel(state, LIGHT_LANDING_ID);
        }
        if (source.typeHolder().is(DamageTypeTags.IS_DROWNING)) {
            multiplier *= 1.0D - tuning.strongLungsPerLevel() * boundedLevel(state, STRONG_LUNGS_ID);
        }
        float reduced = safeScale(damage, Math.max(0.0D, multiplier));
        reduced = CombatExpansionService.modifyFinalTakenDamage(player, source, reduced);
        return ProtectionAugmentService.modifyAfterReductions(player, source, reduced);
    }

    /** Called after vanilla hand-held death protection failed. */
    public static boolean tryPostVanillaDeathProtection(
            LivingEntity entity,
            DamageSource source
    ) {
        if (!(entity instanceof ServerPlayer player)
                || source.typeHolder().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);

        if (boundedLevel(state, EMERGENCY_TOTEM_ID) > 0 && consumeEmergencyTotem(player)) {
            return true;
        }

        if (boundedLevel(state, REINCARNATION_ID) <= 0) {
            return false;
        }
        long now = player.level().getGameTime();
        if (now < state.cooldownReadyTick(REINCARNATION_ID)) {
            return false;
        }

        CombatTuning.Snapshot tuning = CombatTuning.current();
        long recoverTick = now + tuning.reincarnationStasisTicks();
        player.setHealth(1.0F);
        player.stopUsingItem();
        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);
        player.resetFallDistance();
        AugmentationPlayerStateService.replace(
                player,
                state.withCooldownReadyTick(
                        REINCARNATION_ID,
                        now + tuning.reincarnationCooldownTicks()
                )
        );
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        runtime.reincarnationRecoverTick = recoverTick;
        runtime.invulnerableUntilTick = Math.max(runtime.invulnerableUntilTick, recoverTick);
        return true;
    }

    public static boolean isActionLocked(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        return runtime != null
                && runtime.reincarnationRecoverTick > player.level().getGameTime();
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null || runtime.invulnerableUntilTick <= player.level().getGameTime()) {
            return true;
        }
        return source.typeHolder().is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /** Last stand deliberately runs before vanilla hand-held totem processing. */
    private static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, LAST_STAND_ID) <= 0) {
            return true;
        }

        long now = player.level().getGameTime();
        if (now < state.cooldownReadyTick(LAST_STAND_ID)) {
            return true;
        }

        CombatTuning.Snapshot tuning = CombatTuning.current();
        player.setHealth(1.0F);
        AugmentationPlayerStateService.replace(
                player,
                state.withCooldownReadyTick(LAST_STAND_ID, now + tuning.lastStandCooldownTicks())
        );
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        runtime.invulnerableUntilTick = now + tuning.lastStandInvulnerableTicks();
        return false;
    }

    private static void afterDamage(
            LivingEntity entity,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked
    ) {
        CombatExpansionService.afterDamage(entity, source, baseDamageTaken, damageTaken, blocked);

        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (CombatProvenanceContext.isDelayedDamage()) {
            return;
        }

        ProtectionAugmentService.afterDamage(player, source, damageTaken, blocked);
        if (blocked || !(damageTaken > 0.0F)) {
            return;
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, URGENT_STEPS_ID) <= 0) {
            return;
        }
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        long now = player.level().getGameTime();
        runtime.urgentUntilTick = Math.max(
                runtime.urgentUntilTick,
                now + CombatTuning.current().urgentStepsDurationTicks()
        );
        applyDynamicModifiers(player, state, runtime, now);
    }

    private static void afterKill(
            net.minecraft.server.level.ServerLevel level,
            Entity entity,
            LivingEntity killedEntity,
            DamageSource damageSource
    ) {
        CombatExpansionService.afterKill(level, entity, killedEntity, damageSource);
        if (!(entity instanceof ServerPlayer player) || !(killedEntity instanceof Enemy)) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        CombatTuning.Snapshot tuning = CombatTuning.current();

        int lifesteal = boundedLevel(state, LIFESTEAL_ID);
        if (lifesteal > 0) {
            AugmentHealingService.healPrimary(
                    player,
                    (float) (tuning.lifestealHealPerLevel() * lifesteal)
            );
        }

        int killStreak = boundedLevel(state, KILL_STREAK_ID);
        if (killStreak > 0) {
            RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
            long now = player.level().getGameTime();
            runtime.killStacks = Math.min(tuning.killStreakMaxStacks(), runtime.killStacks + 1);
            runtime.killStreakUntilTick = now + tuning.killStreakDurationTicks();
            applyDynamicModifiers(player, state, runtime, now);
        }
    }

    private static void tickRuntime(MinecraftServer server) {
        Iterator<Map.Entry<UUID, RuntimeState>> iterator = RUNTIME.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RuntimeState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isDeadOrDying()) {
                iterator.remove();
                continue;
            }

            RuntimeState runtime = entry.getValue();
            long now = player.level().getGameTime();
            boolean changed = false;
            if (runtime.killStacks > 0 && now >= runtime.killStreakUntilTick) {
                runtime.killStacks = 0;
                runtime.killStreakUntilTick = 0L;
                changed = true;
            }
            if (runtime.urgentUntilTick > 0L && now >= runtime.urgentUntilTick) {
                runtime.urgentUntilTick = 0L;
                changed = true;
            }
            if (runtime.reincarnationRecoverTick > now) {
                player.setDeltaMovement(Vec3.ZERO);
                player.setSprinting(false);
                player.stopUsingItem();
                player.resetFallDistance();
            } else if (runtime.reincarnationRecoverTick > 0L) {
                runtime.reincarnationRecoverTick = 0L;
                player.setHealth(player.getMaxHealth());
            }
            if (runtime.invulnerableUntilTick > 0L && now >= runtime.invulnerableUntilTick) {
                runtime.invulnerableUntilTick = 0L;
            }

            if (changed) {
                applyDynamicModifiers(
                        player,
                        AugmentationPlayerStateService.state(player),
                        runtime,
                        now
                );
            }
            if (!runtime.active()) {
                removeDynamicModifiers(player);
                iterator.remove();
            }
        }
    }

    private static boolean consumeEmergencyTotem(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || !stack.is(Items.TOTEM_OF_UNDYING)) {
                continue;
            }
            DeathProtection protection = stack.get(DataComponents.DEATH_PROTECTION);
            if (protection == null) {
                continue;
            }

            ItemStack protectionItem = stack.copy();
            stack.shrink(1);
            player.getInventory().setChanged();
            player.awardStat(Stats.ITEM_USED.get(protectionItem.getItem()));
            CriteriaTriggers.USED_TOTEM.trigger(player, protectionItem);
            protectionItem.causeUseVibration(player, GameEvent.ITEM_INTERACT_FINISH);
            player.setHealth(1.0F);
            protection.applyEffects(protectionItem, player);
            player.level().broadcastEntityEvent(player, (byte) 35);
            player.inventoryMenu.broadcastChanges();
            return true;
        }
        return false;
    }

    private static void clearRuntime(ServerPlayer player) {
        RUNTIME.remove(player.getUUID());
        removeDynamicModifiers(player);
    }

    private static void applyDynamicModifiers(
            ServerPlayer player,
            AugmentationPlayerState state,
            RuntimeState runtime,
            long now
    ) {
        CombatTuning.Snapshot tuning = CombatTuning.current();
        int killStreakLevel = boundedLevel(state, KILL_STREAK_ID);
        int urgentStepsLevel = boundedLevel(state, URGENT_STEPS_ID);

        double movement = 0.0D;
        double attackSpeed = 0.0D;
        if (runtime.killStacks > 0 && now < runtime.killStreakUntilTick && killStreakLevel > 0) {
            movement += tuning.killStreakMovementPerLevelPerStack()
                    * killStreakLevel * runtime.killStacks;
            attackSpeed += tuning.killStreakAttackSpeedPerLevelPerStack()
                    * killStreakLevel * runtime.killStacks;
        }
        if (runtime.urgentUntilTick > now && urgentStepsLevel > 0) {
            movement += tuning.urgentStepsMovementPerLevel() * urgentStepsLevel;
        }

        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), DYNAMIC_MOVEMENT_ID, movement);
        applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), DYNAMIC_ATTACK_SPEED_ID, attackSpeed);
    }

    private static void removeDynamicModifiers(ServerPlayer player) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(DYNAMIC_MOVEMENT_ID);
        }
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(DYNAMIC_ATTACK_SPEED_ID);
        }
    }

    private static void applyModifier(AttributeInstance instance, Identifier id, double amount) {
        if (instance == null) {
            return;
        }
        if (!(amount > 0.0D)) {
            instance.removeModifier(id);
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                id,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static float safeScale(float damage, double multiplier) {
        double scaled = damage * multiplier;
        if (!Double.isFinite(scaled)) {
            return damage;
        }
        return (float) Math.max(0.0D, Math.min(Float.MAX_VALUE, scaled));
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static final class RuntimeState {
        private int killStacks;
        private long killStreakUntilTick;
        private long urgentUntilTick;
        private long invulnerableUntilTick;
        private long reincarnationRecoverTick;

        private boolean active() {
            return killStacks > 0
                    || killStreakUntilTick > 0L
                    || urgentUntilTick > 0L
                    || invulnerableUntilTick > 0L
                    || reincarnationRecoverTick > 0L;
        }
    }
}
