package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import dev.moonseungjun.openworldrpg.progression.reward.PlayerRewardTransactionService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-owned personal reward authority for bound R01 ordinary external enemies and Steelboar.
 * Offline players are intentionally not queued at ordinary kill resolution; already-committed
 * online plans remain reconnect-safe if delivery is interrupted.
 */
public final class R01OrdinaryEnemyRewardService {
    private R01OrdinaryEnemyRewardService() {}

    public static R01OrdinaryEnemyRewardState state(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getAttachedOrSet(
                R01OrdinaryEnemyRewardAttachments.ORDINARY_ENEMY_REWARDS,
                R01OrdinaryEnemyRewardState.initial()
        );
    }

    public static boolean recordDamageContribution(LivingEntity enemy, ServerPlayer player) {
        return recordValidatedContribution(enemy, player);
    }

    public static boolean recordValidatedSupportContribution(LivingEntity enemy, ServerPlayer player) {
        return recordValidatedContribution(enemy, player);
    }

    public static boolean resolveDefeat(LivingEntity rawEnemy) {
        Objects.requireNonNull(rawEnemy, "rawEnemy");
        LivingEntity enemy = rewardOwner(rawEnemy);
        Optional<R01OrdinaryEnemyRewardRules.EnemySource> sourceOptional = source(enemy);
        if (sourceOptional.isEmpty()
                || !ExternalActorBindingRuntime.ownsDamageAuthority(enemy)
                || !ExternalActorBindingRuntime.isAuthoredSpawn(enemy)
                || !(enemy.level() instanceof ServerLevel level)) {
            return false;
        }

        MinecraftServer server = level.getServer();
        String encounterId = encounterId(enemy);
        R01OrdinaryEnemyRewardState current = state(server);
        var snapshot = current.encounter(encounterId).orElse(null);
        if (snapshot == null) return false;

        RandomSource random = level.getRandom();
        Map<String, R01OrdinaryEnemyRewardRules.RewardPlan> onlinePlans = new HashMap<>();
        for (String playerUuid : snapshot.participants().keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(playerUuid));
            if (player == null) continue;
            onlinePlans.put(
                    playerUuid,
                    R01OrdinaryEnemyRewardRules.createPlan(snapshot.source(), randomRolls(random))
            );
        }

        replace(server, current.completeEncounter(encounterId, Map.copyOf(onlinePlans)));
        for (String playerUuid : onlinePlans.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(playerUuid));
            if (player != null) reconcilePendingFinalizations(player);
        }
        return true;
    }

    public static FinalizationSummary reconcilePendingFinalizations(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = player.level().getServer();
        if (server == null) return FinalizationSummary.none();

        int processed = 0;
        int completed = 0;
        int materialCapacityBlocked = 0;
        int equipmentBindingBlocked = 0;
        for (R01OrdinaryEnemyRewardState.PendingFinalization pending
                : state(server).pendingForPlayer(player.getUUID().toString())) {
            processed++;
            var source = pending.source();
            var plan = pending.plan();
            var progression = PlayerProgressionService.state(player);
            boolean progressionOrGoldDue =
                    progression.combatLevel()
                            < dev.moonseungjun.openworldrpg.progression.ProjectProgressionRules.MAX_COMBAT_LEVEL
                    || progression.classProgress(pending.rewardClass()).rank()
                            < dev.moonseungjun.openworldrpg.progression.ProjectProgressionRules.MAX_CLASS_RANK
                    || plan.gold() > 0L;
            if (progressionOrGoldDue) {
                PlayerRewardTransactionService.grantCombatPercentageRewardOnce(
                        player,
                        pending.rewardId() + "/progression",
                        pending.rewardClass(),
                        source.contentLevel(),
                        source.combatXpFraction(),
                        source.classXpFraction(),
                        plan.gold()
                );
            }

            boolean materialsReady = true;
            for (Map.Entry<String, Integer> material : plan.materials().entrySet()) {
                var result = PlayerInventoryService.deliverMaterialToPouchOnce(
                        player,
                        pending.rewardId() + "/material/" + material.getKey().substring(material.getKey().indexOf(':') + 1),
                        material.getKey(),
                        material.getValue()
                );
                materialsReady &= result.status() != PlayerInventoryState.MaterialDeliveryStatus.CAPACITY_BLOCKED;
            }

            boolean equipmentReady = plan.equipment().isEmpty();
            if (!materialsReady) materialCapacityBlocked++;
            if (!equipmentReady) equipmentBindingBlocked++;
            if (!materialsReady || !equipmentReady) continue;

            replace(server, state(server).clearPending(pending.rewardId()));
            PlayerInventoryService.clearCompletedDeliveryIdsWithPrefix(player, pending.rewardId() + "/material/");
            completed++;
        }
        return new FinalizationSummary(processed, completed, materialCapacityBlocked, equipmentBindingBlocked);
    }

    private static boolean recordValidatedContribution(LivingEntity rawEnemy, ServerPlayer player) {
        Objects.requireNonNull(rawEnemy, "rawEnemy");
        Objects.requireNonNull(player, "player");
        LivingEntity enemy = rewardOwner(rawEnemy);
        Optional<R01OrdinaryEnemyRewardRules.EnemySource> sourceOptional = source(enemy);
        if (sourceOptional.isEmpty()
                || !ExternalActorBindingRuntime.ownsDamageAuthority(enemy)
                || !ExternalActorBindingRuntime.isAuthoredSpawn(enemy)
                || !(enemy.level() instanceof ServerLevel level)
                || player.level() != level) {
            return false;
        }

        Optional<RootClass> activeClass = PlayerProgressionService.state(player).activeClass();
        if (activeClass.isEmpty()) return false;

        MinecraftServer server = level.getServer();
        String encounterId = encounterId(enemy);
        String playerUuid = player.getUUID().toString();
        R01OrdinaryEnemyRewardState current = state(server);
        boolean wasParticipant = current.hasParticipant(encounterId, playerUuid);
        replace(
                server,
                current.recordParticipation(
                        encounterId,
                        sourceOptional.orElseThrow(),
                        playerUuid,
                        activeClass.orElseThrow()
                )
        );
        return !wasParticipant;
    }

    private static LivingEntity rewardOwner(LivingEntity entity) {
        return ExternalActorBindingRuntime.damageAuthorityTarget(entity).orElse(entity);
    }

    private static Optional<R01OrdinaryEnemyRewardRules.EnemySource> source(LivingEntity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return R01OrdinaryEnemyRewardRules.sourceForEntityId(id == null ? null : id.toString());
    }

    private static String encounterId(LivingEntity enemy) {
        return "openworld_rpg:r01/ordinary_enemy/" + enemy.getUUID();
    }

    private static R01OrdinaryEnemyRewardRules.Rolls randomRolls(RandomSource random) {
        return new R01OrdinaryEnemyRewardRules.Rolls(
                random.nextInt(100),
                random.nextInt(100),
                random.nextInt(100),
                random.nextInt(6),
                random.nextInt(100),
                random.nextInt(R01OrdinaryEnemyRewardRules.STEELBOAR_EQUIPMENT_POOL.size()),
                random.nextInt(100),
                random.nextInt(100),
                random.nextInt(R01OrdinaryEnemyRewardRules.ARMOR_SLOTS.size()),
                random.nextLong()
        );
    }

    private static R01OrdinaryEnemyRewardState replace(MinecraftServer server, R01OrdinaryEnemyRewardState next) {
        ServerLevel overworld = server.overworld();
        R01OrdinaryEnemyRewardState current = state(server);
        if (current.equals(next)) return current;
        overworld.setAttached(R01OrdinaryEnemyRewardAttachments.ORDINARY_ENEMY_REWARDS, next);
        return next;
    }

    public record FinalizationSummary(
            int processed,
            int completed,
            int materialCapacityBlocked,
            int equipmentBindingBlocked
    ) {
        public FinalizationSummary {
            if (processed < 0 || completed < 0 || materialCapacityBlocked < 0 || equipmentBindingBlocked < 0
                    || completed > processed || materialCapacityBlocked > processed || equipmentBindingBlocked > processed) {
                throw new IllegalArgumentException("Invalid ordinary-enemy finalization summary.");
            }
        }
        public static FinalizationSummary none() { return new FinalizationSummary(0, 0, 0, 0); }
    }
}