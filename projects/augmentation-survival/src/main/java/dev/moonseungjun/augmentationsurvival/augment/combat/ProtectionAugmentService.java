package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Shared post-mitigation protection runtime.
 *
 * <p>Ordering is: adaptation -> augment shield -> augment absorption -> delayed-damage split ->
 * vanilla absorption/HP. A shield must never reserve delayed HP damage for damage it already
 * absorbed. Delayed installments re-enter the normal hurt pipeline with explicit provenance, but
 * armor/Protection and delay splitting are skipped by the combat mixin.</p>
 */
public final class ProtectionAugmentService {
    public static final String OVERFLOW_HEALING_POOL = "overflow_healing";
    public static final String ACTIVE_SHIELD_POOL = "shield";

    private static final String DAMAGE_ADAPTATION_ID = id("damage_adaptation");
    private static final String DELAYED_DAMAGE_ID = id("delayed_damage");
    private static final String LAST_STAND_ID = id("last_stand");

    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

    private ProtectionAugmentService() {
    }

    public static void initialize() {
        ServerPlayerEvents.JOIN.register(ProtectionAugmentService::clearRuntime);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> clearRuntime(newPlayer)
        );
        ServerTickEvents.END_SERVER_TICK.register(ProtectionAugmentService::tickRuntime);
    }

    /** Applies layers that sit after ordinary augment reductions and before vanilla absorption. */
    public static float modifyAfterReductions(
            ServerPlayer player,
            DamageSource source,
            float damage
    ) {
        if (!(damage > 0.0F) || !Float.isFinite(damage)) {
            return damage;
        }
        if (CombatProvenanceContext.isDelayedDamage()) {
            return damage;
        }

        long now = player.level().getGameTime();
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        CombatTuning.Snapshot tuning = CombatTuning.current();
        RuntimeState runtime = RUNTIME.get(player.getUUID());

        int adaptationLevel = boundedLevel(state, DAMAGE_ADAPTATION_ID);
        if (adaptationLevel > 0 && runtime != null
                && now < runtime.adaptationUntilTick
                && damageFamily(source).equals(runtime.adaptationFamily)) {
            damage = safeScale(
                    damage,
                    1.0D - tuning.damageAdaptationReductionPerLevel() * adaptationLevel
            );
        }

        // Protection pools own the current hit before delayed damage is reserved. Otherwise an
        // absorbed hit can leak back into HP several seconds later after the shield has ended.
        damage = consumeProtectionPools(player, runtime, damage, now);
        if (!(damage > 0.0F)) {
            return 0.0F;
        }

        int delayedLevel = boundedLevel(state, DELAYED_DAMAGE_ID);
        if (delayedLevel > 0) {
            double fraction = tuning.delayedDamageFraction(delayedLevel);
            float delayedTotal = safeScale(damage, fraction);
            damage = Math.max(0.0F, damage - delayedTotal);
            if (delayedTotal > 0.0F) {
                RuntimeState target = runtime(player);
                float installment = delayedTotal / tuning.delayedDamageInstallmentCount();
                for (int index = 1; index <= tuning.delayedDamageInstallmentCount(); index++) {
                    target.delayed.add(new DelayedInstallment(
                            now + (long) tuning.delayedDamageIntervalTicks() * index,
                            installment,
                            source
                    ));
                }
            }
        }

        return damage;
    }

    /**
     * Called by the delayed-damage boundary after armor and Protection have been explicitly skipped.
     * Delayed installments only consume already-existing protection pools; they never adapt/re-delay.
     */
    public static float modifyDelayedDamage(ServerPlayer player, float reservedDamage) {
        if (!(reservedDamage > 0.0F) || !Float.isFinite(reservedDamage)) {
            return reservedDamage;
        }
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        return consumeProtectionPools(
                player,
                runtime,
                reservedDamage,
                player.level().getGameTime()
        );
    }

    /** Records the damage family only after a real non-delayed hit reaches the shared damage event. */
    public static void afterDamage(
            ServerPlayer player,
            DamageSource source,
            float damageTaken,
            boolean blocked
    ) {
        if (blocked || !(damageTaken > 0.0F) || CombatProvenanceContext.isDelayedDamage()) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, DAMAGE_ADAPTATION_ID) <= 0) {
            return;
        }
        RuntimeState runtime = runtime(player);
        runtime.adaptationFamily = damageFamily(source);
        runtime.adaptationUntilTick = player.level().getGameTime()
                + CombatTuning.current().damageAdaptationDurationTicks();
    }

    /** Grants/replaces a distinct active shield pool. */
    public static void grantShield(
            ServerPlayer player,
            String sourcePoolId,
            float amount,
            int durationTicks
    ) {
        if (!(amount > 0.0F) || !Float.isFinite(amount) || durationTicks <= 0) {
            return;
        }
        Objects.requireNonNull(sourcePoolId, "sourcePoolId");
        long expiresAt = player.level().getGameTime() + durationTicks;
        runtime(player).pools.put(
                poolKey(PoolKind.SHIELD, sourcePoolId),
                new ProtectionPool(sourcePoolId, PoolKind.SHIELD, amount, amount, expiresAt)
        );
    }

    /** Adds to a source-owned augment absorption pool without mixing it into vanilla absorption. */
    public static float grantAbsorption(
            ServerPlayer player,
            String sourcePoolId,
            float amount,
            float cap
    ) {
        if (!(amount > 0.0F) || !Float.isFinite(amount) || !(cap > 0.0F) || !Float.isFinite(cap)) {
            return 0.0F;
        }
        Objects.requireNonNull(sourcePoolId, "sourcePoolId");
        RuntimeState runtime = runtime(player);
        String key = poolKey(PoolKind.ABSORPTION, sourcePoolId);
        ProtectionPool existing = runtime.pools.get(key);
        float before = existing == null ? 0.0F : existing.amount;
        float next = Math.min(cap, before + amount);
        float effectiveAmount = existing == null
                ? next
                : Math.max(existing.effectiveAmount, next);
        runtime.pools.put(
                key,
                new ProtectionPool(
                        sourcePoolId,
                        PoolKind.ABSORPTION,
                        next,
                        effectiveAmount,
                        Long.MAX_VALUE
                )
        );
        return Math.max(0.0F, next - before);
    }

    public static float poolAmount(ServerPlayer player, PoolKind kind, String sourcePoolId) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null) {
            return 0.0F;
        }
        ProtectionPool pool = runtime.pools.get(poolKey(kind, sourcePoolId));
        return pool == null ? 0.0F : Math.max(0.0F, pool.amount);
    }

    private static float consumeProtectionPools(
            ServerPlayer player,
            RuntimeState runtime,
            float damage,
            long now
    ) {
        if (runtime == null || !(damage > 0.0F)) {
            return damage;
        }
        expirePools(player, runtime, now);
        damage = consumeKind(player, runtime, PoolKind.SHIELD, damage);
        damage = consumeKind(player, runtime, PoolKind.ABSORPTION, damage);
        return damage;
    }

    private static float consumeKind(
            ServerPlayer player,
            RuntimeState runtime,
            PoolKind kind,
            float damage
    ) {
        if (!(damage > 0.0F)) {
            return 0.0F;
        }
        Iterator<Map.Entry<String, ProtectionPool>> iterator = runtime.pools.entrySet().iterator();
        while (iterator.hasNext() && damage > 0.0F) {
            ProtectionPool pool = iterator.next().getValue();
            if (pool.kind != kind || !(pool.amount > 0.0F)) {
                continue;
            }
            float consumed = Math.min(damage, pool.amount);
            pool.amount -= consumed;
            damage -= consumed;
            if (!(pool.amount > 0.0F)) {
                iterator.remove();
                ReactiveCombatAugmentService.onProtectionPoolEnded(player, pool.effectiveAmount);
            }
        }
        return Math.max(0.0F, damage);
    }

    private static void tickRuntime(MinecraftServer server) {
        Iterator<Map.Entry<UUID, RuntimeState>> runtimeIterator = RUNTIME.entrySet().iterator();
        while (runtimeIterator.hasNext()) {
            Map.Entry<UUID, RuntimeState> entry = runtimeIterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                runtimeIterator.remove();
                continue;
            }

            RuntimeState runtime = entry.getValue();
            long now = player.level().getGameTime();
            expirePools(player, runtime, now);
            if (runtime.adaptationUntilTick > 0L && now >= runtime.adaptationUntilTick) {
                runtime.adaptationUntilTick = 0L;
                runtime.adaptationFamily = null;
            }

            if (player.isDeadOrDying()) {
                runtime.delayed.clear();
                runtime.pools.clear();
            } else if (!runtime.delayed.isEmpty()) {
                ServerLevel level = (ServerLevel) player.level();
                Iterator<DelayedInstallment> delayedIterator = runtime.delayed.iterator();
                while (delayedIterator.hasNext()) {
                    DelayedInstallment installment = delayedIterator.next();
                    if (installment.dueTick > now) {
                        continue;
                    }
                    delayedIterator.remove();
                    if (blocksDelayedInstallment(player, installment.source, now)) {
                        continue;
                    }
                    CombatProvenanceContext.callAsDelayed(
                            installment.amount,
                            () -> player.hurtServer(level, installment.source, installment.amount)
                    );
                    if (player.isDeadOrDying()) {
                        runtime.delayed.clear();
                        runtime.pools.clear();
                        break;
                    }
                }
            }

            if (!runtime.active()) {
                runtimeIterator.remove();
            }
        }
    }

    /**
     * Queued damage must re-check survival windows at execution time because Last Stand can trigger
     * after the reservation was created but before a later installment becomes due.
     */
    private static boolean blocksDelayedInstallment(
            ServerPlayer player,
            DamageSource source,
            long now
    ) {
        if (source.typeHolder().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (ReactiveCombatAugmentService.isInvulnerable(player)) {
            return true;
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, LAST_STAND_ID) <= 0) {
            return false;
        }
        CombatTuning.Snapshot tuning = CombatTuning.current();
        long readyTick = state.cooldownReadyTick(LAST_STAND_ID);
        if (readyTick <= now) {
            return false;
        }
        long triggeredAt = readyTick - tuning.lastStandCooldownTicks();
        return triggeredAt <= now && now < triggeredAt + tuning.lastStandInvulnerableTicks();
    }

    private static void expirePools(ServerPlayer player, RuntimeState runtime, long now) {
        Iterator<Map.Entry<String, ProtectionPool>> iterator = runtime.pools.entrySet().iterator();
        while (iterator.hasNext()) {
            ProtectionPool pool = iterator.next().getValue();
            if (pool.expiresAtTick > now) {
                continue;
            }
            iterator.remove();
            ReactiveCombatAugmentService.onProtectionPoolEnded(player, pool.effectiveAmount);
        }
    }

    private static void clearRuntime(ServerPlayer player) {
        RUNTIME.remove(player.getUUID());
    }

    private static RuntimeState runtime(ServerPlayer player) {
        return RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String damageFamily(DamageSource source) {
        return source.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(source.getMsgId());
    }

    private static String poolKey(PoolKind kind, String sourcePoolId) {
        return kind.name() + ":" + sourcePoolId;
    }

    private static float safeScale(float damage, double multiplier) {
        double scaled = damage * Math.max(0.0D, multiplier);
        if (!Double.isFinite(scaled)) {
            return damage;
        }
        return (float) Math.min(Float.MAX_VALUE, scaled);
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    public enum PoolKind {
        SHIELD,
        ABSORPTION
    }

    private static final class RuntimeState {
        private String adaptationFamily;
        private long adaptationUntilTick;
        private final List<DelayedInstallment> delayed = new ArrayList<>();
        private final LinkedHashMap<String, ProtectionPool> pools = new LinkedHashMap<>();

        private boolean active() {
            return adaptationUntilTick > 0L || !delayed.isEmpty() || !pools.isEmpty();
        }
    }

    private static final class ProtectionPool {
        private final String sourcePoolId;
        private final PoolKind kind;
        private float amount;
        private final float effectiveAmount;
        private final long expiresAtTick;

        private ProtectionPool(
                String sourcePoolId,
                PoolKind kind,
                float amount,
                float effectiveAmount,
                long expiresAtTick
        ) {
            this.sourcePoolId = sourcePoolId;
            this.kind = kind;
            this.amount = amount;
            this.effectiveAmount = effectiveAmount;
            this.expiresAtTick = expiresAtTick;
        }
    }

    private record DelayedInstallment(long dueTick, float amount, DamageSource source) {
        private DelayedInstallment {
            Objects.requireNonNull(source, "source");
        }
    }
}
