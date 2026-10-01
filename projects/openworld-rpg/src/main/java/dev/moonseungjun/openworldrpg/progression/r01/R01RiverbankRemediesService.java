package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import dev.moonseungjun.openworldrpg.market.R01FixedMerchantService;
import dev.moonseungjun.openworldrpg.progression.reward.PlayerRewardTransactionService;
import dev.moonseungjun.openworldrpg.recovery.RecoveryConsumable;
import dev.moonseungjun.openworldrpg.world.structure.R01AlderfordRuntimeBindingRegistry;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned Riverbank Remedies contract and reserved-Healing-Herb boundary. */
public final class R01RiverbankRemediesService {
    public static final String MATERIAL_TRANSACTION =
            "openworld_rpg:r01/riverbank_remedies/turn_in_material";
    public static final String REWARD_TRANSACTION =
            "openworld_rpg:r01/riverbank_remedies/reward";
    public static final String POTION_TRANSACTION =
            "openworld_rpg:r01/riverbank_remedies/healing_potion";
    public static final String HERBALISM_BONUS_FLAG =
            "openworld_rpg:r01/mastery/riverbank_remedies";
    public static final int REQUIRED_HERBS = 3;

    private R01RiverbankRemediesService() {
    }

    public static AcceptStatus accept(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01PlayerState before = R01PlayerStateService.state(player);
        if (!before.opening().firstShrineActivated()) {
            return AcceptStatus.NOT_AVAILABLE;
        }
        if (before.riverbankRemediesState().equals("completed")) {
            return AcceptStatus.ALREADY_COMPLETED;
        }
        if (before.riverbankRemediesState().equals("active")
                || before.riverbankRemediesState().equals("return")
                || before.riverbankRemediesState().equals("turn_in_pending")) {
            return AcceptStatus.ALREADY_ACTIVE;
        }
        R01PlayerStateService.acceptRiverbankRemedies(player);
        return AcceptStatus.ACCEPTED;
    }

    public static AbandonStatus abandon(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01PlayerState before = R01PlayerStateService.state(player);
        if (before.riverbankRemediesState().equals("completed")) {
            return AbandonStatus.ALREADY_COMPLETED;
        }
        if (before.riverbankRemediesState().equals("turn_in_pending")) {
            return AbandonStatus.TURN_IN_COMMITTED;
        }
        if (!before.riverbankRemediesState().equals("active")
                && !before.riverbankRemediesState().equals("return")) {
            return AbandonStatus.NOT_ACTIVE;
        }
        R01PlayerStateService.abandonRiverbankRemedies(player);
        return AbandonStatus.ABANDONED;
    }

    public static int recordHealingHerbHarvest(
            ServerPlayer player,
            String harvestTransactionId,
            int quantity
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(harvestTransactionId, "harvestTransactionId");
        R01PlayerState before = R01PlayerStateService.state(player);
        int beforeReserved = before.riverbankReservedHealingHerbs();
        R01PlayerState after =
                R01PlayerStateService.recordRiverbankHealingHerbGather(
                        player,
                        harvestTransactionId,
                        quantity
                );
        return Math.max(
                0,
                after.riverbankReservedHealingHerbs() - beforeReserved
        );
    }

    public static TurnInResult turnIn(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01PlayerState current = R01PlayerStateService.state(player);
        if (current.riverbankRemediesState().equals("completed")) {
            return new TurnInResult(TurnInStatus.ALREADY_COMPLETED, false);
        }
        if (current.riverbankRemediesState().equals("turn_in_pending")) {
            return executePending(player, true);
        }
        if (R01AlderfordRuntimeBindingRegistry.productionService(
                R01FixedMerchantService.LYSA_SERVICE_ID
        ).isEmpty()) {
            return new TurnInResult(
                    TurnInStatus.SERVICE_NOT_PRODUCTION,
                    false
            );
        }
        if (!current.riverbankRemediesState().equals("return")
                || current.riverbankReservedHealingHerbs() < REQUIRED_HERBS) {
            return new TurnInResult(TurnInStatus.NOT_READY, false);
        }

        RootClass rewardClass = PlayerProgressionService.state(player)
                .activeClass()
                .orElse(null);
        if (rewardClass == null) {
            return new TurnInResult(TurnInStatus.NO_ACTIVE_CLASS, false);
        }
        if (PlayerInventoryService.state(player).materialPouch()
                .getOrDefault(R01GatheringRules.HEALING_HERB, 0)
                < REQUIRED_HERBS) {
            return new TurnInResult(TurnInStatus.NOT_READY, false);
        }

        R01PlayerStateService.beginRiverbankRemediesTurnIn(
                player,
                rewardClass.name().toLowerCase(Locale.ROOT)
        );
        return executePending(player, false);
    }

    public static TurnInResult reconcilePending(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!R01PlayerStateService.state(player)
                .riverbankRemediesState()
                .equals("turn_in_pending")) {
            return new TurnInResult(TurnInStatus.NOTHING_TO_RECONCILE, false);
        }
        return executePending(player, true);
    }

    public static int reservedHealingHerbs(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return R01PlayerStateService.state(player)
                .riverbankReservedHealingHerbs();
    }

    public static Map<String, Integer> protectedPouchCounts(
            ServerPlayer player
    ) {
        int protectedHerbs = reservedHealingHerbs(player);
        if (protectedHerbs <= 0) {
            return Map.of();
        }
        return Map.of(R01GatheringRules.HEALING_HERB, protectedHerbs);
    }

    public static int sellablePouchCount(
            ServerPlayer player,
            String materialId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(materialId, "materialId");
        int owned = PlayerInventoryService.state(player).materialPouch()
                .getOrDefault(materialId, 0);
        return Math.max(
                0,
                owned - protectedPouchCounts(player).getOrDefault(materialId, 0)
        );
    }

    private static TurnInResult executePending(
            ServerPlayer player,
            boolean recovery
    ) {
        R01PlayerState state = R01PlayerStateService.state(player);
        String rewardClassId = state.riverbankRewardClassId()
                .orElseThrow(() -> new IllegalStateException(
                        "Pending Riverbank Remedies turn-in has no reward class."
                ));
        RootClass rewardClass;
        try {
            rewardClass = RootClass.valueOf(
                    rewardClassId.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Unknown persisted Riverbank Remedies reward class: "
                            + rewardClassId,
                    exception
            );
        }

        PlayerInventoryState.MaterialsConsumeOnceResult material =
                PlayerInventoryService.consumeMaterialsOnce(
                        player,
                        MATERIAL_TRANSACTION,
                        Map.of(
                                R01GatheringRules.HEALING_HERB,
                                REQUIRED_HERBS
                        ),
                        false
                );
        if (!material.consumed()) {
            return new TurnInResult(
                    TurnInStatus.RECOVERY_WAITING_FOR_HERBS,
                    false
            );
        }

        PlayerRewardTransactionService.grantPercentageRewardOnce(
                player,
                REWARD_TRANSACTION,
                rewardClass,
                0.40,
                0.30,
                60L
        );

        PlayerInventoryState.DeliveryResult potion =
                PlayerInventoryService.deliverImportantOnce(
                        player,
                        POTION_TRANSACTION,
                        ProjectInventoryItem.recovery(
                                "openworld_rpg:healing_potion",
                                RecoveryConsumable.HEALING_POTION,
                                1,
                                0L
                        )
                );

        R01GatheringService.awardMasteryBonusOnce(
                player,
                R01GatheringRules.GatheringDiscipline.HERBALISM,
                HERBALISM_BONUS_FLAG,
                10
        );
        R01PlayerStateService.completeRiverbankRemedies(player);

        boolean pendingClaim = potion.status()
                == PlayerInventoryState.DeliveryStatus.PENDING
                || potion.status()
                == PlayerInventoryState.DeliveryStatus.STILL_PENDING;
        return new TurnInResult(
                pendingClaim
                        ? TurnInStatus.COMPLETED_PENDING_REWARD
                        : recovery
                                ? TurnInStatus.COMPLETED_RECOVERED
                                : TurnInStatus.COMPLETED,
                pendingClaim
        );
    }

    public enum AcceptStatus {
        ACCEPTED,
        ALREADY_ACTIVE,
        ALREADY_COMPLETED,
        NOT_AVAILABLE
    }

    public enum AbandonStatus {
        ABANDONED,
        NOT_ACTIVE,
        TURN_IN_COMMITTED,
        ALREADY_COMPLETED
    }

    public enum TurnInStatus {
        COMPLETED,
        COMPLETED_RECOVERED,
        COMPLETED_PENDING_REWARD,
        ALREADY_COMPLETED,
        SERVICE_NOT_PRODUCTION,
        NOT_READY,
        NO_ACTIVE_CLASS,
        RECOVERY_WAITING_FOR_HERBS,
        NOTHING_TO_RECONCILE
    }

    public record TurnInResult(
            TurnInStatus status,
            boolean rewardPendingClaim
    ) {
        public TurnInResult {
            Objects.requireNonNull(status, "status");
        }
    }
}
