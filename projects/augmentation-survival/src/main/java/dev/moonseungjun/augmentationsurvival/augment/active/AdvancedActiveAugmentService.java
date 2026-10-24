package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Server-owned advanced active runtime: Rewind, Time Acceleration, Shield Throw and Overtime. */
public final class AdvancedActiveAugmentService {
    private static final String REWIND_ID = id("rewind");
    private static final String TIME_ACCELERATION_ID = id("time_acceleration");
    private static final String SHIELD_THROW_ID = id("shield_throw");
    private static final String OVERTIME_ID = id("overtime");
    private static final String ABILITY_HASTE_ID = id("ability_haste");
    private static final String DOUBLE_CHARGE_ID = id("double_charge");
    private static final String REPLAY_ID = id("replay");
    private static final String COMBAT_CYCLE_ID = id("combat_cycle");
    private static final List<String> COOLDOWN_ACTIVES = List.of(REWIND_ID, TIME_ACCELERATION_ID, SHIELD_THROW_ID);

    private static final Identifier TIME_MOVEMENT_MODIFIER = modifier("time_acceleration_movement");
    private static final Identifier TIME_ATTACK_SPEED_MODIFIER = modifier("time_acceleration_attack_speed");
    private static final Identifier TIME_MINING_MODIFIER = modifier("time_acceleration_mining");
    private static final Identifier BERSERK_OVERTIME_ATTACK_SPEED_MODIFIER = modifier("berserk_overtime_attack_speed");
    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

    private AdvancedActiveAugmentService() {
    }

    public static void initialize() {
        for (String augmentId : new String[]{REWIND_ID, TIME_ACCELERATION_ID, SHIELD_THROW_ID, OVERTIME_ID,
                ABILITY_HASTE_ID, DOUBLE_CHARGE_ID, REPLAY_ID, COMBAT_CYCLE_ID}) {
            AugmentCatalog.require(augmentId);
        }
        ServerPlayerEvents.JOIN.register(player -> {
            clearRuntime(player);
            reconcile(player, AugmentationPlayerStateService.state(player));
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            clearRuntime(newPlayer);
            reconcile(newPlayer, AugmentationPlayerStateService.state(newPlayer));
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (!blocked && damageTaken > 0.0F && entity instanceof ServerPlayer player) {
                cancelFocus(player);
            }
        });
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(AdvancedActiveAugmentService::afterKill);
        ServerTickEvents.END_SERVER_TICK.register(AdvancedActiveAugmentService::tickRuntime);
    }

    public static void reconcile(ServerPlayer player, AugmentationPlayerState state) {
        if (boundedLevel(state, REWIND_ID) > 0 || boundedLevel(state, TIME_ACCELERATION_ID) > 0) {
            RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        }
        if (boundedLevel(state, TIME_ACCELERATION_ID) <= 0) {
            RuntimeState runtime = RUNTIME.get(player.getUUID());
            if (runtime != null) {
                runtime.focus = null;
                runtime.timeAcceleration = null;
            }
            removeTimeAccelerationModifiers(player);
            syncTemporalState(player, false);
        }
    }

    public static boolean tryUse(ServerPlayer player, String abilityId) {
        if (REWIND_ID.equals(abilityId)) return tryRewind(player);
        if (TIME_ACCELERATION_ID.equals(abilityId)) return tryStartTimeAcceleration(player);
        if (SHIELD_THROW_ID.equals(abilityId)) return tryShieldThrow(player, 1.0D, true);
        return false;
    }

    public static void notifyDisruptingAction(ServerPlayer player) {
        cancelFocus(player);
    }

    public static boolean isTimeAccelerationActive(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        return runtime != null && runtime.timeAcceleration != null
                && runtime.timeAcceleration.endTick > player.level().getGameTime();
    }

    public static int modifyUseDuration(LivingEntity user, int original) {
        if (original <= 1) return original;
        boolean active = user instanceof ServerPlayer player
                ? isTimeAccelerationActive(player)
                : ClientTemporalState.timeAccelerationActive();
        if (!active) return original;
        return Math.max(1, (int) Math.ceil(original /
                AdvancedActiveTuning.current().timeAccelerationItemUseSpeedMultiplier()));
    }

    public static float modifyOwnedProjectileSpeed(ServerPlayer owner, float original) {
        if (!(original > 0.0F) || !isTimeAccelerationActive(owner)) return original;
        return (float) (original * AdvancedActiveTuning.current().timeAccelerationProjectileSpeedMultiplier());
    }

    /** Replay Berserk is deliberately excluded because Overtime extends manual-use instances. */
    public static void onLegacyBerserkStarted(ServerPlayer player, int level) {
        if (level <= 0) return;
        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        long now = player.level().getGameTime();
        long duration = tuning.berserkDurationTicks();
        long baseEnd = now + duration;
        long maxEnd = baseEnd + Math.round(duration * AdvancedActiveTuning.current().overtimeMaximumExtensionFraction());
        runtime(player).berserkWindows.add(new LegacyBerserkWindow(
                baseEnd, baseEnd, maxEnd,
                tuning.berserkDamageBonus(level),
                tuning.berserkAttackSpeedBonus(level),
                tuning.berserkIncomingDamageBonus()
        ));
    }

    public static double berserkOvertimeOutgoingBonus(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null) return 0.0D;
        long now = player.level().getGameTime();
        double result = 0.0D;
        for (LegacyBerserkWindow window : runtime.berserkWindows) {
            if (now >= window.baseEndTick && now < window.endTick) result += window.damageBonus;
        }
        return result;
    }

    public static double berserkOvertimeIncomingBonus(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null) return 0.0D;
        long now = player.level().getGameTime();
        double result = 0.0D;
        for (LegacyBerserkWindow window : runtime.berserkWindows) {
            if (now >= window.baseEndTick && now < window.endTick) result += window.incomingBonus;
        }
        return result;
    }

    private static boolean tryRewind(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, REWIND_ID) <= 0) return false;
        RuntimeState runtime = runtime(player);
        HistorySnapshot snapshot = findHistorySnapshot(runtime,
                player.level().getGameTime() - AdvancedActiveTuning.current().rewindHistoryTicks());
        if (snapshot == null) return false;
        ChargeReservation reservation = calculateReservation(player, state, REWIND_ID,
                AdvancedActiveTuning.current().rewindCooldownTicks());
        if (reservation == null) return false;

        MinecraftServer server = player.level().getServer();
        ServerLevel targetLevel = server.getLevel(snapshot.dimension);
        if (targetLevel == null) return false;
        ServerPlayer teleported = player.teleport(new TeleportTransition(
                targetLevel, snapshot.position, snapshot.velocity,
                player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
        if (teleported == null) return false;

        teleported.setHealth(Math.max(0.0001F, Math.min(teleported.getMaxHealth(), snapshot.health)));
        teleported.setAbsorptionAmount(Math.max(0.0F, snapshot.absorption));
        teleported.getFoodData().setFoodLevel(snapshot.foodLevel);
        teleported.getFoodData().setSaturation(snapshot.saturation);
        teleported.setAirSupply(snapshot.airSupply);
        teleported.setRemainingFireTicks(snapshot.fireTicks);
        teleported.removeAllEffects();
        for (MobEffectInstance effect : snapshot.effects) teleported.addEffect(new MobEffectInstance(effect));
        teleported.resetFallDistance();
        commitReservation(teleported, reservation);
        return true;
    }

    private static boolean tryStartTimeAcceleration(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, TIME_ACCELERATION_ID) <= 0) return false;
        RuntimeState runtime = runtime(player);
        long now = player.level().getGameTime();
        if (runtime.focus != null || (runtime.timeAcceleration != null && runtime.timeAcceleration.endTick > now)) {
            return false;
        }
        if (calculateReservation(player, state, TIME_ACCELERATION_ID,
                AdvancedActiveTuning.current().timeAccelerationCooldownTicks()) == null) {
            return false;
        }
        runtime.focus = new FocusChannel(player.level().dimension(), player.position(),
                now + AdvancedActiveTuning.current().timeAccelerationFocusTicks());
        return true;
    }

    private static boolean tryShieldThrow(ServerPlayer player, double efficiency, boolean manualUse) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, SHIELD_THROW_ID);
        if (level <= 0 || !(efficiency > 0.0D)) return false;
        ChargeReservation reservation = null;
        if (manualUse) {
            reservation = calculateReservation(player, state, SHIELD_THROW_ID,
                    AdvancedActiveTuning.current().shieldThrowCooldownTicks(level));
            if (reservation == null) return false;
        }

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack weapon;
        double rawDamage;
        if (off.is(Items.SHIELD)) {
            weapon = main;
            if (weapon.isEmpty()) return false;
            rawDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        } else if (main.is(Items.SHIELD)) {
            weapon = off;
            if (weapon.isEmpty()) return false;
            ItemAttributeModifiers modifiers = weapon.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.EMPTY);
            rawDamage = modifiers.compute(Attributes.ATTACK_DAMAGE, 1.0D, EquipmentSlot.MAINHAND);
        } else {
            return false;
        }
        if (!(rawDamage > 1.0D) || !Double.isFinite(rawDamage)) return false;

        AdvancedActiveTuning.Snapshot tuning = AdvancedActiveTuning.current();
        List<LivingEntity> targets = findShieldThrowTargets(player, tuning.shieldThrowRange(level),
                tuning.shieldThrowTargets(level));
        if (targets.isEmpty()) return false;
        float damage = (float) Math.min(Float.MAX_VALUE,
                rawDamage * tuning.shieldThrowDamageFraction(level) * efficiency);
        if (!(damage > 0.0F) || !Float.isFinite(damage)) return false;

        DamageSource source = player.damageSources().playerAttack(player);
        for (LivingEntity target : targets) {
            CombatProvenanceContext.runAs(CombatProvenance.ACTIVE_SPECIAL,
                    () -> target.hurtServer(player.level(), source, damage));
        }
        if (manualUse) {
            commitReservation(player, reservation);
            if (boundedLevel(AugmentationPlayerStateService.state(player), REPLAY_ID) > 0) {
                runtime(player).replays.add(new ShieldReplay(
                        player.level().getGameTime() + ExpansionTuning.current().replayDelayTicks(),
                        ExpansionTuning.current().replayEfficiency()));
            }
        }
        return true;
    }

    private static void afterKill(ServerLevel level, Entity killer, LivingEntity killed, DamageSource source) {
        if (!(killer instanceof ServerPlayer player)
                || (!(killed instanceof Enemy) && !EntityCompatibilityProfileService.isBoss(killed))
                || !CombatProvenanceContext.resolve(source).isRootDirect()) return;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        boolean boss = EntityCompatibilityProfileService.isBoss(killed);
        int cycle = boundedLevel(state, COMBAT_CYCLE_ID);
        if (cycle > 0) {
            int reduction = 20 * cycle * (boss ? 5 : 1);
            reduceAdvancedCooldowns(player, reduction);
        }
        if (boundedLevel(state, OVERTIME_ID) > 0) {
            extendSustainedActives(player, boss
                    ? AdvancedActiveTuning.current().overtimeBossExtensionTicks()
                    : AdvancedActiveTuning.current().overtimeNormalExtensionTicks());
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
            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            long now = player.level().getGameTime();

            if (boundedLevel(state, REWIND_ID) > 0) captureHistory(player, runtime, now);
            else runtime.history.clear();
            tickFocus(player, state, runtime, now);
            tickTimeAcceleration(player, runtime, now);
            runtime.berserkWindows.removeIf(window -> window.endTick <= now);
            applyBerserkOvertimeAttackSpeed(player, runtime, now);

            Iterator<ShieldReplay> replayIterator = runtime.replays.iterator();
            while (replayIterator.hasNext()) {
                ShieldReplay replay = replayIterator.next();
                if (replay.dueTick > now) continue;
                replayIterator.remove();
                tryShieldThrow(player, replay.efficiency, false);
            }

            if (boundedLevel(state, REWIND_ID) <= 0 && runtime.focus == null
                    && runtime.timeAcceleration == null && runtime.berserkWindows.isEmpty()
                    && runtime.replays.isEmpty()) {
                removeTimeAccelerationModifiers(player);
                removeBerserkOvertimeAttackSpeed(player);
                syncTemporalState(player, false);
                iterator.remove();
            }
        }
    }

    private static void tickFocus(ServerPlayer player, AugmentationPlayerState state, RuntimeState runtime, long now) {
        FocusChannel focus = runtime.focus;
        if (focus == null) return;
        if (!focus.dimension.equals(player.level().dimension())
                || focus.startPosition.distanceToSqr(player.position()) > 1.0E-6D
                || player.isUsingItem()) {
            runtime.focus = null;
            return;
        }
        if (now < focus.completeTick) return;
        runtime.focus = null;
        ChargeReservation reservation = calculateReservation(player, state, TIME_ACCELERATION_ID,
                AdvancedActiveTuning.current().timeAccelerationCooldownTicks());
        if (reservation == null) return;
        int duration = AdvancedActiveTuning.current().timeAccelerationDurationTicks();
        long baseEnd = now + duration;
        long maxEnd = baseEnd + Math.round(duration * AdvancedActiveTuning.current().overtimeMaximumExtensionFraction());
        runtime.timeAcceleration = new SustainedWindow(baseEnd, maxEnd);
        commitReservation(player, reservation);
        applyTimeAccelerationModifiers(player);
        syncTemporalState(player, true);
    }

    private static void tickTimeAcceleration(ServerPlayer player, RuntimeState runtime, long now) {
        if (runtime.timeAcceleration == null) {
            removeTimeAccelerationModifiers(player);
            return;
        }
        if (runtime.timeAcceleration.endTick <= now) {
            runtime.timeAcceleration = null;
            removeTimeAccelerationModifiers(player);
            syncTemporalState(player, false);
            return;
        }
        applyTimeAccelerationModifiers(player);
    }

    private static void extendSustainedActives(ServerPlayer player, int ticks) {
        if (ticks <= 0) return;
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null) return;
        long now = player.level().getGameTime();
        if (runtime.timeAcceleration != null && runtime.timeAcceleration.endTick > now) {
            runtime.timeAcceleration.endTick = Math.min(runtime.timeAcceleration.maxEndTick,
                    runtime.timeAcceleration.endTick + ticks);
        }
        for (LegacyBerserkWindow window : runtime.berserkWindows) {
            if (window.endTick > now) window.endTick = Math.min(window.maxEndTick, window.endTick + ticks);
        }
    }

    private static void reduceAdvancedCooldowns(ServerPlayer player, int ticks) {
        if (ticks <= 0) return;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        AugmentationPlayerState next = state;
        long now = player.level().getGameTime();
        boolean doubleCharge = boundedLevel(state, DOUBLE_CHARGE_ID) > 0;
        for (String abilityId : COOLDOWN_ACTIVES) {
            if (boundedLevel(state, abilityId) <= 0) continue;
            if (!doubleCharge) {
                long ready = next.cooldownReadyTick(abilityId);
                if (ready > now) next = next.withCooldownReadyTick(abilityId, Math.max(now, ready - ticks));
                continue;
            }
            String first = chargeKey(abilityId, 0);
            String second = chargeKey(abilityId, 1);
            String earlier = state.cooldownReadyTick(first) <= state.cooldownReadyTick(second) ? first : second;
            String later = earlier.equals(first) ? second : first;
            int remaining = ticks;
            for (String key : List.of(earlier, later)) {
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

    private static ChargeReservation calculateReservation(ServerPlayer player, AugmentationPlayerState state,
                                                           String abilityId, int baseCooldownTicks) {
        long now = player.level().getGameTime();
        int cooldown = baseCooldownTicks;
        if (boundedLevel(state, ABILITY_HASTE_ID) > 0) {
            cooldown = Math.max(1, (int) Math.round(baseCooldownTicks
                    * (1.0D - ExpansionTuning.current().abilityHasteReduction())));
        }
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            return state.cooldownReadyTick(abilityId) > now ? null : new ChargeReservation(abilityId, now + cooldown);
        }
        String first = chargeKey(abilityId, 0);
        String second = chargeKey(abilityId, 1);
        long firstReady = state.cooldownReadyTick(first);
        long secondReady = state.cooldownReadyTick(second);
        String selected;
        if (firstReady <= now) selected = first;
        else if (secondReady <= now) selected = second;
        else return null;
        long queueTail = Math.max(now, Math.max(firstReady, secondReady));
        return new ChargeReservation(selected, queueTail + cooldown);
    }

    private static void commitReservation(ServerPlayer player, ChargeReservation reservation) {
        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        AugmentationPlayerStateService.replace(player,
                latest.withCooldownReadyTick(reservation.key, reservation.readyTick));
    }

    private static void captureHistory(ServerPlayer player, RuntimeState runtime, long now) {
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) effects.add(new MobEffectInstance(effect));
        runtime.history.addLast(new HistorySnapshot(
                now, player.level().dimension(), player.position(), player.getDeltaMovement(),
                player.getHealth(), player.getAbsorptionAmount(), player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(), player.getAirSupply(), player.getRemainingFireTicks(), effects));
        long cutoff = now - AdvancedActiveTuning.current().rewindHistoryTicks() - 40L;
        while (!runtime.history.isEmpty() && runtime.history.peekFirst().tick < cutoff) runtime.history.removeFirst();
    }

    private static HistorySnapshot findHistorySnapshot(RuntimeState runtime, long targetTick) {
        HistorySnapshot selected = null;
        for (HistorySnapshot snapshot : runtime.history) {
            if (snapshot.tick > targetTick) break;
            selected = snapshot;
        }
        return selected;
    }

    private static List<LivingEntity> findShieldThrowTargets(ServerPlayer player, double range, int limit) {
        double rangeSquared = range * range;
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(range),
                candidate -> candidate != player && !candidate.isDeadOrDying()
                        && (candidate instanceof Enemy || EntityCompatibilityProfileService.isBoss(candidate))
                        && player.distanceToSqr(candidate) <= rangeSquared && player.hasLineOfSight(candidate));
        if (candidates.isEmpty()) return List.of();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity primary = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 delta = candidate.getBoundingBox().getCenter().subtract(eye);
            double along = delta.dot(look);
            if (along <= 0.0D || along > range) continue;
            double perpendicularSq = Math.max(0.0D, delta.lengthSqr() - along * along);
            double aimRadius = Math.max(1.0D, candidate.getBbWidth());
            if (perpendicularSq > aimRadius * aimRadius) continue;
            double score = perpendicularSq + along * 1.0E-4D;
            if (score < best) {
                best = score;
                primary = candidate;
            }
        }
        if (primary == null) return List.of();
        List<LivingEntity> result = new ArrayList<>();
        result.add(primary);
        LivingEntity cursor = primary;
        while (result.size() < limit) {
            LivingEntity from = cursor;
            LivingEntity next = candidates.stream().filter(candidate -> !result.contains(candidate))
                    .min(Comparator.comparingDouble(from::distanceToSqr)).orElse(null);
            if (next == null) break;
            result.add(next);
            cursor = next;
        }
        return result;
    }

    private static void applyTimeAccelerationModifiers(ServerPlayer player) {
        AdvancedActiveTuning.Snapshot tuning = AdvancedActiveTuning.current();
        applyAttributeModifier(player, Attributes.MOVEMENT_SPEED, TIME_MOVEMENT_MODIFIER,
                tuning.timeAccelerationMovementBonus());
        applyAttributeModifier(player, Attributes.ATTACK_SPEED, TIME_ATTACK_SPEED_MODIFIER,
                tuning.timeAccelerationAttackSpeedBonus());
        applyAttributeModifier(player, Attributes.BLOCK_BREAK_SPEED, TIME_MINING_MODIFIER,
                tuning.timeAccelerationMiningBonus());
    }

    private static void removeTimeAccelerationModifiers(ServerPlayer player) {
        removeModifier(player, Attributes.MOVEMENT_SPEED, TIME_MOVEMENT_MODIFIER);
        removeModifier(player, Attributes.ATTACK_SPEED, TIME_ATTACK_SPEED_MODIFIER);
        removeModifier(player, Attributes.BLOCK_BREAK_SPEED, TIME_MINING_MODIFIER);
    }

    private static void applyBerserkOvertimeAttackSpeed(ServerPlayer player, RuntimeState runtime, long now) {
        double amount = 0.0D;
        for (LegacyBerserkWindow window : runtime.berserkWindows) {
            if (now >= window.baseEndTick && now < window.endTick) amount += window.attackSpeedBonus;
        }
        AttributeInstance instance = player.getAttribute(Attributes.ATTACK_SPEED);
        if (instance == null) return;
        if (!(amount > 0.0D)) {
            instance.removeModifier(BERSERK_OVERTIME_ATTACK_SPEED_MODIFIER);
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                BERSERK_OVERTIME_ATTACK_SPEED_MODIFIER, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void removeBerserkOvertimeAttackSpeed(ServerPlayer player) {
        removeModifier(player, Attributes.ATTACK_SPEED, BERSERK_OVERTIME_ATTACK_SPEED_MODIFIER);
    }

    private static void applyAttributeModifier(ServerPlayer player, Holder<Attribute> attribute,
                                               Identifier identifier, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.addOrUpdateTransientModifier(new AttributeModifier(
                identifier, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void removeModifier(ServerPlayer player, Holder<Attribute> attribute, Identifier identifier) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(identifier);
    }

    private static void cancelFocus(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime != null) runtime.focus = null;
    }

    private static void clearRuntime(ServerPlayer player) {
        RUNTIME.remove(player.getUUID());
        removeTimeAccelerationModifiers(player);
        removeBerserkOvertimeAttackSpeed(player);
        syncTemporalState(player, false);
    }

    private static void syncTemporalState(ServerPlayer player, boolean active) {
        if (player.connection != null) TemporalStateNetworking.send(player, active);
    }

    private static RuntimeState runtime(ServerPlayer player) {
        return RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String chargeKey(String abilityId, int index) {
        return abilityId + "#charge" + index;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static Identifier modifier(String path) {
        return Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "active/" + path);
    }

    private static final class RuntimeState {
        final Deque<HistorySnapshot> history = new ArrayDeque<>();
        final List<LegacyBerserkWindow> berserkWindows = new ArrayList<>();
        final List<ShieldReplay> replays = new ArrayList<>();
        FocusChannel focus;
        SustainedWindow timeAcceleration;
    }

    private static final class SustainedWindow {
        long endTick;
        final long maxEndTick;
        SustainedWindow(long endTick, long maxEndTick) {
            this.endTick = endTick;
            this.maxEndTick = maxEndTick;
        }
    }

    private static final class LegacyBerserkWindow {
        final long baseEndTick;
        long endTick;
        final long maxEndTick;
        final double damageBonus;
        final double attackSpeedBonus;
        final double incomingBonus;
        LegacyBerserkWindow(long baseEndTick, long endTick, long maxEndTick,
                            double damageBonus, double attackSpeedBonus, double incomingBonus) {
            this.baseEndTick = baseEndTick;
            this.endTick = endTick;
            this.maxEndTick = maxEndTick;
            this.damageBonus = damageBonus;
            this.attackSpeedBonus = attackSpeedBonus;
            this.incomingBonus = incomingBonus;
        }
    }

    private record FocusChannel(ResourceKey<Level> dimension, Vec3 startPosition, long completeTick) {}
    private record HistorySnapshot(long tick, ResourceKey<Level> dimension, Vec3 position, Vec3 velocity,
                                   float health, float absorption, int foodLevel, float saturation,
                                   int airSupply, int fireTicks, List<MobEffectInstance> effects) {}
    private record ShieldReplay(long dueTick, double efficiency) {}
    private record ChargeReservation(String key, long readyTick) {}
}
