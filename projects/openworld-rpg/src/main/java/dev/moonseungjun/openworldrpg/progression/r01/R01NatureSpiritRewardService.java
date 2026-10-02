package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
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
 * Server-authoritative participation and personal reward bridge for the R01 Nature Spirit elite.
 *
 * <p>One accepted damage/support contribution fixes the participant's reward class. Defeat moves
 * every eligible participant to a persisted personal plan before delivery, so reconnect cannot
 * reroll equipment/material results. Equipment materialization intentionally stays blocked while
 * the ordinary Exalted sell-value contract is missing; progression, Gold and material delivery are
 * already idempotent.</p>
 */
public final class R01NatureSpiritRewardService {
    private R01NatureSpiritRewardService() {
    }

    public static R01NatureSpiritRewardState state(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getAttachedOrSet(
                R01NatureSpiritRewardAttachments.NATURE_SPIRIT_REWARDS,
                R01NatureSpiritRewardState.initial()
        );
    }

    public static boolean recordDamageContribution(
            LivingEntity natureSpirit,
            ServerPlayer player
    ) {
        return recordValidatedContribution(natureSpirit, player);
    }

    /**
     * Future heal/barrier/control/revive adapters call this only after a real encounter-linked
     * support effect has already been accepted by its own server authority.
     */
    public static boolean recordValidatedSupportContribution(
            LivingEntity natureSpirit,
            ServerPlayer player
    ) {
        return recordValidatedContribution(natureSpirit, player);
    }

    public static boolean resolveDefeat(LivingEntity natureSpirit) {
        Objects.requireNonNull(natureSpirit, "natureSpirit");
        if (!isNatureSpirit(natureSpirit)
                || !ExternalActorBindingRuntime.isAuthoredSpawn(natureSpirit)
                || !(natureSpirit.level() instanceof ServerLevel level)) {
            return false;
        }

        MinecraftServer server = level.getServer();
        String encounterId = encounterId(natureSpirit);
        R01NatureSpiritRewardState current = state(server);
        var snapshot = current.encounter(encounterId).orElse(null);
        if (snapshot == null) {
            return false;
        }

        RandomSource random = level.getRandom();
        Map<String, R01NatureSpiritRewardRules.RewardPlan> plans =
                new HashMap<>();
        for (String playerUuid : snapshot.participants().keySet()) {
            plans.put(
                    playerUuid,
                    R01NatureSpiritRewardRules.createPlan(
                            random.nextInt(100),
                            random.nextInt(
                                    R01NatureSpiritRewardRules
                                            .EQUIPMENT_POOL
                                            .size()
                            ),
                            random.nextInt(100),
                            random.nextInt(100),
                            random.nextInt(
                                    R01NatureSpiritRewardRules
                                            .ARMOR_SLOTS
                                            .size()
                            ),
                            random.nextLong(),
                            random.nextInt(100),
                            random.nextInt(2),
                            random.nextInt(100)
                    )
            );
        }
        replace(
                server,
                current.completeEncounter(
                        encounterId,
                        Map.copyOf(plans)
                )
        );

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

    public static FinalizationSummary reconcilePendingFinalizations(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return FinalizationSummary.none();
        }

        int processed = 0;
        int completed = 0;
        int materialCapacityBlocked = 0;
        int equipmentBindingBlocked = 0;

        for (R01NatureSpiritRewardState.PendingFinalization pending
                : state(server).pendingForPlayer(
                        player.getUUID().toString()
                )) {
            processed++;
            R01RewardService.grantNatureSpiritCombatReward(
                    player,
                    pending.rewardId(),
                    pending.rewardClass()
            );

            boolean materialsReady = deliverMaterials(
                    player,
                    pending
            );
            boolean equipmentReady = pending.plan().equipment().isEmpty();

            if (!materialsReady) {
                materialCapacityBlocked++;
            }
            if (!equipmentReady) {
                equipmentBindingBlocked++;
            }
            if (!materialsReady || !equipmentReady) {
                continue;
            }

            replace(
                    server,
                    state(server).clearPending(
                            pending.rewardId()
                    )
            );
            PlayerInventoryService.clearCompletedDeliveryIdsWithPrefix(
                    player,
                    pending.rewardId() + "/material/"
            );
            completed++;
        }

        return new FinalizationSummary(
                processed,
                completed,
                materialCapacityBlocked,
                equipmentBindingBlocked
        );
    }

    private static boolean deliverMaterials(
            ServerPlayer player,
            R01NatureSpiritRewardState.PendingFinalization pending
    ) {
        boolean ready = true;
        int herbs = pending.plan().healingHerbQuantity();
        if (herbs > 0) {
            var result = PlayerInventoryService.deliverMaterialToPouchOnce(
                    player,
                    pending.rewardId() + "/material/healing_herb",
                    R01NatureSpiritRewardRules.HEALING_HERB_ID,
                    herbs
            );
            ready &= result.status()
                    != PlayerInventoryState.MaterialDeliveryStatus
                            .CAPACITY_BLOCKED;
        }

        int crystals = pending.plan().verdantCrystalQuantity();
        if (crystals > 0) {
            var result = PlayerInventoryService.deliverMaterialToPouchOnce(
                    player,
                    pending.rewardId() + "/material/verdant_crystal",
                    R01NatureSpiritRewardRules.VERDANT_CRYSTAL_ID,
                    crystals
            );
            ready &= result.status()
                    != PlayerInventoryState.MaterialDeliveryStatus
                            .CAPACITY_BLOCKED;
        }
        return ready;
    }

    private static boolean recordValidatedContribution(
            LivingEntity natureSpirit,
            ServerPlayer player
    ) {
        Objects.requireNonNull(natureSpirit, "natureSpirit");
        Objects.requireNonNull(player, "player");
        if (!isNatureSpirit(natureSpirit)
                || !ExternalActorBindingRuntime.isAuthoredSpawn(natureSpirit)
                || !(natureSpirit.level() instanceof ServerLevel level)
                || player.level() != level) {
            return false;
        }

        Optional<RootClass> activeClass =
                PlayerProgressionService.state(player).activeClass();
        if (activeClass.isEmpty()) {
            return false;
        }

        MinecraftServer server = level.getServer();
        String encounterId = encounterId(natureSpirit);
        String playerUuid = player.getUUID().toString();
        R01NatureSpiritRewardState current = state(server);
        boolean wasParticipant =
                current.hasParticipant(encounterId, playerUuid);
        replace(
                server,
                current.recordParticipation(
                        encounterId,
                        playerUuid,
                        activeClass.orElseThrow()
                )
        );
        return !wasParticipant;
    }

    private static boolean isNatureSpirit(LivingEntity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(
                entity.getType()
        );
        return id != null
                && R01ExternalActorCatalog.NATURE_SPIRIT
                        .equals(id.toString());
    }

    private static String encounterId(LivingEntity natureSpirit) {
        return "openworld_rpg:r01/nature_spirit/"
                + natureSpirit.getUUID();
    }

    private static R01NatureSpiritRewardState replace(
            MinecraftServer server,
            R01NatureSpiritRewardState next
    ) {
        ServerLevel overworld = server.overworld();
        R01NatureSpiritRewardState current = state(server);
        if (current.equals(next)) {
            return current;
        }
        overworld.setAttached(
                R01NatureSpiritRewardAttachments
                        .NATURE_SPIRIT_REWARDS,
                next
        );
        return next;
    }

    public record FinalizationSummary(
            int processed,
            int completed,
            int materialCapacityBlocked,
            int equipmentBindingBlocked
    ) {
        public FinalizationSummary {
            if (processed < 0
                    || completed < 0
                    || materialCapacityBlocked < 0
                    || equipmentBindingBlocked < 0
                    || completed > processed
                    || materialCapacityBlocked > processed
                    || equipmentBindingBlocked > processed) {
                throw new IllegalArgumentException(
                        "Invalid Nature Spirit finalization summary."
                );
            }
        }

        public static FinalizationSummary none() {
            return new FinalizationSummary(0, 0, 0, 0);
        }
    }
}
