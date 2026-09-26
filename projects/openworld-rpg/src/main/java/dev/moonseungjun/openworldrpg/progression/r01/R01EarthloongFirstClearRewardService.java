package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/**
 * Reconnect-safe server transaction boundary for the Earthloong first-clear material and
 * deterministic weapon choice.
 *
 * <p>Choice identity is committed to personal R01 state before item delivery. A crash may retry
 * the same choice, but can never reopen a different second choice. Final Lucifer UI/model preview
 * remains presentation-gated and is not faked here.</p>
 */
public final class R01EarthloongFirstClearRewardService {
    public static final String CHOICE_DELIVERY_TRANSACTION =
            "openworld_rpg:r01/earthloong_first_clear/choice_item";
    public static final String SCALE_DELIVERY_TRANSACTION =
            "openworld_rpg:r01/earthloong_first_clear/scales";
    public static final String EARTHLOONG_SCALE =
            "openworld_rpg:earthloong_scale";
    public static final int FIRST_CLEAR_SCALE_COUNT = 2;

    private R01EarthloongFirstClearRewardService() {
    }

    public static ChoiceResult commitChoice(
            ServerPlayer player,
            R01EarthloongRewardChoice choice
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(choice, "choice");
        R01PlayerStateService.reconcileEarthloongFirstClearClaims(player);

        R01PlayerState state = R01PlayerStateService.state(player);
        if (!state.quarry().firstClear()) {
            return new ChoiceResult(ChoiceStatus.NOT_ELIGIBLE, choice);
        }

        Optional<String> committed = state.earthloongRewardChoiceFlag();
        if (committed.isPresent()
                && !committed.orElseThrow().equals(choice.choiceFlag())) {
            return new ChoiceResult(ChoiceStatus.DIFFERENT_CHOICE_ALREADY_COMMITTED, choice);
        }
        if (committed.isEmpty()) {
            R01PlayerStateService.commitEarthloongRewardChoice(
                    player,
                    choice.choiceFlag()
            );
        }

        return deliverCommittedChoice(player, choice);
    }

    public static ReconcileResult reconcilePending(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01PlayerState state =
                R01PlayerStateService.reconcileEarthloongFirstClearClaims(player);
        if (!state.quarry().firstClear()) {
            return new ReconcileResult(false, false, false);
        }

        boolean scalesDelivered = reconcileScales(player);
        state = R01PlayerStateService.state(player);

        boolean choiceCommitted = state.earthloongRewardChoiceFlag().isPresent();
        boolean choiceFinalized = state.quarry().firstClearRewardClaimed();
        if (choiceCommitted && !choiceFinalized) {
            String flag = state.earthloongRewardChoiceFlag().orElseThrow();
            R01EarthloongRewardChoice choice = R01EarthloongRewardChoice
                    .fromChoiceFlag(flag)
                    .orElseThrow(() -> new IllegalStateException(
                            "Unknown persisted Earthloong reward choice: " + flag
                    ));
            ChoiceResult result = deliverCommittedChoice(player, choice);
            choiceFinalized = result.status() == ChoiceStatus.DELIVERED
                    || result.status() == ChoiceStatus.PENDING_REWARD_CLAIM
                    || result.status() == ChoiceStatus.ALREADY_COMMITTED;
        }

        return new ReconcileResult(
                scalesDelivered,
                choiceCommitted,
                choiceFinalized
        );
    }

    private static ChoiceResult deliverCommittedChoice(
            ServerPlayer player,
            R01EarthloongRewardChoice choice
    ) {
        R01PlayerState state = R01PlayerStateService.state(player);
        if (state.quarry().firstClearRewardClaimed()) {
            return new ChoiceResult(ChoiceStatus.ALREADY_COMMITTED, choice);
        }

        PlayerInventoryState.DeliveryResult delivery =
                PlayerInventoryService.deliverImportantOnce(
                        player,
                        CHOICE_DELIVERY_TRANSACTION,
                        choice.inventoryItem()
                );

        R01PlayerStateService.markEarthloongRewardChoiceDelivered(
                player,
                choice.choiceFlag()
        );
        return new ChoiceResult(
                switch (delivery.status()) {
                    case PENDING, STILL_PENDING -> ChoiceStatus.PENDING_REWARD_CLAIM;
                    case DELIVERED -> ChoiceStatus.DELIVERED;
                    case ALREADY_COMPLETED -> ChoiceStatus.ALREADY_COMMITTED;
                },
                choice
        );
    }

    private static boolean reconcileScales(ServerPlayer player) {
        R01PlayerState state = R01PlayerStateService.state(player);
        if (!state.economy().pendingRewardClaimIds().contains(
                R01PlayerState.EARTHLOONG_SCALE_CLAIM_ID
        )) {
            return state.ledger().rewardClaimIds().contains(
                    R01PlayerState.EARTHLOONG_SCALE_CLAIM_ID
            );
        }

        PlayerInventoryState.MaterialDeliveryResult delivery =
                PlayerInventoryService.deliverMaterialToPouchOnce(
                        player,
                        SCALE_DELIVERY_TRANSACTION,
                        EARTHLOONG_SCALE,
                        FIRST_CLEAR_SCALE_COUNT
                );
        if (delivery.status()
                == PlayerInventoryState.MaterialDeliveryStatus.CAPACITY_BLOCKED) {
            return false;
        }

        R01PlayerStateService.markEarthloongScalesDelivered(player);
        // This is a single bounded first-clear transaction. Keep the inventory receipt so a
        // save interruption can never turn a recovered R01 claim into duplicate scale delivery.
        return true;
    }

    public enum ChoiceStatus {
        DELIVERED,
        PENDING_REWARD_CLAIM,
        ALREADY_COMMITTED,
        NOT_ELIGIBLE,
        DIFFERENT_CHOICE_ALREADY_COMMITTED
    }

    public record ChoiceResult(
            ChoiceStatus status,
            R01EarthloongRewardChoice choice
    ) {
        public ChoiceResult {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(choice, "choice");
        }
    }

    public record ReconcileResult(
            boolean scalesDelivered,
            boolean choiceCommitted,
            boolean choiceFinalized
    ) {
    }
}
