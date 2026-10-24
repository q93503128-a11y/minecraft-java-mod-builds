package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityService;
import dev.moonseungjun.augmentationsurvival.core.BossCoreRewardCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;

/** Broad combat batch built on the existing provenance/damage/healing boundaries. */
public final class CombatExpansionService {
    private static final String BOSS_HUNTER_ID = id("boss_hunter");
    private static final String CHAIN_EXPLOSION_ID = id("chain_explosion");
    private static final String LIGHTNING_ID = id("lightning");
    private static final String ECHO_STRIKE_ID = id("echo_strike");
    private static final String WEAKNESS_MARK_ID = id("weakness_mark");
    private static final String SPEED_CONVERSION_ID = id("speed_conversion");
    private static final String HEAVY_STRIKE_ID = id("heavy_strike");
    private static final String LIFE_WAVE_ID = id("life_wave");
    private static final String BRAWL_ID = id("brawl");
    private static final String GLASS_CANNON_ID = id("glass_cannon");
    private static final String COMBAT_CYCLE_ID = id("combat_cycle");

    private static final Map<UUID, Integer> LIGHTNING_HITS = new HashMap<>();
    private static final Map<HitKey, HitCounter> ECHO_HITS = new HashMap<>();
    private static final Map<HitKey, HitCounter> WEAKNESS_BUILD = new HashMap<>();
    private static final Map<UUID, WeaknessMark> WEAKNESS_MARKS = new HashMap<>();

    private CombatExpansionService() {
    }

    public static void initialize() {
        ServerPlayerEvents.JOIN.register(CombatExpansionService::clearPlayerRuntime);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> clearPlayerRuntime(newPlayer)
        );
        ServerTickEvents.END_SERVER_TICK.register(CombatExpansionService::tickRuntime);
    }

    /** Adds root-direct offensive bonuses before vanilla armor/protection mitigation. */
    public static float modifyOutgoingDamage(LivingEntity target, DamageSource source, float damage) {
        if (!(damage > 0.0F) || !Float.isFinite(damage)) {
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
        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        double bonus = 0.0D;

        int bossHunter = boundedLevel(state, BOSS_HUNTER_ID);
        if (bossHunter > 0 && isConfiguredBoss(target)) {
            bonus += tuning.bossHunterDamageBonus(bossHunter);
        }

        int speedConversion = boundedLevel(state, SPEED_CONVERSION_ID);
        if (speedConversion > 0) {
            AttributeInstance movement = attacker.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement != null && movement.getBaseValue() > 0.0D) {
                double positiveRatio = Math.max(0.0D, movement.getValue() / movement.getBaseValue() - 1.0D);
                if (attacker.isSprinting()) {
                    positiveRatio = Math.max(0.0D, positiveRatio - tuning.sprintBonusExclusion());
                }
                double movementPercent = positiveRatio * 100.0D;
                bonus += Math.min(
                        tuning.speedConversionDamageCap(speedConversion),
                        movementPercent * tuning.speedConversionDamagePerMovementPercent(speedConversion)
                );
            }
        }

        int brawl = boundedLevel(state, BRAWL_ID);
        if (brawl > 0) {
            int enemies = countNearbyEnemies(attacker, tuning.brawlRadius(), tuning.brawlMaxTargets());
            bonus += tuning.brawlDamagePerEnemy(brawl) * enemies;
        }

        int glassCannon = boundedLevel(state, GLASS_CANNON_ID);
        if (glassCannon > 0) {
            bonus += tuning.glassCannonDamageBonus(glassCannon);
        }

        bonus += ActiveAbilityService.berserkOutgoingBonus(attacker);
        return safeScale(damage, 1.0D + bonus);
    }

    /** Final augment taken-damage adjustments before delayed split and custom protection pools. */
    public static float modifyFinalTakenDamage(LivingEntity victim, DamageSource source, float damage) {
        if (!(damage > 0.0F) || !Float.isFinite(damage)) {
            return damage;
        }
        double multiplier = 1.0D;

        if (victim instanceof ServerPlayer player) {
            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            int brawl = boundedLevel(state, BRAWL_ID);
            if (brawl > 0) {
                ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
                int enemies = countNearbyEnemies(player, tuning.brawlRadius(), tuning.brawlMaxTargets());
                multiplier *= Math.max(0.0D, 1.0D - tuning.brawlReductionPerEnemy(brawl) * enemies);
            }
            multiplier *= ActiveAbilityService.berserkIncomingMultiplier(player);
        }

        WeaknessMark mark = WEAKNESS_MARKS.get(victim.getUUID());
        if (mark != null && mark.expiresAtTick > victim.level().getGameTime()) {
            Entity dealer = source.getEntity();
            MinecraftServer server = victim.getServer();
            ServerPlayer owner = server == null ? null : server.getPlayerList().getPlayer(mark.ownerId);
            if (dealer != null && owner != null
                    && (dealer == owner || owner.isAlliedTo(dealer))) {
                multiplier *= 1.0D + ExpansionTuning.current().weaknessMarkDamageBonus(mark.level);
            }
        }
        return safeScale(damage, multiplier);
    }

    public static void afterDamage(
            LivingEntity target,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked
    ) {
        if (blocked || !(damageTaken > 0.0F) || !(source.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        CombatProvenance provenance = CombatProvenanceContext.resolve(source);
        if (!provenance.isRootDirect()) {
            return;
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(attacker);
        long now = attacker.level().getGameTime();
        updateWeaknessMark(attacker, target, state, now);
        triggerLightning(attacker, target, state, damageTaken);

        if (provenance == CombatProvenance.ROOT_MELEE
                && attacker.getAttackStrengthScale(0.5F) >= 0.90F) {
            triggerEchoStrike(attacker, target, state, baseDamageTaken, now);
            triggerHeavyStrike(attacker, target, state, now);
        }
    }

    public static void afterKill(
            ServerLevel level,
            Entity killer,
            LivingEntity killed,
            DamageSource source
    ) {
        removeTargetRuntime(killed.getUUID());
        if (!(killer instanceof ServerPlayer player) || !(killed instanceof Enemy)) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        triggerChainExplosion(level, player, killed, state);

        CombatProvenance provenance = CombatProvenanceContext.resolve(source);
        if (provenance.isRootDirect()) {
            int cycle = boundedLevel(state, COMBAT_CYCLE_ID);
            if (cycle > 0) {
                int reduction = 20 * cycle;
                if (isConfiguredBoss(killed)) {
                    reduction *= 5;
                }
                ActiveAbilityService.reduceSupportedCooldowns(player, reduction);
            }
        }
    }

    /** Hooked only from primary non-natural healing; shared heals deliberately do not recurse. */
    public static void onPrimaryHealing(ServerPlayer player, float actualHealing) {
        if (!(actualHealing > 0.0F) || !Float.isFinite(actualHealing)) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, LIFE_WAVE_ID);
        if (level <= 0) {
            return;
        }
        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        double radius = tuning.lifeWaveRadius();
        double radiusSquared = radius * radius;
        float damage = (float) Math.min(
                tuning.lifeWaveDamageCap(level),
                actualHealing * tuning.lifeWaveDamageMultiplier(level)
        );
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> candidate instanceof Enemy
                        && !candidate.isDeadOrDying()
                        && player.distanceToSqr(candidate) <= radiusSquared
        );
        for (LivingEntity target : targets) {
            applyDerivedDamage(player, target, damage, CombatProvenance.DERIVED_AOE);
        }
    }

    public static boolean isConfiguredBoss(LivingEntity entity) {
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return entityId != null && BossCoreRewardCatalog.exactAssignment(entityId).isPresent();
    }

    private static void triggerChainExplosion(
            ServerLevel level,
            ServerPlayer player,
            LivingEntity killed,
            AugmentationPlayerState state
    ) {
        int augmentLevel = boundedLevel(state, CHAIN_EXPLOSION_ID);
        if (augmentLevel <= 0) {
            return;
        }
        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        double radius = tuning.chainExplosionRadius(augmentLevel);
        double radiusSquared = radius * radius;
        float damage = (float) Math.min(
                tuning.chainExplosionDamageCap(augmentLevel),
                killed.getMaxHealth() * tuning.chainExplosionMaxHealthFraction(augmentLevel)
        );
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                killed.getBoundingBox().inflate(radius),
                candidate -> candidate != killed
                        && candidate instanceof Enemy
                        && !candidate.isDeadOrDying()
                        && killed.distanceToSqr(candidate) <= radiusSquared
        );
        for (LivingEntity target : targets) {
            applyDerivedDamage(player, target, damage, CombatProvenance.DERIVED_AOE);
        }
    }

    private static void triggerLightning(
            ServerPlayer attacker,
            LivingEntity primaryTarget,
            AugmentationPlayerState state,
            float actualDamage
    ) {
        int augmentLevel = boundedLevel(state, LIGHTNING_ID);
        if (augmentLevel <= 0) {
            return;
        }
        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        int hits = LIGHTNING_HITS.merge(attacker.getUUID(), 1, Integer::sum);
        int trigger = tuning.lightningTriggerHits(augmentLevel);
        if (hits < trigger) {
            return;
        }
        LIGHTNING_HITS.put(attacker.getUUID(), 0);

        double radius = tuning.lightningRadius();
        double radiusSquared = radius * radius;
        List<LivingEntity> candidates = attacker.level().getEntitiesOfClass(
                LivingEntity.class,
                primaryTarget.getBoundingBox().inflate(radius),
                candidate -> candidate != primaryTarget
                        && candidate instanceof Enemy
                        && !candidate.isDeadOrDying()
                        && primaryTarget.distanceToSqr(candidate) <= radiusSquared
        );
        candidates.sort(Comparator.comparingDouble(primaryTarget::distanceToSqr));
        int limit = Math.min(tuning.lightningTargetCount(augmentLevel), candidates.size());
        float damage = (float) (actualDamage * tuning.lightningDamageFraction(augmentLevel));
        for (int index = 0; index < limit; index++) {
            applyDerivedDamage(attacker, candidates.get(index), damage, CombatProvenance.DERIVED_CHAIN);
        }
    }

    private static void triggerEchoStrike(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            float baseDamageTaken,
            long now
    ) {
        int augmentLevel = boundedLevel(state, ECHO_STRIKE_ID);
        if (augmentLevel <= 0) {
            return;
        }
        HitKey key = new HitKey(attacker.getUUID(), target.getUUID());
        HitCounter counter = ECHO_HITS.computeIfAbsent(key, ignored -> new HitCounter());
        counter.count++;
        counter.lastTick = now;
        int trigger = ExpansionTuning.current().echoStrikeTriggerHits(augmentLevel);
        if (counter.count < trigger) {
            return;
        }
        counter.count = 0;
        float damage = (float) (baseDamageTaken
                * ExpansionTuning.current().echoStrikeDamageFraction(augmentLevel));
        applyDerivedDamage(attacker, target, damage, CombatProvenance.ECHO);
    }

    private static void triggerHeavyStrike(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            long now
    ) {
        int augmentLevel = boundedLevel(state, HEAVY_STRIKE_ID);
        if (augmentLevel <= 0 || now < state.cooldownReadyTick(HEAVY_STRIKE_ID)) {
            return;
        }
        float damage = (float) (attacker.getMaxHealth()
                * ExpansionTuning.current().heavyStrikeMaxHealthFraction(augmentLevel));
        AugmentationPlayerStateService.replace(
                attacker,
                state.withCooldownReadyTick(
                        HEAVY_STRIKE_ID,
                        now + ExpansionTuning.current().heavyStrikeCooldownTicks()
                )
        );
        applyDerivedDamage(attacker, target, damage, CombatProvenance.DERIVED_CHAIN);
    }

    private static void updateWeaknessMark(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            long now
    ) {
        int augmentLevel = boundedLevel(state, WEAKNESS_MARK_ID);
        if (augmentLevel <= 0) {
            return;
        }
        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        HitKey key = new HitKey(attacker.getUUID(), target.getUUID());
        HitCounter counter = WEAKNESS_BUILD.computeIfAbsent(key, ignored -> new HitCounter());
        if (counter.lastTick > 0L && now - counter.lastTick > tuning.weaknessMarkWindowTicks()) {
            counter.count = 0;
        }
        counter.count++;
        counter.lastTick = now;
        if (counter.count >= tuning.weaknessMarkRequiredHits()) {
            counter.count = 0;
            WEAKNESS_MARKS.put(
                    target.getUUID(),
                    new WeaknessMark(
                            attacker.getUUID(),
                            augmentLevel,
                            now + tuning.weaknessMarkDurationTicks()
                    )
            );
        }
    }

    private static void applyDerivedDamage(
            ServerPlayer owner,
            LivingEntity target,
            float damage,
            CombatProvenance provenance
    ) {
        if (!(damage > 0.0F) || !Float.isFinite(damage) || target.isDeadOrDying()) {
            return;
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = owner.damageSources().playerAttack(owner);
        CombatProvenanceContext.runAs(
                provenance,
                () -> target.hurtServer(level, source, damage)
        );
    }

    private static int countNearbyEnemies(ServerPlayer player, double radius, int cap) {
        double radiusSquared = radius * radius;
        int count = 0;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> candidate instanceof Enemy
                        && !candidate.isDeadOrDying()
                        && player.distanceToSqr(candidate) <= radiusSquared
        )) {
            count++;
            if (count >= cap) {
                return cap;
            }
        }
        return count;
    }

    private static void tickRuntime(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        WEAKNESS_MARKS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick <= now);
        long staleCutoff = now - 6000L;
        ECHO_HITS.entrySet().removeIf(entry -> entry.getValue().lastTick < staleCutoff);
        WEAKNESS_BUILD.entrySet().removeIf(entry -> entry.getValue().lastTick < staleCutoff);
    }

    private static void clearPlayerRuntime(ServerPlayer player) {
        UUID playerId = player.getUUID();
        LIGHTNING_HITS.remove(playerId);
        ECHO_HITS.keySet().removeIf(key -> key.attackerId.equals(playerId));
        WEAKNESS_BUILD.keySet().removeIf(key -> key.attackerId.equals(playerId));
    }

    private static void removeTargetRuntime(UUID targetId) {
        ECHO_HITS.keySet().removeIf(key -> key.targetId.equals(targetId));
        WEAKNESS_BUILD.keySet().removeIf(key -> key.targetId.equals(targetId));
        WEAKNESS_MARKS.remove(targetId);
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
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

    private record HitKey(UUID attackerId, UUID targetId) {
    }

    private static final class HitCounter {
        private int count;
        private long lastTick;
    }

    private record WeaknessMark(UUID ownerId, int level, long expiresAtTick) {
    }
}
