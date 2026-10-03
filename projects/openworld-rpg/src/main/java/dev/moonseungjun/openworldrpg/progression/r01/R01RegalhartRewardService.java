package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
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
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-authoritative personal reward bridge for R01 Regalhart.
 *
 * <p>One accepted damage/support contribution fixes the participant's reward class. Defeat commits
 * each personal first/repeat RNG plan before any delivery. Progression, Gold and Regalhart Antlers
 * are currently deliverable idempotently. Ordinary equipment and Hartcrown Spear results remain
 * persisted until their complete materialization/runtime presentation path is accepted.</p>
 */
public final class R01RegalhartRewardService {
    private R01RegalhartRewardService() {
    }

    public static R01RegalhartRewardState state(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getAttachedOrSet(
                R01RegalhartRewardAttachments.REGALHART_REWARDS,
                R01RegalhartRewardState.initial()
        );
    }

    public static boolean recordDamageContribution(
            LivingEntity regalhart,
            ServerPlayer player
    ) {
        return recordValidatedContribution(regalhart, player);
    }

    public static boolean recordValidatedSupportContribution(
            LivingEntity regalhart,
            ServerPlayer player
    ) {
        return recordValidatedContribution(regalhart, player);
    }

    public static boolean resolveDefeat(LivingEntity regalhart) {
        Objects.requireNonNull(regalhart, "regalhart");
        if (!isRegalhart(regalhart)
                || !ExternalActorBindingRuntime.isAuthoredSpawn(regalhart)
                || !(regalhart.level() instanceof ServerLevel level)) {
            return false;
        }

        MinecraftServer server = level.getServer();
        String encounterId = encounterId(regalhart);
        R01RegalhartRewardState current = state(server);
        var snapshot = current.encounter(encounterId).orElse(null);
        if (snapshot == null) {
            return false;
        }

        Map<String, R01RegalhartRewardRules.RewardPlan> plans =
                new HashMap<>();
        for (String playerUuid : snapshot.participants().keySet()) {
            boolean firstEligible =
                    !current.firstDefeatCommitted(playerUuid);
            plans.put(
                    playerUuid,
                    createPlan(level, firstEligible)
            );
        }

        R01RegalhartRewardState committed =
                current.completeEncounter(
                        encounterId,
                        Map.copyOf(plans)
                );
        replace(server, committed);

        // Shared territory timing advances exactly once, only after all personal plans are durable.
        R01RegalhartTerritoryController.recordValidDefeat(server);

        for (String playerUuid : snapshot.participants().keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(
                    UUID.fromString(playerUuid)
            );
            if (player != null) {
                reconcilePendingFinalizations(player);
            }
        }
        return true;
    }

    public static boolean abortActiveEncounter(LivingEntity regalhart) {
        Objects.requireNonNull(regalhart, "regalhart");
        if (!isRegalhart(regalhart)
                || !ExternalActorBindingRuntime.isAuthoredSpawn(regalhart)
                || !(regalhart.level() instanceof ServerLevel level)) {
            return false;
        }

        MinecraftServer server = level.getServer();
        String encounterId = encounterId(regalhart);
        R01RegalhartRewardState current = state(server);
        if (current.encounter(encounterId).isEmpty()) {
            return false;
        }
        replace(
                server,
                current.abortEncounter(encounterId)
        );
        return true;
    }

    public static FinalizationSummary reconcilePendingFinalizations(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return FinalizationSummary.none();
        }

        int processed = 0;
        int materialCapacityBlocked = 0;
        int ordinaryEquipmentBindingBlocked = 0;
        int mythicBindingBlocked = 0;

        for (R01RegalhartRewardState.PendingFinalization pending
                : state(server).pendingForPlayer(
                        player.getUUID().toString()
                )) {
            processed++;
            R01RegalhartRewardRules.RewardPlan plan = pending.plan();

            PlayerRewardTransactionService.grantCombatPercentageRewardOnce(
                    player,
                    pending.rewardId() + "/progression",
                    pending.rewardClass(),
                    R01RegalhartRewardRules.CONTENT_LEVEL,
                    plan.combatXpFraction(),
                    plan.classXpFraction(),
                    plan.gold()
            );

            PlayerInventoryState.MaterialDeliveryResult antlers =
                    PlayerInventoryService.deliverMaterialToPouchOnce(
                            player,
                            pending.rewardId()
                                    + "/material/regalhart_antler",
                            R01RegalhartRewardRules.REGALHART_ANTLER_ID,
                            plan.antlerQuantity()
                    );
            if (antlers.status()
                    == PlayerInventoryState.MaterialDeliveryStatus
                            .CAPACITY_BLOCKED) {
                materialCapacityBlocked++;
            }

            // Every clear owns at least one normal equipment result. Its full RNG plan is durable,
            // but final item materialization remains gated by the shared equipment-value contract.
            ordinaryEquipmentBindingBlocked++;

            // A successful signature roll is also durable; physical Hartcrown delivery waits for
            // Hart's Momentum runtime plus accepted item presentation/materialization.
            if (plan.hartcrownSpearDrop()) {
                mythicBindingBlocked++;
            }
        }

        return new FinalizationSummary(
                processed,
                materialCapacityBlocked,
                ordinaryEquipmentBindingBlocked,
                mythicBindingBlocked
        );
    }

    private static R01RegalhartRewardRules.RewardPlan createPlan(
            ServerLevel level,
            boolean firstEligible
    ) {
        var random = level.getRandom();
        return R01RegalhartRewardRules.createPlan(
                firstEligible,
                random.nextInt(
                        R01RegalhartRewardRules.NORMAL_BASE_POOL.size()
                ),
                firstEligible
                        ? random.nextInt(55)
                        : random.nextInt(100),
                random.nextInt(100),
                random.nextInt(
                        R01RegalhartRewardRules.ARMOR_SLOTS.size()
                ),
                random.nextLong(),
                random.nextInt(100),
                random.nextInt(
                        R01RegalhartRewardRules.NORMAL_BASE_POOL.size()
                ),
                random.nextInt(100),
                random.nextInt(100),
                random.nextInt(
                        R01RegalhartRewardRules.ARMOR_SLOTS.size()
                ),
                random.nextLong(),
                random.nextInt(100)
        );
    }

    private static boolean recordValidatedContribution(
            LivingEntity regalhart,
            ServerPlayer player
    ) {
        Objects.requireNonNull(regalhart, "regalhart");
        Objects.requireNonNull(player, "player");
        if (!isRegalhart(regalhart)
                || !ExternalActorBindingRuntime.isAuthoredSpawn(regalhart)
                || !(regalhart.level() instanceof ServerLevel level)
                || player.level() != level) {
            return false;
        }

        Optional<RootClass> activeClass =
                PlayerProgressionService.state(player).activeClass();
        if (activeClass.isEmpty()) {
            return false;
        }

        MinecraftServer server = level.getServer();
        String encounterId = encounterId(regalhart);
        long cycleIndex =
                R01RegalhartTerritoryController.state(server).cycleIndex();
        String playerUuid = player.getUUID().toString();
        R01RegalhartRewardState current = state(server);
        boolean wasParticipant =
                current.hasParticipant(encounterId, playerUuid);
        replace(
                server,
                current.recordParticipation(
                        encounterId,
                        cycleIndex,
                        playerUuid,
                        activeClass.orElseThrow()
                )
        );
        return !wasParticipant;
    }

    private static boolean isRegalhart(LivingEntity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(
                entity.getType()
        );
        return id != null
                && R01ExternalActorCatalog.REGALHART
                        .equals(id.toString());
    }

    private static String encounterId(LivingEntity regalhart) {
        return "openworld_rpg:r01/regalhart/"
                + regalhart.getUUID();
    }

    private static R01RegalhartRewardState replace(
            MinecraftServer server,
            R01RegalhartRewardState next
    ) {
        ServerLevel overworld = server.overworld();
        R01RegalhartRewardState current = state(server);
        if (current.equals(next)) {
            return current;
        }
        overworld.setAttached(
                R01RegalhartRewardAttachments.REGALHART_REWARDS,
                next
        );
        return next;
    }

    public record FinalizationSummary(
            int processed,
            int materialCapacityBlocked,
            int ordinaryEquipmentBindingBlocked,
            int mythicBindingBlocked
    ) {
        public FinalizationSummary {
            if (processed < 0
                    || materialCapacityBlocked < 0
                    || ordinaryEquipmentBindingBlocked < 0
                    || mythicBindingBlocked < 0
                    || materialCapacityBlocked > processed
                    || ordinaryEquipmentBindingBlocked > processed
                    || mythicBindingBlocked > processed) {
                throw new IllegalArgumentException(
                        "Invalid Regalhart finalization summary."
                );
            }
        }

        public static FinalizationSummary none() {
            return new FinalizationSummary(0, 0, 0, 0);
        }
    }
}
