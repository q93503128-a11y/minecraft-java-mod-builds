package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.AugmentHealingService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative shared active-ability engine.
 *
 * <p>The client requests only a slot index. Ownership, fallback slot binding, charges, cooldowns,
 * targeting and all gameplay results are decided here. The first six compatible actives are wired
 * now; the same engine is intentionally reusable by later complex active batches.</p>
 */
public final class ActiveAbilityService {
    private static final String CLEANSE_ID = id("cleanse");
    private static final String PULL_ID = id("pull");
    private static final String SWAP_ID = id("swap");
    private static final String SHIELD_ID = id("shield");
    private static final String BERSERK_ID = id("berserk");
    private static final String HEALING_WAVE_ID = id("healing_wave");
    private static final String ABILITY_HASTE_ID = id("ability_haste");
    private static final String DOUBLE_CHARGE_ID = id("double_charge");
    private static final String REPLAY_ID = id("replay");

    private static final List<String> SUPPORTED_ORDER = List.of(
            CLEANSE_ID,
            PULL_ID,
            SWAP_ID,
            SHIELD_ID,
            BERSERK_ID,
            HEALING_WAVE_ID
    );

    private static final Identifier BERSERK_ATTACK_SPEED_MODIFIER = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "active/berserk_attack_speed"
    );

    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

    private ActiveAbilityService() {
    }

    public static void initialize() {
        for (String augmentId : SUPPORTED_ORDER) {
            AugmentCatalog.require(augmentId);
        }
        ServerPlayerEvents.JOIN.register(ActiveAbilityService::clearRuntime);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> clearRuntime(newPlayer)
        );
        ServerTickEvents.END_SERVER_TICK.register(ActiveAbilityService::tickRuntime);
    }

    public static boolean tryUseSlot(ServerPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 10 || player.isDeadOrDying()
                || CombatAugmentService.isActionLocked(player)) {
            return false;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        String abilityId = resolveAbilityForSlot(state, slotIndex);
        if (abilityId == null) {
            return false;
        }
        return tryUse(player, state, abilityId);
    }

    public static double berserkOutgoingBonus(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null || runtime.berserkWindows.isEmpty()) {
            return 0.0D;
        }
        long now = player.level().getGameTime();
        double total = 0.0D;
        for (BerserkWindow window : runtime.berserkWindows) {
            if (window.expiresAtTick > now) {
                total += window.damageBonus;
            }
        }
        return total;
    }

    public static double berserkIncomingMultiplier(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null || runtime.berserkWindows.isEmpty()) {
            return 1.0D;
        }
        long now = player.level().getGameTime();
        double efficiencySum = 0.0D;
        for (BerserkWindow window : runtime.berserkWindows) {
            if (window.expiresAtTick > now) {
                efficiencySum += window.efficiency;
            }
        }
        return 1.0D + ExpansionTuning.current().berserkIncomingDamageBonus() * efficiencySum;
    }

    /** Combat Cycle calls this only for ROOT direct kills. */
    public static void reduceSupportedCooldowns(ServerPlayer player, int reductionTicks) {
        if (reductionTicks <= 0) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        long now = player.level().getGameTime();
        AugmentationPlayerState next = state;
        boolean doubleCharge = boundedLevel(state, DOUBLE_CHARGE_ID) > 0;

        for (String abilityId : SUPPORTED_ORDER) {
            int level = boundedLevel(state, abilityId);
            if (level <= 0 || baseCooldownTicks(abilityId, level) <= 0) {
                continue;
            }
            if (!doubleCharge) {
                long ready = next.cooldownReadyTick(abilityId);
                if (ready > now) {
                    next = next.withCooldownReadyTick(
                            abilityId,
                            Math.max(now, ready - reductionTicks)
                    );
                }
                continue;
            }

            List<String> keys = new ArrayList<>(List.of(chargeKey(abilityId, 0), chargeKey(abilityId, 1)));
            keys.sort(Comparator.comparingLong(next::cooldownReadyTick));
            int remaining = reductionTicks;
            for (String key : keys) {
                if (remaining <= 0) {
                    break;
                }
                long ready = next.cooldownReadyTick(key);
                if (ready <= now) {
                    continue;
                }
                long pending = ready - now;
                long applied = Math.min(pending, remaining);
                next = next.withCooldownReadyTick(key, ready - applied);
                remaining -= (int) applied;
            }
        }

        if (!next.equals(state)) {
            AugmentationPlayerStateService.replace(player, next);
        }
    }

    private static boolean tryUse(
            ServerPlayer player,
            AugmentationPlayerState state,
            String abilityId
    ) {
        int level = boundedLevel(state, abilityId);
        if (level <= 0 || !SUPPORTED_ORDER.contains(abilityId)) {
            return false;
        }

        int baseCooldown = baseCooldownTicks(abilityId, level);
        ChargeReservation reservation = reserveCharge(player, state, abilityId, baseCooldown);
        if (reservation == null) {
            return false;
        }

        if (!executeAbility(player, abilityId, level, 1.0D, false)) {
            return false;
        }

        if (reservation.key != null) {
            AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
            AugmentationPlayerStateService.replace(
                    player,
                    latest.withCooldownReadyTick(reservation.key, reservation.readyTick)
            );
        }

        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        if (boundedLevel(latest, REPLAY_ID) > 0 && replayCompatible(abilityId)) {
            RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
            runtime.replays.add(new ReplayAction(
                    player.level().getGameTime() + ExpansionTuning.current().replayDelayTicks(),
                    abilityId,
                    level,
                    ExpansionTuning.current().replayEfficiency()
            ));
        }
        return true;
    }

    private static boolean executeAbility(
            ServerPlayer player,
            String abilityId,
            int level,
            double efficiency,
            boolean replayInvocation
    ) {
        if (player.isDeadOrDying() || !(efficiency > 0.0D)) {
            return false;
        }
        if (abilityId.equals(CLEANSE_ID)) {
            return executeCleanse(player);
        }
        if (abilityId.equals(PULL_ID)) {
            return executePull(player, level, efficiency);
        }
        if (abilityId.equals(SWAP_ID)) {
            return !replayInvocation && executeSwap(player);
        }
        if (abilityId.equals(SHIELD_ID)) {
            float amount = (float) (CombatTuning.current().shieldAmount(level) * efficiency);
            if (!(amount > 0.0F)) {
                return false;
            }
            String pool = replayInvocation
                    ? ProtectionAugmentService.ACTIVE_SHIELD_POOL + "_replay"
                    : ProtectionAugmentService.ACTIVE_SHIELD_POOL;
            ProtectionAugmentService.grantShield(
                    player,
                    pool,
                    amount,
                    CombatTuning.current().shieldDurationTicks()
            );
            return true;
        }
        if (abilityId.equals(BERSERK_ID)) {
            ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
            RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
            runtime.berserkWindows.add(new BerserkWindow(
                    player.level().getGameTime() + tuning.berserkDurationTicks(),
                    tuning.berserkDamageBonus(level) * efficiency,
                    tuning.berserkAttackSpeedBonus(level) * efficiency,
                    efficiency
            ));
            applyBerserkAttackSpeed(player, runtime);
            return true;
        }
        if (abilityId.equals(HEALING_WAVE_ID)) {
            return executeHealingWave(player, level, efficiency);
        }
        return false;
    }

    private static boolean executeCleanse(ServerPlayer player) {
        List<Holder<MobEffect>> harmful = new ArrayList<>();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                harmful.add(instance.getEffect());
            }
        }
        if (harmful.isEmpty()) {
            return false;
        }
        for (Holder<MobEffect> effect : harmful) {
            player.removeEffect(effect);
        }
        return true;
    }

    private static boolean executePull(ServerPlayer player, int level, double efficiency) {
        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        double radius = tuning.pullRadius(level);
        double radiusSquared = radius * radius;
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> candidate instanceof Enemy
                        && candidate != player
                        && !candidate.isDeadOrDying()
                        && player.distanceToSqr(candidate) <= radiusSquared
        );
        if (targets.isEmpty()) {
            return false;
        }
        for (LivingEntity target : targets) {
            Vec3 delta = player.position().subtract(target.position());
            if (delta.lengthSqr() < 1.0E-6D) {
                continue;
            }
            double bossScalar = CombatExpansionService.isConfiguredBoss(target)
                    ? tuning.pullBossScalar()
                    : 1.0D;
            Vec3 impulse = delta.normalize().scale(tuning.pullSpeed() * efficiency * bossScalar);
            target.setDeltaMovement(target.getDeltaMovement().add(impulse));
        }
        return true;
    }

    private static boolean executeSwap(ServerPlayer player) {
        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        LivingEntity target = findLookTarget(player, tuning.swapRange());
        if (target == null) {
            return false;
        }

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        double targetX = target.getX();
        double targetY = target.getY();
        double targetZ = target.getZ();

        target.teleportTo(playerX, playerY, playerZ);
        player.teleportTo(targetX, targetY, targetZ);
        player.resetFallDistance();
        target.resetFallDistance();
        return true;
    }

    private static LivingEntity findLookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double rangeSquared = range * range;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                candidate -> candidate != player
                        && !candidate.isDeadOrDying()
                        && !(candidate instanceof ServerPlayer)
                        && player.distanceToSqr(candidate) <= rangeSquared
                        && player.hasLineOfSight(candidate)
        );
        for (LivingEntity candidate : nearby) {
            Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(eye);
            double along = toTarget.dot(look);
            if (!(along > 0.0D) || along > range) {
                continue;
            }
            double perpendicularSq = Math.max(0.0D, toTarget.lengthSqr() - along * along);
            double aimRadius = Math.max(0.75D, candidate.getBbWidth() * 0.75D);
            if (perpendicularSq > aimRadius * aimRadius) {
                continue;
            }
            double score = perpendicularSq + along * 1.0E-4D;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean executeHealingWave(ServerPlayer player, int level, double efficiency) {
        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        double radius = tuning.healingWaveRadius();
        double radiusSquared = radius * radius;
        double fraction = tuning.healingWaveFraction(level) * efficiency;
        if (!(fraction > 0.0D)) {
            return false;
        }

        AugmentHealingService.healPrimary(player, (float) (player.getMaxHealth() * fraction));
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> candidate != player
                        && !candidate.isDeadOrDying()
                        && player.distanceToSqr(candidate) <= radiusSquared
                        && AugmentHealingService.isHealingAlly(player, candidate)
        );
        for (LivingEntity target : nearby) {
            AugmentHealingService.healShared(target, (float) (target.getMaxHealth() * fraction));
        }
        return true;
    }

    private static ChargeReservation reserveCharge(
            ServerPlayer player,
            AugmentationPlayerState state,
            String abilityId,
            int baseCooldownTicks
    ) {
        if (baseCooldownTicks <= 0) {
            return new ChargeReservation(null, 0L);
        }
        long now = player.level().getGameTime();
        int level = boundedLevel(state, ABILITY_HASTE_ID);
        int effectiveCooldown = baseCooldownTicks;
        if (level > 0) {
            effectiveCooldown = Math.max(
                    1,
                    (int) Math.round(baseCooldownTicks * (1.0D - ExpansionTuning.current().abilityHasteReduction()))
            );
        }

        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            if (state.cooldownReadyTick(abilityId) > now) {
                return null;
            }
            return new ChargeReservation(abilityId, now + effectiveCooldown);
        }

        String first = chargeKey(abilityId, 0);
        String second = chargeKey(abilityId, 1);
        long firstReady = state.cooldownReadyTick(first);
        long secondReady = state.cooldownReadyTick(second);
        String selected;
        if (firstReady <= now) {
            selected = first;
        } else if (secondReady <= now) {
            selected = second;
        } else {
            return null;
        }
        long queueTail = Math.max(now, Math.max(firstReady, secondReady));
        return new ChargeReservation(selected, queueTail + effectiveCooldown);
    }

    private static int baseCooldownTicks(String abilityId, int level) {
        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        if (abilityId.equals(CLEANSE_ID)) {
            return tuning.cleanseCooldownTicks();
        }
        if (abilityId.equals(PULL_ID)) {
            return tuning.pullCooldownTicks(level);
        }
        if (abilityId.equals(SWAP_ID)) {
            return tuning.swapCooldownTicks();
        }
        if (abilityId.equals(SHIELD_ID)) {
            return tuning.shieldCooldownTicks(level);
        }
        if (abilityId.equals(BERSERK_ID)) {
            return tuning.berserkCooldownTicks(level);
        }
        if (abilityId.equals(HEALING_WAVE_ID)) {
            return tuning.healingWaveCooldownTicks(level);
        }
        return 0;
    }

    private static String resolveAbilityForSlot(AugmentationPlayerState state, int slotIndex) {
        List<String> bindings = new ArrayList<>(10);
        for (String configured : state.activeSlots()) {
            if (bindings.size() >= 10) {
                break;
            }
            if (SUPPORTED_ORDER.contains(configured)
                    && boundedLevel(state, configured) > 0
                    && !bindings.contains(configured)) {
                bindings.add(configured);
            }
        }
        for (String augmentId : SUPPORTED_ORDER) {
            if (bindings.size() >= 10) {
                break;
            }
            if (boundedLevel(state, augmentId) > 0 && !bindings.contains(augmentId)) {
                bindings.add(augmentId);
            }
        }
        return slotIndex < bindings.size() ? bindings.get(slotIndex) : null;
    }

    private static boolean replayCompatible(String abilityId) {
        return abilityId.equals(PULL_ID)
                || abilityId.equals(SHIELD_ID)
                || abilityId.equals(BERSERK_ID)
                || abilityId.equals(HEALING_WAVE_ID);
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
            boolean modifierChanged = runtime.berserkWindows.removeIf(window -> window.expiresAtTick <= now);

            Iterator<ReplayAction> replayIterator = runtime.replays.iterator();
            while (replayIterator.hasNext()) {
                ReplayAction replay = replayIterator.next();
                if (replay.dueTick > now) {
                    continue;
                }
                replayIterator.remove();
                executeAbility(player, replay.abilityId, replay.level, replay.efficiency, true);
                modifierChanged = true;
            }

            if (modifierChanged) {
                applyBerserkAttackSpeed(player, runtime);
            }
            if (runtime.berserkWindows.isEmpty() && runtime.replays.isEmpty()) {
                removeBerserkModifier(player);
                iterator.remove();
            }
        }
    }

    private static void applyBerserkAttackSpeed(ServerPlayer player, RuntimeState runtime) {
        long now = player.level().getGameTime();
        double amount = 0.0D;
        for (BerserkWindow window : runtime.berserkWindows) {
            if (window.expiresAtTick > now) {
                amount += window.attackSpeedBonus;
            }
        }
        AttributeInstance instance = player.getAttribute(Attributes.ATTACK_SPEED);
        if (instance == null) {
            return;
        }
        if (!(amount > 0.0D)) {
            instance.removeModifier(BERSERK_ATTACK_SPEED_MODIFIER);
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                BERSERK_ATTACK_SPEED_MODIFIER,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));
    }

    private static void clearRuntime(ServerPlayer player) {
        RUNTIME.remove(player.getUUID());
        removeBerserkModifier(player);
    }

    private static void removeBerserkModifier(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(Attributes.ATTACK_SPEED);
        if (instance != null) {
            instance.removeModifier(BERSERK_ATTACK_SPEED_MODIFIER);
        }
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String chargeKey(String abilityId, int index) {
        return abilityId + "#charge" + index;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static final class RuntimeState {
        private final List<BerserkWindow> berserkWindows = new ArrayList<>();
        private final List<ReplayAction> replays = new ArrayList<>();
    }

    private record BerserkWindow(
            long expiresAtTick,
            double damageBonus,
            double attackSpeedBonus,
            double efficiency
    ) {
    }

    private record ReplayAction(
            long dueTick,
            String abilityId,
            int level,
            double efficiency
    ) {
    }

    private record ChargeReservation(String key, long readyTick) {
    }
}
