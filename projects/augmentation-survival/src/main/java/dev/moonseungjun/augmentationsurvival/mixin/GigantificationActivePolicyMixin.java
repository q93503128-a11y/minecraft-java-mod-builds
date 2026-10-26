package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldTuning;
import dev.moonseungjun.augmentationsurvival.augment.active.GigantificationPolicyState;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies Ability Haste, Double Charge, Combat Cycle and Overtime to the eligible Legendary active. */
@Mixin(value = AdvancedWorldAugmentService.class, remap = false)
abstract class GigantificationActivePolicyMixin {
    private static final String GIGANTIFICATION_ID = id("gigantification");
    private static final String ABILITY_HASTE_ID = id("ability_haste");
    private static final String DOUBLE_CHARGE_ID = id("double_charge");
    private static final String COMBAT_CYCLE_ID = id("combat_cycle");
    private static final String OVERTIME_ID = id("overtime");

    @Inject(method = "initialize", at = @At("TAIL"))
    private static void augmentationSurvival$registerGigantificationPolicy(CallbackInfo ci) {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(
                GigantificationActivePolicyMixin::augmentationSurvival$afterRootKill
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> GigantificationPolicyState.clear());
    }

    @Inject(method = "tryGigantification", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$prepareSharedCooldown(
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        GigantificationPolicyState.takeReservation(player.getUUID());
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, GIGANTIFICATION_ID) <= 0
                || AdvancedWorldAugmentService.gigantificationKnockbackMultiplier(player) > 1.0D) {
            return;
        }

        long now = player.level().getGameTime();
        int baseCooldown = AdvancedWorldTuning.current().gigantificationCooldownTicks();
        int effectiveCooldown = boundedLevel(state, ABILITY_HASTE_ID) > 0
                ? Math.max(1, (int) Math.round(
                        baseCooldown * (1.0D - ExpansionTuning.current().abilityHasteReduction())
                ))
                : baseCooldown;

        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            GigantificationPolicyState.putReservation(
                    player.getUUID(), GIGANTIFICATION_ID, now + effectiveCooldown, false
            );
            return;
        }

        String first = chargeKey(0);
        String second = chargeKey(1);
        long firstReady = state.cooldownReadyTick(first);
        long secondReady = state.cooldownReadyTick(second);
        String selected;
        if (firstReady <= now) selected = first;
        else if (secondReady <= now) selected = second;
        else {
            cir.setReturnValue(false);
            return;
        }

        long queueTail = Math.max(now, Math.max(firstReady, secondReady));
        GigantificationPolicyState.putReservation(
                player.getUUID(), selected, queueTail + effectiveCooldown, true
        );

        // The legacy implementation gates on the single cooldown key. Clear only that compatibility
        // key so the selected shared charge can decide eligibility; the authoritative charge is committed on success.
        if (state.cooldownReadyTick(GIGANTIFICATION_ID) > now) {
            AugmentationPlayerStateService.replace(
                    player,
                    state.withCooldownReadyTick(GIGANTIFICATION_ID, 0L)
            );
        }
    }

    @Inject(method = "tryGigantification", at = @At("RETURN"))
    private static void augmentationSurvival$commitSharedCooldown(
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        GigantificationPolicyState.Reservation reservation =
                GigantificationPolicyState.takeReservation(player.getUUID());
        if (reservation == null || !cir.getReturnValueZ()) return;

        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        if (reservation.doubleCharge()) {
            latest = latest.withCooldownReadyTick(GIGANTIFICATION_ID, 0L)
                    .withCooldownReadyTick(reservation.key(), reservation.readyTick());
        } else {
            latest = latest.withCooldownReadyTick(GIGANTIFICATION_ID, reservation.readyTick());
        }
        AugmentationPlayerStateService.replace(player, latest);

        Object giant = AdvancedWorldRuntimeAccessor.augmentationSurvival$getGiantStates().get(player.getUUID());
        if (giant instanceof GiantStateAccessor accessor) {
            long baseEnd = accessor.augmentationSurvival$getEndTick();
            long extra = Math.round(
                    AdvancedWorldTuning.current().gigantificationDurationTicks()
                            * AdvancedActiveTuning.current().overtimeMaximumExtensionFraction()
            );
            GigantificationPolicyState.recordMaximumEnd(player.getUUID(), baseEnd + extra);
        }
    }

    private static void augmentationSurvival$afterRootKill(
            ServerLevel level,
            Entity killer,
            LivingEntity killed,
            DamageSource source
    ) {
        if (!(killer instanceof ServerPlayer player)
                || !CombatProvenanceContext.resolve(source).isRootDirect()) {
            return;
        }
        boolean boss = EntityCompatibilityProfileService.isBoss(killed);
        if (!(killed instanceof Enemy) && !boss) return;

        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, GIGANTIFICATION_ID) <= 0) return;

        int cycle = boundedLevel(state, COMBAT_CYCLE_ID);
        if (cycle > 0) {
            int reduction = 20 * cycle * (boss ? 5 : 1);
            reduceCooldown(player, state, reduction);
            state = AugmentationPlayerStateService.state(player);
        }

        if (boundedLevel(state, OVERTIME_ID) <= 0) return;
        Object giant = AdvancedWorldRuntimeAccessor.augmentationSurvival$getGiantStates().get(player.getUUID());
        if (!(giant instanceof GiantStateAccessor accessor)) return;
        long now = level.getGameTime();
        long currentEnd = accessor.augmentationSurvival$getEndTick();
        if (currentEnd <= now) return;
        int extension = boss
                ? AdvancedActiveTuning.current().overtimeBossExtensionTicks()
                : AdvancedActiveTuning.current().overtimeNormalExtensionTicks();
        long fallbackMax = currentEnd + Math.round(
                AdvancedWorldTuning.current().gigantificationDurationTicks()
                        * AdvancedActiveTuning.current().overtimeMaximumExtensionFraction()
        );
        long maximum = GigantificationPolicyState.maximumEnd(player.getUUID(), fallbackMax);
        accessor.augmentationSurvival$setEndTick(Math.min(maximum, currentEnd + extension));
    }

    private static void reduceCooldown(ServerPlayer player, AugmentationPlayerState state, int ticks) {
        if (ticks <= 0) return;
        long now = player.level().getGameTime();
        AugmentationPlayerState next = state;
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            long ready = state.cooldownReadyTick(GIGANTIFICATION_ID);
            if (ready > now) {
                next = state.withCooldownReadyTick(GIGANTIFICATION_ID, Math.max(now, ready - ticks));
            }
        } else {
            String first = chargeKey(0);
            String second = chargeKey(1);
            String earlier = state.cooldownReadyTick(first) <= state.cooldownReadyTick(second) ? first : second;
            String later = earlier.equals(first) ? second : first;
            int remaining = ticks;
            for (String key : new String[]{earlier, later}) {
                if (remaining <= 0) break;
                long ready = next.cooldownReadyTick(key);
                if (ready <= now) continue;
                long applied = Math.min(ready - now, remaining);
                next = next.withCooldownReadyTick(key, ready - applied);
                remaining -= (int) applied;
            }
        }
        if (!next.equals(state)) AugmentationPlayerStateService.replace(player, next);
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String chargeKey(int index) {
        return GIGANTIFICATION_ID + "#charge" + index;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
